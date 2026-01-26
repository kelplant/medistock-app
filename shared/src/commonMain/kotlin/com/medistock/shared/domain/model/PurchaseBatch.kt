package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PurchaseBatch(
    val id: String,
    val productId: String,
    val siteId: String,
    val batchNumber: String? = null,
    val purchaseDate: Long,
    val initialQuantity: Double,
    val remainingQuantity: Double,
    val purchasePrice: Double,
    val supplierName: String = "",
    val supplierId: String? = null,
    val expiryDate: Long? = null,
    val isExhausted: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val createdBy: String = "",
    val updatedBy: String = ""
)
