package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.AppConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the app_config table in Supabase.
 */
@Serializable
data class AppConfigDto(
    val key: String,
    val value: String?,
    val description: String? = null,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("updated_by") val updatedBy: String?
) {
    fun toModel(): AppConfig = AppConfig(
        key = key,
        value = value,
        description = description,
        updatedAt = updatedAt,
        updatedBy = updatedBy ?: ""
    )

    companion object {
        fun fromModel(config: AppConfig): AppConfigDto = AppConfigDto(
            key = config.key,
            value = config.value,
            description = config.description,
            updatedAt = config.updatedAt,
            updatedBy = config.updatedBy
        )
    }
}
