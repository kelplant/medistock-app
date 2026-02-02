package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SaleBatchAllocation(
    val id: String,
    val saleItemId: String,
    val batchId: String,
    val quantityAllocated: Double,
    val purchasePriceAtAllocation: Double,
    val createdAt: Long = 0,
    val createdBy: String = ""
)
