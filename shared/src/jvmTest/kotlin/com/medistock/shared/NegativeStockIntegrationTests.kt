package com.medistock.shared

import com.medistock.shared.domain.usecase.SaleInput
import com.medistock.shared.domain.usecase.SaleItemInput
import com.medistock.shared.domain.usecase.TransferInput
import com.medistock.shared.domain.usecase.common.BusinessWarning
import com.medistock.shared.domain.usecase.common.UseCaseResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Integration tests for negative stock scenarios.
 * These tests verify the business rule: "stock négatif autorisé"
 * Operations should succeed with warnings when stock is insufficient or zero.
 */
class NegativeStockIntegrationTests {

    private lateinit var sdk: MedistockSDK

    @BeforeEach
    fun setup() {
        sdk = MedistockSDK(DatabaseDriverFactory())
    }

    @Test
    fun `should_proceedWithWarning_when_saleWithZeroStock`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        // No batches = zero stock

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
        val success = result as UseCaseResult.Success

        assertTrue(success.warnings.isNotEmpty())
        val warning = success.warnings.first()
        assertIs<BusinessWarning.InsufficientStock>(warning)
        assertEquals(10.0, warning.requested)
        assertEquals(0.0, warning.available)
        assertEquals(10.0, warning.shortage)
    }

    @Test
    fun `should_proceedWithWarning_when_saleCreatingDeepNegativeStock`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        // No stock, sell large quantity
        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 1000.0,
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

        assertTrue(success.warnings.isNotEmpty())
        val warning = success.warnings.first()
        assertIs<BusinessWarning.InsufficientStock>(warning)
        assertEquals(1000.0, warning.requested)
        assertEquals(0.0, warning.available)
        assertEquals(1000.0, warning.shortage)
    }

    @Test
    fun `should_proceedWithWarning_when_transferWithZeroStock`() = runTest {
        // Arrange
        val sourceSite = sdk.createSite("Source Site", "test-user")
        sdk.siteRepository.insert(sourceSite)

        val destSite = sdk.createSite("Destination Site", "test-user")
        sdk.siteRepository.insert(destSite)

        val product = sdk.createProduct("Test Product", sourceSite.id, userId = "test-user")
        sdk.productRepository.insert(product)

        // No batches at source = zero stock

        val input = TransferInput(
            productId = product.id,
            fromSiteId = sourceSite.id,
            toSiteId = destSite.id,
            quantity = 50.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val success = result as UseCaseResult.Success

        assertTrue(success.warnings.isNotEmpty())
        val warning = success.warnings.first()
        assertIs<BusinessWarning.InsufficientStock>(warning)
        assertEquals(50.0, warning.requested)
        assertEquals(0.0, warning.available)
    }

    @Test
    fun `should_generateWarningsEachTime_when_multipleSalesDepleteStock`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 20.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch)

        // First sale: uses up all stock
        val sale1Input = SaleInput(
            siteId = site.id,
            customerName = "Customer 1",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 20.0,
                    unitPrice = 20.0
                )
            ),
            userId = "test-user"
        )

        val result1 = sdk.saleUseCase.execute(sale1Input)
        assertIs<UseCaseResult.Success<*>>(result1)
        assertTrue((result1 as UseCaseResult.Success).warnings.isEmpty()) // No warning

        // Second sale: stock is now zero
        val sale2Input = SaleInput(
            siteId = site.id,
            customerName = "Customer 2",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 10.0,
                    unitPrice = 20.0
                )
            ),
            userId = "test-user"
        )

        val result2 = sdk.saleUseCase.execute(sale2Input)
        assertIs<UseCaseResult.Success<*>>(result2)
        val success2 = result2 as UseCaseResult.Success
        assertTrue(success2.warnings.isNotEmpty())

        // Third sale: stock is already negative
        val sale3Input = SaleInput(
            siteId = site.id,
            customerName = "Customer 3",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 15.0,
                    unitPrice = 20.0
                )
            ),
            userId = "test-user"
        )

        val result3 = sdk.saleUseCase.execute(sale3Input)
        assertIs<UseCaseResult.Success<*>>(result3)
        val success3 = result3 as UseCaseResult.Success
        assertTrue(success3.warnings.isNotEmpty())
    }

    @Test
    fun `should_replenishStock_when_subsequentPurchaseAfterNegativeStock`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        // Create initial batch
        val batch1 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch1)

        // Sell more than available
        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 30.0,
                    unitPrice = 20.0
                )
            ),
            userId = "test-user"
        )

        val saleResult = sdk.saleUseCase.execute(saleInput)
        assertIs<UseCaseResult.Success<*>>(saleResult)

        // Act: Purchase more stock
        val batch2 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch2)

        // Verify: Stock is replenished
        val allBatches = sdk.purchaseBatchRepository.getByProductAndSite(product.id, site.id)
        val totalStock = allBatches.sumOf { it.remainingQuantity }

        // Initial 10, sold 30 (went negative by 20), then added 50
        // First batch exhausted (0), second batch has 50
        assertEquals(50.0, totalStock)
    }

    @Test
    fun `should_includeCorrectShortageAmount_when_saleWithInsufficientStock`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 15.0,
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
                    quantity = 40.0,
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

        assertTrue(success.warnings.isNotEmpty())
        val warning = success.warnings.first()
        assertIs<BusinessWarning.InsufficientStock>(warning)

        assertEquals(40.0, warning.requested)
        assertEquals(15.0, warning.available)
        assertEquals(25.0, warning.shortage) // 40 - 15 = 25
    }

    @Test
    fun `should_proceedWithWarning_when_saleWithPartialStock`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 5.0,
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
                    quantity = 100.0,
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

        assertTrue(success.warnings.isNotEmpty())
        val warning = success.warnings.first()
        assertIs<BusinessWarning.InsufficientStock>(warning)

        // Batch should be exhausted
        val updatedBatch = sdk.purchaseBatchRepository.getById(batch.id)
        assertEquals(0.0, updatedBatch?.remainingQuantity)
        assertTrue(updatedBatch?.isExhausted == true)
    }

    @Test
    fun `should_proceedWithWarning_when_transferWithInsufficientStock`() = runTest {
        // Arrange
        val sourceSite = sdk.createSite("Source Site", "test-user")
        sdk.siteRepository.insert(sourceSite)

        val destSite = sdk.createSite("Destination Site", "test-user")
        sdk.siteRepository.insert(destSite)

        val product = sdk.createProduct("Test Product", sourceSite.id, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = sourceSite.id,
            quantity = 25.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch)

        val input = TransferInput(
            productId = product.id,
            fromSiteId = sourceSite.id,
            toSiteId = destSite.id,
            quantity = 100.0, // Much more than available
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val success = result as UseCaseResult.Success

        assertTrue(success.warnings.isNotEmpty())
        val warning = success.warnings.first()
        assertIs<BusinessWarning.InsufficientStock>(warning)
        assertEquals(100.0, warning.requested)
        assertEquals(25.0, warning.available)
        assertEquals(75.0, warning.shortage)
    }
}
