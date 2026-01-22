package com.medistock.shared.domain.validation

import com.medistock.shared.db.MedistockDatabase
import kotlinx.datetime.Clock

/**
 * Type of entity for referential integrity checks.
 */
enum class EntityType {
    SITE,
    CATEGORY,
    PACKAGING_TYPE,
    PRODUCT,
    CUSTOMER,
    USER
}

/**
 * Result of checking entity usage.
 */
sealed class DeletionCheck {
    /**
     * Entity is not used anywhere and can be safely deleted (hard delete).
     */
    data object CanDelete : DeletionCheck()

    /**
     * Entity is used and should be deactivated instead of deleted (soft delete).
     */
    data class MustDeactivate(val usageDetails: UsageDetails) : DeletionCheck()
}

/**
 * Details about where an entity is referenced.
 */
data class UsageDetails(
    val isUsed: Boolean,
    val totalUsageCount: Long,
    val usedIn: List<UsageReference>
) {
    companion object {
        fun notUsed() = UsageDetails(
            isUsed = false,
            totalUsageCount = 0,
            usedIn = emptyList()
        )
    }
}

/**
 * Reference to a specific table where an entity is used.
 */
data class UsageReference(
    val table: String,
    val count: Long
)

/**
 * Result of a deactivate or delete operation.
 */
sealed class EntityOperationResult {
    data object Success : EntityOperationResult()
    data class Error(val message: String) : EntityOperationResult()
}

/**
 * Service for checking referential integrity before delete operations.
 *
 * This service determines whether an entity can be safely deleted (hard delete)
 * or should be deactivated instead (soft delete) based on its usage in other tables.
 *
 * Usage:
 * ```kotlin
 * val service = ReferentialIntegrityService(database)
 *
 * // Check if entity can be deleted
 * when (val check = service.checkDeletion(EntityType.SITE, siteId)) {
 *     is DeletionCheck.CanDelete -> {
 *         // Safe to delete
 *         siteRepository.delete(siteId)
 *     }
 *     is DeletionCheck.MustDeactivate -> {
 *         // Must deactivate instead
 *         service.deactivate(EntityType.SITE, siteId, updatedBy)
 *         // Show user: "This site is used in ${check.usageDetails.totalUsageCount} places"
 *     }
 * }
 * ```
 */
class ReferentialIntegrityService(private val database: MedistockDatabase) {

    private val queries get() = database.medistockQueries

    /**
     * Check whether an entity can be deleted or must be deactivated.
     *
     * @param entityType The type of entity to check
     * @param entityId The ID of the entity
     * @return DeletionCheck indicating if entity can be deleted or must be deactivated
     */
    fun checkDeletion(entityType: EntityType, entityId: String): DeletionCheck {
        val usageDetails = getUsageDetails(entityType, entityId)
        return if (usageDetails.isUsed) {
            DeletionCheck.MustDeactivate(usageDetails)
        } else {
            DeletionCheck.CanDelete
        }
    }

    /**
     * Get detailed information about where an entity is used.
     *
     * @param entityType The type of entity to check
     * @param entityId The ID of the entity
     * @return UsageDetails with list of tables and counts
     */
    fun getUsageDetails(entityType: EntityType, entityId: String): UsageDetails {
        return when (entityType) {
            EntityType.SITE -> getSiteUsageDetails(entityId)
            EntityType.CATEGORY -> getCategoryUsageDetails(entityId)
            EntityType.PACKAGING_TYPE -> getPackagingTypeUsageDetails(entityId)
            EntityType.PRODUCT -> getProductUsageDetails(entityId)
            EntityType.CUSTOMER -> getCustomerUsageDetails(entityId)
            EntityType.USER -> getUserUsageDetails(entityId)
        }
    }

    /**
     * Check if an entity is used anywhere.
     *
     * @param entityType The type of entity to check
     * @param entityId The ID of the entity
     * @return true if the entity is referenced by other records
     */
    fun isUsed(entityType: EntityType, entityId: String): Boolean {
        return getUsageDetails(entityType, entityId).isUsed
    }

    /**
     * Deactivate an entity (soft delete).
     *
     * @param entityType The type of entity to deactivate
     * @param entityId The ID of the entity
     * @param updatedBy The user performing the action
     * @return EntityOperationResult indicating success or failure
     */
    fun deactivate(
        entityType: EntityType,
        entityId: String,
        updatedBy: String
    ): EntityOperationResult {
        val now = Clock.System.now().toEpochMilliseconds()
        return try {
            when (entityType) {
                EntityType.SITE -> queries.deactivateSite(now, updatedBy, entityId)
                EntityType.CATEGORY -> queries.deactivateCategory(now, updatedBy, entityId)
                EntityType.PACKAGING_TYPE -> queries.setPackagingTypeActive(0L, now, updatedBy, entityId)
                EntityType.PRODUCT -> queries.deactivateProduct(now, updatedBy, entityId)
                EntityType.CUSTOMER -> queries.deactivateCustomer(now, updatedBy, entityId)
                EntityType.USER -> queries.deactivateUser(now, updatedBy, entityId)
            }
            EntityOperationResult.Success
        } catch (e: Exception) {
            EntityOperationResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Reactivate a previously deactivated entity.
     *
     * @param entityType The type of entity to reactivate
     * @param entityId The ID of the entity
     * @param updatedBy The user performing the action
     * @return EntityOperationResult indicating success or failure
     */
    fun activate(
        entityType: EntityType,
        entityId: String,
        updatedBy: String
    ): EntityOperationResult {
        val now = Clock.System.now().toEpochMilliseconds()
        return try {
            when (entityType) {
                EntityType.SITE -> queries.activateSite(now, updatedBy, entityId)
                EntityType.CATEGORY -> queries.activateCategory(now, updatedBy, entityId)
                EntityType.PACKAGING_TYPE -> queries.setPackagingTypeActive(1L, now, updatedBy, entityId)
                EntityType.PRODUCT -> queries.activateProduct(now, updatedBy, entityId)
                EntityType.CUSTOMER -> queries.activateCustomer(now, updatedBy, entityId)
                EntityType.USER -> queries.activateUser(now, updatedBy, entityId)
            }
            EntityOperationResult.Success
        } catch (e: Exception) {
            EntityOperationResult.Error(e.message ?: "Unknown error")
        }
    }

    // ===== Private helper methods =====

    private fun getSiteUsageDetails(siteId: String): UsageDetails {
        val result = queries.getSiteUsageDetails(siteId).executeAsOne()

        val usages = mutableListOf<UsageReference>()
        if (result.products_count > 0) usages.add(UsageReference("products", result.products_count))
        if (result.batches_count > 0) usages.add(UsageReference("purchase_batches", result.batches_count))
        if (result.movements_count > 0) usages.add(UsageReference("stock_movements", result.movements_count))
        if (result.transfers_count > 0) usages.add(UsageReference("product_transfers", result.transfers_count))
        if (result.inventories_count > 0) usages.add(UsageReference("inventories", result.inventories_count))
        if (result.customers_count > 0) usages.add(UsageReference("customers", result.customers_count))
        if (result.sales_count > 0) usages.add(UsageReference("sales", result.sales_count))

        val total = usages.sumOf { it.count }
        return UsageDetails(
            isUsed = total > 0,
            totalUsageCount = total,
            usedIn = usages
        )
    }

    private fun getCategoryUsageDetails(categoryId: String): UsageDetails {
        val productsCount = queries.getCategoryUsageDetails(categoryId).executeAsOne()

        val usages = mutableListOf<UsageReference>()
        if (productsCount > 0) usages.add(UsageReference("products", productsCount))

        val total = usages.sumOf { it.count }
        return UsageDetails(
            isUsed = total > 0,
            totalUsageCount = total,
            usedIn = usages
        )
    }

    private fun getPackagingTypeUsageDetails(packagingTypeId: String): UsageDetails {
        val productsCount = queries.getPackagingTypeUsageDetails(packagingTypeId).executeAsOne()

        val usages = mutableListOf<UsageReference>()
        if (productsCount > 0) usages.add(UsageReference("products", productsCount))

        val total = usages.sumOf { it.count }
        return UsageDetails(
            isUsed = total > 0,
            totalUsageCount = total,
            usedIn = usages
        )
    }

    private fun getProductUsageDetails(productId: String): UsageDetails {
        val result = queries.getProductUsageDetails(productId).executeAsOne()

        val usages = mutableListOf<UsageReference>()
        if (result.batches_count > 0) usages.add(UsageReference("purchase_batches", result.batches_count))
        if (result.movements_count > 0) usages.add(UsageReference("stock_movements", result.movements_count))
        if (result.transfers_count > 0) usages.add(UsageReference("product_transfers", result.transfers_count))
        if (result.inventory_items_count > 0) usages.add(UsageReference("inventory_items", result.inventory_items_count))
        if (result.sale_items_count > 0) usages.add(UsageReference("sale_items", result.sale_items_count))

        val total = usages.sumOf { it.count }
        return UsageDetails(
            isUsed = total > 0,
            totalUsageCount = total,
            usedIn = usages
        )
    }

    private fun getCustomerUsageDetails(customerId: String): UsageDetails {
        val salesCount = queries.getCustomerUsageDetails(customerId).executeAsOne()

        val usages = mutableListOf<UsageReference>()
        if (salesCount > 0) usages.add(UsageReference("sales", salesCount))

        val total = usages.sumOf { it.count }
        return UsageDetails(
            isUsed = total > 0,
            totalUsageCount = total,
            usedIn = usages
        )
    }

    private fun getUserUsageDetails(userId: String): UsageDetails {
        val result = queries.getUserUsageDetails(userId).executeAsOne()

        val usages = mutableListOf<UsageReference>()
        if (result.permissions_count > 0) usages.add(UsageReference("user_permissions", result.permissions_count))
        if (result.audit_count > 0) usages.add(UsageReference("audit_history", result.audit_count))

        val total = usages.sumOf { it.count }
        return UsageDetails(
            isUsed = total > 0,
            totalUsageCount = total,
            usedIn = usages
        )
    }
}
