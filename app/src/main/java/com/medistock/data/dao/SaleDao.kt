package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Sale
import com.medistock.data.entities.SaleWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sale: Sale): Long

    @Update
    fun update(sale: Sale)

    @Delete
    fun delete(sale: Sale)

    @Query("SELECT * FROM sales WHERE id = :id")
    fun getById(id: Long): Flow<Sale?>

    @Query("SELECT * FROM sales WHERE siteId = :siteId ORDER BY date DESC")
    fun getAllForSite(siteId: Long): Flow<List<Sale>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    fun getSaleWithItems(id: Long): Flow<SaleWithItems?>

    @Transaction
    @Query("SELECT * FROM sales WHERE siteId = :siteId ORDER BY date DESC")
    fun getAllWithItemsForSite(siteId: Long): Flow<List<SaleWithItems>>

    @Query("DELETE FROM sales WHERE id = :id")
    fun deleteById(id: Long)
}
