package com.medistock.data.remote.repository

import com.medistock.data.remote.dto.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class SaleSupabaseRepository : BaseSupabaseRepository("sales") {
    suspend fun getAllSales(): List<SaleDto> = getAll()
    suspend fun getSaleById(id: String): SaleDto? = getById(id)
    suspend fun createSale(sale: SaleDto): SaleDto = create(sale)
    suspend fun updateSale(id: String, sale: SaleDto): SaleDto = update(id, sale)
    suspend fun deleteSale(id: String) = delete(id)

    suspend fun getSalesBySite(siteId: String): List<SaleDto> {
        return supabase.from(tableName).select {
            filter { eq("site_id", siteId) }
            order(column = "date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getSalesByCustomer(customerId: String): List<SaleDto> {
        return supabase.from(tableName).select {
            filter { eq("customer_id", customerId) }
            order(column = "date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getSalesByDateRange(
        startDate: Long,
        endDate: Long,
        siteId: String? = null
    ): List<SaleDto> {
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

    suspend fun searchByCustomerName(customerName: String): List<SaleDto> {
        return supabase.from(tableName).select {
            filter { ilike("customer_name", "%$customerName%") }
            order(column = "date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getTodaySales(siteId: String? = null): List<SaleDto> {
        val today = System.currentTimeMillis()
        val startOfDay = today - (today % (24 * 60 * 60 * 1000))
        return getSalesByDateRange(startOfDay, today, siteId)
    }
}

class SaleItemSupabaseRepository : BaseSupabaseRepository("sale_items") {
    suspend fun getAllSaleItems(): List<SaleItemDto> = getAll()
    suspend fun getSaleItemById(id: String): SaleItemDto? = getById(id)
    suspend fun createSaleItem(saleItem: SaleItemDto): SaleItemDto = create(saleItem)
    suspend fun updateSaleItem(id: String, saleItem: SaleItemDto): SaleItemDto = update(id, saleItem)
    suspend fun deleteSaleItem(id: String) = delete(id)

    suspend fun getSaleItemsBySale(saleId: String): List<SaleItemDto> {
        return supabase.from(tableName).select {
            filter { eq("sale_id", saleId) }
        }.decodeList()
    }

    suspend fun getSaleItemsByProduct(productId: String): List<SaleItemDto> {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
        }.decodeList()
    }

    suspend fun deleteAllSaleItems(saleId: String) {
        supabase.from(tableName).delete {
            filter { eq("sale_id", saleId) }
        }
    }
}

class SaleBatchAllocationSupabaseRepository : BaseSupabaseRepository("sale_batch_allocations") {
    suspend fun getAllAllocations(): List<SaleBatchAllocationDto> = getAll()
    suspend fun getAllocationById(id: String): SaleBatchAllocationDto? = getById(id)
    suspend fun createAllocation(allocation: SaleBatchAllocationDto): SaleBatchAllocationDto = create(allocation)

    suspend fun getAllocationsBySaleItem(saleItemId: String): List<SaleBatchAllocationDto> {
        return supabase.from(tableName).select {
            filter { eq("sale_item_id", saleItemId) }
        }.decodeList()
    }

    suspend fun getAllocationsByBatch(batchId: String): List<SaleBatchAllocationDto> {
        return supabase.from(tableName).select {
            filter { eq("batch_id", batchId) }
            order(column = "created_at", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun deleteAllocationsBySaleItem(saleItemId: String) {
        supabase.from(tableName).delete {
            filter { eq("sale_item_id", saleItemId) }
        }
    }
}

