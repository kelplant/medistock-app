package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.PurchaseBatch
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseBatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(batch: PurchaseBatch): Long

    @Update
    fun update(batch: PurchaseBatch)

    @Query("SELECT * FROM purchase_batches WHERE id = :batchId")
    fun getById(batchId: String): Flow<PurchaseBatch?>

    @Query("SELECT * FROM purchase_batches WHERE productId = :productId AND siteId = :siteId ORDER BY purchaseDate ASC")
    fun getBatchesForProduct(productId: String, siteId: String): Flow<List<PurchaseBatch>>

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
    fun getAvailableBatchesFIFO(productId: String, siteId: String): Flow<List<PurchaseBatch>>

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
    fun getBatchesExpiringSoon(productId: String, siteId: String, thresholdDate: Long): Flow<List<PurchaseBatch>>

    /**
     * Calculate average purchase price for a product.
     */
    @Query("""
        SELECT AVG(purchasePrice) FROM purchase_batches
        WHERE productId = :productId
        AND siteId = :siteId
        AND isExhausted = 0
    """)
    fun getAveragePurchasePrice(productId: String, siteId: String): Double?

    /**
     * Get total remaining quantity across all batches.
     */
    @Query("""
        SELECT SUM(remainingQuantity) FROM purchase_batches
        WHERE productId = :productId
        AND siteId = :siteId
        AND isExhausted = 0
    """)
    fun getTotalRemainingQuantity(productId: String, siteId: String): Double?

    @Query("DELETE FROM purchase_batches WHERE id = :batchId")
    fun deleteById(batchId: String)

    /**
     * Update remaining quantity and exhausted status for a batch.
     */
    @Query("""
        UPDATE purchase_batches
        SET remainingQuantity = :newQuantity,
            isExhausted = CASE WHEN :newQuantity <= 0 THEN 1 ELSE 0 END,
            updatedAt = :updatedAt
        WHERE id = :batchId
    """)
    fun updateRemainingQuantity(batchId: String, newQuantity: Double, updatedAt: Long): Int

    /**
     * Get available batches ordered by purchase date (FIFO) - non-Flow version for transactions.
     */
    @Query("""
        SELECT * FROM purchase_batches
        WHERE productId = :productId
        AND siteId = :siteId
        AND isExhausted = 0
        AND remainingQuantity > 0
        ORDER BY purchaseDate ASC
    """)
    fun getAvailableBatchesFIFOSync(productId: String, siteId: String): List<PurchaseBatch>
}
