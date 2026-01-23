package com.medistock.data.sync

import android.content.Context
import android.content.SharedPreferences
import com.medistock.MedistockApplication
import com.medistock.shared.data.repository.SyncQueueRepository
import com.medistock.shared.domain.model.SyncStatus
import com.medistock.shared.domain.sync.GlobalSyncStatus
import com.medistock.shared.domain.sync.LastSyncInfo
import com.medistock.shared.domain.sync.SyncIndicatorColor
import com.medistock.shared.domain.sync.SyncMode
import com.medistock.util.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestionnaire d'état de synchronisation.
 *
 * Expose l'état de la synchronisation à l'UI via des StateFlows observables.
 * Permet aux composants UI d'afficher:
 * - Le nombre de modifications en attente
 * - Le nombre de conflits à résoudre
 * - Le statut de connexion
 * - La date de dernière synchronisation
 *
 * Usage:
 * ```kotlin
 * // Dans une Activity/Fragment
 * lifecycleScope.launch {
 *     SyncStatusManager.getInstance(context).globalStatus.collect { status ->
 *         updateSyncIndicator(status)
 *     }
 * }
 * ```
 */
class SyncStatusManager private constructor(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "sync_status_prefs"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_LAST_SYNC_SUCCESS = "last_sync_success"
        private const val KEY_SYNC_MODE = "sync_mode"

        @Volatile
        private var INSTANCE: SyncStatusManager? = null

        fun getInstance(context: Context): SyncStatusManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncStatusManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val syncQueueRepository: SyncQueueRepository = MedistockApplication.sdk.syncQueueRepository
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ==================== Observable States ====================

    /** Nombre de modifications en attente de sync */
    val pendingCount: StateFlow<Int> = syncQueueRepository.observePending()
        .map { it.size }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    /** Nombre de conflits à résoudre */
    val conflictCount: StateFlow<Int> = syncQueueRepository.observeConflicts()
        .map { it.size }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    /** État de connexion */
    private val _isOnline = MutableStateFlow(NetworkStatus.isOnline(context))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    /** Mode de synchronisation actuel */
    private val _syncMode = MutableStateFlow(loadSyncMode())
    val syncMode: StateFlow<SyncMode> = _syncMode.asStateFlow()

    /** Dernière synchronisation */
    private val _lastSyncInfo = MutableStateFlow(loadLastSyncInfo())
    val lastSyncInfo: StateFlow<LastSyncInfo> = _lastSyncInfo.asStateFlow()

    /** Sync en cours */
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** État global combiné pour l'UI */
    val globalStatus: StateFlow<GlobalSyncStatus> = combine(
        pendingCount,
        conflictCount,
        isOnline,
        syncMode,
        lastSyncInfo,
        isSyncing
    ) { values ->
        GlobalSyncStatus(
            pendingCount = values[0] as Int,
            conflictCount = values[1] as Int,
            isOnline = values[2] as Boolean,
            syncMode = values[3] as SyncMode,
            lastSyncInfo = values[4] as LastSyncInfo,
            isSyncing = values[5] as Boolean
        )
    }.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5000),
        GlobalSyncStatus()
    )

    // ==================== Public Methods ====================

    /**
     * Met à jour l'état de connexion
     */
    fun updateConnectionStatus() {
        _isOnline.value = NetworkStatus.isOnline(context)
    }

    /**
     * Change le mode de synchronisation
     */
    fun setSyncMode(mode: SyncMode) {
        _syncMode.value = mode
        prefs.edit().putString(KEY_SYNC_MODE, mode.name).apply()
    }

    /**
     * Enregistre une synchronisation réussie
     */
    fun recordSyncSuccess() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_LAST_SYNC_TIME, now)
            .putBoolean(KEY_LAST_SYNC_SUCCESS, true)
            .apply()
        _lastSyncInfo.value = LastSyncInfo(now, true, null)
    }

    /**
     * Enregistre un échec de synchronisation
     */
    fun recordSyncFailure(error: String) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_LAST_SYNC_TIME, now)
            .putBoolean(KEY_LAST_SYNC_SUCCESS, false)
            .apply()
        _lastSyncInfo.value = LastSyncInfo(now, false, error)
    }

    /**
     * Indique qu'une sync est en cours
     */
    fun setSyncing(syncing: Boolean) {
        _isSyncing.value = syncing
    }

    /**
     * Vérifie si une sync est nécessaire (modifications en attente)
     */
    suspend fun needsSync(): Boolean {
        return syncQueueRepository.getPendingCount() > 0
    }

    /**
     * Vérifie s'il y a des conflits à résoudre
     */
    suspend fun hasConflicts(): Boolean {
        return syncQueueRepository.getConflictCount() > 0
    }

    /**
     * Retourne un résumé textuel du statut de sync
     */
    fun getStatusSummary(): String {
        val status = globalStatus.value
        return buildString {
            when {
                !status.isOnline -> append("Mode hors ligne")
                status.isSyncing -> append("Synchronisation en cours...")
                status.conflictCount > 0 -> append("${status.conflictCount} conflit(s) à résoudre")
                status.pendingCount > 0 -> append("${status.pendingCount} modification(s) en attente")
                else -> append("Synchronisé")
            }

            status.lastSyncInfo.timestamp?.let { timestamp ->
                append(" • Dernière sync: ${formatRelativeTime(timestamp)}")
            }
        }
    }

    // ==================== Private Methods ====================

    private fun loadSyncMode(): SyncMode {
        val modeName = prefs.getString(KEY_SYNC_MODE, SyncMode.AUTOMATIC.name)
        return try {
            SyncMode.valueOf(modeName ?: SyncMode.AUTOMATIC.name)
        } catch (e: Exception) {
            SyncMode.AUTOMATIC
        }
    }

    private fun loadLastSyncInfo(): LastSyncInfo {
        val timestamp = prefs.getLong(KEY_LAST_SYNC_TIME, 0).takeIf { it > 0 }
        val success = prefs.getBoolean(KEY_LAST_SYNC_SUCCESS, true)
        return LastSyncInfo(timestamp, success, null)
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "à l'instant"
            diff < 3600_000 -> "${diff / 60_000} min"
            diff < 86400_000 -> "${diff / 3600_000}h"
            else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
