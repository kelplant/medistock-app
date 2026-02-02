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
    fun `should_succeedWithoutWarnings_when_saleHasSufficientStock`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
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

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
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

        val product1 = sdk.createProduct("Product 1", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product1)
        val batch1 = sdk.createPurchaseBatch(
            productId = product1.id,
            siteId = site.id,
            quantity = 100.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch1)

        val product2 = sdk.createProduct("Product 2", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
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

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
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

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
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

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
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

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
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

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
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

    @Test
    fun `should_deduct20BaseUnits_when_selling2BoxesWithConversionFactor10`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        // Create packaging type with level2 name
        val packagingType = sdk.createPackagingType(
            name = "Medicine Box",
            level1Name = "unite",
            level2Name = "boite",
            level2Quantity = 10,
            defaultConversionFactor = 10.0
        )
        sdk.packagingTypeRepository.insert(packagingType)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingType.id, userId = "test-user")
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
                    quantity = 2.0,
                    unitPrice = 120.0,
                    selectedLevel = 2,
                    conversionFactor = 10.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)

        val updatedBatch = sdk.purchaseBatchRepository.getById(batch.id)
        assertEquals(80.0, updatedBatch?.remainingQuantity) // 100 - 20 = 80
    }

    @Test
    fun `should_useFifoWithBaseQuantity_when_level2Sale`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val packagingType = sdk.createPackagingType(
            name = "Box",
            level1Name = "unite",
            level2Name = "boite",
            level2Quantity = 10,
            defaultConversionFactor = 10.0
        )
        sdk.packagingTypeRepository.insert(packagingType)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingType.id, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 5.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch)

        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 2.0,
                    unitPrice = 100.0,
                    selectedLevel = 2,
                    conversionFactor = 10.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data as SaleResult

        // FIFO allocation should use baseQuantity (20 units)
        val processedItem = saleResult.items.first()
        val totalAllocated = processedItem.allocations.sumOf { it.quantity }
        assertEquals(20.0, totalAllocated) // not 2.0
    }

    @Test
    fun `should_createNegative20StockMovement_when_selling2BoxesWithCF10`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val packagingType = sdk.createPackagingType(
            name = "Box",
            level1Name = "unite",
            level2Name = "boite",
            level2Quantity = 10,
            defaultConversionFactor = 10.0
        )
        sdk.packagingTypeRepository.insert(packagingType)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingType.id, userId = "test-user")
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
                    quantity = 2.0,
                    unitPrice = 120.0,
                    selectedLevel = 2,
                    conversionFactor = 10.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data as SaleResult

        val stockMovement = saleResult.items.first().stockMovement
        assertEquals(-20.0, stockMovement.quantity) // Not -2.0
        assertEquals(MovementType.SALE, stockMovement.type)
    }

    @Test
    fun `should_storeSaleItemWithBaseQuantity_when_level2Sale`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val packagingType = sdk.createPackagingType(
            name = "Box",
            level1Name = "unite",
            level2Name = "boite",
            level2Quantity = 10,
            defaultConversionFactor = 10.0
        )
        sdk.packagingTypeRepository.insert(packagingType)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingType.id, userId = "test-user")
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
                    quantity = 2.0,
                    unitPrice = 120.0,
                    selectedLevel = 2,
                    conversionFactor = 10.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data as SaleResult

        val saleItem = saleResult.items.first().saleItem
        assertEquals(2.0, saleItem.quantity) // Display quantity
        assertEquals(20.0, saleItem.baseQuantity) // Base quantity
        assertEquals("boite", saleItem.unit)
    }

    @Test
    fun `should_haveNullBaseQuantity_when_level1Sale`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
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
                    unitPrice = 15.0,
                    selectedLevel = 1,
                    conversionFactor = null
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data as SaleResult

        val saleItem = saleResult.items.first().saleItem
        assertEquals(10.0, saleItem.quantity)
        assertNull(saleItem.baseQuantity)

        // Stock movement should use quantity directly
        val stockMovement = saleResult.items.first().stockMovement
        assertEquals(-10.0, stockMovement.quantity)
    }

    @Test
    fun `should_handleMixedLevels_when_saleHasLevel1AndLevel2Items`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val packagingType = sdk.createPackagingType(
            name = "Box",
            level1Name = "unite",
            level2Name = "boite",
            level2Quantity = 10,
            defaultConversionFactor = 10.0
        )
        sdk.packagingTypeRepository.insert(packagingType)

        val product1 = sdk.createProduct("Product 1", site.id, packagingTypeId = packagingType.id, userId = "test-user")
        sdk.productRepository.insert(product1)
        val batch1 = sdk.createPurchaseBatch(
            productId = product1.id,
            siteId = site.id,
            quantity = 100.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch1)

        val product2 = sdk.createProduct("Product 2", site.id, packagingTypeId = packagingType.id, userId = "test-user")
        sdk.productRepository.insert(product2)
        val batch2 = sdk.createPurchaseBatch(
            productId = product2.id,
            siteId = site.id,
            quantity = 100.0,
            purchasePrice = 5.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch2)

        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                // Level 1 item
                SaleItemInput(
                    productId = product1.id,
                    quantity = 15.0,
                    unitPrice = 12.0,
                    selectedLevel = 1,
                    conversionFactor = null
                ),
                // Level 2 item
                SaleItemInput(
                    productId = product2.id,
                    quantity = 3.0,
                    unitPrice = 60.0,
                    selectedLevel = 2,
                    conversionFactor = 10.0
                )
            ),
            userId = "test-user"
        )

        // Act
        val result = sdk.saleUseCase.execute(saleInput)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data as SaleResult

        // Verify level 1 item
        val item1 = saleResult.items[0].saleItem
        assertEquals(15.0, item1.quantity)
        assertNull(item1.baseQuantity)
        assertEquals("unite", item1.unit)

        // Verify level 2 item
        val item2 = saleResult.items[1].saleItem
        assertEquals(3.0, item2.quantity)
        assertEquals(30.0, item2.baseQuantity)
        assertEquals("boite", item2.unit)

        // Verify stock movements
        assertEquals(-15.0, saleResult.items[0].stockMovement.quantity)
        assertEquals(-30.0, saleResult.items[1].stockMovement.quantity)

        // Verify batch updates
        val updatedBatch1 = sdk.purchaseBatchRepository.getById(batch1.id)
        val updatedBatch2 = sdk.purchaseBatchRepository.getById(batch2.id)
        assertEquals(85.0, updatedBatch1?.remainingQuantity) // 100 - 15
        assertEquals(70.0, updatedBatch2?.remainingQuantity) // 100 - 30
    }
}
