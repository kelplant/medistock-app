package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.PurchaseBatch

@Dao
interface PurchaseBatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: PurchaseBatch): Long

    @Update
    suspend fun update(batch: PurchaseBatch)

    @Query("SELECT * FROM purchase_batches WHERE id = :batchId")
    suspend fun getById(batchId: Long): PurchaseBatch?

    @Query("SELECT * FROM purchase_batches WHERE productId = :productId AND siteId = :siteId ORDER BY purchaseDate ASC")
    suspend fun getBatchesForProduct(productId: Long, siteId: Long): List<PurchaseBatch>

    /**
     * Get available batches (not exhausted) for FIFO consumption.
     * Ordered by purchase date (oldest first).
     */
    @Query("""
        SELECT * FROM purchase_batches
        WHERE productId = :productId
        AND siteId = :siteId
        AND isExhausted = 0
        AND remainingQuantity > 0
        ORDER BY purchaseDate ASC
    """)
    suspend fun getAvailableBatchesFIFO(productId: Long, siteId: Long): List<PurchaseBatch>

    /**
     * Get batches expiring soon (within days threshold).
     */
    @Query("""
        SELECT * FROM purchase_batches
        WHERE productId = :productId
        AND siteId = :siteId
        AND isExhausted = 0
        AND expiryDate IS NOT NULL
        AND expiryDate <= :thresholdDate
        ORDER BY expiryDate ASC
    """)
    suspend fun getBatchesExpiringSoon(productId: Long, siteId: Long, thresholdDate: Long): List<PurchaseBatch>

    /**
     * Calculate average purchase price for a product.
     */
    @Query("""
        SELECT AVG(purchasePrice) FROM purchase_batches
        WHERE productId = :productId
        AND siteId = :siteId
        AND isExhausted = 0
    """)
    suspend fun getAveragePurchasePrice(productId: Long, siteId: Long): Double?

    /**
     * Get total remaining quantity across all batches.
     */
    @Query("""
        SELECT SUM(remainingQuantity) FROM purchase_batches
        WHERE productId = :productId
        AND siteId = :siteId
        AND isExhausted = 0
    """)
    suspend fun getTotalRemainingQuantity(productId: Long, siteId: Long): Double?

    @Query("DELETE FROM purchase_batches WHERE id = :batchId")
    suspend fun deleteById(batchId: Long)
}
