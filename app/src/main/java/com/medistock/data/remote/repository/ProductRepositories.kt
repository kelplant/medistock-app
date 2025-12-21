package com.medistock.data.remote.repository

import com.medistock.data.remote.dto.ProductDto
import com.medistock.data.remote.dto.ProductPriceDto
import com.medistock.data.remote.dto.CurrentStockDto

class ProductSupabaseRepository : BaseSupabaseRepository("products") {
    suspend fun getAllProducts(): List<ProductDto> = getAll()
    suspend fun getProductById(id: Long): ProductDto? = getById(id)
    suspend fun createProduct(product: ProductDto): ProductDto = create(product)
    suspend fun updateProduct(id: Long, product: ProductDto): ProductDto = update(id, product)
    suspend fun deleteProduct(id: Long) = delete(id)

    suspend fun getProductsBySite(siteId: Long): List<ProductDto> {
        return supabase.from(tableName).select {
            filter { eq("site_id", siteId) }
        }.decodeList()
    }

    suspend fun getProductsByCategory(categoryId: Long): List<ProductDto> {
        return supabase.from(tableName).select {
            filter { eq("category_id", categoryId) }
        }.decodeList()
    }

    suspend fun getProductsByPackagingType(packagingTypeId: Long): List<ProductDto> {
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
    suspend fun getPriceById(id: Long): ProductPriceDto? = getById(id)
    suspend fun createPrice(price: ProductPriceDto): ProductPriceDto = create(price)
    suspend fun updatePrice(id: Long, price: ProductPriceDto): ProductPriceDto = update(id, price)
    suspend fun deletePrice(id: Long) = delete(id)

    suspend fun getPriceHistoryByProduct(productId: Long): List<ProductPriceDto> {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
            order("effective_date", ascending = false)
        }.decodeList()
    }

    suspend fun getCurrentPrice(productId: Long): ProductPriceDto? {
        val now = System.currentTimeMillis()
        return supabase.from(tableName).select {
            filter {
                eq("product_id", productId)
                lte("effective_date", now)
            }
            order("effective_date", ascending = false)
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

    suspend fun getStockByProduct(productId: Long): CurrentStockDto? {
        return supabase.from(tableName).select {
            filter { eq("product_id", productId) }
        }.decodeList<CurrentStockDto>().firstOrNull()
    }

    suspend fun getStockBySite(siteId: Long): List<CurrentStockDto> {
        return supabase.from(tableName).select {
            filter { eq("site_id", siteId) }
        }.decodeList()
    }

    suspend fun getLowStockProducts(siteId: Long? = null): List<CurrentStockDto> {
        return supabase.from(tableName).select {
            filter {
                eq("stock_status", "LOW")
                if (siteId != null) {
                    eq("site_id", siteId)
                }
            }
        }.decodeList()
    }

    suspend fun getHighStockProducts(siteId: Long? = null): List<CurrentStockDto> {
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
