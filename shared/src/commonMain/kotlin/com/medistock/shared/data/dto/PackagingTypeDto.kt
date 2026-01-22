package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.PackagingType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the packaging_types table in Supabase.
 */
@Serializable
data class PackagingTypeDto(
    val id: String,
    val name: String,
    @SerialName("level1_name") val level1Name: String,
    @SerialName("level2_name") val level2Name: String? = null,
    @SerialName("level2_quantity") val level2Quantity: Int? = null,
    @SerialName("default_conversion_factor") val defaultConversionFactor: Double? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): PackagingType = PackagingType(
        id = id,
        name = name,
        level1Name = level1Name,
        level2Name = level2Name,
        level2Quantity = level2Quantity,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy ?: "",
        updatedBy = updatedBy ?: ""
    )

    companion object {
        fun fromModel(packagingType: PackagingType, clientId: String? = null): PackagingTypeDto = PackagingTypeDto(
            id = packagingType.id,
            name = packagingType.name,
            level1Name = packagingType.level1Name,
            level2Name = packagingType.level2Name,
            level2Quantity = packagingType.level2Quantity,
            createdAt = packagingType.createdAt,
            updatedAt = packagingType.updatedAt,
            createdBy = packagingType.createdBy,
            updatedBy = packagingType.updatedBy,
            clientId = clientId
        )
    }
}
