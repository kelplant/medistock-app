package com.medistock.data.migration

import android.content.Context
import android.content.SharedPreferences
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.remote.repository.MigrationRepository
import com.medistock.data.remote.repository.SchemaVersionDto
import com.medistock.util.NetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Vérificateur de compatibilité de version du schéma avec support offline.
 *
 * Cette classe gère la vérification de compatibilité entre l'application et
 * la base de données centrale, avec les fonctionnalités suivantes:
 *
 * 1. **Cache local**: Mémorise la dernière version connue du schéma pour
 *    permettre une vérification même hors ligne.
 *
 * 2. **Mode offline**: En mode offline, utilise la version cachée et permet
 *    l'utilisation si l'app était compatible lors de la dernière sync.
 *
 * 3. **Vérification au retour online**: Re-vérifie la compatibilité quand
 *    la connexion revient.
 *
 * 4. **Blocage ferme**: Si l'app est détectée comme incompatible (online),
 *    elle est bloquée même si on repasse offline.
 *
 * Usage:
 * ```kotlin
 * val checker = SchemaVersionChecker(context)
 *
 * // Au démarrage
 * when (val result = checker.checkCompatibility()) {
 *     is VersionCheckResult.Compatible -> continueStartup()
 *     is VersionCheckResult.UpdateRequired -> showUpdateScreen(result)
 *     is VersionCheckResult.OfflineAllowed -> continueInOfflineMode()
 *     is VersionCheckResult.OfflineBlocked -> showUpdateScreen(result)
 * }
 * ```
 */
class SchemaVersionChecker(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "schema_version_cache"
        private const val KEY_CACHED_SCHEMA_VERSION = "cached_schema_version"
        private const val KEY_CACHED_MIN_APP_VERSION = "cached_min_app_version"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_LAST_CHECK_SUCCESS = "last_check_success"
        private const val KEY_BLOCKED_VERSION = "blocked_version"

        /** Durée de validité du cache (24 heures) */
        private const val CACHE_VALIDITY_MS = 24 * 60 * 60 * 1000L
    }

    private val repository = MigrationRepository()
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Vérifie la compatibilité de l'application avec la base de données.
     *
     * Logique:
     * 1. Si online: vérifie avec le serveur et met à jour le cache
     * 2. Si offline avec cache valide: utilise le cache
     * 3. Si offline sans cache: permet l'utilisation (première fois)
     * 4. Si version bloquée: refuse même offline
     */
    suspend fun checkCompatibility(): VersionCheckResult = withContext(Dispatchers.IO) {
        val appVersion = MigrationManager.APP_SCHEMA_VERSION

        // Vérifier si l'app a été précédemment bloquée
        val blockedVersion = prefs.getInt(KEY_BLOCKED_VERSION, -1)
        if (blockedVersion > 0 && appVersion < blockedVersion) {
            return@withContext VersionCheckResult.OfflineBlocked(
                appVersion = appVersion,
                minRequired = blockedVersion,
                reason = "Version bloquée lors de la dernière vérification"
            )
        }

        // Vérifier si Supabase est configuré
        if (!SupabaseClientProvider.isConfigured(context)) {
            return@withContext VersionCheckResult.OfflineAllowed(
                reason = "Supabase non configuré - mode local uniquement"
            )
        }

        // Vérifier la connexion
        if (!NetworkStatus.isOnline(context)) {
            return@withContext handleOfflineCheck(appVersion)
        }

        // Online: vérifier avec le serveur
        return@withContext checkOnline(appVersion)
    }

    /**
     * Vérifie la compatibilité avec le serveur (mode online)
     */
    private suspend fun checkOnline(appVersion: Int): VersionCheckResult {
        return try {
            val schemaVersion = repository.getSchemaVersion()

            if (schemaVersion == null) {
                // Système de versioning non installé - compatible par défaut
                clearBlockedVersion()
                recordSuccessfulCheck(null)
                VersionCheckResult.Compatible(
                    appVersion = appVersion,
                    serverVersion = null,
                    note = "Système de versioning non installé"
                )
            } else {
                // Mettre à jour le cache
                updateCache(schemaVersion)

                if (appVersion < schemaVersion.minAppVersion) {
                    // App trop ancienne - bloquer
                    setBlockedVersion(schemaVersion.minAppVersion)
                    VersionCheckResult.UpdateRequired(
                        appVersion = appVersion,
                        minRequired = schemaVersion.minAppVersion,
                        serverVersion = schemaVersion.schemaVersion
                    )
                } else {
                    // Compatible
                    clearBlockedVersion()
                    recordSuccessfulCheck(schemaVersion)
                    VersionCheckResult.Compatible(
                        appVersion = appVersion,
                        serverVersion = schemaVersion.schemaVersion
                    )
                }
            }
        } catch (e: Exception) {
            // Erreur réseau - fallback sur le cache
            handleOfflineCheck(appVersion)
        }
    }

    /**
     * Gère la vérification en mode offline
     */
    private fun handleOfflineCheck(appVersion: Int): VersionCheckResult {
        val cachedMinVersion = prefs.getInt(KEY_CACHED_MIN_APP_VERSION, -1)
        val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0)
        val lastCheckSuccess = prefs.getBoolean(KEY_LAST_CHECK_SUCCESS, false)

        // Pas de cache - première utilisation offline
        if (cachedMinVersion == -1) {
            return VersionCheckResult.OfflineAllowed(
                reason = "Première utilisation - pas de vérification possible"
            )
        }

        // Cache trop vieux et dernière vérif échouée
        val cacheAge = System.currentTimeMillis() - lastCheckTime
        if (cacheAge > CACHE_VALIDITY_MS && !lastCheckSuccess) {
            return VersionCheckResult.OfflineAllowed(
                reason = "Cache expiré - synchronisation recommandée",
                cacheExpired = true
            )
        }

        // Vérification avec le cache
        return if (appVersion < cachedMinVersion) {
            VersionCheckResult.OfflineBlocked(
                appVersion = appVersion,
                minRequired = cachedMinVersion,
                reason = "Version incompatible (vérification depuis cache)"
            )
        } else {
            VersionCheckResult.OfflineAllowed(
                reason = "Compatible selon le cache (dernière vérif: ${formatAge(cacheAge)})"
            )
        }
    }

    /**
     * Met à jour le cache local
     */
    private fun updateCache(schemaVersion: SchemaVersionDto) {
        prefs.edit()
            .putInt(KEY_CACHED_SCHEMA_VERSION, schemaVersion.schemaVersion)
            .putInt(KEY_CACHED_MIN_APP_VERSION, schemaVersion.minAppVersion)
            .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Enregistre une vérification réussie
     */
    private fun recordSuccessfulCheck(schemaVersion: SchemaVersionDto?) {
        val editor = prefs.edit()
            .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
            .putBoolean(KEY_LAST_CHECK_SUCCESS, true)

        schemaVersion?.let {
            editor.putInt(KEY_CACHED_SCHEMA_VERSION, it.schemaVersion)
            editor.putInt(KEY_CACHED_MIN_APP_VERSION, it.minAppVersion)
        }

        editor.apply()
    }

    /**
     * Marque une version comme bloquée (persiste même offline)
     */
    private fun setBlockedVersion(minVersion: Int) {
        prefs.edit()
            .putInt(KEY_BLOCKED_VERSION, minVersion)
            .apply()
    }

    /**
     * Efface la version bloquée (après mise à jour de l'app)
     */
    private fun clearBlockedVersion() {
        prefs.edit()
            .remove(KEY_BLOCKED_VERSION)
            .apply()
    }

    /**
     * Retourne les informations de cache
     */
    fun getCacheInfo(): CacheInfo {
        return CacheInfo(
            cachedSchemaVersion = prefs.getInt(KEY_CACHED_SCHEMA_VERSION, -1).takeIf { it > 0 },
            cachedMinAppVersion = prefs.getInt(KEY_CACHED_MIN_APP_VERSION, -1).takeIf { it > 0 },
            lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0).takeIf { it > 0 },
            lastCheckSuccess = prefs.getBoolean(KEY_LAST_CHECK_SUCCESS, false),
            blockedVersion = prefs.getInt(KEY_BLOCKED_VERSION, -1).takeIf { it > 0 }
        )
    }

    /**
     * Efface tout le cache (pour les tests ou reset)
     */
    fun clearCache() {
        prefs.edit().clear().apply()
    }

    private fun formatAge(ageMs: Long): String {
        return when {
            ageMs < 60_000 -> "< 1 min"
            ageMs < 3600_000 -> "${ageMs / 60_000} min"
            ageMs < 86400_000 -> "${ageMs / 3600_000}h"
            else -> "${ageMs / 86400_000}j"
        }
    }
}

/**
 * Résultat de la vérification de version
 */
sealed class VersionCheckResult {
    /**
     * L'application est compatible avec le serveur
     */
    data class Compatible(
        val appVersion: Int,
        val serverVersion: Int? = null,
        val note: String? = null
    ) : VersionCheckResult()

    /**
     * Mise à jour requise - l'app est bloquée
     */
    data class UpdateRequired(
        val appVersion: Int,
        val minRequired: Int,
        val serverVersion: Int
    ) : VersionCheckResult()

    /**
     * Mode offline autorisé (pas de vérification possible mais pas bloqué)
     */
    data class OfflineAllowed(
        val reason: String,
        val cacheExpired: Boolean = false
    ) : VersionCheckResult()

    /**
     * Bloqué même en offline (incompatibilité détectée précédemment)
     */
    data class OfflineBlocked(
        val appVersion: Int,
        val minRequired: Int,
        val reason: String
    ) : VersionCheckResult()
}

/**
 * Informations sur le cache de version
 */
data class CacheInfo(
    val cachedSchemaVersion: Int?,
    val cachedMinAppVersion: Int?,
    val lastCheckTime: Long?,
    val lastCheckSuccess: Boolean,
    val blockedVersion: Int?
) {
    val hasCache: Boolean get() = cachedSchemaVersion != null
    val isBlocked: Boolean get() = blockedVersion != null
}
