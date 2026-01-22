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

### 2.1. Authentification partagée
- Déplacer la logique d’auth dans `shared`.
- Uniformiser les règles (Supabase si configuré, sinon local).
- Éviter les divergences Android/iOS.

### 2.2. Modules permissions partagés ✅
- ✅ Enum `Module` unifié dans `shared` (13 modules)
- ✅ Modèle `UserPermission` module-based (canView/Create/Edit/Delete)
- ✅ `UserPermissionRepository` dans shared
- ✅ Android et iOS utilisent les modules shared

### 2.3. Permissions offline-first
- Définir une stratégie commune (cache local + refresh distant).

### 2.4. Sécurisation de la configuration Supabase
- Actuellement stockée en clair dans UserDefaults (iOS) / SharedPreferences (Android).
- Investiguer des solutions de stockage sécurisé :
  - **iOS** : Keychain Services
  - **Android** : EncryptedSharedPreferences / Android Keystore
- Empêcher la lecture des credentials par d'autres apps ou en cas de backup non chiffré.

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

## Phase 4 — UX / Écrans manquants (2–3 semaines)

### 4.1. Mouvements de stock iOS
- Ajouter un écran de création (in/out) aligné Android.
- **Ne jamais bloquer** si stock insuffisant.
- Avertissement non bloquant possible.

### 4.2. Update flow iOS
- Ajouter un écran “version obsolète” similaire au flow Android.
- Définir politique de distribution iOS (App Store / MDM / etc.).

### Livrables
- UI iOS alignée.
- Parité fonctionnelle complète.

---

## Phase 5 — Durcissement Android (1–2 semaines)

### 5.1. Auth Android alignée shared
- Remplacer la logique locale pure par celle de `shared`.

### 5.2. Résolution de conflits explicite
- Centraliser la policy de résolution et l’exposer dans `shared`.

### 5.3. Audit & sync
- Garantir que toutes les opérations syncées génèrent un audit.

### Livrables
- Android conforme aux mêmes règles que iOS.
- Cohérence audit & sync.

---

## Critères de sortie globaux

- ✅ Toutes les opérations métier passent par `shared` (UseCases)
- ✅ Sync bidirectionnelle et offline-first sur les deux plateformes
- ⏳ Auth / permissions identiques Android et iOS (Phase 2 - en cours)
- ✅ Règle "stock négatif autorisé" appliquée partout (`BusinessWarning.InsufficientStock`)
- ⏳ Parité UI complète (Phase 4 - à faire)

---

## État d'avancement

| Phase | Statut | Notes |
|-------|--------|-------|
| Phase 0 - Cadrage | ✅ Terminée | Règles métier documentées |
| Phase 1 - UseCases shared | ✅ Terminée | 4 UseCases + tests |
| Phase 2 - Auth & Permissions | ⏳ En cours | BCrypt ✅, Modules ✅, Auth 2.1 à faire |
| Phase 3 - Sync iOS | ✅ Terminée | Bidirectionnel + Realtime |
| Phase 4 - UX iOS | ⏳ À faire | Écrans manquants |
| Phase 5 - Durcissement Android | ✅ Terminée | ViewModels migrés |

**Dernière mise à jour :** Janvier 2026