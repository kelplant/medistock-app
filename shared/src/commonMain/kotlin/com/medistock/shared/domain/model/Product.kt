package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Product model.
 * Note: The unit is NOT stored in the database. It is derived from:
 * - If selectedLevel = 1: PackagingType.level1Name
 * - If selectedLevel = 2: PackagingType.level2Name
 * Use ProductWithPackaging or load the PackagingType separately to get the unit.
 */
@Serializable
data class Product(
    val id: String,
    val name: String,
    val unitVolume: Double,
    val packagingTypeId: String,
    val selectedLevel: Int = 1,
    val conversionFactor: Double? = null,
    val categoryId: String? = null,
    val marginType: String? = null,
    val marginValue: Double? = null,
    val description: String? = null,
    val siteId: String,
    val minStock: Double? = 0.0,
    val maxStock: Double? = 0.0,
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
)

/**
 * Product with packaging information for UI display.
 * Includes the computed unit based on selectedLevel.
 */
@Serializable
data class ProductWithPackaging(
    val product: Product,
    val packagingType: PackagingType,
    val categoryName: String? = null
) {
    /** The unit name based on selected level */
    val unit: String
        get() = packagingType.getLevelName(product.selectedLevel) ?: packagingType.level1Name

    /** Shortcut properties for convenience */
    val id: String get() = product.id
    val name: String get() = product.name
    val unitVolume: Double get() = product.unitVolume
    val siteId: String get() = product.siteId
    val selectedLevel: Int get() = product.selectedLevel
    val conversionFactor: Double? get() = product.conversionFactor
    val categoryId: String? get() = product.categoryId
    val minStock: Double? get() = product.minStock
    val maxStock: Double? get() = product.maxStock
    val isActive: Boolean get() = product.isActive
}

