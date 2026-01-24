package com.medistock.data.remote

import com.medistock.shared.domain.auth.MigrateUserRequest
import com.medistock.shared.domain.auth.MigrateUserResponse
import com.medistock.shared.domain.auth.OnlineFirstAuthResult
import com.medistock.shared.domain.auth.parseAuthError
import com.medistock.shared.domain.auth.toUser
import com.medistock.shared.domain.model.User
import com.medistock.util.AuthManager
import com.medistock.util.DebugConfig
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserSession
import io.ktor.client.call.body
import kotlinx.serialization.json.Json

/**
 * Sealed class for Supabase Auth results
 */
sealed class SupabaseAuthResult {
    /**
     * Authentication successful with Supabase Auth
     */
    data class Success(
        val user: User,
        val session: AuthManager.SupabaseSession
    ) : SupabaseAuthResult()

    /**
     * Invalid credentials (wrong password)
     */
    data object InvalidCredentials : SupabaseAuthResult()

    /**
     * User not found
     */
    data object UserNotFound : SupabaseAuthResult()

    /**
     * User is inactive
     */
    data object UserInactive : SupabaseAuthResult()

    /**
     * Supabase is not configured
     */
    data object NotConfigured : SupabaseAuthResult()

    /**
     * Error during authentication
     */
    data class Error(val message: String) : SupabaseAuthResult()
}

/**
 * Service for handling Supabase Auth operations.
 * Implements UUID-based email authentication pattern.
 */
class SupabaseAuthService {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "SupabaseAuthService"
        private const val EMAIL_DOMAIN = "medistock.local"

        /**
         * Convert a user UUID to the Supabase Auth email format
         */
        fun uuidToAuthEmail(uuid: String): String {
            return "$uuid@$EMAIL_DOMAIN"
        }
    }

    /**
     * Authenticate a user using Supabase Auth.
     *
     * Flow:
     * 1. Look up username in local DB to get UUID
     * 2. Try Supabase Auth with {uuid}@medistock.local + password
     * 3. If auth fails, call migrate-user-to-auth Edge Function
     * 4. Return session tokens on success
     *
     * @param uuid The user's UUID from local database
     * @param password The plain text password
     * @param localUser The user from local database (for creating User object on success)
     * @return SupabaseAuthResult indicating success or failure
     */
    suspend fun authenticateWithSupabaseAuth(
        uuid: String,
        password: String,
        localUser: User
    ): SupabaseAuthResult {
        // Check if Supabase is configured and initialized
        if (!SupabaseClientProvider.isConfigured() || !SupabaseClientProvider.isInitialized()) {
            return SupabaseAuthResult.NotConfigured
        }

        val authEmail = uuidToAuthEmail(uuid)

        return try {
            val client = SupabaseClientProvider.client

            // Try to sign in with Supabase Auth
            client.auth.signInWith(Email) {
                email = authEmail
                this.password = password
            }

            // Get the session after successful sign in
            val session = client.auth.currentSessionOrNull()

            if (session != null) {
                val supabaseSession = AuthManager.SupabaseSession(
                    accessToken = session.accessToken,
                    refreshToken = session.refreshToken,
                    expiresAt = session.expiresAt?.epochSeconds ?: 0
                )
                SupabaseAuthResult.Success(localUser, supabaseSession)
            } else {
                SupabaseAuthResult.Error("No session returned after authentication")
            }

        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error"
            DebugConfig.d(TAG, "Supabase Auth failed: $errorMessage")

            // Check if this is an invalid credentials error
            if (errorMessage.contains("Invalid login credentials", ignoreCase = true) ||
                errorMessage.contains("invalid_grant", ignoreCase = true)) {
                // User might not be migrated yet, try the Edge Function
                return migrateUserToAuth(localUser.username, password)
            }

            SupabaseAuthResult.Error(errorMessage)
        }
    }

    /**
     * Call the migrate-user-to-auth Edge Function for BCrypt fallback.
     *
     * This is called when Supabase Auth fails, which likely means the user
     * hasn't been migrated from BCrypt-only authentication yet.
     *
     * The Edge Function will:
     * 1. Verify password against BCrypt hash in database
     * 2. Create Supabase Auth user if not exists
     * 3. Return session tokens
     *
     * @param username The username
     * @param password The plain text password
     * @return SupabaseAuthResult indicating success or failure
     */
    suspend fun migrateUserToAuth(
        username: String,
        password: String
    ): SupabaseAuthResult {
        return try {
            val client = SupabaseClientProvider.client

            val response = client.functions.invoke(
                function = "migrate-user-to-auth",
                body = MigrateUserRequest(username, password)
            )

            val responseBody = response.body<String>()
            val result = json.decodeFromString<MigrateUserResponse>(responseBody)

            val userData = result.user
            val sessionData = result.session
            if (result.success && userData != null && sessionData != null) {
                val user = User(
                    id = userData.id,
                    username = userData.username,
                    password = "", // We don't store password in User model
                    fullName = userData.name,
                    isAdmin = userData.isAdmin,
                    isActive = true, // If login succeeded, user is active
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val session = AuthManager.SupabaseSession(
                    accessToken = sessionData.accessToken,
                    refreshToken = sessionData.refreshToken,
                    expiresAt = sessionData.expiresAt ?: 0
                )

                // Set the session on the Supabase client so RLS works
                val expiresIn = sessionData.expiresAt?.let {
                    (it - System.currentTimeMillis() / 1000).coerceAtLeast(0)
                } ?: 3600L
                client.auth.importSession(
                    UserSession(
                        accessToken = sessionData.accessToken,
                        refreshToken = sessionData.refreshToken,
                        expiresIn = expiresIn,
                        tokenType = "Bearer",
                        user = null
                    )
                )

                SupabaseAuthResult.Success(user, session)
            } else {
                // Parse error message to determine failure type
                val errorMsg = result.error ?: result.message ?: "Unknown error"

                when {
                    errorMsg.contains("Invalid credentials", ignoreCase = true) ->
                        SupabaseAuthResult.InvalidCredentials
                    errorMsg.contains("deactivated", ignoreCase = true) ||
                    errorMsg.contains("inactive", ignoreCase = true) ->
                        SupabaseAuthResult.UserInactive
                    errorMsg.contains("not found", ignoreCase = true) ->
                        SupabaseAuthResult.UserNotFound
                    else ->
                        SupabaseAuthResult.Error(errorMsg)
                }
            }

        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error"
            DebugConfig.d(TAG, "migrate-user-to-auth failed: $errorMessage")
            SupabaseAuthResult.Error("Migration failed: $errorMessage")
        }
    }

    /**
     * Online-first authentication for first-time login.
     * This method authenticates directly with Supabase without requiring a local user.
     *
     * Flow:
     * 1. Call migrate-user-to-auth Edge Function with username/password
     * 2. Edge Function verifies credentials against Supabase DB
     * 3. Returns user data + session tokens
     * 4. Sets session on Supabase client for RLS
     *
     * @param username The username
     * @param password The plain text password
     * @return OnlineFirstAuthResult with user data and session tokens
     */
    suspend fun authenticateOnlineFirst(
        username: String,
        password: String
    ): OnlineFirstAuthResult {
        if (!SupabaseClientProvider.isConfigured() || !SupabaseClientProvider.isInitialized()) {
            return OnlineFirstAuthResult.NotConfigured
        }

        return try {
            val client = SupabaseClientProvider.client

            val response = client.functions.invoke(
                function = "migrate-user-to-auth",
                body = MigrateUserRequest(username, password)
            )

            val responseBody = response.body<String>()
            val result = json.decodeFromString<MigrateUserResponse>(responseBody)

            val sessionData = result.session
            if (result.success && result.user != null && sessionData != null) {
                val user = result.toUser()
                    ?: return OnlineFirstAuthResult.Error("Failed to parse user data")

                // Set the session on the Supabase client so RLS works
                val expiresIn = sessionData.expiresAt?.let {
                    (it - System.currentTimeMillis() / 1000).coerceAtLeast(0)
                } ?: 3600L

                client.auth.importSession(
                    UserSession(
                        accessToken = sessionData.accessToken,
                        refreshToken = sessionData.refreshToken,
                        expiresIn = expiresIn,
                        tokenType = "Bearer",
                        user = null
                    )
                )

                OnlineFirstAuthResult.Success(
                    user = user,
                    accessToken = sessionData.accessToken,
                    refreshToken = sessionData.refreshToken,
                    expiresAt = sessionData.expiresAt ?: (System.currentTimeMillis() / 1000 + 3600)
                )
            } else {
                parseAuthError(result.error ?: result.message)
            }

        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error"
            DebugConfig.d(TAG, "Online-first auth failed: $errorMessage")
            OnlineFirstAuthResult.Error("Authentication failed: $errorMessage")
        }
    }

    /**
     * Sign out from Supabase Auth
     */
    suspend fun signOut() {
        try {
            val client = SupabaseClientProvider.client
            client.auth.signOut()
        } catch (e: Exception) {
            DebugConfig.w(TAG, "Supabase sign out error: ${e.message}")
        }
    }

    /**
     * Refresh the current session using the refresh token
     *
     * @return The new session if successful, null otherwise
     */
    suspend fun refreshSession(): AuthManager.SupabaseSession? {
        return try {
            val client = SupabaseClientProvider.client
            client.auth.refreshCurrentSession()

            val session = client.auth.currentSessionOrNull()
            if (session != null) {
                AuthManager.SupabaseSession(
                    accessToken = session.accessToken,
                    refreshToken = session.refreshToken,
                    expiresAt = session.expiresAt?.epochSeconds ?: 0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            DebugConfig.d(TAG, "Session refresh failed: ${e.message}")
            null
        }
    }

    /**
     * Restore the Supabase SDK session from stored tokens in AuthManager.
     * This should be called on app startup to ensure RLS policies work.
     *
     * @param authManager The AuthManager containing stored tokens
     * @return true if session was restored, false otherwise
     */
    suspend fun restoreSessionIfNeeded(authManager: AuthManager): Boolean {
        if (!SupabaseClientProvider.isConfigured() || !SupabaseClientProvider.isInitialized()) {
            return false
        }

        val client = SupabaseClientProvider.client

        // Check if SDK already has a session
        try {
            val existingSession = client.auth.currentSessionOrNull()
            if (existingSession != null) {
                DebugConfig.d(TAG, "SDK session already active")
                return true
            }
        } catch (e: Exception) {
            DebugConfig.d(TAG, "No SDK session, attempting to restore...")
        }

        // Try to restore from AuthManager
        val storedSession = authManager.getSession() ?: run {
            DebugConfig.d(TAG, "No stored tokens in AuthManager")
            return false
        }

        return try {
            val expiresIn = if (storedSession.expiresAt > 0) {
                (storedSession.expiresAt - System.currentTimeMillis() / 1000).coerceAtLeast(0)
            } else {
                3600L
            }

            client.auth.importSession(
                UserSession(
                    accessToken = storedSession.accessToken,
                    refreshToken = storedSession.refreshToken,
                    expiresIn = expiresIn,
                    tokenType = "Bearer",
                    user = null
                )
            )
            DebugConfig.i(TAG, "Session restored from AuthManager")
            true
        } catch (e: Exception) {
            DebugConfig.w(TAG, "Failed to restore session: ${e.message}")
            false
        }
    }
}
