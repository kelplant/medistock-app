package com.medistock.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.medistock.R
import com.medistock.shared.i18n.LocalizationManager
import com.medistock.shared.i18n.SupportedLocale
import com.medistock.ui.auth.ChangePasswordActivity
import com.medistock.ui.auth.LoginActivity
import com.medistock.ui.customer.CustomerListActivity
import com.medistock.ui.inventory.InventoryListActivity
import com.medistock.ui.purchase.PurchaseListActivity
import com.medistock.util.AuthManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var textCurrentLanguage: TextView

    companion object {
        private const val PREFS_NAME = "medistock_prefs"
        private const val KEY_LANGUAGE = "selected_language"

        fun getSavedLanguageCode(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_LANGUAGE, null)
        }

        /**
         * Initialize language from saved preference or system language.
         * Matches iOS behavior: tries saved preference first, then system language, then defaults to English.
         */
        fun initializeLanguage(context: Context) {
            val savedCode = getSavedLanguageCode(context)
            if (savedCode != null) {
                // Use saved preference
                LocalizationManager.setLocaleByCode(savedCode)
            } else {
                // Try system language (first 2 chars of locale)
                val systemLanguage = java.util.Locale.getDefault().language
                val found = LocalizationManager.setLocaleByCode(systemLanguage)
                if (!found) {
                    // Fall back to English
                    LocalizationManager.setLocale(SupportedLocale.DEFAULT)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"

        authManager = AuthManager.getInstance(this)

        setupUserInfo()
        setupMenuItems()
        setupLanguage()
        setupLogout()
        setupAppVersion()
    }

    private fun setupUserInfo() {
        val textFullName = findViewById<TextView>(R.id.textFullName)
        val textUsername = findViewById<TextView>(R.id.textUsername)
        val textUserRole = findViewById<TextView>(R.id.textUserRole)

        textFullName.text = authManager.getFullName().ifBlank { "User" }
        textUsername.text = "@${authManager.getUsername()}"

        if (authManager.isAdmin()) {
            textUserRole.text = "Administrator"
            textUserRole.setBackgroundColor(0xFF2196F3.toInt()) // Blue
        } else {
            textUserRole.text = "User"
            textUserRole.setBackgroundColor(0xFF4CAF50.toInt()) // Green
        }
    }

    private fun setupMenuItems() {
        // Change Password
        findViewById<android.view.View>(R.id.btnChangePassword).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        // Customers
        findViewById<android.view.View>(R.id.btnCustomers).setOnClickListener {
            startActivity(Intent(this, CustomerListActivity::class.java))
        }

        // Purchase History
        findViewById<android.view.View>(R.id.btnPurchaseHistory).setOnClickListener {
            startActivity(Intent(this, PurchaseListActivity::class.java))
        }

        // Inventory History
        findViewById<android.view.View>(R.id.btnInventoryHistory).setOnClickListener {
            startActivity(Intent(this, InventoryListActivity::class.java))
        }
    }

    private fun setupLanguage() {
        textCurrentLanguage = findViewById(R.id.textCurrentLanguage)
        textCurrentLanguage.text = LocalizationManager.getCurrentLocaleDisplayName()

        findViewById<android.view.View>(R.id.btnLanguage).setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun showLanguageDialog() {
        val locales = SupportedLocale.entries.toTypedArray()
        val languageNames = locales.map { it.nativeDisplayName }.toTypedArray()
        val currentIndex = locales.indexOf(LocalizationManager.locale)

        AlertDialog.Builder(this)
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                val selectedLocale = locales[which]
                setLanguage(selectedLocale)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setLanguage(locale: SupportedLocale) {
        // Save to SharedPreferences
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, locale.code).apply()

        // Update LocalizationManager
        LocalizationManager.setLocale(locale)

        // Recreate activity to apply new language (UI will be updated automatically)
        recreate()
    }

    private fun setupLogout() {
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        authManager.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupAppVersion() {
        val textAppVersion = findViewById<TextView>(R.id.textAppVersion)
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            textAppVersion.text = "Version $versionName ($versionCode)"
        } catch (e: Exception) {
            textAppVersion.text = "Version 1.0.0"
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
