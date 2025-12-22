package com.medistock.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * DTO pour la table purchase_batches
 */
@Serializable
data class PurchaseBatchDto(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("product_id") val productId: String,
    @SerialName("site_id") val siteId: String,
    @SerialName("batch_number") val batchNumber: String? = null,
    @SerialName("purchase_date") val purchaseDate: Long,
    @SerialName("initial_quantity") val initialQuantity: Double,
    @SerialName("remaining_quantity") val remainingQuantity: Double,
    @SerialName("purchase_price") val purchasePrice: Double,
    @SerialName("supplier_name") val supplierName: String = "",
    @SerialName("expiry_date") val expiryDate: Long? = null,
    @SerialName("is_exhausted") val isExhausted: Boolean = false,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("updated_by") val updatedBy: String = ""
)

/**
 * DTO pour la table stock_movements
 */
@Serializable
data class StockMovementDto(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("product_id") val productId: String,
    val type: String, // "IN" or "OUT"
    val quantity: Double,
    val date: Long,
    @SerialName("purchase_price_at_movement") val purchasePriceAtMovement: Double,
    @SerialName("selling_price_at_movement") val sellingPriceAtMovement: Double,
    @SerialName("site_id") val siteId: String,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = ""
)

/**
 * DTO pour la table inventories
 */
@Serializable
data class InventoryDto(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("product_id") val productId: String,
    @SerialName("site_id") val siteId: String,
    @SerialName("count_date") val countDate: Long,
    @SerialName("counted_quantity") val countedQuantity: Double,
    @SerialName("theoretical_quantity") val theoreticalQuantity: Double,
    val discrepancy: Double,
    val reason: String = "",
    @SerialName("counted_by") val countedBy: String = "",
    val notes: String = "",
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = ""
)

/**
 * DTO pour la table product_transfers
 */
@Serializable
data class ProductTransferDto(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("product_id") val productId: String,
    val quantity: Double,
    @SerialName("from_site_id") val fromSiteId: String,
    @SerialName("to_site_id") val toSiteId: String,
    val date: Long,
    val notes: String = "",
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("updated_by") val updatedBy: String = ""
)
