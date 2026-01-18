package com.medistock.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.medistock.R
import com.medistock.data.sync.GlobalSyncStatus
import com.medistock.data.sync.SyncIndicatorColor
import com.medistock.data.sync.SyncStatusManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Composant UI affichant l'état de synchronisation.
 *
 * Affiche:
 * - Une icône colorée selon l'état (vert=synced, jaune=pending, bleu=syncing, gris=offline, rouge=error)
 * - Le nombre de modifications en attente
 * - Un badge pour les conflits
 *
 * Usage en XML:
 * ```xml
 * <com.medistock.ui.common.SyncIndicatorView
 *     android:id="@+id/syncIndicator"
 *     android:layout_width="wrap_content"
 *     android:layout_height="wrap_content" />
 * ```
 *
 * Usage programmatique:
 * ```kotlin
 * syncIndicator.bind(lifecycleOwner)
 * syncIndicator.setOnClickListener { showSyncDetails() }
 * ```
 */
class SyncIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val iconView: ImageView
    private val badgeText: TextView
    private val statusText: TextView

    private var syncStatusManager: SyncStatusManager? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_sync_indicator, this, true)

        iconView = findViewById(R.id.syncIcon)
        badgeText = findViewById(R.id.syncBadge)
        statusText = findViewById(R.id.syncStatusText)

        // État initial
        updateUI(GlobalSyncStatus())
    }

    /**
     * Lie le composant à un LifecycleOwner pour observer automatiquement le statut de sync.
     */
    fun bind(lifecycleOwner: LifecycleOwner) {
        syncStatusManager = SyncStatusManager.getInstance(context)

        lifecycleOwner.lifecycleScope.launch {
            syncStatusManager?.globalStatus?.collectLatest { status ->
                updateUI(status)
            }
        }
    }

    /**
     * Met à jour manuellement l'UI avec un statut donné
     */
    fun updateUI(status: GlobalSyncStatus) {
        // Couleur de l'icône
        val iconColor = when (status.indicatorColor) {
            SyncIndicatorColor.SYNCED -> R.color.sync_synced
            SyncIndicatorColor.PENDING -> R.color.sync_pending
            SyncIndicatorColor.SYNCING -> R.color.sync_syncing
            SyncIndicatorColor.OFFLINE -> R.color.sync_offline
            SyncIndicatorColor.ERROR -> R.color.sync_error
        }
        iconView.setColorFilter(ContextCompat.getColor(context, iconColor))

        // Icône selon l'état
        val iconRes = when {
            status.isSyncing -> R.drawable.ic_sync
            !status.isOnline -> R.drawable.ic_cloud_off
            status.hasIssues -> R.drawable.ic_sync_problem
            status.pendingCount > 0 -> R.drawable.ic_sync
            else -> R.drawable.ic_cloud_done
        }
        iconView.setImageResource(iconRes)

        // Animation de rotation si sync en cours
        if (status.isSyncing) {
            iconView.animate()
                .rotation(360f)
                .setDuration(1000)
                .withEndAction {
                    if (syncStatusManager?.isSyncing?.value == true) {
                        iconView.rotation = 0f
                        iconView.animate().rotation(360f).setDuration(1000).start()
                    }
                }
                .start()
        } else {
            iconView.clearAnimation()
            iconView.rotation = 0f
        }

        // Badge pour les modifications en attente ou conflits
        val badgeCount = status.pendingCount + status.conflictCount
        if (badgeCount > 0) {
            badgeText.visibility = VISIBLE
            badgeText.text = if (badgeCount > 99) "99+" else badgeCount.toString()
            // Rouge si conflits, jaune sinon
            val badgeColor = if (status.conflictCount > 0) R.color.sync_error else R.color.sync_pending
            badgeText.background.setTint(ContextCompat.getColor(context, badgeColor))
        } else {
            badgeText.visibility = GONE
        }

        // Texte de statut (optionnel, peut être masqué)
        statusText.text = when {
            status.isSyncing -> context.getString(R.string.sync_status_syncing)
            !status.isOnline -> context.getString(R.string.sync_status_offline)
            status.conflictCount > 0 -> context.getString(R.string.sync_status_conflicts, status.conflictCount)
            status.pendingCount > 0 -> context.getString(R.string.sync_status_pending, status.pendingCount)
            else -> context.getString(R.string.sync_status_synced)
        }
    }

    /**
     * Affiche ou masque le texte de statut
     */
    fun setShowStatusText(show: Boolean) {
        statusText.visibility = if (show) VISIBLE else GONE
    }
}
