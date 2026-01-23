package com.medistock.data.migration

import android.content.Context
import com.medistock.data.remote.repository.MigrationRepository
import com.medistock.data.remote.repository.MigrationResult
import com.medistock.shared.domain.compatibility.CompatibilityChecker
import com.medistock.shared.domain.compatibility.CompatibilityResult
import com.medistock.shared.domain.compatibility.SchemaVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Représente une migration SQL à appliquer
 */
data class Migration(
    val name: String,
    val sql: String,
    val checksum: String
) {
    companion object {
        /**
         * Extrait le nom de la migration depuis le nom du fichier
         * Ex: "2026011702_add_feature.sql" -> "2026011702_add_feature"
         */
        fun nameFromFileName(fileName: String): String {
            return fileName.removeSuffix(".sql")
        }
    }
}

/**
 * Résultat de l'exécution des migrations
 */
data class MigrationRunResult(
    val success: Boolean,
    val migrationsApplied: List<String>,
    val migrationsFailed: List<Pair<String, String>>, // name to error message
    val migrationsSkipped: List<String>, // already applied
    val systemNotInstalled: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Callback pour suivre la progression des migrations
 */
interface MigrationProgressListener {
    fun onMigrationStart(name: String, current: Int, total: Int)
    fun onMigrationComplete(name: String, result: MigrationResult)
    fun onAllComplete(result: MigrationRunResult)
}

/**
 * Gestionnaire des migrations de schéma Supabase
 *
 * Ce gestionnaire:
 * - Charge les fichiers SQL depuis assets/migrations/
 * - Compare avec les migrations déjà appliquées dans Supabase
 * - Applique les nouvelles migrations dans l'ordre alphabétique
 *
 * Usage:
 * ```kotlin
 * val manager = MigrationManager(context)
 * val result = manager.runPendingMigrations()
 * if (result.success) {
 *     println("${result.migrationsApplied.size} migrations applied")
 * }
 * ```
 */
class MigrationManager(
    private val context: Context,
    private val repository: MigrationRepository = MigrationRepository()
) {
    companion object {
        private const val TAG = "MigrationManager"
        private const val MIGRATIONS_FOLDER = "migrations"

        /**
         * Version du schéma supportée par cette version de l'app.
         * @see CompatibilityChecker.APP_SCHEMA_VERSION for the actual value
         */
        val APP_SCHEMA_VERSION: Int
            get() = CompatibilityChecker.APP_SCHEMA_VERSION
    }

    /**
     * Vérifie si cette version de l'app est compatible avec la base de données.
     *
     * Cette vérification doit être faite AVANT de tenter d'utiliser l'app.
     * Si le résultat est AppTooOld, l'utilisateur doit mettre à jour l'app.
     *
     * @return CompatibilityResult indiquant si l'app peut être utilisée
     */
    suspend fun checkCompatibility(): CompatibilityResult {
        return try {
            val schemaVersionDto = repository.getSchemaVersion()

            // Convert DTO to shared model
            val schemaVersion = schemaVersionDto?.let {
                SchemaVersion(
                    schemaVersion = it.schemaVersion,
                    minAppVersion = it.minAppVersion,
                    updatedAt = it.updatedAt
                )
            }

            // Use shared compatibility checker
            val result = CompatibilityChecker.checkCompatibility(schemaVersion)

            // Log the result
            println(CompatibilityChecker.formatCompatibilityInfo(result))

            result
        } catch (e: Exception) {
            println("⚠️ Impossible de vérifier la compatibilité: ${e.message}")
            CompatibilityResult.Unknown(e.message ?: "Unknown error")
        }
    }

    /**
     * Charge toutes les migrations depuis assets/migrations/
     * Les fichiers doivent suivre le format: YYYYMMDDNN_description.sql
     */
    suspend fun loadMigrationsFromAssets(): List<Migration> = withContext(Dispatchers.IO) {
        try {
            val assetManager = context.assets
            val files = assetManager.list(MIGRATIONS_FOLDER) ?: emptyArray()

            files
                .filter { it.endsWith(".sql") }
                .sorted() // Tri alphabétique = ordre chronologique avec le format YYYYMMDDNN
                .mapNotNull { fileName ->
                    try {
                        val sql = assetManager.open("$MIGRATIONS_FOLDER/$fileName")
                            .bufferedReader()
                            .use { it.readText() }

                        Migration(
                            name = Migration.nameFromFileName(fileName),
                            sql = sql,
                            checksum = calculateMD5(sql)
                        )
                    } catch (e: Exception) {
                        println("⚠️ Erreur lors du chargement de $fileName: ${e.message}")
                        null
                    }
                }
        } catch (e: Exception) {
            println("❌ Erreur lors du listage des migrations: ${e.message}")
            emptyList()
        }
    }

    /**
     * Récupère les migrations qui n'ont pas encore été appliquées
     */
    suspend fun getPendingMigrations(): List<Migration> {
        val allMigrations = loadMigrationsFromAssets()
        val appliedMigrations = repository.getAppliedMigrations()
            .filter { it.success }
            .map { it.name }
            .toSet()

        return allMigrations.filter { it.name !in appliedMigrations }
    }

    /**
     * Exécute toutes les migrations en attente
     *
     * IMPORTANT: This method now requires authentication. Pass the Supabase Auth
     * access token to use the secure Edge Function for migration execution.
     * If no token is provided, falls back to direct RPC (legacy mode).
     *
     * @param listener Callback optionnel pour suivre la progression
     * @param appliedBy Identifiant de l'utilisateur/app qui applique les migrations
     * @param accessToken Supabase Auth access token for Edge Function authentication
     */
    suspend fun runPendingMigrations(
        listener: MigrationProgressListener? = null,
        appliedBy: String = "app",
        accessToken: String? = null
    ): MigrationRunResult {
        // Vérifier si le système de migration est installé
        if (!repository.isMigrationSystemInstalled()) {
            val result = MigrationRunResult(
                success = false,
                migrationsApplied = emptyList(),
                migrationsFailed = emptyList(),
                migrationsSkipped = emptyList(),
                systemNotInstalled = true,
                errorMessage = "Migration system not installed in Supabase. Please run 2026011701_migration_system.sql first."
            )
            listener?.onAllComplete(result)
            return result
        }

        val pendingMigrations = getPendingMigrations()

        if (pendingMigrations.isEmpty()) {
            val result = MigrationRunResult(
                success = true,
                migrationsApplied = emptyList(),
                migrationsFailed = emptyList(),
                migrationsSkipped = emptyList()
            )
            listener?.onAllComplete(result)
            return result
        }

        println("Pending migrations: ${pendingMigrations.size} migration(s)")
        if (accessToken != null) {
            println("Using Edge Function for secure migration execution")
        } else {
            println("Warning: No access token provided, using legacy RPC method")
        }

        val applied = mutableListOf<String>()
        val failed = mutableListOf<Pair<String, String>>()
        val skipped = mutableListOf<String>()

        pendingMigrations.forEachIndexed { index, migration ->
            listener?.onMigrationStart(migration.name, index + 1, pendingMigrations.size)
            println("Applying migration: ${migration.name}...")

            val result = repository.applyMigration(
                name = migration.name,
                sql = migration.sql,
                checksum = migration.checksum,
                appliedBy = appliedBy,
                accessToken = accessToken
            )

            listener?.onMigrationComplete(migration.name, result)

            when {
                result.alreadyApplied -> {
                    println("Skipped: ${migration.name} (already applied)")
                    skipped.add(migration.name)
                }
                result.success -> {
                    println("Success: ${migration.name} applied in ${result.executionTimeMs}ms")
                    applied.add(migration.name)
                }
                else -> {
                    println("Failed: ${migration.name} - ${result.error}")
                    failed.add(migration.name to (result.error ?: "Unknown error"))
                    // On arrête en cas d'échec pour éviter les problèmes de dépendances
                    val runResult = MigrationRunResult(
                        success = false,
                        migrationsApplied = applied,
                        migrationsFailed = failed,
                        migrationsSkipped = skipped,
                        errorMessage = "Migration ${migration.name} failed: ${result.error}"
                    )
                    listener?.onAllComplete(runResult)
                    return runResult
                }
            }
        }

        val runResult = MigrationRunResult(
            success = true,
            migrationsApplied = applied,
            migrationsFailed = failed,
            migrationsSkipped = skipped
        )
        listener?.onAllComplete(runResult)
        return runResult
    }

    /**
     * Vérifie l'état des migrations sans les appliquer
     *
     * @return Triple de (appliquées, en attente, système installé)
     */
    suspend fun checkMigrationStatus(): Triple<List<String>, List<String>, Boolean> {
        val systemInstalled = repository.isMigrationSystemInstalled()

        if (!systemInstalled) {
            return Triple(emptyList(), emptyList(), false)
        }

        val allMigrations = loadMigrationsFromAssets().map { it.name }
        val appliedMigrations = repository.getAppliedMigrations()
            .filter { it.success }
            .map { it.name }

        val pendingMigrations = allMigrations.filter { it !in appliedMigrations }

        return Triple(appliedMigrations, pendingMigrations, true)
    }

    /**
     * Calcule le hash MD5 d'une chaîne
     */
    private fun calculateMD5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
