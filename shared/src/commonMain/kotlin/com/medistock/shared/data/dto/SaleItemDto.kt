package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.SaleItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the sale_items table in Supabase.
 */
@Serializable
data class SaleItemDto(
    val id: String,
    @SerialName("sale_id") val saleId: String,
    @SerialName("product_id") val productId: String,
    val quantity: Double,
    @SerialName("base_quantity") val baseQuantity: Double? = null,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("product_name") val productName: String? = null,
    val unit: String? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
    @SerialName("batch_id") val batchId: String? = null,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): SaleItem = SaleItem(
        id = id,
        saleId = saleId,
        productId = productId,
        productName = productName ?: "",
        unit = unit ?: "",
        quantity = quantity,
        baseQuantity = baseQuantity,
        unitPrice = unitPrice,
        totalPrice = totalPrice,
        batchId = batchId,
        createdAt = createdAt ?: 0,
        createdBy = createdBy ?: ""
    )

    companion object {
        fun fromModel(item: SaleItem, productName: String? = null, unit: String? = null, clientId: String? = null): SaleItemDto = SaleItemDto(
            id = item.id,
            saleId = item.saleId,
            productId = item.productId,
            quantity = item.quantity,
            baseQuantity = item.baseQuantity,
            unitPrice = item.unitPrice,
            totalPrice = item.totalPrice,
            productName = productName ?: item.productName.ifEmpty { null },
            unit = unit ?: item.unit.ifEmpty { null },
            batchId = item.batchId,
            clientId = clientId
        )
    }
}
