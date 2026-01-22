package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.SyncQueueItem
import com.medistock.shared.domain.model.SyncOperation
import com.medistock.shared.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList

class SyncQueueRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    // ==================== Insert ====================

    suspend fun insert(item: SyncQueueItem) = withContext(Dispatchers.Default) {
        queries.insertSyncQueueItem(
            id = item.id,
            entity_type = item.entityType,
            entity_id = item.entityId,
            operation = item.operation.name,
            payload = item.payload,
            local_version = item.localVersion,
            remote_version = item.remoteVersion,
            last_known_remote_updated_at = item.lastKnownRemoteUpdatedAt,
            status = item.status.name.lowercase(),
            retry_count = item.retryCount.toLong(),
            last_error = item.lastError,
            last_attempt_at = item.lastAttemptAt,
            created_at = item.createdAt,
            user_id = item.userId,
            site_id = item.siteId
        )
    }

    // ==================== Update ====================

    suspend fun update(item: SyncQueueItem) = withContext(Dispatchers.Default) {
        queries.updateSyncQueueItem(
            entity_type = item.entityType,
            entity_id = item.entityId,
            operation = item.operation.name,
            payload = item.payload,
            local_version = item.localVersion,
            remote_version = item.remoteVersion,
            last_known_remote_updated_at = item.lastKnownRemoteUpdatedAt,
            status = item.status.name.lowercase(),
            retry_count = item.retryCount.toLong(),
            last_error = item.lastError,
            last_attempt_at = item.lastAttemptAt,
            user_id = item.userId,
            site_id = item.siteId,
            id = item.id
        )
    }

    suspend fun updateStatus(id: String, status: SyncStatus) = withContext(Dispatchers.Default) {
        queries.updateSyncQueueStatus(status.name.lowercase(), id)
    }

    suspend fun updateStatusWithRetry(
        id: String,
        status: SyncStatus,
        attemptAt: Long,
        error: String?
    ) = withContext(Dispatchers.Default) {
        queries.updateSyncQueueStatusWithRetry(
            status = status.name.lowercase(),
            last_attempt_at = attemptAt,
            last_error = error,
            id = id
        )
    }

    suspend fun updateAllStatus(oldStatus: SyncStatus, newStatus: SyncStatus) = withContext(Dispatchers.Default) {
        queries.updateAllSyncQueueStatus(
            newStatus = newStatus.name.lowercase(),
            oldStatus = oldStatus.name.lowercase()
        )
    }

    // ==================== Delete ====================

    suspend fun deleteById(id: String) = withContext(Dispatchers.Default) {
        queries.deleteSyncQueueById(id)
    }

    suspend fun deleteByStatus(status: SyncStatus) = withContext(Dispatchers.Default) {
        queries.deleteSyncQueueByStatus(status.name.lowercase())
    }

    suspend fun deleteByEntity(entityType: String, entityId: String) = withContext(Dispatchers.Default) {
        queries.deleteSyncQueueByEntity(entityType, entityId)
    }

    suspend fun deleteSynced() = withContext(Dispatchers.Default) {
        queries.deleteSyncedItems()
    }

    // ==================== Select ====================

    suspend fun getById(id: String): SyncQueueItem? = withContext(Dispatchers.Default) {
        queries.getSyncQueueById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun getByStatus(status: SyncStatus): List<SyncQueueItem> = withContext(Dispatchers.Default) {
        queries.getSyncQueueByStatus(status.name.lowercase()).executeAsList().map { it.toModel() }
    }

    suspend fun getPending(): List<SyncQueueItem> = withContext(Dispatchers.Default) {
        queries.getPendingSyncItems().executeAsList().map { it.toModel() }
    }

    suspend fun getPendingBatch(limit: Int): List<SyncQueueItem> = withContext(Dispatchers.Default) {
        queries.getPendingSyncBatch(limit.toLong()).executeAsList().map { it.toModel() }
    }

    suspend fun getConflicts(): List<SyncQueueItem> = withContext(Dispatchers.Default) {
        queries.getConflictSyncItems().executeAsList().map { it.toModel() }
    }

    suspend fun getFailed(): List<SyncQueueItem> = withContext(Dispatchers.Default) {
        queries.getFailedSyncItems().executeAsList().map { it.toModel() }
    }

    suspend fun getByEntity(entityType: String, entityId: String): List<SyncQueueItem> = withContext(Dispatchers.Default) {
        queries.getSyncQueueByEntity(entityType, entityId).executeAsList().map { it.toModel() }
    }

    suspend fun getLatestPendingForEntity(entityType: String, entityId: String): SyncQueueItem? = withContext(Dispatchers.Default) {
        queries.getLatestPendingForEntity(entityType, entityId).executeAsOneOrNull()?.toModel()
    }

    // ==================== Counts ====================

    suspend fun getTotalCount(): Long = withContext(Dispatchers.Default) {
        queries.getTotalSyncQueueCount().executeAsOne()
    }

    suspend fun getCountByStatus(status: SyncStatus): Long = withContext(Dispatchers.Default) {
        queries.getSyncQueueCountByStatus(status.name.lowercase()).executeAsOne()
    }

    suspend fun getPendingCount(): Long = withContext(Dispatchers.Default) {
        queries.getPendingSyncCount().executeAsOne()
    }

    suspend fun getConflictCount(): Long = withContext(Dispatchers.Default) {
        queries.getConflictSyncCount().executeAsOne()
    }

    suspend fun getPermFailedCount(maxRetries: Int): Long = withContext(Dispatchers.Default) {
        queries.getPermFailedCount(maxRetries.toLong()).executeAsOne()
    }

    // ==================== Flows for UI observation ====================

    fun observePending(): Flow<List<SyncQueueItem>> {
        return queries.getPendingSyncItems()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    fun observeConflicts(): Flow<List<SyncQueueItem>> {
        return queries.getConflictSyncItems()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    // ==================== Optimization ====================

    suspend fun consolidateOperations(entityType: String, entityId: String, keepId: String) = withContext(Dispatchers.Default) {
        queries.consolidateSyncOperations(entityType, entityId, keepId)
    }

    suspend fun removeObsoleteBeforeDelete(entityType: String, entityId: String) = withContext(Dispatchers.Default) {
        queries.removeObsoleteBeforeDelete(entityType, entityId, entityType, entityId)
    }

    // ==================== Mapper ====================

    private fun com.medistock.shared.db.Sync_queue.toModel(): SyncQueueItem {
        return SyncQueueItem(
            id = id,
            entityType = entity_type,
            entityId = entity_id,
            operation = SyncOperation.fromString(operation),
            payload = payload,
            localVersion = local_version,
            remoteVersion = remote_version,
            lastKnownRemoteUpdatedAt = last_known_remote_updated_at,
            status = SyncStatus.fromString(status),
            retryCount = retry_count.toInt(),
            lastError = last_error,
            lastAttemptAt = last_attempt_at,
            createdAt = created_at,
            userId = user_id,
            siteId = site_id
        )
    }
}
