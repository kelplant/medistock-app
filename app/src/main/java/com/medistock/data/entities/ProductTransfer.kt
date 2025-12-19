package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_transfers")
data class ProductTransfer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val quantity: Double,
    val fromSiteId: Long,
    val toSiteId: Long,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)
