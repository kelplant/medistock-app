package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.ProductTransfer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the product_transfers table in Supabase.
 */
@Serializable
data class ProductTransferDto(
    val id: String,
    @SerialName("product_id") val productId: String,
    @SerialName("from_site_id") val fromSiteId: String,
    @SerialName("to_site_id") val toSiteId: String,
    val quantity: Double,
    val status: String = "pending",
    val notes: String? = null,
    val date: Long? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("updated_by") val updatedBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): ProductTransfer = ProductTransfer(
        id = id,
        productId = productId,
        fromSiteId = fromSiteId,
        toSiteId = toSiteId,
        quantity = quantity,
        status = status,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )

    companion object {
        fun fromModel(transfer: ProductTransfer, clientId: String? = null): ProductTransferDto = ProductTransferDto(
            id = transfer.id,
            productId = transfer.productId,
            fromSiteId = transfer.fromSiteId,
            toSiteId = transfer.toSiteId,
            quantity = transfer.quantity,
            status = transfer.status,
            notes = transfer.notes,
            createdAt = transfer.createdAt,
            updatedAt = transfer.updatedAt,
            createdBy = transfer.createdBy,
            updatedBy = transfer.updatedBy,
            clientId = clientId
        )
    }
}
