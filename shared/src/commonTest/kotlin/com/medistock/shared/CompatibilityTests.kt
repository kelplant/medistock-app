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
            schemaVersion = 5,
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
            schemaVersion = 3,
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
            schemaVersion = 10,
            minAppVersion = CompatibilityChecker.APP_SCHEMA_VERSION + 5, // More than current app version
            updatedAt = 1705680000000L
        )

        val result = CompatibilityChecker.checkCompatibility(schemaVersion)

        assertIs<CompatibilityResult.AppTooOld>(result)
        assertFalse(result.isCompatible)
        assertTrue(result.requiresUpdate)
        assertEquals(CompatibilityChecker.APP_SCHEMA_VERSION, result.appVersion)
        assertEquals(CompatibilityChecker.APP_SCHEMA_VERSION + 5, result.minRequired)
        assertEquals(10, result.dbVersion)
    }

    @Test
    fun appSchemaVersion_isPositive() {
        assertTrue(CompatibilityChecker.APP_SCHEMA_VERSION > 0)
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
            dbVersion = 5
        )
        val info = CompatibilityChecker.formatCompatibilityInfo(result)

        assertTrue(info.contains("too old", ignoreCase = true))
        assertTrue(info.contains("1"))
        assertTrue(info.contains("3"))
        assertTrue(info.contains("5"))
    }

    @Test
    fun formatCompatibilityInfo_forUnknown_containsReason() {
        val result = CompatibilityResult.Unknown("Connection timeout")
        val info = CompatibilityChecker.formatCompatibilityInfo(result)

        assertTrue(info.contains("Connection timeout"))
    }
}
