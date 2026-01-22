package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CurrentStock(
    val productId: String,
    val productName: String = "",
    val unit: String = "",
    val categoryName: String = "",
    val siteId: String,
    val siteName: String = "",
    val quantityOnHand: Double = 0.0,
    val minStock: Double = 0.0,
    val maxStock: Double = 0.0
) {
    // Alias for backward compatibility
    val totalStock: Double get() = quantityOnHand
}

@Serializable
data class ProductWithCategory(
    val id: String,
    val name: String,
    val unit: String,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val marginType: String? = null,
    val marginValue: Double? = null,
    val unitVolume: Double? = null,
    val description: String? = null,
    val siteId: String,
    val minStock: Double? = null,
    val maxStock: Double? = null
)

@Serializable
data class StockMovement(
    val id: String,
    val productId: String,
    val siteId: String,
    val quantity: Double,
    val movementType: String,
    val referenceId: String? = null,
    val notes: String? = null,
    val createdAt: Long = 0,
    val createdBy: String = ""
)

@Serializable
data class ProductTransfer(
    val id: String,
    val productId: String,
    val fromSiteId: String,
    val toSiteId: String,
    val quantity: Double,
    val status: String = "pending",
    val notes: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
)
