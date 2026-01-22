Roadmap technique — Parité Android/iOS et consolidation `shared`

> Objectif : rendre l’application iOS conforme à Android **sans duplication de logique métier**, en centralisant tout ce qui doit l’être dans `shared`. Cette roadmap compile l’ensemble des écarts, faiblesses et recommandations identifiés.

## Principes métier (non négociables)

1. **Stock négatif autorisé** : une vente ne doit jamais être bloquée par un stock insuffisant. Un avertissement non bloquant est acceptable.
2. **Source de vérité unique** : règles métier et validations doivent vivre dans `shared`.
3. **Parité fonctionnelle** : Android et iOS doivent avoir les mêmes flux métier, mêmes règles de permissions et même comportement de sync.

---

## Phase 0 — Cadrage & spécification (1 semaine)

### Objectifs
- Aligner les règles métier et les conventions de synchronisation.
- Définir un socle partagé pour modules, permissions, compatibilité et authentification.

### Tâches
- Formaliser les **règles métier** (achats, ventes, transferts, inventaires, stock négatif).
- Définir la **stratégie de sync** (bidirectionnelle, offline-first, résolution de conflits).
- Établir une **liste unique de modules** (permissions) commune Android/iOS.
- Définir la **politique “app too old”** et le comportement de mise à jour minimum requis.

### Livrables
- Document “Business Rules & Sync Policy”.
- Enum des modules unique (spécification).

---

## Phase 1 — Centraliser la logique métier dans `shared` (3–5 semaines) ✅ TERMINÉE

> But : déplacer les workflows transactionnels dans `shared` (achats/ventes/transferts/stock/produits/sites) et ne laisser que la présentation/UX aux apps.

### 1.1. Création d'une couche **UseCases** dans `shared` ✅

**Modules implémentés :**
- ✅ `PurchaseUseCase` - Gestion des achats avec création de lots
- ✅ `SaleUseCase` - Gestion des ventes avec allocation FIFO
- ✅ `TransferUseCase` - Transferts inter-sites avec FIFO
- ✅ `InventoryUseCase` - Inventaires et ajustements de stock

**Contraintes respectées :**
- ✅ Inputs normalisés (DTO commun) : `PurchaseInput`, `SaleInput`, `TransferInput`
- ✅ Outputs = entités + warnings métier : `UseCaseResult<T>` avec `BusinessWarning`
- ✅ Stock négatif autorisé : warning non bloquant `InsufficientStock`

### 1.2. Workflow **Achats** ✅

**Implémenté dans `PurchaseUseCase` :**
- ✅ Création `PurchaseBatch`
- ✅ Création `StockMovement` type `PURCHASE`
- ✅ Calcul automatique du prix de vente (marge)
- ✅ Warning si produit expire bientôt
- ✅ Écriture d'audit

### 1.3. Workflow **Ventes** ✅

**Implémenté dans `SaleUseCase` :**
- ✅ Création `Sale` + `SaleItem`
- ✅ Décrément stock **même si insuffisant** (stock négatif autorisé)
- ✅ Création `StockMovement` type `SALE`
- ✅ Allocation FIFO des lots (oldest first)
- ✅ Calcul coût/revenu/profit
- ✅ Écriture d'audit

### 1.4. Workflow **Transferts** ✅

**Implémenté dans `TransferUseCase` :**
- ✅ Validation sites source/destination différents
- ✅ Décrément stock site A + incrément site B
- ✅ Double `StockMovement` (TRANSFER_OUT/TRANSFER_IN)
- ✅ Transfert FIFO avec préservation date d'achat
- ✅ Écriture d'audit

### 1.5. Workflow **Inventaires** ✅

**Implémenté dans `InventoryUseCase` :**
- ✅ Création inventaire
- ✅ Ajustement stock si besoin
- ✅ StockMovement type `INVENTORY`
- ✅ Audit

### 1.6. **Repositories partagés** ✅

- ✅ `ProductRepository`, `SiteRepository`, `CustomerRepository`
- ✅ `PurchaseBatchRepository`, `StockMovementRepository`
- ✅ `SaleRepository`, `AuditRepository`

### 1.7. **Audit partagé** ✅

- ✅ Toute action métier (UseCase) génère une entrée audit
- ✅ Format JSON pour les valeurs old/new

### Livrables ✅
- ✅ Ensemble des UseCases partagés
- ✅ Tests unitaires de règles métier (`UseCaseTests.kt`, `ModelTests.kt`)
- ✅ Migration iOS : tous les écrans utilisent les UseCases
- ✅ Migration Android : ViewModels utilisent les UseCases via `MedistockSDK`

---

## Phase 2 — Auth & Permissions unifiées (2–3 semaines)

### 2.1. Authentification partagée ✅
- ✅ `AuthResult` sealed class dans shared
- ✅ `PasswordVerifier` interface pour BCrypt platform-specific
- ✅ `AuthService` partagé avec authenticate()
- ✅ Android et iOS utilisent le AuthService partagé

### 2.2. Modules permissions partagés ✅
- ✅ Enum `Module` unifié dans `shared` (13 modules)
- ✅ Modèle `UserPermission` module-based (canView/Create/Edit/Delete)
- ✅ `UserPermissionRepository` dans shared
- ✅ Android et iOS utilisent les modules shared

### 2.3. Permissions offline-first ❌ NON RETENU
- ~~Définir une stratégie commune (cache local + refresh distant).~~
- *Non retenu : la stratégie actuelle (cache local iOS/Android + sync Supabase) est suffisante.*

### 2.4. Sécurisation de la configuration Supabase ✅
- ✅ iOS : `KeychainService.swift` utilisant Keychain Services
- ✅ Android : `SecureSupabasePreferences.kt` utilisant EncryptedSharedPreferences
- ✅ Migration automatique depuis l'ancien stockage non sécurisé
- ✅ Chiffrement AES-256 des credentials

### Livrables
- Auth partagée.
- Modules unifiés.
- Permissions cohérentes dans les deux apps.
- Configuration Supabase stockée de manière sécurisée.

---

## Phase 3 — Synchronisation & Offline parity (4–6 semaines) ✅ TERMINÉE

### 3.1. Sync bidirectionnelle iOS ✅
- ✅ `BidirectionalSyncManager` implémenté
- ✅ Ordre d'import/export des entités respecté
- ✅ DTOs de sync (`SyncDTOs.swift`)

### 3.2. Queue offline iOS ✅
- ✅ `SyncQueueStore` - Persistance SQLite de la queue
- ✅ `SyncQueueProcessor` - Traitement de la queue
- ✅ `SyncQueueHelper` - Enqueue automatique des opérations

### 3.3. Realtime cohérent ✅
- ✅ `RealtimeSyncService` avec Supabase Realtime
- ✅ Filtrage par table
- ✅ Résolution de conflits (server wins)

### 3.4. Scheduler unifié ✅
- ✅ `SyncScheduler` iOS avec trigger sur app resume
- ✅ `SyncStatusManager` pour état de sync

### Livrables ✅
- ✅ Sync bidirectionnelle iOS
- ✅ Queue offline iOS
- ✅ Règles realtime cohérentes

---

## Phase 4 — UX / Écrans manquants (2–3 semaines) ✅ TERMINÉE

### 4.1. Mouvements de stock iOS ✅
- ✅ Écran `StockMovementCreationView` (in/out) aligné Android
- ✅ Stock négatif autorisé avec avertissement non bloquant
- ✅ Navigation depuis la liste des mouvements

### 4.2. Update flow iOS ✅
- ✅ `CompatibilityChecker` partagé dans shared module
- ✅ `CompatibilityManager` iOS pour vérification via Supabase RPC
- ✅ `AppUpdateRequiredView` écran de blocage version
- ✅ Tests unitaires pour CompatibilityChecker

### Livrables ✅
- ✅ UI iOS alignée
- ✅ Parité fonctionnelle complète

---

## Phase 5 — Durcissement Android (1–2 semaines) ✅ TERMINÉE

### 5.1. Auth Android alignée shared ✅
- ✅ Android utilise `AuthService` du module shared

### 5.2. Résolution de conflits explicite ✅
- ✅ Policy "server wins" centralisée via `RealtimeSyncService`

### 5.3. Audit & sync ✅
- ✅ Toutes les opérations via UseCases génèrent un audit

### Livrables ✅
- ✅ Android conforme aux mêmes règles que iOS
- ✅ Cohérence audit & sync

---

## Phase 6 — Consolidation Services (1 semaine) ✅ TERMINÉE

> But : Extraire les services communs dans `shared` pour réduire la duplication de code entre Android et iOS.

### 6.1. PermissionService partagé ✅

**Implémenté dans `shared/domain/permission/` :**
- ✅ `PermissionService` - Service de vérification des permissions
- ✅ `ModulePermissions` - Data class pour les permissions CRUD d'un module
- ✅ Méthodes `canView`, `canCreate`, `canEdit`, `canDelete`
- ✅ Méthode `getModulePermissions` pour récupérer toutes les permissions d'un module
- ✅ Méthode `getAllModulePermissions` pour récupérer les permissions de tous les modules
- ✅ Exposé via `MedistockSDK.permissionService`

### 6.2. SyncOrchestrator partagé ✅

**Implémenté dans `shared/domain/sync/` :**
- ✅ `SyncEntity` enum - Entités synchronisables avec ordre de dépendance
- ✅ `SyncDirection` enum - Direction de synchronisation (local→remote, remote→local, bidirectional)
- ✅ `EntitySyncResult` sealed class - Résultat de sync par entité (Success, Error, Skipped)
- ✅ `SyncResult` data class - Résultat global de synchronisation
- ✅ `SyncProgressListener` interface - Callbacks de progression
- ✅ `SyncOrchestrator` class - Orchestration de la sync avec messages localisés
- ✅ Android `SyncManager` utilise `SyncOrchestrator` pour les messages de progression
- ✅ iOS `BidirectionalSyncManager` utilise `SyncOrchestrator` pour les messages de progression
- ✅ Exposé via `MedistockSDK.syncOrchestrator`

### 6.3. Tests unitaires ✅
- ✅ `PermissionAndSyncTests.kt` - Tests pour ModulePermissions, Module, SyncEntity, SyncOrchestrator

### Livrables ✅
- ✅ Services partagés PermissionService et SyncOrchestrator
- ✅ Tests unitaires couvrant les nouveaux services
- ✅ Android et iOS utilisent les services partagés

---

## Phase 7 — Unification Base de Données Android (3-4 semaines)

> But : Supprimer la duplication Room/SQLDelight sur Android pour utiliser exclusivement SQLDelight via le module shared.

### 7.1. Audit et mapping Room → SQLDelight ⏳

- [ ] Lister toutes les entités Room (17) et leurs équivalents SQLDelight
- [ ] Identifier les différences de schéma entre Room et SQLDelight
- [ ] Documenter les requêtes DAO spécifiques à migrer

### 7.2. Migration des DAOs Android ⏳

- [ ] Créer des wrappers Kotlin pour les repositories shared si nécessaire
- [ ] Migrer les usages de `AppDatabase` vers `MedistockSDK` repositories
- [ ] Supprimer les entités Room une par une (approche incrémentale)

### 7.3. Migration des données existantes ⏳

- [ ] Créer un script de migration Room → SQLDelight pour les données existantes
- [ ] Tester la migration sur différents scénarios (fresh install, upgrade)
- [ ] Gérer le versioning de la base SQLDelight

### 7.4. Nettoyage ⏳

- [ ] Supprimer les fichiers Room (`data/entities/`, `data/db/`)
- [ ] Supprimer les dépendances Room du `build.gradle`
- [ ] Mettre à jour les tests Android

### Livrables
- Android utilise exclusivement SQLDelight via shared
- Pas de duplication de schéma de base de données
- Tests de non-régression validés

---

## Phase 8 — Consolidation Sync (2-3 semaines) ✅ PARTIELLEMENT TERMINÉE

> But : Unifier les stratégies de synchronisation entre Android et iOS.

### 8.1. ConflictResolver partagé ✅

- ✅ Créé `ConflictResolver` class dans `shared/domain/sync/`
- ✅ Implémenté les stratégies : `REMOTE_WINS`, `LOCAL_WINS`, `MERGE`, `ASK_USER`, `KEEP_BOTH`
- ✅ Stratégies configurées par type d'entité (Products=RemoteWins, Sales=LocalWins, etc.)
- ✅ Android `SyncQueueProcessor` utilise `com.medistock.shared.domain.sync.ConflictResolver`
- ✅ iOS `EntityType.conflictStrategy` délègue à `SharedConflictResolver`
- ✅ Ancien `ConflictResolver.kt` Android marqué `@Deprecated`

### 8.2. RetryStrategy partagée ✅

- ✅ Créé `RetryConfiguration` dans shared avec backoff exponentiel
- ✅ Paramètres : `maxRetries=5`, `backoffDelaysMs=[1s,2s,4s,8s,16s]`, `batchSize=10`
- ✅ Android utilise `RetryConfiguration.DEFAULT` via `retryConfig`
- ✅ iOS `SyncConfiguration` délègue à `RetryConfiguration.companion.DEFAULT`

### 8.3. DTOs Sync unifiés ✅

- ✅ Créé 13 DTOs dans `shared/data/dto/` avec sérialisation snake_case
- ✅ Tests unitaires de sérialisation/désérialisation (`DtoTests.kt`)
- ⚠️ Android utilise encore ses propres DTOs dans `data/remote/dto/` (migration partielle)
- ⚠️ iOS utilise encore `SyncDTOs.swift` (migration partielle)

### 8.4. SyncStatusManager partagé ⏳

- [ ] Évaluer si `SyncStatusManager` doit être dans shared
- [ ] Si oui, créer une interface commune avec implémentations platform-specific

### Livrables ✅
- ✅ ConflictResolver et RetryStrategy partagés
- ✅ Tests unitaires pour les nouvelles classes shared (`SyncInfrastructureTests.kt`)
- ⚠️ Migration DTOs à finaliser (utiliser shared DTOs dans Android/iOS)

---

## Phase 9 — Tests de Parité (1-2 semaines)

> But : Garantir que les deux applications produisent les mêmes résultats pour les mêmes inputs.

### 9.1. Tests d'intégration shared ⏳

- [ ] Créer une suite de tests d'intégration dans shared
- [ ] Tester les UseCases avec des scénarios métier complets
- [ ] Vérifier les edge cases (stock négatif, conflits, etc.)

### 9.2. Tests de non-régression ⏳

- [ ] Documenter les scénarios de test manuels critiques
- [ ] Créer des tests UI automatisés si possible (Espresso/XCTest)
- [ ] Établir une checklist de validation avant release

### Livrables
- Suite de tests d'intégration complète
- Documentation des scénarios de test
- CI/CD avec tests automatisés

---

## Critères de sortie globaux

- ✅ Toutes les opérations métier passent par `shared` (UseCases)
- ✅ Sync bidirectionnelle et offline-first sur les deux plateformes
- ✅ Auth / permissions identiques Android et iOS
- ✅ Règle "stock négatif autorisé" appliquée partout (`BusinessWarning.InsufficientStock`)
- ✅ Parité UI complète (écrans stock + version blocking)
- ⏳ Base de données unique (SQLDelight) sur Android
- ⏳ Stratégies de sync unifiées (ConflictResolver, RetryStrategy)
- ⏳ Tests de parité Android/iOS
- ⏳ Intégrité référentielle (soft delete, validation suppression)
- ⏳ Multi-langue (EN/FR/ES minimum avec sélecteur dans profil)

---

## État d'avancement

| Phase | Statut | Notes |
|-------|--------|-------|
| Phase 0 - Cadrage | ✅ Terminée | Règles métier documentées |
| Phase 1 - UseCases shared | ✅ Terminée | 4 UseCases + tests |
| Phase 2 - Auth & Permissions | ✅ Terminée | Auth ✅, Modules ✅, Secure storage ✅ |
| Phase 3 - Sync iOS | ✅ Terminée | Bidirectionnel + Realtime |
| Phase 4 - UX iOS | ✅ Terminée | Stock movements + version blocking |
| Phase 5 - Durcissement Android | ✅ Terminée | ViewModels migrés |
| Phase 6 - Consolidation Services | ✅ Terminée | PermissionService + SyncOrchestrator |
| Phase 7 - Unification DB Android | ⏳ À faire | Supprimer Room, utiliser SQLDelight seul |
| Phase 8 - Consolidation Sync | ✅ Partiellement | ConflictResolver ✅, RetryStrategy ✅, DTOs ⚠️ |
| Phase 9 - Tests de Parité | ⏳ À faire | Tests d'intégration Android/iOS |
| Phase 10 - Parité Écrans Android | ⏳ À faire | Clients, Liste Achats, Liste Inventaires |
| Phase 11 - Intégrité Référentielle | ⏳ À faire | Soft delete, validation suppression références |
| Phase 12 - Internationalisation | ⏳ À faire | Multi-langue avec Lyricist |

**Dernière mise à jour :** 23 Janvier 2026

---

## Tâches différées

### 2.3. Permissions offline-first ❌ NON RETENU
- ~~Définir une stratégie commune (cache local + refresh distant).~~
- *Non retenu : la stratégie actuelle (cache local iOS/Android + sync Supabase) est suffisante.*

### 2.4. Sécurisation de la configuration Supabase ✅ TERMINÉE
- ✅ iOS : Keychain Services (`KeychainService.swift`)
- ✅ Android : EncryptedSharedPreferences (`SecureSupabasePreferences.kt`)
- ✅ Migration automatique transparente

---

## Annexes

### Document de comparaison Android/iOS
Voir [comparaison.md](./comparaison.md) pour l'analyse détaillée des écarts entre les implémentations Android et iOS.

### Priorités des écarts identifiés

| Priorité | Écart | Phase | Statut |
|----------|-------|-------|--------|
| 🔴 Haute | Double DB Android (Room + SQLDelight) | Phase 7 | ⏳ À faire |
| 🔴 Haute | Écrans Clients manquants Android | Phase 10 | ⏳ À faire |
| 🔴 Haute | Suppression références utilisées non bloquée | Phase 11 | ⏳ À faire |
| 🟡 Moyenne | ConflictResolver non partagé | Phase 8 | ✅ Fait |
| 🟡 Moyenne | RetryStrategy différente | Phase 8 | ✅ Fait |
| 🟡 Moyenne | Liste Achats manquante Android | Phase 10 | ⏳ À faire |
| 🟡 Moyenne | Liste Inventaires manquante Android | Phase 10 | ⏳ À faire |
| 🟡 Moyenne | Application mono-langue (EN seulement) | Phase 12 | ⏳ À faire |
| 🟢 Basse | DTOs sync partiellement dupliqués | Phase 8 | ⚠️ Partiel |
| 🟢 Basse | Menu Profil manquant Android | Phase 10 | ⏳ À faire |

---

## Phase 10 — Parité Écrans Android (2-3 semaines) ⏳ À FAIRE

> But : Ajouter les écrans manquants sur Android pour atteindre la parité fonctionnelle avec iOS.

### 10.1. Gestion des Clients 🔴 PRIORITAIRE

**Écrans à créer :**
- [ ] `CustomerListActivity` - Liste des clients avec recherche
- [ ] `CustomerAddEditActivity` - Création/édition de client
- [ ] `CustomerAdapter` - Adapter pour RecyclerView

**Fonctionnalités requises :**
- Liste avec recherche par nom/téléphone
- CRUD complet (via CustomerRepository shared)
- Filtrage par site si pertinent
- Sync avec Supabase

### 10.2. Liste des Achats 🟡

**Écrans à créer :**
- [ ] `PurchaseListActivity` - Historique des achats
- [ ] `PurchaseAdapter` - Adapter pour RecyclerView

**Fonctionnalités requises :**
- Liste des achats triés par date
- Filtrage par produit/fournisseur
- Détail d'un achat existant

### 10.3. Liste des Inventaires 🟡

**Écrans à créer :**
- [ ] `InventoryListActivity` - Liste des inventaires passés
- [ ] `InventoryAdapter` - Adapter pour RecyclerView

**Fonctionnalités requises :**
- Historique des inventaires
- Statut (en cours, terminé)
- Navigation vers détail/édition

### 10.4. Menu Profil 🟢

**Options :**
- [ ] Option A : Créer `ProfileActivity` dédiée
- [ ] Option B : Intégrer dans `SettingsActivity` existante

**Fonctionnalités requises :**
- Informations utilisateur connecté
- Changement de mot de passe (existe déjà)
- Déconnexion

### Livrables
- Parité fonctionnelle écrans Android/iOS
- Tests manuels de validation
- Documentation mise à jour

---

## Phase 11 — Intégrité Référentielle et Soft Delete (2-3 semaines) ⏳ À FAIRE

> But : Empêcher la suppression des références utilisées et implémenter un système de désactivation (soft delete) pour les entités référencées.

### 11.1. Audit des références utilisées 🔴 PRIORITAIRE

**Analyse à effectuer :**
- [ ] Identifier toutes les tables avec des foreign keys (Sites, Products, Categories, PackagingTypes, Users, Customers)
- [ ] Documenter les relations de dépendance (ex: Product → Category, Sale → Product, etc.)
- [ ] Vérifier l'état actuel : peut-on supprimer des références utilisées après migration KMP ?
- [ ] Comparer avec le comportement pré-migration KMP

**Tables concernées :**
- `sites` → référencé par `products`, `stock_movements`, `sales`, `transfers`
- `categories` → référencé par `products`
- `packaging_types` → référencé par `products`
- `products` → référencé par `purchase_batches`, `sale_items`, `stock_movements`, `transfers`
- `users` → référencé par `sales`, `purchases`, `audit_log`
- `customers` → référencé par `sales`

### 11.2. Ajout de la colonne `is_active` dans le schéma 🔴

**Modifications SQLDelight (`Medistock.sq`) :**
- [ ] Ajouter `is_active INTEGER AS Boolean NOT NULL DEFAULT 1` aux tables :
  - `sites`
  - `categories`
  - `packaging_types`
  - `products`
  - `users`
  - `customers`
- [ ] Créer une migration SQLDelight pour les bases existantes
- [ ] Ajouter des index sur `is_active` pour optimiser les requêtes de filtrage

### 11.3. Logique de validation avant suppression 🔴

**Créer `ReferentialIntegrityService` dans `shared/domain/validation/` :**
```kotlin
class ReferentialIntegrityService(private val database: MedistockDatabase) {

    // Vérifier si une référence est utilisée
    suspend fun isReferenceUsed(entityType: EntityType, entityId: String): Boolean

    // Obtenir le détail des usages
    suspend fun getUsageDetails(entityType: EntityType, entityId: String): UsageDetails

    // Désactiver une entité (soft delete)
    suspend fun deactivateEntity(entityType: EntityType, entityId: String): Result<Unit>

    // Supprimer une entité non utilisée (hard delete)
    suspend fun deleteEntity(entityType: EntityType, entityId: String): Result<Unit>
}

data class UsageDetails(
    val isUsed: Boolean,
    val usageCount: Int,
    val usedIn: List<UsageReference>
)

data class UsageReference(
    val table: String,
    val count: Int
)
```

**Requêtes à implémenter dans `Medistock.sq` :**
```sql
-- Vérifier si un site est utilisé
SELECT COUNT(*) FROM products WHERE site_id = ?;
SELECT COUNT(*) FROM stock_movements WHERE site_id = ?;
-- etc.

-- Vérifier si une catégorie est utilisée
SELECT COUNT(*) FROM products WHERE category_id = ?;

-- Vérifier si un packaging_type est utilisé
SELECT COUNT(*) FROM products WHERE packaging_type_id = ?;

-- Vérifier si un produit est utilisé
SELECT COUNT(*) FROM purchase_batches WHERE product_id = ?;
SELECT COUNT(*) FROM sale_items WHERE product_id = ?;
-- etc.
```

### 11.4. Modification des UseCases pour respecter is_active 🟡

**UseCases à mettre à jour :**
- [ ] `SaleUseCase` : ne lister que les produits/clients actifs
- [ ] `PurchaseUseCase` : ne lister que les produits/sites actifs
- [ ] `TransferUseCase` : ne lister que les sites actifs
- [ ] Tous les écrans de sélection : filtrer `WHERE is_active = 1`

**Requêtes SQLDelight à modifier :**
- [ ] `getAllProducts()` → `getAllActiveProducts()`
- [ ] `getAllSites()` → `getAllActiveSites()`
- [ ] `getAllCategories()` → `getAllActiveCategories()`
- [ ] `getAllPackagingTypes()` → `getAllActivePackagingTypes()`
- [ ] `getAllCustomers()` → `getAllActiveCustomers()`
- [ ] `getAllUsers()` → `getAllActiveUsers()`

### 11.5. Mise à jour de l'UI Android et iOS 🟡

**Bouton conditionnel dans les écrans de détail/édition :**
- [ ] **Si référence non utilisée** : Afficher bouton "Delete" (suppression définitive)
- [ ] **Si référence utilisée** : Afficher bouton "Deactivate" (soft delete)
- [ ] Afficher un indicateur visuel pour les entités désactivées dans les listes d'administration

**Écrans à modifier :**

**Android :**
- [ ] `SiteAddEditActivity` - Vérifier usage avant suppression
- [ ] `CategoryAddEditActivity` - Vérifier usage avant suppression
- [ ] `ProductAddEditActivity` - Vérifier usage avant suppression
- [ ] `PackagingTypeAddEditActivity` - Vérifier usage avant suppression
- [ ] `CustomerAddEditActivity` (à créer en Phase 10) - Vérifier usage avant suppression
- [ ] `UserAddEditActivity` - Vérifier usage avant suppression

**iOS :**
- [ ] `SitesViews.swift` - Vérifier usage avant suppression
- [ ] `CategoriesViews.swift` - Vérifier usage avant suppression
- [ ] `ProductsViews.swift` - Vérifier usage avant suppression
- [ ] `PackagingTypesViews.swift` - Vérifier usage avant suppression
- [ ] `CustomersViews.swift` - Vérifier usage avant suppression
- [ ] `UsersViews.swift` - Vérifier usage avant suppression

**Exemple d'implémentation UI :**
```swift
// iOS
if referentialIntegrityService.isReferenceUsed(.product, productId) {
    Button("Deactivate") { /* soft delete */ }
        .foregroundColor(.orange)
} else {
    Button("Delete") { /* hard delete */ }
        .foregroundColor(.red)
}
```

### 11.6. Sync et Supabase 🟡

**Modifications requises :**
- [ ] Les DTOs de sync doivent inclure `is_active`
- [ ] La sync doit respecter le statut `is_active`
- [ ] RLS Supabase : permettre soft delete mais pas hard delete des références utilisées
- [ ] Fonction RPC Supabase pour vérifier l'usage des références

### 11.7. Tests 🟢

**Tests unitaires à créer :**
- [ ] `ReferentialIntegrityServiceTests.kt` - Tests de vérification d'usage
- [ ] Tests de soft delete vs hard delete
- [ ] Tests de filtrage par `is_active`

**Tests d'intégration :**
- [ ] Tenter de supprimer un site utilisé → échec ou soft delete
- [ ] Tenter de supprimer un produit non utilisé → succès
- [ ] Vérifier que les écrans n'affichent que les entités actives

### 11.8. Migration des données existantes 🟡

**Script de migration :**
- [ ] Ajouter `is_active = 1` à toutes les entités existantes
- [ ] Identifier les entités qui devraient être désactivées (orphelines, supprimées manuellement, etc.)

### Livrables
- `ReferentialIntegrityService` implémenté dans shared
- Colonne `is_active` ajoutée à toutes les tables concernées
- UI affiche bouton "Delete" ou "Deactivate" selon usage
- Tous les écrans filtrent par `is_active`
- Tests unitaires et d'intégration
- Migration automatique des données existantes

---

## Phase 12 — Internationalisation (i18n) Multi-langue (2-3 semaines) ⏳ À FAIRE

> But : Transformer les deux applications en multi-langue avec un système de gestion des traductions centralisé.

### 12.1. Choix de la bibliothèque i18n 🔴 PRIORITAIRE

**Bibliothèque recommandée : Lyricist**
- 📦 Library : `cafe.adriel.lyricist:lyricist` (KMP)
- ✅ Supporte Kotlin Multiplatform (Android + iOS)
- ✅ Type-safe (génération de code)
- ✅ Facile d'ajouter de nouvelles langues
- ✅ Support de pluralisation et formatage

**Alternative : Kiwi (Touchlab)**
- Plus complexe mais plus flexible
- Meilleure intégration avec les ressources natives

**Décision :** Lyricist (recommandé pour sa simplicité et son approche type-safe)

### 12.2. Intégration de Lyricist dans shared 🔴

**Dépendances à ajouter dans `shared/build.gradle.kts` :**
```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cafe.adriel.lyricist:lyricist:1.4.1")
        }
    }
}
```

**Structure des traductions dans `shared/src/commonMain/kotlin/com/medistock/shared/i18n/` :**
```kotlin
// Strings.kt - Interface des traductions
interface Strings {
    // Authentication
    val login: String
    val logout: String
    val username: String
    val password: String
    val invalidCredentials: String

    // Operations
    val operations: String
    val purchaseProducts: String
    val sellProducts: String
    val transferProducts: String
    val viewStock: String
    val inventoryStock: String

    // Admin
    val administration: String
    val siteManagement: String
    val manageProducts: String
    val manageCategories: String
    val manageCustomers: String
    val packagingTypes: String
    val userManagement: String

    // CRUD
    val create: String
    val edit: String
    val delete: String
    val deactivate: String
    val save: String
    val cancel: String

    // Messages
    val confirmDelete: String
    val itemUsedCannotDelete: String
    val itemDeactivated: String

    // Et tous les autres strings...
}

// EnStrings.kt - Traductions anglaises
val EnStrings = Strings(
    login = "Login",
    logout = "Logout",
    username = "Username",
    password = "Password",
    // ...
)

// FrStrings.kt - Traductions françaises
val FrStrings = Strings(
    login = "Connexion",
    logout = "Déconnexion",
    username = "Nom d'utilisateur",
    password = "Mot de passe",
    // ...
)

// EsStrings.kt - Traductions espagnoles (exemple)
val EsStrings = Strings(
    login = "Iniciar sesión",
    logout = "Cerrar sesión",
    username = "Nombre de usuario",
    password = "Contraseña",
    // ...
)
```

**Configuration Lyricist :**
```kotlin
// LocalizationManager.kt
object LocalizationManager {
    val lyricist = Lyricist(
        defaultLanguageTag = "en",
        translations = mapOf(
            "en" to EnStrings,
            "fr" to FrStrings,
            "es" to EsStrings
        )
    )

    fun setLanguage(languageCode: String) {
        lyricist.languageTag = languageCode
    }

    fun getAvailableLanguages(): List<Language> = listOf(
        Language("en", "English", "🇬🇧"),
        Language("fr", "Français", "🇫🇷"),
        Language("es", "Español", "🇪🇸")
    )
}

data class Language(
    val code: String,
    val name: String,
    val flag: String
)
```

### 12.3. Stockage de la préférence de langue 🟡

**Ajouter colonne `preferred_language` à la table `users` :**
```sql
-- Medistock.sq
ALTER TABLE users ADD COLUMN preferred_language TEXT DEFAULT 'en';
```

**Service de gestion de la langue :**
```kotlin
// shared/domain/i18n/LanguageService.kt
class LanguageService(
    private val userRepository: UserRepository,
    private val preferencesStore: PreferencesStore // KeyValue store platform-specific
) {
    suspend fun setUserLanguage(userId: String, languageCode: String) {
        userRepository.updateUserLanguage(userId, languageCode)
        preferencesStore.setString("user_language", languageCode)
        LocalizationManager.setLanguage(languageCode)
    }

    suspend fun getUserLanguage(userId: String): String {
        return userRepository.getUser(userId)?.preferredLanguage ?: "en"
    }

    fun getCurrentLanguage(): String {
        return LocalizationManager.lyricist.languageTag
    }
}
```

### 12.4. Intégration Android 🟡

**Modifier tous les strings hardcodés en utilisation de Lyricist :**
```kotlin
// Avant
textView.text = "Login"

// Après
textView.text = LocalizationManager.lyricist.strings.login
```

**Écrans à modifier :**
- [ ] `LoginActivity` - Strings d'authentification
- [ ] `HomeActivity` - Menu principal
- [ ] `SiteAddEditActivity` - Gestion des sites
- [ ] `ProductAddEditActivity` - Gestion des produits
- [ ] `CategoryAddEditActivity` - Gestion des catégories
- [ ] `PackagingTypeAddEditActivity` - Gestion des emballages
- [ ] `UserAddEditActivity` - Gestion des utilisateurs
- [ ] `PurchaseActivity` - Achats
- [ ] `SaleActivity` - Ventes
- [ ] `TransferActivity` - Transferts
- [ ] `InventoryActivity` - Inventaires
- [ ] Tous les autres écrans...

### 12.5. Intégration iOS 🟡

**Modifier tous les Text() hardcodés :**
```swift
// Avant
Text("Login")

// Après
Text(LocalizationManager.shared.strings.login)
```

**Vues à modifier :**
- [ ] `AuthViews.swift` - Authentification
- [ ] `HomeViews.swift` - Menu principal
- [ ] `SitesViews.swift` - Gestion des sites
- [ ] `ProductsViews.swift` - Gestion des produits
- [ ] `CategoriesViews.swift` - Gestion des catégories
- [ ] `PackagingTypesViews.swift` - Gestion des emballages
- [ ] `UsersViews.swift` - Gestion des utilisateurs
- [ ] `PurchasesViews.swift` - Achats
- [ ] `SalesViews.swift` - Ventes
- [ ] `TransfersViews.swift` - Transferts
- [ ] `InventoryCountViews.swift` - Inventaires
- [ ] Toutes les autres vues...

### 12.6. Écran de sélection de langue dans le profil 🟡

**Android - Ajouter option dans ProfileActivity :**
```kotlin
// LanguageSelectionDialog
class LanguageSelectionDialog : DialogFragment() {
    private val languages = LocalizationManager.getAvailableLanguages()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Select Language")
            .setItems(languages.map { "${it.flag} ${it.name}" }.toTypedArray()) { _, which ->
                val selected = languages[which]
                viewModel.setLanguage(selected.code)
            }
            .create()
    }
}
```

**iOS - Ajouter dans ProfileViews.swift :**
```swift
Section("Language") {
    Picker("Language", selection: $selectedLanguage) {
        ForEach(LocalizationManager.shared.getAvailableLanguages()) { language in
            Text("\(language.flag) \(language.name)")
                .tag(language.code)
        }
    }
    .onChange(of: selectedLanguage) { newValue in
        viewModel.setLanguage(newValue)
    }
}
```

### 12.7. Langue par défaut et initialisation 🟡

**Au démarrage de l'app :**
```kotlin
// Android - Application.onCreate()
class MedistockApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Charger la langue de l'utilisateur connecté
        val userId = sessionManager.getUserId()
        if (userId.isNotEmpty()) {
            val language = languageService.getUserLanguage(userId)
            LocalizationManager.setLanguage(language)
        } else {
            // Langue par défaut : anglais
            LocalizationManager.setLanguage("en")
        }
    }
}
```

```swift
// iOS - MedistockApp.swift
@main
struct MedistockApp: App {
    init() {
        // Charger la langue de l'utilisateur connecté
        if let userId = SessionManager.shared.userId, !userId.isEmpty {
            Task {
                let language = await languageService.getUserLanguage(userId)
                LocalizationManager.shared.setLanguage(language)
            }
        } else {
            // Langue par défaut : anglais
            LocalizationManager.shared.setLanguage("en")
        }
    }
}
```

### 12.8. Traductions des messages dynamiques 🟢

**Utiliser les paramètres de formatage :**
```kotlin
// Strings.kt
interface Strings {
    fun itemDeletedSuccess(itemName: String): String
    fun confirmDeleteItem(itemName: String): String
    fun stockQuantity(quantity: Int, unit: String): String
}

// EnStrings.kt
val EnStrings = Strings(
    itemDeletedSuccess = { name -> "$name deleted successfully" },
    confirmDeleteItem = { name -> "Delete $name?" },
    stockQuantity = { qty, unit -> "$qty $unit in stock" }
)

// FrStrings.kt
val FrStrings = Strings(
    itemDeletedSuccess = { name -> "$name supprimé avec succès" },
    confirmDeleteItem = { name -> "Supprimer $name ?" },
    stockQuantity = { qty, unit -> "$qty $unit en stock" }
)
```

### 12.9. Tests 🟢

**Tests à créer :**
- [ ] `LocalizationManagerTests.kt` - Tests de changement de langue
- [ ] `LanguageServiceTests.kt` - Tests de persistance des préférences
- [ ] Tests UI : vérifier que tous les écrans s'affichent correctement en FR/EN/ES

### 12.10. Documentation des traductions 🟢

**Créer un guide pour ajouter une nouvelle langue :**
```markdown
# Ajouter une nouvelle langue

1. Créer un nouveau fichier `XxStrings.kt` dans `shared/i18n/`
2. Implémenter l'interface `Strings` avec toutes les traductions
3. Ajouter la langue dans `LocalizationManager.lyricist.translations`
4. Ajouter la langue dans `getAvailableLanguages()`
5. Tester sur Android et iOS
```

### Livrables
- Lyricist intégré dans le module shared
- Toutes les chaînes de caractères externalisées
- Sélecteur de langue dans le profil utilisateur
- Langue par défaut : anglais
- Support de 3 langues minimum : EN, FR, ES
- Documentation pour ajouter de nouvelles langues
- Tests de changement de langue