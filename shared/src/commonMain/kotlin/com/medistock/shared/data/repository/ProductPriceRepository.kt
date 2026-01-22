package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.ProductPrice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductPriceRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getByProduct(productId: String): List<ProductPrice> = withContext(Dispatchers.Default) {
        queries.getProductPricesByProduct(productId).executeAsList().map { it.toModel() }
    }

    suspend fun getLatestPrice(productId: String): ProductPrice? = withContext(Dispatchers.Default) {
        queries.getLatestProductPrice(productId).executeAsOneOrNull()?.toModel()
    }

    suspend fun getLatestPriceBySite(productId: String, siteId: String): ProductPrice? = withContext(Dispatchers.Default) {
        queries.getLatestProductPriceBySite(productId, siteId).executeAsOneOrNull()?.toModel()
    }

    suspend fun insert(price: ProductPrice) = withContext(Dispatchers.Default) {
        queries.insertProductPrice(
            id = price.id,
            product_id = price.productId,
            effective_date = price.effectiveDate,
            purchase_price = price.purchasePrice,
            selling_price = price.sellingPrice,
            source = price.source,
            site_id = price.siteId,
            price = price.price ?: price.sellingPrice,
            created_at = price.createdAt,
            updated_at = price.updatedAt,
            created_by = price.createdBy,
            updated_by = price.updatedBy
        )
    }

    fun observeByProduct(productId: String): Flow<List<ProductPrice>> {
        return queries.getProductPricesByProduct(productId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    private fun com.medistock.shared.db.Product_prices.toModel(): ProductPrice {
        return ProductPrice(
            id = id,
            productId = product_id,
            effectiveDate = effective_date ?: created_at,
            purchasePrice = purchase_price ?: 0.0,
            sellingPrice = selling_price ?: price ?: 0.0,
            source = source ?: "manual",
            siteId = site_id,
            price = price,
            createdAt = created_at,
            updatedAt = updated_at,
            createdBy = created_by,
            updatedBy = updated_by
        )
    }
}
