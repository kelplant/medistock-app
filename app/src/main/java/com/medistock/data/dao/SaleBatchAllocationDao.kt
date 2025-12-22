package com.medistock.data.dao

import androidx.room.*
import com.medistock.data.entities.SaleBatchAllocation
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleBatchAllocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(allocation: SaleBatchAllocation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(allocations: List<SaleBatchAllocation>)

    @Query("SELECT * FROM sale_batch_allocations WHERE saleItemId = :saleItemId")
    fun getAllocationsForSaleItem(saleItemId: String): Flow<List<SaleBatchAllocation>>

    @Query("SELECT * FROM sale_batch_allocations WHERE batchId = :batchId")
    fun getAllocationsForBatch(batchId: String): Flow<List<SaleBatchAllocation>>

    @Query("""
        SELECT SUM(quantityAllocated) FROM sale_batch_allocations
        WHERE batchId = :batchId
    """)
    fun getTotalAllocatedForBatch(batchId: String): Double?

    @Query("DELETE FROM sale_batch_allocations WHERE saleItemId = :saleItemId")
    fun deleteAllForSaleItem(saleItemId: String)
}
