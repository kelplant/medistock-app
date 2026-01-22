package com.medistock.data.sync

import android.content.Context
import com.medistock.data.db.AppDatabase
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.remote.repository.*
import com.medistock.data.sync.SyncMapper.toDto
import com.medistock.data.sync.SyncMapper.toEntity
import com.medistock.shared.domain.sync.SyncDirection
import com.medistock.shared.domain.sync.SyncOrchestrator
import com.medistock.util.NetworkStatus
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
    private val orchestrator = SyncOrchestrator()

    // Repositories Supabase
    private val productRepo by lazy { ProductSupabaseRepository() }
    private val categoryRepo by lazy { CategorySupabaseRepository() }
    private val siteRepo by lazy { SiteSupabaseRepository() }
    private val customerRepo by lazy { CustomerSupabaseRepository() }
    private val packagingTypeRepo by lazy { PackagingTypeSupabaseRepository() }
    private val userRepo by lazy { UserSupabaseRepository() }
    private val userPermissionRepo by lazy { UserPermissionSupabaseRepository() }
    private val saleRepo by lazy { SaleSupabaseRepository() }
    private val saleItemRepo by lazy { SaleItemSupabaseRepository() }
    private val purchaseBatchRepo by lazy { PurchaseBatchSupabaseRepository() }
    private val stockMovementRepo by lazy { StockMovementSupabaseRepository() }

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

            if (!NetworkStatus.isOnline(context)) {
                onError?.invoke("Connexion", Exception("Connexion indisponible. Mode local actif."))
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

            // 6. Users
            onProgress?.invoke("Synchronisation des utilisateurs...")
            syncUsersToRemote(onError)

            // 7. User Permissions
            onProgress?.invoke("Synchronisation des permissions...")
            syncUserPermissionsToRemote(onError)

            // 8. Purchase Batches
            onProgress?.invoke("Synchronisation des achats...")
            syncPurchaseBatchesToRemote(onError)

            // 9. Sales and Sale Items
            onProgress?.invoke("Synchronisation des ventes...")
            syncSalesToRemote(onError)

            // 10. Stock Movements
            onProgress?.invoke("Synchronisation des mouvements de stock...")
            syncStockMovementsToRemote(onError)

            onProgress?.invoke(orchestrator.getCompletionMessage(SyncDirection.LOCAL_TO_REMOTE))
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

            if (!NetworkStatus.isOnline(context)) {
                onError?.invoke("Connexion", Exception("Connexion indisponible. Mode local actif."))
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

            // 6. Users
            onProgress?.invoke("Récupération des utilisateurs...")
            syncUsersFromRemote(onError)

            // 7. User Permissions
            onProgress?.invoke("Récupération des permissions...")
            syncUserPermissionsFromRemote(onError)

            // 8. Purchase Batches
            onProgress?.invoke("Récupération des achats...")
            syncPurchaseBatchesFromRemote(onError)

            // 9. Sales and Sale Items
            onProgress?.invoke("Récupération des ventes...")
            syncSalesFromRemote(onError)

            // 10. Stock Movements
            onProgress?.invoke("Récupération des mouvements de stock...")
            syncStockMovementsFromRemote(onError)

            onProgress?.invoke(orchestrator.getCompletionMessage(SyncDirection.REMOTE_TO_LOCAL))
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
                    // Upsert: insère si nouveau, met à jour sinon
                    siteRepo.upsertSite(dto)
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
                    // Upsert: insère si nouveau, met à jour sinon
                    packagingTypeRepo.upsertPackagingType(dto)
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
                    // Upsert: insère si nouveau, met à jour sinon
                    categoryRepo.upsertCategory(dto)
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
                    // Upsert: insère si nouveau, met à jour sinon
                    productRepo.upsertProduct(dto)
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
                    // Upsert: insère si nouveau, met à jour sinon
                    customerRepo.upsertCustomer(dto)
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

    // ==================== Synchronisation Users ====================

    private suspend fun syncUsersToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localUsers = database.userDao().getAllUsers()
            localUsers.forEach { user ->
                try {
                    val dto = user.toDto()
                    userRepo.upsert(dto)
                } catch (e: Exception) {
                    onError?.invoke("User: ${user.username}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Users", e)
        }
    }

    private suspend fun syncUsersFromRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val remoteUsers = userRepo.getAllUsers()
            remoteUsers.forEach { dto ->
                try {
                    val entity = dto.toEntity()
                    database.userDao().insertUser(entity)
                } catch (e: Exception) {
                    onError?.invoke("User: ${dto.username}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Users", e)
        }
    }

    // ==================== Synchronisation User Permissions ====================

    private suspend fun syncUserPermissionsToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localPermissions = database.userPermissionDao().getAllPermissions()
            localPermissions.forEach { permission ->
                try {
                    val dto = permission.toDto()
                    userPermissionRepo.upsert(dto)
                } catch (e: Exception) {
                    onError?.invoke("Permission: ${permission.module}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Permissions", e)
        }
    }

    private suspend fun syncUserPermissionsFromRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val remotePermissions = userPermissionRepo.getAllPermissions()
            remotePermissions.forEach { dto ->
                try {
                    val entity = dto.toEntity()
                    database.userPermissionDao().insertPermission(entity)
                } catch (e: Exception) {
                    onError?.invoke("Permission: ${dto.module}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Permissions", e)
        }
    }

    // ==================== Synchronisation Purchase Batches ====================

    private suspend fun syncPurchaseBatchesToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localBatches = database.purchaseBatchDao().getAll().firstOrNull() ?: emptyList()
            localBatches.forEach { batch ->
                try {
                    val dto = batch.toDto()
                    purchaseBatchRepo.upsert(dto)
                } catch (e: Exception) {
                    onError?.invoke("Batch: ${batch.batchNumber ?: batch.id}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Purchase Batches", e)
        }
    }

    private suspend fun syncPurchaseBatchesFromRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val remoteBatches = purchaseBatchRepo.getAllBatches()
            remoteBatches.forEach { dto ->
                try {
                    val entity = dto.toEntity()
                    database.purchaseBatchDao().insert(entity)
                } catch (e: Exception) {
                    onError?.invoke("Batch: ${dto.batchNumber ?: dto.id}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Purchase Batches", e)
        }
    }

    // ==================== Synchronisation Sales ====================

    private suspend fun syncSalesToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localSales = database.saleDao().getAll().firstOrNull() ?: emptyList()
            localSales.forEach { sale ->
                try {
                    // Upsert sale
                    val saleDto = sale.toDto()
                    saleRepo.upsert(saleDto)

                    // Sync sale items for this sale
                    val saleItems = database.saleItemDao().getItemsBySale(sale.id)
                    saleItems.forEach { item ->
                        val itemDto = item.toDto()
                        saleItemRepo.upsert(itemDto)
                    }
                } catch (e: Exception) {
                    onError?.invoke("Sale: ${sale.id}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Sales", e)
        }
    }

    private suspend fun syncSalesFromRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            // Fetch all sales
            val remoteSales = saleRepo.getAllSales()
            // Fetch all sale items
            val remoteSaleItems = saleItemRepo.getAllSaleItems()
            // Group items by sale_id
            val itemsBySale = remoteSaleItems.groupBy { it.saleId }

            remoteSales.forEach { saleDto ->
                try {
                    // Insert sale
                    val saleEntity = saleDto.toEntity()
                    database.saleDao().insert(saleEntity)

                    // Insert sale items
                    val items = itemsBySale[saleDto.id] ?: emptyList()
                    items.forEach { itemDto ->
                        val itemEntity = itemDto.toEntity()
                        database.saleItemDao().insert(itemEntity)
                    }
                } catch (e: Exception) {
                    onError?.invoke("Sale: ${saleDto.id}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Sales", e)
        }
    }

    // ==================== Synchronisation Stock Movements ====================

    private suspend fun syncStockMovementsToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localMovements = database.stockMovementDao().getAll().firstOrNull() ?: emptyList()
            localMovements.forEach { movement ->
                try {
                    val dto = movement.toDto()
                    stockMovementRepo.upsert(dto)
                } catch (e: Exception) {
                    onError?.invoke("Movement: ${movement.id}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Stock Movements", e)
        }
    }

    private suspend fun syncStockMovementsFromRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val remoteMovements = stockMovementRepo.getAllMovements()
            remoteMovements.forEach { dto ->
                try {
                    val entity = dto.toEntity()
                    database.stockMovementDao().insert(entity)
                } catch (e: Exception) {
                    onError?.invoke("Movement: ${dto.id}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Stock Movements", e)
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
