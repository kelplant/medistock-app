import Foundation
import Combine
import shared

/// Persistent storage for sync queue items
/// Uses shared SyncQueueRepository (SQLDelight) for cross-platform consistency
class SyncQueueStore: ObservableObject {
    static let shared = SyncQueueStore()

    @Published private(set) var items: [SyncQueueItem] = []
    @Published private(set) var pendingCount: Int = 0
    @Published private(set) var conflictCount: Int = 0

    private var repository: SyncQueueRepository {
        SDKProvider.shared.syncQueueRepository
    }

    private init() {
        // Load initial data
        Task {
            await loadFromRepository()
        }
    }

    // MARK: - Public Interface

    /// Add an item to the queue
    func enqueue(_ item: SyncQueueItem) {
        Task {
            do {
                try await repository.insert(item: item.toKotlinModel())
                await loadFromRepository()
                debugLog("SyncQueueStore", "Enqueued item: \(item.entityType.rawValue) \(item.entityId)")
            } catch {
                debugLog("SyncQueueStore", "Error enqueueing item: \(error)")
            }
        }
    }

    /// Update an existing item
    func update(_ item: SyncQueueItem) {
        Task {
            do {
                try await repository.update(item: item.toKotlinModel())
                await loadFromRepository()
            } catch {
                debugLog("SyncQueueStore", "Error updating item: \(error)")
            }
        }
    }

    /// Remove an item from the queue
    func remove(id: String) {
        Task {
            do {
                try await repository.deleteById(id: id)
                await loadFromRepository()
            } catch {
                debugLog("SyncQueueStore", "Error removing item: \(error)")
            }
        }
    }

    /// Remove all synced items
    func removeSynced() {
        Task {
            do {
                try await repository.deleteSynced()
                await loadFromRepository()
            } catch {
                debugLog("SyncQueueStore", "Error removing synced items: \(error)")
            }
        }
    }

    /// Get pending items for processing (ordered by creation date)
    func getPendingItems(limit: Int = SyncConfiguration.batchSize) -> [SyncQueueItem] {
        return items
            .filter { $0.status == .pending }
            .sorted { $0.createdAt < $1.createdAt }
            .prefix(limit)
            .map { $0 }
    }

    /// Get pending items asynchronously from repository
    func getPendingItemsAsync(limit: Int = SyncConfiguration.batchSize) async -> [SyncQueueItem] {
        do {
            let kotlinItems = try await repository.getPendingBatch(limit: Int32(limit))
            return kotlinItems.map { SyncQueueItem(from: $0 as! shared.SyncQueueItem) }
        } catch {
            debugLog("SyncQueueStore", "Error getting pending batch: \(error)")
            return []
        }
    }

    /// Get items with conflicts
    func getConflictItems() -> [SyncQueueItem] {
        return items.filter { $0.status == .conflict }
    }

    /// Get failed items
    func getFailedItems() -> [SyncQueueItem] {
        return items.filter { $0.status == .failed }
    }

    /// Find existing operation for an entity
    func findExisting(entityType: EntityType, entityId: String) -> SyncQueueItem? {
        return items.first { $0.entityType == entityType && $0.entityId == entityId && $0.status == .pending }
    }

    /// Find existing operation for an entity asynchronously
    func findExistingAsync(entityType: EntityType, entityId: String) async -> SyncQueueItem? {
        do {
            if let kotlinItem = try await repository.getLatestPendingForEntity(
                entityType: entityType.rawValue,
                entityId: entityId
            ) {
                return SyncQueueItem(from: kotlinItem)
            }
            return nil
        } catch {
            debugLog("SyncQueueStore", "Error finding existing: \(error)")
            return nil
        }
    }

    /// Remove obsolete operations before a DELETE
    func removeObsoleteBeforeDelete(entityType: EntityType, entityId: String) {
        Task {
            do {
                try await repository.removeObsoleteBeforeDelete(
                    entityType: entityType.rawValue,
                    entityId: entityId
                )
                await loadFromRepository()
            } catch {
                debugLog("SyncQueueStore", "Error removing obsolete: \(error)")
            }
        }
    }

    /// Update status and retry count for an item
    func updateStatus(id: String, status: SyncStatus, error: String? = nil) {
        Task {
            await updateStatusAsync(id: id, status: status, error: error)
        }
    }

    /// Update status asynchronously
    func updateStatusAsync(id: String, status: SyncStatus, error: String? = nil) async {
        do {
            if status == .pending || status == .failed {
                try await repository.updateStatusWithRetry(
                    id: id,
                    status: status.toKotlinStatus(),
                    attemptAt: Int64(Date().timeIntervalSince1970 * 1000),
                    error: error
                )
            } else {
                try await repository.updateStatus(id: id, status: status.toKotlinStatus())
            }
            await loadFromRepository()
        } catch {
            debugLog("SyncQueueStore", "Error updating status: \(error)")
        }
    }

    /// Clear all items (use with caution)
    func clearAll() {
        Task {
            do {
                try await repository.deleteByStatus(status: .pending)
                try await repository.deleteByStatus(status: .synced)
                try await repository.deleteByStatus(status: .failed)
                try await repository.deleteByStatus(status: .conflict)
                try await repository.deleteByStatus(status: .inProgress)
                await loadFromRepository()
            } catch {
                debugLog("SyncQueueStore", "Error clearing all: \(error)")
            }
        }
    }

    // MARK: - Repository Integration

    /// Load items from the shared repository
    @MainActor
    func loadFromRepository() async {
        do {
            let pendingItems = try await repository.getPending()
            let conflictItems = try await repository.getConflicts()
            let failedItems = try await repository.getFailed()

            var allItems: [SyncQueueItem] = []
            for kotlinItem in pendingItems {
                allItems.append(SyncQueueItem(from: kotlinItem as! shared.SyncQueueItem))
            }
            for kotlinItem in conflictItems {
                allItems.append(SyncQueueItem(from: kotlinItem as! shared.SyncQueueItem))
            }
            for kotlinItem in failedItems {
                allItems.append(SyncQueueItem(from: kotlinItem as! shared.SyncQueueItem))
            }

            self.items = allItems
            self.pendingCount = Int(try await repository.getPendingCount())
            self.conflictCount = Int(try await repository.getConflictCount())
        } catch {
            debugLog("SyncQueueStore", "Error loading from repository: \(error)")
        }
    }

    /// Refresh counts from repository
    @MainActor
    func refreshCounts() async {
        do {
            self.pendingCount = Int(try await repository.getPendingCount())
            self.conflictCount = Int(try await repository.getConflictCount())
        } catch {
            debugLog("SyncQueueStore", "Error refreshing counts: \(error)")
        }
    }
}
