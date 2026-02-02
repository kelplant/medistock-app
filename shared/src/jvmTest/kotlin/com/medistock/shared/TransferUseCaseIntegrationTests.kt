package com.medistock.shared

import com.medistock.shared.domain.usecase.TransferInput
import com.medistock.shared.domain.usecase.TransferResult
import com.medistock.shared.domain.usecase.MovementType
import com.medistock.shared.domain.usecase.common.BusinessError
import com.medistock.shared.domain.usecase.common.BusinessWarning
import com.medistock.shared.domain.usecase.common.UseCaseResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Integration tests for TransferUseCase.
 * These tests verify the complete transfer workflow including:
 * - Stock movement between sites
 * - Batch creation on destination
 * - FIFO batch selection
 * - Purchase date and price preservation
 * - Stock movement generation (TRANSFER_OUT/TRANSFER_IN)
 * - Validation logic
 * - Audit trail creation
 */
class TransferUseCaseIntegrationTests {

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
    fun `should_moveStockBetweenSites_when_transferIsExecuted`() = runTest {
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
            quantity = 40.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)

        // Verify source batch quantity decreased
        val updatedSourceBatch = sdk.purchaseBatchRepository.getById(batch.id)
        assertEquals(60.0, updatedSourceBatch?.remainingQuantity)

        // Verify destination has stock
        val destBatches = sdk.purchaseBatchRepository.getByProductAndSite(product.id, destSite.id)
        assertTrue(destBatches.isNotEmpty())
        assertEquals(40.0, destBatches.sumOf { it.remainingQuantity })
    }

    @Test
    fun `should_createBatchOnDestination_when_transferIsExecuted`() = runTest {
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
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)

        val destBatches = sdk.purchaseBatchRepository.getByProductAndSite(product.id, destSite.id)
        assertEquals(1, destBatches.size)
        assertEquals(30.0, destBatches.first().initialQuantity)
        assertEquals(30.0, destBatches.first().remainingQuantity)
        assertFalse(destBatches.first().isExhausted)
    }

    @Test
    fun `should_preservePurchaseDateForFIFO_when_transferIsExecuted`() = runTest {
        // Arrange
        val sourceSite = sdk.createSite("Source Site", "test-user")
        sdk.siteRepository.insert(sourceSite)

        val destSite = sdk.createSite("Destination Site", "test-user")
        sdk.siteRepository.insert(destSite)

        val product = sdk.createProduct("Test Product", sourceSite.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val sourceBatch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = sourceSite.id,
            quantity = 50.0,
            purchasePrice = 15.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(sourceBatch)

        val input = TransferInput(
            productId = product.id,
            fromSiteId = sourceSite.id,
            toSiteId = destSite.id,
            quantity = 25.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)

        val destBatches = sdk.purchaseBatchRepository.getByProductAndSite(product.id, destSite.id)
        val destBatch = destBatches.first()

        // Purchase date must be preserved for FIFO ordering
        assertEquals(sourceBatch.purchaseDate, destBatch.purchaseDate)
    }

    @Test
    fun `should_preservePurchasePrice_when_transferIsExecuted`() = runTest {
        // Arrange
        val sourceSite = sdk.createSite("Source Site", "test-user")
        sdk.siteRepository.insert(sourceSite)

        val destSite = sdk.createSite("Destination Site", "test-user")
        sdk.siteRepository.insert(destSite)

        val product = sdk.createProduct("Test Product", sourceSite.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val sourceBatch = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = sourceSite.id,
            quantity = 50.0,
            purchasePrice = 22.50,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(sourceBatch)

        val input = TransferInput(
            productId = product.id,
            fromSiteId = sourceSite.id,
            toSiteId = destSite.id,
            quantity = 20.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)

        val destBatches = sdk.purchaseBatchRepository.getByProductAndSite(product.id, destSite.id)
        val destBatch = destBatches.first()

        assertEquals(22.50, destBatch.purchasePrice)
    }

    @Test
    fun `should_createTransferOutMovement_when_transferIsExecuted`() = runTest {
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
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val transferResult = (result as UseCaseResult.Success).data as TransferResult

        assertEquals(MovementType.TRANSFER_OUT, transferResult.sourceMovement.type)
        assertEquals(-30.0, transferResult.sourceMovement.quantity) // Negative for outgoing
        assertEquals(sourceSite.id, transferResult.sourceMovement.siteId)
    }

    @Test
    fun `should_createTransferInMovement_when_transferIsExecuted`() = runTest {
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
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)
        val transferResult = (result as UseCaseResult.Success).data as TransferResult

        assertEquals(MovementType.TRANSFER_IN, transferResult.destinationMovement.type)
        assertEquals(30.0, transferResult.destinationMovement.quantity) // Positive for incoming
        assertEquals(destSite.id, transferResult.destinationMovement.siteId)
    }

    @Test
    fun `should_failWithSameSiteTransfer_when_transferringToSameSite`() = runTest {
        // Arrange
        val site = sdk.createSite("Test Site", "test-user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = TransferInput(
            productId = product.id,
            fromSiteId = site.id,
            toSiteId = site.id,
            quantity = 10.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.SameSiteTransfer>(error)
    }

    @Test
    fun `should_succeedWithWarning_when_transferringWithInsufficientStock`() = runTest {
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
            quantity = 20.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch)

        val input = TransferInput(
            productId = product.id,
            fromSiteId = sourceSite.id,
            toSiteId = destSite.id,
            quantity = 50.0, // More than available
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
        assertEquals(20.0, warning.available)
    }

    @Test
    fun `should_useFIFOForSourceBatchSelection_when_multipleSourceBatchesExist`() = runTest {
        // Arrange
        val sourceSite = sdk.createSite("Source Site", "test-user")
        sdk.siteRepository.insert(sourceSite)

        val destSite = sdk.createSite("Destination Site", "test-user")
        sdk.siteRepository.insert(destSite)

        val product = sdk.createProduct("Test Product", sourceSite.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        // Oldest batch
        val batch1 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = sourceSite.id,
            quantity = 30.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch1)
        kotlinx.coroutines.delay(10)

        // Newer batch
        val batch2 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = sourceSite.id,
            quantity = 50.0,
            purchasePrice = 15.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch2)

        val input = TransferInput(
            productId = product.id,
            fromSiteId = sourceSite.id,
            toSiteId = destSite.id,
            quantity = 25.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)

        // Verify oldest batch (batch1) was used first
        val updatedBatch1 = sdk.purchaseBatchRepository.getById(batch1.id)
        val updatedBatch2 = sdk.purchaseBatchRepository.getById(batch2.id)

        assertEquals(5.0, updatedBatch1?.remainingQuantity) // 30 - 25 = 5
        assertEquals(50.0, updatedBatch2?.remainingQuantity) // Untouched
    }

    @Test
    fun `should_createMultipleDestBatches_when_transferSpansMultipleSourceBatches`() = runTest {
        // Arrange
        val sourceSite = sdk.createSite("Source Site", "test-user")
        sdk.siteRepository.insert(sourceSite)

        val destSite = sdk.createSite("Destination Site", "test-user")
        sdk.siteRepository.insert(destSite)

        val product = sdk.createProduct("Test Product", sourceSite.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val batch1 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = sourceSite.id,
            quantity = 20.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch1)
        kotlinx.coroutines.delay(10)

        val batch2 = sdk.createPurchaseBatch(
            productId = product.id,
            siteId = sourceSite.id,
            quantity = 30.0,
            purchasePrice = 12.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch2)

        val input = TransferInput(
            productId = product.id,
            fromSiteId = sourceSite.id,
            toSiteId = destSite.id,
            quantity = 35.0, // Will span both batches
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Success<*>>(result)

        val destBatches = sdk.purchaseBatchRepository.getByProductAndSite(product.id, destSite.id)
        assertEquals(2, destBatches.size) // Two destination batches created
        assertEquals(35.0, destBatches.sumOf { it.remainingQuantity })
    }

    @Test
    fun `should_rejectWithValidationError_when_quantityIsNegative`() = runTest {
        // Arrange
        val sourceSite = sdk.createSite("Source Site", "test-user")
        sdk.siteRepository.insert(sourceSite)

        val destSite = sdk.createSite("Destination Site", "test-user")
        sdk.siteRepository.insert(destSite)

        val product = sdk.createProduct("Test Product", sourceSite.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = TransferInput(
            productId = product.id,
            fromSiteId = sourceSite.id,
            toSiteId = destSite.id,
            quantity = -10.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.ValidationError>(error)
    }

    @Test
    fun `should_createAuditEntry_when_transferIsExecuted`() = runTest {
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
            userId = "test-user"
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
        assertEquals("CREATE", auditEntry.action)
        assertEquals("test-user", auditEntry.userId)
    }

    @Test
    fun `should_failWithNotFound_when_sourceSiteDoesNotExist`() = runTest {
        // Arrange
        val destSite = sdk.createSite("Destination Site", "test-user")
        sdk.siteRepository.insert(destSite)

        val product = sdk.createProduct("Test Product", destSite.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = TransferInput(
            productId = product.id,
            fromSiteId = "non-existent-site",
            toSiteId = destSite.id,
            quantity = 10.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.NotFound>(error)
    }

    @Test
    fun `should_failWithNotFound_when_destSiteDoesNotExist`() = runTest {
        // Arrange
        val sourceSite = sdk.createSite("Source Site", "test-user")
        sdk.siteRepository.insert(sourceSite)

        val product = sdk.createProduct("Test Product", sourceSite.id, packagingTypeId = packagingTypeId, userId = "test-user")
        sdk.productRepository.insert(product)

        val input = TransferInput(
            productId = product.id,
            fromSiteId = sourceSite.id,
            toSiteId = "non-existent-site",
            quantity = 10.0,
            userId = "test-user"
        )

        // Act
        val result = sdk.transferUseCase.execute(input)

        // Assert
        assertIs<UseCaseResult.Error>(result)
        val error = (result as UseCaseResult.Error).error
        assertIs<BusinessError.NotFound>(error)
    }

    @Test
    fun `should_exhaustSourceBatch_when_transferExactQuantity`() = runTest {
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
            quantity = 50.0,
            purchasePrice = 10.0,
            userId = "test-user"
        )
        sdk.purchaseBatchRepository.insert(batch)

        val input = TransferInput(
            productId = product.id,
            fromSiteId = sourceSite.id,
            toSiteId = destSite.id,
            quantity = 50.0, // Exact amount
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
}
