package com.medistock.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the inventories table in Supabase.
 * Represents a product inventory count record.
 */
@Serializable
data class InventoryDto(
    val id: String,
    @SerialName("product_id") val productId: String,
    @SerialName("site_id") val siteId: String,
    @SerialName("count_date") val countDate: Long,
    @SerialName("counted_quantity") val countedQuantity: Double,
    @SerialName("theoretical_quantity") val theoreticalQuantity: Double,
    val discrepancy: Double,
    val reason: String = "",
    @SerialName("counted_by") val countedBy: String = "",
    val notes: String = "",
    @SerialName("created_at") val createdAt: Long,
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("client_id") val clientId: String? = null
)
