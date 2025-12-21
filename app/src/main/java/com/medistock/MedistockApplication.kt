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

        // IMPORTANT: L'initialisation Supabase est désactivée au démarrage
        // pour éviter les problèmes de dépendances Ktor HttpTimeout.
        // Supabase sera initialisé à la demande quand l'utilisateur:
        // 1. Configure les credentials dans Administration > Configuration Supabase
        // 2. Utilise une fonctionnalité qui nécessite Supabase

        // L'app fonctionne normalement avec la base de données Room locale
        println("✅ Application démarrée (mode local - Supabase désactivé)")
        println("ℹ️ Pour activer Supabase: Administration > Configuration Supabase")

        // NOTE: Pour réactiver l'initialisation automatique, décommentez:
        // try {
        //     SupabaseClientProvider.initialize(this)
        // } catch (e: Exception) {
        //     println("❌ Erreur Supabase: ${e.message}")
        // }
    }
}
