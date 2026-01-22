package com.medistock.shared.domain.auth

import com.medistock.shared.data.repository.UserRepository
import com.medistock.shared.domain.model.User

/**
 * Interface for password verification.
 * Each platform provides its own implementation using BCrypt.
 */
interface PasswordVerifier {
    /**
     * Verifies a plain text password against a stored hash.
     * @param plainPassword The plain text password.
     * @param hashedPassword The stored BCrypt hash.
     * @return True if the password matches.
     */
    fun verify(plainPassword: String, hashedPassword: String): Boolean
}

/**
 * Shared authentication service.
 * Uses platform-specific PasswordVerifier for BCrypt verification.
 */
class AuthService(
    private val userRepository: UserRepository,
    private val passwordVerifier: PasswordVerifier
) {
    /**
     * Authenticate a user with username and password.
     * @param username The username.
     * @param password The plain text password.
     * @return AuthResult indicating success or failure reason.
     */
    suspend fun authenticate(username: String, password: String): AuthResult {
        val trimmedUsername = username.trim()
        val trimmedPassword = password.trim()

        if (trimmedUsername.isEmpty() || trimmedPassword.isEmpty()) {
            return AuthResult.InvalidCredentials
        }

        // Get user by username
        val user = try {
            userRepository.getByUsername(trimmedUsername)
        } catch (e: Exception) {
            return AuthResult.Error(e.message ?: "Database error")
        }

        // Check if user exists
        if (user == null) {
            return AuthResult.UserNotFound
        }

        // Check if user is active
        if (!user.isActive) {
            return AuthResult.UserInactive
        }

        // Verify password
        val passwordValid = try {
            passwordVerifier.verify(trimmedPassword, user.password)
        } catch (e: Exception) {
            return AuthResult.Error("Password verification failed: ${e.message}")
        }

        if (!passwordValid) {
            return AuthResult.InvalidCredentials
        }

        return AuthResult.Success(user)
    }

    /**
     * Get a user by username without password verification.
     * Useful for checking if user exists.
     */
    suspend fun getUserByUsername(username: String): User? {
        return try {
            userRepository.getByUsername(username.trim())
        } catch (e: Exception) {
            null
        }
    }
}
