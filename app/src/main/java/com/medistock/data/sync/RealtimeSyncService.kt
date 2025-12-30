package com.medistock.data.sync

import com.medistock.data.remote.SupabaseClientProvider
import io.github.jan.supabase.realtime.Realtime
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

    /**
     * Indique si l'événement doit être appliqué localement.
     */
    fun shouldProcess(change: Realtime): Boolean {
        val remoteClientId = extractClientId(change)
        val currentClientId = localClientId

        if (!remoteClientId.isNullOrBlank() && remoteClientId == currentClientId) {
            return false
        }

        // Serveur gagne: appliquer tout ce qui vient d'un autre client ou sans client_id
        return true
    }

    private fun extractClientId(change: Realtime): String? {
        val record = when (change) {
            is Realtime.Insert -> change.record
            is Realtime.Update -> change.record
            is Realtime.Delete -> change.oldRecord
        }

        return record["client_id"]?.jsonPrimitive?.contentOrNull
    }
}
