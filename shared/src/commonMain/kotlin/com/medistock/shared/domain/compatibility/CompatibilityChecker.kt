package com.medistock.shared.domain.compatibility

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Schema version information from the database.
 */
@Serializable
data class SchemaVersion(
    @SerialName("schema_version")
    val schemaVersion: Int,
    @SerialName("min_app_version")
    val minAppVersion: Int,
    @SerialName("updated_at")
    val updatedAt: Long? = null
)

/**
 * Result of app/database compatibility check.
 */
sealed class CompatibilityResult {
    /**
     * App is compatible with the database.
     */
    data object Compatible : CompatibilityResult()

    /**
     * App is too old for this database - update required.
     * @param appVersion Current app schema version
     * @param minRequired Minimum required app schema version
     * @param dbVersion Current database schema version
     */
    data class AppTooOld(
        val appVersion: Int,
        val minRequired: Int,
        val dbVersion: Int
    ) : CompatibilityResult()

    /**
     * Cannot verify compatibility (system not installed or network error).
     * @param reason Description of why verification failed
     */
    data class Unknown(val reason: String) : CompatibilityResult()

    /**
     * Returns true if the app is compatible.
     */
    val isCompatible: Boolean
        get() = this is Compatible

    /**
     * Returns true if the app is too old and needs update.
     */
    val requiresUpdate: Boolean
        get() = this is AppTooOld
}

/**
 * Checks app/database compatibility.
 * Platform-specific code is responsible for fetching the SchemaVersion from Supabase.
 */
object CompatibilityChecker {
    /**
     * Current app schema version.
     *
     * IMPORTANT: Increment this value when you add a migration that
     * modifies the schema in a way that is incompatible with older app versions.
     *
     * History:
     * - Version 1: Initial schema
     * - Version 2: Migration system and versioning
     */
    const val APP_SCHEMA_VERSION = 2

    /**
     * Checks if the current app version is compatible with the database.
     *
     * @param schemaVersion The schema version from the database, or null if not available
     * @return CompatibilityResult indicating if the app can be used
     */
    fun checkCompatibility(schemaVersion: SchemaVersion?): CompatibilityResult {
        if (schemaVersion == null) {
            // Versioning system not installed - assume compatible (old DB without versioning)
            return CompatibilityResult.Compatible
        }

        val dbVersion = schemaVersion.schemaVersion
        val minAppVersion = schemaVersion.minAppVersion

        return if (APP_SCHEMA_VERSION < minAppVersion) {
            CompatibilityResult.AppTooOld(
                appVersion = APP_SCHEMA_VERSION,
                minRequired = minAppVersion,
                dbVersion = dbVersion
            )
        } else {
            CompatibilityResult.Compatible
        }
    }

    /**
     * Formats compatibility info for display.
     */
    fun formatCompatibilityInfo(result: CompatibilityResult): String {
        return when (result) {
            is CompatibilityResult.Compatible ->
                "App compatible (version $APP_SCHEMA_VERSION)"
            is CompatibilityResult.AppTooOld ->
                "App too old: version ${result.appVersion}, minimum required: ${result.minRequired}, database version: ${result.dbVersion}"
            is CompatibilityResult.Unknown ->
                "Cannot verify compatibility: ${result.reason}"
        }
    }
}
