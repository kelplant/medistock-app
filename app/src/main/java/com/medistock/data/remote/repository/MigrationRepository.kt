package com.medistock.data.remote.repository

import com.medistock.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
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
                mapOf("p_name" to name)
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
            val params = buildMap {
                put("p_name", name)
                put("p_sql", sql)
                if (checksum != null) put("p_checksum", checksum)
                put("p_applied_by", appliedBy)
            }

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
}
