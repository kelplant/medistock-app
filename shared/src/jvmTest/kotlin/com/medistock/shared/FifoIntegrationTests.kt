package com.medistock.shared

import com.medistock.shared.domain.usecase.SaleInput
import com.medistock.shared.domain.usecase.SaleItemInput
import com.medistock.shared.domain.usecase.common.UseCaseResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Integration tests for FIFO (First-In-First-Out) batch allocation.
 * These tests verify that when selling products, the oldest batches are consumed first.
 */
class FifoIntegrationTests {

    private lateinit var sdk: MedistockSDK

    @BeforeEach
    fun setup() {
        // Create a fresh in-memory database for each test
        sdk = MedistockSDK(DatabaseDriverFactory())
    }

    @Test
    fun `FIFO allocation consumes oldest batch first`() = runTest {
        // Setup: Create site and product
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        // Create 3 batches with different dates (simulated by insertion order)
        // Batch 1: oldest (100 units at 10€)
        val batch1 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 100.0,
            purchasePrice = 10.0,
            supplierName = "Supplier A",
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch1)

        // Small delay to ensure different timestamps
        kotlinx.coroutines.delay(10)

        // Batch 2: middle (50 units at 12€)
        val batch2 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 12.0,
            supplierName = "Supplier B",
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch2)

        kotlinx.coroutines.delay(10)

        // Batch 3: newest (75 units at 15€)
        val batch3 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 75.0,
            purchasePrice = 15.0,
            supplierName = "Supplier C",
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch3)

        // Execute: Sell 80 units (should consume all of batch1=100, leaving 20)
        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 80.0,
                    unitPrice = 20.0
                )
            ),
            userId = "test-user"
        )

        val result = sdk.saleUseCase.execute(saleInput)

        // Verify: Sale succeeded
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data

        // Verify: Only batch1 was affected (FIFO - oldest first)
        val updatedBatch1 = sdk.purchaseBatchRepository.getById(batch1.id)
        val updatedBatch2 = sdk.purchaseBatchRepository.getById(batch2.id)
        val updatedBatch3 = sdk.purchaseBatchRepository.getById(batch3.id)

        // Batch 1 should have 20 remaining (100 - 80)
        assertEquals(20.0, updatedBatch1?.remainingQuantity)
        // Batch 2 and 3 should be untouched
        assertEquals(50.0, updatedBatch2?.remainingQuantity)
        assertEquals(75.0, updatedBatch3?.remainingQuantity)
    }

    @Test
    fun `FIFO allocation spans multiple batches when needed`() = runTest {
        // Setup
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        // Create 3 batches
        val batch1 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 30.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch1)
        kotlinx.coroutines.delay(10)

        val batch2 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 40.0,
            purchasePrice = 12.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch2)
        kotlinx.coroutines.delay(10)

        val batch3 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 15.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch3)

        // Execute: Sell 60 units (should exhaust batch1=30, take 30 from batch2=40)
        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 60.0,
                    unitPrice = 20.0
                )
            ),
            userId = "test-user"
        )

        val result = sdk.saleUseCase.execute(saleInput)

        // Verify
        assertIs<UseCaseResult.Success<*>>(result)

        val updatedBatch1 = sdk.purchaseBatchRepository.getById(batch1.id)
        val updatedBatch2 = sdk.purchaseBatchRepository.getById(batch2.id)
        val updatedBatch3 = sdk.purchaseBatchRepository.getById(batch3.id)

        // Batch 1 should be exhausted
        assertEquals(0.0, updatedBatch1?.remainingQuantity)
        assertTrue(updatedBatch1?.isExhausted == true)

        // Batch 2 should have 10 remaining (40 - 30)
        assertEquals(10.0, updatedBatch2?.remainingQuantity)

        // Batch 3 should be untouched
        assertEquals(50.0, updatedBatch3?.remainingQuantity)
    }

    @Test
    fun `FIFO creates correct batch allocations for cost tracking`() = runTest {
        // Setup
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        // Batch 1: 50 units at 10€
        val batch1 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch1)
        kotlinx.coroutines.delay(10)

        // Batch 2: 50 units at 20€
        val batch2 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 20.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch2)

        // Execute: Sell 70 units (50 from batch1 at 10€, 20 from batch2 at 20€)
        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 70.0,
                    unitPrice = 25.0
                )
            ),
            userId = "test-user"
        )

        val result = sdk.saleUseCase.execute(saleInput)

        // Verify
        assertIs<UseCaseResult.Success<*>>(result)
        val saleResult = (result as UseCaseResult.Success).data as com.medistock.shared.domain.usecase.SaleResult

        // Verify cost calculation: 50*10 + 20*20 = 500 + 400 = 900
        assertEquals(900.0, saleResult.totalCost)

        // Verify revenue: 70*25 = 1750
        assertEquals(1750.0, saleResult.totalRevenue)

        // Verify gross profit: 1750 - 900 = 850
        assertEquals(850.0, saleResult.grossProfit)
    }

    @Test
    fun `FIFO handles insufficient stock with warning but continues sale`() = runTest {
        // Setup
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        // Only 30 units available
        val batch1 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 30.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch1)

        // Execute: Try to sell 50 units (only 30 available)
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

        val result = sdk.saleUseCase.execute(saleInput)

        // Verify: Sale succeeds but with warning
        assertIs<UseCaseResult.Success<*>>(result)
        val success = result as UseCaseResult.Success

        // Should have a warning about insufficient stock
        assertTrue(success.warnings.isNotEmpty())
        val warning = success.warnings.first()
        assertIs<com.medistock.shared.domain.usecase.common.BusinessWarning.InsufficientStock>(warning)

        // Batch should be exhausted
        val updatedBatch1 = sdk.purchaseBatchRepository.getById(batch1.id)
        assertEquals(0.0, updatedBatch1?.remainingQuantity)
        assertTrue(updatedBatch1?.isExhausted == true)
    }

    @Test
    fun `FIFO skips exhausted batches`() = runTest {
        // Setup
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "test-user")
        sdk.productRepository.insert(product)

        // Batch 1: already exhausted
        val batch1 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 5.0,
            userId = "test-user"
        ).copy(remainingQuantity = 0.0, isExhausted = true)
        sdk.purchaseBatchRepository.insert(batch1)
        kotlinx.coroutines.delay(10)

        // Batch 2: available
        val batch2 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = site.id,
            quantity = 100.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch2)

        // Execute: Sell 30 units
        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Test Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 30.0,
                    unitPrice = 15.0
                )
            ),
            userId = "test-user"
        )

        val result = sdk.saleUseCase.execute(saleInput)

        // Verify
        assertIs<UseCaseResult.Success<*>>(result)

        // Batch 1 should still be exhausted (was skipped)
        val updatedBatch1 = sdk.purchaseBatchRepository.getById(batch1.id)
        assertEquals(0.0, updatedBatch1?.remainingQuantity)

        // Batch 2 should have 70 remaining (100 - 30)
        val updatedBatch2 = sdk.purchaseBatchRepository.getById(batch2.id)
        assertEquals(70.0, updatedBatch2?.remainingQuantity)
    }
}
