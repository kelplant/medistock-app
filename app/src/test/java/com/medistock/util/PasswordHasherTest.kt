package com.medistock.util

import org.junit.Assert.*
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun hashPassword_producesValidBCryptHash() {
        // Given
        val plainPassword = "mySecurePassword123"

        // When
        val hashedPassword = PasswordHasher.hashPassword(plainPassword)

        // Then
        assertNotNull(hashedPassword)
        assertTrue(hashedPassword.isNotEmpty())
        assertTrue(hashedPassword.startsWith("$2a$") ||
                   hashedPassword.startsWith("$2b$") ||
                   hashedPassword.startsWith("$2y$"))
    }

    @Test
    fun hashPassword_producesDifferentHashesForSamePassword() {
        // Given
        val plainPassword = "password123"

        // When
        val hash1 = PasswordHasher.hashPassword(plainPassword)
        val hash2 = PasswordHasher.hashPassword(plainPassword)

        // Then - Should be different due to salt
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun verifyPassword_correctPassword_returnsTrue() {
        // Given
        val plainPassword = "correctPassword"
        val hashedPassword = PasswordHasher.hashPassword(plainPassword)

        // When
        val result = PasswordHasher.verifyPassword(plainPassword, hashedPassword)

        // Then
        assertTrue(result)
    }

    @Test
    fun verifyPassword_incorrectPassword_returnsFalse() {
        // Given
        val plainPassword = "correctPassword"
        val wrongPassword = "wrongPassword"
        val hashedPassword = PasswordHasher.hashPassword(plainPassword)

        // When
        val result = PasswordHasher.verifyPassword(wrongPassword, hashedPassword)

        // Then
        assertFalse(result)
    }

    @Test
    fun verifyPassword_emptyPassword_returnsFalse() {
        // Given
        val hashedPassword = PasswordHasher.hashPassword("password")

        // When
        val result = PasswordHasher.verifyPassword("", hashedPassword)

        // Then
        assertFalse(result)
    }

    @Test
    fun verifyPassword_invalidHash_returnsFalse() {
        // Given
        val plainPassword = "password"
        val invalidHash = "not-a-valid-hash"

        // When
        val result = PasswordHasher.verifyPassword(plainPassword, invalidHash)

        // Then
        assertFalse(result)
    }

    @Test
    fun isHashed_validBCryptHash_returnsTrue() {
        // Given
        val hashedPassword = PasswordHasher.hashPassword("password")

        // When
        val result = PasswordHasher.isHashed(hashedPassword)

        // Then
        assertTrue(result)
    }

    @Test
    fun isHashed_plainTextPassword_returnsFalse() {
        // Given
        val plainPassword = "myPlainPassword123"

        // When
        val result = PasswordHasher.isHashed(plainPassword)

        // Then
        assertFalse(result)
    }

    @Test
    fun isHashed_emptyString_returnsFalse() {
        // When
        val result = PasswordHasher.isHashed("")

        // Then
        assertFalse(result)
    }

    @Test
    fun isHashed_bcrypt2aFormat_returnsTrue() {
        // Given
        val bcryptHash = "$2a$12$somehashvalue"

        // When
        val result = PasswordHasher.isHashed(bcryptHash)

        // Then
        assertTrue(result)
    }

    @Test
    fun isHashed_bcrypt2bFormat_returnsTrue() {
        // Given
        val bcryptHash = "$2b$12$somehashvalue"

        // When
        val result = PasswordHasher.isHashed(bcryptHash)

        // Then
        assertTrue(result)
    }

    @Test
    fun isHashed_bcrypt2yFormat_returnsTrue() {
        // Given
        val bcryptHash = "$2y$12$somehashvalue"

        // When
        val result = PasswordHasher.isHashed(bcryptHash)

        // Then
        assertTrue(result)
    }

    @Test
    fun passwordWorkflow_hashAndVerify() {
        // Given
        val originalPassword = "UserPassword@2024"

        // When - User registration
        val storedHash = PasswordHasher.hashPassword(originalPassword)

        // Then - User login
        assertTrue(PasswordHasher.verifyPassword(originalPassword, storedHash))
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", storedHash))
    }

    @Test
    fun hashPassword_specialCharacters_handledCorrectly() {
        // Given
        val passwordWithSpecialChars = "P@ssw0rd!#$%^&*()"

        // When
        val hashedPassword = PasswordHasher.hashPassword(passwordWithSpecialChars)

        // Then
        assertTrue(PasswordHasher.verifyPassword(passwordWithSpecialChars, hashedPassword))
        assertTrue(PasswordHasher.isHashed(hashedPassword))
    }

    @Test
    fun hashPassword_unicode_handledCorrectly() {
        // Given
        val unicodePassword = "Пароль123"

        // When
        val hashedPassword = PasswordHasher.hashPassword(unicodePassword)

        // Then
        assertTrue(PasswordHasher.verifyPassword(unicodePassword, hashedPassword))
    }

    @Test
    fun verifyPassword_caseSensitive() {
        // Given
        val password = "Password123"
        val hashedPassword = PasswordHasher.hashPassword(password)

        // When/Then
        assertTrue(PasswordHasher.verifyPassword("Password123", hashedPassword))
        assertFalse(PasswordHasher.verifyPassword("password123", hashedPassword))
        assertFalse(PasswordHasher.verifyPassword("PASSWORD123", hashedPassword))
    }
}
