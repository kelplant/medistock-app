package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.SaleBatchAllocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaleBatchAllocationRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getBySaleItem(saleItemId: String): List<SaleBatchAllocation> = withContext(Dispatchers.Default) {
        queries.getSaleBatchAllocationsBySaleItem(saleItemId).executeAsList().map { it.toModel() }
    }

    suspend fun insert(allocation: SaleBatchAllocation) = withContext(Dispatchers.Default) {
        queries.insertSaleBatchAllocation(
            id = allocation.id,
            sale_item_id = allocation.saleItemId,
            batch_id = allocation.batchId,
            quantity_allocated = allocation.quantityAllocated,
            purchase_price_at_allocation = allocation.purchasePriceAtAllocation,
            created_at = allocation.createdAt,
            created_by = allocation.createdBy
        )
    }

    suspend fun deleteForSaleItem(saleItemId: String) = withContext(Dispatchers.Default) {
        queries.deleteSaleBatchAllocationsBySaleItem(saleItemId)
    }

    private fun com.medistock.shared.db.Sale_batch_allocations.toModel(): SaleBatchAllocation {
        return SaleBatchAllocation(
            id = id,
            saleItemId = sale_item_id,
            batchId = batch_id,
            quantityAllocated = quantity_allocated,
            purchasePriceAtAllocation = purchase_price_at_allocation,
            createdAt = created_at,
            createdBy = created_by
        )
    }
}
