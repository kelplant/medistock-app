package com.medistock.shared.i18n

/**
 * Supported locales in the app.
 *
 * To add a new language:
 * 1. Add a new entry to this enum
 * 2. Create the corresponding JSON file: strings_{code}.json
 * 3. The LocalizationManager will automatically pick it up
 */
enum class SupportedLocale(
    val code: String,
    val displayName: String,
    val nativeDisplayName: String
) {
    ENGLISH("en", "English", "English"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    SPANISH("es", "Spanish", "Español"),
    ITALIAN("it", "Italian", "Italiano"),
    RUSSIAN("ru", "Russian", "Русский"),
    BEMBA("bm", "Bemba", "Ichibemba"),
    NYANJA("ny", "Nyanja", "Chinyanja");

    companion object {
        /**
         * Default locale used when no preference is set or when a locale is not supported.
         */
        val DEFAULT = ENGLISH

        /**
         * Get a locale by its code, or null if not found.
         */
        fun fromCode(code: String): SupportedLocale? {
            return entries.find { it.code.equals(code, ignoreCase = true) }
        }

        /**
         * Get a locale by its code, or the default locale if not found.
         */
        fun fromCodeOrDefault(code: String): SupportedLocale {
            return fromCode(code) ?: DEFAULT
        }

        /**
         * Get all available locale codes.
         */
        fun availableCodes(): List<String> = entries.map { it.code }
    }
}
