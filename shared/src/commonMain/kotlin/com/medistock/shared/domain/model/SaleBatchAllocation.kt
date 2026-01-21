package com.medistock.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SaleBatchAllocation(
    val id: String,
    val saleItemId: String,
    val batchId: String,
    val quantity: Double,
    val unitCost: Double
)
