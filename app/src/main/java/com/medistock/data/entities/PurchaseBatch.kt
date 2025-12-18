package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a purchase batch for FIFO (First In, First Out) inventory management.
 * Each batch tracks a specific purchase with its price and remaining quantity.
 */
@Entity(tableName = "purchase_batches")
data class PurchaseBatch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val siteId: Long,
    val batchNumber: String,
    val purchaseDate: Long,
    val initialQuantity: Double,
    val remainingQuantity: Double,
    val purchasePrice: Double, // Prix d'achat unitaire
    val supplierName: String = "",
    val expiryDate: Long? = null, // Date d'expiration pour médicaments
    val isExhausted: Boolean = false, // true when remainingQuantity <= 0
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val updatedBy: String = ""
)
