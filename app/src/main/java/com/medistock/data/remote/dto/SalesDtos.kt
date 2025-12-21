package com.medistock.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO pour la table sales
 */
@Serializable
data class SaleDto(
    val id: Long = 0,
    @SerialName("customer_name") val customerName: String,
    @SerialName("customer_id") val customerId: Long? = null,
    val date: Long,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("site_id") val siteId: Long,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = ""
)

/**
 * DTO pour la table sale_items
 */
@Serializable
data class SaleItemDto(
    val id: Long = 0,
    @SerialName("sale_id") val saleId: Long,
    @SerialName("product_id") val productId: Long,
    @SerialName("product_name") val productName: String,
    val unit: String,
    val quantity: Double,
    @SerialName("price_per_unit") val pricePerUnit: Double,
    val subtotal: Double
)

/**
 * DTO pour la table sale_batch_allocations
 */
@Serializable
data class SaleBatchAllocationDto(
    val id: Long = 0,
    @SerialName("sale_item_id") val saleItemId: Long,
    @SerialName("batch_id") val batchId: Long,
    @SerialName("quantity_allocated") val quantityAllocated: Double,
    @SerialName("purchase_price_at_allocation") val purchasePriceAtAllocation: Double,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * DTO pour la table product_sales (ancien système)
 */
@Serializable
data class ProductSaleDto(
    val id: Long = 0,
    @SerialName("product_id") val productId: Long,
    val quantity: Double,
    @SerialName("price_at_sale") val priceAtSale: Double,
    @SerialName("farmer_name") val farmerName: String,
    val date: Long,
    @SerialName("site_id") val siteId: Long,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = ""
)
