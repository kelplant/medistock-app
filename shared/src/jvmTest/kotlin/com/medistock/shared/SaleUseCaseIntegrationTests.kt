package com.medistock.shared

import com.medistock.shared.domain.usecase.SaleInput
import com.medistock.shared.domain.usecase.SaleItemInput
import com.medistock.shared.domain.usecase.SaleResult
import com.medistock.shared.domain.usecase.MovementType
import com.medistock.shared.domain.usecase.common.BusinessError
import com.medistock.shared.domain.usecase.common.UseCaseResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Integration tests for SaleUseCase.
 * These tests verify the complete sale workflow including:
 * - Sale with sufficient stock
 * - Stock movement generation (negative quantity)
 * - Multiple items processing
 * - Revenue and profit calculation
 * - Validation logic
 * - Audit trail creation
 *
 * Note: FIFO allocation tests are already covered in FifoIntegrationTests.kt
 */
class SaleUseCaseIntegrationTests {

    private lateinit var sdk: MedistockSDK

    @BeforeEach
    fun setup() {
        sdk = MedistockSDK(DatabaseDriverFactory())
    }

    @Test
    fun `should_succeedWithoutWarnings_when_saleHasSufficientStock`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 100.0,
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
                    quantity = 50.0,
                    unitPrice = 20.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val success = result as UseCaseResult.Success
        assertTrue(success.warnings.isEmpty())
    }

    @Test
    fun `should_createStockMovementWithNegativeQuantity_when_saleIsExecuted`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 100.0,
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
                    quantity = 25.0,
                    unitPrice = 20.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data as SaleResult

        // Check stock movements
        val movements = sdk.stockMovementRepository.getBySite(site.id)
        val saleMovement = movements.find { it.type == MovementType.SALE }

        assertNotNull(saleMovement)
        assertEquals(MovementType.SALE, saleMovement.type)
        assertEquals(-25.0, saleMovement.quantity) // Negative for outgoing
        assertEquals(product.id, saleMovement.productId)
        assertEquals(site.id, saleMovement.siteId)
    }

    @Test
    fun `should_processAllItems_when_saleHasMultipleItems`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product1 = sdk.createProduct("Product 1", site.id, userId = "test-user")
        sdk.productRepository.insert(product1)
        val batch1 = sdk.createPurchaseBatch(
            productId = product1.id,
            siteId = site.id,
            quantity = 100.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch1)

        val product2 = sdk.createProduct("Product 2", site.id, userId = "test-user")
        sdk.productRepository.insert(product2)
        val batch2 = sdk.createPurchaseBatch(
            productId = product2.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 20.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch2)

        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(productId = product1.id, quantity = 10.0, unitPrice = 15.0),
                SaleItemInput(productId = product2.id, quantity = 5.0, unitPrice = 30.0)
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data as SaleResult

        // Verify both products were affected
        val updatedBatch1 = sdk.purchaseBatchRepository.getById(batch1.id)
        val updatedBatch2 = sdk.purchaseBatchRepository.getById(batch2.id)

        assertEquals(90.0, updatedBatch1?.remainingQuantity)
        assertEquals(45.0, updatedBatch2?.remainingQuantity)
    }

    @Test
    fun `should_calculateTotalRevenueCorrectly_when_saleIsExecuted`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 100.0,
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
                    quantity = 20.0,
                    unitPrice = 25.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data as SaleResult

        // Revenue = 20 * 25 = 500
        assertEquals(500.0, saleResult.totalRevenue)
    }

    @Test
    fun `should_calculateGrossProfitCorrectly_when_saleIsExecuted`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 100.0,
            purchasePrice = 15.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch)

        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 30.0,
                    unitPrice = 25.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data as SaleResult

        // Cost = 30 * 15 = 450
        // Revenue = 30 * 25 = 750
        // Profit = 750 - 450 = 300
        assertEquals(450.0, saleResult.totalCost)
        assertEquals(750.0, saleResult.totalRevenue)
        assertEquals(300.0, saleResult.grossProfit)
    }

    @Test
    fun `should_rejectWithValidationError_when_itemsListIsEmpty`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = emptyList(),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.ValidationError>(error)
    }

    @Test
    fun `should_rejectWithValidationError_when_unitPriceIsNegative`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 10.0,
                    unitPrice = -5.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.ValidationError>(error)
    }

    @Test
    fun `should_rejectWithValidationError_when_customerNameIsBlank`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "",
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
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.ValidationError>(error)
    }

    @Test
    fun `should_failWithNotFound_when_productDoesNotExist`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = "non-existent-product",
                    quantity = 10.0,
                    unitPrice = 20.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.NotFound>(error)
    }

    @Test
    fun `should_createAuditEntry_when_saleIsExecuted`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 100.0,
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
        val saleResult = (result as UseCaseResult.Success).data as SaleResult

        val auditEntries = sdk.auditRepository.getByTable("sales", limit = 100)
            .filter { it.recordId == saleResult.sale.id }

        assertTrue(auditEntries.isNotEmpty())
        val auditEntry = auditEntries.first()
        assertEquals("sales", auditEntry.tableName)
        assertEquals("CREATE", auditEntry.action)
        assertEquals("test-user", auditEntry.userId)
    }
}
