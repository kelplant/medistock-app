package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.AuditEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuditRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

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
}
