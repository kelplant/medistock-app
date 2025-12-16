package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Product
import com.medistock.data.entities.ProductWithCategory

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Query("SELECT * FROM products")
    suspend fun getAll(): List<Product>

    @Query("SELECT * FROM products WHERE siteId = :siteId")
    suspend fun getProductsForSite(siteId: Long): List<Product>

    @Query("""
        SELECT p.id, p.name, p.unit, p.categoryId, c.name as categoryName,
               p.marginType, p.marginValue, p.unitVolume, p.siteId,
               p.minStock, p.maxStock
        FROM products p
        LEFT JOIN categories c ON p.categoryId = c.id
    """)
    suspend fun getAllWithCategory(): List<ProductWithCategory>

    @Query("""
        SELECT p.id, p.name, p.unit, p.categoryId, c.name as categoryName,
               p.marginType, p.marginValue, p.unitVolume, p.siteId,
               p.minStock, p.maxStock
        FROM products p
        LEFT JOIN categories c ON p.categoryId = c.id
        WHERE p.siteId = :siteId
    """)
    suspend fun getProductsWithCategoryForSite(siteId: Long): List<ProductWithCategory>
}