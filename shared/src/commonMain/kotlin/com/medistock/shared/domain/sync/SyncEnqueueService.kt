package com.medistock.shared.domain.sync

import com.medistock.shared.data.dto.*
import com.medistock.shared.data.repository.SyncQueueRepository
import com.medistock.shared.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Service for enqueuing sync operations.
 * This service handles the creation of sync queue items for all entity types.
 */
class SyncEnqueueService(
    private val syncQueueRepository: SyncQueueRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    // ==================== Generic Enqueue Operations ====================

    suspend fun enqueueInsert(
        entityType: String,
        entityId: String,
        payload: String,
        userId: String? = null,
        siteId: String? = null
    ) {
        val item = SyncQueueItem(
            id = generateId("sync"),
            entityType = entityType,
            entityId = entityId,
            operation = SyncOperation.INSERT,
            payload = payload,
            localVersion = 1,
            remoteVersion = null,
            lastKnownRemoteUpdatedAt = null,
            userId = userId,
            siteId = siteId,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )

        // Optimize: if an INSERT already exists for this entity, replace it
        val existing = syncQueueRepository.getLatestPendingForEntity(entityType, entityId)
        if (existing != null && existing.operation == SyncOperation.INSERT) {
            syncQueueRepository.deleteById(existing.id)
        }

        syncQueueRepository.insert(item)
    }

    suspend fun enqueueUpdate(
        entityType: String,
        entityId: String,
        payload: String,
        localVersion: Long,
        lastKnownRemoteUpdatedAt: Long? = null,
        userId: String? = null,
        siteId: String? = null
    ) {
        // Check if there are already pending operations
        val existing = syncQueueRepository.getLatestPendingForEntity(entityType, entityId)

        when (existing?.operation) {
            SyncOperation.INSERT -> {
                // INSERT followed by UPDATE = keep INSERT with new data
                syncQueueRepository.update(existing.copy(payload = payload))
                return
            }
            SyncOperation.UPDATE -> {
                // UPDATE followed by UPDATE = replace with new data
                syncQueueRepository.update(existing.copy(
                    payload = payload,
                    localVersion = localVersion
                ))
                return
            }
            SyncOperation.DELETE -> {
                // DELETE followed by UPDATE = inconsistent, ignore the UPDATE
                return
            }
            null -> {
                // No existing operation, create new one
            }
        }

        val item = SyncQueueItem(
            id = generateId("sync"),
            entityType = entityType,
            entityId = entityId,
            operation = SyncOperation.UPDATE,
            payload = payload,
            localVersion = localVersion,
            lastKnownRemoteUpdatedAt = lastKnownRemoteUpdatedAt,
            userId = userId,
            siteId = siteId,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )

        syncQueueRepository.insert(item)
    }

    suspend fun enqueueDelete(
        entityType: String,
        entityId: String,
        localVersion: Long = 0,
        lastKnownRemoteUpdatedAt: Long? = null,
        userId: String? = null,
        siteId: String? = null
    ) {
        // Remove all previous operations for this entity
        // Because DELETE makes previous INSERT/UPDATE obsolete
        syncQueueRepository.removeObsoleteBeforeDelete(entityType, entityId)

        // Check if there was an unsynced INSERT
        val existing = syncQueueRepository.getLatestPendingForEntity(entityType, entityId)
        if (existing?.operation == SyncOperation.INSERT) {
            // INSERT followed by DELETE = cancel both (entity never synced)
            syncQueueRepository.deleteById(existing.id)
            return
        }

        val item = SyncQueueItem(
            id = generateId("sync"),
            entityType = entityType,
            entityId = entityId,
            operation = SyncOperation.DELETE,
            payload = "{}", // No payload needed for DELETE
            localVersion = localVersion,
            lastKnownRemoteUpdatedAt = lastKnownRemoteUpdatedAt,
            userId = userId,
            siteId = siteId,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )

        syncQueueRepository.insert(item)
    }

    // ==================== Product Operations ====================

    suspend fun enqueueProductInsert(product: Product, userId: String? = null) {
        val dto = ProductDto.fromModel(product)
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "Product",
            entityId = product.id,
            payload = payload,
            userId = userId,
            siteId = product.siteId
        )
    }

    suspend fun enqueueProductUpdate(product: Product, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = ProductDto.fromModel(product)
        val payload = json.encodeToString(dto)
        enqueueUpdate(
            entityType = "Product",
            entityId = product.id,
            payload = payload,
            localVersion = product.updatedAt,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            userId = userId,
            siteId = product.siteId
        )
    }

    suspend fun enqueueProductDelete(productId: String, siteId: String?, remoteUpdatedAt: Long? = null, userId: String? = null) {
        enqueueDelete(
            entityType = "Product",
            entityId = productId,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            userId = userId,
            siteId = siteId
        )
    }

    // ==================== Category Operations ====================

    suspend fun enqueueCategoryInsert(category: Category, userId: String? = null) {
        val dto = CategoryDto.fromModel(category)
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "Category",
            entityId = category.id,
            payload = payload,
            userId = userId,
            siteId = null
        )
    }

    suspend fun enqueueCategoryUpdate(category: Category, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = CategoryDto.fromModel(category)
        val payload = json.encodeToString(dto)
        enqueueUpdate(
            entityType = "Category",
            entityId = category.id,
            payload = payload,
            localVersion = category.updatedAt,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            userId = userId,
            siteId = null
        )
    }

    // ==================== Site Operations ====================

    suspend fun enqueueSiteInsert(site: Site, userId: String? = null) {
        val dto = SiteDto.fromModel(site)
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "Site",
            entityId = site.id,
            payload = payload,
            userId = userId,
            siteId = null
        )
    }

    suspend fun enqueueSiteUpdate(site: Site, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = SiteDto.fromModel(site)
        val payload = json.encodeToString(dto)
        enqueueUpdate(
            entityType = "Site",
            entityId = site.id,
            payload = payload,
            localVersion = site.updatedAt,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            userId = userId,
            siteId = null
        )
    }

    // ==================== Customer Operations ====================

    suspend fun enqueueCustomerInsert(customer: Customer, userId: String? = null) {
        val dto = CustomerDto.fromModel(customer)
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "Customer",
            entityId = customer.id,
            payload = payload,
            userId = userId,
            siteId = customer.siteId
        )
    }

    suspend fun enqueueCustomerUpdate(customer: Customer, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = CustomerDto.fromModel(customer)
        val payload = json.encodeToString(dto)
        enqueueUpdate(
            entityType = "Customer",
            entityId = customer.id,
            payload = payload,
            localVersion = customer.createdAt, // Customer doesn't have updatedAt
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            userId = userId,
            siteId = customer.siteId
        )
    }

    // ==================== PackagingType Operations ====================

    suspend fun enqueuePackagingTypeInsert(packagingType: PackagingType, userId: String? = null) {
        val dto = PackagingTypeDto.fromModel(packagingType)
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "PackagingType",
            entityId = packagingType.id,
            payload = payload,
            userId = userId,
            siteId = null
        )
    }

    suspend fun enqueuePackagingTypeUpdate(packagingType: PackagingType, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = PackagingTypeDto.fromModel(packagingType)
        val payload = json.encodeToString(dto)
        enqueueUpdate(
            entityType = "PackagingType",
            entityId = packagingType.id,
            payload = payload,
            localVersion = packagingType.updatedAt,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            userId = userId,
            siteId = null
        )
    }

    // ==================== User Operations ====================

    suspend fun enqueueUserInsert(user: User, userId: String? = null) {
        val dto = UserDto.fromModel(user)
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "User",
            entityId = user.id,
            payload = payload,
            userId = userId,
            siteId = null
        )
    }

    suspend fun enqueueUserUpdate(user: User, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = UserDto.fromModel(user)
        val payload = json.encodeToString(dto)
        enqueueUpdate(
            entityType = "User",
            entityId = user.id,
            payload = payload,
            localVersion = user.updatedAt,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            userId = userId,
            siteId = null
        )
    }

    // ==================== UserPermission Operations ====================

    suspend fun enqueueUserPermissionInsert(permission: UserPermission, userId: String? = null) {
        val dto = UserPermissionDto.fromModel(permission)
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "UserPermission",
            entityId = permission.id,
            payload = payload,
            userId = userId,
            siteId = null
        )
    }

    suspend fun enqueueUserPermissionDelete(permissionId: String, userId: String? = null) {
        enqueueDelete(
            entityType = "UserPermission",
            entityId = permissionId,
            userId = userId,
            siteId = null
        )
    }

    // ==================== Sale Operations ====================

    suspend fun enqueueSaleInsert(sale: Sale, userId: String? = null) {
        val dto = SaleDto.fromModel(sale)
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "Sale",
            entityId = sale.id,
            payload = payload,
            userId = userId,
            siteId = sale.siteId
        )
    }

    // ==================== PurchaseBatch Operations ====================

    suspend fun enqueuePurchaseBatchInsert(batch: PurchaseBatch, userId: String? = null) {
        val dto = PurchaseBatchDto.fromModel(batch)
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "PurchaseBatch",
            entityId = batch.id,
            payload = payload,
            userId = userId,
            siteId = batch.siteId
        )
    }

    suspend fun enqueuePurchaseBatchUpdate(batch: PurchaseBatch, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = PurchaseBatchDto.fromModel(batch)
        val payload = json.encodeToString(dto)
        enqueueUpdate(
            entityType = "PurchaseBatch",
            entityId = batch.id,
            payload = payload,
            localVersion = batch.updatedAt,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            userId = userId,
            siteId = batch.siteId
        )
    }

    // ==================== StockMovement Operations ====================

    suspend fun enqueueStockMovementInsert(movement: StockMovement, userId: String? = null) {
        val dto = StockMovementDto.fromModel(movement)
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "StockMovement",
            entityId = movement.id,
            payload = payload,
            userId = userId,
            siteId = movement.siteId
        )
    }

    // ==================== ProductTransfer Operations ====================

    suspend fun enqueueProductTransferInsert(transfer: ProductTransfer, userId: String? = null) {
        val dto = ProductTransferDto.fromModel(transfer)
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "ProductTransfer",
            entityId = transfer.id,
            payload = payload,
            userId = userId,
            siteId = transfer.fromSiteId
        )
    }

    suspend fun enqueueProductTransferUpdate(transfer: ProductTransfer, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = ProductTransferDto.fromModel(transfer)
        val payload = json.encodeToString(dto)
        enqueueUpdate(
            entityType = "ProductTransfer",
            entityId = transfer.id,
            payload = payload,
            localVersion = transfer.updatedAt,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            userId = userId,
            siteId = transfer.fromSiteId
        )
    }

    // ==================== Query Methods ====================

    suspend fun getPendingCount(): Long = syncQueueRepository.getPendingCount()

    suspend fun getConflictCount(): Long = syncQueueRepository.getConflictCount()

    suspend fun hasPendingOperations(entityType: String, entityId: String): Boolean {
        return syncQueueRepository.getLatestPendingForEntity(entityType, entityId) != null
    }

    private fun generateId(prefix: String): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val randomSuffix = Random.nextInt(100000, 999999)
        return "$prefix-$now-$randomSuffix"
    }
}
