package com.medistock.shared.domain.auth

import com.medistock.shared.domain.model.User
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Response from the migrate-user-to-auth Edge Function.
 * This is the shared DTO used by both Android and iOS.
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
 * Result of online-first authentication.
 */
sealed class OnlineFirstAuthResult {
    /**
     * Successfully authenticated online. Contains user data and session tokens.
     */
    data class Success(
        val user: User,
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Long
    ) : OnlineFirstAuthResult()

    /**
     * Invalid credentials (wrong password)
     */
    data object InvalidCredentials : OnlineFirstAuthResult()

    /**
     * User not found in remote database
     */
    data object UserNotFound : OnlineFirstAuthResult()

    /**
     * User account is deactivated
     */
    data object UserInactive : OnlineFirstAuthResult()

    /**
     * Network is required but not available
     */
    data object NetworkRequired : OnlineFirstAuthResult()

    /**
     * Supabase is not configured
     */
    data object NotConfigured : OnlineFirstAuthResult()

    /**
     * Error during authentication
     */
    data class Error(val message: String) : OnlineFirstAuthResult()
}

/**
 * Configuration for determining authentication mode.
 */
object OnlineFirstAuthConfig {
    /**
     * Determines if this is a first-time login that requires network.
     * First login = no users in local database (except system admin marker)
     *
     * @param hasLocalUsers true if there are real users in local database
     * @param hasStoredSession true if there's a valid stored session
     * @return true if online authentication is required
     */
    fun requiresOnlineAuth(hasLocalUsers: Boolean, hasStoredSession: Boolean): Boolean {
        // First login requires network to sync users
        if (!hasLocalUsers) return true

        // If we have local users and a stored session, offline is OK
        return false
    }
}

/**
 * Converts MigrateUserResponse to User domain model.
 */
fun MigrateUserResponse.toUser(): User? {
    val userData = user ?: return null
    val now = Clock.System.now().toEpochMilliseconds()
    return User(
        id = userData.id,
        username = userData.username,
        password = "", // Password is not returned from server
        fullName = userData.name,
        language = null,
        isAdmin = userData.isAdmin,
        isActive = true, // If login succeeded, user is active
        createdAt = now,
        updatedAt = now,
        createdBy = userData.id,
        updatedBy = userData.id
    )
}

/**
 * Parses error message to determine failure type.
 */
fun parseAuthError(errorMessage: String?): OnlineFirstAuthResult {
    if (errorMessage == null) return OnlineFirstAuthResult.Error("Unknown error")

    return when {
        errorMessage.contains("Invalid credentials", ignoreCase = true) ||
        errorMessage.contains("Invalid password", ignoreCase = true) ||
        errorMessage.contains("invalid_grant", ignoreCase = true) ->
            OnlineFirstAuthResult.InvalidCredentials

        errorMessage.contains("deactivated", ignoreCase = true) ||
        errorMessage.contains("inactive", ignoreCase = true) ->
            OnlineFirstAuthResult.UserInactive

        errorMessage.contains("not found", ignoreCase = true) ||
        errorMessage.contains("User not found", ignoreCase = true) ->
            OnlineFirstAuthResult.UserNotFound

        else ->
            OnlineFirstAuthResult.Error(errorMessage)
    }
}
