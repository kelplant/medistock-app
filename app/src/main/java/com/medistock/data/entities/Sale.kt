package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val date: Long,
    val totalAmount: Double,
    val siteId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)
