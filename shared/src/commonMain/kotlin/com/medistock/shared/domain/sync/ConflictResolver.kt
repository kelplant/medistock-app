package com.medistock.shared.domain.sync

import kotlinx.serialization.json.*

/**
 * Resolves sync conflicts according to entity type.
 *
 * Strategy by entity type:
 * - Product, Category, Site, PackagingType: SERVER_WINS (reference data)
 * - Sale, SaleItem, SaleBatchAllocation: CLIENT_WINS (offline transactions valid)
 * - StockMovement, Customer, ProductTransfer: MERGE (both versions valid)
 * - PurchaseBatch: SERVER_WINS (sensitive data)
 * - Inventory: ASK_USER (independent counts)
 * - User, UserPermission: SERVER_WINS (security)
 */
class ConflictResolver {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /**
     * Get resolution strategy for an entity type.
     */
    fun getStrategy(entityType: String): ConflictResolution {
        return when (entityType.lowercase()) {
            // Reference data - Server wins
            "product", "products",
            "category", "categories",
            "site", "sites",
            "packagingtype", "packaging_types" -> ConflictResolution.REMOTE_WINS

            // Sales transactions - Client wins (offline sales are valid)
            "sale", "sales",
            "saleitem", "sale_items",
            "salebatchallocation", "sale_batch_allocations" -> ConflictResolution.LOCAL_WINS

            // Stock movements - Merge (both are valid)
            "stockmovement", "stock_movements" -> ConflictResolution.MERGE

            // Purchase batches - Server wins (sensitive)
            "purchasebatch", "purchase_batches" -> ConflictResolution.REMOTE_WINS

            // Inventory - Ask user
            "inventory", "inventories" -> ConflictResolution.ASK_USER

            // Customers - Merge
            "customer", "customers" -> ConflictResolution.MERGE

            // Security - Server wins
            "user", "users", "app_users",
            "userpermission", "user_permissions" -> ConflictResolution.REMOTE_WINS

            // Transfers - Merge
            "producttransfer", "product_transfers" -> ConflictResolution.MERGE

            // Default - Server wins
            else -> ConflictResolution.REMOTE_WINS
        }
    }

    /**
     * Detect if there's a conflict between local and remote versions.
     * A conflict exists when the remote version has been updated since we last synced.
     *
     * @param lastKnownRemoteUpdatedAt The updatedAt timestamp we recorded when we last synced
     * @param remoteUpdatedAt The current updatedAt timestamp on the remote
     * @return true if there's a conflict
     */
    fun detectConflict(
        lastKnownRemoteUpdatedAt: Long?,
        remoteUpdatedAt: Long?
    ): Boolean {
        if (lastKnownRemoteUpdatedAt == null) return false
        if (remoteUpdatedAt == null) return false
        return remoteUpdatedAt > lastKnownRemoteUpdatedAt
    }

    /**
     * Resolve a conflict according to entity strategy.
     *
     * @param entityType The type of entity (e.g., "Product", "Sale")
     * @param localPayload JSON string of the local entity
     * @param remotePayload JSON string of the remote entity (may be null if deleted)
     * @param localUpdatedAt Local update timestamp
     * @param remoteUpdatedAt Remote update timestamp
     * @return Resolution result with strategy and optionally merged payload
     */
    fun resolve(
        entityType: String,
        localPayload: String,
        remotePayload: String?,
        localUpdatedAt: Long,
        remoteUpdatedAt: Long?
    ): ConflictResolutionResult {
        val strategy = getStrategy(entityType)

        return when (strategy) {
            ConflictResolution.LOCAL_WINS -> ConflictResolutionResult(
                resolution = ConflictResolution.LOCAL_WINS,
                mergedPayload = localPayload,
                message = "Local version kept (valid offline transaction)"
            )

            ConflictResolution.REMOTE_WINS -> ConflictResolutionResult(
                resolution = ConflictResolution.REMOTE_WINS,
                mergedPayload = remotePayload,
                message = "Server version kept (reference data)"
            )

            ConflictResolution.MERGE -> ConflictResolutionResult(
                resolution = ConflictResolution.MERGE,
                mergedPayload = mergePayloads(entityType, localPayload, remotePayload),
                message = "Versions merged"
            )

            ConflictResolution.ASK_USER -> ConflictResolutionResult(
                resolution = ConflictResolution.ASK_USER,
                message = "Conflict detected - user intervention required"
            )

            ConflictResolution.KEEP_BOTH -> ConflictResolutionResult(
                resolution = ConflictResolution.KEEP_BOTH,
                mergedPayload = localPayload,
                message = "Both versions will be kept"
            )
        }
    }

    /**
     * Merge two JSON payloads.
     * Takes all remote fields as base, then overrides with local fields
     * (except system fields: createdAt, updatedAt, createdBy, updatedBy, id).
     *
     * @param entityType The type of entity being merged
     * @param localJson JSON string of local entity
     * @param remoteJson JSON string of remote entity
     * @return Merged JSON string
     */
    fun mergePayloads(entityType: String, localJson: String, remoteJson: String?): String {
        if (remoteJson == null) return localJson

        return try {
            val local = json.parseToJsonElement(localJson).jsonObject
            val remote = json.parseToJsonElement(remoteJson).jsonObject

            val merged = buildJsonObject {
                // Start with all remote fields
                remote.forEach { (key, value) -> put(key, value) }

                // Override with local fields (except system fields)
                val systemFields = setOf(
                    "createdAt", "updatedAt", "created_at", "updated_at",
                    "createdBy", "updatedBy", "created_by", "updated_by",
                    "id"
                )
                local.forEach { (key, value) ->
                    if (key !in systemFields) {
                        put(key, value)
                    }
                }

                // Take the most recent updatedAt
                val localUpdated = local["updated_at"]?.jsonPrimitive?.longOrNull
                    ?: local["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L
                val remoteUpdated = remote["updated_at"]?.jsonPrimitive?.longOrNull
                    ?: remote["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L
                put("updated_at", JsonPrimitive(maxOf(localUpdated, remoteUpdated)))
            }

            merged.toString()
        } catch (e: Exception) {
            // Fallback to local on parse error
            localJson
        }
    }

    /**
     * Compute field differences between two payloads.
     * Useful for showing users what changed in a conflict.
     *
     * @param localJson JSON string of local entity
     * @param remoteJson JSON string of remote entity
     * @return List of field differences
     */
    fun computeFieldDifferences(localJson: String, remoteJson: String?): List<FieldDifference> {
        if (remoteJson == null) return emptyList()

        return try {
            val local = json.parseToJsonElement(localJson).jsonObject
            val remote = json.parseToJsonElement(remoteJson).jsonObject

            val allKeys = (local.keys + remote.keys).toSet()
            allKeys.mapNotNull { key ->
                val localValue = local[key]?.toString()
                val remoteValue = remote[key]?.toString()
                if (localValue != remoteValue) {
                    FieldDifference(key, localValue, remoteValue)
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Check if an operation can be synced safely without conflicts.
     *
     * @param operation The sync operation type ("INSERT", "UPDATE", "DELETE")
     * @param lastKnownRemoteUpdatedAt Last known remote timestamp
     * @param remoteUpdatedAt Current remote timestamp
     * @return true if safe to sync without conflict
     */
    fun canSyncSafely(
        operation: String,
        lastKnownRemoteUpdatedAt: Long?,
        remoteUpdatedAt: Long?
    ): Boolean {
        return when (operation.uppercase()) {
            "INSERT" -> true // Inserts never conflict
            "UPDATE" -> !detectConflict(lastKnownRemoteUpdatedAt, remoteUpdatedAt)
            "DELETE" -> !detectConflict(lastKnownRemoteUpdatedAt, remoteUpdatedAt)
            else -> false
        }
    }

    /**
     * Create a UserConflict object for user intervention.
     */
    fun createUserConflict(
        queueItemId: String,
        entityType: String,
        entityId: String,
        localPayload: String,
        remotePayload: String?,
        localUpdatedAt: Long,
        remoteUpdatedAt: Long
    ): UserConflict {
        return UserConflict(
            queueItemId = queueItemId,
            entityType = entityType,
            entityId = entityId,
            localPayload = localPayload,
            remotePayload = remotePayload,
            localUpdatedAt = localUpdatedAt,
            remoteUpdatedAt = remoteUpdatedAt,
            fieldDifferences = computeFieldDifferences(localPayload, remotePayload)
        )
    }
}
