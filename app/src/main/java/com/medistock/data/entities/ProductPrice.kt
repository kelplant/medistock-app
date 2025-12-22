package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "product_prices")
data class ProductPrice(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val effectiveDate: Long,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val source: String, // "manual" or "calculated"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val updatedBy: String = ""
)
