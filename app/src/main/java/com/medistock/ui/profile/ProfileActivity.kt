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
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.shared.data.dto.UserDto
import com.medistock.shared.domain.model.User
import com.medistock.shared.i18n.L
import com.medistock.shared.i18n.LocalizationManager
import com.medistock.shared.i18n.SupportedLocale
import com.medistock.ui.auth.ChangePasswordActivity
import com.medistock.ui.auth.LoginActivity
import com.medistock.ui.customer.CustomerListActivity
import com.medistock.ui.inventory.InventoryListActivity
import com.medistock.ui.purchase.PurchaseListActivity
import com.medistock.util.AuthManager
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
         * Initialize language from user profile, saved preference, or system language.
         * Priority: 1. User profile, 2. Local cache, 3. System language, 4. Default (English)
         *
         * @param context The context
         * @param user Optional user object with language preference from profile
         */
        fun initializeLanguage(context: Context, user: User? = null) {
            // Priority: user profile > local cache > system language > default
            val languageCode = user?.language
                ?: getSavedLanguageCode(context)
                ?: java.util.Locale.getDefault().language

            val found = LocalizationManager.setLocaleByCode(languageCode)
            if (!found) {
                LocalizationManager.setLocale(SupportedLocale.DEFAULT)
            }

            // Cache the language from user profile for offline access
            if (user?.language != null) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_LANGUAGE, user.language).apply()
                AuthManager.getInstance(context).setLanguage(user.language)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = L.strings.profile

        authManager = AuthManager.getInstance(this)

        applyLocalizedStrings()
        setupUserInfo()
        setupMenuItems()
        setupLanguage()
        setupLogout()
        setupAppVersion()
    }

    private fun applyLocalizedStrings() {
        val strings = L.strings

        // Menu labels
        findViewById<TextView>(R.id.labelChangePassword)?.text = strings.changePassword
        findViewById<TextView>(R.id.labelCustomers)?.text = strings.customers
        findViewById<TextView>(R.id.labelPurchaseHistory)?.text = strings.purchaseHistory
        findViewById<TextView>(R.id.labelInventoryHistory)?.text = strings.inventory
        findViewById<TextView>(R.id.labelLanguage)?.text = strings.language

        // Logout button
        findViewById<Button>(R.id.btnLogout)?.text = strings.logout
    }

    private fun setupUserInfo() {
        val strings = L.strings
        val textFullName = findViewById<TextView>(R.id.textFullName)
        val textUsername = findViewById<TextView>(R.id.textUsername)
        val textUserRole = findViewById<TextView>(R.id.textUserRole)

        textFullName.text = authManager.getFullName().ifBlank { strings.user }
        textUsername.text = "@${authManager.getUsername()}"

        if (authManager.isAdmin()) {
            textUserRole.text = strings.admin
            textUserRole.setBackgroundColor(0xFF2196F3.toInt()) // Blue
        } else {
            textUserRole.text = strings.user
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
        val strings = L.strings
        val locales = SupportedLocale.entries.toTypedArray()
        val languageNames = locales.map { it.nativeDisplayName }.toTypedArray()
        val currentIndex = locales.indexOf(LocalizationManager.locale)

        AlertDialog.Builder(this)
            .setTitle(strings.selectLanguage)
            .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                val selectedLocale = locales[which]
                setLanguage(selectedLocale)
                dialog.dismiss()
            }
            .setNegativeButton(strings.cancel, null)
            .show()
    }

    private fun setLanguage(locale: SupportedLocale) {
        val sdk = MedistockApplication.sdk
        val userId = authManager.getUserId()

        // 1. Save to local SharedPreferences cache
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, locale.code).apply()
        authManager.setLanguage(locale.code)

        // 2. Update local database and sync to Supabase
        if (userId != null) {
            lifecycleScope.launch {
                val now = System.currentTimeMillis()
                try {
                    // Update local DB
                    sdk.userRepository.updateLanguage(userId, locale.code, now, userId)

                    // Sync to Supabase if configured and online
                    if (SupabaseClientProvider.isConfigured(this@ProfileActivity)) {
                        withContext(Dispatchers.IO) {
                            val user = sdk.userRepository.getById(userId)
                            if (user != null) {
                                SupabaseClientProvider.client
                                    .from("app_users")
                                    .update({
                                        set("language", locale.code)
                                        set("updated_at", now)
                                        set("updated_by", userId)
                                    }) {
                                        filter { eq("id", userId) }
                                    }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Log error but don't block - language change is cached locally
                    android.util.Log.e("ProfileActivity", "Failed to sync language: ${e.message}")
                }
            }
        }

        // 3. Update LocalizationManager
        LocalizationManager.setLocale(locale)

        // 4. Recreate activity to apply new language
        recreate()
    }

    private fun setupLogout() {
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun confirmLogout() {
        val strings = L.strings
        AlertDialog.Builder(this)
            .setTitle(strings.logout)
            .setMessage(strings.logoutConfirm)
            .setPositiveButton(strings.logout) { _, _ ->
                performLogout()
            }
            .setNegativeButton(strings.cancel, null)
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
