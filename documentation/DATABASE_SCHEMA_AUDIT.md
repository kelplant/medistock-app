# Audit du Schéma de Base de Données MediStock

**Date de l'audit**: 2026-01-05
**Dernière mise à jour**: 2026-01-05
**Version de la base Android Room**: 12
**Base de données**: PostgreSQL (Supabase) + Room (Android SQLite)

---

## Table des Matières

1. [Vue d'ensemble](#vue-densemble)
2. [Documentation des Tables](#documentation-des-tables)
3. [Vues SQL](#vues-sql)
4. [Historique des Modifications](#historique-des-modifications)

---

## Vue d'ensemble

### Architecture
- **Backend**: Supabase (PostgreSQL)
- **Mobile**: Android Room (SQLite local)
- **Synchronisation**: Supabase Realtime API
- **Gestion de stock**: Système FIFO (First In, First Out) via `purchase_batches`

### Liste des Tables (16 tables)

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
| 10 | `stock_movements` | Mouvements de stock (historique) | ✅ Actif |
| 11 | `inventories` | Inventaires physiques | ✅ Actif |
| 12 | `product_transfers` | Transferts entre sites | ✅ Actif |
| 13 | `sales` | En-têtes de ventes | ✅ Actif |
| 14 | `sale_items` | Lignes de vente | ✅ Actif |
| 15 | `sale_batch_allocations` | Allocations FIFO | ✅ Actif |
| 16 | `audit_history` | Historique d'audit | ✅ Actif |

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
- Référencé par: `products`, `customers`, `purchase_batches`, `stock_movements`, `inventories`, `product_transfers`, `sales`, `audit_history`

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
| `updated_at` | BIGINT | Non | Timestamp de mise à jour |
| `created_by` | TEXT | Non | Utilisateur créateur |
| `updated_by` | TEXT | Non | Dernier modificateur |

**Index**: `idx_customers_site`, `idx_customers_name`

**Clé étrangère**: `site_id` → `sites(id)` ON DELETE RESTRICT

---

### 7. `products` - Produits/Médicaments

**Description**: Catalogue des produits avec informations de conditionnement et marge.

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| `id` | UUID | Non | Identifiant unique (PK) |
| `name` | TEXT | Non | Nom du produit |
| `unit` | TEXT | Non | Unité de base |
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

**Description**: Gestion des lots d'achat pour le suivi FIFO du stock et des coûts. **Source de vérité pour le stock actuel**.

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

**Usage**: Le stock actuel est calculé via `SUM(remaining_quantity)` des lots non épuisés. C'est la **source de vérité** pour le stock.

---

### 10. `stock_movements` - Mouvements de Stock

**Description**: Enregistrement des entrées/sorties de stock pour l'historique et le reporting.

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

**Note**: Cette table sert principalement à l'historique des mouvements. Le stock réel est calculé via `purchase_batches`.

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

**Note**: Le champ `customer_name` est dénormalisé pour garder l'historique même si le client est modifié.

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
| `created_at` | BIGINT | Non | Timestamp de création |
| `created_by` | TEXT | Non | Utilisateur créateur |

**Index**: `idx_sale_items_sale`, `idx_sale_items_product`

**Clés étrangères**:
- `sale_id` → `sales(id)` ON DELETE CASCADE
- `product_id` → `products(id)` ON DELETE RESTRICT

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
| `created_by` | TEXT | Non | Utilisateur créateur |

**Index**: `idx_sale_batch_allocations_sale_item`, `idx_sale_batch_allocations_batch`

**Usage**: Permet de calculer la marge réelle par vente en utilisant le coût FIFO.

---

### 16. `audit_history` - Historique d'Audit

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

## Vues SQL

### `current_stock` - Stock Actuel

Vue calculant le stock actuel par produit et site à partir des `purchase_batches`.

```sql
SELECT
    p.id as product_id,
    p.name as product_name,
    p.description,
    p.site_id,
    s.name as site_name,
    COALESCE(SUM(pb.remaining_quantity), 0) as current_stock,
    p.min_stock,
    p.max_stock,
    CASE
        WHEN COALESCE(SUM(pb.remaining_quantity), 0) <= p.min_stock THEN 'LOW'
        WHEN COALESCE(SUM(pb.remaining_quantity), 0) >= p.max_stock THEN 'HIGH'
        ELSE 'NORMAL'
    END as stock_status
FROM products p
LEFT JOIN purchase_batches pb ON p.id = pb.product_id AND pb.is_exhausted = FALSE
LEFT JOIN sites s ON p.site_id = s.id
GROUP BY p.id, p.name, p.description, p.site_id, s.name, p.min_stock, p.max_stock;
```

### `transaction_flat_view` - Vue Plate des Transactions

Vue consolidée de toutes les transactions (achats, ventes, transferts, corrections) pour le reporting (ex: Looker Studio).

Inclut les types de transaction:
- `PURCHASE` - Achats (depuis `purchase_batches`)
- `SALE` - Ventes (depuis `sale_items` + `sales`)
- `TRANSFER_OUT` - Transferts sortants
- `TRANSFER_IN` - Transferts entrants
- `INVENTORY_ADJUST` - Corrections d'inventaire

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
       │    ┌─────────────────┐     ┌─────────────────────────┐
       └────│  audit_history  │     │      app_users          │
            └─────────────────┘     └────────────┬────────────┘
                                                 │
                                    ┌────────────▼────────────┐
                                    │   user_permissions      │
                                    └─────────────────────────┘
```

---

## Historique des Modifications

### 2026-01-05 - Nettoyage du Schéma (Migration `2026010501_schema_cleanup.sql`)

**Changements effectués:**

1. **Table `product_sales` supprimée**
   - Ancienne table de ventes simplifiée, remplacée par `sales` + `sale_items` + `sale_batch_allocations`
   - Suppression de l'entité Kotlin, du DAO, du repository et du DTO

2. **Champs d'audit ajoutés:**
   - `customers`: Ajout de `updated_at`, `updated_by`
   - `sale_items`: Ajout de `created_at`, `created_by`
   - `sale_batch_allocations`: Ajout de `created_by`

3. **Triggers créés:**
   - `update_customers_updated_at` - Mise à jour automatique de `updated_at`
   - `set_customers_audit_defaults` - Gestion de `created_by`/`updated_by`
   - `set_sale_items_created_by` - Gestion de `created_by`
   - `set_sale_batch_allocations_created_by` - Gestion de `created_by`

4. **Version Room Android:** Incrémentée à 12

**Fichiers modifiés:**
- `supabase/init.sql` - Schéma initial mis à jour
- `supabase/migration/2026010501_schema_cleanup.sql` - Migration créée
- `app/src/main/java/com/medistock/data/db/AppDatabase.kt`
- `app/src/main/java/com/medistock/data/entities/*.kt`
- `app/src/main/java/com/medistock/data/remote/dto/*.kt`

---

## Notes Techniques

### Calcul du Stock

Le stock actuel est calculé **exclusivement** via la table `purchase_batches`:

```kotlin
// Méthode recommandée
fun getTotalRemainingQuantity(productId: String, siteId: String): Double? {
    return purchaseBatchDao.getTotalRemainingQuantity(productId, siteId)
}
```

La table `stock_movements` est conservée pour:
- L'historique des mouvements
- Le reporting
- La traçabilité

### Timestamps

Tous les timestamps sont stockés en **millisecondes Unix** (BIGINT).

```kotlin
val createdAt: Long = System.currentTimeMillis()
```

### Identifiants

Tous les identifiants sont des **UUID v4** générés côté client.

---

*Document généré et maintenu par l'équipe de développement MediStock*
