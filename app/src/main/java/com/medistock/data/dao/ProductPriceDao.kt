package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.ProductPrice
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductPriceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(price: ProductPrice): Long

    @Query("SELECT * FROM product_prices")
    fun getAll(): Flow<List<ProductPrice>>

    @Query("SELECT * FROM product_prices WHERE productId = :productId ORDER BY effectiveDate DESC LIMIT 1")
    fun getLatestPrice(productId: String): Flow<ProductPrice?>
}
