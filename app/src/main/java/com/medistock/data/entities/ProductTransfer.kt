package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "product_transfers")
data class ProductTransfer(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val quantity: Double,
    val fromSiteId: String,
    val toSiteId: String,
    val date: Long,
    val notes: String = "", // Optional notes about the transfer
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val updatedBy: String = ""
)
