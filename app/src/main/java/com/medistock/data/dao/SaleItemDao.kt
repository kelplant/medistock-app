package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.SaleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(saleItem: SaleItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(saleItems: List<SaleItem>)

    @Update
    fun update(saleItem: SaleItem)

    @Delete
    fun delete(saleItem: SaleItem)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getItemsForSale(saleId: String): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getItemsBySale(saleId: String): List<SaleItem>

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    fun deleteAllForSale(saleId: String)
}
