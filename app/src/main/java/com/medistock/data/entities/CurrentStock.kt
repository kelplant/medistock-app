package com.medistock.data.entities

/**
 * Represents the current stock level for a product at a specific site.
 * Calculated from stock movements (entries and exits).
 */
data class CurrentStock(
    val productId: Long,
    val productName: String,
    val unit: String,
    val categoryName: String,
    val siteId: Long,
    val siteName: String,
    val quantityOnHand: Double
)
