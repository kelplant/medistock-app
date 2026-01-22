package com.medistock.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * Secure storage for Supabase credentials using EncryptedSharedPreferences.
 *
 * This class replaces SupabasePreferences with encrypted storage.
 * Credentials are encrypted using AES256-GCM (values) and AES256-SIV (keys).
 *
 * Migration from legacy unencrypted storage is handled automatically.
 */
class SecureSupabasePreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        SECURE_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Legacy preferences for migration
    private val legacyPreferences: SharedPreferences =
        context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateFromLegacyIfNeeded()
    }

    companion object {
        private const val TAG = "SecureSupabasePrefs"
        private const val SECURE_PREFS_NAME = "secure_supabase_config"
        private const val LEGACY_PREFS_NAME = "supabase_config"
        private const val KEY_URL = "supabase_url"
        private const val KEY_API_KEY = "supabase_key"
        private const val KEY_CONFIGURED = "is_configured"
        private const val KEY_SYNC_MODE = "sync_mode"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_MIGRATED = "migrated_to_secure"
    }

    enum class SyncMode {
        REALTIME,
        LOCAL
    }

    /**
     * Migrate credentials from legacy unencrypted SharedPreferences to secure storage.
     */
    private fun migrateFromLegacyIfNeeded() {
        // Check if already migrated
        if (securePreferences.getBoolean(KEY_MIGRATED, false)) {
            return
        }

        val legacyUrl = legacyPreferences.getString(KEY_URL, null)
        val legacyKey = legacyPreferences.getString(KEY_API_KEY, null)
        val legacyClientId = legacyPreferences.getString(KEY_CLIENT_ID, null)
        val legacySyncMode = legacyPreferences.getString(KEY_SYNC_MODE, null)
        val legacyConfigured = legacyPreferences.getBoolean(KEY_CONFIGURED, false)

        if (!legacyUrl.isNullOrEmpty() && !legacyKey.isNullOrEmpty()) {
            DebugConfig.i(TAG, "Migrating credentials from legacy storage to encrypted storage")

            securePreferences.edit()
                .putString(KEY_URL, legacyUrl)
                .putString(KEY_API_KEY, legacyKey)
                .putBoolean(KEY_CONFIGURED, legacyConfigured)
                .putString(KEY_CLIENT_ID, legacyClientId)
                .putString(KEY_SYNC_MODE, legacySyncMode)
                .putBoolean(KEY_MIGRATED, true)
                .apply()

            // Clear legacy preferences
            legacyPreferences.edit().clear().apply()

            DebugConfig.i(TAG, "Migration complete, legacy storage cleared")
        } else {
            // No legacy data, just mark as migrated
            securePreferences.edit().putBoolean(KEY_MIGRATED, true).apply()
        }
    }

    fun saveSupabaseConfig(url: String, apiKey: String) {
        securePreferences.edit()
            .putString(KEY_URL, url)
            .putString(KEY_API_KEY, apiKey)
            .putBoolean(KEY_CONFIGURED, true)
            .apply()
        DebugConfig.d(TAG, "Supabase config saved securely")
    }

    fun getSupabaseUrl(): String {
        return securePreferences.getString(KEY_URL, "") ?: ""
    }

    fun getSupabaseKey(): String {
        return securePreferences.getString(KEY_API_KEY, "") ?: ""
    }

    fun isConfigured(): Boolean {
        return securePreferences.getBoolean(KEY_CONFIGURED, false) &&
                getSupabaseUrl().isNotEmpty() &&
                getSupabaseKey().isNotEmpty()
    }

    fun setSyncMode(mode: SyncMode) {
        securePreferences.edit()
            .putString(KEY_SYNC_MODE, mode.name)
            .apply()
    }

    fun getSyncMode(): SyncMode {
        val stored = securePreferences.getString(KEY_SYNC_MODE, SyncMode.LOCAL.name) ?: SyncMode.LOCAL.name
        return runCatching { SyncMode.valueOf(stored) }.getOrDefault(SyncMode.LOCAL)
    }

    fun getOrCreateClientId(): String {
        val existing = securePreferences.getString(KEY_CLIENT_ID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val newId = UUID.randomUUID().toString()
        securePreferences.edit()
            .putString(KEY_CLIENT_ID, newId)
            .apply()
        return newId
    }

    fun clearConfiguration() {
        securePreferences.edit()
            .remove(KEY_URL)
            .remove(KEY_API_KEY)
            .putBoolean(KEY_CONFIGURED, false)
            .apply()
        DebugConfig.d(TAG, "Supabase configuration cleared")
    }
}
