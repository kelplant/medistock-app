@file:OptIn(kotlinx.serialization.InternalSerializationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)

package com.medistock.data.remote.repository

import com.medistock.data.remote.SupabaseClientProvider
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.call.body
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * DTO pour les migrations stockées dans schema_migrations
 */
@Serializable
data class SchemaMigrationDto(
    val name: String,
    val checksum: String? = null,
    @SerialName("applied_at")
    val appliedAt: Long? = null,
    @SerialName("applied_by")
    val appliedBy: String? = null,
    val success: Boolean = true,
    @SerialName("execution_time_ms")
    val executionTimeMs: Int? = null,
    @SerialName("error_message")
    val errorMessage: String? = null
)

/**
 * DTO pour la version du schéma
 */
@Serializable
data class SchemaVersionDto(
    @SerialName("schema_version")
    val schemaVersion: Int,
    @SerialName("min_app_version")
    val minAppVersion: Int,
    @SerialName("updated_at")
    val updatedAt: Long? = null
)

/**
 * Résultat de l'application d'une migration
 */
data class MigrationResult(
    val success: Boolean,
    val alreadyApplied: Boolean,
    val message: String,
    val executionTimeMs: Int? = null,
    val error: String? = null
)

/**
 * Paramètres pour apply_migration RPC
 */
@Serializable
data class ApplyMigrationParams(
    @SerialName("p_name")
    val pName: String,
    @SerialName("p_sql")
    val pSql: String,
    @SerialName("p_checksum")
    val pChecksum: String? = null,
    @SerialName("p_applied_by")
    val pAppliedBy: String = "app"
)

/**
 * Paramètres pour is_migration_applied RPC
 */
@Serializable
data class IsMigrationAppliedParams(
    @SerialName("p_name")
    val pName: String
)

/**
 * Paramètres pour update_schema_version RPC
 */
@Serializable
data class UpdateSchemaVersionParams(
    @SerialName("p_schema_version")
    val pSchemaVersion: Int,
    @SerialName("p_min_app_version")
    val pMinAppVersion: Int? = null,
    @SerialName("p_updated_by")
    val pUpdatedBy: String = "app"
)

/**
 * Request body for apply-migration Edge Function
 */
@Serializable
data class ApplyMigrationEdgeFunctionRequest(
    val name: String,
    val sql: String,
    val checksum: String
)

/**
 * Response from apply-migration Edge Function
 */
@Serializable
data class ApplyMigrationEdgeFunctionResponse(
    val success: Boolean,
    val alreadyApplied: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val executionTimeMs: Int? = null
)

/**
 * Repository pour gérer les migrations de schéma Supabase
 *
 * Uses the apply-migration Edge Function for secure migration execution.
 * The Edge Function verifies migrations against GitHub before executing.
 *
 * Utilise les fonctions RPC:
 * - get_applied_migrations()
 * - is_migration_applied(name)
 */
class MigrationRepository {

    private val supabase get() = SupabaseClientProvider.client
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Récupère la liste des migrations déjà appliquées
     */
    suspend fun getAppliedMigrations(): List<SchemaMigrationDto> {
        return try {
            supabase.postgrest.rpc("get_applied_migrations")
                .decodeList<SchemaMigrationDto>()
        } catch (e: Exception) {
            println("❌ Erreur lors de la récupération des migrations: ${e.message}")
            emptyList()
        }
    }

    /**
     * Vérifie si une migration spécifique a déjà été appliquée
     */
    suspend fun isMigrationApplied(name: String): Boolean {
        return try {
            supabase.postgrest.rpc(
                "is_migration_applied",
                IsMigrationAppliedParams(pName = name)
            ).decodeSingle<Boolean>()
        } catch (e: Exception) {
            println("❌ Erreur lors de la vérification de la migration $name: ${e.message}")
            // En cas d'erreur, on assume que la migration n'existe pas
            // (probablement le système de migration n'est pas encore installé)
            false
        }
    }

    /**
     * Applique une migration via l'Edge Function apply-migration.
     *
     * The Edge Function:
     * 1. Requires authentication (any authenticated user)
     * 2. Verifies migration checksum against GitHub
     * 3. Executes the SQL using service_role
     * 4. Records the migration in schema_migrations
     *
     * @param name Nom unique de la migration (ex: "2026011702_add_feature")
     * @param sql Le SQL à exécuter
     * @param checksum Hash MD5 du SQL pour vérification d'intégrité
     * @param appliedBy Identifiant de l'utilisateur/système qui applique la migration (unused with Edge Function)
     * @param accessToken The Supabase Auth access token for authentication
     */
    suspend fun applyMigration(
        name: String,
        sql: String,
        checksum: String? = null,
        appliedBy: String = "app",
        accessToken: String? = null
    ): MigrationResult {
        // If we have an access token, use the Edge Function
        if (accessToken != null && checksum != null) {
            return applyMigrationViaEdgeFunction(name, sql, checksum, accessToken)
        }

        // Fallback to direct RPC (for legacy support or when no auth)
        return applyMigrationViaRpc(name, sql, checksum, appliedBy)
    }

    /**
     * Apply migration via the apply-migration Edge Function (preferred method)
     */
    private suspend fun applyMigrationViaEdgeFunction(
        name: String,
        sql: String,
        checksum: String,
        accessToken: String
    ): MigrationResult {
        return try {
            val requestBody = json.encodeToString(
                ApplyMigrationEdgeFunctionRequest.serializer(),
                ApplyMigrationEdgeFunctionRequest(name, sql, checksum)
            )

            val response = supabase.functions.invoke(
                function = "apply-migration",
                body = requestBody,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, "application/json")
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            )

            val responseBody = response.body<String>()
            val result = json.decodeFromString<ApplyMigrationEdgeFunctionResponse>(responseBody)

            MigrationResult(
                success = result.success,
                alreadyApplied = result.alreadyApplied,
                message = result.message ?: if (result.success) "Migration applied" else "Migration failed",
                executionTimeMs = result.executionTimeMs,
                error = result.error
            )
        } catch (e: Exception) {
            println("Edge Function apply-migration failed: ${e.message}")
            MigrationResult(
                success = false,
                alreadyApplied = false,
                message = "Edge Function failed: ${e.message}",
                error = e.message
            )
        }
    }

    /**
     * Apply migration via direct RPC (fallback for legacy/offline)
     */
    private suspend fun applyMigrationViaRpc(
        name: String,
        sql: String,
        checksum: String?,
        appliedBy: String
    ): MigrationResult {
        return try {
            val params = ApplyMigrationParams(
                pName = name,
                pSql = sql,
                pChecksum = checksum,
                pAppliedBy = appliedBy
            )

            val response = supabase.postgrest.rpc(
                "apply_migration",
                params
            )

            // Parse the raw JSON response (JSONB functions return object directly, not array)
            val jsonString = response.data
            val result = Json.decodeFromString<JsonObject>(jsonString)

            MigrationResult(
                success = result["success"]?.jsonPrimitive?.booleanOrNull ?: false,
                alreadyApplied = result["already_applied"]?.jsonPrimitive?.booleanOrNull ?: false,
                message = result["message"]?.jsonPrimitive?.content ?: "Unknown result",
                executionTimeMs = result["execution_time_ms"]?.jsonPrimitive?.intOrNull,
                error = result["error"]?.jsonPrimitive?.content
            )
        } catch (e: Exception) {
            println("RPC apply_migration failed: ${e.message}")
            MigrationResult(
                success = false,
                alreadyApplied = false,
                message = "Failed to apply migration: ${e.message}",
                error = e.message
            )
        }
    }

    /**
     * Vérifie si le système de migration est installé dans Supabase
     * (table schema_migrations et fonctions RPC existent)
     */
    suspend fun isMigrationSystemInstalled(): Boolean {
        return try {
            // Tenter d'appeler get_applied_migrations
            // Si ça fonctionne, le système est installé
            supabase.postgrest.rpc("get_applied_migrations")
                .decodeList<SchemaMigrationDto>()
            true
        } catch (e: Exception) {
            println("⚠️ Système de migration non installé: ${e.message}")
            false
        }
    }

    /**
     * Récupère la version actuelle du schéma de la base de données
     *
     * @return SchemaVersionDto ou null si non disponible (système non installé ou erreur)
     */
    suspend fun getSchemaVersion(): SchemaVersionDto? {
        return try {
            supabase.postgrest.rpc("get_schema_version")
                .decodeSingleOrNull<SchemaVersionDto>()
        } catch (e: Exception) {
            println("⚠️ Impossible de récupérer la version du schéma: ${e.message}")
            null
        }
    }

    /**
     * Met à jour la version du schéma (appelé après une migration importante)
     */
    suspend fun updateSchemaVersion(
        schemaVersion: Int,
        minAppVersion: Int? = null,
        updatedBy: String = "app"
    ): Boolean {
        return try {
            val params = UpdateSchemaVersionParams(
                pSchemaVersion = schemaVersion,
                pMinAppVersion = minAppVersion,
                pUpdatedBy = updatedBy
            )

            val response = supabase.postgrest.rpc(
                "update_schema_version",
                params
            )

            // Parse the raw JSON response (JSONB functions return object directly, not array)
            val jsonString = response.data
            val result = Json.decodeFromString<JsonObject>(jsonString)

            result["success"]?.jsonPrimitive?.booleanOrNull ?: false
        } catch (e: Exception) {
            println("❌ Erreur lors de la mise à jour de la version: ${e.message}")
            false
        }
    }
}
