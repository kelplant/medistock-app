package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerName: String,
    val customerId: String? = null,
    val date: Long,
    val totalAmount: Double,
    val siteId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)
