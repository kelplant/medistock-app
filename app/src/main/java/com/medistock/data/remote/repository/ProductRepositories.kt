package com.medistock.data.remote.repository

import com.medistock.shared.data.dto.ProductDto
import com.medistock.data.remote.dto.ProductPriceDto
import com.medistock.data.remote.dto.CurrentStockDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class ProductSupabaseRepository : BaseSupabaseRepository("products") {
    suspend fun getAllProducts(): List<ProductDto> = getAll()
    suspend fun getProductById(id: String): ProductDto? = getById(id)
    suspend fun createProduct(product: ProductDto): ProductDto = create(product)
    suspend fun updateProduct(id: String, product: ProductDto): ProductDto = update(id, product)
    suspend fun upsertProduct(product: ProductDto): ProductDto = upsert(product)
    suspend fun deleteProduct(id: String) = delete(id)

    suspend fun getProductsBySite(siteId: String): List<ProductDto> {
        return supabase.from(tableName).select {
            filter { eq("site_id", siteId) }
        }.decodeList()
    }

    suspend fun getProductsByCategory(categoryId: String): List<ProductDto> {
        return supabase.from(tableName).select {
            filter { eq("category_id", categoryId) }
        }.decodeList()
    }

    suspend fun getProductsByPackagingType(packagingTypeId: String): List<ProductDto> {
        return supabase.from(tableName).select {
            filter { eq("packaging_type_id", packagingTypeId) }
        }.decodeList()
    }

    suspend fun searchByName(name: String): List<ProductDto> {
        return supabase.from(tableName).select {
            filter { ilike("name", "%$name%") }
        }.decodeList()
    }
}

class ProductPriceSupabaseRepository : BaseSupabaseRepository("product_prices") {
    suspend fun getAllPrices(): List<ProductPriceDto> = getAll()
    suspend fun getPriceById(id: String): ProductPriceDto? = getById(id)
    suspend fun createPrice(price: ProductPriceDto): ProductPriceDto = create(price)
    suspend fun updatePrice(id: String, price: ProductPriceDto): ProductPriceDto = update(id, price)
    suspend fun deletePrice(id: String) = delete(id)

    suspend fun getPriceHistoryByProduct(productId: String): List<ProductPriceDto> {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
            order(column = "effective_date", order = Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getCurrentPrice(productId: String): ProductPriceDto? {
        val now = System.currentTimeMillis()
        return supabase.from(tableName).select {
            filter {
                eq("product_id", productId)
                lte("effective_date", now)
            }
            order(column = "effective_date", order = Order.DESCENDING)
            limit(1)
        }.decodeList<ProductPriceDto>().firstOrNull()
    }

    suspend fun getPricesBySource(source: String): List<ProductPriceDto> {
        return supabase.from(tableName).select {
            filter { eq("source", source) }
        }.decodeList()
    }
}

class CurrentStockRepository : BaseSupabaseRepository("current_stock") {
    suspend fun getAllStock(): List<CurrentStockDto> = getAll()

    suspend fun getStockByProduct(productId: String): CurrentStockDto? {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
        }.decodeList<CurrentStockDto>().firstOrNull()
    }

    suspend fun getStockBySite(siteId: String): List<CurrentStockDto> {
        return supabase.from(tableName).select {
            filter { eq("site_id", siteId) }
        }.decodeList()
    }

    suspend fun getLowStockProducts(siteId: String? = null): List<CurrentStockDto> {
        return supabase.from(tableName).select {
            filter {
                eq("stock_status", "LOW")
                if (siteId != null) {
                    eq("site_id", siteId)
                }
            }
        }.decodeList()
    }

    suspend fun getHighStockProducts(siteId: String? = null): List<CurrentStockDto> {
        return supabase.from(tableName).select {
            filter {
                eq("stock_status", "HIGH")
                if (siteId != null) {
                    eq("site_id", siteId)
                }
            }
        }.decodeList()
    }
}
