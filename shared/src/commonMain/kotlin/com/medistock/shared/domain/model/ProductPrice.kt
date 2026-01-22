package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductPrice(
    val id: String,
    val productId: String,
    val effectiveDate: Long,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val source: String = "manual", // "manual" or "calculated"
    // Legacy fields for backward compatibility
    val siteId: String? = null,
    val price: Double? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
)
