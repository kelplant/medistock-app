package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.AuditHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(auditHistory: AuditHistory): Long

    @Query("SELECT * FROM audit_history ORDER BY changed_at DESC")
    fun getAll(): Flow<List<AuditHistory>>

    @Query("SELECT * FROM audit_history WHERE entity_type = :entityType ORDER BY changed_at DESC")
    fun getByEntityType(entityType: String): Flow<List<AuditHistory>>

    @Query("SELECT * FROM audit_history WHERE entity_type = :entityType AND entity_id = :entityId ORDER BY changed_at DESC")
    fun getByEntity(entityType: String, entityId: String): Flow<List<AuditHistory>>

    @Query("SELECT * FROM audit_history WHERE changed_by = :username ORDER BY changed_at DESC")
    fun getByUser(username: String): Flow<List<AuditHistory>>

    @Query("SELECT * FROM audit_history WHERE site_id = :siteId ORDER BY changed_at DESC")
    fun getBySite(siteId: String): Flow<List<AuditHistory>>

    @Query("SELECT * FROM audit_history WHERE changed_at >= :startTime AND changed_at <= :endTime ORDER BY changed_at DESC")
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<AuditHistory>>

    @Query("""
        SELECT * FROM audit_history
        WHERE (:entityType IS NULL OR entity_type = :entityType)
        AND (:actionType IS NULL OR action_type = :actionType)
        AND (:username IS NULL OR changed_by = :username)
        AND (:siteId IS NULL OR site_id = :siteId)
        AND (:startTime IS NULL OR changed_at >= :startTime)
        AND (:endTime IS NULL OR changed_at <= :endTime)
        ORDER BY changed_at DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getFiltered(
        entityType: String?,
        actionType: String?,
        username: String?,
        siteId: String?,
        startTime: Long?,
        endTime: Long?,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<List<AuditHistory>>

    @Query("DELETE FROM audit_history WHERE changed_at < :beforeTime")
    fun deleteOlderThan(beforeTime: Long): Int

    @Query("SELECT COUNT(*) FROM audit_history")
    fun getCount(): Flow<Int>
}
