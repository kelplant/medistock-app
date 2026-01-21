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
            print("[SyncQueueProcessor] Already processing")
            return
        }

        guard statusManager.isOnline else {
            print("[SyncQueueProcessor] Cannot process: offline")
            return
        }

        guard supabase.isConfigured else {
            print("[SyncQueueProcessor] Cannot process: Supabase not configured")
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

        print("[SyncQueueProcessor] Starting queue processing...")

        var processedCount = 0
        var successCount = 0
        var failedCount = 0
        var conflictCount = 0

        while !Task.isCancelled {
            let batch = store.getPendingItems(limit: SyncConfiguration.batchSize)

            if batch.isEmpty {
                break
            }

            for item in batch {
                guard !Task.isCancelled else { break }

                // Mark as in progress
                store.updateStatus(id: item.id, status: .inProgress)

                let result = await processItem(item)

                switch result {
                case .success:
                    store.updateStatus(id: item.id, status: .synced)
                    successCount += 1

                case .conflict(_, _):
                    store.updateStatus(id: item.id, status: .conflict)
                    conflictCount += 1

                case .retry(let error):
                    if item.retryCount >= SyncConfiguration.maxRetries {
                        store.updateStatus(id: item.id, status: .failed, error: error.localizedDescription)
                        failedCount += 1
                    } else {
                        // Schedule retry with backoff
                        store.updateStatus(id: item.id, status: .pending, error: error.localizedDescription)
                        let delay = SyncConfiguration.backoffDelay(for: item.retryCount)
                        try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                    }

                case .skip(let reason):
                    print("[SyncQueueProcessor] Skipping \(item.entityType.rawValue) \(item.entityId): \(reason)")
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
        print("[SyncQueueProcessor] Processing complete: \(successCount) success, \(failedCount) failed, \(conflictCount) conflicts")
    }

    // MARK: - Item Processing

    private func processItem(_ item: SyncQueueItem) async -> ProcessResult {
        print("[SyncQueueProcessor] Processing \(item.operation.rawValue) \(item.entityType.rawValue) \(item.entityId)")

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
            print("[SyncQueueProcessor] Error processing item: \(error)")
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
            print("[SyncQueueProcessor] Conflict resolved: SERVER_WINS for \(item.entityType.rawValue)")
            return .skip(reason: "Server version wins")

        case .clientWins:
            // Force push local change
            print("[SyncQueueProcessor] Conflict resolved: CLIENT_WINS for \(item.entityType.rawValue)")
            return nil // Continue with update

        case .merge:
            // TODO: Implement proper merge logic
            print("[SyncQueueProcessor] Conflict resolved: MERGE for \(item.entityType.rawValue)")
            return nil // For now, treat as client wins

        case .askUser:
            // Mark as conflict for user resolution
            return .conflict(local: item.payload, remote: nil)

        case .keepBoth:
            // TODO: Implement keep both (create copy with new ID)
            return nil
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
