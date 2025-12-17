package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Product
import com.medistock.data.entities.ProductWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(product: Product): Long

    @Update
    fun update(product: Product)

    @Delete
    fun delete(product: Product)

    @Query("SELECT * FROM products")
    fun getAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    fun getById(productId: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE siteId = :siteId")
    fun getProductsForSite(siteId: Long): Flow<List<Product>>

    @Query("""
        SELECT p.id, p.name, p.unit, p.categoryId, c.name as categoryName,
               p.marginType, p.marginValue, p.unitVolume, p.siteId,
               p.minStock, p.maxStock
        FROM products p
        LEFT JOIN categories c ON p.categoryId = c.id
    """)
    fun getAllWithCategory(): Flow<List<ProductWithCategory>>

    @Query("""
        SELECT p.id, p.name, p.unit, p.categoryId, c.name as categoryName,
               p.marginType, p.marginValue, p.unitVolume, p.siteId,
               p.minStock, p.maxStock
        FROM products p
        LEFT JOIN categories c ON p.categoryId = c.id
        WHERE p.siteId = :siteId
    """)
    fun getProductsWithCategoryForSite(siteId: Long): Flow<List<ProductWithCategory>>
}
