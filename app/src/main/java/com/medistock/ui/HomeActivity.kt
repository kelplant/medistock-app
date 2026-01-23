package com.medistock.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.Module
import com.medistock.shared.domain.model.Site
import com.medistock.ui.auth.LoginActivity
import com.medistock.ui.sales.SaleListActivity
import com.medistock.ui.stock.StockListActivity
import com.medistock.ui.purchase.PurchaseActivity
import com.medistock.ui.inventory.InventoryActivity
import com.medistock.ui.admin.AdminActivity
import com.medistock.ui.transfer.TransferListActivity
import com.medistock.util.AuthManager
import com.medistock.util.AppUpdateManager
import com.medistock.util.PrefsHelper
import com.medistock.util.UpdateCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var sdk: MedistockSDK
    private var sites: List<Site> = emptyList()
    private var currentSite: Site? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthManager.getInstance(this)
        sdk = MedistockApplication.sdk

        // Check if user is logged in
        if (!authManager.isLoggedIn()) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_home)

        // Set title with user name
        supportActionBar?.title = "MediStock - ${authManager.getFullName()}"

        // Load sites and set default if needed
        loadSitesAndSetDefault()

        // Vérifier les mises à jour disponibles sur GitHub
        checkForAppUpdates()

        // Site selector click handler
        findViewById<android.view.View>(R.id.siteSelector).setOnClickListener {
            showSiteSelectionDialog()
        }

        // Setup button click handlers
        setupButtonClickHandlers()

        // Apply permission-based visibility
        applyPermissionVisibility()
    }

    private fun setupButtonClickHandlers() {
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

    /**
     * Apply permission-based visibility to UI elements.
     * Each operation button is shown/hidden based on the user's module permissions.
     */
    private fun applyPermissionVisibility() {
        val userId = authManager.getUserId() ?: return // Not logged in, should not happen
        val isAdmin = authManager.isAdmin()

        lifecycleScope.launch {
            try {
                // Get all permissions at once for efficiency
                val permissions = withContext(Dispatchers.IO) {
                    sdk.permissionService.getAllModulePermissions(userId, isAdmin)
                }

                // Operations section
                findViewById<View>(R.id.purchaseButton).visibility =
                    if (permissions[Module.PURCHASES]?.canView == true) View.VISIBLE else View.GONE

                findViewById<View>(R.id.sellProductButton).visibility =
                    if (permissions[Module.SALES]?.canView == true) View.VISIBLE else View.GONE

                findViewById<View>(R.id.transferProductButton).visibility =
                    if (permissions[Module.TRANSFERS]?.canView == true) View.VISIBLE else View.GONE

                findViewById<View>(R.id.viewStockButton).visibility =
                    if (permissions[Module.STOCK]?.canView == true) View.VISIBLE else View.GONE

                findViewById<View>(R.id.inventoryButton).visibility =
                    if (permissions[Module.INVENTORY]?.canView == true) View.VISIBLE else View.GONE

                // Administration button - show if user has ANY admin-level permission
                val hasAnyAdminPermission = permissions[Module.ADMIN]?.canView == true ||
                    permissions[Module.SITES]?.canView == true ||
                    permissions[Module.PRODUCTS]?.canView == true ||
                    permissions[Module.CATEGORIES]?.canView == true ||
                    permissions[Module.PACKAGING_TYPES]?.canView == true ||
                    permissions[Module.CUSTOMERS]?.canView == true ||
                    permissions[Module.USERS]?.canView == true ||
                    permissions[Module.AUDIT]?.canView == true

                findViewById<View>(R.id.adminButton).visibility =
                    if (hasAnyAdminPermission) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                e.printStackTrace()
                // Fail-closed for security: hide admin features on error
                findViewById<View>(R.id.adminButton).visibility = View.GONE
            }
        }
    }

    private fun loadSitesAndSetDefault() {
        lifecycleScope.launch {
            try {
                sites = withContext(Dispatchers.IO) {
                    sdk.siteRepository.getAll()
                }

                if (sites.isNotEmpty()) {
                    // Check if there's a saved site ID
                    val savedSiteId = PrefsHelper.getActiveSiteId(this@HomeActivity)

                    currentSite = if (savedSiteId != null) {
                        // Try to find the saved site
                        sites.find { it.id == savedSiteId } ?: sites.first()
                    } else {
                        // No saved site, use the first one
                        sites.first()
                    }

                    // Save the current site ID
                    PrefsHelper.saveActiveSiteId(this@HomeActivity, currentSite!!.id)

                    // Update UI
                    updateCurrentSiteDisplay()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateCurrentSiteDisplay() {
        findViewById<TextView>(R.id.currentSiteName)?.text =
            currentSite?.name ?: "Sélectionner un site"
    }

    private fun showSiteSelectionDialog() {
        if (sites.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Aucun site")
                .setMessage("Aucun site disponible. Veuillez d'abord créer un site dans l'administration.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val siteNames = sites.map { it.name }.toTypedArray()
        val currentIndex = sites.indexOfFirst { it.id == currentSite?.id }.takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(this)
            .setTitle("Sélectionner un site")
            .setSingleChoiceItems(siteNames, currentIndex) { dialog, which ->
                currentSite = sites[which]
                PrefsHelper.saveActiveSiteId(this, currentSite!!.id)
                updateCurrentSiteDisplay()
                dialog.dismiss()
            }
            .setNegativeButton("Annuler", null)
            .show()
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
