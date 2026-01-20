package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CurrentStock(
    val productId: String,
    val siteId: String,
    val totalStock: Double
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
