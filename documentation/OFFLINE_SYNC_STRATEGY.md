# Stratégie de Synchronisation Offline/Online - MediStock

## Vue d'ensemble

Ce document décrit l'architecture de synchronisation bidirectionnelle implémentée pour gérer le mode déconnecté et la résolution de conflits dans MediStock.

## Table des matières

1. [Architecture générale](#architecture-générale)
2. [Queue de synchronisation](#queue-de-synchronisation)
3. [Gestion des versions de schéma](#gestion-des-versions-de-schéma)
4. [Résolution des conflits](#résolution-des-conflits)
5. [Composants implémentés](#composants-implémentés)
6. [Guide d'intégration](#guide-dintégration)

---

## Architecture générale

```
┌─────────────────────────────────────────────────────────────────────┐
│                         APPLICATION MOBILE                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐    ┌─────────────────┐    ┌──────────────────┐   │
│  │   UI Layer  │◄───│ SyncStatusManager│◄───│ SyncQueueProcessor│  │
│  │             │    │   (Observable)   │    │  (Background)     │  │
│  └─────────────┘    └─────────────────┘    └──────────────────┘   │
│         │                                          │               │
│         ▼                                          ▼               │
│  ┌─────────────┐    ┌─────────────────┐    ┌──────────────────┐   │
│  │ Repositories│───▶│ SyncQueueHelper │───▶│   sync_queue     │   │
│  │  (Audited)  │    │  (Enqueue ops)  │    │   (Room Table)   │   │
│  └─────────────┘    └─────────────────┘    └──────────────────┘   │
│         │                                          │               │
│         ▼                                          ▼               │
│  ┌─────────────┐                          ┌──────────────────┐    │
│  │  Room DB    │                          │ ConflictResolver │    │
│  │  (Local)    │                          │                  │    │
│  └─────────────┘                          └──────────────────┘    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTPS
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                           SUPABASE                                  │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐   │
│  │   Tables     │  │  sync_log    │  │  schema_version        │   │
│  │  (Data)      │  │  (History)   │  │  (Compatibility)       │   │
│  └──────────────┘  └──────────────┘  └────────────────────────┘   │
│  ┌──────────────┐  ┌──────────────┐                               │
│  │sync_conflicts│  │schema_migr.  │                               │
│  │(Unresolved)  │  │(Migrations)  │                               │
│  └──────────────┘  └──────────────┘                               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Queue de synchronisation

### Principe

Chaque modification locale est enregistrée dans une table `sync_queue` persistante (Room). Cela garantit qu'aucune donnée n'est perdue même si l'application est fermée en mode offline.

### Structure de la queue

```kotlin
SyncQueueItem(
    entityType: String,      // "Product", "Sale", etc.
    entityId: String,        // UUID de l'entité
    operation: SyncOperation, // INSERT, UPDATE, DELETE
    payload: String,         // JSON de l'entité
    localVersion: Long,      // Version locale pour détection conflits
    remoteVersion: Long?,    // Dernière version serveur connue
    status: SyncStatus,      // PENDING, IN_PROGRESS, SYNCED, CONFLICT, FAILED
    retryCount: Int,         // Nombre de tentatives
    lastError: String?       // Message d'erreur
)
```

### Optimisation de la queue

```
INSERT + UPDATE → Conserver INSERT avec données finales
UPDATE + UPDATE → Conserver dernier UPDATE
INSERT + DELETE → Supprimer les deux (jamais synchro)
UPDATE + DELETE → Conserver DELETE seul
```

### Retry avec backoff exponentiel

```
Tentative 1: Immédiat
Tentative 2: Après 1s
Tentative 3: Après 2s
Tentative 4: Après 4s
Tentative 5: Après 8s
Après 5 échecs: Status = FAILED (intervention requise)
```

---

## Gestion des versions de schéma

### Problématique

> *"Comment gérer les désalignements de versions lors d'une montée de version, si quelqu'un fait des actions sur une ancienne version non connecté alors que la base de données centrale s'est mise à jour ?"*

### Solution: Triple vérification

```
┌─────────────────────────────────────────────────────────────────┐
│                    DÉMARRAGE APPLICATION                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. VÉRIFICATION BLOQUAGE LOCAL                                │
│     └─► Si version app < version_bloquée → BLOQUER             │
│                                                                 │
│  2. SI ONLINE: VÉRIFICATION SERVEUR                            │
│     └─► Appel get_schema_version()                             │
│         └─► Si app < min_app_version → BLOQUER + sauvegarder   │
│         └─► Si OK → Mettre à jour le cache                     │
│                                                                 │
│  3. SI OFFLINE: VÉRIFICATION CACHE                             │
│     └─► Si cache valide (< 24h) → Utiliser le cache            │
│     └─► Si pas de cache → Autoriser (première fois)            │
│     └─► Si version bloquée → BLOQUER même offline              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Cache de version

Le `SchemaVersionChecker` maintient un cache local avec:
- `cached_schema_version`: Version du schéma serveur
- `cached_min_app_version`: Version app minimum requise
- `last_check_time`: Date de dernière vérification
- `blocked_version`: Version bloquée (persiste même offline)

### Comportement par scénario

| Scénario | Online | Cache | Action |
|----------|--------|-------|--------|
| Première utilisation | Non | Vide | ✅ Autoriser (pas de vérif possible) |
| App compatible | Oui | - | ✅ Autoriser + mettre à jour cache |
| App trop ancienne | Oui | - | 🚫 Bloquer + enregistrer dans cache |
| Offline avec cache récent | Non | Valide | ✅ Autoriser si app >= cached_min |
| Offline avec app bloquée | Non | Bloqué | 🚫 Bloquer (même offline) |

---

## Résolution des conflits

### Détection de conflit

Un conflit est détecté quand:
```
local.updatedAt > lastSyncTime
AND remote.updatedAt > lastSyncTime
AND local.updatedAt != remote.updatedAt
```

### Stratégies par type d'entité

| Type d'entité | Stratégie | Justification |
|---------------|-----------|---------------|
| **Product** | Server Wins | Référentiel central, cohérence catalogue |
| **Category** | Server Wins | Données de configuration partagées |
| **Site** | Server Wins | Structure organisationnelle |
| **PackagingType** | Server Wins | Configuration système |
| **Sale** | Client Wins | Ventes offline sont des transactions valides |
| **SaleItem** | Client Wins | Lié aux ventes |
| **StockMovement** | Merge | Les deux mouvements sont indépendants |
| **PurchaseBatch** | Server Wins | Données sensibles (coûts) |
| **Inventory** | Ask User | Comptages peuvent différer légitimement |
| **Customer** | Merge | Fusionner les informations |
| **User** | Server Wins | Sécurité |
| **UserPermission** | Server Wins | Sécurité |

### Résolution automatique vs manuelle

```
AUTO-RÉSOLU:
├── Server Wins → Appliquer remote, ignorer local
├── Client Wins → Appliquer local au serveur
├── Merge → Fusionner les champs modifiés
└── Keep Both → Créer une copie avec nouvel ID

INTERVENTION REQUISE:
└── Ask User → Afficher dialogue de choix
    ├── Garder ma version
    ├── Garder version serveur
    └── Fusionner manuellement
```

---

## Composants implémentés

### 1. Entités et DAO

| Fichier | Description |
|---------|-------------|
| `SyncQueueItem.kt` | Entité Room pour la queue de sync |
| `SyncQueueDao.kt` | DAO avec opérations CRUD et consolidation |
| `SyncTypeConverters.kt` | Convertisseurs Room pour enums |

### 2. Logique de synchronisation

| Fichier | Description |
|---------|-------------|
| `SyncQueueProcessor.kt` | Moteur de traitement avec retry |
| `SyncQueueHelper.kt` | Helper pour enqueue les opérations |
| `ConflictResolver.kt` | Stratégies de résolution par entité |
| `SyncStatusManager.kt` | État observable pour l'UI |

### 3. Gestion des versions

| Fichier | Description |
|---------|-------------|
| `SchemaVersionChecker.kt` | Vérification avec cache offline |
| `MigrationManager.kt` | Exécution des migrations SQL |
| `AppUpdateRequiredActivity.kt` | Écran de blocage mise à jour |

### 4. Migration Supabase

| Fichier | Description |
|---------|-------------|
| `2026011801_sync_tracking.sql` | Tables sync_log, sync_conflicts, row_version |

### 5. UI

| Fichier | Description |
|---------|-------------|
| `SyncIndicatorView.kt` | Composant indicateur de sync |
| `view_sync_indicator.xml` | Layout du composant |
| `ic_sync*.xml` | Icônes de synchronisation |

---

## Guide d'intégration

### 1. Ajouter l'enqueue aux repositories

```kotlin
class AuditedProductRepository(context: Context) {
    private val syncHelper = SyncQueueHelper(context)

    suspend fun insert(product: Product) {
        productDao.insert(product)
        auditLogger.logInsert(...)

        // NOUVEAU: Enqueue pour sync
        syncHelper.enqueueProductInsert(product, currentUserId)
    }

    suspend fun update(product: Product) {
        val oldProduct = productDao.getById(product.id)
        productDao.update(product)
        auditLogger.logUpdate(...)

        // NOUVEAU: Enqueue avec version remote
        syncHelper.enqueueProductUpdate(
            product,
            remoteUpdatedAt = oldProduct?.remoteUpdatedAt,
            userId = currentUserId
        )
    }
}
```

### 2. Observer le statut de sync dans l'UI

```kotlin
class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ajouter l'indicateur dans la toolbar
        val syncIndicator = findViewById<SyncIndicatorView>(R.id.syncIndicator)
        syncIndicator.bind(this)

        syncIndicator.setOnClickListener {
            // Ouvrir les détails de sync
            startActivity(Intent(this, SyncDetailsActivity::class.java))
        }
    }
}
```

### 3. Déclencher la sync manuellement

```kotlin
// Dans un ViewModel ou Activity
fun forceSyncNow() {
    val processor = SyncQueueProcessor(context)

    lifecycleScope.launch {
        processor.events.collect { event ->
            when (event) {
                is SyncEvent.ProcessingCompleted -> {
                    showToast("Sync terminée: ${event.success} réussi(s)")
                }
                is SyncEvent.ConflictDetected -> {
                    showConflictDialog(event.conflict)
                }
            }
        }
    }

    processor.startProcessing()
}
```

### 4. Vérifier la compatibilité au démarrage

```kotlin
class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val checker = SchemaVersionChecker(this@LoginActivity)

            when (val result = checker.checkCompatibility()) {
                is VersionCheckResult.UpdateRequired -> {
                    startActivity(Intent(this@LoginActivity, AppUpdateRequiredActivity::class.java).apply {
                        putExtra(EXTRA_MIN_REQUIRED, result.minRequired)
                    })
                    finish()
                }
                is VersionCheckResult.OfflineBlocked -> {
                    // Même traitement
                }
                else -> {
                    // Continuer normalement
                    proceedToLogin()
                }
            }
        }
    }
}
```

---

## Prochaines étapes

1. **Intégrer SyncQueueHelper** dans tous les repositories audités
2. **Ajouter SyncIndicatorView** dans les layouts d'activités principales
3. **Créer SyncDetailsActivity** pour afficher l'historique et conflits
4. **Implémenter les dialogues de résolution** de conflits manuels
5. **Ajouter row_version** aux entités Room locales (optionnel)
6. **Tests d'intégration** pour les scénarios de conflit

---

## Annexe: Diagramme de séquence - Sync complète

```
User               App                  SyncQueue           Supabase
  │                 │                      │                   │
  │  Modification   │                      │                   │
  │────────────────>│                      │                   │
  │                 │  enqueue(item)       │                   │
  │                 │─────────────────────>│                   │
  │                 │                      │                   │
  │  [Mode Online]  │                      │                   │
  │                 │  process()           │                   │
  │                 │─────────────────────>│                   │
  │                 │                      │  fetch remote     │
  │                 │                      │──────────────────>│
  │                 │                      │  remote data      │
  │                 │                      │<──────────────────│
  │                 │                      │                   │
  │                 │                      │  [No Conflict]    │
  │                 │                      │  upsert           │
  │                 │                      │──────────────────>│
  │                 │                      │  OK               │
  │                 │                      │<──────────────────│
  │                 │                      │                   │
  │                 │                      │  [Conflict]       │
  │                 │                      │  resolve()        │
  │                 │  ConflictEvent       │                   │
  │                 │<─────────────────────│                   │
  │  Dialog choix   │                      │                   │
  │<────────────────│                      │                   │
  │  Résolution     │                      │                   │
  │────────────────>│                      │                   │
  │                 │  resolveConflict()   │                   │
  │                 │─────────────────────>│                   │
  │                 │                      │  apply resolution │
  │                 │                      │──────────────────>│
  │                 │                      │                   │
```
