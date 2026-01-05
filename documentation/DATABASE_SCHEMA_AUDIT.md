# Audit du Schéma de Base de Données MediStock

**Date de l'audit**: 2026-01-05
**Version de la base**: 11
**Base de données**: PostgreSQL (Supabase) + Room (Android SQLite)

---

## Table des Matières

1. [Vue d'ensemble](#vue-densemble)
2. [Documentation des Tables](#documentation-des-tables)
3. [Analyse des Problèmes Identifiés](#analyse-des-problèmes-identifiés)
4. [Tables et Champs Obsolètes](#tables-et-champs-obsolètes)
5. [Recommandations](#recommandations)

---

## Vue d'ensemble

### Architecture
- **Backend**: Supabase (PostgreSQL)
- **Mobile**: Android Room (SQLite local)
- **Synchronisation**: Supabase Realtime API
- **Gestion de stock**: Système FIFO (First In, First Out)

### Liste des Tables (17 tables)

| # | Table | Description | Statut |
|---|-------|-------------|--------|
| 1 | `sites` | Sites/pharmacies | ✅ Actif |
| 2 | `categories` | Catégories de produits | ✅ Actif |
| 3 | `packaging_types` | Types de conditionnement | ✅ Actif |
| 4 | `app_users` | Utilisateurs | ✅ Actif |
| 5 | `user_permissions` | Permissions par module | ✅ Actif |
| 6 | `customers` | Clients | ✅ Actif |
| 7 | `products` | Produits/médicaments | ✅ Actif |
| 8 | `product_prices` | Historique des prix | ✅ Actif |
| 9 | `purchase_batches` | Lots d'achat (FIFO) | ✅ Actif |
| 10 | `stock_movements` | Mouvements de stock | ⚠️ Redondant |
| 11 | `inventories` | Inventaires physiques | ✅ Actif |
| 12 | `product_transfers` | Transferts entre sites | ✅ Actif |
| 13 | `sales` | En-têtes de ventes | ✅ Actif |
| 14 | `sale_items` | Lignes de vente | ✅ Actif |
| 15 | `sale_batch_allocations` | Allocations FIFO | ✅ Actif |
| 16 | `product_sales` | Ventes produits (ancien) | ❌ Obsolète |
| 17 | `audit_history` | Historique d'audit | ✅ Actif |

---

## Documentation des Tables

### 1. `sites` - Sites/Pharmacies

**Description**: Représente les différents points de vente, pharmacies ou dépôts.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `name` | TEXT | Non | Nom du site |
| `created_at` | BIGINT | Non | Timestamp de création (ms) |
| `updated_at` | BIGINT | Non | Timestamp de mise à jour (ms) |
| `created_by` | TEXT | Non | Utilisateur créateur |
| `updated_by` | TEXT | Non | Dernier modificateur |

**Index**: `idx_sites_name` sur `name`

**Relations**:
- Référencé par: `products`, `customers`, `purchase_batches`, `stock_movements`, `inventories`, `product_transfers`, `sales`, `product_sales`, `audit_history`

---

### 2. `categories` - Catégories de Produits

**Description**: Classification des produits (Antibiotiques, Antalgiques, etc.)

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `name` | TEXT | Non | Nom de la catégorie |
| `created_at` | BIGINT | Non | Timestamp de création |
| `updated_at` | BIGINT | Non | Timestamp de mise à jour |
| `created_by` | TEXT | Non | Utilisateur créateur |
| `updated_by` | TEXT | Non | Dernier modificateur |

**Index**: `idx_categories_name` sur `name`

**Relations**:
- Référencé par: `products.category_id`

---

### 3. `packaging_types` - Types de Conditionnement

**Description**: Définit les unités de mesure à deux niveaux (ex: Boîte/Comprimés, Flacon/ml).

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `name` | TEXT | Non | Nom complet (ex: "Boîte/Comprimés") |
| `level1_name` | TEXT | Non | Unité niveau 1 (ex: "Boîte") |
| `level2_name` | TEXT | Oui | Unité niveau 2 (ex: "Comprimés") |
| `default_conversion_factor` | DOUBLE | Oui | Facteur de conversion par défaut |
| `is_active` | BOOLEAN | Non | Actif/Inactif |
| `display_order` | INTEGER | Non | Ordre d'affichage |
| `created_at` | BIGINT | Non | Timestamp de création |
| `updated_at` | BIGINT | Non | Timestamp de mise à jour |
| `created_by` | TEXT | Non | Utilisateur créateur |
| `updated_by` | TEXT | Non | Dernier modificateur |

**Index**: `idx_packaging_types_active`, `idx_packaging_types_order`

**Relations**:
- Référencé par: `products.packaging_type_id`

---

### 4. `app_users` - Utilisateurs

**Description**: Utilisateurs de l'application avec authentification.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `username` | TEXT | Non | Nom d'utilisateur (UNIQUE) |
| `password` | TEXT | Non | Hash BCrypt du mot de passe |
| `full_name` | TEXT | Non | Nom complet |
| `is_admin` | BOOLEAN | Non | Est administrateur |
| `is_active` | BOOLEAN | Non | Compte actif |
| `created_at` | BIGINT | Non | Timestamp de création |
| `updated_at` | BIGINT | Non | Timestamp de mise à jour |
| `created_by` | TEXT | Non | Utilisateur créateur |
| `updated_by` | TEXT | Non | Dernier modificateur |

**Index**: `idx_users_username`, `idx_users_active`

**Relations**:
- Référencé par: `user_permissions.user_id`

---

### 5. `user_permissions` - Permissions Utilisateurs

**Description**: Permissions granulaires par module pour chaque utilisateur.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `user_id` | UUID | Non | FK vers `app_users` |
| `module` | TEXT | Non | Nom du module |
| `can_view` | BOOLEAN | Non | Permission lecture |
| `can_create` | BOOLEAN | Non | Permission création |
| `can_edit` | BOOLEAN | Non | Permission modification |
| `can_delete` | BOOLEAN | Non | Permission suppression |
| `created_at` | BIGINT | Non | Timestamp de création |
| `updated_at` | BIGINT | Non | Timestamp de mise à jour |
| `created_by` | TEXT | Non | Utilisateur créateur |
| `updated_by` | TEXT | Non | Dernier modificateur |

**Index**: `idx_user_permissions_user`, `idx_user_permissions_module`

**Clé étrangère**: `user_id` → `app_users(id)` ON DELETE CASCADE

---

### 6. `customers` - Clients

**Description**: Base de données clients pour les ventes.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `name` | TEXT | Non | Nom du client |
| `phone` | TEXT | Oui | Numéro de téléphone |
| `address` | TEXT | Oui | Adresse |
| `notes` | TEXT | Oui | Notes additionnelles |
| `site_id` | UUID | Non | FK vers `sites` |
| `created_at` | BIGINT | Non | Timestamp de création |
| `created_by` | TEXT | Non | Utilisateur créateur |

**Index**: `idx_customers_site`, `idx_customers_name`

**Clé étrangère**: `site_id` → `sites(id)` ON DELETE RESTRICT

**Remarque**: ⚠️ Pas de champ `updated_at`/`updated_by` contrairement aux autres tables.

---

### 7. `products` - Produits/Médicaments

**Description**: Catalogue des produits avec informations de conditionnement et marge.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `name` | TEXT | Non | Nom du produit |
| `unit` | TEXT | Non | Unité de base (auto-rempli) |
| `unit_volume` | DOUBLE | Non | Facteur de conversion |
| `packaging_type_id` | UUID | Oui | FK vers `packaging_types` |
| `selected_level` | INTEGER | Oui | Niveau d'unité (1 ou 2) |
| `conversion_factor` | DOUBLE | Oui | Facteur spécifique au produit |
| `category_id` | UUID | Oui | FK vers `categories` |
| `margin_type` | TEXT | Oui | Type de marge (% ou fixe) |
| `margin_value` | DOUBLE | Oui | Valeur de la marge |
| `description` | TEXT | Oui | Description du produit |
| `site_id` | UUID | Non | FK vers `sites` |
| `min_stock` | DOUBLE | Oui | Stock minimum |
| `max_stock` | DOUBLE | Oui | Stock maximum |
| `created_at` | BIGINT | Non | Timestamp de création |
| `updated_at` | BIGINT | Non | Timestamp de mise à jour |
| `created_by` | TEXT | Non | Utilisateur créateur |
| `updated_by` | TEXT | Non | Dernier modificateur |

**Index**: `idx_products_site`, `idx_products_category`, `idx_products_packaging_type`, `idx_products_name`

**Clés étrangères**:
- `site_id` → `sites(id)` ON DELETE RESTRICT
- `category_id` → `categories(id)` ON DELETE SET NULL
- `packaging_type_id` → `packaging_types(id)` ON DELETE SET NULL

---

### 8. `product_prices` - Historique des Prix

**Description**: Suivi historique des prix d'achat et de vente par produit.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `product_id` | UUID | Non | FK vers `products` |
| `effective_date` | BIGINT | Non | Date d'effet |
| `purchase_price` | DOUBLE | Non | Prix d'achat |
| `selling_price` | DOUBLE | Non | Prix de vente |
| `source` | TEXT | Non | "manual" ou "calculated" |
| `created_at` | BIGINT | Non | Timestamp de création |
| `updated_at` | BIGINT | Non | Timestamp de mise à jour |
| `created_by` | TEXT | Non | Utilisateur créateur |
| `updated_by` | TEXT | Non | Dernier modificateur |

**Index**: `idx_product_prices_product`, `idx_product_prices_date`

**Clé étrangère**: `product_id` → `products(id)` ON DELETE CASCADE

---

### 9. `purchase_batches` - Lots d'Achat (FIFO)

**Description**: Gestion des lots d'achat pour le suivi FIFO du stock et des coûts.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `product_id` | UUID | Non | FK vers `products` |
| `site_id` | UUID | Non | FK vers `sites` |
| `batch_number` | TEXT | Oui | Numéro de lot |
| `purchase_date` | BIGINT | Non | Date d'achat |
| `initial_quantity` | DOUBLE | Non | Quantité initiale |
| `remaining_quantity` | DOUBLE | Non | Quantité restante |
| `purchase_price` | DOUBLE | Non | Prix d'achat unitaire |
| `supplier_name` | TEXT | Non | Nom du fournisseur |
| `expiry_date` | BIGINT | Oui | Date d'expiration |
| `is_exhausted` | BOOLEAN | Non | Lot épuisé |
| `created_at` | BIGINT | Non | Timestamp de création |
| `updated_at` | BIGINT | Non | Timestamp de mise à jour |
| `created_by` | TEXT | Non | Utilisateur créateur |
| `updated_by` | TEXT | Non | Dernier modificateur |

**Index**: `idx_purchase_batches_product`, `idx_purchase_batches_site`, `idx_purchase_batches_exhausted`, `idx_purchase_batches_date`

**Clés étrangères**:
- `product_id` → `products(id)` ON DELETE RESTRICT
- `site_id` → `sites(id)` ON DELETE RESTRICT

**Usage**: Le stock actuel est calculé via `SUM(remaining_quantity)` des lots non épuisés.

---

### 10. `stock_movements` - Mouvements de Stock ⚠️

**Description**: Enregistrement des entrées/sorties de stock.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `product_id` | UUID | Non | FK vers `products` |
| `type` | TEXT | Non | "IN" ou "OUT" |
| `quantity` | DOUBLE | Non | Quantité |
| `date` | BIGINT | Non | Date du mouvement |
| `purchase_price_at_movement` | DOUBLE | Non | Prix d'achat au moment |
| `selling_price_at_movement` | DOUBLE | Non | Prix de vente au moment |
| `site_id` | UUID | Non | FK vers `sites` |
| `created_at` | BIGINT | Non | Timestamp de création |
| `created_by` | TEXT | Non | Utilisateur créateur |

**Index**: `idx_stock_movements_product`, `idx_stock_movements_site`, `idx_stock_movements_type`, `idx_stock_movements_date`

**Statut**: ⚠️ **PARTIELLEMENT REDONDANT** - Voir section [Problèmes Identifiés](#analyse-des-problèmes-identifiés)

---

### 11. `inventories` - Inventaires Physiques

**Description**: Enregistrement des comptages physiques et écarts de stock.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `product_id` | UUID | Non | FK vers `products` |
| `site_id` | UUID | Non | FK vers `sites` |
| `count_date` | BIGINT | Non | Date du comptage |
| `counted_quantity` | DOUBLE | Non | Quantité comptée |
| `theoretical_quantity` | DOUBLE | Non | Quantité théorique |
| `discrepancy` | DOUBLE | Non | Écart (compté - théorique) |
| `reason` | TEXT | Non | Raison de l'écart |
| `counted_by` | TEXT | Non | Personne qui a compté |
| `notes` | TEXT | Non | Notes additionnelles |
| `created_at` | BIGINT | Non | Timestamp de création |
| `created_by` | TEXT | Non | Utilisateur créateur |

**Index**: `idx_inventories_product`, `idx_inventories_site`, `idx_inventories_date`

---

### 12. `product_transfers` - Transferts entre Sites

**Description**: Suivi des transferts de produits entre différents sites.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `product_id` | UUID | Non | FK vers `products` |
| `quantity` | DOUBLE | Non | Quantité transférée |
| `from_site_id` | UUID | Non | Site source |
| `to_site_id` | UUID | Non | Site destination |
| `date` | BIGINT | Non | Date du transfert |
| `notes` | TEXT | Non | Notes |
| `created_at` | BIGINT | Non | Timestamp de création |
| `updated_at` | BIGINT | Non | Timestamp de mise à jour |
| `created_by` | TEXT | Non | Utilisateur créateur |
| `updated_by` | TEXT | Non | Dernier modificateur |

**Index**: `idx_product_transfers_product`, `idx_product_transfers_from_site`, `idx_product_transfers_to_site`, `idx_product_transfers_date`

---

### 13. `sales` - En-têtes de Ventes

**Description**: En-tête des transactions de vente.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `customer_name` | TEXT | Non | Nom du client (dénormalisé) |
| `customer_id` | UUID | Oui | FK vers `customers` |
| `date` | BIGINT | Non | Date de la vente |
| `total_amount` | DOUBLE | Non | Montant total |
| `site_id` | UUID | Non | FK vers `sites` |
| `created_at` | BIGINT | Non | Timestamp de création |
| `created_by` | TEXT | Non | Utilisateur créateur |

**Index**: `idx_sales_customer`, `idx_sales_site`, `idx_sales_date`

**Remarque**: Le champ `customer_name` est dénormalisé pour garder l'historique même si le client est modifié.

---

### 14. `sale_items` - Lignes de Vente

**Description**: Détail des produits vendus dans chaque vente.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `sale_id` | UUID | Non | FK vers `sales` |
| `product_id` | UUID | Non | FK vers `products` |
| `product_name` | TEXT | Non | Nom du produit (dénormalisé) |
| `unit` | TEXT | Non | Unité (dénormalisé) |
| `quantity` | DOUBLE | Non | Quantité vendue |
| `price_per_unit` | DOUBLE | Non | Prix unitaire |
| `subtotal` | DOUBLE | Non | Sous-total |

**Index**: `idx_sale_items_sale`, `idx_sale_items_product`

**Clés étrangères**:
- `sale_id` → `sales(id)` ON DELETE CASCADE
- `product_id` → `products(id)` ON DELETE RESTRICT

**Remarque**: Pas de champs d'audit (`created_at`, `created_by`) contrairement aux autres tables.

---

### 15. `sale_batch_allocations` - Allocations FIFO

**Description**: Trace la correspondance entre les lignes de vente et les lots d'achat utilisés (FIFO).

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `sale_item_id` | UUID | Non | FK vers `sale_items` |
| `batch_id` | UUID | Non | FK vers `purchase_batches` |
| `quantity_allocated` | DOUBLE | Non | Quantité allouée |
| `purchase_price_at_allocation` | DOUBLE | Non | Prix d'achat au moment |
| `created_at` | BIGINT | Non | Timestamp de création |

**Index**: `idx_sale_batch_allocations_sale_item`, `idx_sale_batch_allocations_batch`

**Usage**: Permet de calculer la marge réelle par vente en utilisant le coût FIFO.

---

### 16. `product_sales` - Ventes Produits (ANCIEN) ❌

**Description**: Ancien système de ventes simplifié, remplacé par `sales` + `sale_items`.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `product_id` | UUID | Non | FK vers `products` |
| `quantity` | DOUBLE | Non | Quantité vendue |
| `price_at_sale` | DOUBLE | Non | Prix de vente |
| `farmer_name` | TEXT | Non | ⚠️ Nom du "fermier" |
| `date` | BIGINT | Non | Date de vente |
| `site_id` | UUID | Non | FK vers `sites` |
| `created_at` | BIGINT | Non | Timestamp de création |
| `created_by` | TEXT | Non | Utilisateur créateur |

**Statut**: ❌ **OBSOLÈTE** - Voir section [Tables Obsolètes](#tables-et-champs-obsolètes)

---

### 17. `audit_history` - Historique d'Audit

**Description**: Journalisation automatique de toutes les modifications sur les tables métier.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `entity_type` | TEXT | Non | Nom de la table |
| `entity_id` | UUID | Non | ID de l'entité modifiée |
| `action_type` | TEXT | Non | "INSERT", "UPDATE", "DELETE" |
| `field_name` | TEXT | Oui | Champ modifié |
| `old_value` | TEXT | Oui | Ancienne valeur (JSON) |
| `new_value` | TEXT | Oui | Nouvelle valeur (JSON) |
| `changed_by` | TEXT | Non | Utilisateur |
| `changed_at` | BIGINT | Non | Timestamp |
| `site_id` | UUID | Oui | Site contexte |
| `description` | TEXT | Oui | Description optionnelle |

**Index**: `idx_audit_history_entity`, `idx_audit_history_user`, `idx_audit_history_date`, `idx_audit_history_site`

---

## Analyse des Problèmes Identifiés

### Problème 1: Double Système de Calcul du Stock ⚠️ CRITIQUE

**Situation actuelle**:
Le stock peut être calculé de **deux manières différentes** dans le code:

1. **Via `purchase_batches`** (nouveau système FIFO):
   ```sql
   SELECT SUM(remaining_quantity) FROM purchase_batches
   WHERE product_id = ? AND site_id = ? AND is_exhausted = FALSE
   ```
   - Utilisé dans: `PurchaseBatchDao.getTotalRemainingQuantity()`
   - Utilisé dans la vue SQL `current_stock`

2. **Via `stock_movements`** (ancien système):
   ```sql
   SELECT SUM(CASE WHEN type = 'in' THEN quantity ELSE 0 END) -
          SUM(CASE WHEN type = 'out' THEN quantity ELSE 0 END)
   FROM stock_movements WHERE product_id = ? AND site_id = ?
   ```
   - Utilisé dans: `StockMovementDao.getCurrentStockForSite()`

**Impact**:
- Risque d'incohérence entre les deux calculs
- Le code crée des `StockMovement` ET met à jour les `PurchaseBatch` en parallèle
- Duplication des données de stock

**Fichiers concernés**:
- `app/src/main/java/com/medistock/ui/purchase/PurchaseActivity.kt`
- `app/src/main/java/com/medistock/ui/sales/SaleActivity.kt`
- `app/src/main/java/com/medistock/ui/transfer/TransferActivity.kt`
- `app/src/main/java/com/medistock/ui/inventory/InventoryActivity.kt`

---

### Problème 2: Table `product_sales` Obsolète ❌

**Situation**:
La table `product_sales` est l'ancien système de ventes avec:
- Un seul produit par vente
- Un champ `farmer_name` (contexte agricole initial)
- Pas de lien avec les lots (pas de FIFO)

**Nouveau système** (`sales` + `sale_items` + `sale_batch_allocations`):
- Ventes multi-produits
- Clients gérés via `customers`
- Traçabilité FIFO complète

**État du code**:
- `ProductSaleDao` existe encore avec des méthodes
- `ProductSaleSupabaseRepository` existe encore
- Le code ne semble plus créer de `ProductSale` dans les flux principaux

---

### Problème 3: Incohérences dans les Champs d'Audit

| Table | created_at | updated_at | created_by | updated_by |
|-------|------------|------------|------------|------------|
| `customers` | ✅ | ❌ | ✅ | ❌ |
| `sale_items` | ❌ | ❌ | ❌ | ❌ |
| `stock_movements` | ✅ | ❌ | ✅ | ❌ |
| `inventories` | ✅ | ❌ | ✅ | ❌ |
| `sales` | ✅ | ❌ | ✅ | ❌ |
| `sale_batch_allocations` | ✅ | ❌ | ❌ | ❌ |

**Impact**: Traçabilité incomplète des modifications.

---

### Problème 4: Champs Potentiellement Redondants dans `products`

| Champ | Description | Statut |
|-------|-------------|--------|
| `unit` | Unité de base | ⚠️ Peut être dérivé de `packaging_types.level1_name` ou `level2_name` |
| `unit_volume` | Facteur de conversion | ⚠️ Doublon avec `conversion_factor` |
| `conversion_factor` | Facteur spécifique | OK si différent de `unit_volume` |

Le commentaire dans le code indique:
```kotlin
val unit: String, // Obligatoire - rempli automatiquement depuis PackagingType
val unitVolume: Double, // Obligatoire - facteur de conversion
```

Si `unit` est rempli automatiquement, pourquoi le stocker?

---

## Tables et Champs Obsolètes

### Tables à Supprimer

#### 1. `product_sales` ❌ OBSOLÈTE

**Raison**: Remplacé par le système `sales` + `sale_items` + `sale_batch_allocations`

**Actions requises**:
1. Vérifier qu'aucun code actif n'utilise cette table
2. Migrer les données historiques si nécessaire vers `sales`/`sale_items`
3. Supprimer:
   - Entité: `ProductSale.kt`
   - DAO: `ProductSaleDao.kt`
   - Repository: `ProductSaleSupabaseRepository`
   - DTO: Dans `SalesDtos.kt`
4. Supprimer la table SQL et les triggers associés

#### 2. `stock_movements` ⚠️ À ÉVALUER

**Raison**: Redondant avec `purchase_batches` pour le calcul du stock

**Options**:
- **Option A**: Garder comme journal de traçabilité (audit trail) mais ne plus l'utiliser pour le calcul du stock
- **Option B**: Supprimer si `audit_history` suffit pour la traçabilité

**Recommandation**: Option A - garder pour compatibilité historique et reporting

---

### Champs à Évaluer

#### Dans `products`:

| Champ | Recommandation |
|-------|----------------|
| `unit` | Évaluer si peut être supprimé (dérivé de `packaging_types`) |
| `unit_volume` | Clarifier la différence avec `conversion_factor` |

#### Dans `sales`:

| Champ | Recommandation |
|-------|----------------|
| `customer_name` | Garder (dénormalisation intentionnelle pour historique) |

---

## Recommandations

### Priorité Haute

1. **Unifier le calcul du stock**
   - Décider d'une source unique: `purchase_batches.remaining_quantity`
   - Modifier `StockMovementDao` pour ne plus calculer le stock
   - Garder `stock_movements` uniquement pour le reporting/audit

2. **Supprimer `product_sales`**
   - Migration des données historiques
   - Suppression du code
   - Suppression de la table

### Priorité Moyenne

3. **Harmoniser les champs d'audit**
   - Ajouter `updated_at`/`updated_by` aux tables manquantes
   - Ajouter `created_at`/`created_by` à `sale_items`

4. **Clarifier les champs de `products`**
   - Documenter clairement la différence entre `unit`, `unit_volume`, et `conversion_factor`
   - Ou simplifier si certains sont redondants

### Priorité Basse

5. **Optimiser les index**
   - Revoir les index composites pour les requêtes fréquentes

6. **Nettoyer la vue `transaction_flat_view`**
   - Retirer la section `product_sales` une fois la table supprimée

---

## Schéma des Relations

```
┌─────────────┐     ┌─────────────────┐     ┌────────────────┐
│   sites     │◄────│    products     │────►│   categories   │
└─────────────┘     └────────┬────────┘     └────────────────┘
       ▲                     │
       │                     │              ┌────────────────┐
       │                     └─────────────►│packaging_types │
       │                                    └────────────────┘
       │
       │    ┌─────────────────┐     ┌─────────────────────────┐
       ├────│ purchase_batches│◄────│  sale_batch_allocations │
       │    └─────────────────┘     └────────────┬────────────┘
       │                                         │
       │    ┌─────────────────┐     ┌────────────▼────────────┐
       ├────│ stock_movements │     │      sale_items         │
       │    └─────────────────┘     └────────────┬────────────┘
       │                                         │
       │    ┌─────────────────┐     ┌────────────▼────────────┐
       ├────│   inventories   │     │        sales            │
       │    └─────────────────┘     └────────────┬────────────┘
       │                                         │
       │    ┌─────────────────┐     ┌────────────▼────────────┐
       ├────│product_transfers│     │      customers          │
       │    └─────────────────┘     └─────────────────────────┘
       │
       │    ┌─────────────────┐
       ├────│  product_sales  │ ❌ OBSOLÈTE
       │    └─────────────────┘
       │
       │    ┌─────────────────┐     ┌─────────────────────────┐
       └────│  audit_history  │     │      app_users          │
            └─────────────────┘     └────────────┬────────────┘
                                                 │
                                    ┌────────────▼────────────┐
                                    │   user_permissions      │
                                    └─────────────────────────┘
```

---

## Conclusion

Le schéma de base de données MediStock est globalement bien structuré avec un système FIFO moderne. Cependant, il conserve des traces d'une évolution progressive qui a laissé:

1. **Une table obsolète** (`product_sales`) qui devrait être supprimée
2. **Une redondance** dans le calcul du stock (via `purchase_batches` ET `stock_movements`)
3. **Des incohérences** dans les champs d'audit entre les tables

Les recommandations prioritaires sont:
- Unifier le calcul du stock sur `purchase_batches`
- Supprimer `product_sales` après migration des données
- Harmoniser les champs d'audit

---

*Document généré automatiquement - Audit du schéma MediStock*
