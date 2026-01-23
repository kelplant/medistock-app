package com.medistock.ui.admin

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.medistock.util.DebugConfig
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.sync.SyncManager
import com.medistock.data.sync.SyncScheduler
import com.medistock.util.SecureSupabasePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import com.google.android.material.snackbar.Snackbar

class SupabaseConfigActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_HIDE_KEY = "hide_key"
        private const val TAG = "SupabaseConfig"
    }

    private lateinit var etUrl: TextInputEditText
    private lateinit var etKey: TextInputEditText
    private lateinit var tvStatus: TextView
    private lateinit var tvSyncStatus: TextView
    private lateinit var tvRealtimeStatus: TextView
    private lateinit var tvRealtimeIndicator: TextView
    private lateinit var btnTestRealtime: Button
    private lateinit var preferences: SecureSupabasePreferences
    private lateinit var syncManager: SyncManager
    private var realtimeStatusJob: Job? = null
    private var lastRealtimeStatus: Realtime.Status? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supabase_config)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Configuration Supabase"

        preferences = SecureSupabasePreferences(this)
        syncManager = SyncManager(this)

        etUrl = findViewById(R.id.etSupabaseUrl)
        etKey = findViewById(R.id.etSupabaseKey)
        tvStatus = findViewById(R.id.tvStatus)
        tvSyncStatus = findViewById(R.id.tvSyncStatus)
        tvRealtimeStatus = findViewById(R.id.tvRealtimeStatus)
        tvRealtimeIndicator = findViewById(R.id.tvRealtimeIndicator)
        btnTestRealtime = findViewById(R.id.btnTestRealtime)

        if (preferences.isConfigured()) {
            SupabaseClientProvider.initialize(this)
        }

        val hideKey = intent.getBooleanExtra(EXTRA_HIDE_KEY, false)

        // Load saved values
        etUrl.setText(preferences.getSupabaseUrl())
        if (hideKey) {
            etKey.setText("")
        } else {
            etKey.setText(preferences.getSupabaseKey())
        }
        observeRealtimeStatus()

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveConfiguration()
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            testConnection()
        }

        btnTestRealtime.setOnClickListener {
            testRealtime()
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
        updateRealtimeStatus("Realtime non configuré", false)
        realtimeStatusJob?.cancel()
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

            // Run pending migrations after Supabase is configured
            lifecycleScope.launch {
                MedistockApplication.runMigrationsIfNeeded(this@SupabaseConfigActivity, "config")
            }

            SyncScheduler.start(this)
            SyncScheduler.triggerImmediate(this, "config-saved")
            showStatus("Configuration enregistrée avec succès!", true)
            observeRealtimeStatus()
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
                tempPrefs.edit {
                    putString("url", url)
                    putString("key", key)
                }

                // Try to create a client and make a simple query
                io.github.jan.supabase.createSupabaseClient(
                    supabaseUrl = url,
                    supabaseKey = key
                ) {
                    install(io.github.jan.supabase.postgrest.Postgrest)
                }.close()

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

    private fun testRealtime() {
        if (!preferences.isConfigured()) {
            showStatus("Veuillez d'abord configurer Supabase", false)
            return
        }

        updateRealtimeStatus("Test Realtime en cours...", null)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                // Reset any previous session to avoid stale state
                runCatching { client.realtime.disconnect() }
                client.realtime.connect()

                val connected = withTimeoutOrNull(5000) {
                    client.realtime.status.firstOrNull { status ->
                        status == Realtime.Status.CONNECTED || status == Realtime.Status.DISCONNECTED
                    } == Realtime.Status.CONNECTED
                }

                if (connected != true) {
                    Log.e(TAG, "Canal Realtime fermé ou en échec de connexion: ${client.realtime.status.value}")
                    withContext(Dispatchers.Main) {
                        updateRealtimeStatus(
                            "✗ Realtime inaccessible: statut ${client.realtime.status.value}",
                            false
                        )
                    }
                    return@launch
                }

                val channel = client.realtime.channel("healthcheck-${System.currentTimeMillis()}")
                try {
                    withTimeout(4000) {
                        channel.subscribe(blockUntilSubscribed = true)
                    }
                    withContext(Dispatchers.Main) {
                        updateRealtimeStatus("✓ Realtime connecté et réactif", true)
                    }
                } finally {
                    channel.unsubscribe()
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Erreur Realtime inconnue"
                if (errorMessage.contains("token", ignoreCase = true) ||
                    errorMessage.contains("jwt", ignoreCase = true)
                ) {
                    Log.e(TAG, "Token Realtime invalide ou expiré: $errorMessage")
                } else {
                    Log.e(TAG, "Erreur Realtime (canal fermé ?): $errorMessage")
                }
                withContext(Dispatchers.Main) {
                    updateRealtimeStatus("✗ Realtime indisponible: $errorMessage", false)
                }
            }
        }
    }

    private fun showStatus(message: String, isSuccess: Boolean?) {
        tvStatus.text = message
        tvStatus.visibility = TextView.VISIBLE

        when (isSuccess) {
            true -> {
                tvStatus.setTextColor("#4CAF50".toColorInt())
                tvStatus.setBackgroundColor("#E8F5E9".toColorInt())
            }
            false -> {
                tvStatus.setTextColor("#F44336".toColorInt())
                tvStatus.setBackgroundColor("#FFEBEE".toColorInt())
            }
            null -> {
                tvStatus.setTextColor("#FF9800".toColorInt())
                tvStatus.setBackgroundColor("#FFF3E0".toColorInt())
            }
        }
    }

    private fun updateRealtimeStatus(message: String, isSuccess: Boolean?) {
        tvRealtimeStatus.text = message
        tvRealtimeStatus.visibility = TextView.VISIBLE

        when (isSuccess) {
            true -> {
                tvRealtimeStatus.setTextColor("#4CAF50".toColorInt())
                tvRealtimeStatus.setBackgroundColor("#E8F5E9".toColorInt())
            }
            false -> {
                tvRealtimeStatus.setTextColor("#F44336".toColorInt())
                tvRealtimeStatus.setBackgroundColor("#FFEBEE".toColorInt())
            }
            null -> {
                tvRealtimeStatus.setTextColor("#FF9800".toColorInt())
                tvRealtimeStatus.setBackgroundColor("#FFF3E0".toColorInt())
            }
        }
    }

    private fun observeRealtimeStatus() {
        realtimeStatusJob?.cancel()

        if (!preferences.isConfigured()) {
            updateRealtimeStatus("Realtime non configuré", false)
            updateRealtimeIndicator(null)
            return
        }

        val client = runCatching { SupabaseClientProvider.client }.getOrElse {
            updateRealtimeStatus("Client Supabase non initialisé", false)
            return
        }

        realtimeStatusJob = lifecycleScope.launch {
            try {
                val connected = withTimeoutOrNull(5000) {
                    // Always start from a clean state before observing
                    runCatching { client.realtime.disconnect() }
                    client.realtime.connect()
                    client.realtime.status.firstOrNull { status ->
                        status == Realtime.Status.CONNECTED
                    }
                }
                if (connected == null) {
                    updateRealtimeStatus("Realtime inaccessible (timeout)", false)
                    updateRealtimeIndicator(Realtime.Status.DISCONNECTED)
                    return@launch
                } else {
                    updateRealtimeIndicator(Realtime.Status.CONNECTED)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Impossible de connecter Realtime: ${e.message}", e)
                updateRealtimeStatus("Realtime indisponible: ${e.message}", false)
                updateRealtimeIndicator(Realtime.Status.DISCONNECTED)
                return@launch
            }

            client.realtime.status.collectLatest { status ->
                handleRealtimeStatusChange(status)
                val (message, success) = when (status) {
                    Realtime.Status.CONNECTED -> "Realtime connecté" to true
                    Realtime.Status.CONNECTING -> "Connexion Realtime..." to null
                    else -> {
                        DebugConfig.w(TAG, "Etat Realtime: $status")
                        "Realtime déconnecté" to false
                    }
                }
                updateRealtimeStatus(message, success)
            }
        }
    }

    private fun handleRealtimeStatusChange(status: Realtime.Status) {
        if (status != lastRealtimeStatus && lastRealtimeStatus != null) {
            val snackbarMessage = if (status == Realtime.Status.CONNECTED) {
                "Realtime connecté"
            } else {
                "Realtime déconnecté"
            }
            Snackbar.make(findViewById(android.R.id.content), snackbarMessage, Snackbar.LENGTH_SHORT).show()
        }
        lastRealtimeStatus = status
        updateRealtimeIndicator(status)
    }

    private fun updateRealtimeIndicator(status: Realtime.Status?) {
        when (status) {
            Realtime.Status.CONNECTED -> {
                tvRealtimeIndicator.text = "Realtime connecté"
                tvRealtimeIndicator.setBackgroundColor("#4CAF50".toColorInt())
                tvRealtimeIndicator.setTextColor("#FFFFFF".toColorInt())
            }
            Realtime.Status.CONNECTING -> {
                tvRealtimeIndicator.text = "Connexion..."
                tvRealtimeIndicator.setBackgroundColor("#FFC107".toColorInt())
                tvRealtimeIndicator.setTextColor("#000000".toColorInt())
            }
            Realtime.Status.DISCONNECTED, null -> {
                tvRealtimeIndicator.text = "Realtime déconnecté"
                tvRealtimeIndicator.setBackgroundColor("#F44336".toColorInt())
                tvRealtimeIndicator.setTextColor("#FFFFFF".toColorInt())
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
                tvSyncStatus.setTextColor("#4CAF50".toColorInt())
                tvSyncStatus.setBackgroundColor("#E8F5E9".toColorInt())
            }
            false -> {
                tvSyncStatus.setTextColor("#F44336".toColorInt())
                tvSyncStatus.setBackgroundColor("#FFEBEE".toColorInt())
            }
            null -> {
                tvSyncStatus.setTextColor("#2196F3".toColorInt())
                tvSyncStatus.setBackgroundColor("#E3F2FD".toColorInt())
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

    override fun onDestroy() {
        realtimeStatusJob?.cancel()
        super.onDestroy()
    }
}
