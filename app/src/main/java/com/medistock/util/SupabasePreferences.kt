package com.medistock.util

import android.content.Context
import android.content.SharedPreferences

class SupabasePreferences(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "supabase_config"
        private const val KEY_URL = "supabase_url"
        private const val KEY_API_KEY = "supabase_key"
        private const val KEY_CONFIGURED = "is_configured"
        private const val KEY_SYNC_MODE = "sync_mode"
    }

    enum class SyncMode {
        REALTIME,
        LOCAL
    }

    fun saveSupabaseConfig(url: String, apiKey: String) {
        preferences.edit()
            .putString(KEY_URL, url)
            .putString(KEY_API_KEY, apiKey)
            .putBoolean(KEY_CONFIGURED, true)
            .apply()
    }

    fun getSupabaseUrl(): String {
        return preferences.getString(KEY_URL, "") ?: ""
    }

    fun getSupabaseKey(): String {
        return preferences.getString(KEY_API_KEY, "") ?: ""
    }

    fun isConfigured(): Boolean {
        return preferences.getBoolean(KEY_CONFIGURED, false) &&
                getSupabaseUrl().isNotEmpty() &&
                getSupabaseKey().isNotEmpty()
    }

    fun setSyncMode(mode: SyncMode) {
        preferences.edit()
            .putString(KEY_SYNC_MODE, mode.name)
            .apply()
    }

    fun getSyncMode(): SyncMode {
        val stored = preferences.getString(KEY_SYNC_MODE, SyncMode.LOCAL.name) ?: SyncMode.LOCAL.name
        return runCatching { SyncMode.valueOf(stored) }.getOrDefault(SyncMode.LOCAL)
    }

    fun clearConfiguration() {
        preferences.edit()
            .clear()
            .apply()
    }
}
