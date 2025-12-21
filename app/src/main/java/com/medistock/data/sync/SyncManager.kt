package com.medistock.data.sync

import android.content.Context
import com.medistock.data.db.AppDatabase
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.remote.repository.*
import com.medistock.data.sync.SyncMapper.toDto
import com.medistock.data.sync.SyncMapper.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gestionnaire de synchronisation bidirectionnelle entre Room (local) et Supabase (remote)
 */
class SyncManager(
    private val context: Context
) {
    private val database = AppDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Repositories Supabase
    private val productRepo = ProductSupabaseRepository()
    private val categoryRepo = CategorySupabaseRepository()
    private val siteRepo = SiteSupabaseRepository()
    private val customerRepo = CustomerSupabaseRepository()
    private val packagingTypeRepo = PackagingTypeSupabaseRepository()

    /**
     * Synchronise toutes les données de Room vers Supabase
     * À utiliser lors de la première connexion ou pour une sync complète
     */
    suspend fun syncLocalToRemote(
        onProgress: ((String) -> Unit)? = null,
        onError: ((String, Exception) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured(context)) {
                onError?.invoke("Supabase", Exception("Supabase n'est pas configuré"))
                return@withContext
            }

            // 1. Sites d'abord (car les autres tables dépendent de site_id)
            onProgress?.invoke("Synchronisation des sites...")
            syncSitesToRemote(onError)

            // 2. PackagingTypes
            onProgress?.invoke("Synchronisation des types de conditionnement...")
            syncPackagingTypesToRemote(onError)

            // 3. Categories
            onProgress?.invoke("Synchronisation des catégories...")
            syncCategoriesToRemote(onError)

            // 4. Products
            onProgress?.invoke("Synchronisation des produits...")
            syncProductsToRemote(onError)

            // 5. Customers
            onProgress?.invoke("Synchronisation des clients...")
            syncCustomersToRemote(onError)

            onProgress?.invoke("Synchronisation terminée ✅")
        } catch (e: Exception) {
            onError?.invoke("Sync générale", e)
        }
    }

    /**
     * Synchronise toutes les données de Supabase vers Room
     * Pour récupérer les données d'autres appareils
     */
    suspend fun syncRemoteToLocal(
        onProgress: ((String) -> Unit)? = null,
        onError: ((String, Exception) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured(context)) {
                onError?.invoke("Supabase", Exception("Supabase n'est pas configuré"))
                return@withContext
            }

            // 1. Sites d'abord
            onProgress?.invoke("Récupération des sites...")
            syncSitesFromRemote(onError)

            // 2. PackagingTypes
            onProgress?.invoke("Récupération des types de conditionnement...")
            syncPackagingTypesFromRemote(onError)

            // 3. Categories
            onProgress?.invoke("Récupération des catégories...")
            syncCategoriesFromRemote(onError)

            // 4. Products
            onProgress?.invoke("Récupération des produits...")
            syncProductsFromRemote(onError)

            // 5. Customers
            onProgress?.invoke("Récupération des clients...")
            syncCustomersFromRemote(onError)

            onProgress?.invoke("Récupération terminée ✅")
        } catch (e: Exception) {
            onError?.invoke("Sync générale", e)
        }
    }

    // ==================== Synchronisation Sites ====================

    private suspend fun syncSitesToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localSites = database.siteDao().getAll().firstOrNull() ?: emptyList()
            localSites.forEach { site ->
                try {
                    val dto = site.toDto()
                    if (site.id == 0L) {
                        // Nouveau site - créer
                        siteRepo.createSite(dto)
                    } else {
                        // Mettre à jour
                        siteRepo.updateSite(site.id, dto)
                    }
                } catch (e: Exception) {
                    onError?.invoke("Site: ${site.name}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Sites", e)
        }
    }

    private suspend fun syncSitesFromRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val remoteSites = siteRepo.getAllSites()
            remoteSites.forEach { dto ->
                try {
                    val entity = dto.toEntity()
                    database.siteDao().insert(entity)
                } catch (e: Exception) {
                    onError?.invoke("Site: ${dto.name}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Sites", e)
        }
    }

    // ==================== Synchronisation PackagingTypes ====================

    private suspend fun syncPackagingTypesToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localTypes = database.packagingTypeDao().getAll().firstOrNull() ?: emptyList()
            localTypes.forEach { type ->
                try {
                    val dto = type.toDto()
                    if (type.id == 0L) {
                        packagingTypeRepo.createPackagingType(dto)
                    } else {
                        packagingTypeRepo.updatePackagingType(type.id, dto)
                    }
                } catch (e: Exception) {
                    onError?.invoke("PackagingType: ${type.name}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("PackagingTypes", e)
        }
    }

    private suspend fun syncPackagingTypesFromRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val remoteTypes = packagingTypeRepo.getAllPackagingTypes()
            remoteTypes.forEach { dto ->
                try {
                    val entity = dto.toEntity()
                    database.packagingTypeDao().insert(entity)
                } catch (e: Exception) {
                    onError?.invoke("PackagingType: ${dto.name}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("PackagingTypes", e)
        }
    }

    // ==================== Synchronisation Categories ====================

    private suspend fun syncCategoriesToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localCategories = database.categoryDao().getAll().firstOrNull() ?: emptyList()
            localCategories.forEach { category ->
                try {
                    val dto = category.toDto()
                    if (category.id == 0L) {
                        categoryRepo.createCategory(dto)
                    } else {
                        categoryRepo.updateCategory(category.id, dto)
                    }
                } catch (e: Exception) {
                    onError?.invoke("Category: ${category.name}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Categories", e)
        }
    }

    private suspend fun syncCategoriesFromRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val remoteCategories = categoryRepo.getAllCategories()
            remoteCategories.forEach { dto ->
                try {
                    val entity = dto.toEntity()
                    database.categoryDao().insert(entity)
                } catch (e: Exception) {
                    onError?.invoke("Category: ${dto.name}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Categories", e)
        }
    }

    // ==================== Synchronisation Products ====================

    private suspend fun syncProductsToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localProducts = database.productDao().getAll().firstOrNull() ?: emptyList()
            localProducts.forEach { product ->
                try {
                    val dto = product.toDto()
                    if (product.id == 0L) {
                        productRepo.createProduct(dto)
                    } else {
                        productRepo.updateProduct(product.id, dto)
                    }
                } catch (e: Exception) {
                    onError?.invoke("Product: ${product.name}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Products", e)
        }
    }

    private suspend fun syncProductsFromRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val remoteProducts = productRepo.getAllProducts()
            remoteProducts.forEach { dto ->
                try {
                    val entity = dto.toEntity()
                    database.productDao().insert(entity)
                } catch (e: Exception) {
                    onError?.invoke("Product: ${dto.name}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Products", e)
        }
    }

    // ==================== Synchronisation Customers ====================

    private suspend fun syncCustomersToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localCustomers = database.customerDao().getAll().firstOrNull() ?: emptyList()
            localCustomers.forEach { customer ->
                try {
                    val dto = customer.toDto()
                    if (customer.id == 0L) {
                        customerRepo.createCustomer(dto)
                    } else {
                        customerRepo.updateCustomer(customer.id, dto)
                    }
                } catch (e: Exception) {
                    onError?.invoke("Customer: ${customer.name}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Customers", e)
        }
    }

    private suspend fun syncCustomersFromRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val remoteCustomers = customerRepo.getAllCustomers()
            remoteCustomers.forEach { dto ->
                try {
                    val entity = dto.toEntity()
                    database.customerDao().insert(entity)
                } catch (e: Exception) {
                    onError?.invoke("Customer: ${dto.name}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Customers", e)
        }
    }

    /**
     * Synchronisation bidirectionnelle complète
     * 1. Envoie les données locales vers Supabase
     * 2. Récupère les données de Supabase vers Room
     */
    suspend fun fullSync(
        onProgress: ((String) -> Unit)? = null,
        onError: ((String, Exception) -> Unit)? = null
    ) {
        syncLocalToRemote(onProgress, onError)
        syncRemoteToLocal(onProgress, onError)
    }
}
