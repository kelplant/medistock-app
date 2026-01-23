package com.medistock.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the current_stock view in Supabase.
 * This is a read-only DTO used to fetch aggregated stock information.
 * Uses snake_case for JSON serialization to match database column names.
 */
@Serializable
data class CurrentStockDto(
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    val description: String? = null,
    @SerialName("site_id") val siteId: String,
    @SerialName("site_name") val siteName: String,
    @SerialName("current_stock") val currentStock: Double,
    @SerialName("min_stock") val minStock: Double? = null,
    @SerialName("max_stock") val maxStock: Double? = null,
    @SerialName("stock_status") val stockStatus: String
)
