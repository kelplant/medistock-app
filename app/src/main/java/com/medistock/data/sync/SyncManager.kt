package com.medistock.data.sync

import android.content.Context
import com.medistock.MedistockApplication
import android.util.Log
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.remote.repository.*
import com.medistock.shared.MedistockSDK
import com.medistock.shared.data.dto.*
import com.medistock.shared.domain.sync.SyncDirection
import com.medistock.shared.domain.sync.SyncOrchestrator
import com.medistock.util.DebugConfig
import com.medistock.util.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestionnaire de synchronisation bidirectionnelle entre SQLDelight (local) et Supabase (remote)
 */
class SyncManager(
    private val context: Context
) {
    private val sdk: MedistockSDK = MedistockApplication.sdk
    private val scope = CoroutineScope(Dispatchers.IO)
    private val orchestrator = SyncOrchestrator()

    companion object {
        private const val TAG = "SyncManager"
    }

    // Repositories Supabase
    private val productRepo by lazy { ProductSupabaseRepository() }
    private val categoryRepo by lazy { CategorySupabaseRepository() }
    private val siteRepo by lazy { SiteSupabaseRepository() }
    private val customerRepo by lazy { CustomerSupabaseRepository() }
    private val packagingTypeRepo by lazy { PackagingTypeSupabaseRepository() }
    private val userRepo by lazy { UserSupabaseRepository() }
    private val userPermissionRepo by lazy { UserPermissionSupabaseRepository() }
    private val supplierRepo by lazy { SupplierSupabaseRepository() }
    private val saleRepo by lazy { SaleSupabaseRepository() }
    private val saleItemRepo by lazy { SaleItemSupabaseRepository() }
    private val purchaseBatchRepo by lazy { PurchaseBatchSupabaseRepository() }
    private val stockMovementRepo by lazy { StockMovementSupabaseRepository() }

    /**
     * Synchronise toutes les données de SQLDelight vers Supabase
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

            // 8. Suppliers (before purchase batches due to FK)
            onProgress?.invoke("Synchronisation des fournisseurs...")
            syncSuppliersToRemote(onError)

            // 9. Purchase Batches
            onProgress?.invoke("Synchronisation des achats...")
            syncPurchaseBatchesToRemote(onError)

            // 10. Sales and Sale Items
            onProgress?.invoke("Synchronisation des ventes...")
            syncSalesToRemote(onError)

            // 11. Stock Movements
            onProgress?.invoke("Synchronisation des mouvements de stock...")
            syncStockMovementsToRemote(onError)

            onProgress?.invoke(orchestrator.getCompletionMessage(SyncDirection.LOCAL_TO_REMOTE))
        } catch (e: Exception) {
            onError?.invoke("Sync générale", e)
        }
    }

    /**
     * Synchronise toutes les données de Supabase vers SQLDelight
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

            // 8. Suppliers (before purchase batches due to FK)
            onProgress?.invoke("Récupération des fournisseurs...")
            syncSuppliersFromRemote(onError)

            // 9. Purchase Batches
            onProgress?.invoke("Récupération des achats...")
            syncPurchaseBatchesFromRemote(onError)

            // 10. Sales and Sale Items
            onProgress?.invoke("Récupération des ventes...")
            syncSalesFromRemote(onError)

            // 11. Stock Movements
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
            val localSites = sdk.siteRepository.getAll()
            localSites.forEach { site ->
                try {
                    val dto = SiteDto.fromModel(site)
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
                    // Use upsert (INSERT OR REPLACE) to handle both new and existing records
                    sdk.siteRepository.upsert(dto.toModel())
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
            val localTypes = sdk.packagingTypeRepository.getAll()
            localTypes.forEach { type ->
                try {
                    val dto = PackagingTypeDto.fromModel(type)
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
                    // Use upsert (INSERT OR REPLACE) to handle both new and existing records
                    sdk.packagingTypeRepository.upsert(dto.toModel())
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
            val localCategories = sdk.categoryRepository.getAll()
            localCategories.forEach { category ->
                try {
                    val dto = CategoryDto.fromModel(category)
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
                    // Use upsert (INSERT OR REPLACE) to handle both new and existing records
                    sdk.categoryRepository.upsert(dto.toModel())
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
            val localProducts = sdk.productRepository.getAll()
            localProducts.forEach { product ->
                try {
                    val dto = ProductDto.fromModel(product)
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
                    // Use upsert (INSERT OR REPLACE) to handle both new and existing records
                    sdk.productRepository.upsert(dto.toModel())
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
            val localCustomers = sdk.customerRepository.getAll()
            localCustomers.forEach { customer ->
                try {
                    val dto = CustomerDto.fromModel(customer)
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
                    // Use upsert (INSERT OR REPLACE) to handle both new and existing records
                    sdk.customerRepository.upsert(dto.toModel())
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
            val localUsers = sdk.userRepository.getAll()
            localUsers.forEach { user ->
                try {
                    val dto = UserDto.fromModel(user)
                    userRepo.upsert(dto)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync user ${user.username}: ${e.message}")
                    onError?.invoke("User: ${user.username}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync users: ${e.message}")
            onError?.invoke("Users", e)
        }
    }

    private suspend fun syncUsersFromRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val remoteUsers = userRepo.getAllUsers()
            remoteUsers.forEach { dto ->
                try {
                    // Use upsert (INSERT OR REPLACE) to handle both new and existing records
                    sdk.userRepository.upsert(dto.toModel())
                } catch (e: Exception) {
                    onError?.invoke("User: ${dto.username}", e)
                }
            }

            // Remove local system admin if real users were synced
            if (remoteUsers.isNotEmpty()) {
                val removed = sdk.defaultAdminService.removeLocalAdminIfRemoteUsersExist()
                if (removed) {
                    DebugConfig.d("SyncManager", "Local system admin removed after syncing ${remoteUsers.size} remote users")
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Users", e)
        }
    }

    // ==================== Synchronisation User Permissions ====================

    private suspend fun syncUserPermissionsToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localPermissions = sdk.userPermissionRepository.getAll()
            localPermissions.forEach { permission ->
                try {
                    val dto = UserPermissionDto.fromModel(permission)
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
                    // Use upsert (INSERT OR REPLACE) to handle both new and existing records
                    sdk.userPermissionRepository.upsert(dto.toModel())
                } catch (e: Exception) {
                    onError?.invoke("Permission: ${dto.module}", e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke("Permissions", e)
        }
    }

    // ==================== Synchronisation Suppliers ====================

    private suspend fun syncSuppliersToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localSuppliers = sdk.supplierRepository.getAll()
            localSuppliers.forEach { supplier ->
                try {
                    val dto = SupplierDto.fromModel(supplier)
                    supplierRepo.upsertSupplier(dto)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync supplier ${supplier.name}: ${e.message}")
                    onError?.invoke("Supplier: ${supplier.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync suppliers: ${e.message}")
            onError?.invoke("Suppliers", e)
        }
    }

    private suspend fun syncSuppliersFromRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val remoteSuppliers = supplierRepo.getAllSuppliers()
            remoteSuppliers.forEach { dto ->
                try {
                    sdk.supplierRepository.upsert(dto.toModel())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync supplier ${dto.name} from remote: ${e.message}")
                    onError?.invoke("Supplier: ${dto.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync suppliers from remote: ${e.message}")
            onError?.invoke("Suppliers", e)
        }
    }

    // ==================== Synchronisation Purchase Batches ====================

    private suspend fun syncPurchaseBatchesToRemote(onError: ((String, Exception) -> Unit)?) {
        try {
            val localBatches = sdk.purchaseBatchRepository.getAll()
            localBatches.forEach { batch ->
                try {
                    val dto = PurchaseBatchDto.fromModel(batch)
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
                    // Use upsert (INSERT OR REPLACE) to handle both new and existing records
                    sdk.purchaseBatchRepository.upsert(dto.toModel())
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
            val localSales = sdk.saleRepository.getAll()
            localSales.forEach { sale ->
                try {
                    // Upsert sale
                    val saleDto = SaleDto.fromModel(sale)
                    saleRepo.upsert(saleDto)

                    // Sync sale items for this sale
                    val saleItems = sdk.saleRepository.getItemsForSale(sale.id)
                    saleItems.forEach { item: com.medistock.shared.domain.model.SaleItem ->
                        val itemDto = SaleItemDto.fromModel(item)
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
                    // Use upsert (INSERT OR REPLACE) to handle both new and existing records
                    sdk.saleRepository.upsert(saleDto.toModel())

                    // Upsert sale items
                    val items = itemsBySale[saleDto.id] ?: emptyList()
                    items.forEach { itemDto ->
                        sdk.saleRepository.upsertSaleItem(itemDto.toModel())
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
            val localMovements = sdk.stockMovementRepository.getAll()
            localMovements.forEach { movement ->
                try {
                    val dto = StockMovementDto.fromModel(movement)
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
                    // Use upsert (INSERT OR REPLACE) to handle both new and existing records
                    sdk.stockMovementRepository.upsert(dto.toModel())
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
     * 2. Récupère les données de Supabase vers SQLDelight
     */
    suspend fun fullSync(
        onProgress: ((String) -> Unit)? = null,
        onError: ((String, Exception) -> Unit)? = null
    ) {
        syncLocalToRemote(onProgress, onError)
        syncRemoteToLocal(onProgress, onError)
    }
}
