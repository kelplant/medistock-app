package com.medistock.shared.i18n

import kotlin.test.*

/**
 * Unit tests for LocalizationManager.
 */
class LocalizationManagerTest {

    @BeforeTest
    fun setUp() {
        // Reset to default locale before each test
        LocalizationManager.setLocale(SupportedLocale.DEFAULT)
    }

    // ===== SupportedLocale Tests =====

    @Test
    fun `SupportedLocale DEFAULT is ENGLISH`() {
        assertEquals(SupportedLocale.ENGLISH, SupportedLocale.DEFAULT)
    }

    @Test
    fun `SupportedLocale fromCode returns correct locale`() {
        assertEquals(SupportedLocale.ENGLISH, SupportedLocale.fromCode("en"))
        assertEquals(SupportedLocale.FRENCH, SupportedLocale.fromCode("fr"))
        assertEquals(SupportedLocale.GERMAN, SupportedLocale.fromCode("de"))
        assertEquals(SupportedLocale.SPANISH, SupportedLocale.fromCode("es"))
        assertEquals(SupportedLocale.ITALIAN, SupportedLocale.fromCode("it"))
        assertEquals(SupportedLocale.RUSSIAN, SupportedLocale.fromCode("ru"))
        assertEquals(SupportedLocale.BEMBA, SupportedLocale.fromCode("bm"))
        assertEquals(SupportedLocale.NYANJA, SupportedLocale.fromCode("ny"))
    }

    @Test
    fun `SupportedLocale fromCode is case insensitive`() {
        assertEquals(SupportedLocale.FRENCH, SupportedLocale.fromCode("FR"))
        assertEquals(SupportedLocale.FRENCH, SupportedLocale.fromCode("Fr"))
    }

    @Test
    fun `SupportedLocale fromCode returns null for unknown code`() {
        assertNull(SupportedLocale.fromCode("xx"))
        assertNull(SupportedLocale.fromCode(""))
    }

    @Test
    fun `SupportedLocale fromCodeOrDefault returns default for unknown code`() {
        assertEquals(SupportedLocale.ENGLISH, SupportedLocale.fromCodeOrDefault("xx"))
    }

    @Test
    fun `SupportedLocale availableCodes returns all codes`() {
        val codes = SupportedLocale.availableCodes()
        assertTrue(codes.contains("en"))
        assertTrue(codes.contains("fr"))
        assertTrue(codes.contains("de"))
        assertTrue(codes.contains("es"))
        assertTrue(codes.contains("it"))
        assertTrue(codes.contains("ru"))
        assertTrue(codes.contains("bm"))
        assertTrue(codes.contains("ny"))
        assertEquals(8, codes.size)
    }

    // ===== LocalizationManager Tests =====

    @Test
    fun `LocalizationManager default locale is ENGLISH`() {
        assertEquals(SupportedLocale.ENGLISH, LocalizationManager.locale)
    }

    @Test
    fun `LocalizationManager setLocale changes current locale`() {
        LocalizationManager.setLocale(SupportedLocale.FRENCH)
        assertEquals(SupportedLocale.FRENCH, LocalizationManager.locale)
    }

    @Test
    fun `LocalizationManager setLocaleByCode returns true for valid code`() {
        assertTrue(LocalizationManager.setLocaleByCode("fr"))
        assertEquals(SupportedLocale.FRENCH, LocalizationManager.locale)
    }

    @Test
    fun `LocalizationManager setLocaleByCode returns false and sets default for invalid code`() {
        LocalizationManager.setLocale(SupportedLocale.FRENCH)
        assertFalse(LocalizationManager.setLocaleByCode("xx"))
        assertEquals(SupportedLocale.ENGLISH, LocalizationManager.locale)
    }

    @Test
    fun `LocalizationManager strings returns correct implementation`() {
        LocalizationManager.setLocale(SupportedLocale.ENGLISH)
        assertEquals("Save", LocalizationManager.strings.save)

        LocalizationManager.setLocale(SupportedLocale.FRENCH)
        assertEquals("Enregistrer", LocalizationManager.strings.save)

        LocalizationManager.setLocale(SupportedLocale.GERMAN)
        assertEquals("Speichern", LocalizationManager.strings.save)

        LocalizationManager.setLocale(SupportedLocale.SPANISH)
        assertEquals("Guardar", LocalizationManager.strings.save)
    }

    @Test
    fun `LocalizationManager getStringsForLocale returns correct implementation`() {
        assertEquals(StringsEn, LocalizationManager.getStringsForLocale(SupportedLocale.ENGLISH))
        assertEquals(StringsFr, LocalizationManager.getStringsForLocale(SupportedLocale.FRENCH))
        assertEquals(StringsDe, LocalizationManager.getStringsForLocale(SupportedLocale.GERMAN))
        assertEquals(StringsEs, LocalizationManager.getStringsForLocale(SupportedLocale.SPANISH))
        assertEquals(StringsIt, LocalizationManager.getStringsForLocale(SupportedLocale.ITALIAN))
        assertEquals(StringsRu, LocalizationManager.getStringsForLocale(SupportedLocale.RUSSIAN))
        assertEquals(StringsBm, LocalizationManager.getStringsForLocale(SupportedLocale.BEMBA))
        assertEquals(StringsNy, LocalizationManager.getStringsForLocale(SupportedLocale.NYANJA))
    }

    @Test
    fun `LocalizationManager getCurrentLocaleCode returns correct code`() {
        LocalizationManager.setLocale(SupportedLocale.FRENCH)
        assertEquals("fr", LocalizationManager.getCurrentLocaleCode())
    }

    @Test
    fun `LocalizationManager getCurrentLocaleDisplayName returns native name`() {
        LocalizationManager.setLocale(SupportedLocale.FRENCH)
        assertEquals("Français", LocalizationManager.getCurrentLocaleDisplayName())
    }

    @Test
    fun `LocalizationManager getAvailableLocales returns all locales`() {
        val locales = LocalizationManager.getAvailableLocales()
        assertEquals(8, locales.size)
        assertTrue(locales.any { it.first == "en" && it.second == "English" })
        assertTrue(locales.any { it.first == "fr" && it.second == "Français" })
    }

    // ===== Format Tests =====

    @Test
    fun `LocalizationManager format replaces single parameter`() {
        val result = LocalizationManager.format("Hello, {name}!", "name" to "World")
        assertEquals("Hello, World!", result)
    }

    @Test
    fun `LocalizationManager format replaces multiple parameters`() {
        val result = LocalizationManager.format(
            "{product}: {available} available, {requested} requested",
            "product" to "Aspirin",
            "available" to 10,
            "requested" to 20
        )
        assertEquals("Aspirin: 10 available, 20 requested", result)
    }

    @Test
    fun `LocalizationManager format leaves unknown parameters unchanged`() {
        val result = LocalizationManager.format("Hello, {name}!", "other" to "World")
        assertEquals("Hello, {name}!", result)
    }

    @Test
    fun `LocalizationManager format with empty params returns original`() {
        val result = LocalizationManager.format("Hello, World!")
        assertEquals("Hello, World!", result)
    }

    // ===== L Shorthand Tests =====

    @Test
    fun `L shorthand provides same strings as LocalizationManager`() {
        LocalizationManager.setLocale(SupportedLocale.FRENCH)
        assertEquals(LocalizationManager.strings.save, L.strings.save)
        assertEquals(LocalizationManager.locale, L.locale)
    }

    @Test
    fun `L shorthand setLocale works correctly`() {
        L.setLocale(SupportedLocale.GERMAN)
        assertEquals(SupportedLocale.GERMAN, L.locale)
        assertEquals("Speichern", L.strings.save)
    }

    @Test
    fun `L shorthand format works correctly`() {
        val result = L.format("Welcome, {name}!", "name" to "Test")
        assertEquals("Welcome, Test!", result)
    }

    // ===== Strings Content Tests =====

    @Test
    fun `All locales have appName as MediStock`() {
        SupportedLocale.entries.forEach { locale ->
            val strings = LocalizationManager.getStringsForLocale(locale)
            assertEquals("MediStock", strings.appName, "appName should be MediStock for ${locale.code}")
        }
    }

    @Test
    fun `All locales have non-empty common strings`() {
        SupportedLocale.entries.forEach { locale ->
            val strings = LocalizationManager.getStringsForLocale(locale)
            assertTrue(strings.ok.isNotBlank(), "ok should not be blank for ${locale.code}")
            assertTrue(strings.cancel.isNotBlank(), "cancel should not be blank for ${locale.code}")
            assertTrue(strings.save.isNotBlank(), "save should not be blank for ${locale.code}")
            assertTrue(strings.delete.isNotBlank(), "delete should not be blank for ${locale.code}")
            assertTrue(strings.edit.isNotBlank(), "edit should not be blank for ${locale.code}")
        }
    }

    @Test
    fun `French strings are correctly translated`() {
        val fr = StringsFr
        assertEquals("Annuler", fr.cancel)
        assertEquals("Enregistrer", fr.save)
        assertEquals("Supprimer", fr.delete)
        assertEquals("Modifier", fr.edit)
        assertEquals("Connexion", fr.loginTitle)
        assertEquals("Produits", fr.products)
    }

    @Test
    fun `German strings are correctly translated`() {
        val de = StringsDe
        assertEquals("Abbrechen", de.cancel)
        assertEquals("Speichern", de.save)
        assertEquals("Löschen", de.delete)
        assertEquals("Bearbeiten", de.edit)
        assertEquals("Anmeldung", de.loginTitle)
        assertEquals("Produkte", de.products)
    }

    @Test
    fun `Spanish strings are correctly translated`() {
        val es = StringsEs
        assertEquals("Cancelar", es.cancel)
        assertEquals("Guardar", es.save)
        assertEquals("Eliminar", es.delete)
        assertEquals("Editar", es.edit)
        assertEquals("Iniciar sesión", es.loginTitle)
        assertEquals("Productos", es.products)
    }

    @Test
    fun `Parameterized strings contain placeholders`() {
        val en = StringsEn
        assertTrue(en.welcomeBack.contains("{name}"), "welcomeBack should contain {name}")
        assertTrue(en.insufficientStock.contains("{product}"), "insufficientStock should contain {product}")
        assertTrue(en.insufficientStock.contains("{available}"), "insufficientStock should contain {available}")
        assertTrue(en.insufficientStock.contains("{requested}"), "insufficientStock should contain {requested}")
        assertTrue(en.entityInUse.contains("{entity}"), "entityInUse should contain {entity}")
        assertTrue(en.entityInUse.contains("{count}"), "entityInUse should contain {count}")
    }
}
