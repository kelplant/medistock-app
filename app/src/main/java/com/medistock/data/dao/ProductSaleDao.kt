package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.ProductSale
import com.medistock.data.entities.StockMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductSaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBlocking(sale: ProductSale): Long

    suspend fun insert(sale: ProductSale): Long {
        return insertBlocking(sale)
    }

    @Query("SELECT * FROM product_sales")
    fun getAll(): Flow<List<ProductSale>>

    @Query("SELECT * FROM product_sales WHERE productId = :productId AND siteId = :siteId ORDER BY date DESC")
    fun getSalesForProduct(productId: Long, siteId: Long): Flow<List<ProductSale>>

    @Query("SELECT * FROM product_sales WHERE siteId = :siteId ORDER BY date DESC")
    fun getAllForSite(siteId: Long): Flow<List<ProductSale>>
}