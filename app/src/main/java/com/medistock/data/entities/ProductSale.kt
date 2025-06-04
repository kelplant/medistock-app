package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_sales")
data class ProductSale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val quantity: Double,
    val priceAtSale: Double,
    val farmerName: String,
    val date: Long,
    val siteId: Long
)