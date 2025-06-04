package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_prices")
data class ProductPrice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val effectiveDate: Long,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val source: String // "manual" or "calculated"
)