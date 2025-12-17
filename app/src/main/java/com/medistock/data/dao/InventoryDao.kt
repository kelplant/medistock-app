package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.Inventory
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBlocking(inventory: Inventory): Long

    suspend fun insert(inventory: Inventory): Long {
        return insertBlocking(inventory)
    }

    @Update
    fun update(inventory: Inventory)

    @Query("SELECT * FROM inventories WHERE id = :inventoryId")
    fun getById(inventoryId: Long): Flow<Inventory?>

    @Query("SELECT * FROM inventories WHERE productId = :productId AND siteId = :siteId ORDER BY countDate DESC")
    fun getInventoriesForProduct(productId: Long, siteId: Long): Flow<List<Inventory>>

    @Query("SELECT * FROM inventories WHERE siteId = :siteId ORDER BY countDate DESC")
    fun getInventoriesForSite(siteId: Long): Flow<List<Inventory>>

    @Query("SELECT * FROM inventories ORDER BY countDate DESC LIMIT :limit")
    fun getRecentInventories(limit: Int = 50): Flow<List<Inventory>>

    /**
     * Get inventories with discrepancies (difference != 0).
     */
    @Query("""
        SELECT * FROM inventories
        WHERE siteId = :siteId
        AND discrepancy != 0
        ORDER BY countDate DESC
    """)
    fun getInventoriesWithDiscrepancies(siteId: Long): Flow<List<Inventory>>

    /**
     * Get total discrepancy value for a site.
     */
    @Query("""
        SELECT SUM(ABS(discrepancy)) FROM inventories
        WHERE siteId = :siteId
        AND countDate >= :startDate
    """)
    fun getTotalDiscrepancy(siteId: Long, startDate: Long): Double?

    @Query("DELETE FROM inventories WHERE id = :inventoryId")
    fun deleteById(inventoryId: Long)
}
