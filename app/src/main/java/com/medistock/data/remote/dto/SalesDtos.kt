package com.medistock.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * DTO pour la table sales
 */
@Serializable
data class SaleDto(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("customer_name") val customerName: String,
    @SerialName("customer_id") val customerId: String? = null,
    val date: Long,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("site_id") val siteId: String,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = ""
)

/**
 * DTO pour la table sale_items
 */
@Serializable
data class SaleItemDto(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("sale_id") val saleId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    val unit: String,
    val quantity: Double,
    @SerialName("price_per_unit") val pricePerUnit: Double,
    val subtotal: Double,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = ""
)

/**
 * DTO pour la table sale_batch_allocations
 */
@Serializable
data class SaleBatchAllocationDto(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("sale_item_id") val saleItemId: String,
    @SerialName("batch_id") val batchId: String,
    @SerialName("quantity_allocated") val quantityAllocated: Double,
    @SerialName("purchase_price_at_allocation") val purchasePriceAtAllocation: Double,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = ""
)

