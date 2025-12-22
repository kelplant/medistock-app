package com.medistock.data.remote.repository

import com.medistock.data.remote.dto.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class PurchaseBatchSupabaseRepository : BaseSupabaseRepository("purchase_batches") {
    suspend fun getAllBatches(): List<PurchaseBatchDto> = getAll()
    suspend fun getBatchById(id: String): PurchaseBatchDto? = getById(id)
    suspend fun createBatch(batch: PurchaseBatchDto): PurchaseBatchDto = create(batch)
    suspend fun updateBatch(id: String, batch: PurchaseBatchDto): PurchaseBatchDto = update(id, batch)
    suspend fun deleteBatch(id: String) = delete(id)

    suspend fun getBatchesByProduct(productId: String): List<PurchaseBatchDto> {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
            order(column = "purchase_date", order = Order.ASCENDING)
        }.decodeList()
    }

    suspend fun getActiveBatchesByProduct(productId: String): List<PurchaseBatchDto> {
        return supabase.from(tableName).select {
            filter {
                eq("product_id", productId)
                eq("is_exhausted", false)
            }
            order(column = "purchase_date", order = Order.ASCENDING)
        }.decodeList()
    }

    suspend fun getBatchesBySite(siteId: String): List<PurchaseBatchDto> {
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
            order(column = "expiry_date", order = Order.ASCENDING)
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
    suspend fun getMovementById(id: String): StockMovementDto? = getById(id)
    suspend fun createMovement(movement: StockMovementDto): StockMovementDto = create(movement)

    suspend fun getMovementsByProduct(productId: String): List<StockMovementDto> {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
            order(column = "date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getMovementsBySite(siteId: String): List<StockMovementDto> {
        return supabase.from(tableName).select {
            filter { eq("site_id", siteId) }
            order(column = "date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getMovementsByType(type: String, siteId: String? = null): List<StockMovementDto> {
        return supabase.from(tableName).select {
            filter {
                eq("type", type)
                if (siteId != null) {
                    eq("site_id", siteId)
                }
            }
            order(column = "date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getMovementsByDateRange(
        startDate: Long,
        endDate: Long,
        siteId: String? = null
    ): List<StockMovementDto> {
        return supabase.from(tableName).select {
            filter {
                gte("date", startDate)
                lte("date", endDate)
                if (siteId != null) {
                    eq("site_id", siteId)
                }
            }
            order(column = "date", order = Order.DESCENDING)
        }.decodeList()
    }
}

class InventorySupabaseRepository : BaseSupabaseRepository("inventories") {
    suspend fun getAllInventories(): List<InventoryDto> = getAll()
    suspend fun getInventoryById(id: String): InventoryDto? = getById(id)
    suspend fun createInventory(inventory: InventoryDto): InventoryDto = create(inventory)
    suspend fun updateInventory(id: String, inventory: InventoryDto): InventoryDto = update(id, inventory)
    suspend fun deleteInventory(id: String) = delete(id)

    suspend fun getInventoriesByProduct(productId: String): List<InventoryDto> {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
            order(column = "count_date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getInventoriesBySite(siteId: String): List<InventoryDto> {
        return supabase.from(tableName).select {
            filter { eq("site_id", siteId) }
            order(column = "count_date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getInventoriesWithDiscrepancy(siteId: String? = null): List<InventoryDto> {
        return supabase.from(tableName).select {
            filter {
                neq("discrepancy", 0.0)
                if (siteId != null) {
                    eq("site_id", siteId)
                }
            }
            order(column = "count_date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getLatestInventory(productId: String, siteId: String): InventoryDto? {
        return supabase.from(tableName).select {
            filter {
                eq("product_id", productId)
                eq("site_id", siteId)
            }
            order(column = "count_date", order = Order.DESCENDING)
            limit(1)
        }.decodeList<InventoryDto>().firstOrNull()
    }
}

class ProductTransferSupabaseRepository : BaseSupabaseRepository("product_transfers") {
    suspend fun getAllTransfers(): List<ProductTransferDto> = getAll()
    suspend fun getTransferById(id: String): ProductTransferDto? = getById(id)
    suspend fun createTransfer(transfer: ProductTransferDto): ProductTransferDto = create(transfer)
    suspend fun updateTransfer(id: String, transfer: ProductTransferDto): ProductTransferDto = update(id, transfer)
    suspend fun deleteTransfer(id: String) = delete(id)

    suspend fun getTransfersFromSite(siteId: String): List<ProductTransferDto> {
        return supabase.from(tableName).select {
            filter { eq("from_site_id", siteId) }
            order(column = "date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getTransfersToSite(siteId: String): List<ProductTransferDto> {
        return supabase.from(tableName).select {
            filter { eq("to_site_id", siteId) }
            order(column = "date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getTransfersByProduct(productId: String): List<ProductTransferDto> {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
            order(column = "date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getTransfersBetweenSites(fromSiteId: String, toSiteId: String): List<ProductTransferDto> {
        return supabase.from(tableName).select {
            filter {
                eq("from_site_id", fromSiteId)
                eq("to_site_id", toSiteId)
            }
            order(column = "date", order = Order.DESCENDING)
        }.decodeList()
    }
}
