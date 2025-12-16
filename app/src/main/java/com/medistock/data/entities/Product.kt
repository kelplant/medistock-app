package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String,
    val categoryId: Long,
    val marginType: String,
    val marginValue: Double,
    val unitVolume: Double,
    val siteId: Long,
    val minStock: Double = 0.0, // Minimum stock threshold for alerts
    val maxStock: Double = 0.0  // Maximum stock threshold for ordering
)