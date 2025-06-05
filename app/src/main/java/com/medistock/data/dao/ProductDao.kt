package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Product

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Query("SELECT * FROM products")
    suspend fun getAll(): List<Product>

    @Query("SELECT * FROM products WHERE siteId = :siteId")
    suspend fun getProductsForSite(siteId: Long): List<Product>
}