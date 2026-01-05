package com.medistock.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Tracks which purchase batches were used for each sale item.
 * This enables true FIFO inventory management based on purchase price.
 */
@Entity(
    tableName = "sale_batch_allocations",
    foreignKeys = [
        ForeignKey(
            entity = SaleItem::class,
            parentColumns = ["id"],
            childColumns = ["saleItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PurchaseBatch::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("saleItemId"), Index("batchId")]
)
data class SaleBatchAllocation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val saleItemId: String,
    val batchId: String,
    val quantityAllocated: Double, // Quantity taken from this batch
    val purchasePriceAtAllocation: Double, // Purchase price of the batch at time of sale
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)
