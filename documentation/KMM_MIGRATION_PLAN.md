# MediStock - Plan de Migration KMM (Kotlin Multiplatform Mobile)

## Vue d'ensemble

Ce document décrit le plan de migration de MediStock vers une architecture KMM pour supporter iOS en plus d'Android.

## Architecture Cible

```
medistock-app/
├── shared/                          # Module KMM partagé
│   ├── src/
│   │   ├── commonMain/              # Code partagé (Kotlin)
│   │   │   ├── kotlin/
│   │   │   │   └── com/medistock/shared/
│   │   │   │       ├── domain/model/    # Modèles de données
│   │   │   │       ├── data/repository/ # Repositories
│   │   │   │       ├── Platform.kt      # Interface plateforme
│   │   │   │       └── MedistockSDK.kt  # Point d'entrée
│   │   │   └── sqldelight/              # Schéma SQLDelight
│   │   ├── androidMain/             # Implémentation Android
│   │   └── iosMain/                 # Implémentation iOS
│   └── build.gradle.kts
├── app/                             # App Android existante
├── iosApp/                          # Nouvelle app iOS (SwiftUI)
└── build.gradle
```

## Phases de Migration

### Phase 1 : Infrastructure KMM (COMPLÉTÉE)

- [x] Création du module `shared`
- [x] Configuration de Gradle pour KMM
- [x] Configuration de SQLDelight (remplacement de Room)
- [x] Création des fichiers Platform expect/actual
- [x] Création du squelette iOS

### Phase 2 : Migration des Modèles

- [x] Sites, Categories, Products
- [x] PurchaseBatch, Sale, SaleItem
- [x] User, UserPermission
- [x] Stock, StockMovement, ProductTransfer
- [ ] PackagingType, ProductPrice
- [ ] Inventory, Customer
- [ ] AuditHistory, SyncQueueItem

### Phase 3 : Migration des Repositories

- [x] SiteRepository (exemple)
- [x] ProductRepository (exemple)
- [ ] CategoryRepository
- [ ] PurchaseBatchRepository
- [ ] SaleRepository
- [ ] UserRepository
- [ ] StockRepository

### Phase 4 : Migration de la Logique Métier

- [ ] SyncManager → shared/data/sync/
- [ ] ConflictResolver → shared/data/sync/
- [ ] SyncQueueProcessor → shared/data/sync/
- [ ] AuthManager → shared/domain/auth/

### Phase 5 : Intégration Supabase Partagée

- [ ] SupabaseClient multiplateforme
- [ ] DTOs partagés
- [ ] Repositories Supabase partagés

### Phase 6 : App iOS

- [ ] Configuration Xcode complète
- [ ] UI SwiftUI pour toutes les fonctionnalités
- [ ] Tests iOS

## Technologies Utilisées

| Composant | Android (Avant) | Partagé (KMM) |
|-----------|-----------------|---------------|
| Base de données | Room | SQLDelight |
| HTTP Client | Ktor Android | Ktor Multiplatform |
| Sérialisation | kotlinx.serialization | kotlinx.serialization |
| Coroutines | kotlinx-coroutines-android | kotlinx-coroutines-core |
| Backend | Supabase Android | Supabase KMM |

## Commandes Utiles

### Compiler le module shared

```bash
./gradlew :shared:build
```

### Générer le framework iOS

```bash
./gradlew :shared:linkDebugFrameworkIosArm64
./gradlew :shared:linkReleaseFrameworkIosArm64
```

### Générer les classes SQLDelight

```bash
./gradlew :shared:generateSqlDelightInterface
```

### Build complet Android

```bash
./gradlew :app:assembleDebug
```

## Notes Importantes

### Compatibilité

- **Android minimum**: API 26 (Android 8.0)
- **iOS minimum**: iOS 15.0
- **Kotlin**: 2.0.21
- **Gradle**: 8.10.1

### Migration Progressive

L'app Android continue de fonctionner avec Room pendant la migration. Une fois les repositories partagés testés, on pourra basculer progressivement vers SQLDelight.

### Schéma de Base de Données

Le schéma SQLDelight dans `shared/src/commonMain/sqldelight/` est conçu pour être compatible avec le schéma Room existant, facilitant la migration des données.

## Prochaines Étapes

1. Compléter la migration des repositories restants
2. Migrer la logique de synchronisation
3. Développer l'UI iOS complète
4. Tests cross-platform
5. Migration progressive de l'app Android vers le module shared

## Ressources

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [SQLDelight Documentation](https://cashapp.github.io/sqldelight/)
- [Supabase Kotlin Documentation](https://supabase.com/docs/reference/kotlin/introduction)
