package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val unit: String,
    val categoryId: Long?,  // Nullable pour correspondre aux queries
    val marginType: String?,  // Nullable
    val marginValue: Double?,  // Nullable
    val unitVolume: Double?,  // Nullable
    val siteId: Long,
    val minStock: Double? = 0.0,
    val maxStock: Double? = 0.0
)