package com.medistock.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * DTO pour la table products
 */
@Serializable
data class ProductDto(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val unit: String,
    @SerialName("unit_volume") val unitVolume: Double,
    @SerialName("packaging_type_id") val packagingTypeId: String? = null,
    @SerialName("selected_level") val selectedLevel: Int? = null,
    @SerialName("conversion_factor") val conversionFactor: Double? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("margin_type") val marginType: String? = null,
    @SerialName("margin_value") val marginValue: Double? = null,
    val description: String? = null,
    @SerialName("site_id") val siteId: String,
    @SerialName("min_stock") val minStock: Double? = 0.0,
    @SerialName("max_stock") val maxStock: Double? = 0.0,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("updated_by") val updatedBy: String = ""
)

/**
 * DTO pour la table product_prices
 */
@Serializable
data class ProductPriceDto(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("product_id") val productId: String,
    @SerialName("effective_date") val effectiveDate: Long,
    @SerialName("purchase_price") val purchasePrice: Double,
    @SerialName("selling_price") val sellingPrice: Double,
    val source: String, // "manual" or "calculated"
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("updated_by") val updatedBy: String = ""
)
