package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "product_sales")
data class ProductSale(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val quantity: Double,
    val priceAtSale: Double,
    val farmerName: String,
    val date: Long,
    val siteId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)
