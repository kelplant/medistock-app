package com.medistock.data.remote.repository

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.*

import com.medistock.data.remote.dto.*

/**
 * Repository pour les lots d'achat (FIFO)
 */
class PurchaseBatchSupabaseRepository : BaseSupabaseRepository("purchase_batches") {

    suspend fun getAllBatches(): List<PurchaseBatchDto> = getAll()

    suspend fun getBatchById(id: Long): PurchaseBatchDto? = getById(id)

    suspend fun createBatch(batch: PurchaseBatchDto): PurchaseBatchDto = create(batch)

    suspend fun updateBatch(id: Long, batch: PurchaseBatchDto): PurchaseBatchDto = update(id, batch)

    suspend fun deleteBatch(id: Long) = delete(id)

    /**
     * Récupère les lots d'un produit
     */
    suspend fun getBatchesByProduct(productId: Long): List<PurchaseBatchDto> {
        return getWithFilter {
            eq("product_id", productId)
            order("purchase_date", ascending = true) // FIFO: plus ancien en premier
        }
    }

    /**
     * Récupère les lots actifs (non épuisés) d'un produit
     */
    suspend fun getActiveBatchesByProduct(productId: Long): List<PurchaseBatchDto> {
        return getWithFilter {
            eq("product_id", productId)
            eq("is_exhausted", false)
            order("purchase_date", ascending = true) // FIFO
        }
    }

    /**
     * Récupère les lots d'un site
     */
    suspend fun getBatchesBySite(siteId: Long): List<PurchaseBatchDto> {
        return getWithFilter {
            eq("site_id", siteId)
        }
    }

    /**
     * Récupère les lots proches de l'expiration
     */
    suspend fun getExpiringBatches(daysThreshold: Int = 30): List<PurchaseBatchDto> {
        val thresholdDate = System.currentTimeMillis() + (daysThreshold * 24 * 60 * 60 * 1000L)
        return getWithFilter {
            eq("is_exhausted", false)
            not { isNull("expiry_date") }
            lte("expiry_date", thresholdDate)
            order("expiry_date", ascending = true)
        }
    }

    /**
     * Récupère les lots par fournisseur
     */
    suspend fun getBatchesBySupplier(supplierName: String): List<PurchaseBatchDto> {
        return getWithFilter {
            ilike("supplier_name", "%$supplierName%")
        }
    }
}

/**
 * Repository pour les mouvements de stock
 */
class StockMovementSupabaseRepository : BaseSupabaseRepository("stock_movements") {

    suspend fun getAllMovements(): List<StockMovementDto> = getAll()

    suspend fun getMovementById(id: Long): StockMovementDto? = getById(id)

    suspend fun createMovement(movement: StockMovementDto): StockMovementDto = create(movement)

    /**
     * Récupère les mouvements d'un produit
     */
    suspend fun getMovementsByProduct(productId: Long): List<StockMovementDto> {
        return getWithFilter {
            eq("product_id", productId)
            order("date", ascending = false)
        }
    }

    /**
     * Récupère les mouvements d'un site
     */
    suspend fun getMovementsBySite(siteId: Long): List<StockMovementDto> {
        return getWithFilter {
            eq("site_id", siteId)
            order("date", ascending = false)
        }
    }

    /**
     * Récupère les mouvements par type (IN/OUT)
     */
    suspend fun getMovementsByType(type: String, siteId: Long? = null): List<StockMovementDto> {
        return getWithFilter {
            eq("type", type)
            if (siteId != null) {
                eq("site_id", siteId)
            }
            order("date", ascending = false)
        }
    }

    /**
     * Récupère les mouvements sur une période
     */
    suspend fun getMovementsByDateRange(
        startDate: Long,
        endDate: Long,
        siteId: Long? = null
    ): List<StockMovementDto> {
        return getWithFilter {
            gte("date", startDate)
            lte("date", endDate)
            if (siteId != null) {
                eq("site_id", siteId)
            }
            order("date", ascending = false)
        }
    }
}

/**
 * Repository pour les inventaires
 */
class InventorySupabaseRepository : BaseSupabaseRepository("inventories") {

    suspend fun getAllInventories(): List<InventoryDto> = getAll()

    suspend fun getInventoryById(id: Long): InventoryDto? = getById(id)

    suspend fun createInventory(inventory: InventoryDto): InventoryDto = create(inventory)

    suspend fun updateInventory(id: Long, inventory: InventoryDto): InventoryDto = update(id, inventory)

    suspend fun deleteInventory(id: Long) = delete(id)

    /**
     * Récupère les inventaires d'un produit
     */
    suspend fun getInventoriesByProduct(productId: Long): List<InventoryDto> {
        return getWithFilter {
            eq("product_id", productId)
            order("count_date", ascending = false)
        }
    }

    /**
     * Récupère les inventaires d'un site
     */
    suspend fun getInventoriesBySite(siteId: Long): List<InventoryDto> {
        return getWithFilter {
            eq("site_id", siteId)
            order("count_date", ascending = false)
        }
    }

    /**
     * Récupère les inventaires avec écarts
     */
    suspend fun getInventoriesWithDiscrepancy(siteId: Long? = null): List<InventoryDto> {
        return getWithFilter {
            not { eq("discrepancy", 0.0) }
            if (siteId != null) {
                eq("site_id", siteId)
            }
            order("count_date", ascending = false)
        }
    }

    /**
     * Récupère le dernier inventaire d'un produit
     */
    suspend fun getLatestInventory(productId: Long, siteId: Long): InventoryDto? {
        return getWithFilter<InventoryDto> {
            eq("product_id", productId)
            eq("site_id", siteId)
            order("count_date", ascending = false)
            limit(1)
        }.firstOrNull()
    }
}

/**
 * Repository pour les transferts de produits
 */
class ProductTransferSupabaseRepository : BaseSupabaseRepository("product_transfers") {

    suspend fun getAllTransfers(): List<ProductTransferDto> = getAll()

    suspend fun getTransferById(id: Long): ProductTransferDto? = getById(id)

    suspend fun createTransfer(transfer: ProductTransferDto): ProductTransferDto = create(transfer)

    suspend fun updateTransfer(id: Long, transfer: ProductTransferDto): ProductTransferDto = update(id, transfer)

    suspend fun deleteTransfer(id: Long) = delete(id)

    /**
     * Récupère les transferts depuis un site
     */
    suspend fun getTransfersFromSite(siteId: Long): List<ProductTransferDto> {
        return getWithFilter {
            eq("from_site_id", siteId)
            order("date", ascending = false)
        }
    }

    /**
     * Récupère les transferts vers un site
     */
    suspend fun getTransfersToSite(siteId: Long): List<ProductTransferDto> {
        return getWithFilter {
            eq("to_site_id", siteId)
            order("date", ascending = false)
        }
    }

    /**
     * Récupère tous les transferts concernant un site (depuis ou vers)
     */
    suspend fun getTransfersBySite(siteId: Long): List<ProductTransferDto> {
        return getWithFilter {
            or {
                eq("from_site_id", siteId)
                eq("to_site_id", siteId)
            }
            order("date", ascending = false)
        }
    }

    /**
     * Récupère les transferts d'un produit
     */
    suspend fun getTransfersByProduct(productId: Long): List<ProductTransferDto> {
        return getWithFilter {
            eq("product_id", productId)
            order("date", ascending = false)
        }
    }

    /**
     * Récupère les transferts entre deux sites
     */
    suspend fun getTransfersBetweenSites(fromSiteId: Long, toSiteId: Long): List<ProductTransferDto> {
        return getWithFilter {
            eq("from_site_id", fromSiteId)
            eq("to_site_id", toSiteId)
            order("date", ascending = false)
        }
    }
}
