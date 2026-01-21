import Foundation
import shared

/// Helper for enqueuing sync operations with smart deduplication
/// Mirrors Android's SyncQueueHelper
class SyncQueueHelper {
    static let shared = SyncQueueHelper()

    private let store = SyncQueueStore.shared
    private let encoder = JSONEncoder()

    private init() {}

    // MARK: - Generic Enqueue Methods

    /// Enqueue an INSERT operation
    func enqueueInsert<T: Encodable>(
        entityType: EntityType,
        entityId: String,
        entity: T,
        userId: String,
        siteId: String? = nil
    ) {
        let payload = try? encoder.encode(entity)

        let item = SyncQueueItem(
            entityType: entityType,
            entityId: entityId,
            operation: .insert,
            payload: payload,
            userId: userId,
            siteId: siteId
        )

        store.enqueue(item)
        print("[SyncQueueHelper] Enqueued INSERT for \(entityType.rawValue) \(entityId)")
    }

    /// Enqueue an UPDATE operation with smart deduplication
    func enqueueUpdate<T: Encodable>(
        entityType: EntityType,
        entityId: String,
        entity: T,
        userId: String,
        siteId: String? = nil,
        lastKnownRemoteUpdatedAt: Int64? = nil
    ) {
        let payload = try? encoder.encode(entity)

        // Check for existing operation
        if let existing = store.findExisting(entityType: entityType, entityId: entityId) {
            switch existing.operation {
            case .insert:
                // INSERT + UPDATE = Keep INSERT with updated payload
                var updated = existing
                store.remove(id: existing.id)
                let newItem = SyncQueueItem(
                    id: existing.id,
                    entityType: entityType,
                    entityId: entityId,
                    operation: .insert,
                    payload: payload,
                    localVersion: existing.localVersion,
                    remoteVersion: existing.remoteVersion,
                    lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt ?? existing.lastKnownRemoteUpdatedAt,
                    createdAt: existing.createdAt,
                    userId: userId,
                    siteId: siteId
                )
                store.enqueue(newItem)
                print("[SyncQueueHelper] Updated INSERT payload for \(entityType.rawValue) \(entityId)")
                return

            case .update:
                // UPDATE + UPDATE = Replace with latest
                store.remove(id: existing.id)
                let newItem = SyncQueueItem(
                    id: existing.id,
                    entityType: entityType,
                    entityId: entityId,
                    operation: .update,
                    payload: payload,
                    localVersion: existing.localVersion + 1,
                    remoteVersion: existing.remoteVersion,
                    lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt ?? existing.lastKnownRemoteUpdatedAt,
                    createdAt: existing.createdAt,
                    userId: userId,
                    siteId: siteId
                )
                store.enqueue(newItem)
                print("[SyncQueueHelper] Replaced UPDATE for \(entityType.rawValue) \(entityId)")
                return

            case .delete:
                // DELETE + UPDATE = Ignore UPDATE (DELETE takes precedence)
                print("[SyncQueueHelper] Ignoring UPDATE after DELETE for \(entityType.rawValue) \(entityId)")
                return
            }
        }

        // No existing operation - create new UPDATE
        let item = SyncQueueItem(
            entityType: entityType,
            entityId: entityId,
            operation: .update,
            payload: payload,
            lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt,
            userId: userId,
            siteId: siteId
        )

        store.enqueue(item)
        print("[SyncQueueHelper] Enqueued UPDATE for \(entityType.rawValue) \(entityId)")
    }

    /// Enqueue a DELETE operation with cleanup of obsolete operations
    func enqueueDelete(
        entityType: EntityType,
        entityId: String,
        userId: String,
        siteId: String? = nil
    ) {
        // Check for existing operation
        if let existing = store.findExisting(entityType: entityType, entityId: entityId) {
            switch existing.operation {
            case .insert:
                // INSERT + DELETE = Cancel both (entity was never synced)
                store.remove(id: existing.id)
                print("[SyncQueueHelper] Cancelled INSERT+DELETE for \(entityType.rawValue) \(entityId)")
                return

            case .update, .delete:
                // Remove existing and add DELETE
                store.removeObsoleteBeforeDelete(entityType: entityType, entityId: entityId)
            }
        }

        let item = SyncQueueItem(
            entityType: entityType,
            entityId: entityId,
            operation: .delete,
            payload: nil,
            userId: userId,
            siteId: siteId
        )

        store.enqueue(item)
        print("[SyncQueueHelper] Enqueued DELETE for \(entityType.rawValue) \(entityId)")
    }

    // MARK: - Entity-Specific Methods

    func enqueueSiteInsert(_ site: Site, userId: String) {
        let dto = SiteDTO(from: site)
        enqueueInsert(entityType: .site, entityId: site.id, entity: dto, userId: userId)
    }

    func enqueueSiteUpdate(_ site: Site, userId: String, lastKnownRemoteUpdatedAt: Int64? = nil) {
        let dto = SiteDTO(from: site)
        enqueueUpdate(entityType: .site, entityId: site.id, entity: dto, userId: userId, lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt)
    }

    func enqueueCategoryInsert(_ category: Category, userId: String) {
        let dto = CategoryDTO(from: category)
        enqueueInsert(entityType: .category, entityId: category.id, entity: dto, userId: userId)
    }

    func enqueueCategoryUpdate(_ category: Category, userId: String, lastKnownRemoteUpdatedAt: Int64? = nil) {
        let dto = CategoryDTO(from: category)
        enqueueUpdate(entityType: .category, entityId: category.id, entity: dto, userId: userId, lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt)
    }

    func enqueueProductInsert(_ product: Product, userId: String, siteId: String) {
        let dto = ProductDTO(from: product)
        enqueueInsert(entityType: .product, entityId: product.id, entity: dto, userId: userId, siteId: siteId)
    }

    func enqueueProductUpdate(_ product: Product, userId: String, siteId: String, lastKnownRemoteUpdatedAt: Int64? = nil) {
        let dto = ProductDTO(from: product)
        enqueueUpdate(entityType: .product, entityId: product.id, entity: dto, userId: userId, siteId: siteId, lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt)
    }

    func enqueueProductDelete(_ productId: String, userId: String, siteId: String) {
        enqueueDelete(entityType: .product, entityId: productId, userId: userId, siteId: siteId)
    }

    func enqueueCustomerInsert(_ customer: Customer, userId: String) {
        let dto = CustomerDTO(from: customer)
        enqueueInsert(entityType: .customer, entityId: customer.id, entity: dto, userId: userId)
    }

    func enqueueCustomerUpdate(_ customer: Customer, userId: String, lastKnownRemoteUpdatedAt: Int64? = nil) {
        let dto = CustomerDTO(from: customer)
        enqueueUpdate(entityType: .customer, entityId: customer.id, entity: dto, userId: userId, lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt)
    }

    func enqueuePackagingTypeInsert(_ packagingType: PackagingType, userId: String) {
        let dto = PackagingTypeDTO(from: packagingType)
        enqueueInsert(entityType: .packagingType, entityId: packagingType.id, entity: dto, userId: userId)
    }

    func enqueuePackagingTypeUpdate(_ packagingType: PackagingType, userId: String, lastKnownRemoteUpdatedAt: Int64? = nil) {
        let dto = PackagingTypeDTO(from: packagingType)
        enqueueUpdate(entityType: .packagingType, entityId: packagingType.id, entity: dto, userId: userId, lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt)
    }

    func enqueuePurchaseBatchInsert(_ batch: PurchaseBatch, userId: String, siteId: String) {
        let dto = PurchaseBatchDTO(from: batch)
        enqueueInsert(entityType: .purchaseBatch, entityId: batch.id, entity: dto, userId: userId, siteId: siteId)
    }

    func enqueueSaleInsert(_ sale: Sale, items: [SaleItem], userId: String, siteId: String) {
        let dto = SaleDTO(from: sale)
        enqueueInsert(entityType: .sale, entityId: sale.id, entity: dto, userId: userId, siteId: siteId)

        // Also enqueue sale items
        for item in items {
            let itemDto = SaleItemDTO(from: item)
            enqueueInsert(entityType: .saleItem, entityId: item.id, entity: itemDto, userId: userId, siteId: siteId)
        }
    }

    func enqueueStockMovementInsert(_ movement: StockMovement, userId: String, siteId: String) {
        let dto = StockMovementDTO(from: movement)
        enqueueInsert(entityType: .stockMovement, entityId: movement.id, entity: dto, userId: userId, siteId: siteId)
    }

    func enqueueUserInsert(_ user: User, userId: String) {
        let dto = UserDTO(from: user)
        enqueueInsert(entityType: .user, entityId: user.id, entity: dto, userId: userId)
    }

    func enqueueUserUpdate(_ user: User, userId: String, lastKnownRemoteUpdatedAt: Int64? = nil) {
        let dto = UserDTO(from: user)
        enqueueUpdate(entityType: .user, entityId: user.id, entity: dto, userId: userId, lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt)
    }
}
