package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList

/**
 * Repository for app configuration key-value storage.
 */
class AppConfigRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    /**
     * Get all configuration entries.
     */
    suspend fun getAll(): List<AppConfig> = withContext(Dispatchers.Default) {
        queries.getAllAppConfig().executeAsList().map { it.toModel() }
    }

    /**
     * Get a configuration value by key.
     */
    suspend fun getByKey(key: String): AppConfig? = withContext(Dispatchers.Default) {
        queries.getAppConfigByKey(key).executeAsOneOrNull()?.toModel()
    }

    /**
     * Get a configuration value as string, or return default if not found.
     */
    suspend fun getValue(key: String, default: String? = null): String? = withContext(Dispatchers.Default) {
        queries.getAppConfigByKey(key).executeAsOneOrNull()?.value_ ?: default
    }

    /**
     * Get the currency symbol, with fallback to default.
     */
    suspend fun getCurrencySymbol(): String = withContext(Dispatchers.Default) {
        queries.getAppConfigByKey(AppConfig.KEY_CURRENCY_SYMBOL).executeAsOneOrNull()?.value_
            ?: AppConfig.DEFAULT_CURRENCY_SYMBOL
    }

    /**
     * Set the currency symbol.
     */
    suspend fun setCurrencySymbol(symbol: String, updatedBy: String) = withContext(Dispatchers.Default) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        queries.upsertAppConfig(
            key = AppConfig.KEY_CURRENCY_SYMBOL,
            value_ = symbol,
            description = "Currency symbol for prices display",
            updated_at = now,
            updated_by = updatedBy
        )
    }

    /**
     * Insert a new configuration entry.
     */
    suspend fun insert(config: AppConfig) = withContext(Dispatchers.Default) {
        queries.insertAppConfig(
            key = config.key,
            value_ = config.value,
            description = config.description,
            updated_at = config.updatedAt,
            updated_by = config.updatedBy
        )
    }

    /**
     * Update an existing configuration entry.
     */
    suspend fun update(config: AppConfig) = withContext(Dispatchers.Default) {
        queries.updateAppConfig(
            value_ = config.value,
            description = config.description,
            updated_at = config.updatedAt,
            updated_by = config.updatedBy,
            key = config.key
        )
    }

    /**
     * Upsert (insert or replace) a configuration entry.
     */
    suspend fun upsert(config: AppConfig) = withContext(Dispatchers.Default) {
        queries.upsertAppConfig(
            key = config.key,
            value_ = config.value,
            description = config.description,
            updated_at = config.updatedAt,
            updated_by = config.updatedBy
        )
    }

    /**
     * Set a configuration value by key.
     */
    suspend fun setValue(key: String, value: String?, description: String? = null, updatedBy: String) = withContext(Dispatchers.Default) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        queries.upsertAppConfig(
            key = key,
            value_ = value,
            description = description,
            updated_at = now,
            updated_by = updatedBy
        )
    }

    /**
     * Delete a configuration entry.
     */
    suspend fun delete(key: String) = withContext(Dispatchers.Default) {
        queries.deleteAppConfig(key)
    }

    /**
     * Observe all configuration entries.
     */
    fun observeAll(): Flow<List<AppConfig>> {
        return queries.getAllAppConfig()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    private fun com.medistock.shared.db.App_config.toModel(): AppConfig {
        return AppConfig(
            key = key,
            value = value_,
            description = description,
            updatedAt = updated_at,
            updatedBy = updated_by
        )
    }
}
