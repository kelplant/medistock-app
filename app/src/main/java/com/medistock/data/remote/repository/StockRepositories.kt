package com.medistock.data.remote.repository

import com.medistock.data.remote.dto.*

class PurchaseBatchSupabaseRepository : BaseSupabaseRepository("purchase_batches") {
    suspend fun getAllBatches(): List<PurchaseBatchDto> = getAll()
    suspend fun getBatchById(id: Long): PurchaseBatchDto? = getById(id)
    suspend fun createBatch(batch: PurchaseBatchDto): PurchaseBatchDto = create(batch)
    suspend fun updateBatch(id: Long, batch: PurchaseBatchDto): PurchaseBatchDto = update(id, batch)
    suspend fun deleteBatch(id: Long) = delete(id)

    suspend fun getBatchesByProduct(productId: Long): List<PurchaseBatchDto> {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
            order("purchase_date", ascending = true)
        }.decodeList()
    }

    suspend fun getActiveBatchesByProduct(productId: Long): List<PurchaseBatchDto> {
        return supabase.from(tableName).select {
            filter {
                eq("product_id", productId)
                eq("is_exhausted", false)
            }
            order("purchase_date", ascending = true)
        }.decodeList()
    }

    suspend fun getBatchesBySite(siteId: Long): List<PurchaseBatchDto> {
        return supabase.from(tableName).select {
            filter { eq("site_id", siteId) }
        }.decodeList()
    }

    suspend fun getExpiringBatches(daysThreshold: Int = 30): List<PurchaseBatchDto> {
        val thresholdDate = System.currentTimeMillis() + (daysThreshold * 24 * 60 * 60 * 1000L)
        return supabase.from(tableName).select {
            filter {
                eq("is_exhausted", false)
                lte("expiry_date", thresholdDate)
            }
            order("expiry_date", ascending = true)
        }.decodeList()
    }

    suspend fun getBatchesBySupplier(supplierName: String): List<PurchaseBatchDto> {
        return supabase.from(tableName).select {
            filter { ilike("supplier_name", "%$supplierName%") }
        }.decodeList()
    }
}

class StockMovementSupabaseRepository : BaseSupabaseRepository("stock_movements") {
    suspend fun getAllMovements(): List<StockMovementDto> = getAll()
    suspend fun getMovementById(id: Long): StockMovementDto? = getById(id)
    suspend fun createMovement(movement: StockMovementDto): StockMovementDto = create(movement)

    suspend fun getMovementsByProduct(productId: Long): List<StockMovementDto> {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
            order("date", ascending = false)
        }.decodeList()
    }

    suspend fun getMovementsBySite(siteId: Long): List<StockMovementDto> {
        return supabase.from(tableName).select {
            filter { eq("site_id", siteId) }
            order("date", ascending = false)
        }.decodeList()
    }

    suspend fun getMovementsByType(type: String, siteId: Long? = null): List<StockMovementDto> {
        return supabase.from(tableName).select {
            filter {
                eq("type", type)
                if (siteId != null) {
                    eq("site_id", siteId)
                }
            }
            order("date", ascending = false)
        }.decodeList()
    }

    suspend fun getMovementsByDateRange(
        startDate: Long,
        endDate: Long,
        siteId: Long? = null
    ): List<StockMovementDto> {
        return supabase.from(tableName).select {
            filter {
                gte("date", startDate)
                lte("date", endDate)
                if (siteId != null) {
                    eq("site_id", siteId)
                }
            }
            order("date", ascending = false)
        }.decodeList()
    }
}

class InventorySupabaseRepository : BaseSupabaseRepository("inventories") {
    suspend fun getAllInventories(): List<InventoryDto> = getAll()
    suspend fun getInventoryById(id: Long): InventoryDto? = getById(id)
    suspend fun createInventory(inventory: InventoryDto): InventoryDto = create(inventory)
    suspend fun updateInventory(id: Long, inventory: InventoryDto): InventoryDto = update(id, inventory)
    suspend fun deleteInventory(id: Long) = delete(id)

    suspend fun getInventoriesByProduct(productId: Long): List<InventoryDto> {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
            order("count_date", ascending = false)
        }.decodeList()
    }

    suspend fun getInventoriesBySite(siteId: Long): List<InventoryDto> {
        return supabase.from(tableName).select {
            filter { eq("site_id", siteId) }
            order("count_date", ascending = false)
        }.decodeList()
    }

    suspend fun getInventoriesWithDiscrepancy(siteId: Long? = null): List<InventoryDto> {
        return supabase.from(tableName).select {
            filter {
                neq("discrepancy", 0.0)
                if (siteId != null) {
                    eq("site_id", siteId)
                }
            }
            order("count_date", ascending = false)
        }.decodeList()
    }

    suspend fun getLatestInventory(productId: Long, siteId: Long): InventoryDto? {
        return supabase.from(tableName).select {
            filter {
                eq("product_id", productId)
                eq("site_id", siteId)
            }
            order("count_date", ascending = false)
            limit(1)
        }.decodeList<InventoryDto>().firstOrNull()
    }
}

class ProductTransferSupabaseRepository : BaseSupabaseRepository("product_transfers") {
    suspend fun getAllTransfers(): List<ProductTransferDto> = getAll()
    suspend fun getTransferById(id: Long): ProductTransferDto? = getById(id)
    suspend fun createTransfer(transfer: ProductTransferDto): ProductTransferDto = create(transfer)
    suspend fun updateTransfer(id: Long, transfer: ProductTransferDto): ProductTransferDto = update(id, transfer)
    suspend fun deleteTransfer(id: Long) = delete(id)

    suspend fun getTransfersFromSite(siteId: Long): List<ProductTransferDto> {
        return supabase.from(tableName).select {
            filter { eq("from_site_id", siteId) }
            order("date", ascending = false)
        }.decodeList()
    }

    suspend fun getTransfersToSite(siteId: Long): List<ProductTransferDto> {
        return supabase.from(tableName).select {
            filter { eq("to_site_id", siteId) }
            order("date", ascending = false)
        }.decodeList()
    }

    suspend fun getTransfersByProduct(productId: Long): List<ProductTransferDto> {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
            order("date", ascending = false)
        }.decodeList()
    }

    suspend fun getTransfersBetweenSites(fromSiteId: Long, toSiteId: Long): List<ProductTransferDto> {
        return supabase.from(tableName).select {
            filter {
                eq("from_site_id", fromSiteId)
                eq("to_site_id", toSiteId)
            }
            order("date", ascending = false)
        }.decodeList()
    }
}
