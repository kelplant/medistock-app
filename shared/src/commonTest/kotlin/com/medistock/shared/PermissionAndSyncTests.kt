package com.medistock.shared

import com.medistock.shared.domain.model.Module
import com.medistock.shared.domain.model.UserPermission
import com.medistock.shared.domain.permission.ModulePermissions
import com.medistock.shared.domain.sync.SyncDirection
import com.medistock.shared.domain.sync.SyncEntity
import com.medistock.shared.domain.sync.SyncOrchestrator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModulePermissionsTest {
    @Test
    fun testForAdminGrantsAllPermissions() {
        val permissions = ModulePermissions.forAdmin(Module.PRODUCTS)

        assertEquals(Module.PRODUCTS, permissions.module)
        assertTrue(permissions.canView)
        assertTrue(permissions.canCreate)
        assertTrue(permissions.canEdit)
        assertTrue(permissions.canDelete)
    }

    @Test
    fun testNoAccessDeniesAllPermissions() {
        val permissions = ModulePermissions.noAccess(Module.SALES)

        assertEquals(Module.SALES, permissions.module)
        assertFalse(permissions.canView)
        assertFalse(permissions.canCreate)
        assertFalse(permissions.canEdit)
        assertFalse(permissions.canDelete)
    }

    @Test
    fun testFromPermissionWithValidPermission() {
        val userPermission = UserPermission(
            id = "perm-1",
            userId = "user-1",
            module = "products",
            canView = true,
            canCreate = true,
            canEdit = false,
            canDelete = false
        )

        val permissions = ModulePermissions.fromPermission(Module.PRODUCTS, userPermission)

        assertEquals(Module.PRODUCTS, permissions.module)
        assertTrue(permissions.canView)
        assertTrue(permissions.canCreate)
        assertFalse(permissions.canEdit)
        assertFalse(permissions.canDelete)
    }

    @Test
    fun testFromPermissionWithNullReturnsNoAccess() {
        val permissions = ModulePermissions.fromPermission(Module.PURCHASES, null)

        assertEquals(Module.PURCHASES, permissions.module)
        assertFalse(permissions.canView)
        assertFalse(permissions.canCreate)
        assertFalse(permissions.canEdit)
        assertFalse(permissions.canDelete)
    }
}

class ModuleEnumTest {
    @Test
    fun testAllModulesExist() {
        val modules = Module.entries
        assertTrue(modules.size >= 13, "Should have at least 13 modules")

        assertTrue(modules.any { it.name == "PRODUCTS" })
        assertTrue(modules.any { it.name == "SALES" })
        assertTrue(modules.any { it.name == "PURCHASES" })
        assertTrue(modules.any { it.name == "INVENTORY" })
    }

    @Test
    fun testModuleFromName() {
        assertEquals(Module.PRODUCTS, Module.fromName("products"))
        assertEquals(Module.SALES, Module.fromName("sales"))
        assertEquals(null, Module.fromName("invalid_module"))
    }
}

class SyncEntityTest {
    @Test
    fun testSyncOrderHasCorrectSize() {
        val order = SyncEntity.syncOrder()
        assertEquals(11, order.size, "Should have 11 sync entities")
    }

    @Test
    fun testSitesAreSyncedFirst() {
        val order = SyncEntity.syncOrder()
        assertEquals(SyncEntity.SITES, order.first())
    }

    @Test
    fun testStockMovementsAreSyncedLast() {
        val order = SyncEntity.syncOrder()
        assertEquals(SyncEntity.STOCK_MOVEMENTS, order.last())
    }

    @Test
    fun testDependencyOrder() {
        val order = SyncEntity.syncOrder()

        // Sites must come before Products (products have site_id)
        assertTrue(order.indexOf(SyncEntity.SITES) < order.indexOf(SyncEntity.PRODUCTS))

        // Categories must come before Products (products have category_id)
        assertTrue(order.indexOf(SyncEntity.CATEGORIES) < order.indexOf(SyncEntity.PRODUCTS))

        // Products must come before PurchaseBatches (batches have product_id)
        assertTrue(order.indexOf(SyncEntity.PRODUCTS) < order.indexOf(SyncEntity.PURCHASE_BATCHES))

        // Sales must come before SaleItems (items have sale_id)
        assertTrue(order.indexOf(SyncEntity.SALES) < order.indexOf(SyncEntity.SALE_ITEMS))
    }

    @Test
    fun testTableNames() {
        assertEquals("sites", SyncEntity.SITES.tableName)
        assertEquals("products", SyncEntity.PRODUCTS.tableName)
        assertEquals("app_users", SyncEntity.USERS.tableName)
        assertEquals("stock_movements", SyncEntity.STOCK_MOVEMENTS.tableName)
    }
}

class SyncOrchestratorTest {
    private val orchestrator = SyncOrchestrator()

    @Test
    fun testGetEntitiesToSync() {
        val entities = orchestrator.getEntitiesToSync()
        assertEquals(11, entities.size)
        assertEquals(SyncEntity.SITES, entities.first())
    }

    @Test
    fun testGetProgressMessageLocalToRemote() {
        val message = orchestrator.getProgressMessage(SyncEntity.PRODUCTS, SyncDirection.LOCAL_TO_REMOTE)
        assertTrue(message.contains("produits"))
        assertTrue(message.contains("Synchronisation"))
    }

    @Test
    fun testGetProgressMessageRemoteToLocal() {
        val message = orchestrator.getProgressMessage(SyncEntity.SALES, SyncDirection.REMOTE_TO_LOCAL)
        assertTrue(message.contains("ventes"))
        assertTrue(message.contains("Récupération"))
    }

    @Test
    fun testCalculateProgress() {
        assertEquals(10, orchestrator.calculateProgress(0, 10))
        assertEquals(50, orchestrator.calculateProgress(4, 10))
        assertEquals(100, orchestrator.calculateProgress(9, 10))
    }

    @Test
    fun testCalculateProgressWithZeroTotal() {
        assertEquals(0, orchestrator.calculateProgress(0, 0))
    }

    @Test
    fun testSuccessResult() {
        val result = orchestrator.successResult(SyncEntity.PRODUCTS, 42)
        assertTrue(result is com.medistock.shared.domain.sync.EntitySyncResult.Success)
        val success = result as com.medistock.shared.domain.sync.EntitySyncResult.Success
        assertEquals(SyncEntity.PRODUCTS, success.entity)
        assertEquals(42, success.itemsProcessed)
    }

    @Test
    fun testErrorResult() {
        val result = orchestrator.errorResult(SyncEntity.SALES, "Network error")
        assertTrue(result is com.medistock.shared.domain.sync.EntitySyncResult.Error)
        val error = result as com.medistock.shared.domain.sync.EntitySyncResult.Error
        assertEquals(SyncEntity.SALES, error.entity)
        assertEquals("Network error", error.error)
    }

    @Test
    fun testSkippedResult() {
        val result = orchestrator.skippedResult(SyncEntity.USERS, "No changes")
        assertTrue(result is com.medistock.shared.domain.sync.EntitySyncResult.Skipped)
        val skipped = result as com.medistock.shared.domain.sync.EntitySyncResult.Skipped
        assertEquals(SyncEntity.USERS, skipped.entity)
        assertEquals("No changes", skipped.reason)
    }

    @Test
    fun testCreateSyncResult() {
        val entityResults = listOf(
            orchestrator.successResult(SyncEntity.SITES, 5),
            orchestrator.successResult(SyncEntity.PRODUCTS, 10),
            orchestrator.errorResult(SyncEntity.SALES, "Error")
        )

        val result = orchestrator.createSyncResult(
            direction = SyncDirection.BIDIRECTIONAL,
            entityResults = entityResults,
            startTime = 1000L,
            endTime = 2000L
        )

        assertEquals(SyncDirection.BIDIRECTIONAL, result.direction)
        assertEquals(3, result.entityResults.size)
        assertEquals(1000L, result.durationMs)
        assertFalse(result.isSuccess)
        assertEquals(1, result.errors.size)
        assertEquals(2, result.successCount)
        assertEquals(15, result.totalItemsProcessed)
    }

    @Test
    fun testSyncResultIsSuccessWhenNoErrors() {
        val entityResults = listOf(
            orchestrator.successResult(SyncEntity.SITES, 5),
            orchestrator.successResult(SyncEntity.PRODUCTS, 10)
        )

        val result = orchestrator.createSyncResult(
            direction = SyncDirection.LOCAL_TO_REMOTE,
            entityResults = entityResults,
            startTime = 0L,
            endTime = 100L
        )

        assertTrue(result.isSuccess)
        assertEquals(0, result.errors.size)
    }

    @Test
    fun testCompletionMessages() {
        val local = orchestrator.getCompletionMessage(SyncDirection.LOCAL_TO_REMOTE)
        val remote = orchestrator.getCompletionMessage(SyncDirection.REMOTE_TO_LOCAL)
        val bidi = orchestrator.getCompletionMessage(SyncDirection.BIDIRECTIONAL)

        assertTrue(local.contains("Synchronisation"))
        assertTrue(remote.contains("Récupération"))
        assertTrue(bidi.contains("complète"))
    }
}
