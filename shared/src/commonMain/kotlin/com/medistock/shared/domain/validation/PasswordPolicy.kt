package com.medistock.shared.domain.validation

import com.medistock.shared.i18n.Strings

/**
 * Password complexity validation policy.
 *
 * Requirements:
 * - Minimum 8 characters
 * - At least 1 uppercase letter (A-Z)
 * - At least 1 lowercase letter (a-z)
 * - At least 1 digit (0-9)
 * - At least 1 special character (!@#$%^&*()_+-=[]{}|;':\",./<>?)
 *
 * Usage:
 * ```kotlin
 * val result = PasswordPolicy.validate("MyPassword123!")
 * if (result.isValid) {
 *     // Password meets all requirements
 * } else {
 *     result.errors.forEach { error ->
 *         println(PasswordPolicy.getErrorMessage(error, strings))
 *     }
 * }
 * ```
 */
object PasswordPolicy {

    /** Minimum password length */
    const val MIN_LENGTH = 8

    /** Special characters considered valid */
    const val SPECIAL_CHARACTERS = "!@#\$%^&*()_+-=[]{}|;':\",./<>?`~"

    /**
     * Result of password validation.
     *
     * @property isValid True if password meets all requirements
     * @property errors List of validation errors (empty if valid)
     * @property strength Password strength level
     * @property criteriaCount Number of criteria met (0-5)
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<PasswordError>,
        val strength: PasswordStrength,
        val criteriaCount: Int
    ) {
        companion object {
            /** Creates a valid result with STRONG strength */
            fun valid() = ValidationResult(
                isValid = true,
                errors = emptyList(),
                strength = PasswordStrength.STRONG,
                criteriaCount = 5
            )
        }
    }

    /**
     * Password validation errors.
     */
    enum class PasswordError {
        TOO_SHORT,
        MISSING_UPPERCASE,
        MISSING_LOWERCASE,
        MISSING_DIGIT,
        MISSING_SPECIAL
    }

    /**
     * Password strength levels.
     */
    enum class PasswordStrength {
        /** 0-2 criteria met */
        WEAK,
        /** 3-4 criteria met */
        MEDIUM,
        /** All 5 criteria met */
        STRONG;

        /**
         * Get the progress percentage for this strength level.
         * Useful for progress bars.
         */
        fun toProgress(): Int = when (this) {
            WEAK -> 33
            MEDIUM -> 66
            STRONG -> 100
        }

        /**
         * Get the color hex for this strength level.
         */
        fun toColorHex(): String = when (this) {
            WEAK -> "#F44336"      // Red
            MEDIUM -> "#FF9800"    // Orange
            STRONG -> "#4CAF50"    // Green
        }

        /**
         * Get RGB components (0-255) for this strength level.
         */
        fun toRGB(): Triple<Int, Int, Int> = when (this) {
            WEAK -> Triple(244, 67, 54)     // Red
            MEDIUM -> Triple(255, 152, 0)   // Orange
            STRONG -> Triple(76, 175, 80)   // Green
        }
    }

    /**
     * Validate a password against all requirements.
     *
     * @param password The password to validate
     * @return ValidationResult containing validity, errors, and strength
     */
    fun validate(password: String): ValidationResult {
        val errors = mutableListOf<PasswordError>()

        // Check minimum length
        if (password.length < MIN_LENGTH) {
            errors.add(PasswordError.TOO_SHORT)
        }

        // Check for uppercase letter
        if (!password.any { it.isUpperCase() }) {
            errors.add(PasswordError.MISSING_UPPERCASE)
        }

        // Check for lowercase letter
        if (!password.any { it.isLowerCase() }) {
            errors.add(PasswordError.MISSING_LOWERCASE)
        }

        // Check for digit
        if (!password.any { it.isDigit() }) {
            errors.add(PasswordError.MISSING_DIGIT)
        }

        // Check for special character
        if (!password.any { it in SPECIAL_CHARACTERS }) {
            errors.add(PasswordError.MISSING_SPECIAL)
        }

        val criteriaCount = 5 - errors.size
        val strength = calculateStrength(criteriaCount)

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            strength = strength,
            criteriaCount = criteriaCount
        )
    }

    /**
     * Get the password strength without full validation.
     * Useful for real-time strength indicators.
     *
     * @param password The password to check
     * @return PasswordStrength level
     */
    fun getStrength(password: String): PasswordStrength {
        var criteriaCount = 0

        if (password.length >= MIN_LENGTH) criteriaCount++
        if (password.any { it.isUpperCase() }) criteriaCount++
        if (password.any { it.isLowerCase() }) criteriaCount++
        if (password.any { it.isDigit() }) criteriaCount++
        if (password.any { it in SPECIAL_CHARACTERS }) criteriaCount++

        return calculateStrength(criteriaCount)
    }

    /**
     * Calculate strength level based on criteria count.
     */
    private fun calculateStrength(criteriaCount: Int): PasswordStrength {
        return when {
            criteriaCount <= 2 -> PasswordStrength.WEAK
            criteriaCount <= 4 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    /**
     * Get localized error message for a password error.
     *
     * @param error The password error
     * @param strings The localized strings instance
     * @return Localized error message
     */
    fun getErrorMessage(error: PasswordError, strings: Strings): String {
        return when (error) {
            PasswordError.TOO_SHORT -> strings.passwordMinLength
            PasswordError.MISSING_UPPERCASE -> strings.passwordNeedsUppercase
            PasswordError.MISSING_LOWERCASE -> strings.passwordNeedsLowercase
            PasswordError.MISSING_DIGIT -> strings.passwordNeedsDigit
            PasswordError.MISSING_SPECIAL -> strings.passwordNeedsSpecial
        }
    }

    /**
     * Get localized strength label.
     *
     * @param strength The password strength
     * @param strings The localized strings instance
     * @return Localized strength label
     */
    fun getStrengthLabel(strength: PasswordStrength, strings: Strings): String {
        return when (strength) {
            PasswordStrength.WEAK -> strings.passwordStrengthWeak
            PasswordStrength.MEDIUM -> strings.passwordStrengthMedium
            PasswordStrength.STRONG -> strings.passwordStrengthStrong
        }
    }

    /**
     * Get all password requirements as localized strings.
     * Useful for displaying requirements to users.
     *
     * @param strings The localized strings instance
     * @return List of requirement descriptions
     */
    fun getRequirements(strings: Strings): List<String> {
        return listOf(
            strings.passwordMinLength,
            strings.passwordNeedsUppercase,
            strings.passwordNeedsLowercase,
            strings.passwordNeedsDigit,
            strings.passwordNeedsSpecial
        )
    }

    /**
     * Check which requirements are met by a password.
     * Returns a map of requirement to whether it's met.
     *
     * @param password The password to check
     * @return Map of PasswordError to whether that requirement is met
     */
    fun checkRequirements(password: String): Map<PasswordError, Boolean> {
        return mapOf(
            PasswordError.TOO_SHORT to (password.length >= MIN_LENGTH),
            PasswordError.MISSING_UPPERCASE to password.any { it.isUpperCase() },
            PasswordError.MISSING_LOWERCASE to password.any { it.isLowerCase() },
            PasswordError.MISSING_DIGIT to password.any { it.isDigit() },
            PasswordError.MISSING_SPECIAL to password.any { it in SPECIAL_CHARACTERS }
        )
    }
}
