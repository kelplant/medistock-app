package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.ProductPrice
import com.medistock.data.entities.ProductSale

@Dao
interface ProductPriceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(price: ProductPrice): Long

    @Query("SELECT * FROM product_prices")
    suspend fun getAll(): List<ProductPrice>

    @Query("SELECT * FROM product_prices WHERE productId = :productId ORDER BY effectiveDate DESC LIMIT 1")
    suspend fun getLatestPrice(productId: Long): ProductPrice?
}