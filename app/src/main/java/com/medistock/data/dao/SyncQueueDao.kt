package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.SyncQueueItem
import com.medistock.data.entities.SyncOperation
import com.medistock.data.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    // ==================== Insertion ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SyncQueueItem>)

    // ==================== Mise à jour ====================

    @Update
    suspend fun update(item: SyncQueueItem)

    @Query("""
        UPDATE sync_queue
        SET status = :status, lastAttemptAt = :attemptAt, lastError = :error, retryCount = retryCount + 1
        WHERE id = :id
    """)
    suspend fun updateStatusWithRetry(id: String, status: SyncStatus, attemptAt: Long, error: String?)

    @Query("UPDATE sync_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: SyncStatus)

    @Query("UPDATE sync_queue SET status = :newStatus WHERE status = :oldStatus")
    suspend fun updateAllStatus(oldStatus: SyncStatus, newStatus: SyncStatus)

    // ==================== Suppression ====================

    @Delete
    suspend fun delete(item: SyncQueueItem)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sync_queue WHERE status = :status")
    suspend fun deleteByStatus(status: SyncStatus)

    @Query("DELETE FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteByEntity(entityType: String, entityId: String)

    /** Supprime les éléments synchronisés avec succès */
    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun deleteSynced()

    // ==================== Requêtes ====================

    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getById(id: String): SyncQueueItem?

    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: SyncStatus): List<SyncQueueItem>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<SyncQueueItem>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingBatch(limit: Int): List<SyncQueueItem>

    @Query("SELECT * FROM sync_queue WHERE status = 'CONFLICT'")
    suspend fun getConflicts(): List<SyncQueueItem>

    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED'")
    suspend fun getFailed(): List<SyncQueueItem>

    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun getByEntity(entityType: String, entityId: String): List<SyncQueueItem>

    @Query("""
        SELECT * FROM sync_queue
        WHERE entityType = :entityType AND entityId = :entityId AND status = 'PENDING'
        ORDER BY createdAt DESC LIMIT 1
    """)
    suspend fun getLatestPendingForEntity(entityType: String, entityId: String): SyncQueueItem?

    // ==================== Flows pour observation UI ====================

    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SyncQueueItem>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun observePending(): Flow<List<SyncQueueItem>>

    @Query("SELECT * FROM sync_queue WHERE status = 'CONFLICT'")
    fun observeConflicts(): Flow<List<SyncQueueItem>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'CONFLICT'")
    fun observeConflictCount(): Flow<Int>

    // ==================== Statistiques ====================

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :status")
    suspend fun getCountByStatus(status: SyncStatus): Int

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("""
        SELECT COUNT(*) FROM sync_queue
        WHERE status = 'FAILED' AND retryCount >= :maxRetries
    """)
    suspend fun getPermFailedCount(maxRetries: Int): Int

    // ==================== Optimisation de la queue ====================

    /**
     * Consolide les opérations multiples sur une même entité.
     * Par exemple: INSERT puis UPDATE => garder seulement INSERT avec données finales
     */
    @Query("""
        DELETE FROM sync_queue
        WHERE entityType = :entityType
        AND entityId = :entityId
        AND status = 'PENDING'
        AND id != :keepId
    """)
    suspend fun consolidateOperations(entityType: String, entityId: String, keepId: String)

    /**
     * Annule les opérations obsolètes:
     * Si DELETE existe, supprimer les INSERT/UPDATE précédents
     */
    @Query("""
        DELETE FROM sync_queue
        WHERE entityType = :entityType
        AND entityId = :entityId
        AND status = 'PENDING'
        AND operation != 'DELETE'
        AND EXISTS (
            SELECT 1 FROM sync_queue
            WHERE entityType = :entityType
            AND entityId = :entityId
            AND operation = 'DELETE'
            AND status = 'PENDING'
        )
    """)
    suspend fun removeObsoleteBeforeDelete(entityType: String, entityId: String)
}
