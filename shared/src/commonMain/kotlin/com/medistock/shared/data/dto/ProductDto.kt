package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.Product
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the products table in Supabase.
 * Note: unit is no longer stored - it's derived from packaging_type based on selected_level.
 */
@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    @SerialName("unit_volume") val unitVolume: Double,
    @SerialName("packaging_type_id") val packagingTypeId: String,
    @SerialName("selected_level") val selectedLevel: Int = 1,
    @SerialName("conversion_factor") val conversionFactor: Double? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("margin_type") val marginType: String? = null,
    @SerialName("margin_value") val marginValue: Double? = null,
    val description: String? = null,
    @SerialName("site_id") val siteId: String,
    @SerialName("min_stock") val minStock: Double? = 0.0,
    @SerialName("max_stock") val maxStock: Double? = 0.0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("updated_by") val updatedBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): Product = Product(
        id = id,
        name = name,
        unitVolume = unitVolume,
        packagingTypeId = packagingTypeId,
        selectedLevel = selectedLevel,
        conversionFactor = conversionFactor,
        categoryId = categoryId,
        marginType = marginType,
        marginValue = marginValue,
        description = description,
        siteId = siteId,
        minStock = minStock,
        maxStock = maxStock,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    companion object {
        fun fromModel(product: Product, clientId: String? = null): ProductDto = ProductDto(
            id = product.id,
            name = product.name,
            unitVolume = product.unitVolume,
            packagingTypeId = product.packagingTypeId,
            selectedLevel = product.selectedLevel,
            conversionFactor = product.conversionFactor,
            categoryId = product.categoryId,
            marginType = product.marginType,
            marginValue = product.marginValue,
            description = product.description,
            siteId = product.siteId,
            minStock = product.minStock,
            maxStock = product.maxStock,
            isActive = product.isActive,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt,
            createdBy = product.createdBy,
            updatedBy = product.updatedBy,
            clientId = clientId
        )
    }
}
