# Guide des Tests Unitaires - Medistock App

## 📋 Vue d'ensemble

Ce projet dispose d'une suite complète de tests couvrant les composants critiques de l'application, organisée en trois niveaux :
- **Tests Shared (KMM)** : Logique métier partagée entre Android et iOS
- **Tests Android** : Tests spécifiques à la plateforme Android
- **Tests d'instrumentation** : Tests avec base de données réelle

## 🧪 Structure des Tests

```
shared/src/
└── commonTest/                     # Tests partagés KMM (Android + iOS)
    └── kotlin/com/medistock/shared/
        ├── ModelTests.kt           # Tests des modèles (Site, Product, User, etc.)
        └── UseCaseTests.kt         # Tests des UseCases et règles métier

app/src/
├── test/                           # Tests unitaires Android (JVM)
│   └── java/com/medistock/
│       ├── data/
│       │   └── entities/           # Tests de validation des entités
│       └── util/                   # Tests des utilitaires (PasswordHasher, PermissionManager)
│
└── androidTest/                    # Tests d'instrumentation Android
    └── java/com/medistock/
        ├── data/dao/               # Tests DAO avec base réelle (FIFO, etc.)
        ├── data/repository/        # Tests des repositories
        └── ui/viewmodel/           # Tests ViewModels
```

## 🚀 Exécuter les Tests

### Tests du module Shared (KMM)
```bash
# Tous les tests shared (Android + iOS)
./gradlew :shared:allTests

# Tests shared sur simulateur iOS uniquement
./gradlew :shared:iosSimulatorArm64Test

# Tests shared sur Android uniquement
./gradlew :shared:testDebugUnitTest
```

### Tests Android
```bash
# Tests unitaires Android
./gradlew :app:testDebugUnitTest

# Tests d'un module spécifique
./gradlew test --tests "com.medistock.util.*"
```

### Tests avec rapport détaillé
```bash
./gradlew test --info
```

### Tests d'instrumentation Android
```bash
./gradlew connectedAndroidTest
```

### Rapport de couverture
Les rapports HTML des tests sont générés dans :
```
shared/build/reports/tests/           # Tests shared
app/build/reports/tests/testDebugUnitTest/index.html  # Tests Android
```

## 📊 Couverture des Tests

### Tests Shared (KMM) - Logique Métier ✅
- ✅ **UseCaseTests** : Tests des inputs/outputs UseCases
  - `PurchaseInput`, `SaleInput`, `TransferInput` validation
  - `BusinessError` (ValidationError, NotFound, SameSiteTransfer, etc.)
  - `BusinessWarning` (InsufficientStock, LowStock, ExpiringProduct)
  - `UseCaseResult` (Success, Error, hasWarnings, getOrThrow, map)
  - `PurchaseResult`, `SaleResult`, `TransferResult`
  - `MovementType` constants
- ✅ **ModelTests** : Tests des modèles partagés
  - Site, Product, User, PurchaseBatch, Sale, SaleItem

### Tests Android Critiques (P0) ✅
- ✅ **FifoAllocationTest** : 8 tests - Allocation multi-lots, épuisement
- ✅ **PurchaseBatchDaoTest** : FIFO, quantités, dates d'expiration
- ✅ **SaleDaoTest** : Ventes avec items, transactions
- ✅ **PermissionManager** : 11 tests - Permissions granulaires, admin bypass
- ✅ **PasswordHasher** : 13 tests - BCrypt hashing, vérification

### Tests Android Importants (P1) ✅
- ✅ **AuditedProductRepository** : 4 tests - Audit logging
- ✅ **ProductViewModel** : 5 tests - StateFlow, calculs de marge
- ✅ **Entity Validation** : 13 tests - Validation entités, contraintes

### Total : ~100+ tests (shared + Android)

## 🎯 Tests Clés

### 1. Tests FIFO (Critiques pour pharmacie)
```kotlin
fifoAllocation_multipleBatches_allocatesOldestFirst()
fifoAllocation_exhaustBatch_allocatesMultipleBatches()
fifoAllocation_excludesExhaustedBatches()
```

### 2. Tests Authentification
```kotlin
login_savesUserSession()
logout_clearsSession()
sessionPersistsAcrossInstances()
```

### 3. Tests Permissions
```kotlin
canView_adminUser_returnsTrue()  // Admin bypass
canCreate_nonAdminWithPermission_returnsTrue()
permissionHierarchy_viewDoesNotImplyCreate()
```

### 4. Tests Sécurité
```kotlin
hashPassword_producesDifferentHashesForSamePassword()  // Salt aléatoire
verifyPassword_correctPassword_returnsTrue()
verifyPassword_caseSensitive()
```

## 🔧 Configuration Git Hooks

### Pre-commit Hook
Un hook Git `pre-commit` a été configuré pour **bloquer automatiquement** les commits si les tests échouent.

**Localisation** : `.git/hooks/pre-commit`

**Comportement** :
- ✅ Lance `./gradlew test` avant chaque commit
- ❌ Bloque le commit si des tests échouent
- ✅ Affiche un message clair avec le résultat

**Désactiver temporairement** (non recommandé) :
```bash
git commit --no-verify -m "message"
```

## 📝 Conventions de Tests

### Nommage
- Format : `methodName_condition_expectedResult()`
- Exemple : `canView_adminUser_returnsTrue()`

### Structure (Given-When-Then)
```kotlin
@Test
fun testName_condition_result() = runTest {
    // Given - Préparation
    val data = createTestData()

    // When - Action
    val result = performAction(data)

    // Then - Assertion
    assertEquals(expected, result)
}
```

### Annotations
- `@Test` : Test unitaire standard
- `@Before` : Setup avant chaque test
- `@After` : Cleanup après chaque test
- `@RunWith(AndroidJUnit4::class)` : Tests Android
- `@OptIn(ExperimentalCoroutinesApi::class)` : Tests coroutines

## 🛠️ Dépendances de Test

```gradle
// Tests unitaires
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.mockito:mockito-core:5.3.1'
testImplementation 'org.mockito.kotlin:mockito-kotlin:5.0.0'
testImplementation 'androidx.arch.core:core-testing:2.2.0'
testImplementation 'kotlinx-coroutines-test:1.7.3'
testImplementation 'androidx.room:room-testing:2.6.1'
testImplementation 'app.cash.turbine:turbine:1.0.0'
testImplementation 'org.robolectric:robolectric:4.11.1'

// Tests Android
androidTestImplementation 'androidx.test.ext:junit:1.1.5'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
```

## 🐛 Debug des Tests

### Voir les logs détaillés
```bash
./gradlew test --info --stacktrace
```

### Tester un seul test
```bash
./gradlew test --tests "ProductDaoTest.insertProduct_insertsProductCorrectly"
```

### Re-run des tests qui ont échoué
```bash
./gradlew test --rerun-tasks
```

## 📈 Prochaines Étapes

### Tests à ajouter (optionnel)
1. **Tests UI Espresso** - Flux critiques (login, vente, achat)
2. **Tests d'intégration** - End-to-end avec base réelle
3. **Tests de performance** - Requêtes lourdes, syncs massifs
4. **Tests de migration** - Vérification migrations SQL

### CI/CD ✅
Le workflow GitHub Actions (`.github/workflows/ci.yml`) exécute automatiquement les tests :

| Job | Commande | Plateforme |
|-----|----------|------------|
| `test-shared` | `./gradlew :shared:allTests` | Linux |
| `build-android` | `./gradlew :app:testDebugUnitTest` | Linux |
| `build-ios` | `./gradlew :shared:iosSimulatorArm64Test` | macOS |

**Déclencheurs :**
- Manuel (`workflow_dispatch`)
- Push de tags (`v*`)

## ✅ Checklist Développeur

Avant chaque commit :
- [ ] Tous les tests passent localement (`./gradlew test`)
- [ ] Nouveaux tests ajoutés pour nouveau code
- [ ] Coverage maintenu au niveau critique
- [ ] Pas de tests désactivés sans raison

## 📚 Ressources

- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Mockito Documentation](https://site.mockito.org/)
- [Room Testing Guide](https://developer.android.com/training/data-storage/room/testing-db)
- [Coroutines Testing](https://kotlinlang.org/docs/coroutines-testing.html)

---

**Note** : Les tests sont maintenant **obligatoires** grâce au hook pre-commit. Toute modification du code doit passer tous les tests pour être committée.
