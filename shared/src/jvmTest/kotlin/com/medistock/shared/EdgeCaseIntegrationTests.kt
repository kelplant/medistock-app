package com.medistock.shared

import com.medistock.shared.domain.usecase.PurchaseInput
import com.medistock.shared.domain.usecase.PurchaseResult
import com.medistock.shared.domain.usecase.SaleInput
import com.medistock.shared.domain.usecase.SaleItemInput
import com.medistock.shared.domain.usecase.SaleResult
import com.medistock.shared.domain.usecase.TransferInput
import com.medistock.shared.domain.usecase.common.BusinessError
import com.medistock.shared.domain.usecase.common.UseCaseResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Integration tests for edge cases and boundary conditions.
 * These tests verify system behavior at the limits:
 * - Floating point precision
 * - Very large and very small quantities
 * - Zero values
 * - Exact exhaustion scenarios
 * - Negative profit calculations
 */
class EdgeCaseIntegrationTests {

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
    fun `should_maintainFloatingPointPrecision_when_calculatingMargin`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
            .copy(marginType = "percentage", marginValue = 17.5)
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 1.0,
            purchasePrice = 23.45,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        // 23.45 * 1.175 = 27.55375
        assertEquals(27.55375, purchaseResult.calculatedSellingPrice, 0.00001)
    }

    @Test
    fun `should_succeedWithVeryLargeQuantity_when_purchasing`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 1e9, // 1 billion
            purchasePrice = 0.01,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        assertEquals(1e9, purchaseResult.purchaseBatch.initialQuantity)
        assertEquals(1e9, purchaseResult.purchaseBatch.remainingQuantity)
    }

    @Test
    fun `should_succeedWithVerySmallQuantity_when_purchasing`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 0.001,
            purchasePrice = 100.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        assertEquals(0.001, purchaseResult.purchaseBatch.initialQuantity)
        assertEquals(0.001, purchaseResult.purchaseBatch.remainingQuantity)
    }

    @Test
    fun `should_rejectWithValidationError_when_quantityIsZero`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 0.0,
            purchasePrice = 10.0,
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
    fun `should_exhaustBatch_when_saleExactlyConsumesAllStock`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 75.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch)

        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 75.0, // Exact amount
                    unitPrice = 20.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)

        val updatedBatch = sdk.purchaseBatchRepository.getById(batch.id)
        assertEquals(0.0, updatedBatch?.remainingQuantity)
        assertTrue(updatedBatch?.isExhausted == true)
    }

    @Test
    fun `should_leaveSmallAmount_when_saleLeaves0Point001InBatch`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 10.001,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch)

        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 10.0,
                    unitPrice = 20.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)

        val updatedBatch = sdk.purchaseBatchRepository.getById(batch.id)
        assertNotNull(updatedBatch)
        assertEquals(0.001, updatedBatch.remainingQuantity, 0.0001)
        assertFalse(updatedBatch.isExhausted)
    }

    @Test
    fun `should_exhaustBatch_when_transferExactlyConsumesAllStock`() = runTest {
        // Arrange
        val sourceSite = sdk.createSite("Source Site", "test-user")
        sdk.siteRepository.insert(sourceSite)

        val destSite = sdk.createSite("Destination Site", "test-user")
        sdk.siteRepository.insert(destSite)

        val product = sdk.createProduct("Test Product", sourceSite.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = sourceSite.id,
            quantity = 100.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch)

        val input = TransferInput(
            productId = product.id,
            fromSiteId = sourceSite.id,
            toSiteId = destSite.id,
            quantity = 100.0, // Exact amount
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)

        val updatedBatch = sdk.purchaseBatchRepository.getById(batch.id)
        assertEquals(0.0, updatedBatch?.remainingQuantity)
        assertTrue(updatedBatch?.isExhausted == true)
    }

    @Test
    fun `should_returnOriginalPrice_when_marginCalculationWithPriceZero`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
            .copy(marginType = "percentage", marginValue = 50.0)
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 0.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        // 0 * 1.50 = 0
        assertEquals(0.0, purchaseResult.calculatedSellingPrice)
    }

    @Test
    fun `should_calculateNegativeProfit_when_costExceedsRevenue`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        // Purchase at high price
        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 100.0,
            purchasePrice = 50.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch)

        // Sell at lower price (loss)
        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 20.0,
                    unitPrice = 30.0 // Selling below cost
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data as SaleResult

        // Cost = 20 * 50 = 1000
        // Revenue = 20 * 30 = 600
        // Profit = 600 - 1000 = -400 (loss)
        assertEquals(1000.0, saleResult.totalCost)
        assertEquals(600.0, saleResult.totalRevenue)
        assertEquals(-400.0, saleResult.grossProfit)
    }

    @Test
    fun `should_handleFloatingPointPrecision_when_multipleOperations`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 0.3,
            purchasePrice = 0.1,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch)

        // Sell 0.1 three times
        for (i in 1..3) {
            val saleInput = SaleInput(
                siteId = site.id,
                customerName = "Customer $i",
                items = listOf(
                    SaleItemInput(
                        productId = product.id,
                        quantity = 0.1,
                        unitPrice = 1.0
                    )
                ),
                userId = "test-user"
            )
            val result = sdk.saleUseCase.execute(saleInput)
            assertIs<UseCaseResult.Success<*>>(result)
        }

        // Verify batch is exhausted (or very close to zero)
        val updatedBatch = sdk.purchaseBatchRepository.getById(batch.id)
        assertNotNull(updatedBatch)
        val remaining = updatedBatch.remainingQuantity
        assertTrue(remaining < 0.0001)
    }

    @Test
    fun `should_handleVeryLargePriceValue_when_purchasing`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 1.0,
            purchasePrice = 999999.99,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        assertEquals(999999.99, purchaseResult.purchaseBatch.purchasePrice)
    }

    @Test
    fun `should_handleMultipleDecimalPlaces_when_calculatingRevenue`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 3.333,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch)

        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 3.0,
                    unitPrice = 7.777
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data as SaleResult

        // Revenue = 3 * 7.777 = 23.331
        assertEquals(23.331, saleResult.totalRevenue, 0.001)
    }
}
