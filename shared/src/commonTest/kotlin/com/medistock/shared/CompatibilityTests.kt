package com.medistock.shared

import com.medistock.shared.domain.compatibility.CompatibilityChecker
import com.medistock.shared.domain.compatibility.CompatibilityResult
import com.medistock.shared.domain.compatibility.SchemaVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for app/database compatibility checking
 */
class CompatibilityCheckerTest {

    @Test
    fun checkCompatibility_withNullSchemaVersion_returnsCompatible() {
        val result = CompatibilityChecker.checkCompatibility(null)

        assertIs<CompatibilityResult.Compatible>(result)
        assertTrue(result.isCompatible)
        assertFalse(result.requiresUpdate)
    }

    @Test
    fun checkCompatibility_whenAppVersionEqualsMinRequired_returnsCompatible() {
        val schemaVersion = SchemaVersion(
            schemaVersion = CompatibilityChecker.MIN_SCHEMA_VERSION,
            minAppVersion = CompatibilityChecker.APP_SCHEMA_VERSION,
            updatedAt = null
        )

        val result = CompatibilityChecker.checkCompatibility(schemaVersion)

        assertIs<CompatibilityResult.Compatible>(result)
        assertTrue(result.isCompatible)
        assertFalse(result.requiresUpdate)
    }

    @Test
    fun checkCompatibility_whenAppVersionGreaterThanMinRequired_returnsCompatible() {
        val schemaVersion = SchemaVersion(
            schemaVersion = CompatibilityChecker.MIN_SCHEMA_VERSION + 5,
            minAppVersion = 1, // Less than APP_SCHEMA_VERSION
            updatedAt = null
        )

        val result = CompatibilityChecker.checkCompatibility(schemaVersion)

        assertIs<CompatibilityResult.Compatible>(result)
        assertTrue(result.isCompatible)
        assertFalse(result.requiresUpdate)
    }

    @Test
    fun checkCompatibility_whenAppVersionLessThanMinRequired_returnsAppTooOld() {
        val schemaVersion = SchemaVersion(
            schemaVersion = 50,
            minAppVersion = CompatibilityChecker.APP_SCHEMA_VERSION + 5, // More than current app version
            updatedAt = 1705680000000L
        )

        val result = CompatibilityChecker.checkCompatibility(schemaVersion)

        assertIs<CompatibilityResult.AppTooOld>(result)
        assertFalse(result.isCompatible)
        assertTrue(result.requiresUpdate)
        assertEquals(CompatibilityChecker.APP_SCHEMA_VERSION, result.appVersion)
        assertEquals(CompatibilityChecker.APP_SCHEMA_VERSION + 5, result.minRequired)
        assertEquals(50, result.dbVersion)
    }

    @Test
    fun checkCompatibility_whenDbSchemaVersionTooLow_returnsDbTooOld() {
        val schemaVersion = SchemaVersion(
            schemaVersion = 4, // Lower than MIN_SCHEMA_VERSION (29)
            minAppVersion = 1,
            updatedAt = null
        )

        val result = CompatibilityChecker.checkCompatibility(schemaVersion)

        assertIs<CompatibilityResult.DbTooOld>(result)
        assertFalse(result.isCompatible)
        assertTrue(result.requiresUpdate)
        assertEquals(4, result.dbSchemaVersion)
        assertEquals(CompatibilityChecker.MIN_SCHEMA_VERSION, result.minRequired)
        assertEquals(CompatibilityChecker.APP_SCHEMA_VERSION, result.appVersion)
    }

    @Test
    fun checkCompatibility_whenDbSchemaVersionEqualsMin_returnsCompatible() {
        val schemaVersion = SchemaVersion(
            schemaVersion = CompatibilityChecker.MIN_SCHEMA_VERSION,
            minAppVersion = CompatibilityChecker.APP_SCHEMA_VERSION,
            updatedAt = null
        )

        val result = CompatibilityChecker.checkCompatibility(schemaVersion)

        assertIs<CompatibilityResult.Compatible>(result)
    }

    @Test
    fun checkCompatibility_whenDbSchemaVersionAboveMin_returnsCompatible() {
        val schemaVersion = SchemaVersion(
            schemaVersion = CompatibilityChecker.MIN_SCHEMA_VERSION + 5,
            minAppVersion = CompatibilityChecker.APP_SCHEMA_VERSION,
            updatedAt = null
        )

        val result = CompatibilityChecker.checkCompatibility(schemaVersion)

        assertIs<CompatibilityResult.Compatible>(result)
    }

    @Test
    fun checkCompatibility_appTooOldTakesPrecedenceOverDbTooOld() {
        // Both conditions met: app too old AND db too old
        val schemaVersion = SchemaVersion(
            schemaVersion = 1, // DB too old
            minAppVersion = CompatibilityChecker.APP_SCHEMA_VERSION + 5, // App too old
            updatedAt = null
        )

        val result = CompatibilityChecker.checkCompatibility(schemaVersion)

        // AppTooOld should be returned first (checked before DbTooOld)
        assertIs<CompatibilityResult.AppTooOld>(result)
    }

    @Test
    fun appSchemaVersion_isPositive() {
        assertTrue(CompatibilityChecker.APP_SCHEMA_VERSION > 0)
    }

    @Test
    fun minSchemaVersion_isPositive() {
        assertTrue(CompatibilityChecker.MIN_SCHEMA_VERSION > 0)
    }
}

class CompatibilityResultTest {

    @Test
    fun compatible_hasCorrectProperties() {
        val result = CompatibilityResult.Compatible

        assertTrue(result.isCompatible)
        assertFalse(result.requiresUpdate)
    }

    @Test
    fun appTooOld_hasCorrectProperties() {
        val result = CompatibilityResult.AppTooOld(
            appVersion = 1,
            minRequired = 3,
            dbVersion = 5
        )

        assertFalse(result.isCompatible)
        assertTrue(result.requiresUpdate)
        assertEquals(1, result.appVersion)
        assertEquals(3, result.minRequired)
        assertEquals(5, result.dbVersion)
    }

    @Test
    fun dbTooOld_hasCorrectProperties() {
        val result = CompatibilityResult.DbTooOld(
            dbSchemaVersion = 4,
            minRequired = 29,
            appVersion = 3
        )

        assertFalse(result.isCompatible)
        assertTrue(result.requiresUpdate)
        assertEquals(4, result.dbSchemaVersion)
        assertEquals(29, result.minRequired)
        assertEquals(3, result.appVersion)
    }

    @Test
    fun unknown_hasCorrectProperties() {
        val result = CompatibilityResult.Unknown("Network error")

        assertFalse(result.isCompatible)
        assertFalse(result.requiresUpdate)
        assertEquals("Network error", result.reason)
    }
}

class SchemaVersionTest {

    @Test
    fun schemaVersion_createsCorrectly() {
        val schemaVersion = SchemaVersion(
            schemaVersion = 5,
            minAppVersion = 2,
            updatedAt = 1705680000000L
        )

        assertEquals(5, schemaVersion.schemaVersion)
        assertEquals(2, schemaVersion.minAppVersion)
        assertEquals(1705680000000L, schemaVersion.updatedAt)
    }

    @Test
    fun schemaVersion_withNullUpdatedAt_createsCorrectly() {
        val schemaVersion = SchemaVersion(
            schemaVersion = 3,
            minAppVersion = 1,
            updatedAt = null
        )

        assertEquals(3, schemaVersion.schemaVersion)
        assertEquals(1, schemaVersion.minAppVersion)
        assertEquals(null, schemaVersion.updatedAt)
    }
}

class FormatCompatibilityInfoTest {

    @Test
    fun formatCompatibilityInfo_forCompatible_containsVersion() {
        val result = CompatibilityResult.Compatible
        val info = CompatibilityChecker.formatCompatibilityInfo(result)

        assertTrue(info.contains("compatible", ignoreCase = true))
        assertTrue(info.contains(CompatibilityChecker.APP_SCHEMA_VERSION.toString()))
    }

    @Test
    fun formatCompatibilityInfo_forAppTooOld_containsAllVersions() {
        val result = CompatibilityResult.AppTooOld(
            appVersion = 1,
            minRequired = 3,
            dbVersion = 50
        )
        val info = CompatibilityChecker.formatCompatibilityInfo(result)

        assertTrue(info.contains("App too old", ignoreCase = true))
        assertTrue(info.contains("1"))
        assertTrue(info.contains("3"))
        assertTrue(info.contains("50"))
    }

    @Test
    fun formatCompatibilityInfo_forDbTooOld_containsAllVersions() {
        val result = CompatibilityResult.DbTooOld(
            dbSchemaVersion = 4,
            minRequired = 29,
            appVersion = 3
        )
        val info = CompatibilityChecker.formatCompatibilityInfo(result)

        assertTrue(info.contains("Database too old", ignoreCase = true))
        assertTrue(info.contains("4"))
        assertTrue(info.contains("29"))
    }

    @Test
    fun formatCompatibilityInfo_forUnknown_containsReason() {
        val result = CompatibilityResult.Unknown("Connection timeout")
        val info = CompatibilityChecker.formatCompatibilityInfo(result)

        assertTrue(info.contains("Connection timeout"))
    }
}
