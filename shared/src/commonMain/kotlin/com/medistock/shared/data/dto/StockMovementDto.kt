package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.StockMovement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the stock_movements table in Supabase.
 */
@Serializable
data class StockMovementDto(
    val id: String,
    @SerialName("product_id") val productId: String,
    @SerialName("site_id") val siteId: String,
    val quantity: Double,
    @SerialName("type") val movementType: String,
    @SerialName("reference_id") val referenceId: String? = null,
    val notes: String? = null,
    val date: Long? = null,
    @SerialName("purchase_price_at_movement") val purchasePriceAtMovement: Double? = null,
    @SerialName("selling_price_at_movement") val sellingPriceAtMovement: Double? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): StockMovement = StockMovement(
        id = id,
        productId = productId,
        siteId = siteId,
        quantity = quantity,
        movementType = movementType,
        referenceId = referenceId,
        notes = notes,
        createdAt = createdAt,
        createdBy = createdBy
    )

    companion object {
        fun fromModel(movement: StockMovement, clientId: String? = null): StockMovementDto = StockMovementDto(
            id = movement.id,
            productId = movement.productId,
            siteId = movement.siteId,
            quantity = movement.quantity,
            movementType = movement.movementType,
            referenceId = movement.referenceId,
            notes = movement.notes,
            date = movement.createdAt,
            createdAt = movement.createdAt,
            createdBy = movement.createdBy,
            clientId = clientId
        )
    }
}
