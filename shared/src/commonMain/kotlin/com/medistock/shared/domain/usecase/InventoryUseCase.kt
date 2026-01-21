package com.medistock.shared.domain.usecase

import com.medistock.shared.data.repository.*
import com.medistock.shared.domain.model.*
import com.medistock.shared.domain.usecase.common.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Input for a single inventory count
 */
data class InventoryCountInput(
    val productId: String,
    val countedQuantity: Double,
    val reason: String? = null
)

/**
 * Input data for creating an inventory session
 */
data class InventoryInput(
    val siteId: String,
    val counts: List<InventoryCountInput>,
    val notes: String? = null,
    val userId: String
)

/**
 * Result for a single inventory count
 */
data class InventoryCountResult(
    val productId: String,
    val productName: String,
    val theoreticalQuantity: Double,
    val countedQuantity: Double,
    val discrepancy: Double,
    val adjustment: StockMovement?
)

/**
 * Inventory entry record (stored in database)
 */
@Serializable
data class InventoryEntry(
    val id: String,
    val inventoryId: String,
    val productId: String,
    val theoreticalQuantity: Double,
    val countedQuantity: Double,
    val discrepancy: Double,
    val reason: String? = null,
    val createdAt: Long
)

/**
 * Result of a successful inventory
 */
data class InventoryResult(
    val inventory: Inventory,
    val counts: List<InventoryCountResult>,
    val totalDiscrepancies: Int,
    val positiveAdjustments: Int,
    val negativeAdjustments: Int
)

/**
 * UseCase for handling inventory operations.
 * Encapsulates all business logic for creating inventories:
 * - Validates inputs
 * - Compares counted vs theoretical stock
 * - Creates stock adjustments if discrepancy != 0
 * - Creates StockMovements for adjustments
 * - Creates audit entry
 */
class InventoryUseCase(
    private val inventoryRepository: InventoryRepository,
    private val purchaseBatchRepository: PurchaseBatchRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val productRepository: ProductRepository,
    private val siteRepository: SiteRepository,
    private val auditRepository: AuditRepository,
    private val stockRepository: StockRepository
) {
    private val json = Json { prettyPrint = false; encodeDefaults = true }

    /**
     * Execute an inventory operation
     *
     * @param input The inventory input data
     * @return UseCaseResult with InventoryResult or error
     */
    suspend fun execute(input: InventoryInput): UseCaseResult<InventoryResult> {
        // 1. Validate inputs
        val validationError = validateInput(input)
        if (validationError != null) {
            return UseCaseResult.Error(validationError)
        }

        // 2. Verify site exists
        val site = siteRepository.getById(input.siteId)
            ?: return UseCaseResult.Error(
                BusinessError.NotFound("Site", input.siteId)
            )

        // 3. Verify all products exist
        val products = mutableMapOf<String, Product>()
        for (count in input.counts) {
            val product = productRepository.getById(count.productId)
                ?: return UseCaseResult.Error(
                    BusinessError.NotFound("Product", count.productId)
                )
            products[count.productId] = product
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val warnings = mutableListOf<BusinessWarning>()

        // 4. Create Inventory session
        val inventory = Inventory(
            id = generateId("inventory"),
            siteId = input.siteId,
            status = "completed",
            startedAt = now,
            completedAt = now,
            notes = input.notes,
            createdBy = input.userId
        )

        // 5. Process each count
        val countResults = mutableListOf<InventoryCountResult>()
        val adjustments = mutableListOf<StockMovement>()
        var positiveAdjustments = 0
        var negativeAdjustments = 0

        for (countInput in input.counts) {
            val product = products[countInput.productId]!!

            // Get theoretical stock
            val currentStock = stockRepository.getCurrentStockByProductAndSite(
                countInput.productId,
                input.siteId
            )
            val theoreticalQuantity = currentStock?.totalStock ?: 0.0

            // Calculate discrepancy
            val discrepancy = countInput.countedQuantity - theoreticalQuantity

            // Create adjustment if needed
            var adjustment: StockMovement? = null
            if (discrepancy != 0.0) {
                val movementType = if (discrepancy > 0) {
                    MovementType.INVENTORY // Surplus found
                } else {
                    MovementType.INVENTORY // Shortage found
                }

                adjustment = StockMovement(
                    id = generateId("movement"),
                    productId = countInput.productId,
                    siteId = input.siteId,
                    quantity = discrepancy, // Positive or negative
                    movementType = movementType,
                    referenceId = inventory.id,
                    notes = countInput.reason ?: "Ajustement inventaire: ${if (discrepancy > 0) "surplus" else "manque"}",
                    createdAt = now,
                    createdBy = input.userId
                )
                adjustments.add(adjustment)

                if (discrepancy > 0) {
                    positiveAdjustments++

                    // Create a new batch to account for the surplus
                    val surplusBatch = PurchaseBatch(
                        id = generateId("batch"),
                        productId = countInput.productId,
                        siteId = input.siteId,
                        batchNumber = "INV-${inventory.id.takeLast(6)}",
                        purchaseDate = now,
                        initialQuantity = discrepancy,
                        remainingQuantity = discrepancy,
                        purchasePrice = 0.0, // Unknown cost for inventory adjustment
                        supplierName = "Ajustement inventaire",
                        expiryDate = null,
                        isExhausted = false,
                        createdAt = now,
                        updatedAt = now,
                        createdBy = input.userId,
                        updatedBy = input.userId
                    )
                    purchaseBatchRepository.insert(surplusBatch)
                } else {
                    negativeAdjustments++

                    // Need to deduct from existing batches using FIFO
                    deductFromBatchesFIFO(
                        productId = countInput.productId,
                        siteId = input.siteId,
                        quantity = -discrepancy, // Make positive
                        userId = input.userId,
                        timestamp = now
                    )
                }

                // Add warning for low stock after adjustment
                val finalStock = countInput.countedQuantity
                product.minStock?.let { minStock ->
                    if (finalStock < minStock && minStock > 0) {
                        warnings.add(
                            BusinessWarning.LowStock(
                                productId = countInput.productId,
                                productName = product.name,
                                siteId = input.siteId,
                                currentStock = finalStock,
                                minStock = minStock
                            )
                        )
                    }
                }
            }

            countResults.add(
                InventoryCountResult(
                    productId = countInput.productId,
                    productName = product.name,
                    theoreticalQuantity = theoreticalQuantity,
                    countedQuantity = countInput.countedQuantity,
                    discrepancy = discrepancy,
                    adjustment = adjustment
                )
            )
        }

        return try {
            // 6. Persist to database
            inventoryRepository.insert(inventory)

            // Insert stock movements for adjustments
            for (adjustment in adjustments) {
                stockMovementRepository.insert(adjustment)
            }

            // 7. Create audit entry
            val auditEntry = AuditEntry(
                id = generateId("audit"),
                tableName = "inventories",
                recordId = inventory.id,
                action = "CREATE",
                oldValues = null,
                newValues = json.encodeToString(inventory),
                userId = input.userId,
                timestamp = now
            )
            auditRepository.insert(auditEntry)

            // 8. Return result
            UseCaseResult.Success(
                data = InventoryResult(
                    inventory = inventory,
                    counts = countResults,
                    totalDiscrepancies = countResults.count { it.discrepancy != 0.0 },
                    positiveAdjustments = positiveAdjustments,
                    negativeAdjustments = negativeAdjustments
                ),
                warnings = warnings
            )
        } catch (e: Exception) {
            UseCaseResult.Error(
                BusinessError.SystemError("Failed to save inventory: ${e.message}", e)
            )
        }
    }

    /**
     * Deduct quantity from batches using FIFO
     */
    private suspend fun deductFromBatchesFIFO(
        productId: String,
        siteId: String,
        quantity: Double,
        userId: String,
        timestamp: Long
    ) {
        val batches = purchaseBatchRepository.getByProductAndSite(productId, siteId)
            .filter { !it.isExhausted && it.remainingQuantity > 0 }

        var remaining = quantity

        for (batch in batches) {
            if (remaining <= 0) break

            val deducted = minOf(batch.remainingQuantity, remaining)
            val newRemaining = batch.remainingQuantity - deducted
            val isExhausted = newRemaining <= 0

            purchaseBatchRepository.updateQuantity(
                id = batch.id,
                remainingQuantity = newRemaining,
                isExhausted = isExhausted,
                updatedAt = timestamp,
                updatedBy = userId
            )

            remaining -= deducted
        }
    }

    /**
     * Validate input data
     */
    private fun validateInput(input: InventoryInput): BusinessError? {
        if (input.siteId.isBlank()) {
            return BusinessError.ValidationError("siteId", "Site ID is required")
        }
        if (input.counts.isEmpty()) {
            return BusinessError.ValidationError("counts", "At least one count is required")
        }
        if (input.userId.isBlank()) {
            return BusinessError.ValidationError("userId", "User ID is required")
        }

        // Validate each count
        for ((index, count) in input.counts.withIndex()) {
            if (count.productId.isBlank()) {
                return BusinessError.ValidationError("counts[$index].productId", "Product ID is required")
            }
            if (count.countedQuantity < 0) {
                return BusinessError.ValidationError("counts[$index].countedQuantity", "Counted quantity cannot be negative")
            }
        }

        return null
    }

    private fun generateId(prefix: String): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val randomSuffix = Random.nextInt(100000, 999999)
        return "$prefix-$now-$randomSuffix"
    }
}
