package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Sale(
    val id: String,
    val customerName: String,
    val customerId: String? = null,
    val date: Long,
    val totalAmount: Double,
    val siteId: String,
    val createdAt: Long = 0,
    val createdBy: String = ""
)

@Serializable
data class SaleItem(
    val id: String,
    val saleId: String,
    val productId: String,
    val productName: String = "",
    val unit: String = "",
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double,
    val createdAt: Long = 0,
    val createdBy: String = ""
) {
    // Aliases for backward compatibility
    val pricePerUnit: Double get() = unitPrice
    val subtotal: Double get() = totalPrice
}

@Serializable
data class SaleWithItems(
    val sale: Sale,
    val items: List<SaleItem>
)
