package com.medistock.shared.data.dto

import com.medistock.shared.domain.model.SaleBatchAllocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the sale_batch_allocations table in Supabase.
 */
@Serializable
data class SaleBatchAllocationDto(
    val id: String,
    @SerialName("sale_item_id") val saleItemId: String,
    @SerialName("batch_id") val batchId: String,
    val quantity: Double,
    @SerialName("unit_cost") val unitCost: Double,
    @SerialName("quantity_allocated") val quantityAllocated: Double? = null,
    @SerialName("purchase_price_at_allocation") val purchasePriceAtAllocation: Double? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): SaleBatchAllocation = SaleBatchAllocation(
        id = id,
        saleItemId = saleItemId,
        batchId = batchId,
        quantity = quantityAllocated ?: quantity,
        unitCost = purchasePriceAtAllocation ?: unitCost
    )

    companion object {
        fun fromModel(allocation: SaleBatchAllocation, clientId: String? = null): SaleBatchAllocationDto = SaleBatchAllocationDto(
            id = allocation.id,
            saleItemId = allocation.saleItemId,
            batchId = allocation.batchId,
            quantity = allocation.quantity,
            unitCost = allocation.unitCost,
            quantityAllocated = allocation.quantity,
            purchasePriceAtAllocation = allocation.unitCost,
            clientId = clientId
        )
    }
}
