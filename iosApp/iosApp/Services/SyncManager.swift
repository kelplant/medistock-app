import Foundation
import Network
import shared

/// Centralized sync manager implementing online-first pattern
/// Mirrors Android's SyncManager architecture
class SyncManager: ObservableObject {
    static let shared = SyncManager()

    // MARK: - Published State
    @Published private(set) var isOnline = true
    @Published private(set) var isSyncing = false
    @Published private(set) var pendingCount = 0
    @Published private(set) var lastSyncDate: Date?
    @Published private(set) var lastError: String?

    // MARK: - Dependencies
    private let supabase = SupabaseClient.shared
    private let monitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "com.medistock.networkmonitor")
    private let syncQueue = DispatchQueue(label: "com.medistock.syncqueue")

    // MARK: - Pending Operations Queue (persisted in UserDefaults)
    private let pendingOpsKey = "medistock_pending_operations"

    private init() {
        loadPendingCount()
        startNetworkMonitoring()
    }

    // MARK: - Network Monitoring

    private func startNetworkMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                let wasOffline = !(self?.isOnline ?? true)
                self?.isOnline = path.status == .satisfied

                // Trigger sync when coming back online
                if wasOffline && path.status == .satisfied {
                    Task {
                        await self?.processPendingOperations()
                    }
                }
            }
        }
        monitor.start(queue: monitorQueue)
    }

    // MARK: - Online-First Operations

    /// Execute an operation with online-first pattern
    /// - If online: Execute on Supabase first, then locally
    /// - If offline: Queue for later sync, execute locally only
    func executeOnlineFirst<T>(
        entityType: String,
        entityId: String,
        operation: SyncOperation,
        remoteOperation: @escaping () async throws -> T,
        localOperation: @escaping () async throws -> Void,
        payload: Data? = nil
    ) async throws -> T? {
        // Try remote first if online and configured
        if isOnline && supabase.isConfigured {
            do {
                let result = try await remoteOperation()
                // Success - execute local operation to sync
                try await localOperation()
                return result
            } catch {
                // If network error, queue and do local only
                if isNetworkError(error) {
                    await queueOperation(entityType: entityType, entityId: entityId, operation: operation, payload: payload)
                    try await localOperation()
                    return nil
                }
                // Other errors - throw
                throw error
            }
        } else if supabase.isConfigured {
            // Offline - queue and do local only
            await queueOperation(entityType: entityType, entityId: entityId, operation: operation, payload: payload)
            try await localOperation()
            return nil
        } else {
            // Supabase not configured - local only
            try await localOperation()
            return nil
        }
    }

    /// Execute operation for entities without local storage (permissions, etc.)
    func executeOnlineOnly<T>(
        entityType: String,
        operation: @escaping () async throws -> T
    ) async throws -> T {
        guard supabase.isConfigured else {
            throw SyncError.notConfigured
        }

        guard isOnline else {
            throw SyncError.offline
        }

        return try await operation()
    }

    // MARK: - Pending Operations Queue

    private func queueOperation(entityType: String, entityId: String, operation: SyncOperation, payload: Data?) async {
        var pending = loadPendingOperations()

        // Smart queue management (like Android)
        let existingIndex = pending.firstIndex { $0.entityType == entityType && $0.entityId == entityId }

        if let index = existingIndex {
            let existing = pending[index]
            switch (existing.operation, operation) {
            case (.insert, .update):
                // INSERT + UPDATE = update the INSERT payload
                pending[index] = PendingOperation(
                    entityType: entityType,
                    entityId: entityId,
                    operation: .insert,
                    payload: payload,
                    createdAt: existing.createdAt
                )
            case (.insert, .delete):
                // INSERT + DELETE = cancel both (never synced)
                pending.remove(at: index)
            case (.update, .update):
                // UPDATE + UPDATE = replace with latest
                pending[index] = PendingOperation(
                    entityType: entityType,
                    entityId: entityId,
                    operation: .update,
                    payload: payload,
                    createdAt: existing.createdAt
                )
            case (.update, .delete):
                // UPDATE + DELETE = replace with DELETE
                pending[index] = PendingOperation(
                    entityType: entityType,
                    entityId: entityId,
                    operation: .delete,
                    payload: nil,
                    createdAt: existing.createdAt
                )
            default:
                // Add new operation
                pending.append(PendingOperation(
                    entityType: entityType,
                    entityId: entityId,
                    operation: operation,
                    payload: payload,
                    createdAt: Date()
                ))
            }
        } else {
            pending.append(PendingOperation(
                entityType: entityType,
                entityId: entityId,
                operation: operation,
                payload: payload,
                createdAt: Date()
            ))
        }

        savePendingOperations(pending)
        await MainActor.run {
            self.pendingCount = pending.count
        }
    }

    func processPendingOperations() async {
        guard isOnline && supabase.isConfigured else { return }

        var pending = loadPendingOperations()
        guard !pending.isEmpty else { return }

        await MainActor.run { isSyncing = true }

        var failedOps: [PendingOperation] = []

        for op in pending {
            do {
                try await processOperation(op)
            } catch {
                print("Failed to sync \(op.entityType) \(op.entityId): \(error)")
                // Keep failed operations for retry
                var failedOp = op
                failedOp.retryCount += 1
                if failedOp.retryCount < 5 {
                    failedOps.append(failedOp)
                }
            }
        }

        savePendingOperations(failedOps)

        await MainActor.run {
            self.pendingCount = failedOps.count
            self.isSyncing = false
            self.lastSyncDate = Date()
            self.lastError = failedOps.isEmpty ? nil : "Certaines opérations n'ont pas pu être synchronisées"
        }
    }

    private func processOperation(_ op: PendingOperation) async throws {
        // This will be called by specific repositories
        // For now, notify that we need external processing
        NotificationCenter.default.post(
            name: .syncOperationNeeded,
            object: nil,
            userInfo: [
                "entityType": op.entityType,
                "entityId": op.entityId,
                "operation": op.operation.rawValue,
                "payload": op.payload as Any
            ]
        )
    }

    // MARK: - Persistence

    private func loadPendingOperations() -> [PendingOperation] {
        guard let data = UserDefaults.standard.data(forKey: pendingOpsKey),
              let ops = try? JSONDecoder().decode([PendingOperation].self, from: data) else {
            return []
        }
        return ops
    }

    private func savePendingOperations(_ ops: [PendingOperation]) {
        if let data = try? JSONEncoder().encode(ops) {
            UserDefaults.standard.set(data, forKey: pendingOpsKey)
        }
    }

    private func loadPendingCount() {
        pendingCount = loadPendingOperations().count
    }

    // MARK: - Helpers

    private func isNetworkError(_ error: Error) -> Bool {
        let nsError = error as NSError
        return nsError.domain == NSURLErrorDomain &&
            [NSURLErrorNotConnectedToInternet,
             NSURLErrorNetworkConnectionLost,
             NSURLErrorTimedOut,
             NSURLErrorCannotConnectToHost].contains(nsError.code)
    }

    // MARK: - Manual Sync Trigger

    func triggerSync() async {
        await processPendingOperations()
    }

    func clearPendingOperations() {
        UserDefaults.standard.removeObject(forKey: pendingOpsKey)
        DispatchQueue.main.async {
            self.pendingCount = 0
        }
    }
}

// MARK: - Supporting Types

enum SyncOperation: String, Codable {
    case insert
    case update
    case delete
}

struct PendingOperation: Codable {
    let entityType: String
    let entityId: String
    let operation: SyncOperation
    let payload: Data?
    let createdAt: Date
    var retryCount: Int = 0
}

enum SyncError: LocalizedError {
    case notConfigured
    case offline
    case conflict
    case serverError(String)

    var errorDescription: String? {
        switch self {
        case .notConfigured:
            return "Supabase n'est pas configuré"
        case .offline:
            return "Pas de connexion internet"
        case .conflict:
            return "Conflit de synchronisation"
        case .serverError(let message):
            return "Erreur serveur: \(message)"
        }
    }
}

// MARK: - Notification Names

extension Notification.Name {
    static let syncOperationNeeded = Notification.Name("syncOperationNeeded")
    static let syncCompleted = Notification.Name("syncCompleted")
}

// MARK: - Remote Models for Supabase

struct RemoteSite: Codable, Identifiable {
    let id: String
    let name: String
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
}

struct RemoteCategory: Codable, Identifiable {
    let id: String
    let name: String
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
}

struct RemotePackagingType: Codable, Identifiable {
    let id: String
    let name: String
    let level1Name: String
    let level2Name: String?
    let level2Quantity: Int32?
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
}

struct RemoteProduct: Codable, Identifiable {
    let id: String
    let name: String
    let description: String?
    let unit: String
    let unitVolume: Double?
    let packagingTypeId: String?
    let conversionFactor: Double
    let categoryId: String?
    let marginType: String
    let marginValue: Double
    let selectedLevel: String
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String

    enum CodingKeys: String, CodingKey {
        case id, name, unit, conversionFactor, categoryId, marginType, marginValue
        case selectedLevel, createdAt, updatedAt, createdBy, updatedBy
        case description = "description"
        case unitVolume, packagingTypeId
    }
}

struct RemoteCustomer: Codable, Identifiable {
    let id: String
    let name: String
    let phone: String?
    let email: String?
    let address: String?
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
}

struct RemoteSiteProduct: Codable {
    let id: String
    let siteId: String
    let productId: String
    let minStock: Double
    let maxStock: Double
    let createdAt: Int64
    let updatedAt: Int64
    let createdBy: String
    let updatedBy: String
}
