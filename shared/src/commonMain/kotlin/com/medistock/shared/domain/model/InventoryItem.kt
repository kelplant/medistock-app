package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents an individual product count during an inventory session.
 * Links to an Inventory (session) and tracks counted vs theoretical quantities.
 */
@Serializable
data class InventoryItem(
    val id: String,
    val inventoryId: String? = null,
    val productId: String,
    val siteId: String,
    val countDate: Long,
    val countedQuantity: Double,
    val theoreticalQuantity: Double,
    val discrepancy: Double = 0.0,
    val reason: String = "",
    val countedBy: String = "",
    val notes: String = "",
    val createdAt: Long = 0,
    val createdBy: String = ""
)
