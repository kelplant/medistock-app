package com.medistock.ui.admin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.shared.MedistockSDK
import com.medistock.shared.data.dto.AppConfigDto
import com.medistock.shared.domain.model.AppConfig
import com.medistock.shared.i18n.L
import com.medistock.util.AuthManager
import com.medistock.util.PrefsHelper
import com.medistock.data.remote.repository.BaseSupabaseRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSettingsActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var sdk: MedistockSDK
    private lateinit var editCurrencySymbol: EditText
    private lateinit var btnSaveSettings: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var labelCurrencySymbol: TextView
    private lateinit var labelCurrencyDescription: TextView
    private lateinit var switchDebugMode: SwitchCompat
    private lateinit var labelDebugMode: TextView
    private lateinit var labelDebugDescription: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthManager.getInstance(this)
        sdk = MedistockApplication.sdk

        // Security check: only admins can access app settings
        if (!authManager.isAdmin()) {
            Toast.makeText(this, L.strings.error, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.activity_app_settings)

        val strings = L.strings
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = strings.appSettings

        initViews()
        applyLocalizedStrings()
        loadSettings()

        btnSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun initViews() {
        editCurrencySymbol = findViewById(R.id.editCurrencySymbol)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        progressBar = findViewById(R.id.progressBar)
        labelCurrencySymbol = findViewById(R.id.labelCurrencySymbol)
        labelCurrencyDescription = findViewById(R.id.labelCurrencyDescription)
        switchDebugMode = findViewById(R.id.switchDebugMode)
        labelDebugMode = findViewById(R.id.labelDebugMode)
        labelDebugDescription = findViewById(R.id.labelDebugDescription)

        // Initialize debug toggle from saved preference
        switchDebugMode.isChecked = PrefsHelper.isDebugModeEnabled(this)
        switchDebugMode.setOnCheckedChangeListener { _, isChecked ->
            PrefsHelper.saveDebugMode(this, isChecked)
            com.medistock.util.DebugConfig.isDebugEnabled = isChecked
            BaseSupabaseRepository.DEBUG = isChecked
        }
    }

    private fun applyLocalizedStrings() {
        val strings = L.strings
        labelCurrencySymbol.text = strings.currencySymbolSetting
        labelCurrencyDescription.text = strings.currencySymbolDescription
        btnSaveSettings.text = strings.save
        labelDebugMode.text = strings.debugMode
        labelDebugDescription.text = strings.debugModeDescription
    }

    private fun loadSettings() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Load from local DB first
                val currencySymbol = withContext(Dispatchers.IO) {
                    sdk.appConfigRepository.getCurrencySymbol()
                }
                editCurrencySymbol.setText(currencySymbol)

                // Try to sync from Supabase if configured
                if (SupabaseClientProvider.isConfigured(this@AppSettingsActivity)) {
                    try {
                        val remoteConfig = withContext(Dispatchers.IO) {
                            SupabaseClientProvider.client
                                .from("app_config")
                                .select {
                                    filter { eq("key", AppConfig.KEY_CURRENCY_SYMBOL) }
                                }
                                .decodeSingleOrNull<AppConfigDto>()
                        }
                        val remoteValue = remoteConfig?.value
                        if (remoteValue != null) {
                            editCurrencySymbol.setText(remoteValue)
                            // Update local cache
                            withContext(Dispatchers.IO) {
                                sdk.appConfigRepository.setCurrencySymbol(
                                    remoteValue,
                                    authManager.getUserId() ?: "system"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Supabase fetch failed, use local value
                        println("⚠️ Failed to fetch config from Supabase: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AppSettingsActivity,
                    "${L.strings.error}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun saveSettings() {
        val strings = L.strings
        val currencySymbol = editCurrencySymbol.text.toString().trim()

        if (currencySymbol.isEmpty() || currencySymbol.length > 5) {
            Toast.makeText(this, strings.invalidCurrencySymbol, Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val userId = authManager.getUserId() ?: "system"

                // Save to local DB
                withContext(Dispatchers.IO) {
                    sdk.appConfigRepository.setCurrencySymbol(currencySymbol, userId)
                }

                // Sync to Supabase if configured
                if (SupabaseClientProvider.isConfigured(this@AppSettingsActivity)) {
                    try {
                        val now = System.currentTimeMillis()
                        val configDto = AppConfigDto(
                            key = AppConfig.KEY_CURRENCY_SYMBOL,
                            value = currencySymbol,
                            description = "Currency symbol for prices display",
                            updatedAt = now,
                            updatedBy = userId
                        )
                        withContext(Dispatchers.IO) {
                            SupabaseClientProvider.client
                                .from("app_config")
                                .upsert(configDto)
                        }
                    } catch (e: Exception) {
                        println("⚠️ Failed to sync config to Supabase: ${e.message}")
                        // Don't fail - local save succeeded
                    }
                }

                Toast.makeText(
                    this@AppSettingsActivity,
                    strings.settingsSavedSuccessfully,
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AppSettingsActivity,
                    "${strings.error}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSaveSettings.isEnabled = !loading
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
