# Règles Métier MediStock

> Document de référence pour les règles métier. Toute implémentation (Android/iOS/shared) doit respecter ces règles.

## Principes fondamentaux

### 1. Stock négatif autorisé
- **Règle**: Une vente ne doit JAMAIS être bloquée par un stock insuffisant
- **Comportement**: Avertissement non bloquant si stock < quantité demandée
- **Justification**: La disponibilité commerciale prime sur la cohérence comptable immédiate

### 2. Source de vérité unique
- **Règle**: Toutes les règles métier vivent dans le module `shared`
- **Comportement**: Android et iOS appellent les mêmes UseCases
- **Justification**: Éviter les divergences de comportement entre plateformes

### 3. Traçabilité complète
- **Règle**: Toute mutation de données génère une entrée d'audit
- **Comportement**: Audit automatique via les UseCases
- **Justification**: Conformité et débogage

### 4. FIFO (First In First Out)
- **Règle**: Les lots sont consommés par ordre de date d'achat (plus ancien en premier)
- **Comportement**: Allocation automatique lors des ventes et transferts
- **Justification**: Gestion des dates d'expiration et traçabilité des coûts

---

## Calcul du Stock

### Méthode de calcul
Le stock est calculé dynamiquement à partir des mouvements:

```sql
Stock = SUM(mouvements "in") - SUM(mouvements "out")
```

### Types de mouvements
| Type | Direction | Description |
|------|-----------|-------------|
| `in` | Entrée (+) | Achat, réception transfert, ajustement positif |
| `out` | Sortie (-) | Vente, envoi transfert, ajustement négatif |

### Requête type
```sql
SELECT
    COALESCE(
        SUM(CASE WHEN sm.type = 'in' THEN sm.quantity ELSE 0 END) -
        SUM(CASE WHEN sm.type = 'out' THEN sm.quantity ELSE 0 END),
        0
    ) as quantityOnHand
FROM stock_movements sm
WHERE sm.product_id = :productId AND sm.site_id = :siteId
```

---

## Workflows Métier

### Achats (Purchase)

**Flux:**
```
1. Validation des données d'entrée
   - productId requis et valide
   - siteId requis et valide
   - quantity > 0
   - purchasePrice > 0
   - supplierName optionnel

2. Calcul du prix de vente (automatique)
   - Si marginType = "fixed": sellingPrice = purchasePrice + marginValue
   - Si marginType = "percentage": sellingPrice = purchasePrice * (1 + marginValue/100)

3. Création PurchaseBatch
   - id = UUID généré
   - initialQuantity = quantity
   - remainingQuantity = quantity
   - isExhausted = false
   - batchNumber = généré ou fourni

4. Création StockMovement
   - type = "in"
   - quantity = quantity
   - purchasePriceAtMovement = purchasePrice
   - sellingPriceAtMovement = sellingPrice calculé

5. Écriture Audit
   - action = "CREATE"
   - entityType = "PURCHASE_BATCH"
   - entityId = purchaseBatch.id
```

**Entité PurchaseBatch:**
```kotlin
data class PurchaseBatch(
    val id: String,
    val productId: String,
    val siteId: String,
    val batchNumber: String? = null,
    val purchaseDate: Long,
    val initialQuantity: Double,
    val remainingQuantity: Double,  // Mis à jour par FIFO
    val purchasePrice: Double,
    val supplierName: String = "",
    val expiryDate: Long? = null,
    val isExhausted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String,
    val updatedBy: String
)
```

**Règles spécifiques:**
- Un lot peut avoir une date d'expiration optionnelle
- Le numéro de lot (batchNumber) est optionnel
- Le prix de vente est calculé automatiquement via la marge du produit
- `remainingQuantity` est décrémenté lors des ventes (FIFO)
- `isExhausted = true` quand `remainingQuantity <= 0`

---

### Ventes (Sale) - Avec allocation FIFO

**Flux:**
```
1. Validation des données d'entrée
   - siteId requis et valide
   - customerName requis (ou customerId)
   - items non vide
   - Chaque item: productId valide, quantity > 0, unitPrice > 0

2. Vérification stock (NON BLOQUANTE)
   - Pour chaque item: vérifier stock disponible
   - Si stock < quantity: WARNING (pas d'erreur)
   - Retourner liste des warnings

3. Création Sale
   - id = UUID généré
   - totalAmount = somme des (quantity * unitPrice)
   - date = timestamp actuel

4. Pour chaque item du panier:

   4a. Création SaleItem
       - totalPrice = quantity * unitPrice

   4b. Allocation FIFO des lots
       - Récupérer lots non épuisés triés par purchaseDate ASC
       - Pour chaque lot jusqu'à couvrir la quantité:
         * quantityFromBatch = min(lot.remainingQuantity, quantityRestante)
         * Créer SaleBatchAllocation
         * Décrémenter lot.remainingQuantity
         * Si lot.remainingQuantity <= 0: lot.isExhausted = true
       - Calculer prix d'achat moyen pondéré

   4c. Création StockMovement
       - type = "out"
       - quantity = item.quantity
       - purchasePriceAtMovement = prix moyen pondéré des lots

5. Écriture Audit
   - action = "CREATE"
   - entityType = "SALE"
   - entityId = sale.id
```

**Entités:**

```kotlin
data class Sale(
    val id: String,
    val customerName: String,
    val customerId: String? = null,
    val date: Long,
    val totalAmount: Double,
    val siteId: String,
    val createdAt: Long,
    val createdBy: String
)

data class SaleItem(
    val id: String,
    val saleId: String,
    val productId: String,
    val quantity: Double,
    val unitPrice: Double,      // Prix de vente unitaire
    val totalPrice: Double      // quantity * unitPrice
)

data class SaleBatchAllocation(
    val id: String,
    val saleItemId: String,
    val batchId: String,
    val quantityAllocated: Double,
    val purchasePriceAtAllocation: Double  // Pour traçabilité coût
)
```

**Algorithme FIFO détaillé:**
```kotlin
fun allocateBatchesFIFO(
    productId: String,
    siteId: String,
    quantityNeeded: Double
): List<BatchAllocation> {
    val batches = getBatchesOrderedByPurchaseDate(productId, siteId)
        .filter { !it.isExhausted && it.remainingQuantity > 0 }

    var remaining = quantityNeeded
    val allocations = mutableListOf<BatchAllocation>()

    for (batch in batches) {
        if (remaining <= 0) break

        val allocated = minOf(batch.remainingQuantity, remaining)
        allocations.add(BatchAllocation(batch.id, allocated, batch.purchasePrice))
        remaining -= allocated
    }

    // Si remaining > 0, stock insuffisant mais on continue (stock négatif autorisé)
    return allocations
}
```

**Règles spécifiques:**
- Stock négatif autorisé (principe #1)
- Le client (Customer) est optionnel mais customerName requis
- L'allocation FIFO permet de tracer le coût réel de chaque vente
- Le prix d'achat moyen pondéré est enregistré dans le mouvement

---

### Transferts (Transfer) - FIFO inter-sites

**Flux:**
```
1. Validation des données d'entrée
   - fromSiteId requis et valide
   - toSiteId requis et valide
   - fromSiteId != toSiteId (ERREUR si égaux)
   - productId requis et valide
   - quantity > 0

2. Vérification stock source (NON BLOQUANTE)
   - Si stock source < quantity: WARNING (pas d'erreur)

3. Création ProductTransfer
   - id = UUID généré
   - date = timestamp actuel

4. Transfert FIFO des lots
   - Récupérer lots source triés par purchaseDate ASC
   - Pour chaque lot jusqu'à couvrir la quantité:
     * quantityFromBatch = min(lot.remainingQuantity, quantityRestante)
     * Décrémenter lot.remainingQuantity sur site source
     * Créer NOUVEAU lot sur site destination avec:
       - Même purchaseDate (préserve l'ordre FIFO)
       - batchNumber = original + "-TRANSFER"
       - Même purchasePrice
       - initialQuantity = remainingQuantity = quantityTransférée
     * Si lot source épuisé: isExhausted = true

5. Création StockMovements (2 entrées)
   - Site source: type = "out", quantity = quantity
   - Site destination: type = "in", quantity = quantity
   - purchasePriceAtMovement = prix moyen pondéré

6. Écriture Audit
   - action = "CREATE"
   - entityType = "TRANSFER"
   - entityId = transfer.id
```

**Entité:**
```kotlin
data class ProductTransfer(
    val id: String,
    val productId: String,
    val quantity: Double,
    val fromSiteId: String,
    val toSiteId: String,
    val date: Long,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String,
    val updatedBy: String
)
```

**Règles spécifiques:**
- Un transfert génère toujours 2 mouvements de stock
- Le stock source peut devenir négatif (principe #1)
- Les lots transférés conservent leur date d'achat originale
- Le batchNumber est suffixé avec "-TRANSFER" pour traçabilité

---

### Inventaires (Inventory)

**Flux:**
```
1. Validation des données d'entrée
   - siteId requis et valide
   - productId requis et valide
   - countedQuantity >= 0 (peut être zéro)

2. Calcul de l'écart
   - theoreticalQuantity = stock calculé actuel
   - discrepancy = countedQuantity - theoreticalQuantity

3. Création Inventory
   - id = UUID généré
   - countDate = timestamp actuel

4. Ajustement du stock (si discrepancy != 0)
   - Si discrepancy > 0 (surplus): type = "in"
   - Si discrepancy < 0 (manque): type = "out"
   - quantity = abs(discrepancy)

5. Création StockMovement (si ajustement)
   - type = "in" ou "out" selon discrepancy
   - quantity = abs(discrepancy)
   - notes = "Ajustement inventaire"

6. Écriture Audit
   - action = "CREATE"
   - entityType = "INVENTORY"
   - entityId = inventory.id
```

**Entité:**
```kotlin
data class Inventory(
    val id: String,
    val productId: String,
    val siteId: String,
    val countDate: Long,
    val countedQuantity: Double,
    val theoreticalQuantity: Double,
    val discrepancy: Double,  // countedQuantity - theoreticalQuantity
    val reason: String = "",
    val countedBy: String = "",
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String,
    val updatedBy: String
)
```

**Règles spécifiques:**
- L'inventaire compare le comptage physique au stock théorique
- Un ajustement automatique est créé si écart détecté
- Un inventaire avec discrepancy = 0 est valide (confirmation du stock)
- La raison de l'écart doit être documentée

---

### Mouvements de Stock (StockMovement)

**Entité:**
```kotlin
data class StockMovement(
    val id: String,
    val productId: String,
    val siteId: String,
    val type: String,                    // "in" ou "out"
    val quantity: Double,
    val movementType: String,            // "PURCHASE", "SALE", "TRANSFER_IN", "TRANSFER_OUT", "INVENTORY", "MANUAL"
    val referenceId: String? = null,     // ID de la transaction source
    val purchasePriceAtMovement: Double,
    val sellingPriceAtMovement: Double,
    val notes: String? = null,
    val createdAt: Long,
    val createdBy: String
)
```

**Types de mouvement (movementType):**
| Type | Direction (type) | Description |
|------|------------------|-------------|
| PURCHASE | in | Achat de stock |
| SALE | out | Vente |
| TRANSFER_IN | in | Réception de transfert |
| TRANSFER_OUT | out | Envoi de transfert |
| INVENTORY | in/out | Ajustement d'inventaire |
| MANUAL | in/out | Mouvement manuel |

---

## Entités et Validations

### Produit (Product)

```kotlin
data class Product(
    val id: String,
    val name: String,                    // Requis, max 255 chars
    val unit: String,                    // Requis (depuis PackagingType)
    val unitVolume: Double,              // Requis, > 0
    val packagingTypeId: String? = null,
    val selectedLevel: Int? = null,      // 1 ou 2
    val conversionFactor: Double? = null,// Override spécifique produit
    val categoryId: String? = null,
    val marginType: String? = null,      // "fixed" ou "percentage"
    val marginValue: Double? = null,     // >= 0
    val description: String? = null,
    val siteId: String? = null,          // Site par défaut
    val minStock: Double? = 0.0,         // Seuil d'alerte bas
    val maxStock: Double? = 0.0,         // Seuil d'alerte haut
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String,
    val updatedBy: String
)
```

**Calcul du prix de vente:**
```kotlin
fun calculateSellingPrice(purchasePrice: Double, product: Product): Double {
    return when (product.marginType) {
        "fixed" -> purchasePrice + (product.marginValue ?: 0.0)
        "percentage" -> purchasePrice * (1 + (product.marginValue ?: 0.0) / 100)
        else -> purchasePrice
    }
}
```

### Site

```kotlin
data class Site(
    val id: String,
    val name: String,  // Requis, unique, max 255 chars
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String,
    val updatedBy: String
)
```

### Utilisateur (User)

```kotlin
data class User(
    val id: String,
    val username: String,   // Requis, unique, max 50 chars
    val password: String,   // Hash BCrypt (JAMAIS en clair)
    val fullName: String,   // Requis, max 100 chars
    val isAdmin: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String,
    val updatedBy: String
)
```

### Client (Customer)

```kotlin
data class Customer(
    val id: String,
    val name: String,        // Requis, max 255 chars
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String,
    val updatedBy: String
)
```

### Catégorie (Category)

```kotlin
data class Category(
    val id: String,
    val name: String,  // Requis, unique
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String,
    val updatedBy: String
)
```

### Type d'emballage (PackagingType)

```kotlin
data class PackagingType(
    val id: String,
    val name: String,         // Requis
    val level1Name: String,   // Nom niveau 1 (ex: "Boîte")
    val level2Name: String?,  // Nom niveau 2 optionnel (ex: "Plaquette")
    val level2Quantity: Int?, // Quantité niveau 2 par niveau 1
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String,
    val updatedBy: String
)
```

---

## Permissions

### Modules

```kotlin
object Modules {
    const val STOCK = "STOCK"
    const val SALES = "SALES"
    const val PURCHASES = "PURCHASES"
    const val INVENTORY = "INVENTORY"
    const val TRANSFERS = "TRANSFERS"
    const val ADMIN = "ADMIN"
    const val PRODUCTS = "PRODUCTS"
    const val SITES = "SITES"
    const val CATEGORIES = "CATEGORIES"
    const val USERS = "USERS"
    const val CUSTOMERS = "CUSTOMERS"
    const val AUDIT = "AUDIT"
    const val PACKAGING_TYPES = "PACKAGING_TYPES"
}
```

### Permission par utilisateur

```kotlin
data class UserPermission(
    val id: String,
    val userId: String,
    val module: String,
    val canView: Boolean = false,
    val canCreate: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String,
    val updatedBy: String
)
```

### Vérification des permissions

```kotlin
interface PermissionChecker {
    suspend fun canView(module: String): Boolean
    suspend fun canCreate(module: String): Boolean
    suspend fun canEdit(module: String): Boolean
    suspend fun canDelete(module: String): Boolean
}
```

### Règles spéciales

- **Administrateur**: Accès complet à tous les modules (bypass des permissions)
- **Utilisateur inactif**: Aucun accès (login refusé)
- **Cache local**: Permissions cachées localement pour mode offline

---

## Synchronisation

### Stratégie

- **Mode**: Online-first, offline backup
- **Résolution de conflits**: Server wins (basé sur `updatedAt`)
- **Ordre de sync**:
  1. Sites
  2. Categories
  3. PackagingTypes
  4. Products
  5. Users
  6. Customers
  7. PurchaseBatches
  8. Sales + SaleItems + SaleBatchAllocations
  9. StockMovements
  10. Inventories
  11. Transfers

### Règles de sync

1. **Push local → remote**: Les modifications locales sont envoyées à Supabase
2. **Pull remote → local**: Les données Supabase sont synchronisées localement
3. **Filtrage realtime**: Ignorer les événements générés par le même `client_id`
4. **Queue offline**: Les opérations hors ligne sont stockées et rejouées à la reconnexion

### Optimisations de queue

| Séquence | Résultat |
|----------|----------|
| INSERT + UPDATE | INSERT (avec payload mis à jour) |
| UPDATE + UPDATE | UPDATE (dernier payload) |
| INSERT + DELETE | Annulation (rien à sync) |
| UPDATE + DELETE | DELETE seul |

---

## Audit

### Structure

```kotlin
data class AuditEntry(
    val id: String,
    val entityType: String,    // "SALE", "PURCHASE_BATCH", etc.
    val entityId: String,
    val action: String,        // "CREATE", "UPDATE", "DELETE"
    val oldValue: String?,     // JSON état précédent
    val newValue: String?,     // JSON nouvel état
    val userId: String,
    val timestamp: Long,
    val createdAt: Long
)
```

### Règles

- Toute opération via UseCase génère un audit
- Les audits ne sont jamais supprimés
- Les audits sont synchronisés comme les autres entités
- `oldValue` est null pour CREATE
- `newValue` est null pour DELETE

---

## Exceptions et Erreurs

### Exceptions métier

```kotlin
// Stock insuffisant (WARNING, pas bloquant)
class InsufficientStockWarning(
    val productId: String,
    val siteId: String,
    val requested: Double,
    val available: Double
)

// Validation échouée (bloquant)
class ValidationException(
    val field: String,
    val message: String
)

// Transfert vers même site (bloquant)
class SameSiteTransferException(
    val siteId: String
)
```

### Comportement des warnings vs erreurs

| Situation | Type | Comportement |
|-----------|------|--------------|
| Stock insuffisant pour vente | Warning | Continue, retourne warning |
| Stock insuffisant pour transfert | Warning | Continue, retourne warning |
| Champ requis manquant | Error | Bloque, lance exception |
| Site source = destination | Error | Bloque, lance exception |
| Quantité <= 0 | Error | Bloque, lance exception |

---

## Résumé des flux par opération

| Opération | Entités créées | Mouvements | Lots impactés |
|-----------|----------------|------------|---------------|
| Achat | PurchaseBatch | 1x "in" | Nouveau lot créé |
| Vente | Sale, SaleItem, SaleBatchAllocation | 1x "out" par item | FIFO décrémenté |
| Transfert | ProductTransfer | 1x "out" + 1x "in" | FIFO source → nouveau destination |
| Inventaire | Inventory | 0-1x "in"/"out" | Aucun |
| Mouvement manuel | - | 1x "in"/"out" | Aucun |
