package com.medistock.data.remote.repository


import io.github.jan.supabase.postgrest.from

import com.medistock.data.remote.dto.*

/**
 * Repository pour les ventes
 */
class SaleSupabaseRepository : BaseSupabaseRepository("sales") {

    suspend fun getAllSales(): List<SaleDto> = getAll()

    suspend fun getSaleById(id: Long): SaleDto? = getById(id)

    suspend fun createSale(sale: SaleDto): SaleDto = create(sale)

    suspend fun updateSale(id: Long, sale: SaleDto): SaleDto = update(id, sale)

    suspend fun deleteSale(id: Long) = delete(id)

    /**
     * Récupère les ventes d'un site
     */
    suspend fun getSalesBySite(siteId: Long): List<SaleDto> {
        return getWithFilter {
            eq("site_id", siteId)
            order("date", ascending = false)
        }
    }

    /**
     * Récupère les ventes d'un client
     */
    suspend fun getSalesByCustomer(customerId: Long): List<SaleDto> {
        return getWithFilter {
            eq("customer_id", customerId)
            order("date", ascending = false)
        }
    }

    /**
     * Récupère les ventes sur une période
     */
    suspend fun getSalesByDateRange(
        startDate: Long,
        endDate: Long,
        siteId: Long? = null
    ): List<SaleDto> {
        return getWithFilter {
            gte("date", startDate)
            lte("date", endDate)
            if (siteId != null) {
                eq("site_id", siteId)
            }
            order("date", ascending = false)
        }
    }

    /**
     * Recherche de ventes par nom de client
     */
    suspend fun searchByCustomerName(customerName: String): List<SaleDto> {
        return getWithFilter {
            ilike("customer_name", "%$customerName%")
            order("date", ascending = false)
        }
    }

    /**
     * Récupère les ventes du jour
     */
    suspend fun getTodaySales(siteId: Long? = null): List<SaleDto> {
        val today = System.currentTimeMillis()
        val startOfDay = today - (today % (24 * 60 * 60 * 1000))
        return getSalesByDateRange(startOfDay, today, siteId)
    }
}

/**
 * Repository pour les lignes de vente
 */
class SaleItemSupabaseRepository : BaseSupabaseRepository("sale_items") {

    suspend fun getAllSaleItems(): List<SaleItemDto> = getAll()

    suspend fun getSaleItemById(id: Long): SaleItemDto? = getById(id)

    suspend fun createSaleItem(saleItem: SaleItemDto): SaleItemDto = create(saleItem)

    suspend fun updateSaleItem(id: Long, saleItem: SaleItemDto): SaleItemDto = update(id, saleItem)

    suspend fun deleteSaleItem(id: Long) = delete(id)

    /**
     * Récupère les lignes d'une vente
     */
    suspend fun getSaleItemsBySale(saleId: Long): List<SaleItemDto> {
        return getWithFilter {
            eq("sale_id", saleId)
        }
    }

    /**
     * Récupère les ventes d'un produit
     */
    suspend fun getSaleItemsByProduct(productId: Long): List<SaleItemDto> {
        return getWithFilter {
            eq("product_id", productId)
        }
    }

    /**
     * Supprime toutes les lignes d'une vente
     */
    suspend fun deleteAllSaleItems(saleId: Long) {
        supabase.from("sale_items").delete {
            filter {
                eq("sale_id", saleId)
            }
        }
    }
}

/**
 * Repository pour les allocations FIFO des batches aux ventes
 */
class SaleBatchAllocationSupabaseRepository : BaseSupabaseRepository("sale_batch_allocations") {

    suspend fun getAllAllocations(): List<SaleBatchAllocationDto> = getAll()

    suspend fun getAllocationById(id: Long): SaleBatchAllocationDto? = getById(id)

    suspend fun createAllocation(allocation: SaleBatchAllocationDto): SaleBatchAllocationDto = create(allocation)

    /**
     * Récupère les allocations d'une ligne de vente
     */
    suspend fun getAllocationsBySaleItem(saleItemId: Long): List<SaleBatchAllocationDto> {
        return getWithFilter {
            eq("sale_item_id", saleItemId)
        }
    }

    /**
     * Récupère les allocations d'un batch
     */
    suspend fun getAllocationsByBatch(batchId: Long): List<SaleBatchAllocationDto> {
        return getWithFilter {
            eq("batch_id", batchId)
            order("created_at", ascending = false)
        }
    }

    /**
     * Supprime toutes les allocations d'une ligne de vente
     */
    suspend fun deleteAllocationsBySaleItem(saleItemId: Long) {
        supabase.from("sale_batch_allocations").delete {
            filter {
                eq("sale_item_id", saleItemId)
            }
        }
    }
}

/**
 * Repository pour les ventes produits (ancien système)
 */
class ProductSaleSupabaseRepository : BaseSupabaseRepository("product_sales") {

    suspend fun getAllProductSales(): List<ProductSaleDto> = getAll()

    suspend fun getProductSaleById(id: Long): ProductSaleDto? = getById(id)

    suspend fun createProductSale(productSale: ProductSaleDto): ProductSaleDto = create(productSale)

    /**
     * Récupère les ventes d'un produit
     */
    suspend fun getProductSalesByProduct(productId: Long): List<ProductSaleDto> {
        return getWithFilter {
            eq("product_id", productId)
            order("date", ascending = false)
        }
    }

    /**
     * Récupère les ventes d'un site
     */
    suspend fun getProductSalesBySite(siteId: Long): List<ProductSaleDto> {
        return getWithFilter {
            eq("site_id", siteId)
            order("date", ascending = false)
        }
    }

    /**
     * Récupère les ventes par fermier/agriculteur
     */
    suspend fun getProductSalesByFarmer(farmerName: String): List<ProductSaleDto> {
        return getWithFilter {
            ilike("farmer_name", "%$farmerName%")
            order("date", ascending = false)
        }
    }
}
