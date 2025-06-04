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
    val siteId: Long
)