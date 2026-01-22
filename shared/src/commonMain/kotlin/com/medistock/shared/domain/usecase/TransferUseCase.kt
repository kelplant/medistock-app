package com.medistock.shared.domain.usecase

import com.medistock.shared.data.repository.*
import com.medistock.shared.domain.model.*
import com.medistock.shared.domain.usecase.common.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Input data for creating a transfer
 */
data class TransferInput(
    val productId: String,
    val fromSiteId: String,
    val toSiteId: String,
    val quantity: Double,
    val notes: String? = null,
    val userId: String
)

/**
 * Details of a batch transferred
 */
data class TransferredBatch(
    val sourceBatchId: String,
    val destinationBatchId: String,
    val quantity: Double,
    val purchasePrice: Double
)

/**
 * Result of a successful transfer
 */
data class TransferResult(
    val transfer: ProductTransfer,
    val transferredBatches: List<TransferredBatch>,
    val sourceMovement: StockMovement,
    val destinationMovement: StockMovement,
    val averageCost: Double
)

/**
 * UseCase for handling transfer operations.
 * Encapsulates all business logic for creating transfers:
 * - Validates inputs (different source/destination sites)
 * - Transfers batches using FIFO
 * - Creates new batches on destination site (preserving purchase date)
 * - Creates StockMovements on both sites
 * - Handles insufficient stock as WARNING (not blocking per business rules)
 * - Creates audit entry
 */
class TransferUseCase(
    private val productTransferRepository: ProductTransferRepository,
    private val purchaseBatchRepository: PurchaseBatchRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val productRepository: ProductRepository,
    private val siteRepository: SiteRepository,
    private val auditRepository: AuditRepository
) {
    private val json = Json { prettyPrint = false; encodeDefaults = true }

    /**
     * Execute a transfer operation
     *
     * @param input The transfer input data
     * @return UseCaseResult with TransferResult or error
     */
    suspend fun execute(input: TransferInput): UseCaseResult<TransferResult> {
        // 1. Validate inputs
        val validationError = validateInput(input)
        if (validationError != null) {
            return UseCaseResult.Error(validationError)
        }

        // 2. Check source != destination (this is a blocking error)
        if (input.fromSiteId == input.toSiteId) {
            return UseCaseResult.Error(
                BusinessError.SameSiteTransfer(input.fromSiteId)
            )
        }

        // 3. Verify product exists
        val product = productRepository.getById(input.productId)
            ?: return UseCaseResult.Error(
                BusinessError.NotFound("Product", input.productId)
            )

        // 4. Verify both sites exist
        val fromSite = siteRepository.getById(input.fromSiteId)
            ?: return UseCaseResult.Error(
                BusinessError.NotFound("Site", input.fromSiteId)
            )

        val toSite = siteRepository.getById(input.toSiteId)
            ?: return UseCaseResult.Error(
                BusinessError.NotFound("Site", input.toSiteId)
            )

        val now = Clock.System.now().toEpochMilliseconds()
        val warnings = mutableListOf<BusinessWarning>()

        // 5. FIFO batch transfer
        val transferResult = transferBatchesFIFO(
            productId = input.productId,
            fromSiteId = input.fromSiteId,
            toSiteId = input.toSiteId,
            quantityNeeded = input.quantity,
            userId = input.userId,
            timestamp = now
        )

        // Add warning if insufficient stock (but continue - negative stock allowed)
        if (transferResult.insufficientStock) {
            warnings.add(
                BusinessWarning.InsufficientStock(
                    productId = input.productId,
                    productName = product.name,
                    siteId = input.fromSiteId,
                    requested = input.quantity,
                    available = transferResult.totalAvailable
                )
            )
        }

        // 6. Create ProductTransfer record
        val transfer = ProductTransfer(
            id = generateId("transfer"),
            productId = input.productId,
            fromSiteId = input.fromSiteId,
            toSiteId = input.toSiteId,
            quantity = input.quantity,
            status = "completed",
            notes = input.notes,
            createdAt = now,
            updatedAt = now,
            createdBy = input.userId,
            updatedBy = input.userId
        )

        // 7. Create StockMovements
        val sourceMovement = StockMovement(
            id = generateId("movement"),
            productId = input.productId,
            siteId = input.fromSiteId,
            quantity = -input.quantity, // Negative (out)
            type = MovementType.TRANSFER_OUT,
            date = now,
            purchasePriceAtMovement = 0.0,
            sellingPriceAtMovement = 0.0,
            movementType = MovementType.TRANSFER_OUT,
            referenceId = transfer.id,
            notes = "Transfert vers ${toSite.name}",
            createdAt = now,
            createdBy = input.userId
        )

        val destinationMovement = StockMovement(
            id = generateId("movement"),
            productId = input.productId,
            siteId = input.toSiteId,
            quantity = input.quantity, // Positive (in)
            type = MovementType.TRANSFER_IN,
            date = now,
            purchasePriceAtMovement = 0.0,
            sellingPriceAtMovement = 0.0,
            movementType = MovementType.TRANSFER_IN,
            referenceId = transfer.id,
            notes = "Transfert depuis ${fromSite.name}",
            createdAt = now,
            createdBy = input.userId
        )

        return try {
            // 8. Persist to database
            productTransferRepository.insert(transfer)
            stockMovementRepository.insert(sourceMovement)
            stockMovementRepository.insert(destinationMovement)

            // 9. Create audit entry
            val auditEntry = AuditEntry(
                id = generateId("audit"),
                tableName = "product_transfers",
                recordId = transfer.id,
                action = "CREATE",
                oldValues = null,
                newValues = json.encodeToString(transfer),
                userId = input.userId,
                timestamp = now
            )
            auditRepository.insert(auditEntry)

            // 10. Return result
            val averageCost = if (transferResult.totalTransferred > 0) {
                transferResult.totalCost / transferResult.totalTransferred
            } else {
                0.0
            }

            UseCaseResult.Success(
                data = TransferResult(
                    transfer = transfer,
                    transferredBatches = transferResult.batches,
                    sourceMovement = sourceMovement,
                    destinationMovement = destinationMovement,
                    averageCost = averageCost
                ),
                warnings = warnings
            )
        } catch (e: Exception) {
            UseCaseResult.Error(
                BusinessError.SystemError("Failed to save transfer: ${e.message}", e)
            )
        }
    }

    /**
     * FIFO batch transfer result
     */
    private data class FIFOTransferResult(
        val batches: List<TransferredBatch>,
        val totalTransferred: Double,
        val totalCost: Double,
        val totalAvailable: Double,
        val insufficientStock: Boolean
    )

    /**
     * Transfer batches using FIFO (First In First Out)
     * Creates new batches on destination site preserving original purchase date
     */
    private suspend fun transferBatchesFIFO(
        productId: String,
        fromSiteId: String,
        toSiteId: String,
        quantityNeeded: Double,
        userId: String,
        timestamp: Long
    ): FIFOTransferResult {
        // Get available batches from source site ordered by purchase_date ASC
        val sourceBatches = purchaseBatchRepository.getByProductAndSite(productId, fromSiteId)
            .filter { !it.isExhausted && it.remainingQuantity > 0 }

        val totalAvailable = sourceBatches.sumOf { it.remainingQuantity }
        val transferredBatches = mutableListOf<TransferredBatch>()
        var remaining = quantityNeeded
        var totalCost = 0.0
        var totalTransferred = 0.0

        for (batch in sourceBatches) {
            if (remaining <= 0) break

            val transferred = minOf(batch.remainingQuantity, remaining)
            val newSourceRemaining = batch.remainingQuantity - transferred
            val isSourceExhausted = newSourceRemaining <= 0

            // Update source batch
            purchaseBatchRepository.updateQuantity(
                id = batch.id,
                remainingQuantity = newSourceRemaining,
                isExhausted = isSourceExhausted,
                updatedAt = timestamp,
                updatedBy = userId
            )

            // Create new batch on destination site
            val destinationBatch = PurchaseBatch(
                id = generateId("batch"),
                productId = productId,
                siteId = toSiteId,
                batchNumber = batch.batchNumber?.let { "$it-TRANSFER" },
                purchaseDate = batch.purchaseDate, // Preserve original date for FIFO
                initialQuantity = transferred,
                remainingQuantity = transferred,
                purchasePrice = batch.purchasePrice, // Preserve original cost
                supplierName = batch.supplierName,
                expiryDate = batch.expiryDate,
                isExhausted = false,
                createdAt = timestamp,
                updatedAt = timestamp,
                createdBy = userId,
                updatedBy = userId
            )
            purchaseBatchRepository.insert(destinationBatch)

            transferredBatches.add(
                TransferredBatch(
                    sourceBatchId = batch.id,
                    destinationBatchId = destinationBatch.id,
                    quantity = transferred,
                    purchasePrice = batch.purchasePrice
                )
            )

            totalCost += transferred * batch.purchasePrice
            totalTransferred += transferred
            remaining -= transferred
        }

        return FIFOTransferResult(
            batches = transferredBatches,
            totalTransferred = totalTransferred,
            totalCost = totalCost,
            totalAvailable = totalAvailable,
            insufficientStock = remaining > 0
        )
    }

    /**
     * Validate input data
     */
    private fun validateInput(input: TransferInput): BusinessError? {
        if (input.productId.isBlank()) {
            return BusinessError.ValidationError("productId", "Product ID is required")
        }
        if (input.fromSiteId.isBlank()) {
            return BusinessError.ValidationError("fromSiteId", "Source site ID is required")
        }
        if (input.toSiteId.isBlank()) {
            return BusinessError.ValidationError("toSiteId", "Destination site ID is required")
        }
        if (input.quantity <= 0) {
            return BusinessError.ValidationError("quantity", "Quantity must be greater than 0")
        }
        if (input.userId.isBlank()) {
            return BusinessError.ValidationError("userId", "User ID is required")
        }
        return null
    }

    private fun generateId(prefix: String): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val randomSuffix = Random.nextInt(100000, 999999)
        return "$prefix-$now-$randomSuffix"
    }
}
