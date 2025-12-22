package com.medistock.util

import android.content.Context
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.AuditHistory
import java.util.concurrent.Executors

/**
 * Utility class for logging audit history entries
 * Uses a single-threaded executor to avoid blocking UI while still being synchronous
 */
class AuditLogger(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val auditHistoryDao = database.auditHistoryDao()
    private val executor = Executors.newSingleThreadExecutor()

    companion object {
        @Volatile
        private var INSTANCE: AuditLogger? = null

        fun getInstance(context: Context): AuditLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuditLogger(context.applicationContext).also { INSTANCE = it }
            }
        }

        // Action types
        const val ACTION_INSERT = "INSERT"
        const val ACTION_UPDATE = "UPDATE"
        const val ACTION_DELETE = "DELETE"
    }

    /**
     * Log an INSERT operation (new entity created)
     */
    fun logInsert(
        entityType: String,
        entityId: String,
        newValues: Map<String, String>,
        username: String,
        siteId: String? = null,
        description: String? = null
    ) {
        executor.execute {
            // Log a single entry for INSERT with all new values
            val auditEntry = AuditHistory(
                entityType = entityType,
                entityId = entityId,
                actionType = ACTION_INSERT,
                fieldName = null,
                oldValue = null,
                newValue = newValues.entries.joinToString(", ") { "${it.key}=${it.value}" },
                changedBy = username,
                siteId = siteId,
                description = description
            )
            auditHistoryDao.insert(auditEntry)
        }
    }

    /**
     * Log an UPDATE operation (entity modified)
     */
    fun logUpdate(
        entityType: String,
        entityId: String,
        changes: Map<String, Pair<String?, String?>>, // Map of fieldName to (oldValue, newValue)
        username: String,
        siteId: String? = null,
        description: String? = null
    ) {
        executor.execute {
            // Log each field change as a separate entry
            changes.forEach { (fieldName, values) ->
                val (oldValue, newValue) = values
                val auditEntry = AuditHistory(
                    entityType = entityType,
                    entityId = entityId,
                    actionType = ACTION_UPDATE,
                    fieldName = fieldName,
                    oldValue = oldValue,
                    newValue = newValue,
                    changedBy = username,
                    siteId = siteId,
                    description = description
                )
                auditHistoryDao.insert(auditEntry)
            }
        }
    }

    /**
     * Log a DELETE operation (entity removed)
     */
    fun logDelete(
        entityType: String,
        entityId: String,
        oldValues: Map<String, String>,
        username: String,
        siteId: String? = null,
        description: String? = null
    ) {
        executor.execute {
            // Log a single entry for DELETE with all old values
            val auditEntry = AuditHistory(
                entityType = entityType,
                entityId = entityId,
                actionType = ACTION_DELETE,
                fieldName = null,
                oldValue = oldValues.entries.joinToString(", ") { "${it.key}=${it.value}" },
                newValue = null,
                changedBy = username,
                siteId = siteId,
                description = description
            )
            auditHistoryDao.insert(auditEntry)
        }
    }

    /**
     * Log a simple action without detailed field tracking
     */
    fun logAction(
        entityType: String,
        entityId: String,
        actionType: String,
        description: String,
        username: String,
        siteId: String? = null
    ) {
        executor.execute {
            val auditEntry = AuditHistory(
                entityType = entityType,
                entityId = entityId,
                actionType = actionType,
                fieldName = null,
                oldValue = null,
                newValue = null,
                changedBy = username,
                siteId = siteId,
                description = description
            )
            auditHistoryDao.insert(auditEntry)
        }
    }

    /**
     * Cleanup old audit entries (optional maintenance)
     */
    fun cleanupOldEntries(daysToKeep: Int = 90) {
        executor.execute {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            auditHistoryDao.deleteOlderThan(cutoffTime)
        }
    }
}
