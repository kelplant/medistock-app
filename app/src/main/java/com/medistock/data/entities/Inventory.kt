package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a physical inventory count.
 * Used to compare actual stock vs theoretical stock.
 */
@Entity(tableName = "inventories")
data class Inventory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val siteId: Long,
    val countDate: Long,
    val countedQuantity: Double,
    val theoreticalQuantity: Double,
    val discrepancy: Double, // countedQuantity - theoreticalQuantity
    val reason: String = "", // Reason for discrepancy
    val countedBy: String = "", // Person who performed the count
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)
