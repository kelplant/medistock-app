# Guide des Tests Unitaires - Medistock App

## 📋 Vue d'ensemble

Ce projet dispose maintenant d'une suite complète de tests unitaires couvrant les composants critiques de l'application.

## 🧪 Structure des Tests

```
app/src/
├── test/                           # Tests unitaires (JVM)
│   └── java/com/medistock/
│       ├── data/
│       │   ├── dao/                # Tests des DAOs (ProductDao, PurchaseBatchDao, SaleDao, FIFO)
│       │   ├── entities/           # Tests de validation des entités
│       │   └── repository/         # Tests des repositories auditées
│       └── util/                   # Tests des utilitaires (AuthManager, PermissionManager, PasswordHasher)
│
└── androidTest/                    # Tests d'instrumentation Android
    └── java/com/medistock/
        ├── data/dao/               # Tests DAO avec base réelle
        ├── ui/                     # Tests UI Espresso
        └── integration/            # Tests d'intégration end-to-end
```

## 🚀 Exécuter les Tests

### Tous les tests unitaires
```bash
./gradlew test
```

### Tests d'un module spécifique
```bash
./gradlew test --tests "com.medistock.data.dao.*"
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
app/build/reports/tests/testDebugUnitTest/index.html
```

## 📊 Couverture des Tests

### Tests Critiques (P0) ✅
- ✅ **ProductDao** : 10 tests - CRUD, filtrage par site, jointures
- ✅ **PurchaseBatchDao** : 14 tests - FIFO, quantités, dates d'expiration
- ✅ **SaleDao** : 9 tests - Ventes avec items, transactions
- ✅ **FIFO Allocation** : 8 tests - Allocation multi-lots, épuisement
- ✅ **AuthManager** : 11 tests - Login, logout, sessions
- ✅ **PermissionManager** : 11 tests - Permissions granulaires, admin bypass
- ✅ **PasswordHasher** : 13 tests - BCrypt hashing, vérification

### Tests Importants (P1) ✅
- ✅ **AuditedProductRepository** : 4 tests - Audit logging
- ✅ **ProductViewModel** : 5 tests - StateFlow, calculs de marge
- ✅ **Entity Validation** : 13 tests - Validation entités, contraintes

### Total : ~98 tests unitaires

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

### CI/CD
Le projet dispose déjà d'une GitHub Action pour les releases.
Considérer l'ajout d'une étape `./gradlew test` dans le workflow.

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
