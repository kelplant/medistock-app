package com.medistock

import android.app.Application
import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.sync.SyncScheduler
import com.medistock.ui.auth.LoginActivity
import com.medistock.ui.common.UserProfileMenu
import org.conscrypt.Conscrypt
import java.security.Security

/**
 * Classe Application pour Medistock
 * Cette classe s'exécute une seule fois au démarrage de l'application
 */
class MedistockApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Ensure modern trust store (Let's Encrypt, etc.) is available
        if (Security.getProvider("Conscrypt") == null) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        // Initialiser le client Supabase au démarrage de l'app
        // Version downgradée à Supabase 2.2.2 + Ktor 2.3.4 pour résoudre le problème HttpTimeout
        try {
            SupabaseClientProvider.initialize(this)
            runCatching { SupabaseClientProvider.client.realtime.connect() }
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
