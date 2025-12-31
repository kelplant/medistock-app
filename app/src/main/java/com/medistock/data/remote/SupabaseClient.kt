package com.medistock.data.remote

import android.content.Context
import com.medistock.util.SupabasePreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Client Supabase singleton pour Medistock
 *
 * Ce client fournit l'accès aux fonctionnalités Supabase:
 * - Postgrest: APIs REST auto-générées pour toutes les tables
 * - Realtime: Subscriptions temps réel pour les changements de données
 */
object SupabaseClientProvider {

    private var _client: SupabaseClient? = null
    private var appContext: Context? = null
    private var clientId: String? = null

    /**
     * Instance unique du client Supabase
     */
    val client: SupabaseClient
        get() = _client ?: throw IllegalStateException(
            "Supabase client not initialized. Call initialize(context) first or configure Supabase in Administration."
        )

    /**
     * Vérifie si le client est correctement configuré
     * @return true si les credentials sont configurés, false sinon
     */
    fun isConfigured(context: Context): Boolean {
        val prefs = SupabasePreferences(context)
        return prefs.isConfigured()
    }

    /**
     * Initialise le client Supabase avec les préférences sauvegardées
     * À appeler dans Application.onCreate() ou au démarrage de l'app
     */
    fun initialize(context: Context) {
        try {
            appContext = context.applicationContext
            val prefs = SupabasePreferences(appContext!!)
            clientId = prefs.getOrCreateClientId()

            val url = prefs.getSupabaseUrl()
            val key = prefs.getSupabaseKey()

            // Si pas encore configuré, utiliser les valeurs par défaut du fichier de config
            val supabaseUrl = url.ifEmpty { SupabaseConfig.SUPABASE_URL }
            val supabaseKey = key.ifEmpty { SupabaseConfig.SUPABASE_ANON_KEY }

            if (supabaseUrl == "https://YOUR_PROJECT_ID.supabase.co" ||
                supabaseKey == "YOUR_SUPABASE_ANON_KEY" ||
                supabaseUrl.isEmpty() ||
                supabaseKey.isEmpty()) {
                // Ne pas throw d'erreur, juste logger
                println("⚠️ Supabase pas encore configuré. Allez dans Administration > Configuration Supabase")
                return
            }

            _client = createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = supabaseKey
            ) {
                // Installation du module Postgrest pour les APIs REST
                install(Postgrest)

                // Installation du module Realtime pour les subscriptions
                install(Realtime)
            }

            if (SupabaseConfig.DEBUG_MODE) {
                println("✅ Supabase client initialisé avec succès")
                println("📡 URL: $supabaseUrl")
            }
        } catch (e: Exception) {
            println("❌ Erreur lors de l'initialisation du client Supabase: ${e.message}")
            e.printStackTrace()
            // Ne pas re-throw l'exception pour éviter que l'app crash au démarrage
        }
    }

    /**
     * Identifiant unique du client local utilisé pour différencier les événements Realtime
     */
    fun getClientId(): String? {
        if (clientId == null && appContext != null) {
            clientId = SupabasePreferences(appContext!!).getOrCreateClientId()
        }
        return clientId
    }

    /**
     * Réinitialise le client avec une nouvelle configuration
     * Utile après modification des credentials
     */
    fun reinitialize(context: Context) {
        _client = null
        initialize(context)
    }
}
