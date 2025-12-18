package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val type: String,
    val quantity: Double,
    val date: Long,
    val purchasePriceAtMovement: Double,
    val sellingPriceAtMovement: Double,
    val siteId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)