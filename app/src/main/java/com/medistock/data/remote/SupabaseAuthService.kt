package com.medistock.data.remote

import com.medistock.shared.domain.model.User
import com.medistock.util.AuthManager
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.ktor.client.call.body
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Response from migrate-user-to-auth Edge Function
 */
@Serializable
data class MigrateUserResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val user: MigrateUserData? = null,
    val session: MigrateSessionData? = null
)

@Serializable
data class MigrateUserData(
    val id: String,
    val username: String,
    val name: String,
    val isAdmin: Boolean
)

@Serializable
data class MigrateSessionData(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long? = null
)

/**
 * Request body for migrate-user-to-auth Edge Function
 */
@Serializable
data class MigrateUserRequest(
    val username: String,
    val password: String
)

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
            println("Supabase Auth failed: $errorMessage")

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

            val requestBody = json.encodeToString(
                MigrateUserRequest.serializer(),
                MigrateUserRequest(username, password)
            )

            val response = client.functions.invoke(
                function = "migrate-user-to-auth",
                body = requestBody,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, "application/json")
                }
            )

            val responseBody = response.body<String>()
            val result = json.decodeFromString<MigrateUserResponse>(responseBody)

            if (result.success && result.user != null && result.session != null) {
                val user = User(
                    id = result.user.id,
                    username = result.user.username,
                    password = "", // We don't store password in User model
                    fullName = result.user.name,
                    isAdmin = result.user.isAdmin,
                    isActive = true, // If login succeeded, user is active
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val session = AuthManager.SupabaseSession(
                    accessToken = result.session.accessToken,
                    refreshToken = result.session.refreshToken,
                    expiresAt = result.session.expiresAt ?: 0
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
            println("migrate-user-to-auth failed: $errorMessage")
            SupabaseAuthResult.Error("Migration failed: $errorMessage")
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
            println("Supabase sign out error: ${e.message}")
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
            println("Session refresh failed: ${e.message}")
            null
        }
    }
}
