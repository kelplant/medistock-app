package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.ProductTransfer
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductTransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transfer: ProductTransfer): Long

    @Update
    fun update(transfer: ProductTransfer)

    @Delete
    fun delete(transfer: ProductTransfer)

    @Query("SELECT * FROM product_transfers ORDER BY date DESC")
    fun getAll(): Flow<List<ProductTransfer>>

    @Query("SELECT * FROM product_transfers WHERE fromSiteId = :siteId OR toSiteId = :siteId ORDER BY date DESC")
    fun getTransfersForSite(siteId: Long): Flow<List<ProductTransfer>>

    @Query("SELECT * FROM product_transfers WHERE id = :id")
    fun getById(id: Long): Flow<ProductTransfer?>

    @Query("SELECT * FROM product_transfers WHERE productId = :productId ORDER BY date DESC")
    fun getTransfersForProduct(productId: Long): Flow<List<ProductTransfer>>
}
