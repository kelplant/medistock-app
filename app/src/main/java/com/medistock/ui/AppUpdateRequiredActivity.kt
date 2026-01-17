package com.medistock.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.medistock.R
import com.medistock.data.migration.MigrationManager

/**
 * Écran affiché quand l'app est trop ancienne pour la base de données.
 *
 * Cet écran bloque l'utilisation de l'app et demande à l'utilisateur
 * de mettre à jour vers une version plus récente.
 */
class AppUpdateRequiredActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_APP_VERSION = "app_version"
        const val EXTRA_MIN_REQUIRED = "min_required"
        const val EXTRA_DB_VERSION = "db_version"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_update_required)

        // Récupérer les informations de version
        val appVersion = intent.getIntExtra(EXTRA_APP_VERSION, MigrationManager.APP_SCHEMA_VERSION)
        val minRequired = intent.getIntExtra(EXTRA_MIN_REQUIRED, 0)
        val dbVersion = intent.getIntExtra(EXTRA_DB_VERSION, 0)

        // Configurer les vues
        val tvMessage = findViewById<TextView>(R.id.tvUpdateMessage)
        val tvVersionInfo = findViewById<TextView>(R.id.tvVersionInfo)
        val btnUpdate = findViewById<Button>(R.id.btnUpdate)
        val btnClose = findViewById<Button>(R.id.btnClose)

        tvMessage.text = getString(R.string.update_required_message)
        tvVersionInfo.text = getString(
            R.string.update_version_info,
            appVersion,
            minRequired,
            dbVersion
        )

        btnUpdate.setOnClickListener {
            openPlayStore()
        }

        btnClose.setOnClickListener {
            finishAffinity() // Ferme toutes les activités
        }
    }

    private fun openPlayStore() {
        try {
            // Essayer d'ouvrir le Play Store directement
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback vers le navigateur
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                }
                startActivity(intent)
            } catch (e2: Exception) {
                // Ignorer si aucune option n'est disponible
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Empêcher le retour arrière - l'utilisateur doit mettre à jour ou fermer
        // Ne rien faire
    }
}
