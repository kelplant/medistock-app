package com.medistock.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.R
import com.medistock.ui.auth.LoginActivity
import com.medistock.ui.sales.SaleListActivity
import com.medistock.ui.stock.StockListActivity
import com.medistock.ui.purchase.PurchaseActivity
import com.medistock.ui.inventory.InventoryActivity
import com.medistock.ui.admin.AdminActivity
import com.medistock.ui.transfer.TransferListActivity
import com.medistock.util.AuthManager
import com.medistock.util.AppUpdateManager
import com.medistock.util.UpdateCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthManager.getInstance(this)

        // Check if user is logged in
        if (!authManager.isLoggedIn()) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_home)

        // Set title with user name
        supportActionBar?.title = "MediStock - ${authManager.getFullName()}"

        // Vérifier les mises à jour disponibles sur GitHub
        checkForAppUpdates()

        findViewById<android.view.View>(R.id.viewStockButton).setOnClickListener {
            startActivity(Intent(this, StockListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.sellProductButton).setOnClickListener {
            startActivity(Intent(this, SaleListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.transferProductButton).setOnClickListener {
            startActivity(Intent(this, TransferListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.purchaseButton).setOnClickListener {
            startActivity(Intent(this, PurchaseActivity::class.java))
        }

        findViewById<android.view.View>(R.id.inventoryButton).setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
        }

        findViewById<android.view.View>(R.id.adminButton).setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Vérifie si une mise à jour est disponible sur GitHub Releases.
     * Si oui, affiche un dialogue pour proposer à l'utilisateur de la télécharger.
     */
    private fun checkForAppUpdates() {
        lifecycleScope.launch {
            try {
                val updateManager = AppUpdateManager(this@HomeActivity)
                val result = withContext(Dispatchers.IO) {
                    updateManager.checkForUpdate()
                }

                when (result) {
                    is UpdateCheckResult.UpdateAvailable -> {
                        // Une mise à jour est disponible, afficher un dialogue
                        showUpdateAvailableDialog(
                            currentVersion = result.currentVersion,
                            newVersion = result.newVersion,
                            releaseNotes = result.release.body
                        )
                    }
                    is UpdateCheckResult.NoUpdateAvailable -> {
                        println("✅ Application à jour")
                    }
                    is UpdateCheckResult.Error -> {
                        println("⚠️ Impossible de vérifier les mises à jour: ${result.message}")
                        // Ne pas afficher d'erreur à l'utilisateur, cela peut être dû à l'absence de connexion
                    }
                }
            } catch (e: Exception) {
                println("⚠️ Erreur lors de la vérification des mises à jour: ${e.message}")
                // Ne pas perturber l'utilisateur avec cette erreur
            }
        }
    }

    /**
     * Affiche un dialogue proposant à l'utilisateur de télécharger la mise à jour.
     */
    private fun showUpdateAvailableDialog(
        currentVersion: String,
        newVersion: String,
        releaseNotes: String?
    ) {
        AlertDialog.Builder(this)
            .setTitle("Mise à jour disponible")
            .setMessage(buildUpdateMessage(currentVersion, newVersion, releaseNotes))
            .setPositiveButton("Télécharger") { _, _ ->
                // Rediriger vers l'écran de mise à jour
                navigateToUpdateScreen()
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

    /**
     * Redirige vers l'écran de téléchargement/installation de mise à jour.
     */
    private fun navigateToUpdateScreen() {
        val intent = Intent(this, AppUpdateRequiredActivity::class.java)
        startActivity(intent)
    }
}
