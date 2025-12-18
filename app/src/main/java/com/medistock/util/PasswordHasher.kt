package com.medistock.util

import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * Utility class for password hashing and verification using BCrypt
 */
object PasswordHasher {

    private const val BCRYPT_COST = 12 // Cost factor (higher = more secure but slower)

    /**
     * Hashes a plain text password using BCrypt
     * @param plainPassword The plain text password to hash
     * @return The hashed password
     */
    fun hashPassword(plainPassword: String): String {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray())
    }

    /**
     * Verifies a plain text password against a hashed password
     * @param plainPassword The plain text password to verify
     * @param hashedPassword The hashed password to compare against
     * @return True if the password matches, false otherwise
     */
    fun verifyPassword(plainPassword: String, hashedPassword: String): Boolean {
        return try {
            BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword).verified
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a password is already hashed (BCrypt format)
     * BCrypt hashes start with $2a$, $2b$, or $2y$
     * @param password The password to check
     * @return True if the password appears to be hashed, false otherwise
     */
    fun isHashed(password: String): Boolean {
        return password.startsWith("$2a$") ||
               password.startsWith("$2b$") ||
               password.startsWith("$2y$")
    }
}
