package com.medistock.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.medistock.shared.domain.model.User

/**
 * Manages user authentication and session with Supabase Auth token storage.
 * Uses EncryptedSharedPreferences for secure token storage.
 */
class AuthManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val PREFS_NAME = "medistock_auth_secure"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_IS_ADMIN = "is_admin"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
        private const val KEY_LANGUAGE = "user_language"

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Session data class for Supabase Auth tokens
     */
    data class SupabaseSession(
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Long
    )

    /**
     * Save user session after successful login (legacy - without tokens)
     */
    fun login(user: User) {
        prefs.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_FULL_NAME, user.fullName)
            putBoolean(KEY_IS_ADMIN, user.isAdmin)
            putBoolean(KEY_IS_LOGGED_IN, true)
            user.language?.let { putString(KEY_LANGUAGE, it) }
            apply()
        }
    }

    /**
     * Save user session with Supabase Auth tokens
     */
    fun loginWithSession(user: User, session: SupabaseSession) {
        prefs.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_FULL_NAME, user.fullName)
            putBoolean(KEY_IS_ADMIN, user.isAdmin)
            putBoolean(KEY_IS_LOGGED_IN, true)
            user.language?.let { putString(KEY_LANGUAGE, it) }
            putString(KEY_ACCESS_TOKEN, session.accessToken)
            putString(KEY_REFRESH_TOKEN, session.refreshToken)
            putLong(KEY_TOKEN_EXPIRES_AT, session.expiresAt)
            apply()
        }
    }

    /**
     * Save Supabase Auth session tokens
     */
    fun saveSession(session: SupabaseSession) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, session.accessToken)
            putString(KEY_REFRESH_TOKEN, session.refreshToken)
            putLong(KEY_TOKEN_EXPIRES_AT, session.expiresAt)
            apply()
        }
    }

    /**
     * Get current Supabase Auth session if available
     */
    fun getSession(): SupabaseSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0)
        return SupabaseSession(accessToken, refreshToken, expiresAt)
    }

    /**
     * Get current access token for API calls
     */
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Check if current session is expired
     */
    fun isSessionExpired(): Boolean {
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0)
        if (expiresAt == 0L) return true
        // Consider expired 5 minutes before actual expiry for safety margin
        return System.currentTimeMillis() / 1000 >= expiresAt - 300
    }

    /**
     * Check if user has valid Supabase Auth session
     */
    fun hasValidSession(): Boolean {
        return isLoggedIn() && getAccessToken() != null && !isSessionExpired()
    }

    /**
     * Clear user session
     */
    fun logout() {
        prefs.edit().clear().apply()
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Get current user ID
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * Get current username
     */
    fun getUsername(): String {
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }

    /**
     * Get current user full name
     */
    fun getFullName(): String {
        return prefs.getString(KEY_FULL_NAME, "") ?: ""
    }

    /**
     * Check if current user is admin
     */
    fun isAdmin(): Boolean {
        return prefs.getBoolean(KEY_IS_ADMIN, false)
    }

    /**
     * Get cached user language preference
     */
    fun getLanguage(): String? {
        return prefs.getString(KEY_LANGUAGE, null)
    }

    /**
     * Set cached user language preference
     */
    fun setLanguage(language: String?) {
        prefs.edit().apply {
            if (language != null) {
                putString(KEY_LANGUAGE, language)
            } else {
                remove(KEY_LANGUAGE)
            }
            apply()
        }
    }
}
