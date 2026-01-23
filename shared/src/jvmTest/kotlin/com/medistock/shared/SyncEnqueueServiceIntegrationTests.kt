package com.medistock.shared

import com.medistock.shared.domain.model.SyncOperation
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for SyncEnqueueService deduplication logic.
 * These tests verify correct handling of operation sequences like:
 * - INSERT + UPDATE → keep INSERT with new payload
 * - INSERT + DELETE → cancel both
 * - UPDATE + UPDATE → replace with latest
 * - DELETE + UPDATE → ignore UPDATE
 */
class SyncEnqueueServiceIntegrationTests {

    private lateinit var sdk: MedistockSDK

    @BeforeEach
    fun setup() {
        sdk = MedistockSDK(DatabaseDriverFactory())
    }

    @Test
    fun `INSERT followed by UPDATE keeps INSERT with new payload`() = runTest {
        val entityType = "TestEntity"
        val entityId = "test-id-1"

        // First INSERT
        sdk.syncEnqueueService.enqueueInsert(
            entityType = entityType,
            entityId = entityId,
            payload = """{"name": "Original"}""",
            userId = "user-1"
        )

        // Then UPDATE (should update the INSERT payload)
        sdk.syncEnqueueService.enqueueUpdate(
            entityType = entityType,
            entityId = entityId,
            payload = """{"name": "Updated"}""",
            localVersion = 2,
            userId = "user-1"
        )

        // Verify only one item exists and it's an INSERT with updated payload
        val pending = sdk.syncQueueRepository.getLatestPendingForEntity(entityType, entityId)
        assertEquals(SyncOperation.INSERT, pending?.operation)
        assertTrue(pending?.payload?.contains("Updated") == true)
    }

    @Test
    fun `INSERT followed by DELETE cancels both`() = runTest {
        val entityType = "TestEntity"
        val entityId = "test-id-2"

        // First INSERT
        sdk.syncEnqueueService.enqueueInsert(
            entityType = entityType,
            entityId = entityId,
            payload = """{"name": "ToDelete"}""",
            userId = "user-1"
        )

        // Then DELETE (should cancel both since entity was never synced)
        sdk.syncEnqueueService.enqueueDelete(
            entityType = entityType,
            entityId = entityId,
            userId = "user-1"
        )

        // Verify no pending items
        val pending = sdk.syncQueueRepository.getLatestPendingForEntity(entityType, entityId)
        assertNull(pending)
    }

    @Test
    fun `UPDATE followed by UPDATE replaces with latest`() = runTest {
        val entityType = "TestEntity"
        val entityId = "test-id-3"

        // First UPDATE
        sdk.syncEnqueueService.enqueueUpdate(
            entityType = entityType,
            entityId = entityId,
            payload = """{"name": "First Update"}""",
            localVersion = 1,
            userId = "user-1"
        )

        // Second UPDATE (should replace)
        sdk.syncEnqueueService.enqueueUpdate(
            entityType = entityType,
            entityId = entityId,
            payload = """{"name": "Second Update"}""",
            localVersion = 2,
            userId = "user-1"
        )

        // Verify only latest UPDATE exists
        val pending = sdk.syncQueueRepository.getLatestPendingForEntity(entityType, entityId)
        assertEquals(SyncOperation.UPDATE, pending?.operation)
        assertTrue(pending?.payload?.contains("Second Update") == true)
        assertEquals(2L, pending?.localVersion)
    }

    @Test
    fun `DELETE followed by UPDATE is ignored`() = runTest {
        val entityType = "TestEntity"
        val entityId = "test-id-4"

        // First DELETE
        sdk.syncEnqueueService.enqueueDelete(
            entityType = entityType,
            entityId = entityId,
            userId = "user-1"
        )

        // Then UPDATE (should be ignored)
        sdk.syncEnqueueService.enqueueUpdate(
            entityType = entityType,
            entityId = entityId,
            payload = """{"name": "Ignored Update"}""",
            localVersion = 2,
            userId = "user-1"
        )

        // Verify DELETE is still there
        val pending = sdk.syncQueueRepository.getLatestPendingForEntity(entityType, entityId)
        assertEquals(SyncOperation.DELETE, pending?.operation)
    }

    @Test
    fun `multiple INSERTs for same entity keep latest`() = runTest {
        val entityType = "TestEntity"
        val entityId = "test-id-5"

        // First INSERT
        sdk.syncEnqueueService.enqueueInsert(
            entityType = entityType,
            entityId = entityId,
            payload = """{"version": 1}""",
            userId = "user-1"
        )

        // Second INSERT (should replace)
        sdk.syncEnqueueService.enqueueInsert(
            entityType = entityType,
            entityId = entityId,
            payload = """{"version": 2}""",
            userId = "user-1"
        )

        // Third INSERT (should replace again)
        sdk.syncEnqueueService.enqueueInsert(
            entityType = entityType,
            entityId = entityId,
            payload = """{"version": 3}""",
            userId = "user-1"
        )

        // Verify only latest INSERT exists
        val pending = sdk.syncQueueRepository.getLatestPendingForEntity(entityType, entityId)
        assertEquals(SyncOperation.INSERT, pending?.operation)
        assertTrue(pending?.payload?.contains("version\": 3") == true || pending?.payload?.contains("\"version\":3") == true)
    }

    @Test
    fun `operations for different entities remain independent`() = runTest {
        val entityType = "TestEntity"
        val entityId1 = "entity-a"
        val entityId2 = "entity-b"

        // INSERT for entity A
        sdk.syncEnqueueService.enqueueInsert(
            entityType = entityType,
            entityId = entityId1,
            payload = """{"name": "Entity A"}""",
            userId = "user-1"
        )

        // INSERT for entity B
        sdk.syncEnqueueService.enqueueInsert(
            entityType = entityType,
            entityId = entityId2,
            payload = """{"name": "Entity B"}""",
            userId = "user-1"
        )

        // Verify both exist independently
        val pendingA = sdk.syncQueueRepository.getLatestPendingForEntity(entityType, entityId1)
        val pendingB = sdk.syncQueueRepository.getLatestPendingForEntity(entityType, entityId2)

        assertTrue(pendingA?.payload?.contains("Entity A") == true)
        assertTrue(pendingB?.payload?.contains("Entity B") == true)
    }

    @Test
    fun `enqueue preserves entity type`() = runTest {
        sdk.syncEnqueueService.enqueueInsert(
            entityType = "Product",
            entityId = "prod-1",
            payload = """{}""",
            userId = "user-1"
        )

        val pending = sdk.syncQueueRepository.getLatestPendingForEntity("Product", "prod-1")
        assertEquals("Product", pending?.entityType)
    }

    @Test
    fun `enqueue preserves site ID`() = runTest {
        sdk.syncEnqueueService.enqueueInsert(
            entityType = "TestEntity",
            entityId = "test-id-6",
            payload = """{}""",
            userId = "user-1",
            siteId = "site-123"
        )

        val pending = sdk.syncQueueRepository.getLatestPendingForEntity("TestEntity", "test-id-6")
        assertEquals("site-123", pending?.siteId)
    }

    @Test
    fun `enqueue preserves user ID`() = runTest {
        sdk.syncEnqueueService.enqueueInsert(
            entityType = "TestEntity",
            entityId = "test-id-7",
            payload = """{}""",
            userId = "user-456"
        )

        val pending = sdk.syncQueueRepository.getLatestPendingForEntity("TestEntity", "test-id-7")
        assertEquals("user-456", pending?.userId)
    }

    @Test
    fun `pending count returns correct number`() = runTest {
        // Add 3 items
        sdk.syncEnqueueService.enqueueInsert("Type1", "id-1", "{}", "user")
        sdk.syncEnqueueService.enqueueInsert("Type2", "id-2", "{}", "user")
        sdk.syncEnqueueService.enqueueInsert("Type3", "id-3", "{}", "user")

        val count = sdk.syncEnqueueService.getPendingCount()
        assertEquals(3L, count)
    }

    @Test
    fun `hasPendingOperations returns true when exists`() = runTest {
        sdk.syncEnqueueService.enqueueInsert("TestEntity", "exists-id", "{}", "user")

        assertTrue(sdk.syncEnqueueService.hasPendingOperations("TestEntity", "exists-id"))
        assertFalse(sdk.syncEnqueueService.hasPendingOperations("TestEntity", "not-exists"))
    }

    @Test
    fun `enqueue product insert creates correct queue item`() = runTest {
        val site = sdk.createSite("Test Site", "user")
        sdk.siteRepository.insert(site)

        val product = sdk.createProduct("Test Product", site.id, userId = "user")
        sdk.productRepository.insert(product)

        sdk.syncEnqueueService.enqueueProductInsert(product, "user")

        val pending = sdk.syncQueueRepository.getLatestPendingForEntity("Product", product.id)
        assertEquals(SyncOperation.INSERT, pending?.operation)
        assertEquals("Product", pending?.entityType)
        assertEquals(product.id, pending?.entityId)
        assertEquals(site.id, pending?.siteId)
    }
}
