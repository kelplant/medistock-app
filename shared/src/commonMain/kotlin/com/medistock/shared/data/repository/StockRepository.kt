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
        queries.getCurrentStockByProductAndSite(productId, siteId).executeAsOneOrNull()?.let {
            CurrentStock(
                productId = it.product_id,
                siteId = it.site_id,
                totalStock = it.total_stock
            )
        }
    }

    suspend fun getAllCurrentStock(): List<CurrentStock> = withContext(Dispatchers.Default) {
        queries.getAllCurrentStock().executeAsList().map {
            CurrentStock(
                productId = it.product_id,
                siteId = it.site_id,
                totalStock = it.total_stock
            )
        }
    }

    fun observeAllCurrentStock(): Flow<List<CurrentStock>> {
        return queries.getAllCurrentStock()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    CurrentStock(
                        productId = it.product_id,
                        siteId = it.site_id,
                        totalStock = it.total_stock
                    )
                }
            }
    }
}
