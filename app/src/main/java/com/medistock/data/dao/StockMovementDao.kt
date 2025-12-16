package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.CurrentStock
import com.medistock.data.entities.StockMovement

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovement): Long

    @Query("SELECT * FROM stock_movements")
    suspend fun getAll(): List<StockMovement>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId AND siteId = :siteId ORDER BY date DESC")
    suspend fun getMovementsForProduct(productId: Long, siteId: Long): List<StockMovement>

    @Query("SELECT * FROM stock_movements WHERE siteId = :siteId")
    suspend fun getAllForSite(siteId: Long): List<StockMovement>

    /**
     * Calculate current stock for all products at a specific site.
     * Stock = SUM(entries) - SUM(exits)
     */
    @Query("""
        SELECT
            p.id as productId,
            p.name as productName,
            p.unit as unit,
            COALESCE(c.name, '') as categoryName,
            s.id as siteId,
            s.name as siteName,
            COALESCE(
                SUM(CASE WHEN sm.type = 'in' THEN sm.quantity ELSE 0 END) -
                SUM(CASE WHEN sm.type = 'out' THEN sm.quantity ELSE 0 END),
                0
            ) as quantityOnHand,
            p.minStock as minStock,
            p.maxStock as maxStock
        FROM products p
        LEFT JOIN categories c ON p.categoryId = c.id
        CROSS JOIN sites s
        LEFT JOIN stock_movements sm ON sm.productId = p.id AND sm.siteId = s.id
        WHERE s.id = :siteId
        GROUP BY p.id, s.id
        ORDER BY p.name
    """)
    suspend fun getCurrentStockForSite(siteId: Long): List<CurrentStock>

    /**
     * Calculate current stock for all products across all sites.
     */
    @Query("""
        SELECT
            p.id as productId,
            p.name as productName,
            p.unit as unit,
            COALESCE(c.name, '') as categoryName,
            s.id as siteId,
            s.name as siteName,
            COALESCE(
                SUM(CASE WHEN sm.type = 'in' THEN sm.quantity ELSE 0 END) -
                SUM(CASE WHEN sm.type = 'out' THEN sm.quantity ELSE 0 END),
                0
            ) as quantityOnHand,
            p.minStock as minStock,
            p.maxStock as maxStock
        FROM products p
        LEFT JOIN categories c ON p.categoryId = c.id
        CROSS JOIN sites s
        LEFT JOIN stock_movements sm ON sm.productId = p.id AND sm.siteId = s.id
        GROUP BY p.id, s.id
        ORDER BY s.name, p.name
    """)
    suspend fun getCurrentStockAllSites(): List<CurrentStock>

    /**
     * Calculate total stock for a specific product across all sites.
     */
    @Query("""
        SELECT
            p.id as productId,
            p.name as productName,
            p.unit as unit,
            COALESCE(c.name, '') as categoryName,
            0 as siteId,
            'Total' as siteName,
            COALESCE(
                SUM(CASE WHEN sm.type = 'in' THEN sm.quantity ELSE 0 END) -
                SUM(CASE WHEN sm.type = 'out' THEN sm.quantity ELSE 0 END),
                0
            ) as quantityOnHand
        FROM products p
        LEFT JOIN categories c ON p.categoryId = c.id
        LEFT JOIN stock_movements sm ON sm.productId = p.id
        WHERE p.id = :productId
        GROUP BY p.id
    """)
    suspend fun getTotalStockForProduct(productId: Long): CurrentStock?
}