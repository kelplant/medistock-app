@file:OptIn(kotlinx.serialization.InternalSerializationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)

package com.medistock.data.remote.repository

import com.medistock.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    val appliedAt: String? = null,
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
    val updatedAt: String? = null
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
 * Repository pour gérer les migrations de schéma Supabase
 *
 * Utilise les fonctions RPC:
 * - apply_migration(name, sql, checksum, applied_by)
 * - get_applied_migrations()
 * - is_migration_applied(name)
 */
class MigrationRepository {

    private val supabase get() = SupabaseClientProvider.client

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
     * Applique une migration
     *
     * @param name Nom unique de la migration (ex: "2026011702_add_feature")
     * @param sql Le SQL à exécuter
     * @param checksum Hash MD5 du SQL pour vérification d'intégrité (optionnel)
     * @param appliedBy Identifiant de l'utilisateur/système qui applique la migration
     */
    suspend fun applyMigration(
        name: String,
        sql: String,
        checksum: String? = null,
        appliedBy: String = "app"
    ): MigrationResult {
        return try {
            val params = ApplyMigrationParams(
                pName = name,
                pSql = sql,
                pChecksum = checksum,
                pAppliedBy = appliedBy
            )

            val result = supabase.postgrest.rpc(
                "apply_migration",
                params
            ).decodeSingle<JsonObject>()

            MigrationResult(
                success = result["success"]?.jsonPrimitive?.booleanOrNull ?: false,
                alreadyApplied = result["already_applied"]?.jsonPrimitive?.booleanOrNull ?: false,
                message = result["message"]?.jsonPrimitive?.content ?: "Unknown result",
                executionTimeMs = result["execution_time_ms"]?.jsonPrimitive?.intOrNull,
                error = result["error"]?.jsonPrimitive?.content
            )
        } catch (e: Exception) {
            println("❌ Erreur lors de l'application de la migration $name: ${e.message}")
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

            val result = supabase.postgrest.rpc(
                "update_schema_version",
                params
            ).decodeSingle<JsonObject>()

            result["success"]?.jsonPrimitive?.booleanOrNull ?: false
        } catch (e: Exception) {
            println("❌ Erreur lors de la mise à jour de la version: ${e.message}")
            false
        }
    }
}
