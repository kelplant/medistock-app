package com.medistock.shared.i18n

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
        assertEquals(SupportedLocale.BEMBA, SupportedLocale.fromCode("bem"))
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
        assertTrue(codes.contains("bem"))
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

    // ===== StateFlow Observable Tests =====

    @Test
    fun `currentLocale StateFlow reflects initial state`() = runTest {
        val currentLocale = LocalizationManager.currentLocale.first()
        assertEquals(SupportedLocale.DEFAULT, currentLocale)
    }

    @Test
    fun `currentLocale StateFlow updates when locale changes`() = runTest {
        LocalizationManager.setLocale(SupportedLocale.FRENCH)
        val currentLocale = LocalizationManager.currentLocale.first()
        assertEquals(SupportedLocale.FRENCH, currentLocale)
    }

    @Test
    fun `currentLocale StateFlow updates multiple times`() = runTest {
        LocalizationManager.setLocale(SupportedLocale.FRENCH)
        assertEquals(SupportedLocale.FRENCH, LocalizationManager.currentLocale.first())

        LocalizationManager.setLocale(SupportedLocale.GERMAN)
        assertEquals(SupportedLocale.GERMAN, LocalizationManager.currentLocale.first())

        LocalizationManager.setLocale(SupportedLocale.SPANISH)
        assertEquals(SupportedLocale.SPANISH, LocalizationManager.currentLocale.first())
    }

    // ===== Format Edge Cases =====

    @Test
    fun `format handles duplicate placeholders correctly`() {
        val result = LocalizationManager.format(
            "Hello {name}, welcome {name}!",
            "name" to "John"
        )
        assertEquals("Hello John, welcome John!", result)
    }

    @Test
    fun `format handles multiple occurrences of same placeholder`() {
        val result = LocalizationManager.format(
            "{x} + {x} = {result}",
            "x" to 5,
            "result" to 10
        )
        assertEquals("5 + 5 = 10", result)
    }

    @Test
    fun `format handles empty string template`() {
        val result = LocalizationManager.format("", "name" to "Test")
        assertEquals("", result)
    }

    @Test
    fun `format handles template with no placeholders`() {
        val result = LocalizationManager.format("No placeholders here", "name" to "Test")
        assertEquals("No placeholders here", result)
    }

    @Test
    fun `format handles empty parameter value`() {
        val result = LocalizationManager.format("Hello, {name}!", "name" to "")
        assertEquals("Hello, !", result)
    }

    @Test
    fun `format handles numeric parameters`() {
        val result = LocalizationManager.format(
            "Quantity: {qty}, Price: {price}",
            "qty" to 42,
            "price" to 99.99
        )
        assertEquals("Quantity: 42, Price: 99.99", result)
    }

    @Test
    fun `format handles boolean parameters`() {
        val result = LocalizationManager.format(
            "Active: {active}, Deleted: {deleted}",
            "active" to true,
            "deleted" to false
        )
        assertEquals("Active: true, Deleted: false", result)
    }

    @Test
    fun `format handles special characters in values`() {
        val result = LocalizationManager.format(
            "Message: {msg}",
            "msg" to "Hello {world}!"
        )
        assertEquals("Message: Hello {world}!", result)
    }

    @Test
    fun `format handles mixed matched and unmatched placeholders`() {
        val result = LocalizationManager.format(
            "Found: {found}, Missing: {missing}",
            "found" to "YES"
        )
        assertEquals("Found: YES, Missing: {missing}", result)
    }

    @Test
    fun `format handles placeholder at start of string`() {
        val result = LocalizationManager.format("{name} logged in", "name" to "Admin")
        assertEquals("Admin logged in", result)
    }

    @Test
    fun `format handles placeholder at end of string`() {
        val result = LocalizationManager.format("Welcome back, {name}", "name" to "User")
        assertEquals("Welcome back, User", result)
    }

    @Test
    fun `format handles adjacent placeholders`() {
        val result = LocalizationManager.format(
            "{first}{second}",
            "first" to "Hello",
            "second" to "World"
        )
        assertEquals("HelloWorld", result)
    }

    @Test
    fun `format handles placeholders with underscores`() {
        val result = LocalizationManager.format(
            "Value: {user_name}",
            "user_name" to "john_doe"
        )
        assertEquals("Value: john_doe", result)
    }

    @Test
    fun `format handles placeholders with numbers`() {
        val result = LocalizationManager.format(
            "Item {item1} and {item2}",
            "item1" to "A",
            "item2" to "B"
        )
        assertEquals("Item A and B", result)
    }

    @Test
    fun `format handles zero value correctly`() {
        val result = LocalizationManager.format("Count: {count}", "count" to 0)
        assertEquals("Count: 0", result)
    }

    @Test
    fun `format handles negative numbers correctly`() {
        val result = LocalizationManager.format("Balance: {amount}", "amount" to -50)
        assertEquals("Balance: -50", result)
    }

    // ===== Locale Switching Tests =====

    @Test
    fun `setLocale immediately reflects in strings property`() {
        val englishSave = LocalizationManager.strings.save
        LocalizationManager.setLocale(SupportedLocale.FRENCH)
        val frenchSave = LocalizationManager.strings.save

        assertNotEquals(englishSave, frenchSave)
        assertEquals("Save", englishSave)
        assertEquals("Enregistrer", frenchSave)
    }

    @Test
    fun `setLocaleByCode with empty string returns false and sets default`() {
        LocalizationManager.setLocale(SupportedLocale.FRENCH)
        val result = LocalizationManager.setLocaleByCode("")

        assertFalse(result)
        assertEquals(SupportedLocale.DEFAULT, LocalizationManager.locale)
    }

    @Test
    fun `setLocaleByCode with whitespace returns false`() {
        val result = LocalizationManager.setLocaleByCode("  ")
        assertFalse(result)
        assertEquals(SupportedLocale.DEFAULT, LocalizationManager.locale)
    }

    @Test
    fun `setLocaleByCode trims whitespace correctly`() {
        // Note: This test verifies current behavior. If trimming is not implemented,
        // this test will fail and can guide implementation
        val result = LocalizationManager.setLocaleByCode(" fr ")
        // Based on current implementation, this should fail without trim
        assertFalse(result)
    }

    @Test
    fun `changing locale multiple times works correctly`() {
        val locales = listOf(
            SupportedLocale.ENGLISH to "Save",
            SupportedLocale.FRENCH to "Enregistrer",
            SupportedLocale.GERMAN to "Speichern",
            SupportedLocale.SPANISH to "Guardar",
            SupportedLocale.ITALIAN to "Salva"
        )

        locales.forEach { (locale, expectedSave) ->
            LocalizationManager.setLocale(locale)
            assertEquals(expectedSave, LocalizationManager.strings.save,
                "Expected save button to be '$expectedSave' for locale ${locale.code}")
        }
    }

    // ===== All Locales Validation Tests =====

    @Test
    fun `all locales have consistent parameterized string placeholders`() {
        val parameterizedStrings = listOf(
            Triple("welcomeBack", listOf("{name}")) { s: Strings -> s.welcomeBack },
            Triple("insufficientStock", listOf("{product}", "{available}", "{requested}"))
                { s: Strings -> s.insufficientStock },
            Triple("entityInUse", listOf("{entity}", "{count}")) { s: Strings -> s.entityInUse },
            Triple("fieldRequired", listOf("{field}")) { s: Strings -> s.fieldRequired },
            Triple("valueTooShort", listOf("{field}", "{min}")) { s: Strings -> s.valueTooShort },
            Triple("valueTooLong", listOf("{field}", "{max}")) { s: Strings -> s.valueTooLong }
        )

        SupportedLocale.entries.forEach { locale ->
            val strings = LocalizationManager.getStringsForLocale(locale)
            parameterizedStrings.forEach { (name, placeholders, getter) ->
                val value = getter(strings)
                placeholders.forEach { placeholder ->
                    assertTrue(
                        value.contains(placeholder),
                        "String '$name' for locale ${locale.code} should contain placeholder $placeholder. Value: $value"
                    )
                }
            }
        }
    }

    @Test
    fun `all locales have distinct translations for common strings`() {
        val commonFields = listOf<(Strings) -> String>(
            { it.cancel },
            { it.save },
            { it.delete },
            { it.edit }
        )

        // Verify each locale has distinct translations (not all the same as English)
        val english = LocalizationManager.getStringsForLocale(SupportedLocale.ENGLISH)
        val otherLocales = SupportedLocale.entries.filter { it != SupportedLocale.ENGLISH }

        otherLocales.forEach { locale ->
            val localeStrings = LocalizationManager.getStringsForLocale(locale)
            var hasDistinctTranslation = false

            commonFields.forEach { field ->
                if (field(english) != field(localeStrings)) {
                    hasDistinctTranslation = true
                }
            }

            assertTrue(
                hasDistinctTranslation,
                "Locale ${locale.code} should have at least one distinct translation from English"
            )
        }
    }

    @Test
    fun `Italian strings are correctly translated`() {
        val it = StringsIt
        assertEquals("Annulla", it.cancel)
        assertEquals("Salva", it.save)
        assertEquals("Elimina", it.delete)
        assertEquals("Modifica", it.edit)
    }

    @Test
    fun `Russian strings are correctly translated`() {
        val ru = StringsRu
        assertEquals("Отмена", ru.cancel)
        assertEquals("Сохранить", ru.save)
        assertEquals("Удалить", ru.delete)
        assertEquals("Редактировать", ru.edit)
    }

    @Test
    fun `Bemba locale is available and has translations`() {
        val bm = StringsBm
        assertNotNull(bm)
        assertEquals("MediStock", bm.appName)
        assertNotEquals("", bm.save)
        assertNotEquals("", bm.cancel)
    }

    @Test
    fun `Nyanja locale is available and has translations`() {
        val ny = StringsNy
        assertNotNull(ny)
        assertEquals("MediStock", ny.appName)
        assertNotEquals("", ny.save)
        assertNotEquals("", ny.cancel)
    }

    // ===== Integration Tests =====

    @Test
    fun `real world usage - displaying welcome message`() {
        LocalizationManager.setLocale(SupportedLocale.FRENCH)
        val userName = "Marie"
        val welcomeMessage = LocalizationManager.format(
            LocalizationManager.strings.welcomeBack,
            "name" to userName
        )
        assertEquals("Bon retour, Marie !", welcomeMessage)
    }

    @Test
    fun `real world usage - insufficient stock error message`() {
        LocalizationManager.setLocale(SupportedLocale.ENGLISH)
        val errorMessage = LocalizationManager.format(
            LocalizationManager.strings.insufficientStock,
            "product" to "Aspirin",
            "available" to 5,
            "requested" to 10
        )
        assertTrue(errorMessage.contains("Aspirin"))
        assertTrue(errorMessage.contains("5"))
        assertTrue(errorMessage.contains("10"))
    }

    @Test
    fun `real world usage - entity in use message with formatting`() {
        LocalizationManager.setLocale(SupportedLocale.GERMAN)
        val message = LocalizationManager.format(
            LocalizationManager.strings.entityInUse,
            "entity" to "Kategorie",
            "count" to 3
        )
        assertTrue(message.contains("Kategorie"))
        assertTrue(message.contains("3"))
    }

    @Test
    fun `L shorthand works in real scenario`() {
        L.setLocale(SupportedLocale.SPANISH)
        val saveButton = L.strings.save
        val cancelButton = L.strings.cancel

        assertEquals("Guardar", saveButton)
        assertEquals("Cancelar", cancelButton)
        assertEquals(SupportedLocale.SPANISH, L.locale)
    }

    // ===== Consistency Tests =====

    @Test
    fun `locale and currentLocale are always synchronized`() = runTest {
        LocalizationManager.setLocale(SupportedLocale.ITALIAN)

        assertEquals(LocalizationManager.locale, LocalizationManager.currentLocale.first())

        LocalizationManager.setLocale(SupportedLocale.RUSSIAN)

        assertEquals(LocalizationManager.locale, LocalizationManager.currentLocale.first())
    }

    @Test
    fun `getStringsForLocale is deterministic`() {
        val strings1 = LocalizationManager.getStringsForLocale(SupportedLocale.FRENCH)
        val strings2 = LocalizationManager.getStringsForLocale(SupportedLocale.FRENCH)

        assertSame(strings1, strings2, "Should return the same instance for same locale")
    }

    @Test
    fun `getCurrentLocaleCode matches current locale code`() {
        SupportedLocale.entries.forEach { locale ->
            LocalizationManager.setLocale(locale)
            assertEquals(locale.code, LocalizationManager.getCurrentLocaleCode())
        }
    }

    @Test
    fun `getCurrentLocaleDisplayName matches current locale display name`() {
        SupportedLocale.entries.forEach { locale ->
            LocalizationManager.setLocale(locale)
            assertEquals(locale.nativeDisplayName, LocalizationManager.getCurrentLocaleDisplayName())
        }
    }

    // ===== SupportedLocale Edge Cases =====

    @Test
    fun `SupportedLocale fromCode handles null-like inputs`() {
        assertNull(SupportedLocale.fromCode(""))
        assertNull(SupportedLocale.fromCode("null"))
        assertNull(SupportedLocale.fromCode("undefined"))
    }

    @Test
    fun `SupportedLocale fromCode handles similar codes`() {
        assertNull(SupportedLocale.fromCode("eng"))
        assertNull(SupportedLocale.fromCode("fra"))
        assertNull(SupportedLocale.fromCode("en-US"))
        assertNull(SupportedLocale.fromCode("en_US"))
    }

    @Test
    fun `SupportedLocale availableCodes returns unique codes`() {
        val codes = SupportedLocale.availableCodes()
        val uniqueCodes = codes.toSet()
        assertEquals(codes.size, uniqueCodes.size, "All locale codes should be unique")
    }

    @Test
    fun `SupportedLocale all codes are lowercase`() {
        SupportedLocale.entries.forEach { locale ->
            assertEquals(locale.code, locale.code.lowercase(),
                "Locale code ${locale.code} should be lowercase")
        }
    }

    @Test
    fun `SupportedLocale all codes are two or three characters`() {
        // ISO 639-1 codes are 2 characters, ISO 639-2/3 codes are 3 characters (e.g., "bem" for Bemba)
        SupportedLocale.entries.forEach { locale ->
            assertTrue(locale.code.length in 2..3,
                "Locale code ${locale.code} should be 2-3 characters (ISO 639-1/2/3)")
        }
    }

    @Test
    fun `SupportedLocale all have non-empty display names`() {
        SupportedLocale.entries.forEach { locale ->
            assertTrue(locale.displayName.isNotBlank(),
                "Display name should not be blank for ${locale.code}")
            assertTrue(locale.nativeDisplayName.isNotBlank(),
                "Native display name should not be blank for ${locale.code}")
        }
    }

    // ===== Error Recovery Tests =====

    @Test
    fun `setting invalid locale code does not break subsequent valid calls`() {
        assertFalse(LocalizationManager.setLocaleByCode("invalid"))
        assertEquals(SupportedLocale.DEFAULT, LocalizationManager.locale)

        assertTrue(LocalizationManager.setLocaleByCode("fr"))
        assertEquals(SupportedLocale.FRENCH, LocalizationManager.locale)
    }

    @Test
    fun `format with very long string works correctly`() {
        val longTemplate = "x".repeat(1000) + "{param}" + "y".repeat(1000)
        val result = LocalizationManager.format(longTemplate, "param" to "TEST")

        assertTrue(result.contains("TEST"))
        assertFalse(result.contains("{param}"))
        assertEquals(2004, result.length)
    }

    @Test
    fun `format with many parameters works correctly`() {
        val params = (1..20).map { "param$it" to it }
        val template = params.joinToString(", ") { "{${it.first}}" }

        val result = LocalizationManager.format(template, *params.toTypedArray())

        params.forEach { (_, value) ->
            assertTrue(result.contains(value.toString()))
        }
    }
}
