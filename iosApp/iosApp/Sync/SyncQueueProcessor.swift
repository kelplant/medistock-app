import Foundation
import Combine

/// Processes pending sync queue items
/// Mirrors Android's SyncQueueProcessor
class SyncQueueProcessor: ObservableObject {
    static let shared = SyncQueueProcessor()

    // MARK: - Published State

    @Published private(set) var state = ProcessorState()

    // MARK: - Dependencies

    private let store = SyncQueueStore.shared
    private let statusManager = SyncStatusManager.shared
    private let supabase = SupabaseService.shared
    private let decoder = JSONDecoder()

    private var isProcessing = false
    private var processingTask: Task<Void, Never>?

    private init() {}

    // MARK: - Public Interface

    /// Start processing the queue
    func startProcessing() {
        guard !isProcessing else {
            debugLog("SyncQueueProcessor", "Already processing")
            return
        }

        guard statusManager.isOnline else {
            debugLog("SyncQueueProcessor", "Cannot process: offline")
            return
        }

        guard supabase.isConfigured else {
            debugLog("SyncQueueProcessor", "Cannot process: Supabase not configured")
            return
        }

        processingTask = Task {
            await processQueue()
        }
    }

    /// Stop processing
    func stopProcessing() {
        processingTask?.cancel()
        processingTask = nil
        isProcessing = false
        statusManager.setSyncing(false)
    }

    // MARK: - Queue Processing

    private func processQueue() async {
        isProcessing = true
        await MainActor.run {
            state = ProcessorState(isProcessing: true)
            statusManager.setSyncing(true)
        }

        debugLog("SyncQueueProcessor", "Starting queue processing...")

        var processedCount = 0
        var successCount = 0
        var failedCount = 0
        var conflictCount = 0

        while !Task.isCancelled {
            // Use async method to get pending items from shared repository
            let batch = await store.getPendingItemsAsync(limit: SyncConfiguration.batchSize)

            if batch.isEmpty {
                break
            }

            for item in batch {
                guard !Task.isCancelled else { break }

                // Mark as in progress
                await store.updateStatusAsync(id: item.id, status: .inProgress)

                let result = await processItem(item)

                switch result {
                case .success:
                    await store.updateStatusAsync(id: item.id, status: .synced)
                    successCount += 1

                case .conflict(_, _):
                    await store.updateStatusAsync(id: item.id, status: .conflict)
                    conflictCount += 1

                case .retry(let error):
                    if item.retryCount >= SyncConfiguration.maxRetries {
                        await store.updateStatusAsync(id: item.id, status: .failed, error: error.localizedDescription)
                        failedCount += 1
                    } else {
                        // Schedule retry with backoff
                        await store.updateStatusAsync(id: item.id, status: .pending, error: error.localizedDescription)
                        let delay = SyncConfiguration.backoffDelay(for: item.retryCount)
                        try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                    }

                case .skip(let reason):
                    debugLog("SyncQueueProcessor", "Skipping \(item.entityType.rawValue) \(item.entityId): \(reason)")
                    store.remove(id: item.id)
                }

                processedCount += 1

                await MainActor.run {
                    state = ProcessorState(
                        isProcessing: true,
                        processedCount: processedCount,
                        successCount: successCount,
                        failedCount: failedCount,
                        conflictCount: conflictCount
                    )
                }
            }
        }

        // Cleanup synced items
        store.removeSynced()

        // Refresh counts after cleanup
        await store.refreshCounts()

        await MainActor.run {
            state = ProcessorState(
                isProcessing: false,
                processedCount: processedCount,
                successCount: successCount,
                failedCount: failedCount,
                conflictCount: conflictCount
            )
            statusManager.setSyncing(false)

            if failedCount == 0 && conflictCount == 0 {
                statusManager.recordSyncSuccess()
            } else {
                statusManager.recordSyncFailure(error: "Sync completed with \(failedCount) failures, \(conflictCount) conflicts")
            }
        }

        isProcessing = false
        debugLog("SyncQueueProcessor", "Processing complete: \(successCount) success, \(failedCount) failed, \(conflictCount) conflicts")
    }

    // MARK: - Item Processing

    private func processItem(_ item: SyncQueueItem) async -> ProcessResult {
        debugLog("SyncQueueProcessor", "Processing \(item.operation.rawValue) \(item.entityType.rawValue) \(item.entityId)")

        do {
            switch item.operation {
            case .insert:
                return try await processInsert(item)

            case .update:
                return try await processUpdate(item)

            case .delete:
                return try await processDelete(item)
            }
        } catch {
            debugLog("SyncQueueProcessor", "Error processing item: \(error)")
            return .retry(error: error)
        }
    }

    private func processInsert(_ item: SyncQueueItem) async throws -> ProcessResult {
        guard let payload = item.payload else {
            return .skip(reason: "No payload for INSERT")
        }

        let table = item.entityType.tableName

        switch item.entityType {
        case .site:
            let dto = try decoder.decode(SiteDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .category:
            let dto = try decoder.decode(CategoryDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .packagingType:
            let dto = try decoder.decode(PackagingTypeDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .product:
            let dto = try decoder.decode(ProductDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .customer:
            let dto = try decoder.decode(CustomerDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .user:
            let dto = try decoder.decode(UserDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .purchaseBatch:
            let dto = try decoder.decode(PurchaseBatchDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .sale:
            let dto = try decoder.decode(SaleDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .saleItem:
            let dto = try decoder.decode(SaleItemDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .stockMovement:
            let dto = try decoder.decode(StockMovementDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        default:
            return .skip(reason: "Unsupported entity type for INSERT")
        }

        return .success
    }

    private func processUpdate(_ item: SyncQueueItem) async throws -> ProcessResult {
        guard let payload = item.payload else {
            return .skip(reason: "No payload for UPDATE")
        }

        let table = item.entityType.tableName

        // Check for conflicts
        if let conflict = try await detectConflict(item) {
            return conflict
        }

        // Same logic as insert - use upsert
        return try await processInsert(item)
    }

    private func processDelete(_ item: SyncQueueItem) async throws -> ProcessResult {
        let table = item.entityType.tableName

        try await supabase.delete(from: table, id: item.entityId)

        return .success
    }

    // MARK: - Conflict Detection

    private func detectConflict(_ item: SyncQueueItem) async throws -> ProcessResult? {
        guard let lastKnown = item.lastKnownRemoteUpdatedAt else {
            // No last known version - no conflict detection possible
            return nil
        }

        let table = item.entityType.tableName

        // Fetch remote version
        struct RemoteVersion: Decodable {
            let updatedAt: Int64

            enum CodingKeys: String, CodingKey {
                case updatedAt = "updated_at"
            }
        }

        if let remote: RemoteVersion = try await supabase.fetchById(from: table, id: item.entityId) {
            if remote.updatedAt > lastKnown {
                // Conflict detected - apply resolution strategy
                return try await resolveConflict(item, remoteUpdatedAt: remote.updatedAt)
            }
        }

        return nil
    }

    private func resolveConflict(_ item: SyncQueueItem, remoteUpdatedAt: Int64) async throws -> ProcessResult? {
        let strategy = item.entityType.conflictStrategy

        switch strategy {
        case .serverWins:
            // Skip local change, server version wins
            debugLog("SyncQueueProcessor", "Conflict resolved: SERVER_WINS for \(item.entityType.rawValue)")
            return .skip(reason: "Server version wins")

        case .clientWins:
            // Force push local change
            debugLog("SyncQueueProcessor", "Conflict resolved: CLIENT_WINS for \(item.entityType.rawValue)")
            return nil // Continue with update

        case .merge:
            // Merge local and remote: take remote as base, overlay local changes
            debugLog("SyncQueueProcessor", "Conflict resolved: MERGE for \(item.entityType.rawValue)")
            return try await processMerge(item, remoteUpdatedAt: remoteUpdatedAt)

        case .askUser:
            // Mark as conflict for user resolution
            return .conflict(local: item.payload, remote: nil)

        case .keepBoth:
            // Keep remote, create a copy of local with new ID
            debugLog("SyncQueueProcessor", "Conflict resolved: KEEP_BOTH for \(item.entityType.rawValue)")
            return try await processKeepBoth(item)
        }
    }

    // MARK: - Merge Strategy Implementation

    /// Merge strategy: combines local and remote versions
    /// - Remote fields are preserved except for fields explicitly changed locally
    /// - Uses timestamp comparison: newer timestamp wins for each field
    private func processMerge(_ item: SyncQueueItem, remoteUpdatedAt: Int64) async throws -> ProcessResult? {
        guard let localPayload = item.payload else {
            return .skip(reason: "No local payload for MERGE")
        }

        let table = item.entityType.tableName

        // Fetch the full remote record
        guard let remoteData: Data = try await fetchRemoteAsData(table: table, id: item.entityId) else {
            // Remote doesn't exist anymore, just insert local
            debugLog("SyncQueueProcessor", "Remote record not found during merge, inserting local")
            return nil
        }

        // Parse both as dictionaries for field-level merge
        guard var localDict = try? JSONSerialization.jsonObject(with: localPayload) as? [String: Any],
              var remoteDict = try? JSONSerialization.jsonObject(with: remoteData) as? [String: Any] else {
            // Can't parse, fall back to client wins
            debugLog("SyncQueueProcessor", "Cannot parse payloads for merge, falling back to client wins")
            return nil
        }

        // Merge strategy: start with remote, overlay non-nil local values
        // This preserves remote changes while applying local updates
        for (key, value) in localDict {
            // Skip system fields that should come from remote
            if ["created_at", "created_by", "sync_version"].contains(key) {
                continue
            }
            // Apply local value if it's not nil
            if !(value is NSNull) {
                remoteDict[key] = value
            }
        }

        // Update the timestamp
        remoteDict["updated_at"] = Int64(Date().timeIntervalSince1970 * 1000)

        // Convert merged dict back to payload and upsert
        guard let mergedData = try? JSONSerialization.data(withJSONObject: remoteDict) else {
            return nil
        }

        // Upsert the merged record based on entity type
        try await upsertMergedRecord(table: table, entityType: item.entityType, payload: mergedData)

        debugLog("SyncQueueProcessor", "Merge completed for \(item.entityType.rawValue) \(item.entityId)")
        return .success
    }

    /// Keep both strategy: keeps remote as-is and creates a copy of local with new ID
    private func processKeepBoth(_ item: SyncQueueItem) async throws -> ProcessResult? {
        guard let localPayload = item.payload else {
            return .skip(reason: "No local payload for KEEP_BOTH")
        }

        // Parse local payload
        guard var localDict = try? JSONSerialization.jsonObject(with: localPayload) as? [String: Any] else {
            return .skip(reason: "Cannot parse local payload for KEEP_BOTH")
        }

        // Generate new ID for the copy
        let newId = UUID().uuidString
        let originalId = item.entityId

        localDict["id"] = newId
        localDict["created_at"] = Int64(Date().timeIntervalSince1970 * 1000)
        localDict["updated_at"] = Int64(Date().timeIntervalSince1970 * 1000)

        // Add suffix to name if present to indicate it's a copy
        if let name = localDict["name"] as? String {
            localDict["name"] = "\(name) (copie locale)"
        }

        guard let newPayload = try? JSONSerialization.data(withJSONObject: localDict) else {
            return .skip(reason: "Cannot serialize new payload for KEEP_BOTH")
        }

        let table = item.entityType.tableName

        // Insert the new copy
        try await upsertMergedRecord(table: table, entityType: item.entityType, payload: newPayload)

        debugLog("SyncQueueProcessor", "Keep both: created copy \(newId) of \(originalId) for \(item.entityType.rawValue)")
        return .success
    }

    // MARK: - Helper Methods

    private func fetchRemoteAsData(table: String, id: String) async throws -> Data? {
        guard let client = supabase.currentClient() else {
            return nil
        }

        // Fetch as raw JSON data
        let data = try await client.database
            .from(table)
            .select()
            .eq("id", value: id)
            .limit(1)
            .execute()
            .data

        // Parse the response array and return first element as Data
        guard let array = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]],
              let first = array.first else {
            return nil
        }

        return try JSONSerialization.data(withJSONObject: first)
    }

    private func upsertMergedRecord(table: String, entityType: EntityType, payload: Data) async throws {
        switch entityType {
        case .site:
            let dto = try decoder.decode(SiteDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .category:
            let dto = try decoder.decode(CategoryDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .packagingType:
            let dto = try decoder.decode(PackagingTypeDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .product:
            let dto = try decoder.decode(ProductDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .customer:
            let dto = try decoder.decode(CustomerDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .user:
            let dto = try decoder.decode(UserDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .purchaseBatch:
            let dto = try decoder.decode(PurchaseBatchDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .sale:
            let dto = try decoder.decode(SaleDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .saleItem:
            let dto = try decoder.decode(SaleItemDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .stockMovement:
            let dto = try decoder.decode(StockMovementDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        case .transfer:
            let dto = try decoder.decode(ProductTransferDTO.self, from: payload)
            try await supabase.upsert(into: table, record: dto)

        default:
            debugLog("SyncQueueProcessor", "Unsupported entity type for merge/keepBoth: \(entityType.rawValue)")
        }
    }
}

// MARK: - Processor State

struct ProcessorState {
    var isProcessing: Bool = false
    var processedCount: Int = 0
    var successCount: Int = 0
    var failedCount: Int = 0
    var conflictCount: Int = 0
}
