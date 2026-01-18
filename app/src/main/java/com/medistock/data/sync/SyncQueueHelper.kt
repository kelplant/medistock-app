package com.medistock.data.sync

import android.content.Context
import com.medistock.data.dao.SyncQueueDao
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.*
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
 * helper.enqueueInsert(product, remoteUpdatedAt)
 *
 * // Lors d'un update
 * helper.enqueueUpdate(product, localVersion, remoteUpdatedAt)
 *
 * // Lors d'un delete
 * helper.enqueueDelete<Product>(productId, localVersion, remoteUpdatedAt)
 * ```
 */
class SyncQueueHelper(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val syncQueueDao: SyncQueueDao by lazy { database.syncQueueDao() }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    // ==================== Enqueue Operations ====================

    /**
     * Ajoute une opération INSERT à la queue
     */
    suspend inline fun <reified T : Any> enqueueInsert(
        entity: T,
        entityId: String,
        entityType: String = T::class.simpleName ?: "Unknown",
        userId: String? = null,
        siteId: String? = null
    ) {
        val payload = json.encodeToString(entity)

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
    suspend inline fun <reified T : Any> enqueueUpdate(
        entity: T,
        entityId: String,
        localVersion: Long,
        lastKnownRemoteUpdatedAt: Long? = null,
        entityType: String = T::class.simpleName ?: "Unknown",
        userId: String? = null,
        siteId: String? = null
    ) {
        val payload = json.encodeToString(entity)

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

    // ==================== Convenience Methods ====================

    /**
     * Enqueue un Product
     */
    suspend fun enqueueProductInsert(product: Product, userId: String? = null) {
        val dto = SyncMapper.toDto(product)
        enqueueInsert(
            entity = dto,
            entityId = product.id,
            entityType = "Product",
            userId = userId,
            siteId = product.siteId
        )
    }

    suspend fun enqueueProductUpdate(product: Product, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = SyncMapper.toDto(product)
        enqueueUpdate(
            entity = dto,
            entityId = product.id,
            localVersion = product.updatedAt,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            entityType = "Product",
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

    /**
     * Enqueue un Category
     */
    suspend fun enqueueCategoryInsert(category: Category, userId: String? = null) {
        val dto = SyncMapper.toDto(category)
        enqueueInsert(
            entity = dto,
            entityId = category.id,
            entityType = "Category",
            userId = userId,
            siteId = category.siteId
        )
    }

    suspend fun enqueueCategoryUpdate(category: Category, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = SyncMapper.toDto(category)
        enqueueUpdate(
            entity = dto,
            entityId = category.id,
            localVersion = category.updatedAt,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            entityType = "Category",
            userId = userId,
            siteId = category.siteId
        )
    }

    /**
     * Enqueue un Customer
     */
    suspend fun enqueueCustomerInsert(customer: Customer, userId: String? = null) {
        val dto = SyncMapper.toDto(customer)
        enqueueInsert(
            entity = dto,
            entityId = customer.id,
            entityType = "Customer",
            userId = userId,
            siteId = customer.siteId
        )
    }

    suspend fun enqueueCustomerUpdate(customer: Customer, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = SyncMapper.toDto(customer)
        enqueueUpdate(
            entity = dto,
            entityId = customer.id,
            localVersion = customer.updatedAt,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            entityType = "Customer",
            userId = userId,
            siteId = customer.siteId
        )
    }

    /**
     * Enqueue un Site
     */
    suspend fun enqueueSiteInsert(site: Site, userId: String? = null) {
        val dto = SyncMapper.toDto(site)
        enqueueInsert(
            entity = dto,
            entityId = site.id,
            entityType = "Site",
            userId = userId
        )
    }

    suspend fun enqueueSiteUpdate(site: Site, remoteUpdatedAt: Long? = null, userId: String? = null) {
        val dto = SyncMapper.toDto(site)
        enqueueUpdate(
            entity = dto,
            entityId = site.id,
            localVersion = site.updatedAt,
            lastKnownRemoteUpdatedAt = remoteUpdatedAt,
            entityType = "Site",
            userId = userId
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
