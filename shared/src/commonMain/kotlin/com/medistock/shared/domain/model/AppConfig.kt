package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Application configuration key-value storage.
 * Used for global settings like currency symbol, instance name, etc.
 */
@Serializable
data class AppConfig(
    val key: String,
    val value: String?,
    val description: String? = null,
    val updatedAt: Long = 0,
    val updatedBy: String = ""
) {
    companion object {
        // Configuration keys
        const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        const val KEY_INSTANCE_NAME = "instance_name"
        const val KEY_RECOVERY_SECRET_KEY = "recovery_secret_key"
        const val KEY_SETUP_COMPLETED_AT = "setup_completed_at"

        // Default values
        const val DEFAULT_CURRENCY_SYMBOL = "F"
    }
}
