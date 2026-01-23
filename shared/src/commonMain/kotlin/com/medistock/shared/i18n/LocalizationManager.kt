package com.medistock.shared.i18n

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for handling app localization.
 *
 * Usage:
 * ```kotlin
 * // Get the current strings
 * val strings = LocalizationManager.strings
 * println(strings.save) // "Save" in English
 *
 * // Change locale
 * LocalizationManager.setLocale(SupportedLocale.FRENCH)
 * println(strings.save) // "Enregistrer" in French
 *
 * // Use parameterized strings
 * val message = LocalizationManager.format(
 *     strings.welcomeBack,
 *     "name" to "John"
 * )
 * // Result: "Welcome back, John!"
 * ```
 *
 * For iOS (Swift), use the L object which provides direct access to strings:
 * ```swift
 * Text(L.shared.strings.save) // "Save" in English
 * L.shared.setLocaleByCode("fr")
 * Text(L.shared.strings.save) // "Enregistrer" in French
 * ```
 */
object LocalizationManager {

    private val _currentLocale = MutableStateFlow(SupportedLocale.DEFAULT)

    /**
     * Observable current locale.
     */
    val currentLocale: StateFlow<SupportedLocale> = _currentLocale.asStateFlow()

    /**
     * Get the current locale (non-observable).
     */
    val locale: SupportedLocale
        get() = _currentLocale.value

    /**
     * Get the current strings for the active locale.
     */
    val strings: Strings
        get() = getStringsForLocale(_currentLocale.value)

    /**
     * Set the current locale.
     *
     * @param locale The locale to set
     */
    fun setLocale(locale: SupportedLocale) {
        _currentLocale.value = locale
    }

    /**
     * Set the current locale by code.
     *
     * @param code The locale code (e.g., "en", "fr")
     * @return true if the locale was found and set, false if not found (default used)
     */
    fun setLocaleByCode(code: String): Boolean {
        val locale = SupportedLocale.fromCode(code)
        return if (locale != null) {
            setLocale(locale)
            true
        } else {
            setLocale(SupportedLocale.DEFAULT)
            false
        }
    }

    /**
     * Get strings for a specific locale.
     *
     * @param locale The locale to get strings for
     * @return The Strings implementation for that locale
     */
    fun getStringsForLocale(locale: SupportedLocale): Strings {
        return when (locale) {
            SupportedLocale.ENGLISH -> StringsEn
            SupportedLocale.FRENCH -> StringsFr
            SupportedLocale.GERMAN -> StringsDe
            SupportedLocale.SPANISH -> StringsEs
            SupportedLocale.ITALIAN -> StringsIt
            SupportedLocale.RUSSIAN -> StringsRu
            SupportedLocale.BEMBA -> StringsBm
            SupportedLocale.NYANJA -> StringsNy
        }
    }

    /**
     * Format a string with named parameters.
     *
     * Parameters in the string should be in the format {paramName}.
     *
     * Example:
     * ```kotlin
     * format("Hello, {name}!", "name" to "World")
     * // Returns: "Hello, World!"
     * ```
     *
     * @param template The string template with {param} placeholders
     * @param params The parameter name-value pairs
     * @return The formatted string
     */
    fun format(template: String, vararg params: Pair<String, Any>): String {
        var result = template
        for ((key, value) in params) {
            result = result.replace("{$key}", value.toString())
        }
        return result
    }

    /**
     * Get all available locales for display in settings.
     *
     * @return List of pairs (locale code, native display name)
     */
    fun getAvailableLocales(): List<Pair<String, String>> {
        return SupportedLocale.entries.map { it.code to it.nativeDisplayName }
    }

    /**
     * Get the current locale code.
     */
    fun getCurrentLocaleCode(): String = _currentLocale.value.code

    /**
     * Get the current locale native display name.
     */
    fun getCurrentLocaleDisplayName(): String = _currentLocale.value.nativeDisplayName
}

/**
 * Shorthand accessor for localization (L for Localization).
 * Provides convenient access to localized strings.
 *
 * Usage in Kotlin:
 * ```kotlin
 * val saveText = L.strings.save
 * ```
 *
 * Usage in Swift:
 * ```swift
 * let saveText = L.shared.strings.save
 * ```
 */
object L {
    val strings: Strings
        get() = LocalizationManager.strings

    val locale: SupportedLocale
        get() = LocalizationManager.locale

    fun setLocale(locale: SupportedLocale) = LocalizationManager.setLocale(locale)
    fun setLocaleByCode(code: String) = LocalizationManager.setLocaleByCode(code)
    fun format(template: String, vararg params: Pair<String, Any>) = LocalizationManager.format(template, *params)
}
