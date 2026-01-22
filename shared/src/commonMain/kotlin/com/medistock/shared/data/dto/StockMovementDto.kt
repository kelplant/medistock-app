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
    val type: String,
    val date: Long,
    @SerialName("purchase_price_at_movement") val purchasePriceAtMovement: Double = 0.0,
    @SerialName("selling_price_at_movement") val sellingPriceAtMovement: Double = 0.0,
    // Legacy field for backward compatibility
    @SerialName("movement_type") val movementType: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): StockMovement = StockMovement(
        id = id,
        productId = productId,
        siteId = siteId,
        quantity = quantity,
        type = type,
        date = date,
        purchasePriceAtMovement = purchasePriceAtMovement,
        sellingPriceAtMovement = sellingPriceAtMovement,
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
            type = movement.type,
            date = movement.date,
            purchasePriceAtMovement = movement.purchasePriceAtMovement,
            sellingPriceAtMovement = movement.sellingPriceAtMovement,
            movementType = movement.movementType ?: movement.type,
            referenceId = movement.referenceId,
            notes = movement.notes,
            createdAt = movement.createdAt,
            createdBy = movement.createdBy,
            clientId = clientId
        )
    }
}
