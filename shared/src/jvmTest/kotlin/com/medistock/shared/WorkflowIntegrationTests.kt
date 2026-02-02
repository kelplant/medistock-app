package com.medistock.shared

import com.medistock.shared.domain.usecase.*
import com.medistock.shared.domain.usecase.common.BusinessError
import com.medistock.shared.domain.usecase.common.UseCaseResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for complete business workflows.
 * Tests the full cycle: purchase → stock verification → sale → profit calculation
 */
class WorkflowIntegrationTests {

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

    // ========== PURCHASE WORKFLOW TESTS ==========

    @Test
    fun `purchase creates batch and stock movement`() = runTest {
        // Setup
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        // Execute purchase
        val purchaseInput = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 100.0,
            purchasePrice = 10.0,
            supplierName = "Test Supplier",
            batchNumber = "LOT-001",
            userId = "test-user"
        )

        val result = sdk.purchaseUseCase.execute(purchaseInput)

        // Verify
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        // Verify batch was created
        val batches = sdk.purchaseBatchRepository.getByProductAndSite(product.id, site.id)
        assertEquals(1, batches.size)
        assertEquals(100.0, batches[0].initialQuantity)
        assertEquals(100.0, batches[0].remainingQuantity)
        assertEquals(10.0, batches[0].purchasePrice)

        // Verify stock movement was created
        val movements = sdk.stockMovementRepository.getByProduct(product.id)
        assertEquals(1, movements.size)
        assertEquals(100.0, movements[0].quantity)
        assertEquals(MovementType.PURCHASE, movements[0].type)
    }

    @Test
    fun `purchase with product margin calculates selling price`() = runTest {
        // Setup
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        // Product with 20% margin
        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
            .copy(marginType = "percentage", marginValue = 20.0)
        sdk.productRepository.insert(product)

        // Execute purchase at 100€
        val purchaseInput = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 100.0,
            userId = "test-user"
        )

        val result = sdk.purchaseUseCase.execute(purchaseInput)

        // Verify
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        // Selling price should be 100 * 1.20 = 120
        assertEquals(120.0, purchaseResult.calculatedSellingPrice)
    }

    // ========== COMPLETE WORKFLOW: PURCHASE → SALE ==========

    @Test
    fun `complete workflow - purchase then sale with profit calculation`() = runTest {
        // Setup
        val site = sdk.createSite("Pharmacy", "admin")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Paracetamol", site.id, packagingTypeId = packagingTypeId, userId = "admin")
        sdk.productRepository.insert(product)

        // Step 1: Purchase 100 units at 5€ each
        val purchaseInput = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 100.0,
            purchasePrice = 5.0,
            supplierName = "Pharma Supplier",
            userId = "admin"
        )
        val purchaseResult = sdk.purchaseUseCase.execute(purchaseInput)
        assertIs<UseCaseResult.Success<*>>(purchaseResult)

        // Step 2: Sell 30 units at 10€ each
        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Patient A",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 30.0,
                    unitPrice = 10.0
                )
            ),
            userId = "admin"
        )
        val saleResult = sdk.saleUseCase.execute(saleInput)
        assertIs<UseCaseResult.Success<*>>(saleResult)
        val sale = (saleResult as UseCaseResult.Success).data as SaleResult

        // Verify profit calculation
        // Cost: 30 * 5€ = 150€
        // Revenue: 30 * 10€ = 300€
        // Profit: 300€ - 150€ = 150€
        assertEquals(150.0, sale.totalCost)
        assertEquals(300.0, sale.totalRevenue)
        assertEquals(150.0, sale.grossProfit)

        // Verify remaining stock
        val batches = sdk.purchaseBatchRepository.getByProductAndSite(product.id, site.id)
        assertEquals(1, batches.size)
        assertEquals(70.0, batches[0].remainingQuantity) // 100 - 30
    }

    @Test
    fun `workflow with multiple purchases and sales`() = runTest {
        // Setup
        val site = sdk.createSite("Pharmacy", "admin")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Ibuprofen", site.id, packagingTypeId = packagingTypeId, userId = "admin")
        sdk.productRepository.insert(product)

        // Purchase 1: 50 units at 8€
        val purchase1 = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 8.0,
            supplierName = "Supplier A",
            userId = "admin"
        )
        sdk.purchaseUseCase.execute(purchase1)
        kotlinx.coroutines.delay(10)

        // Purchase 2: 50 units at 10€
        val purchase2 = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 10.0,
            supplierName = "Supplier B",
            userId = "admin"
        )
        sdk.purchaseUseCase.execute(purchase2)

        // Sale: 70 units at 15€ (should use FIFO: 50 at 8€ + 20 at 10€)
        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Patient B",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 70.0,
                    unitPrice = 15.0
                )
            ),
            userId = "admin"
        )
        val saleResult = sdk.saleUseCase.execute(saleInput)
        assertIs<UseCaseResult.Success<*>>(saleResult)
        val sale = (saleResult as UseCaseResult.Success).data as SaleResult

        // Verify FIFO cost calculation
        // Cost: (50 * 8€) + (20 * 10€) = 400€ + 200€ = 600€
        // Revenue: 70 * 15€ = 1050€
        // Profit: 1050€ - 600€ = 450€
        assertEquals(600.0, sale.totalCost)
        assertEquals(1050.0, sale.totalRevenue)
        assertEquals(450.0, sale.grossProfit)
    }

    // ========== TRANSFER WORKFLOW TESTS ==========

    @Test
    fun `transfer moves stock between sites with FIFO`() = runTest {
        // Setup: 2 sites
        val siteA = sdk.createSite("Site A", "admin")
        val siteB = sdk.createSite("Site B", "admin")
        sdk.siteRepository.insert(siteA)
        sdk.siteRepository.insert(siteB)

        val product = sdk.createProduct("Product X", siteA.id, packagingTypeId = packagingTypeId, userId = "admin")
        sdk.productRepository.insert(product)

        // Create batches on Site A
        val batch1 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = siteA.id,
            quantity = 40.0,
            purchasePrice = 5.0,
            userId = "admin"
        )
        sdk.purchaseBatchRepository.insert(batch1)
        kotlinx.coroutines.delay(10)

        val batch2 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = siteA.id,
            quantity = 60.0,
            purchasePrice = 7.0,
            userId = "admin"
        )
        sdk.purchaseBatchRepository.insert(batch2)

        // Execute transfer: 50 units from Site A to Site B
        val transferInput = TransferInput(
            productId = product.id,
            fromSiteId = siteA.id,
            toSiteId = siteB.id,
            quantity = 50.0,
            notes = "Stock rebalancing",
            userId = "admin"
        )
        val result = sdk.transferUseCase.execute(transferInput)

        // Verify
        assertIs<UseCaseResult.Success<*>>(result)
        val transferResult = (result as UseCaseResult.Success).data as TransferResult

        // Verify Site A batches (FIFO: batch1 exhausted, batch2 reduced by 10)
        val updatedBatch1 = sdk.purchaseBatchRepository.getById(batch1.id)
        val updatedBatch2 = sdk.purchaseBatchRepository.getById(batch2.id)

        assertNotNull(updatedBatch1)
        assertNotNull(updatedBatch2)
        assertEquals(0.0, updatedBatch1.remainingQuantity)
        assertTrue(updatedBatch1.isExhausted)
        assertEquals(50.0, updatedBatch2.remainingQuantity) // 60 - 10

        // Verify Site B has new batches
        val siteBBatches = sdk.purchaseBatchRepository.getByProductAndSite(product.id, siteB.id)
        assertEquals(2, siteBBatches.size) // 2 batches transferred
        val totalTransferred = siteBBatches.sumOf { it.remainingQuantity }
        assertEquals(50.0, totalTransferred)

        // Verify stock movements
        val movements = sdk.stockMovementRepository.getByProduct(product.id)
        val outMovement = movements.find { it.type == MovementType.TRANSFER_OUT }
        val inMovement = movements.find { it.type == MovementType.TRANSFER_IN }
        assertNotNull(outMovement)
        assertNotNull(inMovement)
        assertEquals(-50.0, outMovement.quantity)
        assertEquals(50.0, inMovement.quantity)
    }

    @Test
    fun `transfer to same site fails with error`() = runTest {
        // Setup
        val site = sdk.createSite("Site", "admin")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Product", site.id, packagingTypeId = packagingTypeId, userId = "admin")
        sdk.productRepository.insert(product)

        // Try to transfer to same site
        val transferInput = TransferInput(
            productId = product.id,
            fromSiteId = site.id,
            toSiteId = site.id, // Same site!
            quantity = 10.0,
            userId = "admin"
        )
        val result = sdk.transferUseCase.execute(transferInput)

        // Verify error
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.SameSiteTransfer>(error)
    }

    // ========== MULTI-SITE WORKFLOW ==========

    @Test
    fun `complete multi-site workflow - purchase, transfer, sell`() = runTest {
        // Setup: Central warehouse + Retail store
        val warehouse = sdk.createSite("Central Warehouse", "admin")
        val store = sdk.createSite("Retail Store", "admin")
        sdk.siteRepository.insert(warehouse)
        sdk.siteRepository.insert(store)

        val product = sdk.createProduct("Medicine A", warehouse.id, packagingTypeId = packagingTypeId, userId = "admin")
        sdk.productRepository.insert(product)

        // Step 1: Purchase at warehouse (100 units at 20€)
        val purchaseInput = PurchaseInput(
            productId = product.id,
            siteId = warehouse.id,
            quantity = 100.0,
            purchasePrice = 20.0,
            supplierName = "Main Supplier",
            userId = "admin"
        )
        sdk.purchaseUseCase.execute(purchaseInput)

        // Step 2: Transfer to store (60 units)
        val transferInput = TransferInput(
            productId = product.id,
            fromSiteId = warehouse.id,
            toSiteId = store.id,
            quantity = 60.0,
            notes = "Store replenishment",
            userId = "admin"
        )
        sdk.transferUseCase.execute(transferInput)

        // Step 3: Sell at store (25 units at 35€)
        val saleInput = SaleInput(
            siteId = store.id,
            customerName = "Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 25.0,
                    unitPrice = 35.0
                )
            ),
            userId = "admin"
        )
        val saleResult = sdk.saleUseCase.execute(saleInput)
        assertIs<UseCaseResult.Success<*>>(saleResult)
        val sale = (saleResult as UseCaseResult.Success).data as SaleResult

        // Verify final state
        // Warehouse: 100 - 60 = 40 units
        val warehouseBatches = sdk.purchaseBatchRepository.getByProductAndSite(product.id, warehouse.id)
        val warehouseStock = warehouseBatches.sumOf { it.remainingQuantity }
        assertEquals(40.0, warehouseStock)

        // Store: 60 - 25 = 35 units
        val storeBatches = sdk.purchaseBatchRepository.getByProductAndSite(product.id, store.id)
        val storeStock = storeBatches.sumOf { it.remainingQuantity }
        assertEquals(35.0, storeStock)

        // Profit: 25*35 - 25*20 = 875 - 500 = 375€
        assertEquals(500.0, sale.totalCost)
        assertEquals(875.0, sale.totalRevenue)
        assertEquals(375.0, sale.grossProfit)
    }

    // ========== AUDIT TRAIL TESTS ==========

    @Test
    fun `all operations create audit entries`() = runTest {
        // Setup
        val site = sdk.createSite("Site", "admin")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Product", site.id, packagingTypeId = packagingTypeId, userId = "admin")
        sdk.productRepository.insert(product)

        // Purchase
        val purchaseInput = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 50.0,
            purchasePrice = 10.0,
            userId = "admin"
        )
        sdk.purchaseUseCase.execute(purchaseInput)

        // Sale
        val saleInput = SaleInput(
            siteId = site.id,
            customerName = "Customer",
            items = listOf(
                SaleItemInput(
                    productId = product.id,
                    quantity = 10.0,
                    unitPrice = 15.0
                )
            ),
            userId = "admin"
        )
        sdk.saleUseCase.execute(saleInput)

        // Verify audit entries
        val audits = sdk.auditRepository.getAll()
        assertTrue(audits.size >= 2) // At least purchase + sale

        val purchaseAudit = audits.find { it.tableName == "purchase_batches" }
        val saleAudit = audits.find { it.tableName == "sales" }

        assertNotNull(purchaseAudit)
        assertNotNull(saleAudit)
        assertEquals("CREATE", purchaseAudit.action)
        assertEquals("CREATE", saleAudit.action)
        assertEquals("admin", purchaseAudit.userId)
        assertEquals("admin", saleAudit.userId)
    }
}
