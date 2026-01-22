# Analyse Comparative Android vs iOS - MediStock

> Document généré le 22 Janvier 2026
> Objectif : Identifier les écarts entre les implémentations Android et iOS, et planifier les améliorations.

---

## 1. Tableau de Parité Fonctionnelle

| Fonctionnalité | Android | iOS | Parité | Notes |
|----------------|---------|-----|--------|-------|
| **Authentification** | ✅ AuthManager + AuthService shared | ✅ SessionManager + AuthService shared | ✅ | Les deux utilisent AuthService partagé |
| **BCrypt Password Hashing** | ✅ PasswordHasher (jBCrypt) | ✅ BCryptPasswordVerifier (swift-bcrypt) | ✅ | Implémentation platform-specific, interface partagée |
| **Permissions par module** | ✅ PermissionManager | ✅ PermissionManager | ✅ | Les deux utilisent PermissionService partagé |
| **Stockage sécurisé credentials** | ✅ EncryptedSharedPreferences | ✅ Keychain Services | ✅ | Implémentation platform-specific |
| **Sync bidirectionnelle** | ✅ SyncManager (548 lignes) | ✅ BidirectionalSyncManager (~302 lignes) | ⚠️ | Android plus complet (voir différences) |
| **Queue offline** | ✅ SyncQueueProcessor | ✅ SyncQueueProcessor | ✅ | Logique similaire |
| **Realtime Supabase** | ✅ RealtimeSyncManager | ✅ RealtimeSyncService | ✅ | Corrigé: sale_items ajouté iOS |
| **Background sync** | ✅ WorkManager | ✅ BGProcessingTask | ✅ | Platform-specific |
| **Vérification compatibilité app** | ✅ CompatibilityChecker | ✅ CompatibilityManager | ✅ | CompatibilityChecker partagé |
| **Écran mise à jour requise** | ✅ AppUpdateRequiredActivity | ✅ AppUpdateRequiredView | ✅ | |
| **FIFO Stock allocation** | ✅ Via UseCases partagés | ✅ Via UseCases partagés | ✅ | |
| **Stock négatif autorisé** | ✅ Warning non-bloquant | ✅ Warning non-bloquant | ✅ | BusinessWarning.InsufficientStock |

---

## 2. Écrans / Vues

### Comparaison des écrans principaux

| Écran | Android | iOS | Parité |
|-------|---------|-----|--------|
| Login | ✅ LoginActivity | ✅ LoginView | ✅ |
| Home/Dashboard | ✅ HomeActivity | ✅ HomeView | ✅ |
| Sites (liste/CRUD) | ✅ SiteListActivity/AddEdit | ✅ SitesListView/SiteEditorView | ✅ |
| Catégories | ✅ CategoryListActivity/AddEdit | ✅ CategoriesListView/CategoryEditorView | ✅ |
| Produits | ✅ ProductListActivity/AddEdit | ✅ ProductsListView/ProductEditorView | ✅ |
| Achats (création) | ✅ PurchaseActivity | ✅ PurchaseEditorView | ✅ |
| Achats (liste) | ✅ PurchaseListActivity | ✅ PurchasesListView | ✅ |
| Ventes | ✅ SaleListActivity/SaleActivity | ✅ SalesListView/SaleEditorView | ✅ |
| Transferts | ✅ TransferListActivity/TransferActivity | ✅ TransfersListView/TransferEditorView | ✅ |
| Inventaires (création) | ✅ InventoryActivity | ✅ InventoryEditorView | ✅ |
| Inventaires (liste) | ✅ InventoryListActivity | ✅ InventoryListView | ✅ |
| Mouvements stock | ✅ StockMovementListActivity/Activity | ✅ StockMovementsListView/CreationView | ✅ |
| Utilisateurs | ✅ UserListActivity/AddEdit | ✅ UsersListView/UserEditorView | ✅ |
| Clients (liste) | ✅ CustomerListActivity | ✅ CustomersListView | ✅ |
| Clients (CRUD) | ✅ CustomerAddEditActivity | ✅ CustomerEditorView | ✅ |
| Packaging types | ✅ PackagingTypeListActivity/AddEdit | ✅ PackagingTypesListView/EditorView | ✅ |
| Config Supabase | ✅ SupabaseConfigActivity | ✅ SupabaseConfigView | ✅ |
| Menu Profil | ✅ ProfileActivity | ✅ ProfileMenuView | ✅ |
| Permissions utilisateur | ⚠️ Intégré dans UserAddEdit | ✅ UserPermissionsEditView | ⚠️ |

### Métriques détaillées

| Métrique | Android | iOS | Notes |
|----------|---------|-----|-------|
| **Écrans principaux** | 36 Activities | 36 Views principales | ✅ Parité atteinte |
| **Vues Row/Cell** | ~17 Adapters | 12 RowViews | Architecture différente |
| **Vues utilitaires** | Intégrées | 9 (BadgeView, EmptyState, etc.) | iOS plus modulaire |

### Écrans récemment ajoutés sur Android ✅

1. **CustomerListActivity** - Gestion de la liste des clients avec recherche
2. **CustomerAddEditActivity** - Création/édition de clients (nom, téléphone, adresse, notes)
3. **PurchaseListActivity** - Historique des achats avec filtrage (All/Active/Exhausted)
4. **InventoryListActivity** - Liste des inventaires passés avec filtrage par écarts
5. **ProfileActivity** - Menu profil utilisateur (info, change password, logout)

---

## 3. Différences de Comportement

| Aspect | Android | iOS | Impact |
|--------|---------|-----|--------|
| **Architecture UI** | Activities + Fragments + RecyclerView | SwiftUI Views + ObservableObject | Aucun (platform idioms) |
| **Base de données locale** | Room (17 entities, 17 DAOs) + SQLDelight | SQLDelight via shared | ⚠️ Android a double DB |
| **ViewModels** | 6 ViewModels (Purchase, Sale, etc.) | Pas de ViewModels explicites | Style différent |
| **Résolution conflits sync** | ConflictResolver avec stratégies | Server-wins hardcodé | ⚠️ Android plus flexible |
| **Retry sync** | ExponentialBackoff configurable | Fixed retry count | ⚠️ Android plus robuste |
| **Sync progress** | SyncOrchestrator messages localisés | SyncOrchestrator messages localisés | ✅ |
| **State management** | LiveData/StateFlow | @Published + Combine | Aucun (platform idioms) |
| **Navigation** | Intent-based | NavigationStack/NavigationLink | Aucun (platform idioms) |

---

## 4. Contenu du Module Shared

| Catégorie | Éléments | Utilisé par |
|-----------|----------|-------------|
| **Modèles (18)** | User, Site, Category, Product, PurchaseBatch, Sale, SaleItem, StockMovement, Customer, Inventory, InventoryItem, ProductTransfer, PackagingType, UserPermission, Audit, Module, etc. | Android ✅, iOS ✅ |
| **Repositories (14)** | UserRepository, SiteRepository, CategoryRepository, ProductRepository, PurchaseBatchRepository, SaleRepository, StockMovementRepository, CustomerRepository, InventoryRepository, PackagingTypeRepository, UserPermissionRepository, AuditRepository, ProductTransferRepository, SyncQueueRepository | Android ✅, iOS ✅ |
| **UseCases (4)** | PurchaseUseCase, SaleUseCase, TransferUseCase, InventoryUseCase | Android ✅, iOS ✅ |
| **Services (3)** | AuthService, PermissionService, SyncOrchestrator | Android ✅, iOS ✅ |
| **Utils** | CompatibilityChecker, BusinessWarning, UseCaseResult | Android ✅, iOS ✅ |
| **Database** | SQLDelight (18 tables) | Android ⚠️ (+ Room), iOS ✅ |

---

## 5. Ce qui N'EST PAS dans Shared mais DEVRAIT y être

| Élément | Android | iOS | Raison de migration |
|---------|---------|-----|---------------------|
| **ConflictResolver** | ✅ ConflictResolver.kt avec stratégies | ❌ Server-wins hardcodé | Unifier la logique de résolution de conflits |
| **ExponentialBackoff** | ✅ Configurable | ❌ Fixed retry | Unifier la stratégie de retry |
| **SyncStatusManager** | ✅ Complet | ✅ Simplifié | Pourrait être partagé pour cohérence |
| **DTOs Supabase** | ⚠️ Partiellement dupliqués | ✅ SyncDTOs.swift | Unifier les DTOs de sync |

---

## 6. Ce qui NE DOIT PAS être dans Shared (Platform-specific)

| Élément | Raison |
|---------|--------|
| **UI (Activities/Views)** | Platform idioms différents (UIKit/SwiftUI vs Android Views) |
| **Navigation** | Intent vs NavigationStack |
| **Background processing** | WorkManager vs BGProcessingTask |
| **Secure storage** | EncryptedSharedPreferences vs Keychain |
| **BCrypt implementation** | jBCrypt vs swift-bcrypt (interface partagée OK) |
| **Network client** | Ktor platform-specific implementations |
| **Local notifications** | NotificationManager vs UNUserNotificationCenter |
| **Biometric auth** | BiometricPrompt vs LocalAuthentication |
| **File system access** | Context.filesDir vs FileManager |

---

## 7. Écarts Critiques à Corriger

| Priorité | Écart | Description | Action recommandée |
|----------|-------|-------------|-------------------|
| 🔴 Haute | **Double DB Android** | Android utilise Room ET SQLDelight | Migrer Android vers SQLDelight seul |
| 🟡 Moyenne | **ConflictResolver** | Android a stratégies, iOS hardcodé | Créer ConflictResolver partagé |
| 🟡 Moyenne | **Retry strategy** | ExponentialBackoff vs fixed | Partager la stratégie |
| 🟢 Basse | **DTOs sync** | Légère duplication | Unifier dans shared |

---

## 8. Architecture Actuelle

```
┌─────────────────────────────────────────────────────────────┐
│                    SHARED MODULE (KMM)                       │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐│
│  │  4 UseCases │ │ 3 Services  │ │    14 Repositories      ││
│  │  Purchase   │ │ Auth        │ │    Product, Sale,       ││
│  │  Sale       │ │ Permission  │ │    PurchaseBatch,       ││
│  │  Transfer   │ │ SyncOrch.   │ │    StockMovement, etc.  ││
│  │  Inventory  │ │             │ │                         ││
│  └─────────────┘ └─────────────┘ └─────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │              18 Domain Models + SQLDelight DB            ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
           │                              │
           ▼                              ▼
┌─────────────────────┐        ┌─────────────────────┐
│      ANDROID        │        │        iOS          │
│  ┌───────────────┐  │        │  ┌───────────────┐  │
│  │ Room DB (dup) │  │        │  │ SQLDelight    │  │
│  │ 17 entities   │  │        │  │ (via shared)  │  │
│  └───────────────┘  │        │  └───────────────┘  │
│  ┌───────────────┐  │        │  ┌───────────────┐  │
│  │ 36 Activities │  │        │  │ 39 SwiftUI    │  │
│  │ 6 ViewModels  │  │        │  │ Views         │  │
│  └───────────────┘  │        │  └───────────────┘  │
│  ┌───────────────┐  │        │  ┌───────────────┐  │
│  │ SyncManager   │  │        │  │ Bidir.Sync    │  │
│  │ ConflictRes.  │  │        │  │ Manager       │  │
│  │ WorkManager   │  │        │  │ BGTask        │  │
│  └───────────────┘  │        │  └───────────────┘  │
└─────────────────────┘        └─────────────────────┘
```

---

## 9. Recommandations Prioritaires

1. **Supprimer Room sur Android** - Migrer vers SQLDelight exclusif pour éviter la double base de données
2. **Partager ConflictResolver** - Créer une interface de résolution de conflits dans shared avec stratégies configurables
3. **Partager ExponentialBackoff** - Stratégie de retry unifiée dans shared
4. **Unifier DTOs Sync** - Centraliser les DTOs de synchronisation Supabase dans shared
5. **Tests de parité** - Créer des tests d'intégration vérifiant que les deux apps produisent les mêmes résultats pour les mêmes inputs

---

## 10. Métriques de Code

### Android
- **Activities** : 36
- **ViewModels** : 6
- **Room Entities** : 17
- **Room DAOs** : 17
- **Adapters** : ~17 (dont PurchaseBatchAdapter, InventoryAdapter)

### iOS
- **SwiftUI Views** : 39
- **ObservableObjects** : ~10
- **Services** : ~8

### Shared
- **Domain Models** : 18
- **Repositories** : 14
- **UseCases** : 4
- **Services** : 3
- **SQLDelight Tables** : 18

---

*Document de référence pour la roadmap technique.*
