package com.medistock.shared.domain.sync

/**
 * Sync entity types in dependency order.
 * This order must be respected during sync to maintain referential integrity.
 */
enum class SyncEntity(val tableName: String, val displayNameFr: String) {
    SITES("sites", "sites"),
    PACKAGING_TYPES("packaging_types", "conditionnements"),
    CATEGORIES("categories", "catégories"),
    PRODUCTS("products", "produits"),
    CUSTOMERS("customers", "clients"),
    USERS("app_users", "utilisateurs"),
    USER_PERMISSIONS("user_permissions", "permissions"),
    PURCHASE_BATCHES("purchase_batches", "achats"),
    SALES("sales", "ventes"),
    SALE_ITEMS("sale_items", "articles de vente"),
    STOCK_MOVEMENTS("stock_movements", "mouvements de stock");

    companion object {
        /**
         * Get entities in dependency order for sync operations.
         * Sites must be synced first as other entities depend on site_id.
         */
        fun syncOrder(): List<SyncEntity> = listOf(
            SITES,
            PACKAGING_TYPES,
            CATEGORIES,
            PRODUCTS,
            CUSTOMERS,
            USERS,
            USER_PERMISSIONS,
            PURCHASE_BATCHES,
            SALES,
            SALE_ITEMS,
            STOCK_MOVEMENTS
        )
    }
}

/**
 * Sync direction for operations.
 */
enum class SyncDirection {
    LOCAL_TO_REMOTE,
    REMOTE_TO_LOCAL,
    BIDIRECTIONAL
}

/**
 * Result of a sync operation for a single entity.
 */
sealed class EntitySyncResult {
    data class Success(
        val entity: SyncEntity,
        val itemsProcessed: Int
    ) : EntitySyncResult()

    data class Error(
        val entity: SyncEntity,
        val error: String,
        val exception: Throwable? = null
    ) : EntitySyncResult()

    data class Skipped(
        val entity: SyncEntity,
        val reason: String
    ) : EntitySyncResult()
}

/**
 * Overall sync result containing results for all entities.
 */
data class SyncResult(
    val direction: SyncDirection,
    val entityResults: List<EntitySyncResult>,
    val startTime: Long,
    val endTime: Long
) {
    val isSuccess: Boolean
        get() = entityResults.none { it is EntitySyncResult.Error }

    val errors: List<EntitySyncResult.Error>
        get() = entityResults.filterIsInstance<EntitySyncResult.Error>()

    val successCount: Int
        get() = entityResults.count { it is EntitySyncResult.Success }

    val totalItemsProcessed: Int
        get() = entityResults
            .filterIsInstance<EntitySyncResult.Success>()
            .sumOf { it.itemsProcessed }

    val durationMs: Long
        get() = endTime - startTime
}

/**
 * Progress callback for sync operations.
 */
interface SyncProgressListener {
    /**
     * Called when starting to sync an entity.
     * @param entity The entity being synced
     * @param direction The sync direction
     * @param current Current entity index (0-based)
     * @param total Total number of entities
     */
    fun onEntityStart(entity: SyncEntity, direction: SyncDirection, current: Int, total: Int)

    /**
     * Called when an entity sync completes.
     * @param result The result of the entity sync
     */
    fun onEntityComplete(result: EntitySyncResult)

    /**
     * Called when the entire sync operation completes.
     * @param result The overall sync result
     */
    fun onSyncComplete(result: SyncResult)
}

/**
 * Shared sync orchestration logic.
 * Provides the sync order and progress tracking that both Android and iOS use.
 *
 * The actual sync operations are platform-specific (Room DAOs on Android,
 * SQLDelight on iOS), but this class provides the common orchestration logic.
 *
 * Usage:
 * ```kotlin
 * val orchestrator = SyncOrchestrator()
 * val entities = orchestrator.getEntitiesToSync()
 * for ((index, entity) in entities.withIndex()) {
 *     listener.onEntityStart(entity, direction, index, entities.size)
 *     val result = syncEntity(entity) // Platform-specific
 *     listener.onEntityComplete(result)
 * }
 * ```
 */
class SyncOrchestrator {

    /**
     * Get the list of entities to sync in the correct dependency order.
     */
    fun getEntitiesToSync(): List<SyncEntity> = SyncEntity.syncOrder()

    /**
     * Get progress message for an entity.
     * @param entity The entity being synced
     * @param direction The sync direction
     * @return A localized progress message
     */
    fun getProgressMessage(entity: SyncEntity, direction: SyncDirection): String {
        return when (direction) {
            SyncDirection.LOCAL_TO_REMOTE -> "Synchronisation des ${entity.displayNameFr}..."
            SyncDirection.REMOTE_TO_LOCAL -> "Récupération des ${entity.displayNameFr}..."
            SyncDirection.BIDIRECTIONAL -> "Synchronisation des ${entity.displayNameFr}..."
        }
    }

    /**
     * Get completion message based on direction.
     * @param direction The sync direction
     * @return A localized completion message
     */
    fun getCompletionMessage(direction: SyncDirection): String {
        return when (direction) {
            SyncDirection.LOCAL_TO_REMOTE -> "Synchronisation terminée ✅"
            SyncDirection.REMOTE_TO_LOCAL -> "Récupération terminée ✅"
            SyncDirection.BIDIRECTIONAL -> "Synchronisation complète terminée ✅"
        }
    }

    /**
     * Calculate progress percentage.
     * @param currentIndex Current entity index (0-based)
     * @param totalEntities Total number of entities
     * @return Progress percentage (0-100)
     */
    fun calculateProgress(currentIndex: Int, totalEntities: Int): Int {
        if (totalEntities == 0) return 0
        return ((currentIndex + 1) * 100) / totalEntities
    }

    /**
     * Create a SyncResult from individual entity results.
     * @param direction The sync direction
     * @param entityResults List of entity sync results
     * @param startTime Start timestamp in milliseconds
     * @param endTime End timestamp in milliseconds
     * @return The combined SyncResult
     */
    fun createSyncResult(
        direction: SyncDirection,
        entityResults: List<EntitySyncResult>,
        startTime: Long,
        endTime: Long
    ): SyncResult = SyncResult(
        direction = direction,
        entityResults = entityResults,
        startTime = startTime,
        endTime = endTime
    )

    /**
     * Create a success result for an entity.
     */
    fun successResult(entity: SyncEntity, itemsProcessed: Int): EntitySyncResult =
        EntitySyncResult.Success(entity, itemsProcessed)

    /**
     * Create an error result for an entity.
     */
    fun errorResult(entity: SyncEntity, error: String, exception: Throwable? = null): EntitySyncResult =
        EntitySyncResult.Error(entity, error, exception)

    /**
     * Create a skipped result for an entity.
     */
    fun skippedResult(entity: SyncEntity, reason: String): EntitySyncResult =
        EntitySyncResult.Skipped(entity, reason)
}
