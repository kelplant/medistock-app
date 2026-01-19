# 📘 Guide d'Intégration Supabase - Medistock Android

## 🎯 Vue d'ensemble

Ce guide explique comment utiliser l'intégration Supabase dans l'application Medistock Android.

## ✅ Prérequis

1. **Configurer les credentials Supabase** dans `SupabaseConfig.kt` :
   ```kotlin
   const val SUPABASE_URL = "https://xxxxxxxxxxxxx.supabase.co"
   const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   ```

2. **Initialiser le client** au démarrage de l'app (dans `Application.onCreate()` ou `MainActivity.onCreate()`) :
   ```kotlin
   SupabaseClientProvider.initialize()
   ```

---

## 📚 Utilisation des Repositories

### 1️⃣ Sites

```kotlin
class ExampleViewModel : ViewModel() {
    private val siteRepository = SiteSupabaseRepository()

    fun loadSites() {
        viewModelScope.launch {
            try {
                // Récupérer tous les sites
                val sites = siteRepository.getAllSites()

                // Créer un nouveau site
                val newSite = SiteDto(
                    name = "Pharmacie Centrale",
                    createdBy = "admin"
                )
                val created = siteRepository.createSite(newSite)

                // Rechercher par nom
                val results = siteRepository.searchByName("Centrale")

                // Mettre à jour
                val updated = siteRepository.updateSite(
                    created.id,
                    created.copy(name = "Pharmacie Centrale Modifiée")
                )

                // Supprimer
                siteRepository.deleteSite(created.id)

            } catch (e: Exception) {
                Log.e("Sites", "Erreur: ${e.message}")
            }
        }
    }
}
```

### 2️⃣ Produits

```kotlin
class ProductViewModel : ViewModel() {
    private val productRepository = ProductSupabaseRepository()
    private val priceRepository = ProductPriceSupabaseRepository()

    fun loadProducts(siteId: Long) {
        viewModelScope.launch {
            try {
                // Récupérer les produits d'un site
                val products = productRepository.getProductsBySite(siteId)

                // Créer un produit
                val newProduct = ProductDto(
                    name = "Paracétamol 500mg",
                    unit = "Comprimés",
                    unitVolume = 1.0,
                    categoryId = 1,
                    siteId = siteId,
                    minStock = 100.0,
                    maxStock = 1000.0,
                    createdBy = "admin"
                )
                val created = productRepository.createProduct(newProduct)

                // Ajouter un prix
                val price = ProductPriceDto(
                    productId = created.id,
                    effectiveDate = System.currentTimeMillis(),
                    purchasePrice = 50.0,
                    sellingPrice = 100.0,
                    source = "manual",
                    createdBy = "admin"
                )
                priceRepository.createPrice(price)

                // Récupérer le prix actuel
                val currentPrice = priceRepository.getCurrentPrice(created.id)

            } catch (e: Exception) {
                Log.e("Products", "Erreur: ${e.message}")
            }
        }
    }
}
```

### 3️⃣ Stock et Lots d'Achat (FIFO)

```kotlin
class StockViewModel : ViewModel() {
    private val batchRepository = PurchaseBatchSupabaseRepository()
    private val stockRepository = CurrentStockRepository()

    fun createPurchase(productId: Long, siteId: Long) {
        viewModelScope.launch {
            try {
                // Créer un lot d'achat
                val batch = PurchaseBatchDto(
                    productId = productId,
                    siteId = siteId,
                    batchNumber = "LOT-2024-001",
                    purchaseDate = System.currentTimeMillis(),
                    initialQuantity = 500.0,
                    remainingQuantity = 500.0,
                    purchasePrice = 50.0,
                    supplierName = "Fournisseur XYZ",
                    expiryDate = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000),
                    createdBy = "admin"
                )
                val created = batchRepository.createBatch(batch)

                // Récupérer les lots actifs pour FIFO
                val activeBatches = batchRepository.getActiveBatchesByProduct(productId)

                // Vérifier le stock actuel
                val currentStock = stockRepository.getStockByProduct(productId)
                println("Stock actuel: ${currentStock?.currentStock}")

                // Vérifier les produits en rupture
                val lowStock = stockRepository.getLowStockProducts(siteId)

                // Vérifier les lots qui expirent bientôt
                val expiring = batchRepository.getExpiringBatches(30) // 30 jours

            } catch (e: Exception) {
                Log.e("Stock", "Erreur: ${e.message}")
            }
        }
    }
}
```

### 4️⃣ Ventes

```kotlin
class SalesViewModel : ViewModel() {
    private val saleRepository = SaleSupabaseRepository()
    private val saleItemRepository = SaleItemSupabaseRepository()
    private val allocationRepository = SaleBatchAllocationSupabaseRepository()

    fun createSale(siteId: Long, customerId: Long?) {
        viewModelScope.launch {
            try {
                // Créer la vente
                val sale = SaleDto(
                    customerName = "Client ABC",
                    customerId = customerId,
                    date = System.currentTimeMillis(),
                    totalAmount = 1500.0,
                    siteId = siteId,
                    createdBy = "admin"
                )
                val createdSale = saleRepository.createSale(sale)

                // Ajouter des lignes de vente
                val items = listOf(
                    SaleItemDto(
                        saleId = createdSale.id,
                        productId = 1,
                        productName = "Paracétamol 500mg",
                        unit = "Comprimés",
                        quantity = 30.0,
                        pricePerUnit = 50.0,
                        subtotal = 1500.0
                    )
                )

                items.forEach { item ->
                    val createdItem = saleItemRepository.createSaleItem(item)

                    // Allouer les lots FIFO (normalement fait par Edge Function)
                    val allocation = SaleBatchAllocationDto(
                        saleItemId = createdItem.id,
                        batchId = 1, // Le lot le plus ancien
                        quantityAllocated = 30.0,
                        purchasePriceAtAllocation = 50.0
                    )
                    allocationRepository.createAllocation(allocation)
                }

                // Récupérer les ventes du jour
                val todaySales = saleRepository.getTodaySales(siteId)

                // Récupérer les ventes sur une période
                val startDate = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
                val endDate = System.currentTimeMillis()
                val weeklySales = saleRepository.getSalesByDateRange(startDate, endDate, siteId)

            } catch (e: Exception) {
                Log.e("Sales", "Erreur: ${e.message}")
            }
        }
    }
}
```

### 5️⃣ Utilisateurs et Permissions

```kotlin
class UserViewModel : ViewModel() {
    private val userRepository = UserSupabaseRepository()
    private val permissionRepository = UserPermissionSupabaseRepository()

    fun createUser(username: String, password: String) {
        viewModelScope.launch {
            try {
                // Vérifier si le username existe déjà
                if (userRepository.usernameExists(username)) {
                    println("Username déjà pris")
                    return@launch
                }

                // Hasher le mot de passe
                val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

                // Créer l'utilisateur
                val user = AppUserDto(
                    username = username,
                    password = hashedPassword,
                    fullName = "Nom Complet",
                    isAdmin = false,
                    isActive = true,
                    createdBy = "admin"
                )
                val created = userRepository.createUser(user)

                // Ajouter des permissions
                val permissions = listOf(
                    UserPermissionDto(
                        userId = created.id,
                        module = "products",
                        canView = true,
                        canCreate = true,
                        canEdit = true,
                        canDelete = false,
                        createdBy = "admin"
                    ),
                    UserPermissionDto(
                        userId = created.id,
                        module = "sales",
                        canView = true,
                        canCreate = true,
                        canEdit = false,
                        canDelete = false,
                        createdBy = "admin"
                    )
                )

                permissions.forEach { perm ->
                    permissionRepository.createPermission(perm)
                }

                // Vérifier une permission
                val canEditProducts = permissionRepository.hasPermission(
                    created.id,
                    "products",
                    "edit"
                )
                println("Peut éditer les produits: $canEditProducts")

            } catch (e: Exception) {
                Log.e("Users", "Erreur: ${e.message}")
            }
        }
    }
}
```

### 6️⃣ Inventaires

```kotlin
class InventoryViewModel : ViewModel() {
    private val inventoryRepository = InventorySupabaseRepository()

    fun performInventory(productId: Long, siteId: Long) {
        viewModelScope.launch {
            try {
                // Créer un inventaire
                val inventory = InventoryDto(
                    productId = productId,
                    siteId = siteId,
                    countDate = System.currentTimeMillis(),
                    countedQuantity = 450.0,
                    theoreticalQuantity = 500.0,
                    discrepancy = -50.0, // Perte de 50 unités
                    reason = "Péremption",
                    countedBy = "admin",
                    notes = "Lot expiré détruit",
                    createdBy = "admin"
                )
                val created = inventoryRepository.createInventory(inventory)

                // Récupérer les inventaires avec écarts
                val withDiscrepancy = inventoryRepository.getInventoriesWithDiscrepancy(siteId)

                // Récupérer le dernier inventaire
                val latest = inventoryRepository.getLatestInventory(productId, siteId)

            } catch (e: Exception) {
                Log.e("Inventory", "Erreur: ${e.message}")
            }
        }
    }
}
```

### 7️⃣ Transferts entre Sites

```kotlin
class TransferViewModel : ViewModel() {
    private val transferRepository = ProductTransferSupabaseRepository()

    fun transferProduct(productId: Long, fromSiteId: Long, toSiteId: Long, quantity: Double) {
        viewModelScope.launch {
            try {
                // Créer un transfert
                val transfer = ProductTransferDto(
                    productId = productId,
                    quantity = quantity,
                    fromSiteId = fromSiteId,
                    toSiteId = toSiteId,
                    date = System.currentTimeMillis(),
                    notes = "Transfert de stock inter-sites",
                    createdBy = "admin"
                )
                val created = transferRepository.createTransfer(transfer)

                // Récupérer tous les transferts d'un site
                val siteTransfers = transferRepository.getTransfersBySite(fromSiteId)

                // Récupérer les transferts entre deux sites
                val betweenSites = transferRepository.getTransfersBetweenSites(fromSiteId, toSiteId)

            } catch (e: Exception) {
                Log.e("Transfer", "Erreur: ${e.message}")
            }
        }
    }
}
```

### 8️⃣ Audit et Historique

```kotlin
class AuditViewModel : ViewModel() {
    private val auditRepository = AuditHistorySupabaseRepository()

    fun logChange() {
        viewModelScope.launch {
            try {
                // Créer une entrée d'audit
                val audit = AuditHistoryDto(
                    entityType = "Product",
                    entityId = 1,
                    actionType = "UPDATE",
                    fieldName = "price",
                    oldValue = "50.0",
                    newValue = "55.0",
                    changedBy = "admin",
                    siteId = 1,
                    description = "Mise à jour du prix de vente"
                )
                auditRepository.createAuditEntry(audit)

                // Récupérer l'historique d'une entité
                val productHistory = auditRepository.getAuditHistoryByEntity("Product", 1)

                // Récupérer l'historique d'un utilisateur
                val userActions = auditRepository.getAuditHistoryByUser("admin")

                // Récupérer les dernières modifications
                val recent = auditRepository.getRecentAuditHistory(20)

                // Purger l'ancien historique (plus de 3 ans)
                val threeYearsAgo = System.currentTimeMillis() - (3L * 365 * 24 * 60 * 60 * 1000)
                auditRepository.purgeOldAuditHistory(threeYearsAgo)

            } catch (e: Exception) {
                Log.e("Audit", "Erreur: ${e.message}")
            }
        }
    }
}
```

---

## 🔄 Synchronisation Temps Réel (Realtime)

```kotlin
class RealtimeViewModel : ViewModel() {
    private val supabase = SupabaseClientProvider.client

    fun observeProducts() {
        viewModelScope.launch {
            try {
                supabase.from("products").realtime().listen { change ->
                    when (change) {
                        is Realtime.Insert -> {
                            val newProduct = change.record.decodeAs<ProductDto>()
                            println("Nouveau produit: ${newProduct.name}")
                            // Mettre à jour l'UI
                        }
                        is Realtime.Update -> {
                            val updatedProduct = change.record.decodeAs<ProductDto>()
                            println("Produit modifié: ${updatedProduct.name}")
                            // Mettre à jour l'UI
                        }
                        is Realtime.Delete -> {
                            val deletedId = change.oldRecord["id"]
                            println("Produit supprimé: $deletedId")
                            // Mettre à jour l'UI
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Realtime", "Erreur: ${e.message}")
            }
        }
    }
}
```

---

## 🚨 Gestion des Erreurs

```kotlin
class SafeViewModel : ViewModel() {
    private val productRepository = ProductSupabaseRepository()

    fun safeLoadProducts() {
        viewModelScope.launch {
            try {
                val products = productRepository.getAllProducts()
                // Succès
            } catch (e: io.ktor.client.plugins.ClientRequestException) {
                // Erreur 4xx (Bad Request, Unauthorized, etc.)
                Log.e("Error", "Erreur client: ${e.message}")
            } catch (e: io.ktor.client.plugins.ServerResponseException) {
                // Erreur 5xx (Internal Server Error, etc.)
                Log.e("Error", "Erreur serveur: ${e.message}")
            } catch (e: Exception) {
                // Autres erreurs (réseau, etc.)
                Log.e("Error", "Erreur: ${e.message}")
            }
        }
    }
}
```

---

## 📊 Résumé des Repositories Disponibles

| Repository | Table | Fonctionnalités |
|-----------|-------|-----------------|
| `SiteSupabaseRepository` | sites | CRUD + recherche |
| `CategorySupabaseRepository` | categories | CRUD + recherche |
| `PackagingTypeSupabaseRepository` | packaging_types | CRUD + actifs + tri |
| `CustomerSupabaseRepository` | customers | CRUD + par site + recherche |
| `UserSupabaseRepository` | app_users | CRUD + par username + actifs |
| `UserPermissionSupabaseRepository` | user_permissions | CRUD + vérification permissions |
| `ProductSupabaseRepository` | products | CRUD + par site/catégorie + recherche |
| `ProductPriceSupabaseRepository` | product_prices | CRUD + historique + prix actuel |
| `CurrentStockRepository` | current_stock (vue) | Lecture stock temps réel |
| `PurchaseBatchSupabaseRepository` | purchase_batches | CRUD + FIFO + expiration |
| `StockMovementSupabaseRepository` | stock_movements | Création + par produit/site/type |
| `InventorySupabaseRepository` | inventories | CRUD + écarts + dernier inventaire |
| `ProductTransferSupabaseRepository` | product_transfers | CRUD + par site + entre sites |
| `SaleSupabaseRepository` | sales | CRUD + par site/client/période |
| `SaleItemSupabaseRepository` | sale_items | CRUD + par vente/produit |
| `SaleBatchAllocationSupabaseRepository` | sale_batch_allocations | CRUD FIFO allocations |
| `ProductSaleSupabaseRepository` | product_sales | CRUD ancien système |
| `AuditHistorySupabaseRepository` | audit_history | Création + historique complet |

---

## 🔑 Points Importants

1. **Toutes les opérations sont asynchrones** : Utilisez `viewModelScope.launch` ou `lifecycleScope.launch`
2. **Gestion des erreurs** : Toujours entourer les appels de `try/catch`
3. **Credentials** : Ne jamais commiter les vraies credentials dans le code
4. **RLS** : Les politiques Row Level Security sont activées - configurez-les selon vos besoins
5. **Performance** : Utilisez les filtres pour limiter les données récupérées
6. **Realtime** : Désabonnez-vous des channels quand vous quittez l'écran

---

**🎉 Votre intégration Supabase est complète !**

Pour toute question ou amélioration, consultez la [documentation Supabase](https://supabase.com/docs).
