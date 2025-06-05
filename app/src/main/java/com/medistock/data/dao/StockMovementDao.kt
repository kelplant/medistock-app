package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.StockMovement

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovement): Long

    @Query("SELECT * FROM stock_movements")
    suspend fun getAll(): List<StockMovement>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId AND siteId = :siteId ORDER BY date DESC")
    suspend fun getMovementsForProduct(productId: Long, siteId: Long): List<StockMovement>

    @Query("SELECT * FROM stock_movements WHERE siteId = :siteId")
    suspend fun getAllForSite(siteId: Long): List<StockMovement>
}