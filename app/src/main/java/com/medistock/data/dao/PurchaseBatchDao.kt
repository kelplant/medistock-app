package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.PurchaseBatch
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseBatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBlocking(batch: PurchaseBatch): Long

    suspend fun insert(batch: PurchaseBatch): Long {
        return insertBlocking(batch)
    }

    @Update
    suspend fun update(batch: PurchaseBatch)

    @Query("SELECT * FROM purchase_batches WHERE id = :batchId")
    suspend fun getById(batchId: Long): PurchaseBatch?

    @Query("SELECT * FROM purchase_batches WHERE productId = :productId AND siteId = :siteId ORDER BY purchaseDate ASC")
    fun getBatchesForProduct(productId: Long, siteId: Long): Flow<List<PurchaseBatch>>

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
    fun getAvailableBatchesFIFO(productId: Long, siteId: Long): Flow<List<PurchaseBatch>>

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
    fun getBatchesExpiringSoon(productId: Long, siteId: Long, thresholdDate: Long): Flow<List<PurchaseBatch>>

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
