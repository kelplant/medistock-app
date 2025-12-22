package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.ProductSale
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductSaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sale: ProductSale): Long

    @Query("SELECT * FROM product_sales")
    fun getAll(): Flow<List<ProductSale>>

    @Query("SELECT * FROM product_sales WHERE productId = :productId AND siteId = :siteId ORDER BY date DESC")
    fun getSalesForProduct(productId: String, siteId: String): Flow<List<ProductSale>>

    @Query("SELECT * FROM product_sales WHERE siteId = :siteId ORDER BY date DESC")
    fun getAllForSite(siteId: String): Flow<List<ProductSale>>
}
