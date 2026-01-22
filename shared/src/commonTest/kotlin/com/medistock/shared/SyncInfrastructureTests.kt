package com.medistock.shared

import com.medistock.shared.domain.sync.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RetryConfigurationTest {
    @Test
    fun testDefaultConfiguration() {
        val config = RetryConfiguration.DEFAULT

        assertEquals(5, config.maxRetries)
        assertEquals(10, config.batchSize)
        assertEquals(5, config.backoffDelaysMs.size)
        assertEquals(30000L, config.syncIntervalMs)
    }

    @Test
    fun testGetDelayMs() {
        val config = RetryConfiguration()

        assertEquals(1000L, config.getDelayMs(0))
        assertEquals(2000L, config.getDelayMs(1))
        assertEquals(4000L, config.getDelayMs(2))
        assertEquals(8000L, config.getDelayMs(3))
        assertEquals(16000L, config.getDelayMs(4))
        // Beyond array size returns last value
        assertEquals(16000L, config.getDelayMs(10))
    }

    @Test
    fun testGetDelaySeconds() {
        val config = RetryConfiguration()

        assertEquals(1.0, config.getDelaySeconds(0))
        assertEquals(2.0, config.getDelaySeconds(1))
        assertEquals(16.0, config.getDelaySeconds(4))
    }

    @Test
    fun testShouldRetry() {
        val config = RetryConfiguration(maxRetries = 3)

        assertTrue(config.shouldRetry(0))
        assertTrue(config.shouldRetry(1))
        assertTrue(config.shouldRetry(2))
        assertFalse(config.shouldRetry(3))
        assertFalse(config.shouldRetry(5))
    }

    @Test
    fun testCustomConfiguration() {
        val config = RetryConfiguration(
            maxRetries = 3,
            backoffDelaysMs = listOf(500L, 1000L, 2000L),
            batchSize = 5,
            syncIntervalMs = 60000L
        )

        assertEquals(3, config.maxRetries)
        assertEquals(5, config.batchSize)
        assertEquals(60000L, config.syncIntervalMs)
        assertEquals(500L, config.getDelayMs(0))
        assertEquals(2000L, config.getDelayMs(2))
    }
}

class ConflictResolverTest {
    private val resolver = ConflictResolver()

    @Test
    fun testGetStrategyForReferenceData() {
        assertEquals(ConflictResolution.REMOTE_WINS, resolver.getStrategy("Product"))
        assertEquals(ConflictResolution.REMOTE_WINS, resolver.getStrategy("products"))
        assertEquals(ConflictResolution.REMOTE_WINS, resolver.getStrategy("Category"))
        assertEquals(ConflictResolution.REMOTE_WINS, resolver.getStrategy("Site"))
        assertEquals(ConflictResolution.REMOTE_WINS, resolver.getStrategy("PackagingType"))
    }

    @Test
    fun testGetStrategyForSales() {
        assertEquals(ConflictResolution.LOCAL_WINS, resolver.getStrategy("Sale"))
        assertEquals(ConflictResolution.LOCAL_WINS, resolver.getStrategy("sales"))
        assertEquals(ConflictResolution.LOCAL_WINS, resolver.getStrategy("SaleItem"))
        assertEquals(ConflictResolution.LOCAL_WINS, resolver.getStrategy("SaleBatchAllocation"))
    }

    @Test
    fun testGetStrategyForMergeableEntities() {
        assertEquals(ConflictResolution.MERGE, resolver.getStrategy("StockMovement"))
        assertEquals(ConflictResolution.MERGE, resolver.getStrategy("Customer"))
        assertEquals(ConflictResolution.MERGE, resolver.getStrategy("ProductTransfer"))
    }

    @Test
    fun testGetStrategyForSecurityEntities() {
        assertEquals(ConflictResolution.REMOTE_WINS, resolver.getStrategy("User"))
        assertEquals(ConflictResolution.REMOTE_WINS, resolver.getStrategy("UserPermission"))
        assertEquals(ConflictResolution.REMOTE_WINS, resolver.getStrategy("PurchaseBatch"))
    }

    @Test
    fun testGetStrategyForInventory() {
        assertEquals(ConflictResolution.ASK_USER, resolver.getStrategy("Inventory"))
    }

    @Test
    fun testGetStrategyDefaultsToRemoteWins() {
        assertEquals(ConflictResolution.REMOTE_WINS, resolver.getStrategy("UnknownEntity"))
    }

    @Test
    fun testDetectConflictWhenRemoteIsNewer() {
        assertTrue(resolver.detectConflict(
            lastKnownRemoteUpdatedAt = 1000L,
            remoteUpdatedAt = 2000L
        ))
    }

    @Test
    fun testDetectConflictWhenNoChange() {
        assertFalse(resolver.detectConflict(
            lastKnownRemoteUpdatedAt = 1000L,
            remoteUpdatedAt = 1000L
        ))
    }

    @Test
    fun testDetectConflictWhenRemoteIsOlder() {
        assertFalse(resolver.detectConflict(
            lastKnownRemoteUpdatedAt = 2000L,
            remoteUpdatedAt = 1000L
        ))
    }

    @Test
    fun testDetectConflictWithNullLastKnown() {
        assertFalse(resolver.detectConflict(
            lastKnownRemoteUpdatedAt = null,
            remoteUpdatedAt = 2000L
        ))
    }

    @Test
    fun testDetectConflictWithNullRemote() {
        assertFalse(resolver.detectConflict(
            lastKnownRemoteUpdatedAt = 1000L,
            remoteUpdatedAt = null
        ))
    }

    @Test
    fun testResolveLocalWins() {
        val localPayload = """{"id":"1","name":"Local"}"""
        val remotePayload = """{"id":"1","name":"Remote"}"""

        val result = resolver.resolve(
            entityType = "Sale",
            localPayload = localPayload,
            remotePayload = remotePayload,
            localUpdatedAt = 1000L,
            remoteUpdatedAt = 2000L
        )

        assertEquals(ConflictResolution.LOCAL_WINS, result.resolution)
        assertEquals(localPayload, result.mergedPayload)
        assertNotNull(result.message)
    }

    @Test
    fun testResolveRemoteWins() {
        val localPayload = """{"id":"1","name":"Local"}"""
        val remotePayload = """{"id":"1","name":"Remote"}"""

        val result = resolver.resolve(
            entityType = "Product",
            localPayload = localPayload,
            remotePayload = remotePayload,
            localUpdatedAt = 1000L,
            remoteUpdatedAt = 2000L
        )

        assertEquals(ConflictResolution.REMOTE_WINS, result.resolution)
        assertEquals(remotePayload, result.mergedPayload)
    }

    @Test
    fun testResolveMerge() {
        val localPayload = """{"id":"1","name":"Local","quantity":10}"""
        val remotePayload = """{"id":"1","name":"Remote","quantity":5}"""

        val result = resolver.resolve(
            entityType = "StockMovement",
            localPayload = localPayload,
            remotePayload = remotePayload,
            localUpdatedAt = 1000L,
            remoteUpdatedAt = 2000L
        )

        assertEquals(ConflictResolution.MERGE, result.resolution)
        assertNotNull(result.mergedPayload)
    }

    @Test
    fun testResolveAskUser() {
        val result = resolver.resolve(
            entityType = "Inventory",
            localPayload = """{"id":"1"}""",
            remotePayload = """{"id":"1"}""",
            localUpdatedAt = 1000L,
            remoteUpdatedAt = 2000L
        )

        assertEquals(ConflictResolution.ASK_USER, result.resolution)
        assertNull(result.mergedPayload)
    }

    @Test
    fun testMergePayloadsWithBasicFields() {
        val localPayload = """{"id":"1","name":"LocalName","description":"LocalDesc","updated_at":1000}"""
        val remotePayload = """{"id":"1","name":"RemoteName","phone":"123","updated_at":2000}"""

        val merged = resolver.mergePayloads("Customer", localPayload, remotePayload)

        // Merged should have local name and description, but keep remote phone
        assertTrue(merged.contains("LocalName"))
        assertTrue(merged.contains("LocalDesc"))
        assertTrue(merged.contains("123"))
    }

    @Test
    fun testMergePayloadsPreservesId() {
        val localPayload = """{"id":"local-id","name":"Local"}"""
        val remotePayload = """{"id":"remote-id","name":"Remote"}"""

        val merged = resolver.mergePayloads("Customer", localPayload, remotePayload)

        // ID from remote should be preserved (id is a system field)
        assertTrue(merged.contains("remote-id"))
    }

    @Test
    fun testMergePayloadsWithNullRemote() {
        val localPayload = """{"id":"1","name":"Local"}"""

        val merged = resolver.mergePayloads("Customer", localPayload, null)

        assertEquals(localPayload, merged)
    }

    @Test
    fun testComputeFieldDifferences() {
        val localPayload = """{"id":"1","name":"Local","phone":"111"}"""
        val remotePayload = """{"id":"1","name":"Remote","phone":"111"}"""

        val differences = resolver.computeFieldDifferences(localPayload, remotePayload)

        // Only name should be different
        assertEquals(1, differences.size)
        assertEquals("name", differences[0].fieldName)
        assertTrue(differences[0].localValue?.contains("Local") == true)
        assertTrue(differences[0].remoteValue?.contains("Remote") == true)
    }

    @Test
    fun testComputeFieldDifferencesWithNullRemote() {
        val localPayload = """{"id":"1","name":"Local"}"""

        val differences = resolver.computeFieldDifferences(localPayload, null)

        assertTrue(differences.isEmpty())
    }

    @Test
    fun testCanSyncSafelyInsert() {
        assertTrue(resolver.canSyncSafely("INSERT", null, null))
        assertTrue(resolver.canSyncSafely("INSERT", 1000L, 2000L))
    }

    @Test
    fun testCanSyncSafelyUpdateWithNoConflict() {
        assertTrue(resolver.canSyncSafely("UPDATE", 1000L, 1000L))
        assertTrue(resolver.canSyncSafely("UPDATE", 2000L, 1000L))
    }

    @Test
    fun testCanSyncSafelyUpdateWithConflict() {
        assertFalse(resolver.canSyncSafely("UPDATE", 1000L, 2000L))
    }

    @Test
    fun testCanSyncSafelyDeleteWithNoConflict() {
        assertTrue(resolver.canSyncSafely("DELETE", 1000L, 1000L))
    }

    @Test
    fun testCanSyncSafelyDeleteWithConflict() {
        assertFalse(resolver.canSyncSafely("DELETE", 1000L, 2000L))
    }

    @Test
    fun testCreateUserConflict() {
        val localPayload = """{"id":"1","name":"Local"}"""
        val remotePayload = """{"id":"1","name":"Remote"}"""

        val conflict = resolver.createUserConflict(
            queueItemId = "queue-1",
            entityType = "Inventory",
            entityId = "inv-1",
            localPayload = localPayload,
            remotePayload = remotePayload,
            localUpdatedAt = 1000L,
            remoteUpdatedAt = 2000L
        )

        assertEquals("queue-1", conflict.queueItemId)
        assertEquals("Inventory", conflict.entityType)
        assertEquals("inv-1", conflict.entityId)
        assertEquals(localPayload, conflict.localPayload)
        assertEquals(remotePayload, conflict.remotePayload)
        assertEquals(1000L, conflict.localUpdatedAt)
        assertEquals(2000L, conflict.remoteUpdatedAt)
        assertTrue(conflict.fieldDifferences.isNotEmpty())
    }
}

class ConflictResolutionEnumTest {
    @Test
    fun testAllStrategiesExist() {
        val strategies = ConflictResolution.entries

        assertEquals(5, strategies.size)
        assertTrue(strategies.contains(ConflictResolution.LOCAL_WINS))
        assertTrue(strategies.contains(ConflictResolution.REMOTE_WINS))
        assertTrue(strategies.contains(ConflictResolution.MERGE))
        assertTrue(strategies.contains(ConflictResolution.ASK_USER))
        assertTrue(strategies.contains(ConflictResolution.KEEP_BOTH))
    }
}
