package com.medistock.data.remote.repository

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.*

import com.medistock.data.remote.dto.ProductDto
import com.medistock.data.remote.dto.ProductPriceDto
import com.medistock.data.remote.dto.CurrentStockDto

/**
 * Repository pour les produits
 */
class ProductSupabaseRepository : BaseSupabaseRepository("products") {

    suspend fun getAllProducts(): List<ProductDto> = getAll()

    suspend fun getProductById(id: Long): ProductDto? = getById(id)

    suspend fun createProduct(product: ProductDto): ProductDto = create(product)

    suspend fun updateProduct(id: Long, product: ProductDto): ProductDto = update(id, product)

    suspend fun deleteProduct(id: Long) = delete(id)

    /**
     * Récupère les produits d'un site
     */
    suspend fun getProductsBySite(siteId: Long): List<ProductDto> {
        return getWithFilter {
            eq("site_id", siteId)
        }
    }

    /**
     * Récupère les produits d'une catégorie
     */
    suspend fun getProductsByCategory(categoryId: Long): List<ProductDto> {
        return getWithFilter {
            eq("category_id", categoryId)
        }
    }

    /**
     * Récupère les produits par type de conditionnement
     */
    suspend fun getProductsByPackagingType(packagingTypeId: Long): List<ProductDto> {
        return getWithFilter {
            eq("packaging_type_id", packagingTypeId)
        }
    }

    /**
     * Recherche de produits par nom
     */
    suspend fun searchByName(name: String): List<ProductDto> {
        return getWithFilter {
            ilike("name", "%$name%")
        }
    }

    /**
     * Récupère les produits avec stock faible
     */
    suspend fun getLowStockProducts(siteId: Long? = null): List<ProductDto> {
        // TODO: Implémenter avec une requête qui compare current_stock et min_stock
        // Nécessite une jointure avec la vue current_stock
        return emptyList()
    }
}

/**
 * Repository pour les prix des produits
 */
class ProductPriceSupabaseRepository : BaseSupabaseRepository("product_prices") {

    suspend fun getAllPrices(): List<ProductPriceDto> = getAll()

    suspend fun getPriceById(id: Long): ProductPriceDto? = getById(id)

    suspend fun createPrice(price: ProductPriceDto): ProductPriceDto = create(price)

    suspend fun updatePrice(id: Long, price: ProductPriceDto): ProductPriceDto = update(id, price)

    suspend fun deletePrice(id: Long) = delete(id)

    /**
     * Récupère l'historique des prix d'un produit
     */
    suspend fun getPriceHistoryByProduct(productId: Long): List<ProductPriceDto> {
        return getWithFilter {
            eq("product_id", productId)
            order("effective_date", ascending = false)
        }
    }

    /**
     * Récupère le prix actuel d'un produit
     */
    suspend fun getCurrentPrice(productId: Long): ProductPriceDto? {
        val now = System.currentTimeMillis()
        return getWithFilter<ProductPriceDto> {
            eq("product_id", productId)
            lte("effective_date", now)
            order("effective_date", ascending = false)
            limit(1)
        }.firstOrNull()
    }

    /**
     * Récupère les prix par source (manual/calculated)
     */
    suspend fun getPricesBySource(source: String): List<ProductPriceDto> {
        return getWithFilter {
            eq("source", source)
        }
    }
}

/**
 * Repository pour la vue current_stock (lecture seule)
 */
class CurrentStockRepository : BaseSupabaseRepository("current_stock") {

    suspend fun getAllStock(): List<CurrentStockDto> = getAll()

    /**
     * Récupère le stock d'un produit spécifique
     */
    suspend fun getStockByProduct(productId: Long): CurrentStockDto? {
        return getWithFilter<CurrentStockDto> {
            eq("product_id", productId)
        }.firstOrNull()
    }

    /**
     * Récupère le stock d'un site
     */
    suspend fun getStockBySite(siteId: Long): List<CurrentStockDto> {
        return getWithFilter {
            eq("site_id", siteId)
        }
    }

    /**
     * Récupère les produits avec stock faible
     */
    suspend fun getLowStockProducts(siteId: Long? = null): List<CurrentStockDto> {
        return getWithFilter {
            eq("stock_status", "LOW")
            if (siteId != null) {
                eq("site_id", siteId)
            }
        }
    }

    /**
     * Récupère les produits avec stock élevé
     */
    suspend fun getHighStockProducts(siteId: Long? = null): List<CurrentStockDto> {
        return getWithFilter {
            eq("stock_status", "HIGH")
            if (siteId != null) {
                eq("site_id", siteId)
            }
        }
    }
}
