package com.medistock.shared.domain.usecase

import com.medistock.shared.data.repository.*
import com.medistock.shared.domain.model.*
import com.medistock.shared.domain.usecase.common.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Input data for creating a purchase
 */
data class PurchaseInput(
    val productId: String,
    val siteId: String,
    val quantity: Double,
    val purchasePrice: Double,
    val supplierName: String = "",
    val supplierId: String? = null,
    val batchNumber: String? = null,
    val expiryDate: Long? = null,
    val userId: String
)

/**
 * Result of a successful purchase
 */
data class PurchaseResult(
    val purchaseBatch: PurchaseBatch,
    val stockMovement: StockMovement,
    val calculatedSellingPrice: Double
)

/**
 * UseCase for handling purchase operations.
 * Encapsulates all business logic for creating purchases:
 * - Validates inputs
 * - Creates PurchaseBatch
 * - Creates StockMovement (type "in")
 * - Calculates selling price based on product margin
 * - Creates audit entry
 */
class PurchaseUseCase(
    private val purchaseBatchRepository: PurchaseBatchRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val productRepository: ProductRepository,
    private val siteRepository: SiteRepository,
    private val auditRepository: AuditRepository
) {
    private val json = Json { prettyPrint = false; encodeDefaults = true }

    /**
     * Execute a purchase operation
     *
     * @param input The purchase input data
     * @return UseCaseResult with PurchaseResult or error
     */
    suspend fun execute(input: PurchaseInput): UseCaseResult<PurchaseResult> {
        // 1. Validate inputs
        val validationError = validateInput(input)
        if (validationError != null) {
            return UseCaseResult.Error(validationError)
        }

        // 2. Verify product exists
        val product = productRepository.getById(input.productId)
            ?: return UseCaseResult.Error(
                BusinessError.NotFound("Product", input.productId)
            )

        // 3. Verify site exists
        val site = siteRepository.getById(input.siteId)
            ?: return UseCaseResult.Error(
                BusinessError.NotFound("Site", input.siteId)
            )

        // 4. Calculate selling price based on product margin
        val sellingPrice = calculateSellingPrice(input.purchasePrice, product)

        val now = Clock.System.now().toEpochMilliseconds()

        // 5. Create PurchaseBatch
        val purchaseBatch = PurchaseBatch(
            id = generateId("batch"),
            productId = input.productId,
            siteId = input.siteId,
            batchNumber = input.batchNumber,
            purchaseDate = now,
            initialQuantity = input.quantity,
            remainingQuantity = input.quantity,
            purchasePrice = input.purchasePrice,
            supplierName = input.supplierName,
            supplierId = input.supplierId,
            expiryDate = input.expiryDate,
            isExhausted = false,
            createdAt = now,
            updatedAt = now,
            createdBy = input.userId,
            updatedBy = input.userId
        )

        // 6. Create StockMovement (type "in" for purchase)
        val stockMovement = StockMovement(
            id = generateId("movement"),
            productId = input.productId,
            siteId = input.siteId,
            quantity = input.quantity,
            type = MovementType.PURCHASE,
            date = now,
            purchasePriceAtMovement = input.purchasePrice,
            sellingPriceAtMovement = 0.0,
            movementType = MovementType.PURCHASE,
            referenceId = purchaseBatch.id,
            notes = "Achat: ${input.supplierName}",
            createdAt = now,
            createdBy = input.userId
        )

        return try {
            // 7. Persist to database
            purchaseBatchRepository.insert(purchaseBatch)
            stockMovementRepository.insert(stockMovement)

            // 8. Create audit entry
            val auditEntry = AuditEntry(
                id = generateId("audit"),
                tableName = "purchase_batches",
                recordId = purchaseBatch.id,
                action = "CREATE",
                oldValues = null,
                newValues = json.encodeToString(purchaseBatch),
                userId = input.userId,
                timestamp = now
            )
            auditRepository.insert(auditEntry)

            // 9. Build warnings list
            val warnings = mutableListOf<BusinessWarning>()

            // Check if product is expiring soon (within 30 days)
            input.expiryDate?.let { expiry ->
                val daysUntilExpiry = ((expiry - now) / (24 * 60 * 60 * 1000)).toInt()
                if (daysUntilExpiry in 0..30) {
                    warnings.add(
                        BusinessWarning.ExpiringProduct(
                            productId = input.productId,
                            productName = product.name,
                            batchId = purchaseBatch.id,
                            expiryDate = expiry,
                            daysUntilExpiry = daysUntilExpiry
                        )
                    )
                }
            }

            UseCaseResult.Success(
                data = PurchaseResult(
                    purchaseBatch = purchaseBatch,
                    stockMovement = stockMovement,
                    calculatedSellingPrice = sellingPrice
                ),
                warnings = warnings
            )
        } catch (e: Exception) {
            UseCaseResult.Error(
                BusinessError.SystemError("Failed to save purchase: ${e.message}", e)
            )
        }
    }

    /**
     * Validate input data
     */
    private fun validateInput(input: PurchaseInput): BusinessError? {
        if (input.productId.isBlank()) {
            return BusinessError.ValidationError("productId", "Product ID is required")
        }
        if (input.siteId.isBlank()) {
            return BusinessError.ValidationError("siteId", "Site ID is required")
        }
        if (input.quantity <= 0) {
            return BusinessError.ValidationError("quantity", "Quantity must be greater than 0")
        }
        if (input.purchasePrice < 0) {
            return BusinessError.ValidationError("purchasePrice", "Purchase price cannot be negative")
        }
        if (input.userId.isBlank()) {
            return BusinessError.ValidationError("userId", "User ID is required")
        }
        return null
    }

    /**
     * Calculate selling price based on product margin settings
     */
    private fun calculateSellingPrice(purchasePrice: Double, product: Product): Double {
        return when (product.marginType) {
            "fixed" -> purchasePrice + (product.marginValue ?: 0.0)
            "percentage" -> purchasePrice * (1 + (product.marginValue ?: 0.0) / 100)
            else -> purchasePrice
        }
    }

    private fun generateId(prefix: String): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val randomSuffix = Random.nextInt(100000, 999999)
        return "$prefix-$now-$randomSuffix"
    }
}

/**
 * Movement type constants
 */
object MovementType {
    const val PURCHASE = "PURCHASE"
    const val SALE = "SALE"
    const val TRANSFER_IN = "TRANSFER_IN"
    const val TRANSFER_OUT = "TRANSFER_OUT"
    const val INVENTORY = "INVENTORY"
    const val MANUAL_IN = "MANUAL_IN"
    const val MANUAL_OUT = "MANUAL_OUT"
}
