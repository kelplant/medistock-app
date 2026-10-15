package com.medistock.data.sync

import com.medistock.MedistockApplication
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.remote.repository.*
import com.medistock.shared.data.dto.*
import com.medistock.util.DebugConfig
import com.medistock.util.NetworkStatus
import io.github.jan.supabase.postgrest.from
import com.medistock.shared.domain.model.*
import com.medistock.shared.domain.sync.SyncEnqueueService

/**
 * Helper pour ajouter des opérations à la queue de synchronisation.
 * Wrapper Android autour du SyncEnqueueService partagé.
 *
 * Usage:
 * ```kotlin
 * val helper = SyncQueueHelper.getInstance()
 *
 * // Lors d'un insert
 * helper.enqueueProductInsert(product, userId)
 *
 * // Lors d'un update
 * helper.enqueueProductUpdate(product, remoteUpdatedAt, userId)
 *
 * // Lors d'un delete
 * helper.enqueueProductDelete(productId, siteId, remoteUpdatedAt, userId)
 * ```
 */
class SyncQueueHelper private constructor() {

    private val syncEnqueueService: SyncEnqueueService = MedistockApplication.sdk.syncEnqueueService

    companion object {
        @Volatile
        private var INSTANCE: SyncQueueHelper? = null

        fun getInstance(): SyncQueueHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncQueueHelper().also { INSTANCE = it }
            }
        }
    }

    /**
     * Online-first delete, same flow as iOS OnlineFirstHelper.delete:
     * 1. delete on Supabase when online, 2. delete locally, 3. queue the DELETE
     * if the remote delete did not happen. Without this, a local delete is never
     * propagated and the next pull from Supabase brings the row back.
     */
    suspend fun deleteOnlineFirst(
        context: android.content.Context,
        table: String,
        entityType: String,
        entityId: String,
        userId: String? = null,
        localDelete: suspend () -> Unit
    ) {
        var deletedOnline = false
        if (NetworkStatus.isOnline(context) && SupabaseClientProvider.isConfigured(context)) {
            try {
                SupabaseClientProvider.client.from(table).delete {
                    filter { eq("id", entityId) }
                }
                deletedOnline = true
            } catch (e: Exception) {
                DebugConfig.w("SyncQueueHelper", "Remote delete failed for $table/$entityId: ${e.message}")
            }
        }

        localDelete()

        if (!deletedOnline) {
            syncEnqueueService.enqueueDelete(entityType = entityType, entityId = entityId, userId = userId)
        }
    }

    /**
     * Online-first save, same flow as iOS OnlineFirstHelper.save:
     * 1. upsert on Supabase when online, 2. save locally, 3. queue the change
     * if the remote upsert did not happen. Without this, a local create/edit only
     * reaches Supabase at the next full sync.
     */
    private suspend fun saveOnlineFirst(
        context: android.content.Context,
        table: String,
        remoteUpsert: suspend () -> Unit,
        localSave: suspend () -> Unit,
        enqueue: suspend () -> Unit
    ) {
        var savedOnline = false
        if (NetworkStatus.isOnline(context) && SupabaseClientProvider.isConfigured(context)) {
            try {
                remoteUpsert()
                savedOnline = true
            } catch (e: Exception) {
                DebugConfig.w("SyncQueueHelper", "Remote save failed for $table: ${e.message}")
            }
        }

        localSave()

        if (!savedOnline) enqueue()
    }

    suspend fun saveSite(context: android.content.Context, site: Site, isNew: Boolean, userId: String?, localSave: suspend () -> Unit) =
        saveOnlineFirst(context, "sites", { SiteSupabaseRepository().upsertSite(SiteDto.fromModel(site)) }, localSave) {
            if (isNew) enqueueSiteInsert(site, userId) else enqueueSiteUpdate(site, userId = userId)
        }

    suspend fun saveCategory(context: android.content.Context, category: Category, isNew: Boolean, userId: String?, localSave: suspend () -> Unit) =
        saveOnlineFirst(context, "categories", { CategorySupabaseRepository().upsertCategory(CategoryDto.fromModel(category)) }, localSave) {
            if (isNew) enqueueCategoryInsert(category, userId) else enqueueCategoryUpdate(category, userId = userId)
        }

    suspend fun saveCustomer(context: android.content.Context, customer: Customer, isNew: Boolean, userId: String?, localSave: suspend () -> Unit) =
        saveOnlineFirst(context, "customers", { CustomerSupabaseRepository().upsertCustomer(CustomerDto.fromModel(customer)) }, localSave) {
            if (isNew) enqueueCustomerInsert(customer, userId) else enqueueCustomerUpdate(customer, userId = userId)
        }

    suspend fun saveProduct(context: android.content.Context, product: Product, isNew: Boolean, userId: String?, localSave: suspend () -> Unit) =
        saveOnlineFirst(context, "products", { ProductSupabaseRepository().upsertProduct(ProductDto.fromModel(product)) }, localSave) {
            if (isNew) enqueueProductInsert(product, userId) else enqueueProductUpdate(product, userId = userId)
        }

    suspend fun savePackagingType(context: android.content.Context, packagingType: PackagingType, isNew: Boolean, userId: String?, localSave: suspend () -> Unit) =
        saveOnlineFirst(context, "packaging_types", { PackagingTypeSupabaseRepository().upsertPackagingType(PackagingTypeDto.fromModel(packagingType)) }, localSave) {
            if (isNew) enqueuePackagingTypeInsert(packagingType, userId) else enqueuePackagingTypeUpdate(packagingType, userId = userId)
        }

    /** The sync queue has no supplier support: offline edits are pushed by the next full sync. */
    suspend fun saveSupplier(context: android.content.Context, supplier: Supplier, localSave: suspend () -> Unit) =
        saveOnlineFirst(context, "suppliers", { SupplierSupabaseRepository().upsertSupplier(SupplierDto.fromModel(supplier)) }, localSave) {}

    // ==================== Product Operations ====================

    suspend fun enqueueProductInsert(product: Product, userId: String? = null) {
        syncEnqueueService.enqueueProductInsert(product, userId)
    }

    suspend fun enqueueProductUpdate(product: Product, remoteUpdatedAt: Long? = null, userId: String? = null) {
        syncEnqueueService.enqueueProductUpdate(product, remoteUpdatedAt, userId)
    }

    suspend fun enqueueProductDelete(productId: String, siteId: String?, remoteUpdatedAt: Long? = null, userId: String? = null) {
        syncEnqueueService.enqueueProductDelete(productId, siteId, remoteUpdatedAt, userId)
    }

    // ==================== Category Operations ====================

    suspend fun enqueueCategoryInsert(category: Category, userId: String? = null) {
        syncEnqueueService.enqueueCategoryInsert(category, userId)
    }

    suspend fun enqueueCategoryUpdate(category: Category, remoteUpdatedAt: Long? = null, userId: String? = null) {
        syncEnqueueService.enqueueCategoryUpdate(category, remoteUpdatedAt, userId)
    }

    // ==================== Customer Operations ====================

    suspend fun enqueueCustomerInsert(customer: Customer, userId: String? = null) {
        syncEnqueueService.enqueueCustomerInsert(customer, userId)
    }

    suspend fun enqueueCustomerUpdate(customer: Customer, remoteUpdatedAt: Long? = null, userId: String? = null) {
        syncEnqueueService.enqueueCustomerUpdate(customer, remoteUpdatedAt, userId)
    }

    // ==================== Site Operations ====================

    suspend fun enqueueSiteInsert(site: Site, userId: String? = null) {
        syncEnqueueService.enqueueSiteInsert(site, userId)
    }

    suspend fun enqueueSiteUpdate(site: Site, remoteUpdatedAt: Long? = null, userId: String? = null) {
        syncEnqueueService.enqueueSiteUpdate(site, remoteUpdatedAt, userId)
    }

    // ==================== PackagingType Operations ====================

    suspend fun enqueuePackagingTypeInsert(packagingType: PackagingType, userId: String? = null) {
        syncEnqueueService.enqueuePackagingTypeInsert(packagingType, userId)
    }

    suspend fun enqueuePackagingTypeUpdate(packagingType: PackagingType, remoteUpdatedAt: Long? = null, userId: String? = null) {
        syncEnqueueService.enqueuePackagingTypeUpdate(packagingType, remoteUpdatedAt, userId)
    }

    // ==================== User Operations ====================

    suspend fun enqueueUserInsert(user: User, userId: String? = null) {
        syncEnqueueService.enqueueUserInsert(user, userId)
    }

    suspend fun enqueueUserUpdate(user: User, remoteUpdatedAt: Long? = null, userId: String? = null) {
        syncEnqueueService.enqueueUserUpdate(user, remoteUpdatedAt, userId)
    }

    // ==================== UserPermission Operations ====================

    suspend fun enqueueUserPermissionInsert(permission: UserPermission, userId: String? = null) {
        syncEnqueueService.enqueueUserPermissionInsert(permission, userId)
    }

    suspend fun enqueueUserPermissionDelete(permissionId: String, userId: String? = null) {
        syncEnqueueService.enqueueUserPermissionDelete(permissionId, userId)
    }

    // ==================== Sale Operations ====================

    suspend fun enqueueSaleInsert(sale: Sale, userId: String? = null) {
        syncEnqueueService.enqueueSaleInsert(sale, userId)
    }

    // ==================== PurchaseBatch Operations ====================

    suspend fun enqueuePurchaseBatchInsert(batch: PurchaseBatch, userId: String? = null) {
        syncEnqueueService.enqueuePurchaseBatchInsert(batch, userId)
    }

    suspend fun enqueuePurchaseBatchUpdate(batch: PurchaseBatch, remoteUpdatedAt: Long? = null, userId: String? = null) {
        syncEnqueueService.enqueuePurchaseBatchUpdate(batch, remoteUpdatedAt, userId)
    }

    // ==================== StockMovement Operations ====================

    suspend fun enqueueStockMovementInsert(movement: StockMovement, userId: String? = null) {
        syncEnqueueService.enqueueStockMovementInsert(movement, userId)
    }

    // ==================== ProductTransfer Operations ====================

    suspend fun enqueueProductTransferInsert(transfer: ProductTransfer, userId: String? = null) {
        syncEnqueueService.enqueueProductTransferInsert(transfer, userId)
    }

    suspend fun enqueueProductTransferUpdate(transfer: ProductTransfer, remoteUpdatedAt: Long? = null, userId: String? = null) {
        syncEnqueueService.enqueueProductTransferUpdate(transfer, remoteUpdatedAt, userId)
    }

    // ==================== Query Methods ====================

    suspend fun getPendingCount(): Long = syncEnqueueService.getPendingCount()

    suspend fun getConflictCount(): Long = syncEnqueueService.getConflictCount()

    suspend fun hasPendingOperations(entityType: String, entityId: String): Boolean {
        return syncEnqueueService.hasPendingOperations(entityType, entityId)
    }
}
