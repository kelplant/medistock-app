package com.medistock.data.entities

data class ProductWithCategory(
    val id: String,
    val name: String,
    val unit: String,
    val categoryId: String?,
    val categoryName: String?,
    val marginType: String?,
    val marginValue: Double?,
    val unitVolume: Double?,
    val siteId: String,
    val minStock: Double?,
    val maxStock: Double?
)
