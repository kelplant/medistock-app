package com.medistock.shared.domain.auth

import com.medistock.shared.domain.model.User

/**
 * Represents the result of an authentication attempt.
 */
sealed class AuthResult {
    /**
     * Authentication succeeded.
     * @param user The authenticated user.
     */
    data class Success(val user: User) : AuthResult()

    /**
     * The provided credentials are invalid (wrong password).
     */
    data object InvalidCredentials : AuthResult()

    /**
     * The user account exists but is inactive.
     */
    data object UserInactive : AuthResult()

    /**
     * No user found with the provided username.
     */
    data object UserNotFound : AuthResult()

    /**
     * A network or database error occurred.
     * @param message Description of the error.
     */
    data class Error(val message: String) : AuthResult()

    /**
     * Returns true if authentication was successful.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns the user if authentication was successful, null otherwise.
     */
    fun userOrNull(): User? = (this as? Success)?.user
}
