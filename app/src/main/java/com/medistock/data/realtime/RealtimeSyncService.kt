package com.medistock.data.realtime

import android.content.Context
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.util.SupabasePreferences
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Service utilitaire pour filtrer les événements Realtime provenant de Supabase.
 *
 * Règle métier:
 * - On ignore les événements émis par le même client (client_id identique)
 * - On traite normalement les événements sans client_id ou provenant d'un autre client (serveur gagne)
 */
object RealtimeSyncService {

    private val localClientId: String?
        get() = SupabaseClientProvider.getClientId()

    fun isRealtimeEnabled(context: Context): Boolean {
        val prefs = SupabasePreferences(context)
        return prefs.getSyncMode() == SupabasePreferences.SyncMode.REALTIME
    }

    /**
     * Indique si l'événement doit être appliqué localement.
     */
    fun shouldProcess(change: PostgresAction): Boolean {
        val remoteClientId = extractClientId(change)
        val currentClientId = localClientId

        if (!remoteClientId.isNullOrBlank() && remoteClientId == currentClientId) {
            return false
        }

        // Serveur gagne: appliquer tout ce qui vient d'un autre client ou sans client_id
        return true
    }

    private fun extractClientId(change: PostgresAction): String? {
        val record = when (change) {
            is PostgresAction.Insert -> change.record
            is PostgresAction.Update -> change.record
            is PostgresAction.Select -> change.record
            is PostgresAction.Delete -> change.oldRecord
        }

        return record["client_id"]?.jsonPrimitive?.contentOrNull
    }
}
