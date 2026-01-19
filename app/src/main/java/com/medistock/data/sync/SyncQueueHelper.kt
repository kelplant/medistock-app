package com.medistock.data.sync

import android.content.Context
import com.medistock.data.dao.SyncQueueDao
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.*
import com.medistock.data.sync.SyncMapper.toDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Helper pour ajouter des opérations à la queue de synchronisation.
 *
 * Cette classe doit être utilisée par les repositories audités pour
 * enregistrer automatiquement les changements locaux dans la queue de sync.
 *
 * Usage:
 * ```kotlin
 * val helper = SyncQueueHelper(context)
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
class SyncQueueHelper(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val syncQueueDao: SyncQueueDao = database.syncQueueDao()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    // ==================== Generic Enqueue Operations ====================

    /**
     * Ajoute une opération INSERT à la queue
     */
    suspend fun enqueueInsert(
        entityType: String,
        entityId: String,
        payload: String,
        userId: String? = null,
        siteId: String? = null
    ) {
        val item = SyncQueueItem(
            entityType = entityType,
            entityId = entityId,
            operation = SyncOperation.INSERT,
            payload = payload,
            localVersion = 1,
            remoteVersion = null,
            lastKnownRemoteUpdatedAt = null,
            userId = userId,
            siteId = siteId
        )

        // Optimiser: si un INSERT existe déjà pour cette entité, le remplacer
        val existing = syncQueueDao.getLatestPendingForEntity(entityType, entityId)
        if (existing != null && existing.operation == SyncOperation.INSERT) {
            syncQueueDao.deleteById(existing.id)
        }

        syncQueueDao.insert(item)
    }

    /**
     * Ajoute une opération UPDATE à la queue
     */
    suspend fun enqueueUpdate(
        entityType: String,
        entityId: String,
        payload: String,
        localVersion: Long,
        lastKnownRemoteUpdatedAt: Long? = null,
        userId: String? = null,
        siteId: String? = null
    ) {
        // Vérifier s'il y a déjà des opérations en attente
        val existing = syncQueueDao.getLatestPendingForEntity(entityType, entityId)

        when (existing?.operation) {
            SyncOperation.INSERT -> {
                // INSERT suivi de UPDATE = garder INSERT avec les nouvelles données
                syncQueueDao.update(existing.copy(payload = payload))
                return
            }
            SyncOperation.UPDATE -> {
                // UPDATE suivi de UPDATE = remplacer avec les nouvelles données
                syncQueueDao.update(existing.copy(
                    payload = payload,
                    localVersion = localVersion
                ))
                return
            }
            SyncOperation.DELETE -> {
                // DELETE suivi de UPDATE = incohérent, ignorer l'UPDATE
                return
            }
            null -> {
                // Pas d'opération existante, créer une nouvelle
            }
        }

        val item = SyncQueueItem(
            entityType = entityType,
            entityId = entityId,
            operation = SyncOperation.UPDATE,
            payload = payload,
            localVersion = localVersion,
            lastKnownRemoteUpdatedAt = lastKnownRemoteUpdatedAt,
            userId = userId,
            siteId = siteId
        )

        syncQueueDao.insert(item)
    }

    /**
     * Ajoute une opération DELETE à la queue
     */
    suspend fun enqueueDelete(
        entityType: String,
        entityId: String,
        localVersion: Long = 0,
        lastKnownRemoteUpdatedAt: Long? = null,
        userId: String? = null,
        siteId: String? = null
    ) {
        // Supprimer toutes les opérations précédentes pour cette entité
        // Car DELETE rend obsolètes les INSERT/UPDATE précédents
        syncQueueDao.removeObsoleteBeforeDelete(entityType, entityId)

        // Vérifier s'il y avait un INSERT non synchronisé
        val existing = syncQueueDao.getLatestPendingForEntity(entityType, entityId)
        if (existing?.operation == SyncOperation.INSERT) {
            // INSERT suivi de DELETE = annuler les deux (entité jamais synchro)
            syncQueueDao.deleteById(existing.id)
            return
        }

        val item = SyncQueueItem(
            entityType = entityType,
            entityId = entityId,
            operation = SyncOperation.DELETE,
            payload = "{}", // Pas besoin de payload pour DELETE
            localVersion = localVersion,
            lastKnownRemoteUpdatedAt = lastKnownRemoteUpdatedAt,
            userId = userId,
            siteId = siteId
        )

        syncQueueDao.insert(item)
    }

    // ==================== Product Operations ====================

    /**
     * Enqueue un Product INSERT
     */
    suspend fun enqueueProductInsert(product: Product, userId: String? = null) {
        val dto = product.toDto()
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "Product",
            entityId = product.id,
            payload = payload,
            userId = userId,
            siteId = product.siteId
        )
    }

    /**
     * Enqueue un Product UPDATE
     */
    suspend fun enqueueProductUpdate(product: Product, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = product.toDto()
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

    /**
     * Enqueue un Product DELETE
     */
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

    /**
     * Enqueue un Category INSERT
     */
    suspend fun enqueueCategoryInsert(category: Category, userId: String? = null) {
        val dto = category.toDto()
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "Category",
            entityId = category.id,
            payload = payload,
            userId = userId,
            siteId = null // Category n'a pas de siteId
        )
    }

    /**
     * Enqueue un Category UPDATE
     */
    suspend fun enqueueCategoryUpdate(category: Category, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = category.toDto()
        val payload = json.encodeToString(dto)
        enqueueUpdate(
            entityType = "Category",
            entityId = category.id,
            payload = payload,
            localVersion = category.updatedAt,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            userId = userId,
            siteId = null // Category n'a pas de siteId
        )
    }

    // ==================== Customer Operations ====================

    /**
     * Enqueue un Customer INSERT
     */
    suspend fun enqueueCustomerInsert(customer: Customer, userId: String? = null) {
        val dto = customer.toDto()
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "Customer",
            entityId = customer.id,
            payload = payload,
            userId = userId,
            siteId = customer.siteId
        )
    }

    /**
     * Enqueue un Customer UPDATE
     */
    suspend fun enqueueCustomerUpdate(customer: Customer, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = customer.toDto()
        val payload = json.encodeToString(dto)
        enqueueUpdate(
            entityType = "Customer",
            entityId = customer.id,
            payload = payload,
            localVersion = customer.createdAt, // Customer n'a pas de updatedAt
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            userId = userId,
            siteId = customer.siteId
        )
    }

    // ==================== Site Operations ====================

    /**
     * Enqueue un Site INSERT
     */
    suspend fun enqueueSiteInsert(site: Site, userId: String? = null) {
        val dto = site.toDto()
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "Site",
            entityId = site.id,
            payload = payload,
            userId = userId,
            siteId = null
        )
    }

    /**
     * Enqueue un Site UPDATE
     */
    suspend fun enqueueSiteUpdate(site: Site, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = site.toDto()
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

    // ==================== PackagingType Operations ====================

    /**
     * Enqueue un PackagingType INSERT
     */
    suspend fun enqueuePackagingTypeInsert(packagingType: PackagingType, userId: String? = null) {
        val dto = packagingType.toDto()
        val payload = json.encodeToString(dto)
        enqueueInsert(
            entityType = "PackagingType",
            entityId = packagingType.id,
            payload = payload,
            userId = userId,
            siteId = null
        )
    }

    /**
     * Enqueue un PackagingType UPDATE
     */
    suspend fun enqueuePackagingTypeUpdate(packagingType: PackagingType, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = packagingType.toDto()
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

    // ==================== Query Methods ====================

    /**
     * Retourne le nombre d'opérations en attente
     */
    suspend fun getPendingCount(): Int = syncQueueDao.getPendingCount()

    /**
     * Retourne le nombre de conflits
     */
    suspend fun getConflictCount(): Int = syncQueueDao.getCountByStatus(SyncStatus.CONFLICT)

    /**
     * Vérifie si une entité a des opérations en attente
     */
    suspend fun hasPendingOperations(entityType: String, entityId: String): Boolean {
        return syncQueueDao.getLatestPendingForEntity(entityType, entityId) != null
    }
}
