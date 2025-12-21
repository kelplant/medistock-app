package com.medistock.data.remote

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

    /**
     * Instance unique du client Supabase
     */
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            // Installation du module Postgrest pour les APIs REST
            install(Postgrest)

            // Installation du module Realtime pour les subscriptions
            install(Realtime)

            // Configuration du client HTTP pour Android
            httpEngine = Android.create()
        }
    }

    /**
     * Vérifie si le client est correctement configuré
     * @return true si les credentials sont configurés, false sinon
     */
    fun isConfigured(): Boolean {
        return SupabaseConfig.SUPABASE_URL != "https://YOUR_PROJECT_ID.supabase.co" &&
                SupabaseConfig.SUPABASE_ANON_KEY != "YOUR_SUPABASE_ANON_KEY"
    }

    /**
     * Initialise le client Supabase
     * À appeler dans Application.onCreate() ou au démarrage de l'app
     */
    fun initialize() {
        if (!isConfigured()) {
            throw IllegalStateException(
                """
                Supabase n'est pas configuré !
                Veuillez définir SUPABASE_URL et SUPABASE_ANON_KEY dans SupabaseConfig.kt

                Pour trouver vos credentials:
                1. Allez sur https://app.supabase.com
                2. Sélectionnez votre projet
                3. Allez dans Settings > API
                4. Copiez l'URL et la clé anon
                """.trimIndent()
            )
        }

        if (SupabaseConfig.DEBUG_MODE) {
            println("✅ Supabase client initialisé avec succès")
            println("📡 URL: ${SupabaseConfig.SUPABASE_URL}")
        }
    }
}
