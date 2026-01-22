package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.Site
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the sites table in Supabase.
 * Uses snake_case for JSON serialization to match database column names.
 */
@Serializable
data class SiteDto(
    val id: String,
    val name: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("updated_by") val updatedBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    /**
     * Convert this DTO to a domain model.
     */
    fun toModel(): Site = Site(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    companion object {
        /**
         * Create a DTO from a domain model.
         * @param site The domain model to convert
         * @param clientId Optional client ID for realtime filtering
         */
        fun fromModel(site: Site, clientId: String? = null): SiteDto = SiteDto(
            id = site.id,
            name = site.name,
            createdAt = site.createdAt,
            updatedAt = site.updatedAt,
            createdBy = site.createdBy,
            updatedBy = site.updatedBy,
            clientId = clientId
        )
    }
}
