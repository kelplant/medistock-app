package com.medistock.shared.domain.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncModeTest {
    @Test
    fun testAllSyncModesExist() {
        val modes = SyncMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(SyncMode.AUTOMATIC))
        assertTrue(modes.contains(SyncMode.MANUAL))
        assertTrue(modes.contains(SyncMode.OFFLINE_FORCED))
    }

    @Test
    fun testSyncModeEnumConversion() {
        assertEquals(SyncMode.AUTOMATIC, SyncMode.valueOf("AUTOMATIC"))
        assertEquals(SyncMode.MANUAL, SyncMode.valueOf("MANUAL"))
        assertEquals(SyncMode.OFFLINE_FORCED, SyncMode.valueOf("OFFLINE_FORCED"))
    }
}

class LastSyncInfoTest {
    @Test
    fun testDefaultValues() {
        val lastSyncInfo = LastSyncInfo()

        assertNull(lastSyncInfo.timestamp)
        assertTrue(lastSyncInfo.success)
        assertNull(lastSyncInfo.error)
        assertFalse(lastSyncInfo.hasEverSynced)
    }

    @Test
    fun testHasEverSynced_whenTimestampIsNull() {
        val lastSyncInfo = LastSyncInfo(timestamp = null, success = true)

        assertFalse(lastSyncInfo.hasEverSynced)
    }

    @Test
    fun testHasEverSynced_whenTimestampExists() {
        val lastSyncInfo = LastSyncInfo(timestamp = 1000L, success = true)

        assertTrue(lastSyncInfo.hasEverSynced)
    }

    @Test
    fun testSuccessfulSync() {
        val lastSyncInfo = LastSyncInfo(
            timestamp = 1000L,
            success = true,
            error = null
        )

        assertEquals(1000L, lastSyncInfo.timestamp)
        assertTrue(lastSyncInfo.success)
        assertNull(lastSyncInfo.error)
        assertTrue(lastSyncInfo.hasEverSynced)
    }

    @Test
    fun testFailedSync() {
        val lastSyncInfo = LastSyncInfo(
            timestamp = 2000L,
            success = false,
            error = "Network error"
        )

        assertEquals(2000L, lastSyncInfo.timestamp)
        assertFalse(lastSyncInfo.success)
        assertEquals("Network error", lastSyncInfo.error)
        assertTrue(lastSyncInfo.hasEverSynced)
    }

    @Test
    fun testNeverSynced() {
        val lastSyncInfo = LastSyncInfo(
            timestamp = null,
            success = true
        )

        assertNull(lastSyncInfo.timestamp)
        assertFalse(lastSyncInfo.hasEverSynced)
    }
}

class SyncIndicatorColorTest {
    @Test
    fun testAllIndicatorColorsExist() {
        val colors = SyncIndicatorColor.values()
        assertEquals(5, colors.size)
        assertTrue(colors.contains(SyncIndicatorColor.SYNCED))
        assertTrue(colors.contains(SyncIndicatorColor.PENDING))
        assertTrue(colors.contains(SyncIndicatorColor.SYNCING))
        assertTrue(colors.contains(SyncIndicatorColor.OFFLINE))
        assertTrue(colors.contains(SyncIndicatorColor.ERROR))
    }

    @Test
    fun testIndicatorColorEnumConversion() {
        assertEquals(SyncIndicatorColor.SYNCED, SyncIndicatorColor.valueOf("SYNCED"))
        assertEquals(SyncIndicatorColor.PENDING, SyncIndicatorColor.valueOf("PENDING"))
        assertEquals(SyncIndicatorColor.SYNCING, SyncIndicatorColor.valueOf("SYNCING"))
        assertEquals(SyncIndicatorColor.OFFLINE, SyncIndicatorColor.valueOf("OFFLINE"))
        assertEquals(SyncIndicatorColor.ERROR, SyncIndicatorColor.valueOf("ERROR"))
    }
}

class GlobalSyncStatusTest {
    @Test
    fun testDefaultValues() {
        val status = GlobalSyncStatus()

        assertEquals(0, status.pendingCount)
        assertEquals(0, status.conflictCount)
        assertFalse(status.isOnline)
        assertEquals(SyncMode.AUTOMATIC, status.syncMode)
        assertFalse(status.isSyncing)
        assertTrue(status.isFullySynced)
        assertFalse(status.hasIssues)
        assertEquals(SyncIndicatorColor.OFFLINE, status.indicatorColor)
    }

    @Test
    fun testIsFullySynced_whenNoPendingOrConflicts() {
        val status = GlobalSyncStatus(
            pendingCount = 0,
            conflictCount = 0,
            isSyncing = false
        )

        assertTrue(status.isFullySynced)
    }

    @Test
    fun testIsFullySynced_whenPendingExists() {
        val status = GlobalSyncStatus(
            pendingCount = 5,
            conflictCount = 0,
            isSyncing = false
        )

        assertFalse(status.isFullySynced)
    }

    @Test
    fun testIsFullySynced_whenConflictsExist() {
        val status = GlobalSyncStatus(
            pendingCount = 0,
            conflictCount = 2,
            isSyncing = false
        )

        assertFalse(status.isFullySynced)
    }

    @Test
    fun testIsFullySynced_whenSyncing() {
        val status = GlobalSyncStatus(
            pendingCount = 0,
            conflictCount = 0,
            isSyncing = true
        )

        assertFalse(status.isFullySynced)
    }

    @Test
    fun testHasIssues_whenConflictsExist() {
        val status = GlobalSyncStatus(
            conflictCount = 1,
            lastSyncInfo = LastSyncInfo(timestamp = 1000L, success = true)
        )

        assertTrue(status.hasIssues)
    }

    @Test
    fun testHasIssues_whenLastSyncFailed() {
        val status = GlobalSyncStatus(
            conflictCount = 0,
            lastSyncInfo = LastSyncInfo(
                timestamp = 1000L,
                success = false,
                error = "Network error"
            )
        )

        assertTrue(status.hasIssues)
    }

    @Test
    fun testHasIssues_whenNeverSyncedButFailed() {
        val status = GlobalSyncStatus(
            conflictCount = 0,
            lastSyncInfo = LastSyncInfo(
                timestamp = null,
                success = false,
                error = "Initial sync failed"
            )
        )

        // Should not have issues if never synced (hasEverSynced = false)
        assertFalse(status.hasIssues)
    }

    @Test
    fun testHasIssues_whenNoProblems() {
        val status = GlobalSyncStatus(
            conflictCount = 0,
            lastSyncInfo = LastSyncInfo(timestamp = 1000L, success = true)
        )

        assertFalse(status.hasIssues)
    }

    @Test
    fun testIndicatorColor_error_whenConflicts() {
        val status = GlobalSyncStatus(
            conflictCount = 1,
            isOnline = true,
            isSyncing = false,
            pendingCount = 0
        )

        assertEquals(SyncIndicatorColor.ERROR, status.indicatorColor)
    }

    @Test
    fun testIndicatorColor_error_whenLastSyncFailed() {
        val status = GlobalSyncStatus(
            conflictCount = 0,
            isOnline = true,
            isSyncing = false,
            pendingCount = 0,
            lastSyncInfo = LastSyncInfo(
                timestamp = 1000L,
                success = false,
                error = "Error"
            )
        )

        assertEquals(SyncIndicatorColor.ERROR, status.indicatorColor)
    }

    @Test
    fun testIndicatorColor_offline_whenNotOnline() {
        val status = GlobalSyncStatus(
            isOnline = false,
            conflictCount = 0,
            isSyncing = false,
            pendingCount = 0,
            lastSyncInfo = LastSyncInfo(timestamp = 1000L, success = true)
        )

        assertEquals(SyncIndicatorColor.OFFLINE, status.indicatorColor)
    }

    @Test
    fun testIndicatorColor_syncing_whenSyncInProgress() {
        val status = GlobalSyncStatus(
            isOnline = true,
            conflictCount = 0,
            isSyncing = true,
            pendingCount = 0,
            lastSyncInfo = LastSyncInfo(timestamp = 1000L, success = true)
        )

        assertEquals(SyncIndicatorColor.SYNCING, status.indicatorColor)
    }

    @Test
    fun testIndicatorColor_pending_whenChangesExist() {
        val status = GlobalSyncStatus(
            isOnline = true,
            conflictCount = 0,
            isSyncing = false,
            pendingCount = 5,
            lastSyncInfo = LastSyncInfo(timestamp = 1000L, success = true)
        )

        assertEquals(SyncIndicatorColor.PENDING, status.indicatorColor)
    }

    @Test
    fun testIndicatorColor_synced_whenEverythingOk() {
        val status = GlobalSyncStatus(
            isOnline = true,
            conflictCount = 0,
            isSyncing = false,
            pendingCount = 0,
            lastSyncInfo = LastSyncInfo(timestamp = 1000L, success = true)
        )

        assertEquals(SyncIndicatorColor.SYNCED, status.indicatorColor)
    }

    @Test
    fun testIndicatorColorPriority_errorBeforeOffline() {
        val status = GlobalSyncStatus(
            isOnline = false, // offline
            conflictCount = 1, // error
            isSyncing = false,
            pendingCount = 0
        )

        // Error should take priority over offline
        assertEquals(SyncIndicatorColor.ERROR, status.indicatorColor)
    }

    @Test
    fun testIndicatorColorPriority_offlineBeforeSyncing() {
        val status = GlobalSyncStatus(
            isOnline = false, // offline
            conflictCount = 0,
            isSyncing = true, // syncing
            pendingCount = 0,
            lastSyncInfo = LastSyncInfo(timestamp = 1000L, success = true)
        )

        // Offline should take priority over syncing
        assertEquals(SyncIndicatorColor.OFFLINE, status.indicatorColor)
    }

    @Test
    fun testIndicatorColorPriority_syncingBeforePending() {
        val status = GlobalSyncStatus(
            isOnline = true,
            conflictCount = 0,
            isSyncing = true, // syncing
            pendingCount = 5, // pending
            lastSyncInfo = LastSyncInfo(timestamp = 1000L, success = true)
        )

        // Syncing should take priority over pending
        assertEquals(SyncIndicatorColor.SYNCING, status.indicatorColor)
    }

    @Test
    fun testAutomaticSyncMode() {
        val status = GlobalSyncStatus(
            syncMode = SyncMode.AUTOMATIC,
            isOnline = true
        )

        assertEquals(SyncMode.AUTOMATIC, status.syncMode)
    }

    @Test
    fun testManualSyncMode() {
        val status = GlobalSyncStatus(
            syncMode = SyncMode.MANUAL,
            isOnline = true
        )

        assertEquals(SyncMode.MANUAL, status.syncMode)
    }

    @Test
    fun testOfflineForcedMode() {
        val status = GlobalSyncStatus(
            syncMode = SyncMode.OFFLINE_FORCED,
            isOnline = false
        )

        assertEquals(SyncMode.OFFLINE_FORCED, status.syncMode)
    }

    @Test
    fun testComplexScenario_pendingAndOnline() {
        val status = GlobalSyncStatus(
            pendingCount = 3,
            conflictCount = 0,
            isOnline = true,
            syncMode = SyncMode.AUTOMATIC,
            lastSyncInfo = LastSyncInfo(timestamp = 1000L, success = true),
            isSyncing = false
        )

        assertFalse(status.isFullySynced)
        assertFalse(status.hasIssues)
        assertEquals(SyncIndicatorColor.PENDING, status.indicatorColor)
    }

    @Test
    fun testComplexScenario_syncFailedWithConflicts() {
        val status = GlobalSyncStatus(
            pendingCount = 2,
            conflictCount = 1,
            isOnline = true,
            syncMode = SyncMode.AUTOMATIC,
            lastSyncInfo = LastSyncInfo(
                timestamp = 2000L,
                success = false,
                error = "Sync failed"
            ),
            isSyncing = false
        )

        assertFalse(status.isFullySynced)
        assertTrue(status.hasIssues)
        assertEquals(SyncIndicatorColor.ERROR, status.indicatorColor)
    }
}
