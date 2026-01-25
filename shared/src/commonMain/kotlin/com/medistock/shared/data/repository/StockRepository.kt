package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.CurrentStock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StockRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getCurrentStockByProductAndSite(productId: String, siteId: String): CurrentStock? = withContext(Dispatchers.Default) {
        queries.getCurrentStockForProductAndSite(productId, siteId).executeAsOneOrNull()?.let {
            CurrentStock(
                productId = it.product_id,
                productName = it.product_name,
                unit = it.unit ?: "",
                categoryName = it.category_name ?: "",
                siteId = it.site_id,
                siteName = it.site_name,
                quantityOnHand = it.quantity_on_hand,
                minStock = it.min_stock ?: 0.0,
                maxStock = it.max_stock ?: 0.0
            )
        }
    }

    suspend fun getCurrentStockForSite(siteId: String): List<CurrentStock> = withContext(Dispatchers.Default) {
        queries.getCurrentStockForSite(siteId).executeAsList().map {
            CurrentStock(
                productId = it.product_id,
                productName = it.product_name,
                unit = it.unit ?: "",
                categoryName = it.category_name ?: "",
                siteId = it.site_id,
                siteName = it.site_name,
                quantityOnHand = it.quantity_on_hand,
                minStock = it.min_stock ?: 0.0,
                maxStock = it.max_stock ?: 0.0
            )
        }
    }

    suspend fun getAllCurrentStock(): List<CurrentStock> = withContext(Dispatchers.Default) {
        queries.getCurrentStockAllSites().executeAsList().map {
            CurrentStock(
                productId = it.product_id,
                productName = it.product_name,
                unit = it.unit ?: "",
                categoryName = it.category_name ?: "",
                siteId = it.site_id,
                siteName = it.site_name,
                quantityOnHand = it.quantity_on_hand,
                minStock = it.min_stock ?: 0.0,
                maxStock = it.max_stock ?: 0.0
            )
        }
    }

    fun observeCurrentStockForSite(siteId: String): Flow<List<CurrentStock>> {
        return queries.getCurrentStockForSite(siteId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    CurrentStock(
                        productId = it.product_id,
                        productName = it.product_name,
                        unit = it.unit ?: "",
                        categoryName = it.category_name ?: "",
                        siteId = it.site_id,
                        siteName = it.site_name,
                        quantityOnHand = it.quantity_on_hand,
                        minStock = it.min_stock ?: 0.0,
                        maxStock = it.max_stock ?: 0.0
                    )
                }
            }
    }

    fun observeAllCurrentStock(): Flow<List<CurrentStock>> {
        return queries.getCurrentStockAllSites()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    CurrentStock(
                        productId = it.product_id,
                        productName = it.product_name,
                        unit = it.unit ?: "",
                        categoryName = it.category_name ?: "",
                        siteId = it.site_id,
                        siteName = it.site_name,
                        quantityOnHand = it.quantity_on_hand,
                        minStock = it.min_stock ?: 0.0,
                        maxStock = it.max_stock ?: 0.0
                    )
                }
            }
    }
}
