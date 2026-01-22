package com.medistock.shared.domain.audit

import com.medistock.shared.data.repository.AuditRepository
import com.medistock.shared.domain.model.AuditEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Service for recording audit trail of data changes.
 * Replaces Room triggers with explicit audit logging.
 */
class AuditService(private val auditRepository: AuditRepository) {

    /**
     * Records an INSERT action in the audit trail.
     */
    suspend fun recordInsert(
        tableName: String,
        recordId: String,
        newValues: String,
        userId: String
    ) {
        val entry = AuditEntry(
            id = generateId("audit"),
            tableName = tableName,
            recordId = recordId,
            action = ACTION_INSERT,
            oldValues = null,
            newValues = newValues,
            userId = userId,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        auditRepository.insert(entry)
    }

    /**
     * Records an UPDATE action in the audit trail.
     */
    suspend fun recordUpdate(
        tableName: String,
        recordId: String,
        oldValues: String?,
        newValues: String,
        userId: String
    ) {
        val entry = AuditEntry(
            id = generateId("audit"),
            tableName = tableName,
            recordId = recordId,
            action = ACTION_UPDATE,
            oldValues = oldValues,
            newValues = newValues,
            userId = userId,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        auditRepository.insert(entry)
    }

    /**
     * Records a DELETE action in the audit trail.
     */
    suspend fun recordDelete(
        tableName: String,
        recordId: String,
        oldValues: String?,
        userId: String
    ) {
        val entry = AuditEntry(
            id = generateId("audit"),
            tableName = tableName,
            recordId = recordId,
            action = ACTION_DELETE,
            oldValues = oldValues,
            newValues = null,
            userId = userId,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        auditRepository.insert(entry)
    }

    /**
     * Wraps an insert operation with automatic audit logging.
     * @param tableName The table being modified
     * @param recordId The ID of the record being inserted
     * @param newValuesJson JSON representation of the new entity
     * @param userId The user performing the action
     * @param operation The insert operation to execute
     * @return The result of the operation
     */
    suspend fun <R> withInsertAudit(
        tableName: String,
        recordId: String,
        newValuesJson: String,
        userId: String,
        operation: suspend () -> R
    ): R {
        val result = operation()
        recordInsert(
            tableName = tableName,
            recordId = recordId,
            newValues = newValuesJson,
            userId = userId
        )
        return result
    }

    /**
     * Wraps an update operation with automatic audit logging.
     * @param tableName The table being modified
     * @param recordId The ID of the record being updated
     * @param oldValuesJson JSON representation of the old entity (null if not available)
     * @param newValuesJson JSON representation of the new entity
     * @param userId The user performing the action
     * @param operation The update operation to execute
     * @return The result of the operation
     */
    suspend fun <R> withUpdateAudit(
        tableName: String,
        recordId: String,
        oldValuesJson: String?,
        newValuesJson: String,
        userId: String,
        operation: suspend () -> R
    ): R {
        val result = operation()
        recordUpdate(
            tableName = tableName,
            recordId = recordId,
            oldValues = oldValuesJson,
            newValues = newValuesJson,
            userId = userId
        )
        return result
    }

    /**
     * Wraps a delete operation with automatic audit logging.
     * @param tableName The table being modified
     * @param recordId The ID of the record being deleted
     * @param oldValuesJson JSON representation of the deleted entity (null if not available)
     * @param userId The user performing the action
     * @param operation The delete operation to execute
     * @return The result of the operation
     */
    suspend fun <R> withDeleteAudit(
        tableName: String,
        recordId: String,
        oldValuesJson: String?,
        userId: String,
        operation: suspend () -> R
    ): R {
        val result = operation()
        recordDelete(
            tableName = tableName,
            recordId = recordId,
            oldValues = oldValuesJson,
            userId = userId
        )
        return result
    }

    private fun generateId(prefix: String): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val randomSuffix = Random.nextInt(100000, 999999)
        return "$prefix-$now-$randomSuffix"
    }

    companion object {
        const val ACTION_INSERT = "INSERT"
        const val ACTION_UPDATE = "UPDATE"
        const val ACTION_DELETE = "DELETE"

        // Table names for audit
        const val TABLE_SITES = "sites"
        const val TABLE_CATEGORIES = "categories"
        const val TABLE_PRODUCTS = "products"
        const val TABLE_CUSTOMERS = "customers"
        const val TABLE_PACKAGING_TYPES = "packaging_types"
        const val TABLE_USERS = "users"
        const val TABLE_USER_PERMISSIONS = "user_permissions"
        const val TABLE_PURCHASE_BATCHES = "purchase_batches"
        const val TABLE_SALES = "sales"
        const val TABLE_SALE_ITEMS = "sale_items"
        const val TABLE_STOCK_MOVEMENTS = "stock_movements"
        const val TABLE_PRODUCT_TRANSFERS = "product_transfers"
        const val TABLE_INVENTORIES = "inventories"
        const val TABLE_INVENTORY_ITEMS = "inventory_items"

        /**
         * JSON serializer for converting entities to JSON strings for audit logging.
         */
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /**
         * Helper function to serialize an entity to JSON for audit logging.
         */
        inline fun <reified T> toJson(entity: T): String = json.encodeToString(entity)
    }
}
