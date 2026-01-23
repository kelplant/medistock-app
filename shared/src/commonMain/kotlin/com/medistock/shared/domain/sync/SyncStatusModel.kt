package com.medistock.shared.domain.sync

/**
 * Sync status models shared between Android and iOS.
 * These models define the common contract for sync state management.
 */

/**
 * Modes de synchronisation / Sync modes
 */
enum class SyncMode {
    /** Synchronisation automatique en arrière-plan / Automatic background sync */
    AUTOMATIC,
    /** Synchronisation manuelle uniquement / Manual sync only */
    MANUAL,
    /** Mode hors ligne forcé (pas de sync même si connecté) / Forced offline mode */
    OFFLINE_FORCED
}

/**
 * Informations sur la dernière synchronisation / Last sync information
 * @param timestamp Timestamp in milliseconds since epoch (null if never synced)
 * @param success Whether the last sync was successful
 * @param error Error message if the last sync failed
 */
data class LastSyncInfo(
    val timestamp: Long? = null,
    val success: Boolean = true,
    val error: String? = null
) {
    /** Whether the app has ever synced */
    val hasEverSynced: Boolean get() = timestamp != null
}

/**
 * État global de synchronisation / Global sync status
 * Combines all sync-related state into a single object.
 */
data class GlobalSyncStatus(
    val pendingCount: Int = 0,
    val conflictCount: Int = 0,
    val isOnline: Boolean = false,
    val syncMode: SyncMode = SyncMode.AUTOMATIC,
    val lastSyncInfo: LastSyncInfo = LastSyncInfo(),
    val isSyncing: Boolean = false
) {
    /** Indique si tout est synchronisé / Whether everything is fully synced */
    val isFullySynced: Boolean
        get() = pendingCount == 0 && conflictCount == 0 && !isSyncing

    /** Indique s'il y a des problèmes nécessitant attention / Whether there are issues requiring attention */
    val hasIssues: Boolean
        get() = conflictCount > 0 || (!lastSyncInfo.success && lastSyncInfo.hasEverSynced)

    /** Suggested indicator color for UI */
    val indicatorColor: SyncIndicatorColor
        get() = when {
            hasIssues -> SyncIndicatorColor.ERROR
            !isOnline -> SyncIndicatorColor.OFFLINE
            isSyncing -> SyncIndicatorColor.SYNCING
            pendingCount > 0 -> SyncIndicatorColor.PENDING
            else -> SyncIndicatorColor.SYNCED
        }
}

/**
 * Couleurs pour l'indicateur de sync UI / Colors for the sync indicator in UI
 */
enum class SyncIndicatorColor {
    /** Vert - tout est synchronisé / Green - fully synced */
    SYNCED,
    /** Jaune - modifications en attente / Yellow - pending modifications */
    PENDING,
    /** Bleu - synchronisation en cours / Blue - sync in progress */
    SYNCING,
    /** Gris - hors ligne / Gray - offline */
    OFFLINE,
    /** Rouge - erreur ou conflits / Red - error or conflicts */
    ERROR
}
