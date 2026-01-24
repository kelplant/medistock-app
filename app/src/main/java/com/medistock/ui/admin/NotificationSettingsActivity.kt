package com.medistock.ui.admin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.R
import com.medistock.shared.data.dto.NotificationSettingsDto
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.shared.i18n.L
import com.medistock.util.AuthManager
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var switchExpiryEnabled: Switch
    private lateinit var editExpiryWarningDays: EditText
    private lateinit var switchLowStockEnabled: Switch
    private lateinit var btnSaveSettings: Button
    private lateinit var progressBar: ProgressBar

    private var currentSettings: NotificationSettingsDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = L.strings.notificationSettings

        authManager = AuthManager.getInstance(this)

        // Bind views
        switchExpiryEnabled = findViewById(R.id.switchExpiryEnabled)
        editExpiryWarningDays = findViewById(R.id.editExpiryWarningDays)
        switchLowStockEnabled = findViewById(R.id.switchLowStockEnabled)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        progressBar = findViewById(R.id.progressBar)

        btnSaveSettings.setOnClickListener { saveSettings() }

        // Check if Supabase is configured
        if (!SupabaseClientProvider.isConfigured(this)) {
            Toast.makeText(this, L.strings.supabaseNotConfigured, Toast.LENGTH_LONG).show()
            btnSaveSettings.isEnabled = false
            return
        }

        loadSettings()
    }

    private fun loadSettings() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val settings = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("notification_settings")
                        .select()
                        .decodeSingleOrNull<NotificationSettingsDto>()
                }

                currentSettings = settings ?: NotificationSettingsDto()
                populateForm(currentSettings!!)
            } catch (e: Exception) {
                Toast.makeText(
                    this@NotificationSettingsActivity,
                    "${L.strings.error}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                // Use defaults
                currentSettings = NotificationSettingsDto()
                populateForm(currentSettings!!)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun populateForm(settings: NotificationSettingsDto) {
        switchExpiryEnabled.isChecked = settings.expiryAlertEnabled == 1
        editExpiryWarningDays.setText(settings.expiryWarningDays.toString())
        switchLowStockEnabled.isChecked = settings.lowStockAlertEnabled == 1
    }

    private fun saveSettings() {
        val expiryWarningDays = editExpiryWarningDays.text.toString().toIntOrNull()
        if (expiryWarningDays == null || expiryWarningDays < 1 || expiryWarningDays > 365) {
            Toast.makeText(this, L.strings.notificationInvalidDays, Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        val updatedSettings = NotificationSettingsDto(
            id = "global",
            expiryAlertEnabled = if (switchExpiryEnabled.isChecked) 1 else 0,
            expiryWarningDays = expiryWarningDays,
            expiryDedupDays = currentSettings?.expiryDedupDays ?: 3,
            expiredDedupDays = currentSettings?.expiredDedupDays ?: 7,
            lowStockAlertEnabled = if (switchLowStockEnabled.isChecked) 1 else 0,
            lowStockDedupDays = currentSettings?.lowStockDedupDays ?: 7,
            updatedAt = System.currentTimeMillis(),
            updatedBy = authManager.getUserId()
        )

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("notification_settings")
                        .upsert(updatedSettings)
                }

                currentSettings = updatedSettings
                Toast.makeText(
                    this@NotificationSettingsActivity,
                    L.strings.settingsSaved,
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@NotificationSettingsActivity,
                    "${L.strings.error}: ${e.message}",
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
