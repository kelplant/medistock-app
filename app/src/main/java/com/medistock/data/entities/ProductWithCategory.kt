package com.medistock.data.entities

data class ProductWithCategory(
    val id: Long,
    val name: String,
    val unit: String,
    val categoryId: Long?,
    val categoryName: String?,
    val marginType: String?,
    val marginValue: Double?,
    val unitVolume: Double?,
    val siteId: Long,
    val minStock: Double?,
    val maxStock: Double?
)