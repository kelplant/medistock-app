package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.Category
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the categories table in Supabase.
 */
@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("updated_by") val updatedBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): Category = Category(
        id = id,
        name = name,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    companion object {
        fun fromModel(category: Category, clientId: String? = null): CategoryDto = CategoryDto(
            id = category.id,
            name = category.name,
            isActive = category.isActive,
            createdAt = category.createdAt,
            updatedAt = category.updatedAt,
            createdBy = category.createdBy,
            updatedBy = category.updatedBy,
            clientId = clientId
        )
    }
}
