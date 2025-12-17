package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.ProductPrice
import com.medistock.data.entities.ProductSale
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductPriceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBlocking(price: ProductPrice): Long

    suspend fun insert(price: ProductPrice): Long {
        return insertBlocking(price)
    }

    @Query("SELECT * FROM product_prices")
    fun getAll(): Flow<List<ProductPrice>>

    @Query("SELECT * FROM product_prices WHERE productId = :productId ORDER BY effectiveDate DESC LIMIT 1")
    fun getLatestPrice(productId: Long): Flow<ProductPrice?>
}