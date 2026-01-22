package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.PurchaseBatch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the purchase_batches table in Supabase.
 */
@Serializable
data class PurchaseBatchDto(
    val id: String,
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
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("updated_by") val updatedBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): PurchaseBatch = PurchaseBatch(
        id = id,
        productId = productId,
        siteId = siteId,
        batchNumber = batchNumber,
        purchaseDate = purchaseDate,
        initialQuantity = initialQuantity,
        remainingQuantity = remainingQuantity,
        purchasePrice = purchasePrice,
        supplierName = supplierName,
        expiryDate = expiryDate,
        isExhausted = isExhausted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    companion object {
        fun fromModel(batch: PurchaseBatch, clientId: String? = null): PurchaseBatchDto = PurchaseBatchDto(
            id = batch.id,
            productId = batch.productId,
            siteId = batch.siteId,
            batchNumber = batch.batchNumber,
            purchaseDate = batch.purchaseDate,
            initialQuantity = batch.initialQuantity,
            remainingQuantity = batch.remainingQuantity,
            purchasePrice = batch.purchasePrice,
            supplierName = batch.supplierName,
            expiryDate = batch.expiryDate,
            isExhausted = batch.isExhausted,
            createdAt = batch.createdAt,
            updatedAt = batch.updatedAt,
            createdBy = batch.createdBy,
            updatedBy = batch.updatedBy,
            clientId = clientId
        )
    }
}
