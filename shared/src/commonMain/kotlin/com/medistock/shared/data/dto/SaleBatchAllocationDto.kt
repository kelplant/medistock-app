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
    @SerialName("quantity_allocated") val quantityAllocated: Double,
    @SerialName("purchase_price_at_allocation") val purchasePriceAtAllocation: Double,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("client_id") val clientId: String? = null
) {
    fun toModel(): SaleBatchAllocation = SaleBatchAllocation(
        id = id,
        saleItemId = saleItemId,
        batchId = batchId,
        quantityAllocated = quantityAllocated,
        purchasePriceAtAllocation = purchasePriceAtAllocation,
        createdAt = createdAt,
        createdBy = createdBy
    )

    companion object {
        fun fromModel(allocation: SaleBatchAllocation, clientId: String? = null): SaleBatchAllocationDto = SaleBatchAllocationDto(
            id = allocation.id,
            saleItemId = allocation.saleItemId,
            batchId = allocation.batchId,
            quantityAllocated = allocation.quantityAllocated,
            purchasePriceAtAllocation = allocation.purchasePriceAtAllocation,
            createdAt = allocation.createdAt,
            createdBy = allocation.createdBy,
            clientId = clientId
        )
    }
}
