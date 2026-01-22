import Foundation
import shared

/// Helper for enqueuing sync operations with smart deduplication
/// Uses the shared SyncEnqueueService for cross-platform consistency
class SyncQueueHelper {
    static let shared = SyncQueueHelper()

    private let store = SyncQueueStore.shared
    private let encoder = JSONEncoder()

    private var syncEnqueueService: SyncEnqueueService {
        SDKProvider.shared.syncEnqueueService
    }

    private init() {}

    // MARK: - Generic Enqueue Methods using Shared Service

    /// Enqueue an INSERT operation using shared service
    func enqueueInsertAsync<T: Encodable>(
        entityType: EntityType,
        entityId: String,
        entity: T,
        userId: String?,
        siteId: String? = nil
    ) async {
        do {
            guard let payload = try? encoder.encode(entity),
                  let payloadString = String(data: payload, encoding: .utf8) else {
                debugLog("SyncQueueHelper", "Error encoding payload for INSERT")
                return
            }

            try await syncEnqueueService.enqueueInsert(
                entityType: entityType.rawValue,
                entityId: entityId,
                payload: payloadString,
                userId: userId,
                siteId: siteId
            )
            await store.loadFromRepository()
            debugLog("SyncQueueHelper", "Enqueued INSERT for \(entityType.rawValue) \(entityId)")
        } catch {
            debugLog("SyncQueueHelper", "Error enqueuing INSERT: \(error)")
        }
    }

    /// Enqueue an UPDATE operation using shared service
    func enqueueUpdateAsync<T: Encodable>(
        entityType: EntityType,
        entityId: String,
        entity: T,
        userId: String?,
        siteId: String? = nil,
        localVersion: Int64 = 1,
        lastKnownRemoteUpdatedAt: Int64? = nil
    ) async {
        do {
            guard let payload = try? encoder.encode(entity),
                  let payloadString = String(data: payload, encoding: .utf8) else {
                debugLog("SyncQueueHelper", "Error encoding payload for UPDATE")
                return
            }

            try await syncEnqueueService.enqueueUpdate(
                entityType: entityType.rawValue,
                entityId: entityId,
                payload: payloadString,
                localVersion: localVersion,
                lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt.map { KotlinLong(value: $0) },
                userId: userId,
                siteId: siteId
            )
            await store.loadFromRepository()
            debugLog("SyncQueueHelper", "Enqueued UPDATE for \(entityType.rawValue) \(entityId)")
        } catch {
            debugLog("SyncQueueHelper", "Error enqueuing UPDATE: \(error)")
        }
    }

    /// Enqueue a DELETE operation using shared service
    func enqueueDeleteAsync(
        entityType: EntityType,
        entityId: String,
        userId: String?,
        siteId: String? = nil,
        localVersion: Int64 = 0,
        lastKnownRemoteUpdatedAt: Int64? = nil
    ) async {
        do {
            try await syncEnqueueService.enqueueDelete(
                entityType: entityType.rawValue,
                entityId: entityId,
                localVersion: localVersion,
                lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt.map { KotlinLong(value: $0) },
                userId: userId,
                siteId: siteId
            )
            await store.loadFromRepository()
            debugLog("SyncQueueHelper", "Enqueued DELETE for \(entityType.rawValue) \(entityId)")
        } catch {
            debugLog("SyncQueueHelper", "Error enqueuing DELETE: \(error)")
        }
    }

    // MARK: - Legacy Synchronous Methods (for backward compatibility)

    /// Enqueue an INSERT operation
    func enqueueInsert<T: Encodable>(
        entityType: EntityType,
        entityId: String,
        entity: T,
        userId: String,
        siteId: String? = nil
    ) {
        Task {
            await enqueueInsertAsync(
                entityType: entityType,
                entityId: entityId,
                entity: entity,
                userId: userId,
                siteId: siteId
            )
        }
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
        Task {
            await enqueueUpdateAsync(
                entityType: entityType,
                entityId: entityId,
                entity: entity,
                userId: userId,
                siteId: siteId,
                lastKnownRemoteUpdatedAt: lastKnownRemoteUpdatedAt
            )
        }
    }

    /// Enqueue a DELETE operation with cleanup of obsolete operations
    func enqueueDelete(
        entityType: EntityType,
        entityId: String,
        userId: String,
        siteId: String? = nil
    ) {
        Task {
            await enqueueDeleteAsync(
                entityType: entityType,
                entityId: entityId,
                userId: userId,
                siteId: siteId
            )
        }
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

    func enqueueCategoryInsert(_ category: shared.Category, userId: String) {
        let dto = CategoryDTO(from: category)
        enqueueInsert(entityType: .category, entityId: category.id, entity: dto, userId: userId)
    }

    func enqueueCategoryUpdate(_ category: shared.Category, userId: String, lastKnownRemoteUpdatedAt: Int64? = nil) {
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

    // MARK: - Query Methods (using shared service)

    func getPendingCount() async -> Int {
        do {
            return Int(try await syncEnqueueService.getPendingCount())
        } catch {
            return 0
        }
    }

    func getConflictCount() async -> Int {
        do {
            return Int(try await syncEnqueueService.getConflictCount())
        } catch {
            return 0
        }
    }

    func hasPendingOperations(entityType: EntityType, entityId: String) async -> Bool {
        do {
            let result = try await syncEnqueueService.hasPendingOperations(
                entityType: entityType.rawValue,
                entityId: entityId
            )
            return result.boolValue
        } catch {
            return false
        }
    }
}
