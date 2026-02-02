package com.medistock.shared.domain.usecase

import com.medistock.shared.data.repository.*
import com.medistock.shared.domain.model.*
import com.medistock.shared.domain.usecase.common.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Input for a single sale item
 */
data class SaleItemInput(
    val productId: String,
    val quantity: Double,
    val unitPrice: Double,
    val selectedLevel: Int = 1,
    val conversionFactor: Double? = null,
    val batchId: String? = null
)

/**
 * Input data for creating a sale
 */
data class SaleInput(
    val siteId: String,
    val customerName: String,
    val customerId: String? = null,
    val items: List<SaleItemInput>,
    val userId: String
)

/**
 * Details of a batch allocation for a sale item
 */
data class BatchAllocationDetail(
    val batchId: String,
    val quantity: Double,
    val unitCost: Double
)

/**
 * Result for a single processed sale item
 */
data class ProcessedSaleItem(
    val saleItem: SaleItem,
    val allocations: List<BatchAllocationDetail>,
    val stockMovement: StockMovement,
    val averageCost: Double
)

/**
 * Result of a successful sale
 */
data class SaleResult(
    val sale: Sale,
    val items: List<ProcessedSaleItem>,
    val totalCost: Double,
    val totalRevenue: Double,
    val grossProfit: Double
)

/**
 * UseCase for handling sale operations.
 * Encapsulates all business logic for creating sales:
 * - Validates inputs
 * - Creates Sale and SaleItems
 * - Allocates batches using FIFO (oldest first)
 * - Creates SaleBatchAllocations for cost tracking
 * - Creates StockMovements
 * - Handles insufficient stock as WARNING (not blocking per business rules)
 * - Creates audit entry
 */
class SaleUseCase(
    private val saleRepository: SaleRepository,
    private val purchaseBatchRepository: PurchaseBatchRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockRepository: StockRepository,
    private val productRepository: ProductRepository,
    private val packagingTypeRepository: PackagingTypeRepository,
    private val siteRepository: SiteRepository,
    private val auditRepository: AuditRepository,
    private val saleBatchAllocationRepository: SaleBatchAllocationRepository
) {
    private val json = Json { prettyPrint = false; encodeDefaults = true }

    /**
     * Execute a sale operation
     *
     * @param input The sale input data
     * @return UseCaseResult with SaleResult or error
     */
    suspend fun execute(input: SaleInput): UseCaseResult<SaleResult> {
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

        // 3. Verify all products exist and collect product info
        val products = mutableMapOf<String, Product>()
        val packagingTypes = mutableMapOf<String, PackagingType>()
        for (item in input.items) {
            val product = productRepository.getById(item.productId)
                ?: return UseCaseResult.Error(
                    BusinessError.NotFound("Product", item.productId)
                )
            products[item.productId] = product

            // Load packaging type for unit derivation
            val packagingType = packagingTypeRepository.getById(product.packagingTypeId)
            if (packagingType != null) {
                packagingTypes[product.packagingTypeId] = packagingType
            }
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val warnings = mutableListOf<BusinessWarning>()

        // 4. Calculate total amount
        val totalAmount = input.items.sumOf { it.quantity * it.unitPrice }

        // 5. Create Sale
        val sale = Sale(
            id = generateId("sale"),
            customerName = input.customerName,
            customerId = input.customerId,
            date = now,
            totalAmount = totalAmount,
            siteId = input.siteId,
            createdAt = now,
            createdBy = input.userId
        )

        // 6. Process each item with FIFO allocation
        val processedItems = mutableListOf<ProcessedSaleItem>()
        var totalCost = 0.0

        for (itemInput in input.items) {
            val product = products[itemInput.productId]!!
            val packagingType = packagingTypes[product.packagingTypeId]
            val unit = packagingType?.getLevelName(itemInput.selectedLevel) ?: "unit"

            // Compute base_quantity: level 1 equivalent of the display quantity
            val baseQuantity: Double? = if (itemInput.selectedLevel == 2 && itemInput.conversionFactor != null) {
                itemInput.quantity * itemInput.conversionFactor
            } else {
                null
            }

            // Create SaleItem with productName and unit for historical accuracy
            val saleItem = SaleItem(
                id = generateId("saleitem"),
                saleId = sale.id,
                productId = itemInput.productId,
                productName = product.name,
                unit = unit,
                quantity = itemInput.quantity,
                baseQuantity = baseQuantity,
                unitPrice = itemInput.unitPrice,
                totalPrice = itemInput.quantity * itemInput.unitPrice,
                batchId = itemInput.batchId
            )

            // Effective quantity in level 1 (base) units for stock operations
            val effectiveQuantity = baseQuantity ?: itemInput.quantity

            // Batch allocation (always in base units)
            // If a preferred batch is specified, allocate from it first, then FIFO for remainder
            val allocationResult = allocateBatchesFIFO(
                productId = itemInput.productId,
                siteId = input.siteId,
                quantityNeeded = effectiveQuantity,
                saleItemId = saleItem.id,
                userId = input.userId,
                timestamp = now,
                preferredBatchId = itemInput.batchId
            )

            // Add warning if insufficient stock (but continue - negative stock allowed)
            if (allocationResult.insufficientStock) {
                warnings.add(
                    BusinessWarning.InsufficientStock(
                        productId = itemInput.productId,
                        productName = product.name,
                        siteId = input.siteId,
                        requested = effectiveQuantity,
                        available = allocationResult.totalAvailable
                    )
                )
            }

            // Create StockMovement (always, even with insufficient stock)
            val stockMovement = StockMovement(
                id = generateId("movement"),
                productId = itemInput.productId,
                siteId = input.siteId,
                quantity = -effectiveQuantity, // Negative for sale (out), always in base units
                type = MovementType.SALE,
                date = now,
                purchasePriceAtMovement = if (allocationResult.totalAllocated > 0) allocationResult.totalCost / allocationResult.totalAllocated else 0.0,
                sellingPriceAtMovement = itemInput.unitPrice,
                movementType = MovementType.SALE,
                referenceId = sale.id,
                notes = "Vente: ${input.customerName}",
                createdAt = now,
                createdBy = input.userId
            )

            val itemCost = allocationResult.totalCost
            totalCost += itemCost

            processedItems.add(
                ProcessedSaleItem(
                    saleItem = saleItem,
                    allocations = allocationResult.allocations,
                    stockMovement = stockMovement,
                    averageCost = if (allocationResult.totalAllocated > 0) {
                        itemCost / allocationResult.totalAllocated
                    } else {
                        0.0
                    }
                )
            )
        }

        return try {
            // 7. Persist to database
            // Insert sale
            saleRepository.insert(sale)

            // Insert sale items
            for (processed in processedItems) {
                saleRepository.insertSaleItem(processed.saleItem)

                // Insert batch allocations
                for (allocation in processed.allocations) {
                    saleBatchAllocationRepository.insert(
                        SaleBatchAllocation(
                            id = generateId("allocation"),
                            saleItemId = processed.saleItem.id,
                            batchId = allocation.batchId,
                            quantityAllocated = allocation.quantity,
                            purchasePriceAtAllocation = allocation.unitCost,
                            createdAt = now,
                            createdBy = input.userId
                        )
                    )
                }

                // Insert stock movement
                stockMovementRepository.insert(processed.stockMovement)

                // Update current_stock (delta is negative for sales)
                stockRepository.updateStockDelta(
                    productId = processed.stockMovement.productId,
                    siteId = processed.stockMovement.siteId,
                    delta = processed.stockMovement.quantity, // Already negative for OUT
                    lastMovementId = processed.stockMovement.id
                )
            }

            // 8. Create audit entry
            val auditEntry = AuditEntry(
                id = generateId("audit"),
                tableName = "sales",
                recordId = sale.id,
                action = "CREATE",
                oldValues = null,
                newValues = json.encodeToString(sale),
                userId = input.userId,
                timestamp = now
            )
            auditRepository.insert(auditEntry)

            // 9. Return result with warnings
            val grossProfit = totalAmount - totalCost

            UseCaseResult.Success(
                data = SaleResult(
                    sale = sale,
                    items = processedItems,
                    totalCost = totalCost,
                    totalRevenue = totalAmount,
                    grossProfit = grossProfit
                ),
                warnings = warnings
            )
        } catch (e: Exception) {
            UseCaseResult.Error(
                BusinessError.SystemError("Failed to save sale: ${e.message}", e)
            )
        }
    }

    /**
     * FIFO batch allocation result
     */
    private data class FIFOAllocationResult(
        val allocations: List<BatchAllocationDetail>,
        val totalAllocated: Double,
        val totalCost: Double,
        val totalAvailable: Double,
        val insufficientStock: Boolean
    )

    /**
     * Allocate batches using FIFO (First In First Out)
     * If preferredBatchId is provided, allocate from that batch first, then FIFO for remainder.
     * Otherwise, oldest batches (by purchase_date) are consumed first.
     */
    private suspend fun allocateBatchesFIFO(
        productId: String,
        siteId: String,
        quantityNeeded: Double,
        saleItemId: String,
        userId: String,
        timestamp: Long,
        preferredBatchId: String? = null
    ): FIFOAllocationResult {
        // Get available batches ordered by purchase_date ASC (oldest first)
        val allBatches = purchaseBatchRepository.getByProductAndSite(productId, siteId)
            .filter { !it.isExhausted && it.remainingQuantity > 0 }

        // If a preferred batch is specified, put it first, then the rest in FIFO order
        val batches = if (preferredBatchId != null) {
            val preferred = allBatches.filter { it.id == preferredBatchId }
            val rest = allBatches.filter { it.id != preferredBatchId }
            preferred + rest
        } else {
            allBatches
        }

        val totalAvailable = batches.sumOf { it.remainingQuantity }
        val allocations = mutableListOf<BatchAllocationDetail>()
        var remaining = quantityNeeded
        var totalCost = 0.0
        var totalAllocated = 0.0

        for (batch in batches) {
            if (remaining <= 0) break

            val allocated = minOf(batch.remainingQuantity, remaining)
            val newRemaining = batch.remainingQuantity - allocated
            val isExhausted = newRemaining <= 0

            // Update batch in database
            purchaseBatchRepository.updateQuantity(
                id = batch.id,
                remainingQuantity = newRemaining,
                isExhausted = isExhausted,
                updatedAt = timestamp,
                updatedBy = userId
            )

            allocations.add(
                BatchAllocationDetail(
                    batchId = batch.id,
                    quantity = allocated,
                    unitCost = batch.purchasePrice
                )
            )

            totalCost += allocated * batch.purchasePrice
            totalAllocated += allocated
            remaining -= allocated
        }

        return FIFOAllocationResult(
            allocations = allocations,
            totalAllocated = totalAllocated,
            totalCost = totalCost,
            totalAvailable = totalAvailable,
            insufficientStock = remaining > 0
        )
    }

    /**
     * Validate input data
     */
    private fun validateInput(input: SaleInput): BusinessError? {
        if (input.siteId.isBlank()) {
            return BusinessError.ValidationError("siteId", "Site ID is required")
        }
        if (input.customerName.isBlank()) {
            return BusinessError.ValidationError("customerName", "Customer name is required")
        }
        if (input.items.isEmpty()) {
            return BusinessError.ValidationError("items", "At least one item is required")
        }
        if (input.userId.isBlank()) {
            return BusinessError.ValidationError("userId", "User ID is required")
        }

        // Validate each item
        for ((index, item) in input.items.withIndex()) {
            if (item.productId.isBlank()) {
                return BusinessError.ValidationError("items[$index].productId", "Product ID is required")
            }
            if (item.quantity <= 0) {
                return BusinessError.ValidationError("items[$index].quantity", "Quantity must be greater than 0")
            }
            if (item.unitPrice < 0) {
                return BusinessError.ValidationError("items[$index].unitPrice", "Unit price cannot be negative")
            }
            // Level 2 quantity cannot exceed one level 1 unit's capacity
            if (item.selectedLevel == 2 && item.conversionFactor != null) {
                if (item.quantity > item.conversionFactor) {
                    return BusinessError.ValidationError(
                        "items[$index].quantity",
                        "Level 2 quantity (${item.quantity}) exceeds capacity of one level 1 unit (${item.conversionFactor})"
                    )
                }
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
