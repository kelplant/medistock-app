package com.medistock

import android.app.Application
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.sync.SyncScheduler

/**
 * Classe Application pour Medistock
 * Cette classe s'exécute une seule fois au démarrage de l'application
 */
class MedistockApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialiser le client Supabase au démarrage de l'app
        // Version downgradée à Supabase 2.2.2 + Ktor 2.3.4 pour résoudre le problème HttpTimeout
        try {
            SupabaseClientProvider.initialize(this)
            println("✅ Application démarrée avec Supabase 2.2.2")
            SyncScheduler.start(this)
        } catch (e: IllegalStateException) {
            // Les credentials Supabase ne sont pas encore configurés
            println("⚠️ Supabase non configuré: ${e.message}")
            println("⚠️ Veuillez configurer Supabase dans Administration > Configuration Supabase")
            SyncScheduler.start(this)
        } catch (e: Exception) {
            // Autre erreur lors de l'initialisation
            println("❌ Erreur lors de l'initialisation Supabase: ${e.message}")
            e.printStackTrace()
            SyncScheduler.start(this)
        }
    }
}
