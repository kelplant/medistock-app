package com.medistock

import android.app.Application
import com.medistock.data.remote.SupabaseClientProvider

/**
 * Classe Application pour Medistock
 * Cette classe s'exécute une seule fois au démarrage de l'application
 */
class MedistockApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialiser le client Supabase au démarrage de l'app
        try {
            SupabaseClientProvider.initialize()
        } catch (e: IllegalStateException) {
            // Les credentials Supabase ne sont pas encore configurés
            // C'est normal si vous n'avez pas encore rempli SupabaseConfig.kt
            println("⚠️ Supabase non configuré: ${e.message}")
            println("⚠️ Veuillez configurer SUPABASE_URL et SUPABASE_ANON_KEY dans SupabaseConfig.kt")
        } catch (e: Exception) {
            // Autre erreur lors de l'initialisation
            println("❌ Erreur lors de l'initialisation Supabase: ${e.message}")
            e.printStackTrace()
        }
    }
}
