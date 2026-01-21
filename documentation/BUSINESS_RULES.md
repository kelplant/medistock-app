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

---

## Workflows Métier

### Achats (Purchase)

**Flux:**
```
1. Validation des données d'entrée
   - productId requis et valide
   - siteId requis et valide
   - quantity > 0
   - purchasePrice >= 0
   - supplierName requis

2. Création PurchaseBatch
   - id = UUID généré
   - initialQuantity = quantity
   - remainingQuantity = quantity
   - isExhausted = false

3. Mise à jour du stock
   - Stock += quantity (création si inexistant)

4. Création StockMovement
   - type = "PURCHASE"
   - quantity = +quantity
   - referenceId = purchaseBatch.id

5. Écriture Audit
   - action = "CREATE"
   - entityType = "PURCHASE_BATCH"
   - entityId = purchaseBatch.id
```

**Règles spécifiques:**
- Un lot peut avoir une date d'expiration optionnelle
- Le numéro de lot (batchNumber) est optionnel
- Le prix de vente peut être calculé automatiquement via la marge du produit

---

### Ventes (Sale)

**Flux:**
```
1. Validation des données d'entrée
   - siteId requis et valide
   - items non vide
   - Chaque item: productId valide, quantity > 0, unitPrice >= 0

2. Vérification stock (NON BLOQUANTE)
   - Pour chaque item: vérifier stock disponible
   - Si stock < quantity: WARNING (pas d'erreur)
   - Retourner liste des warnings

3. Création Sale
   - id = UUID généré
   - totalAmount = somme des items
   - date = timestamp actuel

4. Création SaleItems
   - Pour chaque item du panier
   - totalPrice = quantity * unitPrice

5. Mise à jour du stock (TOUJOURS EXÉCUTÉ)
   - Stock -= quantity (même si résultat négatif)

6. Allocation des lots (optionnel, FIFO)
   - Décrémenter remainingQuantity des lots
   - Marquer isExhausted si remainingQuantity <= 0

7. Création StockMovements
   - type = "SALE"
   - quantity = -quantity (négatif)
   - referenceId = sale.id

8. Écriture Audit
   - action = "CREATE"
   - entityType = "SALE"
   - entityId = sale.id
```

**Règles spécifiques:**
- Stock négatif autorisé (principe #1)
- Le client (Customer) est optionnel
- Les méthodes de paiement: CASH, CARD, MOBILE, CREDIT
- Statuts possibles: PENDING, COMPLETED, CANCELLED

---

### Transferts (Transfer)

**Flux:**
```
1. Validation des données d'entrée
   - sourceSiteId requis et valide
   - destinationSiteId requis et valide
   - sourceSiteId != destinationSiteId
   - productId requis et valide
   - quantity > 0

2. Vérification stock source (NON BLOQUANTE)
   - Si stock source < quantity: WARNING (pas d'erreur)

3. Création Transfer
   - id = UUID généré
   - status = "COMPLETED" (ou "PENDING" si workflow d'approbation)

4. Mise à jour des stocks
   - Stock site source -= quantity
   - Stock site destination += quantity

5. Création StockMovements (2 entrées)
   - Site source: type = "TRANSFER_OUT", quantity = -quantity
   - Site destination: type = "TRANSFER_IN", quantity = +quantity
   - referenceId = transfer.id pour les deux

6. Écriture Audit
   - action = "CREATE"
   - entityType = "TRANSFER"
   - entityId = transfer.id
```

**Règles spécifiques:**
- Un transfert génère toujours 2 mouvements de stock
- Le stock source peut devenir négatif (principe #1)

---

### Inventaires (Inventory)

**Flux:**
```
1. Validation des données d'entrée
   - siteId requis et valide
   - productId requis et valide
   - countedQuantity >= 0

2. Calcul de l'écart
   - currentStock = stock actuel du produit sur le site
   - adjustment = countedQuantity - currentStock

3. Création InventoryEntry
   - id = UUID généré
   - expectedQuantity = currentStock
   - actualQuantity = countedQuantity
   - difference = adjustment

4. Mise à jour du stock (si adjustment != 0)
   - Stock = countedQuantity (remplacement direct)

5. Création StockMovement (si adjustment != 0)
   - type = "INVENTORY_ADJUSTMENT"
   - quantity = adjustment (peut être négatif)
   - referenceId = inventoryEntry.id

6. Écriture Audit
   - action = "CREATE"
   - entityType = "INVENTORY"
   - entityId = inventoryEntry.id
```

**Règles spécifiques:**
- L'inventaire remplace la valeur du stock, pas d'ajout/retrait
- Un inventaire avec adjustment = 0 est valide (confirmation du stock)

---

### Mouvements de Stock manuels

**Types autorisés:**
- `MANUAL_IN`: Entrée manuelle (réception, correction positive)
- `MANUAL_OUT`: Sortie manuelle (perte, casse, correction négative)

**Flux:**
```
1. Validation
   - productId requis et valide
   - siteId requis et valide
   - quantity > 0
   - type in [MANUAL_IN, MANUAL_OUT]

2. Mise à jour du stock
   - Si MANUAL_IN: Stock += quantity
   - Si MANUAL_OUT: Stock -= quantity (peut devenir négatif)

3. Création StockMovement
   - type = type fourni
   - quantity = +/- selon le type
   - notes = raison du mouvement (optionnel mais recommandé)

4. Écriture Audit
   - action = "CREATE"
   - entityType = "STOCK_MOVEMENT"
```

---

## Entités et Validations

### Produit (Product)

| Champ | Requis | Validation |
|-------|--------|------------|
| id | Oui | UUID |
| name | Oui | Non vide, max 255 chars |
| unit | Oui | Non vide |
| unitVolume | Oui | > 0 |
| categoryId | Non | UUID valide si fourni |
| packagingTypeId | Non | UUID valide si fourni |
| marginType | Oui | "PERCENTAGE" ou "FIXED" |
| marginValue | Oui | >= 0 |

### Site

| Champ | Requis | Validation |
|-------|--------|------------|
| id | Oui | UUID |
| name | Oui | Non vide, unique, max 255 chars |

### Utilisateur (User)

| Champ | Requis | Validation |
|-------|--------|------------|
| id | Oui | UUID |
| username | Oui | Non vide, unique, max 50 chars |
| password | Oui | Hash BCrypt (jamais en clair) |
| fullName | Oui | Non vide, max 100 chars |
| isAdmin | Oui | Boolean |
| isActive | Oui | Boolean |

### Client (Customer)

| Champ | Requis | Validation |
|-------|--------|------------|
| id | Oui | UUID |
| name | Oui | Non vide, max 255 chars |
| phone | Non | Format téléphone valide si fourni |
| email | Non | Format email valide si fourni |

---

## Permissions

### Modules

| Module | Description |
|--------|-------------|
| STOCK | Consultation et mouvements de stock |
| SALES | Création et gestion des ventes |
| PURCHASES | Création et gestion des achats |
| INVENTORY | Réalisation des inventaires |
| TRANSFERS | Transferts inter-sites |
| PRODUCTS | Gestion des produits |
| SITES | Gestion des sites |
| CATEGORIES | Gestion des catégories |
| USERS | Gestion des utilisateurs |
| CUSTOMERS | Gestion des clients |
| AUDIT | Consultation de l'historique |
| PACKAGING_TYPES | Gestion des types d'emballage |
| ADMIN | Administration générale |

### Actions par module

Chaque module supporte 4 actions:
- `canView`: Consultation
- `canCreate`: Création
- `canEdit`: Modification
- `canDelete`: Suppression

### Règles spéciales

- **Administrateur**: Accès complet à tous les modules (bypass des permissions)
- **Utilisateur inactif**: Aucun accès (login refusé)

---

## Synchronisation

### Stratégie

- **Mode**: Online-first, offline backup
- **Résolution de conflits**: Server wins (basé sur `updatedAt`)
- **Ordre de sync**: Sites → Categories → PackagingTypes → Products → Users → Customers → PurchaseBatches → Sales → StockMovements

### Règles de sync

1. **Push local → remote**: Les modifications locales sont envoyées à Supabase
2. **Pull remote → local**: Les données Supabase sont synchronisées localement
3. **Filtrage realtime**: Ignorer les événements générés par le même `client_id`
4. **Queue offline**: Les opérations hors ligne sont stockées et rejouées à la reconnexion

### Optimisations de queue

- INSERT + UPDATE = INSERT (avec payload mis à jour)
- UPDATE + UPDATE = UPDATE (dernier payload)
- INSERT + DELETE = Annulation (pas de sync nécessaire)
- UPDATE + DELETE = DELETE seul

---

## Audit

### Structure

| Champ | Description |
|-------|-------------|
| id | UUID |
| entityType | Type d'entité (SALE, PURCHASE_BATCH, etc.) |
| entityId | ID de l'entité concernée |
| action | CREATE, UPDATE, DELETE |
| oldValue | JSON de l'état précédent (pour UPDATE/DELETE) |
| newValue | JSON du nouvel état (pour CREATE/UPDATE) |
| userId | ID de l'utilisateur ayant effectué l'action |
| timestamp | Date/heure de l'action |

### Règles

- Toute opération via UseCase génère un audit
- Les audits ne sont jamais supprimés
- Les audits sont synchronisés comme les autres entités
