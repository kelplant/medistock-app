package com.medistock.shared.domain.validation

import com.medistock.shared.domain.validation.PasswordPolicy.PasswordError
import com.medistock.shared.domain.validation.PasswordPolicy.PasswordStrength
import com.medistock.shared.i18n.LocalizationManager
import com.medistock.shared.i18n.SupportedLocale
import kotlin.test.*

/**
 * Unit tests for PasswordPolicy.
 */
class PasswordPolicyTest {

    @BeforeTest
    fun setUp() {
        LocalizationManager.setLocale(SupportedLocale.ENGLISH)
    }

    // ===== Basic Validation Tests =====

    @Test
    fun `empty password fails all validations`() {
        val result = PasswordPolicy.validate("")

        assertFalse(result.isValid)
        assertEquals(5, result.errors.size)
        assertTrue(result.errors.contains(PasswordError.TOO_SHORT))
        assertTrue(result.errors.contains(PasswordError.MISSING_UPPERCASE))
        assertTrue(result.errors.contains(PasswordError.MISSING_LOWERCASE))
        assertTrue(result.errors.contains(PasswordError.MISSING_DIGIT))
        assertTrue(result.errors.contains(PasswordError.MISSING_SPECIAL))
        assertEquals(PasswordStrength.WEAK, result.strength)
        assertEquals(0, result.criteriaCount)
    }

    @Test
    fun `password too short fails validation`() {
        val result = PasswordPolicy.validate("Abc1!")

        assertFalse(result.isValid)
        assertTrue(result.errors.contains(PasswordError.TOO_SHORT))
    }

    @Test
    fun `password exactly 8 characters passes length check`() {
        val result = PasswordPolicy.validate("Abcdef1!")

        assertFalse(result.errors.contains(PasswordError.TOO_SHORT))
    }

    @Test
    fun `password missing uppercase fails`() {
        val result = PasswordPolicy.validate("abcdefgh1!")

        assertFalse(result.isValid)
        assertTrue(result.errors.contains(PasswordError.MISSING_UPPERCASE))
    }

    @Test
    fun `password missing lowercase fails`() {
        val result = PasswordPolicy.validate("ABCDEFGH1!")

        assertFalse(result.isValid)
        assertTrue(result.errors.contains(PasswordError.MISSING_LOWERCASE))
    }

    @Test
    fun `password missing digit fails`() {
        val result = PasswordPolicy.validate("Abcdefgh!")

        assertFalse(result.isValid)
        assertTrue(result.errors.contains(PasswordError.MISSING_DIGIT))
    }

    @Test
    fun `password missing special character fails`() {
        val result = PasswordPolicy.validate("Abcdefgh1")

        assertFalse(result.isValid)
        assertTrue(result.errors.contains(PasswordError.MISSING_SPECIAL))
    }

    @Test
    fun `valid password passes all checks`() {
        val result = PasswordPolicy.validate("Abcdef1!")

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertEquals(PasswordStrength.STRONG, result.strength)
        assertEquals(5, result.criteriaCount)
    }

    // ===== Strength Tests =====

    @Test
    fun `strength is WEAK when 0 criteria met`() {
        // Empty string meets no criteria
        val strength = PasswordPolicy.getStrength("")
        assertEquals(PasswordStrength.WEAK, strength)
    }

    @Test
    fun `strength is WEAK when 1 criterion met`() {
        // Only lowercase
        val strength = PasswordPolicy.getStrength("abc")
        assertEquals(PasswordStrength.WEAK, strength)
    }

    @Test
    fun `strength is WEAK when 2 criteria met`() {
        // Lowercase + digit
        val strength = PasswordPolicy.getStrength("abc123")
        assertEquals(PasswordStrength.WEAK, strength)
    }

    @Test
    fun `strength is MEDIUM when 3 criteria met`() {
        // Length + lowercase + digit
        val strength = PasswordPolicy.getStrength("abcdefgh1")
        assertEquals(PasswordStrength.MEDIUM, strength)
    }

    @Test
    fun `strength is MEDIUM when 4 criteria met`() {
        // Length + lowercase + uppercase + digit
        val strength = PasswordPolicy.getStrength("Abcdefgh1")
        assertEquals(PasswordStrength.MEDIUM, strength)
    }

    @Test
    fun `strength is STRONG when all 5 criteria met`() {
        // All criteria
        val strength = PasswordPolicy.getStrength("Abcdefgh1!")
        assertEquals(PasswordStrength.STRONG, strength)
    }

    // ===== Special Characters Tests =====

    @Test
    fun `all common special characters are recognized`() {
        val specialChars = "!@#\$%^&*()_+-=[]{}|;':\",./<>?`~"

        specialChars.forEach { char ->
            val password = "Abcdefg1$char"
            val result = PasswordPolicy.validate(password)
            assertFalse(
                result.errors.contains(PasswordError.MISSING_SPECIAL),
                "Special character '$char' should be recognized"
            )
        }
    }

    @Test
    fun `space is not a special character`() {
        val result = PasswordPolicy.validate("Abcdefg1 ")
        assertTrue(result.errors.contains(PasswordError.MISSING_SPECIAL))
    }

    // ===== Strength Progress & Color Tests =====

    @Test
    fun `weak strength returns 33 percent progress`() {
        assertEquals(33, PasswordStrength.WEAK.toProgress())
    }

    @Test
    fun `medium strength returns 66 percent progress`() {
        assertEquals(66, PasswordStrength.MEDIUM.toProgress())
    }

    @Test
    fun `strong strength returns 100 percent progress`() {
        assertEquals(100, PasswordStrength.STRONG.toProgress())
    }

    @Test
    fun `weak strength color is red`() {
        assertEquals("#F44336", PasswordStrength.WEAK.toColorHex())
        assertEquals(Triple(244, 67, 54), PasswordStrength.WEAK.toRGB())
    }

    @Test
    fun `medium strength color is orange`() {
        assertEquals("#FF9800", PasswordStrength.MEDIUM.toColorHex())
        assertEquals(Triple(255, 152, 0), PasswordStrength.MEDIUM.toRGB())
    }

    @Test
    fun `strong strength color is green`() {
        assertEquals("#4CAF50", PasswordStrength.STRONG.toColorHex())
        assertEquals(Triple(76, 175, 80), PasswordStrength.STRONG.toRGB())
    }

    // ===== Error Message Tests =====

    @Test
    fun `error messages are correctly mapped`() {
        val strings = LocalizationManager.strings

        assertEquals(strings.passwordMinLength, PasswordPolicy.getErrorMessage(PasswordError.TOO_SHORT, strings))
        assertEquals(strings.passwordNeedsUppercase, PasswordPolicy.getErrorMessage(PasswordError.MISSING_UPPERCASE, strings))
        assertEquals(strings.passwordNeedsLowercase, PasswordPolicy.getErrorMessage(PasswordError.MISSING_LOWERCASE, strings))
        assertEquals(strings.passwordNeedsDigit, PasswordPolicy.getErrorMessage(PasswordError.MISSING_DIGIT, strings))
        assertEquals(strings.passwordNeedsSpecial, PasswordPolicy.getErrorMessage(PasswordError.MISSING_SPECIAL, strings))
    }

    @Test
    fun `strength labels are correctly mapped`() {
        val strings = LocalizationManager.strings

        assertEquals(strings.passwordStrengthWeak, PasswordPolicy.getStrengthLabel(PasswordStrength.WEAK, strings))
        assertEquals(strings.passwordStrengthMedium, PasswordPolicy.getStrengthLabel(PasswordStrength.MEDIUM, strings))
        assertEquals(strings.passwordStrengthStrong, PasswordPolicy.getStrengthLabel(PasswordStrength.STRONG, strings))
    }

    // ===== Requirements List Test =====

    @Test
    fun `getRequirements returns all 5 requirements`() {
        val requirements = PasswordPolicy.getRequirements(LocalizationManager.strings)
        assertEquals(5, requirements.size)
    }

    // ===== Check Requirements Map Test =====

    @Test
    fun `checkRequirements returns correct map for partial password`() {
        val requirements = PasswordPolicy.checkRequirements("Abc")

        assertFalse(requirements[PasswordError.TOO_SHORT]!!)  // Length < 8
        assertTrue(requirements[PasswordError.MISSING_UPPERCASE]!!)  // Has uppercase
        assertTrue(requirements[PasswordError.MISSING_LOWERCASE]!!)  // Has lowercase
        assertFalse(requirements[PasswordError.MISSING_DIGIT]!!)  // No digit
        assertFalse(requirements[PasswordError.MISSING_SPECIAL]!!)  // No special
    }

    @Test
    fun `checkRequirements returns all true for valid password`() {
        val requirements = PasswordPolicy.checkRequirements("Abcdefg1!")

        assertTrue(requirements.values.all { it })
    }

    // ===== Edge Cases =====

    @Test
    fun `password with only spaces fails all checks`() {
        val result = PasswordPolicy.validate("        ")

        assertFalse(result.isValid)
        assertEquals(4, result.errors.size) // Length met, but nothing else
        assertFalse(result.errors.contains(PasswordError.TOO_SHORT)) // 8 spaces = 8 chars
        assertTrue(result.errors.contains(PasswordError.MISSING_UPPERCASE))
        assertTrue(result.errors.contains(PasswordError.MISSING_LOWERCASE))
        assertTrue(result.errors.contains(PasswordError.MISSING_DIGIT))
        assertTrue(result.errors.contains(PasswordError.MISSING_SPECIAL))
    }

    @Test
    fun `unicode letters are handled correctly`() {
        // Unicode uppercase/lowercase should work
        val result = PasswordPolicy.validate("Äbcdefg1!")

        // 'Ä' is uppercase, 'bcdefg' are lowercase
        assertFalse(result.errors.contains(PasswordError.MISSING_UPPERCASE))
        assertFalse(result.errors.contains(PasswordError.MISSING_LOWERCASE))
    }

    @Test
    fun `very long password is valid if meets criteria`() {
        val longPassword = "A" + "a".repeat(100) + "1!"
        val result = PasswordPolicy.validate(longPassword)

        assertTrue(result.isValid)
    }

    @Test
    fun `min length constant is 8`() {
        assertEquals(8, PasswordPolicy.MIN_LENGTH)
    }

    // ===== Real-world Password Examples =====

    @Test
    fun `common weak passwords fail validation`() {
        val weakPasswords = listOf(
            "password",
            "12345678",
            "qwerty123",
            "abcdefgh",
            "Password1"  // No special character
        )

        weakPasswords.forEach { password ->
            val result = PasswordPolicy.validate(password)
            assertFalse(result.isValid, "Password '$password' should be invalid")
        }
    }

    @Test
    fun `strong passwords pass validation`() {
        val strongPasswords = listOf(
            "Abcdefg1!",
            "MyP@ssw0rd",
            "Secur3P@ss!",
            "C0mplex#Password",
            "Test123!@#"
        )

        strongPasswords.forEach { password ->
            val result = PasswordPolicy.validate(password)
            assertTrue(result.isValid, "Password '$password' should be valid")
        }
    }

    // ===== ValidationResult.valid() Test =====

    @Test
    fun `ValidationResult valid factory creates correct result`() {
        val result = PasswordPolicy.ValidationResult.valid()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertEquals(PasswordStrength.STRONG, result.strength)
        assertEquals(5, result.criteriaCount)
    }

    // ===== Localization Tests =====

    @Test
    fun `error messages change with locale`() {
        LocalizationManager.setLocale(SupportedLocale.FRENCH)
        val frenchStrings = LocalizationManager.strings
        val frenchError = PasswordPolicy.getErrorMessage(PasswordError.TOO_SHORT, frenchStrings)

        LocalizationManager.setLocale(SupportedLocale.ENGLISH)
        val englishStrings = LocalizationManager.strings
        val englishError = PasswordPolicy.getErrorMessage(PasswordError.TOO_SHORT, englishStrings)

        assertNotEquals(frenchError, englishError)
        assertTrue(frenchError.contains("8"))
        assertTrue(englishError.contains("8"))
    }

    @Test
    fun `strength labels available in all supported locales`() {
        SupportedLocale.entries.forEach { locale ->
            LocalizationManager.setLocale(locale)
            val strings = LocalizationManager.strings

            val weak = PasswordPolicy.getStrengthLabel(PasswordStrength.WEAK, strings)
            val medium = PasswordPolicy.getStrengthLabel(PasswordStrength.MEDIUM, strings)
            val strong = PasswordPolicy.getStrengthLabel(PasswordStrength.STRONG, strings)

            assertTrue(weak.isNotBlank(), "Weak label should not be blank for ${locale.code}")
            assertTrue(medium.isNotBlank(), "Medium label should not be blank for ${locale.code}")
            assertTrue(strong.isNotBlank(), "Strong label should not be blank for ${locale.code}")
        }
    }
}
