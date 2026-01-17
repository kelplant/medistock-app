package com.medistock

import android.app.Application
import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.medistock.data.migration.CompatibilityResult
import com.medistock.data.migration.MigrationManager
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.sync.SyncScheduler
import com.medistock.ui.auth.LoginActivity
import com.medistock.ui.common.UserProfileMenu
import io.github.jan.supabase.realtime.realtime
import org.conscrypt.Conscrypt
import java.security.Security
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Classe Application pour Medistock
 * Cette classe s'exécute une seule fois au démarrage de l'application
 */
class MedistockApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        /**
         * Résultat de la vérification de compatibilité app/DB.
         * Vérifié par LoginActivity au démarrage.
         * null = pas encore vérifié, Compatible = OK, AppTooOld = mise à jour requise
         */
        @Volatile
        var compatibilityResult: CompatibilityResult? = null
            private set
    }

    /**
     * Vérifie la compatibilité et exécute les migrations Supabase en attente
     * Cette fonction est appelée au démarrage de l'app après l'initialisation de Supabase
     */
    private suspend fun checkCompatibilityAndRunMigrations() {
        try {
            val migrationManager = MigrationManager(this@MedistockApplication)

            // 1. Vérifier la compatibilité app/DB
            val compat = migrationManager.checkCompatibility()
            compatibilityResult = compat

            when (compat) {
                is CompatibilityResult.AppTooOld -> {
                    println("❌ App trop ancienne - mise à jour requise")
                    println("   Version app: ${compat.appVersion}, Min requise: ${compat.minRequired}")
                    // Ne pas exécuter les migrations si l'app est trop ancienne
                    return
                }
                is CompatibilityResult.Unknown -> {
                    println("⚠️ Impossible de vérifier la compatibilité: ${compat.reason}")
                    // Continuer quand même (peut-être offline ou système non installé)
                }
                is CompatibilityResult.Compatible -> {
                    println("✅ App compatible avec la base de données")
                }
            }

            // 2. Exécuter les migrations en attente
            val result = migrationManager.runPendingMigrations(appliedBy = "app")

            when {
                result.systemNotInstalled -> {
                    println("⚠️ Système de migration non installé dans Supabase")
                    println("⚠️ Veuillez exécuter 2026011701_migration_system.sql dans Supabase")
                }
                result.migrationsApplied.isNotEmpty() -> {
                    println("✅ ${result.migrationsApplied.size} migration(s) appliquée(s):")
                    result.migrationsApplied.forEach { println("   - $it") }
                }
                result.migrationsFailed.isNotEmpty() -> {
                    println("❌ ${result.migrationsFailed.size} migration(s) échouée(s):")
                    result.migrationsFailed.forEach { (name, error) ->
                        println("   - $name: $error")
                    }
                }
                else -> {
                    println("✅ Aucune nouvelle migration à appliquer")
                }
            }
        } catch (e: Exception) {
            println("❌ Erreur lors de la vérification/migrations: ${e.message}")
            // En cas d'erreur, on considère que c'est compatible (offline, etc.)
            if (compatibilityResult == null) {
                compatibilityResult = CompatibilityResult.Unknown(e.message ?: "Unknown error")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Force a light theme to match our defined palette (no dark variants yet)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Ensure modern trust store (Let's Encrypt, etc.) is available
        if (Security.getProvider("Conscrypt") == null) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        // Initialiser le client Supabase au démarrage de l'app
        // Version downgradée à Supabase 2.2.2 + Ktor 2.3.4 pour résoudre le problème HttpTimeout
        try {
            SupabaseClientProvider.initialize(this)
            appScope.launch {
                runCatching { SupabaseClientProvider.client.realtime.connect() }
                    .onFailure { println("⚠️ Realtime connect failed at startup: ${it.message}") }

                // Vérifier la compatibilité et exécuter les migrations Supabase en attente
                checkCompatibilityAndRunMigrations()
            }
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

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is AppCompatActivity && activity !is LoginActivity) {
                    UserProfileMenu.attach(activity)
                }
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
