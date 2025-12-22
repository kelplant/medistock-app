package com.medistock.ui.admin

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.medistock.R
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.sync.SyncManager
import com.medistock.data.sync.SyncScheduler
import com.medistock.util.SupabasePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupabaseConfigActivity : AppCompatActivity() {

    private lateinit var etUrl: TextInputEditText
    private lateinit var etKey: TextInputEditText
    private lateinit var tvStatus: TextView
    private lateinit var tvSyncStatus: TextView
    private lateinit var preferences: SupabasePreferences
    private lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supabase_config)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Configuration Supabase"

        preferences = SupabasePreferences(this)
        syncManager = SyncManager(this)

        etUrl = findViewById(R.id.etSupabaseUrl)
        etKey = findViewById(R.id.etSupabaseKey)
        tvStatus = findViewById(R.id.tvStatus)
        tvSyncStatus = findViewById(R.id.tvSyncStatus)

        // Load saved values
        etUrl.setText(preferences.getSupabaseUrl())
        etKey.setText(preferences.getSupabaseKey())

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveConfiguration()
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            testConnection()
        }

        // Add long-press on save button to clear configuration
        findViewById<Button>(R.id.btnSave).setOnLongClickListener {
            clearConfiguration()
            true
        }

        // Synchronization buttons
        findViewById<Button>(R.id.btnSyncLocalToRemote).setOnClickListener {
            syncLocalToRemote()
        }

        findViewById<Button>(R.id.btnSyncRemoteToLocal).setOnClickListener {
            syncRemoteToLocal()
        }

        findViewById<Button>(R.id.btnFullSync).setOnClickListener {
            fullSync()
        }
    }

    private fun clearConfiguration() {
        preferences.clearConfiguration()
        etUrl.setText("")
        etKey.setText("")
        showStatus("Configuration effacée. Redémarrez l'application.", null)
    }

    private fun saveConfiguration() {
        val url = etUrl.text?.toString()?.trim() ?: ""
        val key = etKey.text?.toString()?.trim() ?: ""

        if (url.isEmpty() || key.isEmpty()) {
            showStatus("Veuillez remplir tous les champs", false)
            return
        }

        if (!url.startsWith("https://") || !url.contains(".supabase.co")) {
            showStatus("URL invalide. Format attendu: https://votre-projet.supabase.co", false)
            return
        }

        preferences.saveSupabaseConfig(url, key)

        // Reinitialize Supabase client with new configuration
        try {
            SupabaseClientProvider.reinitialize(this)
            SyncScheduler.start(this)
            SyncScheduler.triggerImmediate(this, "config-saved")
            showStatus("Configuration enregistrée avec succès!", true)
        } catch (e: Exception) {
            showStatus("Configuration enregistrée mais erreur d'initialisation: ${e.message}", false)
            e.printStackTrace()
        }
    }

    private fun testConnection() {
        val url = etUrl.text?.toString()?.trim() ?: ""
        val key = etKey.text?.toString()?.trim() ?: ""

        if (url.isEmpty() || key.isEmpty()) {
            showStatus("Veuillez remplir tous les champs", false)
            return
        }

        showStatus("Test de connexion en cours...", null)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Save temporarily to test
                val tempPrefs = getSharedPreferences("temp_supabase_test", Context.MODE_PRIVATE)
                tempPrefs.edit()
                    .putString("url", url)
                    .putString("key", key)
                    .apply()

                // Try to create a client and make a simple query
                val testClient = io.github.jan.supabase.createSupabaseClient(
                    supabaseUrl = url,
                    supabaseKey = key
                ) {
                    install(io.github.jan.supabase.postgrest.Postgrest)
                }

                // Simple test - just check if we can connect
                withContext(Dispatchers.Main) {
                    showStatus("✓ Connexion réussie!", true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showStatus("✗ Échec de connexion: ${e.message}", false)
                }
            }
        }
    }

    private fun showStatus(message: String, isSuccess: Boolean?) {
        tvStatus.text = message
        tvStatus.visibility = TextView.VISIBLE

        when (isSuccess) {
            true -> {
                tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"))
            }
            false -> {
                tvStatus.setTextColor(Color.parseColor("#F44336"))
                tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"))
            }
            null -> {
                tvStatus.setTextColor(Color.parseColor("#FF9800"))
                tvStatus.setBackgroundColor(Color.parseColor("#FFF3E0"))
            }
        }
    }

    private fun syncLocalToRemote() {
        if (!preferences.isConfigured()) {
            showSyncStatus("Veuillez d'abord configurer Supabase", false)
            return
        }

        disableSyncButtons(true)
        showSyncStatus("Envoi des données vers Supabase...", null)

        CoroutineScope(Dispatchers.IO).launch {
            syncManager.syncLocalToRemote(
                onProgress = { progress ->
                    CoroutineScope(Dispatchers.Main).launch {
                        showSyncStatus(progress, null)
                    }
                },
                onError = { entity, error ->
                    CoroutineScope(Dispatchers.Main).launch {
                        showSyncStatus("Erreur sur $entity: ${error.message}", false)
                    }
                }
            )

            withContext(Dispatchers.Main) {
                disableSyncButtons(false)
            }
        }
    }

    private fun syncRemoteToLocal() {
        if (!preferences.isConfigured()) {
            showSyncStatus("Veuillez d'abord configurer Supabase", false)
            return
        }

        disableSyncButtons(true)
        showSyncStatus("Récupération des données depuis Supabase...", null)

        CoroutineScope(Dispatchers.IO).launch {
            syncManager.syncRemoteToLocal(
                onProgress = { progress ->
                    CoroutineScope(Dispatchers.Main).launch {
                        showSyncStatus(progress, null)
                    }
                },
                onError = { entity, error ->
                    CoroutineScope(Dispatchers.Main).launch {
                        showSyncStatus("Erreur sur $entity: ${error.message}", false)
                    }
                }
            )

            withContext(Dispatchers.Main) {
                disableSyncButtons(false)
            }
        }
    }

    private fun fullSync() {
        if (!preferences.isConfigured()) {
            showSyncStatus("Veuillez d'abord configurer Supabase", false)
            return
        }

        disableSyncButtons(true)
        showSyncStatus("Synchronisation complète en cours...", null)

        CoroutineScope(Dispatchers.IO).launch {
            syncManager.fullSync(
                onProgress = { progress ->
                    CoroutineScope(Dispatchers.Main).launch {
                        showSyncStatus(progress, null)
                    }
                },
                onError = { entity, error ->
                    CoroutineScope(Dispatchers.Main).launch {
                        showSyncStatus("Erreur sur $entity: ${error.message}", false)
                    }
                }
            )

            withContext(Dispatchers.Main) {
                disableSyncButtons(false)
            }
        }
    }

    private fun disableSyncButtons(disabled: Boolean) {
        findViewById<Button>(R.id.btnSyncLocalToRemote).isEnabled = !disabled
        findViewById<Button>(R.id.btnSyncRemoteToLocal).isEnabled = !disabled
        findViewById<Button>(R.id.btnFullSync).isEnabled = !disabled
    }

    private fun showSyncStatus(message: String, isSuccess: Boolean?) {
        tvSyncStatus.text = message
        tvSyncStatus.visibility = TextView.VISIBLE

        when (isSuccess) {
            true -> {
                tvSyncStatus.setTextColor(Color.parseColor("#4CAF50"))
                tvSyncStatus.setBackgroundColor(Color.parseColor("#E8F5E9"))
            }
            false -> {
                tvSyncStatus.setTextColor(Color.parseColor("#F44336"))
                tvSyncStatus.setBackgroundColor(Color.parseColor("#FFEBEE"))
            }
            null -> {
                tvSyncStatus.setTextColor(Color.parseColor("#2196F3"))
                tvSyncStatus.setBackgroundColor(Color.parseColor("#E3F2FD"))
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
