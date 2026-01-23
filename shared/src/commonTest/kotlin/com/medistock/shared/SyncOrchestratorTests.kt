package com.medistock.shared

import com.medistock.shared.domain.sync.SyncEntity
import com.medistock.shared.domain.sync.SyncOrchestrator
import com.medistock.shared.domain.sync.SyncDirection
import com.medistock.shared.domain.sync.EntitySyncResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for SyncOrchestrator.
 * These tests verify:
 * - Sync entity ordering (dependency management)
 * - Progress calculation
 * - Result creation
 */
class SyncOrchestratorTests {

    private val orchestrator = SyncOrchestrator()

    @Test
    fun `should_haveOrderForAllEntities_when_gettingSyncOrder`() {
        // Act
        val entities = orchestrator.getEntitiesToSync()

        // Assert
        assertTrue(entities.isNotEmpty())
        assertEquals(SyncEntity.values().size, entities.size)
    }

    @Test
    fun `should_placeSitesFirst_when_gettingSyncOrder`() {
        // Act
        val entities = orchestrator.getEntitiesToSync()

        // Assert
        assertEquals(SyncEntity.SITES, entities.first())
    }

    @Test
    fun `should_placeProductsAfterSitesAndCategories_when_gettingSyncOrder`() {
        // Act
        val entities = orchestrator.getEntitiesToSync()

        // Assert
        val sitesIndex = entities.indexOf(SyncEntity.SITES)
        val categoriesIndex = entities.indexOf(SyncEntity.CATEGORIES)
        val productsIndex = entities.indexOf(SyncEntity.PRODUCTS)

        assertTrue(productsIndex > sitesIndex, "Products must come after Sites")
        assertTrue(productsIndex > categoriesIndex, "Products must come after Categories")
    }

    @Test
    fun `should_placeSalesAfterProducts_when_gettingSyncOrder`() {
        // Act
        val entities = orchestrator.getEntitiesToSync()

        // Assert
        val productsIndex = entities.indexOf(SyncEntity.PRODUCTS)
        val salesIndex = entities.indexOf(SyncEntity.SALES)

        assertTrue(salesIndex > productsIndex, "Sales must come after Products")
    }

    @Test
    fun `should_placeSaleItemsAfterSales_when_gettingSyncOrder`() {
        // Act
        val entities = orchestrator.getEntitiesToSync()

        // Assert
        val salesIndex = entities.indexOf(SyncEntity.SALES)
        val saleItemsIndex = entities.indexOf(SyncEntity.SALE_ITEMS)

        assertTrue(saleItemsIndex > salesIndex, "Sale Items must come after Sales")
    }

    @Test
    fun `should_calculate0Percent_when_progressAtStart`() {
        // Arrange
        val currentIndex = 0
        val totalEntities = 10

        // Act
        val progress = orchestrator.calculateProgress(currentIndex, totalEntities)

        // Assert
        assertEquals(10, progress) // (0+1)*100/10 = 10%
    }

    @Test
    fun `should_calculate50Percent_when_progressAtMiddle`() {
        // Arrange
        val currentIndex = 4
        val totalEntities = 10

        // Act
        val progress = orchestrator.calculateProgress(currentIndex, totalEntities)

        // Assert
        assertEquals(50, progress) // (4+1)*100/10 = 50%
    }

    @Test
    fun `should_calculate100Percent_when_progressAtEnd`() {
        // Arrange
        val currentIndex = 9
        val totalEntities = 10

        // Act
        val progress = orchestrator.calculateProgress(currentIndex, totalEntities)

        // Assert
        assertEquals(100, progress) // (9+1)*100/10 = 100%
    }

    @Test
    fun `should_calculate0Percent_when_totalEntitiesIsZero`() {
        // Arrange
        val currentIndex = 0
        val totalEntities = 0

        // Act
        val progress = orchestrator.calculateProgress(currentIndex, totalEntities)

        // Assert
        assertEquals(0, progress)
    }

    @Test
    fun `should_generateProgressMessage_when_localToRemoteDirection`() {
        // Act
        val message = orchestrator.getProgressMessage(SyncEntity.PRODUCTS, SyncDirection.LOCAL_TO_REMOTE)

        // Assert
        assertTrue(message.contains("produits"))
        assertTrue(message.contains("Synchronisation"))
    }

    @Test
    fun `should_generateProgressMessage_when_remoteToLocalDirection`() {
        // Act
        val message = orchestrator.getProgressMessage(SyncEntity.SITES, SyncDirection.REMOTE_TO_LOCAL)

        // Assert
        assertTrue(message.contains("sites"))
        assertTrue(message.contains("Récupération"))
    }

    @Test
    fun `should_createSuccessResult_when_entitySyncSucceeds`() {
        // Act
        val result = orchestrator.successResult(SyncEntity.PRODUCTS, 42)

        // Assert
        assertTrue(result is EntitySyncResult.Success)
        assertEquals(SyncEntity.PRODUCTS, (result as EntitySyncResult.Success).entity)
        assertEquals(42, result.itemsProcessed)
    }

    @Test
    fun `should_createErrorResult_when_entitySyncFails`() {
        // Act
        val exception = Exception("Test error")
        val result = orchestrator.errorResult(SyncEntity.SALES, "Failed to sync", exception)

        // Assert
        assertTrue(result is EntitySyncResult.Error)
        assertEquals(SyncEntity.SALES, (result as EntitySyncResult.Error).entity)
        assertEquals("Failed to sync", result.error)
        assertEquals(exception, result.exception)
    }

    @Test
    fun `should_createSkippedResult_when_entityIsSkipped`() {
        // Act
        val result = orchestrator.skippedResult(SyncEntity.CUSTOMERS, "No changes")

        // Assert
        assertTrue(result is EntitySyncResult.Skipped)
        assertEquals(SyncEntity.CUSTOMERS, (result as EntitySyncResult.Skipped).entity)
        assertEquals("No changes", result.reason)
    }

    @Test
    fun `should_createSyncResultWithSuccess_when_allEntitiesSucceed`() {
        // Arrange
        val entityResults = listOf(
            EntitySyncResult.Success(SyncEntity.SITES, 5),
            EntitySyncResult.Success(SyncEntity.PRODUCTS, 10)
        )
        val startTime = 1000L
        val endTime = 2000L

        // Act
        val syncResult = orchestrator.createSyncResult(
            direction = SyncDirection.BIDIRECTIONAL,
            entityResults = entityResults,
            startTime = startTime,
            endTime = endTime
        )

        // Assert
        assertTrue(syncResult.isSuccess)
        assertEquals(2, syncResult.successCount)
        assertEquals(15, syncResult.totalItemsProcessed)
        assertEquals(1000L, syncResult.durationMs)
        assertTrue(syncResult.errors.isEmpty())
    }

    @Test
    fun `should_createSyncResultWithError_when_someEntitiesFail`() {
        // Arrange
        val entityResults = listOf(
            EntitySyncResult.Success(SyncEntity.SITES, 5),
            EntitySyncResult.Error(SyncEntity.PRODUCTS, "Failed"),
            EntitySyncResult.Success(SyncEntity.SALES, 3)
        )
        val startTime = 1000L
        val endTime = 3000L

        // Act
        val syncResult = orchestrator.createSyncResult(
            direction = SyncDirection.LOCAL_TO_REMOTE,
            entityResults = entityResults,
            startTime = startTime,
            endTime = endTime
        )

        // Assert
        assertFalse(syncResult.isSuccess)
        assertEquals(2, syncResult.successCount)
        assertEquals(8, syncResult.totalItemsProcessed) // Only successful ones
        assertEquals(1, syncResult.errors.size)
        assertEquals(2000L, syncResult.durationMs)
    }

    @Test
    fun `should_generateCompletionMessage_when_syncCompletes`() {
        // Act
        val localToRemoteMsg = orchestrator.getCompletionMessage(SyncDirection.LOCAL_TO_REMOTE)
        val remoteToLocalMsg = orchestrator.getCompletionMessage(SyncDirection.REMOTE_TO_LOCAL)
        val bidirectionalMsg = orchestrator.getCompletionMessage(SyncDirection.BIDIRECTIONAL)

        // Assert
        assertTrue(localToRemoteMsg.contains("Synchronisation"))
        assertTrue(remoteToLocalMsg.contains("Récupération"))
        assertTrue(bidirectionalMsg.contains("complète"))
    }

    @Test
    fun `should_maintainDependencyOrder_when_verifyingFullSyncOrder`() {
        // Act
        val entities = orchestrator.getEntitiesToSync()

        // Assert - Verify key dependency relationships
        val sitesIndex = entities.indexOf(SyncEntity.SITES)
        val categoriesIndex = entities.indexOf(SyncEntity.CATEGORIES)
        val productsIndex = entities.indexOf(SyncEntity.PRODUCTS)
        val purchaseBatchesIndex = entities.indexOf(SyncEntity.PURCHASE_BATCHES)
        val salesIndex = entities.indexOf(SyncEntity.SALES)
        val saleItemsIndex = entities.indexOf(SyncEntity.SALE_ITEMS)

        // Sites must be first
        assertEquals(0, sitesIndex)

        // Products depend on Sites and Categories
        assertTrue(productsIndex > sitesIndex)
        assertTrue(productsIndex > categoriesIndex)

        // Purchase batches depend on Products
        assertTrue(purchaseBatchesIndex > productsIndex)

        // Sales depend on Products
        assertTrue(salesIndex > productsIndex)

        // Sale items depend on Sales
        assertTrue(saleItemsIndex > salesIndex)
    }

    @Test
    fun `should_calculateProgressAtVariousStages_when_syncProgresses`() {
        // Arrange
        val entities = orchestrator.getEntitiesToSync()
        val totalEntities = entities.size

        // Act & Assert - Test progress at different stages
        val progress25 = orchestrator.calculateProgress(totalEntities / 4, totalEntities)
        val progress50 = orchestrator.calculateProgress(totalEntities / 2, totalEntities)
        val progress75 = orchestrator.calculateProgress((totalEntities * 3) / 4, totalEntities)

        assertTrue(progress25 >= 20 && progress25 <= 35)
        assertTrue(progress50 >= 45 && progress50 <= 60)
        assertTrue(progress75 >= 70 && progress75 <= 85)
    }
}
