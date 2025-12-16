package com.medistock.data.entities

/**
 * Data class representing a Product with its associated Category name.
 * Used for queries that join products with their categories.
 */
data class ProductWithCategory(
    val id: Long,
    val name: String,
    val unit: String,
    val categoryId: Long,
    val categoryName: String,
    val marginType: String,
    val marginValue: Double,
    val unitVolume: Double,
    val siteId: Long,
    val minStock: Double = 0.0,
    val maxStock: Double = 0.0
)
