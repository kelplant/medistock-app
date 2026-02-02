package com.medistock.shared

import com.medistock.shared.domain.usecase.PurchaseInput
import com.medistock.shared.domain.usecase.PurchaseResult
import com.medistock.shared.domain.usecase.MovementType
import com.medistock.shared.domain.usecase.common.BusinessError
import com.medistock.shared.domain.usecase.common.BusinessWarning
import com.medistock.shared.domain.usecase.common.UseCaseResult
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Integration tests for PurchaseUseCase.
 * These tests verify the complete purchase workflow including:
 * - Batch creation with correct quantities
 * - Stock movement generation
 * - Margin calculation (fixed and percentage)
 * - Expiring product warnings
 * - Validation logic
 * - Audit trail creation
 */
class PurchaseUseCaseIntegrationTests {

    private lateinit var sdk: MedistockSDK
    private lateinit var packagingTypeId: String

    @BeforeEach
    fun setup() {
        sdk = MedistockSDK(DatabaseDriverFactory())
        val packagingType = sdk.createPackagingType(name = "Box", level1Name = "Unit")
        kotlinx.coroutines.runBlocking {
            sdk.packagingTypeRepository.insert(packagingType)
        }
        packagingTypeId = packagingType.id
    }

    @Test
    fun `should_createBatchWithCorrectQuantities_when_purchaseIsExecuted`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 100.0,
            purchasePrice = 50.0,
            supplierName = "Test Supplier",
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        assertEquals(100.0, purchaseResult.purchaseBatch.initialQuantity)
        assertEquals(100.0, purchaseResult.purchaseBatch.remainingQuantity)
        assertFalse(purchaseResult.purchaseBatch.isExhausted)
    }

    @Test
    fun `should_createStockMovementWithPositiveQuantity_when_purchaseIsExecuted`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 25.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        assertEquals(MovementType.PURCHASE, purchaseResult.stockMovement.type)
        assertEquals(50.0, purchaseResult.stockMovement.quantity)
        assertEquals(product.id, purchaseResult.stockMovement.productId)
        assertEquals(site.id, purchaseResult.stockMovement.siteId)
    }

    @Test
    fun `should_calculateSellingPriceWithPercentageMargin_when_productHasPercentageMargin`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
            .copy(marginType = "percentage", marginValue = 20.0)
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 100.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        // 100 * (1 + 20/100) = 100 * 1.20 = 120
        assertEquals(120.0, purchaseResult.calculatedSellingPrice)
    }

    @Test
    fun `should_calculateSellingPriceWithFixedMargin_when_productHasFixedMargin`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
            .copy(marginType = "fixed", marginValue = 25.0)
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 75.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        // 75 + 25 = 100
        assertEquals(100.0, purchaseResult.calculatedSellingPrice)
    }

    @Test
    fun `should_generateExpiringProductWarning_when_expiryDateIsWithin30Days`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val now = Clock.System.now().toEpochMilliseconds()
        val expiryDate = now + (15 * 24 * 60 * 60 * 1000L) // 15 days from now

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 50.0,
            expiryDate = expiryDate,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val success = result as UseCaseResult.Success

        assertTrue(success.warnings.isNotEmpty())
        val warning = success.warnings.first()
        assertIs<BusinessWarning.ExpiringProduct>(warning)
        assertEquals(product.id, warning.productId)
        assertTrue(warning.daysUntilExpiry in 14..16) // Allow some tolerance
    }

    @Test
    fun `should_rejectWithValidationError_when_quantityIsNegative`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = -10.0,
            purchasePrice = 50.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.ValidationError>(error)
        assertEquals("quantity", error.field)
    }

    @Test
    fun `should_rejectWithValidationError_when_productIdIsBlank`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val input = PurchaseInput(
            productId = "",
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 50.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.ValidationError>(error)
        assertEquals("productId", error.field)
    }

    @Test
    fun `should_failWithNotFound_when_productDoesNotExist`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val input = PurchaseInput(
            productId = "non-existent-product",
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 50.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.NotFound>(error)
        assertEquals("Product", error.entityType)
    }

    @Test
    fun `should_failWithNotFound_when_siteDoesNotExist`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = "non-existent-site",
            quantity = 10.0,
            purchasePrice = 50.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.NotFound>(error)
        assertEquals("Site", error.entityType)
    }

    @Test
    fun `should_createAuditEntry_when_purchaseIsExecuted`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 50.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        val auditEntries = sdk.auditRepository.getByTable("purchase_batches", limit = 100)
            .filter { it.recordId == purchaseResult.purchaseBatch.id }

        assertTrue(auditEntries.isNotEmpty())
        val auditEntry = auditEntries.first()
        assertEquals("purchase_batches", auditEntry.tableName)
        assertEquals("CREATE", auditEntry.action)
        assertEquals("test-user", auditEntry.userId)
    }

    @Test
    fun `should_succeedWithLargeQuantity_when_purchasingMillionUnits`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 1000000.0,
            purchasePrice = 1.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        assertEquals(1000000.0, purchaseResult.purchaseBatch.initialQuantity)
        assertEquals(1000000.0, purchaseResult.purchaseBatch.remainingQuantity)
    }

    @Test
    fun `should_maintainPricePrecision_when_purchasingWithDecimalPrice`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 10.555,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        assertEquals(10.555, purchaseResult.purchaseBatch.purchasePrice)
    }

    @Test
    fun `should_returnSamePrice_when_marginIsZero`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
            .copy(marginType = "percentage", marginValue = 0.0)
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 100.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        assertEquals(100.0, purchaseResult.calculatedSellingPrice)
    }

    @Test
    fun `should_preserveBatchNumber_when_batchNumberIsProvided`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 50.0,
            batchNumber = "BATCH-2026-001",
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        assertEquals("BATCH-2026-001", purchaseResult.purchaseBatch.batchNumber)
    }
}
