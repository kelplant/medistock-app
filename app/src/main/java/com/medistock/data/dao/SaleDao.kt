package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Sale
import com.medistock.data.entities.SaleWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales")
    fun getAll(): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sale: Sale): Long

    @Update
    fun update(sale: Sale)

    @Delete
    fun delete(sale: Sale)

    @Query("SELECT * FROM sales WHERE id = :id")
    fun getById(id: String): Flow<Sale?>

    @Query("SELECT * FROM sales WHERE siteId = :siteId ORDER BY date DESC")
    fun getAllForSite(siteId: String): Flow<List<Sale>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    fun getSaleWithItems(id: String): Flow<SaleWithItems?>

    @Transaction
    @Query("SELECT * FROM sales WHERE siteId = :siteId ORDER BY date DESC")
    fun getAllWithItemsForSite(siteId: String): Flow<List<SaleWithItems>>

    @Query("DELETE FROM sales WHERE id = :id")
    fun deleteById(id: String)
}
