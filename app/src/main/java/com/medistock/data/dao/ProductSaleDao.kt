package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.ProductSale
import com.medistock.data.entities.StockMovement

@Dao
interface ProductSaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: ProductSale): Long

    @Query("SELECT * FROM product_sales")
    suspend fun getAll(): List<ProductSale>

    @Query("SELECT * FROM product_sales WHERE productId = :productId AND siteId = :siteId ORDER BY date DESC")
    suspend fun getSalesForProduct(productId: Long, siteId: Long): List<ProductSale>

    @Query("SELECT * FROM product_sales WHERE siteId = :siteId ORDER BY date DESC")
    suspend fun getAllForSite(siteId: Long): List<ProductSale>
}