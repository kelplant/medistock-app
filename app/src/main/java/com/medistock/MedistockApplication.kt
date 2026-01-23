package com.medistock

import android.app.Application
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.medistock.shared.domain.compatibility.CompatibilityResult
import com.medistock.data.migration.MigrationManager
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.sync.SyncScheduler
import com.medistock.shared.DatabaseDriverFactory
import com.medistock.shared.MedistockSDK
import com.medistock.ui.AppUpdateRequiredActivity
import com.medistock.ui.auth.LoginActivity
import com.medistock.ui.common.UserProfileMenu
import com.medistock.ui.profile.ProfileActivity
import com.medistock.util.AppUpdateManager
import com.medistock.util.UpdateCheckResult
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

    /** Compte le nombre d'activités visibles pour détecter foreground/background */
    private var visibleActivityCount = 0

    /** Timestamp de la dernière vérification de compatibilité */
    private var lastCompatibilityCheck = 0L

    /** Timestamp de la dernière vérification de mise à jour GitHub */
    private var lastGitHubUpdateCheck = 0L

    /** Intervalle minimum entre deux vérifications (30 secondes) */
    private val compatibilityCheckInterval = 30_000L

    /** Intervalle minimum entre deux vérifications de mise à jour GitHub (5 minutes) */
    private val githubUpdateCheckInterval = 5 * 60_000L

    companion object {
        /**
         * Résultat de la vérification de compatibilité app/DB.
         * Vérifié par LoginActivity au démarrage.
         * null = pas encore vérifié, Compatible = OK, AppTooOld = mise à jour requise
         */
        @Volatile
        var compatibilityResult: CompatibilityResult? = null
            private set

        /**
         * Shared MedistockSDK instance for accessing UseCases and repositories
         */
        @Volatile
        private var _sdk: MedistockSDK? = null

        /**
         * Get the shared MedistockSDK instance
         * Must be called after Application.onCreate()
         */
        val sdk: MedistockSDK
            get() = _sdk ?: throw IllegalStateException("MedistockSDK not initialized. Call from Activity after onCreate.")

        /**
         * Met à jour le résultat de compatibilité (appelé par les vérifications)
         */
        internal fun updateCompatibilityResult(result: CompatibilityResult) {
            compatibilityResult = result
        }
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
            updateCompatibilityResult(compat)
            lastCompatibilityCheck = System.currentTimeMillis()

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
                updateCompatibilityResult(CompatibilityResult.Unknown(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Re-vérifie la compatibilité quand l'app revient au premier plan.
     * Ne vérifie que si assez de temps s'est écoulé depuis la dernière vérification.
     *
     * @param currentActivity L'activité actuellement au premier plan
     */
    private fun recheckCompatibilityOnForeground(currentActivity: Activity) {
        // Ne pas re-vérifier si on est déjà sur l'écran de mise à jour
        if (currentActivity is AppUpdateRequiredActivity) return

        val now = System.currentTimeMillis()

        // Vérification de compatibilité app/DB
        if (now - lastCompatibilityCheck >= compatibilityCheckInterval &&
            SupabaseClientProvider.isConfigured(this)) {

            println("🔄 Re-vérification de la compatibilité (retour au premier plan)...")

            appScope.launch {
                try {
                    val migrationManager = MigrationManager(this@MedistockApplication)
                    val compat = migrationManager.checkCompatibility()
                    updateCompatibilityResult(compat)
                    lastCompatibilityCheck = System.currentTimeMillis()

                    if (compat is CompatibilityResult.AppTooOld) {
                        println("❌ App devenue incompatible - redirection vers mise à jour")
                        // Lancer l'écran de mise à jour sur le thread UI
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            val intent = Intent(currentActivity, AppUpdateRequiredActivity::class.java).apply {
                                putExtra(AppUpdateRequiredActivity.EXTRA_APP_VERSION, compat.appVersion)
                                putExtra(AppUpdateRequiredActivity.EXTRA_MIN_REQUIRED, compat.minRequired)
                                putExtra(AppUpdateRequiredActivity.EXTRA_DB_VERSION, compat.dbVersion)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            currentActivity.startActivity(intent)
                            currentActivity.finish()
                        }
                    }
                } catch (e: Exception) {
                    println("⚠️ Erreur lors de la re-vérification: ${e.message}")
                    // En cas d'erreur, on ne bloque pas (peut-être offline)
                }
            }
        }

        // Vérification de mise à jour GitHub (moins fréquente)
        if (now - lastGitHubUpdateCheck >= githubUpdateCheckInterval) {
            println("🔄 Vérification des mises à jour GitHub (retour au premier plan)...")

            appScope.launch {
                try {
                    val updateManager = AppUpdateManager(this@MedistockApplication)
                    val result = updateManager.checkForUpdate()
                    lastGitHubUpdateCheck = System.currentTimeMillis()

                    if (result is UpdateCheckResult.UpdateAvailable) {
                        // Afficher le dialogue sur le thread UI
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            if (currentActivity is AppCompatActivity) {
                                showUpdateAvailableDialog(
                                    currentActivity,
                                    result.currentVersion,
                                    result.newVersion,
                                    result.release.body
                                )
                            }
                        }
                    } else if (result is UpdateCheckResult.NoUpdateAvailable) {
                        println("✅ Application à jour")
                    }
                } catch (e: Exception) {
                    println("⚠️ Erreur lors de la vérification des mises à jour GitHub: ${e.message}")
                    // En cas d'erreur, on ne bloque pas (peut-être offline)
                }
            }
        }
    }

    /**
     * Affiche un dialogue proposant à l'utilisateur de télécharger la mise à jour.
     */
    private fun showUpdateAvailableDialog(
        activity: AppCompatActivity,
        currentVersion: String,
        newVersion: String,
        releaseNotes: String?
    ) {
        val message = buildUpdateMessage(currentVersion, newVersion, releaseNotes)

        AlertDialog.Builder(activity)
            .setTitle("Mise à jour disponible")
            .setMessage(message)
            .setPositiveButton("Télécharger") { _, _ ->
                // Rediriger vers l'écran de mise à jour
                val intent = Intent(activity, AppUpdateRequiredActivity::class.java)
                activity.startActivity(intent)
            }
            .setNegativeButton("Plus tard") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    /**
     * Construit le message du dialogue de mise à jour.
     */
    private fun buildUpdateMessage(
        currentVersion: String,
        newVersion: String,
        releaseNotes: String?
    ): String {
        val message = StringBuilder()
        message.append("Une nouvelle version de MediStock est disponible.\n\n")
        message.append("Version actuelle : $currentVersion\n")
        message.append("Nouvelle version : $newVersion\n")

        if (!releaseNotes.isNullOrBlank()) {
            message.append("\nNouveautés :\n")
            // Limiter la longueur des notes de version pour le dialogue
            val shortNotes = if (releaseNotes.length > 200) {
                releaseNotes.take(200) + "..."
            } else {
                releaseNotes
            }
            message.append(shortNotes)
        }

        return message.toString()
    }

    override fun onCreate() {
        super.onCreate()

        // Force a light theme to match our defined palette (no dark variants yet)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Ensure modern trust store (Let's Encrypt, etc.) is available
        if (Security.getProvider("Conscrypt") == null) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        // Initialize shared MedistockSDK (SQLDelight database + UseCases)
        try {
            val driverFactory = DatabaseDriverFactory(this)
            _sdk = MedistockSDK(driverFactory)
            println("✅ MedistockSDK initialized")
        } catch (e: Exception) {
            println("❌ Failed to initialize MedistockSDK: ${e.message}")
            e.printStackTrace()
        }

        // Initialize language from saved preference
        ProfileActivity.initializeLanguage(this)
        println("✅ Language initialized: ${com.medistock.shared.i18n.LocalizationManager.getCurrentLocaleDisplayName()}")

        // Initialiser le client Supabase au démarrage de l'app
        // Version downgradée à Supabase 2.2.2 + Ktor 2.3.4 pour résoudre le problème HttpTimeout
        try {
            SupabaseClientProvider.initialize(this)
            appScope.launch {
                runCatching { SupabaseClientProvider.client.realtime.connect() }
                    .onFailure { println("⚠️ Realtime connect failed at startup: ${it.message}") }

                // Vérifier la compatibilité et exécuter les migrations Supabase en attente
                // IMPORTANT: Les migrations doivent être exécutées AVANT la sync
                checkCompatibilityAndRunMigrations()

                // Démarrer la sync APRÈS les migrations
                SyncScheduler.start(this@MedistockApplication)
                println("✅ Application démarrée avec Supabase 2.2.2")
            }
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

            override fun onActivityStarted(activity: Activity) {
                val wasInBackground = visibleActivityCount == 0
                visibleActivityCount++

                // Si l'app revient au premier plan, re-vérifier la compatibilité
                if (wasInBackground) {
                    println("📱 App revenue au premier plan")
                    recheckCompatibilityOnForeground(activity)
                }
            }

            override fun onActivityStopped(activity: Activity) {
                visibleActivityCount--
                if (visibleActivityCount == 0) {
                    println("📱 App passée en arrière-plan")
                }
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
