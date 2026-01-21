import Foundation
import Combine

/// Persistent storage for sync queue items
/// Uses UserDefaults for simplicity - can be migrated to CoreData/SwiftData for better performance
class SyncQueueStore: ObservableObject {
    static let shared = SyncQueueStore()

    private let storageKey = "medistock_sync_queue"
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    @Published private(set) var items: [SyncQueueItem] = []
    @Published private(set) var pendingCount: Int = 0
    @Published private(set) var conflictCount: Int = 0

    private init() {
        loadFromStorage()
        updateCounts()
    }

    // MARK: - Public Interface

    /// Add an item to the queue
    func enqueue(_ item: SyncQueueItem) {
        items.append(item)
        saveToStorage()
        updateCounts()
    }

    /// Update an existing item
    func update(_ item: SyncQueueItem) {
        if let index = items.firstIndex(where: { $0.id == item.id }) {
            items[index] = item
            saveToStorage()
            updateCounts()
        }
    }

    /// Remove an item from the queue
    func remove(id: String) {
        items.removeAll { $0.id == id }
        saveToStorage()
        updateCounts()
    }

    /// Remove all synced items
    func removeSynced() {
        items.removeAll { $0.status == .synced }
        saveToStorage()
        updateCounts()
    }

    /// Get pending items for processing (ordered by creation date)
    func getPendingItems(limit: Int = SyncConfiguration.batchSize) -> [SyncQueueItem] {
        items
            .filter { $0.status == .pending }
            .sorted { $0.createdAt < $1.createdAt }
            .prefix(limit)
            .map { $0 }
    }

    /// Get items with conflicts
    func getConflictItems() -> [SyncQueueItem] {
        items.filter { $0.status == .conflict }
    }

    /// Get failed items
    func getFailedItems() -> [SyncQueueItem] {
        items.filter { $0.status == .failed }
    }

    /// Find existing operation for an entity
    func findExisting(entityType: EntityType, entityId: String) -> SyncQueueItem? {
        items.first { $0.entityType == entityType && $0.entityId == entityId && $0.status == .pending }
    }

    /// Remove obsolete operations before a DELETE
    func removeObsoleteBeforeDelete(entityType: EntityType, entityId: String) {
        items.removeAll {
            $0.entityType == entityType &&
            $0.entityId == entityId &&
            $0.status == .pending
        }
        saveToStorage()
        updateCounts()
    }

    /// Update status and retry count for an item
    func updateStatus(id: String, status: SyncStatus, error: String? = nil) {
        if let index = items.firstIndex(where: { $0.id == id }) {
            var item = items[index]
            item.status = status
            item.lastError = error
            item.lastAttemptAt = Date()
            if status == .failed || status == .pending {
                item.retryCount += 1
            }
            items[index] = item
            saveToStorage()
            updateCounts()
        }
    }

    /// Clear all items (use with caution)
    func clearAll() {
        items.removeAll()
        saveToStorage()
        updateCounts()
    }

    // MARK: - Persistence

    private func loadFromStorage() {
        guard let data = UserDefaults.standard.data(forKey: storageKey),
              let decoded = try? decoder.decode([SyncQueueItem].self, from: data) else {
            items = []
            return
        }
        items = decoded
    }

    private func saveToStorage() {
        guard let data = try? encoder.encode(items) else { return }
        UserDefaults.standard.set(data, forKey: storageKey)
    }

    private func updateCounts() {
        pendingCount = items.filter { $0.status == .pending || $0.status == .inProgress }.count
        conflictCount = items.filter { $0.status == .conflict }.count
    }
}
