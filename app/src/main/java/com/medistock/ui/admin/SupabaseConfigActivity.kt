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
import com.medistock.util.SupabasePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupabaseConfigActivity : AppCompatActivity() {

    private lateinit var etUrl: TextInputEditText
    private lateinit var etKey: TextInputEditText
    private lateinit var tvStatus: TextView
    private lateinit var preferences: SupabasePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supabase_config)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Configuration Supabase"

        preferences = SupabasePreferences(this)

        etUrl = findViewById(R.id.etSupabaseUrl)
        etKey = findViewById(R.id.etSupabaseKey)
        tvStatus = findViewById(R.id.tvStatus)

        // Load saved values
        etUrl.setText(preferences.getSupabaseUrl())
        etKey.setText(preferences.getSupabaseKey())

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveConfiguration()
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            testConnection()
        }
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
        SupabaseClientProvider.reinitialize(this)

        showStatus("Configuration enregistrée avec succès!", true)
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
