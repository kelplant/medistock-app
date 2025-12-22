package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val type: String,
    val quantity: Double,
    val date: Long,
    val purchasePriceAtMovement: Double,
    val sellingPriceAtMovement: Double,
    val siteId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)
