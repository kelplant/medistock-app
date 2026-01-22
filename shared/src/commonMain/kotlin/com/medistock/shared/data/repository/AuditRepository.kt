package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.AuditEntry
import com.medistock.shared.domain.model.AuditHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuditRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    // AuditEntry methods (original)
    suspend fun getAll(limit: Long = 100): List<AuditEntry> = withContext(Dispatchers.Default) {
        queries.getAuditHistory(limit).executeAsList().map { it.toModel() }
    }

    suspend fun getByTable(tableName: String, limit: Long = 100): List<AuditEntry> = withContext(Dispatchers.Default) {
        queries.getAuditHistoryByTable(tableName, limit).executeAsList().map { it.toModel() }
    }

    suspend fun insert(entry: AuditEntry) = withContext(Dispatchers.Default) {
        queries.insertAuditEntry(
            id = entry.id,
            table_name = entry.tableName,
            record_id = entry.recordId,
            action = entry.action,
            old_values = entry.oldValues,
            new_values = entry.newValues,
            user_id = entry.userId,
            timestamp = entry.timestamp
        )
    }

    // AuditHistory methods (for UI compatibility with Room entity structure)
    suspend fun getAllHistory(): List<AuditHistory> = withContext(Dispatchers.Default) {
        queries.getAllAuditHistory().executeAsList().map { it.toAuditHistory() }
    }

    suspend fun getHistoryByEntityType(entityType: String, limit: Long = 100): List<AuditHistory> = withContext(Dispatchers.Default) {
        queries.getAuditHistoryByTable(entityType, limit).executeAsList().map { it.toAuditHistory() }
    }

    suspend fun getHistoryByUser(username: String, limit: Long = 100): List<AuditHistory> = withContext(Dispatchers.Default) {
        queries.getAuditHistoryByUser(username, limit).executeAsList().map { it.toAuditHistory() }
    }

    suspend fun getHistoryByDateRange(startTime: Long, endTime: Long, limit: Long = 100): List<AuditHistory> = withContext(Dispatchers.Default) {
        queries.getAuditHistoryByDateRange(startTime, endTime, limit).executeAsList().map { it.toAuditHistory() }
    }

    suspend fun getHistoryCount(): Long = withContext(Dispatchers.Default) {
        queries.getAuditHistoryCount().executeAsOne()
    }

    fun observeAllHistory(): Flow<List<AuditHistory>> {
        return queries.getAllAuditHistory()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toAuditHistory() } }
    }

    private fun com.medistock.shared.db.Audit_history.toModel(): AuditEntry {
        return AuditEntry(
            id = id,
            tableName = table_name,
            recordId = record_id,
            action = action,
            oldValues = old_values,
            newValues = new_values,
            userId = user_id,
            timestamp = timestamp
        )
    }

    /**
     * Maps SQLDelight audit_history to AuditHistory model for UI compatibility.
     * Field mapping:
     * - table_name → entityType
     * - record_id → entityId
     * - action → actionType
     * - old_values → oldValue
     * - new_values → newValue
     * - user_id → changedBy
     * - timestamp → changedAt
     */
    private fun com.medistock.shared.db.Audit_history.toAuditHistory(): AuditHistory {
        return AuditHistory(
            id = id,
            entityType = table_name,
            entityId = record_id,
            actionType = action,
            fieldName = null, // Not available in SQLDelight schema
            oldValue = old_values,
            newValue = new_values,
            changedBy = user_id,
            changedAt = timestamp,
            siteId = null, // Not available in SQLDelight schema
            description = null // Not available in SQLDelight schema
        )
    }
}
