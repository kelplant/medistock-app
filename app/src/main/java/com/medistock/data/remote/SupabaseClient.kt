package com.medistock.data.remote

import android.content.Context
import com.medistock.util.SupabasePreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.android.*

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
        appContext = context.applicationContext
        val prefs = SupabasePreferences(appContext!!)

        val url = prefs.getSupabaseUrl()
        val key = prefs.getSupabaseKey()

        // Si pas encore configuré, utiliser les valeurs par défaut du fichier de config
        val supabaseUrl = url.ifEmpty { SupabaseConfig.SUPABASE_URL }
        val supabaseKey = key.ifEmpty { SupabaseConfig.SUPABASE_ANON_KEY }

        if (supabaseUrl == "https://YOUR_PROJECT_ID.supabase.co" ||
            supabaseKey == "YOUR_SUPABASE_ANON_KEY") {
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

            // Configuration du client HTTP pour Android
            httpEngine = Android.create()
        }

        if (SupabaseConfig.DEBUG_MODE) {
            println("✅ Supabase client initialisé avec succès")
            println("📡 URL: $supabaseUrl")
        }
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
