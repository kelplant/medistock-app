package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Inventory

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inventory: Inventory): Long

    @Update
    suspend fun update(inventory: Inventory)

    @Query("SELECT * FROM inventories WHERE id = :inventoryId")
    suspend fun getById(inventoryId: Long): Inventory?

    @Query("SELECT * FROM inventories WHERE productId = :productId AND siteId = :siteId ORDER BY countDate DESC")
    suspend fun getInventoriesForProduct(productId: Long, siteId: Long): List<Inventory>

    @Query("SELECT * FROM inventories WHERE siteId = :siteId ORDER BY countDate DESC")
    suspend fun getInventoriesForSite(siteId: Long): List<Inventory>

    @Query("SELECT * FROM inventories ORDER BY countDate DESC LIMIT :limit")
    suspend fun getRecentInventories(limit: Int = 50): List<Inventory>

    /**
     * Get inventories with discrepancies (difference != 0).
     */
    @Query("""
        SELECT * FROM inventories
        WHERE siteId = :siteId
        AND discrepancy != 0
        ORDER BY countDate DESC
    """)
    suspend fun getInventoriesWithDiscrepancies(siteId: Long): List<Inventory>

    /**
     * Get total discrepancy value for a site.
     */
    @Query("""
        SELECT SUM(ABS(discrepancy)) FROM inventories
        WHERE siteId = :siteId
        AND countDate >= :startDate
    """)
    suspend fun getTotalDiscrepancy(siteId: Long, startDate: Long): Double?

    @Query("DELETE FROM inventories WHERE id = :inventoryId")
    suspend fun deleteById(inventoryId: Long)
}
