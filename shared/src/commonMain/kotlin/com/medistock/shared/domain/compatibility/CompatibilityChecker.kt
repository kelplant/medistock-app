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
     * Database is too old for this app - migrations need to be applied.
     * @param dbSchemaVersion Current database schema version
     * @param minRequired Minimum schema version required by this app
     * @param appVersion Current app schema version
     */
    data class DbTooOld(
        val dbSchemaVersion: Int,
        val minRequired: Int,
        val appVersion: Int
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
        get() = this is AppTooOld || this is DbTooOld
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
     * - Version 3: Remove product unit, add packaging types, suppliers, app_config
     */
    const val APP_SCHEMA_VERSION = 3

    /**
     * Minimum Supabase schema_version required by this app.
     *
     * IMPORTANT: Increment this value when you add a Supabase migration
     * that the app depends on. This ensures the app blocks usage if the
     * database hasn't been migrated yet.
     *
     * This is the reverse check: the DB checks if the app is new enough
     * (via min_app_version), and the app checks if the DB is new enough
     * (via this constant).
     */
    const val MIN_SCHEMA_VERSION = 29

    /**
     * Checks if the current app version is compatible with the database.
     * Performs a bidirectional check:
     * 1. Is the app new enough for this database? (DB.min_app_version <= APP_SCHEMA_VERSION)
     * 2. Is the database new enough for this app? (DB.schema_version >= MIN_SCHEMA_VERSION)
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

        // Check 1: Is the app too old for this database?
        if (APP_SCHEMA_VERSION < minAppVersion) {
            return CompatibilityResult.AppTooOld(
                appVersion = APP_SCHEMA_VERSION,
                minRequired = minAppVersion,
                dbVersion = dbVersion
            )
        }

        // Check 2: Is the database too old for this app?
        if (dbVersion < MIN_SCHEMA_VERSION) {
            return CompatibilityResult.DbTooOld(
                dbSchemaVersion = dbVersion,
                minRequired = MIN_SCHEMA_VERSION,
                appVersion = APP_SCHEMA_VERSION
            )
        }

        return CompatibilityResult.Compatible
    }

    /**
     * Formats compatibility info for display.
     */
    fun formatCompatibilityInfo(result: CompatibilityResult): String {
        return when (result) {
            is CompatibilityResult.Compatible ->
                "App compatible (version $APP_SCHEMA_VERSION, min schema $MIN_SCHEMA_VERSION)"
            is CompatibilityResult.AppTooOld ->
                "App too old: version ${result.appVersion}, minimum required: ${result.minRequired}, database version: ${result.dbVersion}"
            is CompatibilityResult.DbTooOld ->
                "Database too old: schema version ${result.dbSchemaVersion}, minimum required: ${result.minRequired}, app version: ${result.appVersion}"
            is CompatibilityResult.Unknown ->
                "Cannot verify compatibility: ${result.reason}"
        }
    }
}
