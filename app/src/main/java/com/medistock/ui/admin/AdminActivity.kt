package com.medistock.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.Module
import com.medistock.shared.i18n.L
import com.medistock.ui.customer.CustomerListActivity
import com.medistock.ui.supplier.SupplierListActivity
import com.medistock.ui.manage.ManageProductMenuActivity
import com.medistock.ui.site.SiteListActivity
import com.medistock.ui.movement.StockMovementListActivity
import com.medistock.ui.user.UserListActivity
import com.medistock.ui.packaging.PackagingTypeListActivity
import com.medistock.util.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var sdk: MedistockSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = L.strings.settings

        authManager = AuthManager.getInstance(this)
        sdk = MedistockApplication.sdk

        // Apply localized strings
        applyLocalizedStrings()

        // Setup click handlers
        setupButtonClickHandlers()

        // Apply permission-based visibility
        applyPermissionVisibility()
    }

    private fun applyLocalizedStrings() {
        val strings = L.strings

        findViewById<TextView>(R.id.labelManageSites)?.text = strings.sites
        findViewById<TextView>(R.id.labelManageProducts)?.text = strings.products
        findViewById<TextView>(R.id.labelStockMovement)?.text = strings.stockMovements
        findViewById<TextView>(R.id.labelPackagingTypes)?.text = strings.packagingTypes
        findViewById<TextView>(R.id.labelManageCustomers)?.text = strings.customers
        findViewById<TextView>(R.id.labelManageSuppliers)?.text = strings.suppliers
        findViewById<TextView>(R.id.labelManageUsers)?.text = strings.users
        // "Audit History" - use reports for now as there's no direct audit string
        findViewById<TextView>(R.id.labelAuditHistory)?.text = strings.reports
        findViewById<TextView>(R.id.labelAppSettings)?.text = strings.appSettings
        findViewById<TextView>(R.id.labelNotificationSettings)?.text = strings.notificationSettings
        // Supabase Config - keep as "Supabase" since it's a brand name
        findViewById<TextView>(R.id.labelSupabaseConfig)?.text = strings.syncSettings
    }

    private fun setupButtonClickHandlers() {
        findViewById<View>(R.id.btnManageSites).setOnClickListener {
            startActivity(Intent(this, SiteListActivity::class.java))
        }

        findViewById<View>(R.id.btnManageProducts).setOnClickListener {
            startActivity(Intent(this, ManageProductMenuActivity::class.java))
        }

        findViewById<View>(R.id.btnStockMovement).setOnClickListener {
            startActivity(Intent(this, StockMovementListActivity::class.java))
        }

        findViewById<View>(R.id.btnManagePackagingTypes).setOnClickListener {
            startActivity(Intent(this, PackagingTypeListActivity::class.java))
        }

        findViewById<View>(R.id.btnManageCustomers).setOnClickListener {
            startActivity(Intent(this, CustomerListActivity::class.java))
        }

        findViewById<View>(R.id.btnManageSuppliers).setOnClickListener {
            startActivity(Intent(this, SupplierListActivity::class.java))
        }

        findViewById<View>(R.id.btnManageUsers).setOnClickListener {
            startActivity(Intent(this, UserListActivity::class.java))
        }

        findViewById<View>(R.id.btnAuditHistory).setOnClickListener {
            startActivity(Intent(this, AuditHistoryActivity::class.java))
        }

        findViewById<View>(R.id.btnAppSettings).setOnClickListener {
            startActivity(Intent(this, AppSettingsActivity::class.java))
        }

        findViewById<View>(R.id.btnNotificationSettings).setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }

        findViewById<View>(R.id.btnSupabaseConfig).setOnClickListener {
            startActivity(Intent(this, SupabaseConfigActivity::class.java))
        }
    }

    /**
     * Apply permission-based visibility to admin menu items.
     * Each button is shown/hidden based on the user's module permissions.
     */
    private fun applyPermissionVisibility() {
        val userId = authManager.getUserId() ?: return // Not logged in
        val isAdmin = authManager.isAdmin()

        lifecycleScope.launch {
            try {
                // Get all permissions at once for efficiency
                val permissions = withContext(Dispatchers.IO) {
                    sdk.permissionService.getAllModulePermissions(userId, isAdmin)
                }

                // Sites management
                findViewById<View>(R.id.btnManageSites).visibility =
                    if (permissions[Module.SITES]?.canView == true) View.VISIBLE else View.GONE

                // Products management (includes Products and Categories)
                val canViewProducts = permissions[Module.PRODUCTS]?.canView == true ||
                    permissions[Module.CATEGORIES]?.canView == true
                findViewById<View>(R.id.btnManageProducts).visibility =
                    if (canViewProducts) View.VISIBLE else View.GONE

                // Stock movements
                findViewById<View>(R.id.btnStockMovement).visibility =
                    if (permissions[Module.STOCK]?.canView == true) View.VISIBLE else View.GONE

                // Packaging types
                findViewById<View>(R.id.btnManagePackagingTypes).visibility =
                    if (permissions[Module.PACKAGING_TYPES]?.canView == true) View.VISIBLE else View.GONE

                // Customers management
                findViewById<View>(R.id.btnManageCustomers).visibility =
                    if (permissions[Module.CUSTOMERS]?.canView == true) View.VISIBLE else View.GONE

                // Suppliers management
                findViewById<View>(R.id.btnManageSuppliers).visibility =
                    if (permissions[Module.SUPPLIERS]?.canView == true) View.VISIBLE else View.GONE

                // User management
                findViewById<View>(R.id.btnManageUsers).visibility =
                    if (permissions[Module.USERS]?.canView == true) View.VISIBLE else View.GONE

                // Audit history
                findViewById<View>(R.id.btnAuditHistory).visibility =
                    if (permissions[Module.AUDIT]?.canView == true) View.VISIBLE else View.GONE

                // App settings - visible for admins only
                findViewById<View>(R.id.btnAppSettings).visibility =
                    if (isAdmin || permissions[Module.ADMIN]?.canView == true) View.VISIBLE else View.GONE

                // Notification settings - visible for admins only
                findViewById<View>(R.id.btnNotificationSettings).visibility =
                    if (isAdmin || permissions[Module.ADMIN]?.canView == true) View.VISIBLE else View.GONE

                // Supabase configuration - always visible for admins, or if user has ADMIN permission
                findViewById<View>(R.id.btnSupabaseConfig).visibility =
                    if (isAdmin || permissions[Module.ADMIN]?.canView == true) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                e.printStackTrace()
                // On error, fallback to legacy behavior (only admins see admin features)
                if (isAdmin) {
                    findViewById<View>(R.id.btnManageUsers).visibility = View.VISIBLE
                    findViewById<View>(R.id.btnAuditHistory).visibility = View.VISIBLE
                    findViewById<View>(R.id.btnAppSettings).visibility = View.VISIBLE
                    findViewById<View>(R.id.btnNotificationSettings).visibility = View.VISIBLE
                } else {
                    findViewById<View>(R.id.btnManageUsers).visibility = View.GONE
                    findViewById<View>(R.id.btnAuditHistory).visibility = View.GONE
                    findViewById<View>(R.id.btnAppSettings).visibility = View.GONE
                    findViewById<View>(R.id.btnNotificationSettings).visibility = View.GONE
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
