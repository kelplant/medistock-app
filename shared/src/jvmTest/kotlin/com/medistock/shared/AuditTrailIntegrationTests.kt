package com.medistock.shared

import com.medistock.shared.domain.usecase.PurchaseInput
import com.medistock.shared.domain.usecase.PurchaseResult
import com.medistock.shared.domain.usecase.SaleInput
import com.medistock.shared.domain.usecase.SaleItemInput
import com.medistock.shared.domain.usecase.SaleResult
import com.medistock.shared.domain.usecase.TransferInput
import com.medistock.shared.domain.usecase.TransferResult
import com.medistock.shared.domain.usecase.common.UseCaseResult
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Integration tests for audit trail functionality.
 * These tests verify that all critical operations create proper audit entries:
 * - Table name tracking
 * - Action type recording
 * - User ID association
 * - Timestamp accuracy
 * - Multiple operation tracking
 */
class AuditTrailIntegrationTests {

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
    fun `should_createAuditEntryForPurchaseBatches_when_purchaseIsExecuted`() = runTest {
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
            userId = "audit-user-123"
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
    }

    @Test
    fun `should_createAuditEntryForSales_when_saleIsExecuted`() = runTest {
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
            userId = "audit-user-456"
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
    }

    @Test
    fun `should_createAuditEntryForProductTransfers_when_transferIsExecuted`() = runTest {
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
            quantity = 30.0,
            userId = "audit-user-789"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val transferResult = (result as UseCaseResult.Success).data as TransferResult

        val auditEntries = sdk.auditRepository.getByTable("product_transfers", limit = 100)
            .filter { it.recordId == transferResult.transfer.id }

        assertTrue(auditEntries.isNotEmpty())
        val auditEntry = auditEntries.first()
        assertEquals("product_transfers", auditEntry.tableName)
    }

    @Test
    fun `should_recordUserId_when_auditEntryIsCreated`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 20.0,
            userId = "specific-user-id"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        val auditEntries = sdk.auditRepository.getByTable("purchase_batches", limit = 100)
            .filter { it.recordId == purchaseResult.purchaseBatch.id }

        val auditEntry = auditEntries.first()
        assertEquals("specific-user-id", auditEntry.userId)
    }

    @Test
    fun `should_recordCreateAction_when_auditEntryIsCreated`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 20.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        val auditEntries = sdk.auditRepository.getByTable("purchase_batches", limit = 100)
            .filter { it.recordId == purchaseResult.purchaseBatch.id }

        val auditEntry = auditEntries.first()
        assertEquals("CREATE", auditEntry.action)
    }

    @Test
    fun `should_recordTimestampNearNow_when_auditEntryIsCreated`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val beforeTime = Clock.System.now().toEpochMilliseconds()

        val input = PurchaseInput(
            productId = product.id,
            siteId = site.id,
            quantity = 10.0,
            purchasePrice = 20.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.purchaseUseCase.execute(input)

        val afterTime = Clock.System.now().toEpochMilliseconds()

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult

        val auditEntries = sdk.auditRepository.getByTable("purchase_batches", limit = 100)
            .filter { it.recordId == purchaseResult.purchaseBatch.id }

        val auditEntry = auditEntries.first()
        assertTrue(auditEntry.timestamp >= beforeTime)
        assertTrue(auditEntry.timestamp <= afterTime)
    }

    @Test
    fun `should_createMultipleAuditEntries_when_multipleOperationsAreExecuted`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        // Execute multiple purchases
        for (i in 1..3) {
            val input = PurchaseInput(
                productId = product.id,
                siteId = site.id,
                quantity = 10.0 * i,
                purchasePrice = 20.0,
                userId = "test-user"
            )
            sdk.purchaseUseCase.execute(input)
        }

        // Act
        val allAuditEntries = sdk.auditRepository.getByTable("purchase_batches", limit = 100)

        // Assert
        assertTrue(allAuditEntries.size >= 3)
        assertTrue(allAuditEntries.all { it.action == "CREATE" })
        assertTrue(allAuditEntries.all { it.tableName == "purchase_batches" })
    }

    @Test
    fun `should_includeNewValues_when_auditEntryIsCreated`() = runTest {
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

        val auditEntries = sdk.auditRepository.getByTable("purchase_batches", limit = 100)
            .filter { it.recordId == purchaseResult.purchaseBatch.id }

        val auditEntry = auditEntries.first()
        assertNotNull(auditEntry.newValues)
        assertTrue(auditEntry.newValues?.contains(product.id) == true)
    }

    @Test
    fun `should_haveDifferentRecordIds_when_multipleOperationsAreExecuted`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val recordIds = mutableSetOf<String>()

        // Execute multiple purchases
        for (i in 1..5) {
            val input = PurchaseInput(
                productId = product.id,
                siteId = site.id,
                quantity = 10.0,
                purchasePrice = 20.0,
                userId = "test-user"
            )
            val result = sdk.purchaseUseCase.execute(input)
            assertIs<UseCaseResult.Success<*>>(result)
            val purchaseResult = (result as UseCaseResult.Success).data as PurchaseResult
            recordIds.add(purchaseResult.purchaseBatch.id)
        }

        // Assert
        assertEquals(5, recordIds.size) // All record IDs should be unique
    }

    @Test
    fun `should_createAuditForSaleAndTransfer_when_bothOperationsAreExecuted`() = runTest {
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

        // Execute sale
        val saleInput = SaleInput(
            siteId = sourceSite.id,
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
        sdk.saleUseCase.execute(saleInput)

        // Execute transfer
        val transferInput = TransferInput(
            productId = product.id,
            fromSiteId = sourceSite.id,
            toSiteId = destSite.id,
            quantity = 30.0,
            userId = "test-user"
        )
        sdk.transferUseCase.execute(transferInput)

        // Assert
        val saleAudits = sdk.auditRepository.getByTable("sales", limit = 100)
        val transferAudits = sdk.auditRepository.getByTable("product_transfers", limit = 100)

        assertTrue(saleAudits.isNotEmpty())
        assertTrue(transferAudits.isNotEmpty())
    }
}
