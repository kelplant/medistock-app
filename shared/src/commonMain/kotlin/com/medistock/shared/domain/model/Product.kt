package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val unit: String,
    val unitVolume: Double,
    val packagingTypeId: String? = null,
    val selectedLevel: Int? = null,
    val conversionFactor: Double? = null,
    val categoryId: String? = null,
    val marginType: String? = null,
    val marginValue: Double? = null,
    val description: String? = null,
    val siteId: String,
    val minStock: Double? = 0.0,
    val maxStock: Double? = 0.0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
)

