# Guide des Tests - Medistock App

## Vue d'ensemble

Ce projet dispose d'une suite complète de tests couvrant les composants critiques de l'application, organisée en quatre niveaux :

| Niveau | Type | Outil | Description |
|--------|------|-------|-------------|
| 1 | Tests Shared (KMM) | JUnit | Logique métier partagée Android/iOS |
| 2 | Tests d'intégration JVM | JUnit 5 | FIFO, workflows, inventaires |
| 3 | Tests Android | JUnit | Tests spécifiques plateforme |
| 4 | Tests E2E | Maestro | Tests UI automatisés Android/iOS |

---

## Structure des Tests

```
shared/src/
├── commonTest/                     # Tests partagés KMM (Android + iOS)
│   └── kotlin/com/medistock/shared/
│       ├── UseCaseTests.kt              # 29 tests - UseCases et regles metier
│       ├── DtoTests.kt                  # 30 tests - Serialisation DTOs
│       ├── SyncInfrastructureTests.kt   # 32 tests - Infrastructure sync
│       ├── AuthTests.kt                 # 22 tests - Authentification
│       ├── PermissionAndSyncTests.kt    # 22 tests - Permissions et sync
│       ├── CompatibilityTests.kt        # 13 tests - Compatibilite
│       ├── ModelTests.kt                # 11 tests - Modeles
│       └── AuditServiceTests.kt         # 4 tests - Service audit
│
├── jvmTest/                        # Tests d'integration JVM
│   └── kotlin/com/medistock/shared/
│       ├── FifoIntegrationTests.kt      # 5 tests FIFO
│       ├── WorkflowIntegrationTests.kt  # 8 tests workflows
│       └── InventoryIntegrationTests.kt # 8 tests inventaires

app/src/
├── test/                           # Tests unitaires Android (JVM)
│   └── java/com/medistock/util/
│       └── PasswordHasherTest.kt        # 16 tests - BCrypt hashing
│
└── androidTest/                    # Tests d'instrumentation Android
    └── java/com/medistock/data/sync/
        └── SyncManagerTest.kt           # 2 tests - Sync manager

.maestro/                           # Tests E2E Maestro
├── config.yaml                     # Configuration globale
├── shared/                         # Flows reutilisables
│   └── login.yaml
├── android/                        # Tests Android
│   ├── 01_authentication.yaml
│   ├── 02_sites_crud.yaml
│   ├── 03_purchase_sale_workflow.yaml
│   ├── 04_transfers.yaml
│   └── 05_inventory.yaml
└── ios/                            # Tests iOS
    ├── 01_authentication.yaml
    ├── 02_sites_crud.yaml
    ├── 03_purchase_sale_workflow.yaml
    ├── 04_transfers.yaml
    └── 05_inventory.yaml
```

---

## 1. Tests Unitaires Shared (KMM)

### Executer les tests

```bash
# Tous les tests shared (Android + iOS)
./gradlew :shared:allTests

# Tests shared sur simulateur iOS uniquement
./gradlew :shared:iosSimulatorArm64Test

# Tests shared sur Android uniquement
./gradlew :shared:testDebugUnitTest
```

### Couverture (163 tests)

| Fichier | Tests | Description |
|---------|-------|-------------|
| `SyncInfrastructureTests.kt` | 32 | RetryConfiguration, ConflictResolver, SyncQueue |
| `DtoTests.kt` | 30 | Serialisation/deserialisation de tous les DTOs |
| `UseCaseTests.kt` | 29 | Inputs/outputs UseCases, BusinessError, BusinessWarning |
| `AuthTests.kt` | 22 | Authentification, sessions, tokens |
| `PermissionAndSyncTests.kt` | 22 | Permissions granulaires, sync bidirectionnelle |
| `CompatibilityTests.kt` | 13 | Compatibilite entre plateformes |
| `ModelTests.kt` | 11 | Site, Product, User, PurchaseBatch, Sale |
| `AuditServiceTests.kt` | 4 | Service d'audit et logging |

---

## 2. Tests d'Integration JVM

Ces tests utilisent une base SQLite en memoire pour tester les workflows complets.

### Executer les tests

```bash
# Tous les tests d'integration JVM
./gradlew :shared:jvmTest

# Avec rapport detaille
./gradlew :shared:jvmTest --info
```

### Tests FIFO (`FifoIntegrationTests.kt`)

| Test | Description |
|------|-------------|
| `sale consumes oldest batch first (FIFO)` | Le lot le plus ancien est consomme en premier |
| `sale spanning multiple batches` | Vente repartie sur plusieurs lots |
| `sale tracks correct purchase cost for profit` | Cout d'achat correct pour calcul benefice |
| `sale with insufficient stock returns warning` | Warning si stock insuffisant |
| `sale skips exhausted batches` | Les lots epuises sont ignores |

### Tests Workflows (`WorkflowIntegrationTests.kt`)

| Test | Description |
|------|-------------|
| `purchase creates batch and stock movement` | Achat cree lot et mouvement |
| `purchase with margin calculates selling price` | Calcul prix vente avec marge |
| `complete purchase to sale workflow with profit` | Workflow complet achat->vente |
| `multiple purchases and sales` | Achats et ventes multiples |
| `transfer between sites uses FIFO` | Transfert inter-sites FIFO |
| `transfer to same site fails` | Transfert meme site echoue |
| `multi-site stock workflow` | Workflow multi-sites |
| `audit trail is created for all operations` | Trace audit operations |

### Tests Inventaire (`InventoryIntegrationTests.kt`)

| Test | Description |
|------|-------------|
| `inventory adjustment creates surplus batch` | Surplus cree nouveau lot |
| `inventory adjustment deducts from batches (FIFO)` | Deficit deduit en FIFO |
| `inventory creates surplus batch when positive` | Comptage positif = lot |
| `inventory with multiple products` | Ajustements independants |
| `inventory creates audit entry` | Entree audit creee |
| `inventory with zero quantity no adjustment` | Zero = pas d'ajustement |
| `inventory creates stock movement` | Mouvement stock cree |
| `inventory session has correct metadata` | Metadonnees session |

### Rapports

Les rapports HTML sont generes dans :
```
shared/build/reports/tests/jvmTest/index.html
```

---

## 3. Tests Android

### Tests unitaires (16 tests)

```bash
# Executer les tests unitaires Android
./gradlew :app:testDebugUnitTest
```

| Fichier | Tests | Description |
|---------|-------|-------------|
| `PasswordHasherTest.kt` | 16 | BCrypt hashing, verification, salt aleatoire |

### Tests d'instrumentation (2 tests)

```bash
# Executer les tests d'instrumentation (necessite emulateur/device)
./gradlew :app:connectedAndroidTest
```

| Fichier | Tests | Description |
|---------|-------|-------------|
| `SyncManagerTest.kt` | 2 | Sync manager avec base reelle |

### Rapports

```
app/build/reports/tests/testDebugUnitTest/index.html
app/build/reports/androidTests/connected/index.html
```

---

## 4. Tests E2E avec Maestro

Maestro permet d'executer des tests UI automatises sur Android et iOS.

### 3.1 Installation de Maestro

#### macOS (Homebrew)

```bash
brew tap mobile-dev-inc/tap
brew install maestro
```

#### Verification

```bash
maestro --version
```

#### Autres systemes

Voir la [documentation officielle](https://maestro.mobile.dev/getting-started/installing-maestro).

---

### 3.2 Tests Android

#### Prerequis

```bash
# Demarrer un emulateur Android (ou connecter un appareil)
emulator -avd Pixel_6_API_34   # ou le nom de votre AVD

# Verifier que l'emulateur est detecte
adb devices
```

#### Installer l'application

```bash
cd /path/to/medistock-app
./gradlew :app:installDebug
```

#### Lancer les tests

```bash
# Tous les tests Android
maestro test .maestro/android/

# Un test specifique
maestro test .maestro/android/01_authentication.yaml
maestro test .maestro/android/02_sites_crud.yaml
maestro test .maestro/android/03_purchase_sale_workflow.yaml
maestro test .maestro/android/04_transfers.yaml
maestro test .maestro/android/05_inventory.yaml
```

---

### 3.3 Tests iOS

#### Prerequis

```bash
# Lister les simulateurs disponibles
xcrun simctl list devices

# Demarrer un simulateur (exemple iPhone 15)
open -a Simulator
# ou
xcrun simctl boot "iPhone 15"
```

#### Compiler et installer l'application

```bash
cd /path/to/medistock-app/iosApp

# Compiler
xcodebuild -workspace iosApp.xcworkspace \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  build

# Installer sur le simulateur
xcrun simctl install booted build/Build/Products/Debug-iphonesimulator/iosApp.app
```

#### Lancer les tests

```bash
cd /path/to/medistock-app

# Tous les tests iOS
maestro test .maestro/ios/

# Un test specifique
maestro test .maestro/ios/01_authentication.yaml
maestro test .maestro/ios/02_sites_crud.yaml
maestro test .maestro/ios/03_purchase_sale_workflow.yaml
maestro test .maestro/ios/04_transfers.yaml
maestro test .maestro/ios/05_inventory.yaml
```

---

### 3.4 Mode interactif (developpement/debug)

```bash
# Lancer Maestro Studio pour inspecter l'UI
maestro studio
```

Cela ouvre une interface web ou vous pouvez :
- Voir la hierarchie des elements UI
- Tester des commandes Maestro en temps reel
- Debugger les selecteurs qui ne fonctionnent pas

---

### 3.5 Generer un rapport

```bash
# Rapport JUnit XML
maestro test .maestro/android/ --format junit --output report.xml

# Rapport HTML (avec screenshots)
maestro test .maestro/android/ --format html --output report/
```

---

### 3.6 Variables d'environnement

Les tests utilisent des variables qui peuvent etre personnalisees :

| Variable | Valeur par defaut | Description |
|----------|-------------------|-------------|
| TEST_USERNAME | admin | Nom d'utilisateur pour les tests |
| TEST_PASSWORD | admin123 | Mot de passe pour les tests |
| TEST_SITE | Site Test | Nom du site de test |
| TEST_PRODUCT | Produit Test | Nom du produit de test |

```bash
# Personnaliser les identifiants
maestro test \
  -e TEST_USERNAME=monuser \
  -e TEST_PASSWORD=monpassword \
  .maestro/android/
```

---

### 3.7 Tests Maestro inclus

#### 01_authentication.yaml
- Connexion avec identifiants valides
- Deconnexion
- Connexion avec identifiants invalides

#### 02_sites_crud.yaml
- Creation d'un site
- Modification d'un site
- Suppression d'un site

#### 03_purchase_sale_workflow.yaml
- Creation d'un achat (ajout de stock)
- Verification du stock apres achat
- Creation d'une vente (FIFO)
- Verification du stock apres vente
- Verification du calcul des benefices

#### 04_transfers.yaml
- Creation de sites source et destination
- Creation d'un transfert inter-sites
- Verification du stock apres transfert

#### 05_inventory.yaml
- Demarrage d'une session d'inventaire
- Comptage avec ecart (discrepancy)
- Validation et ajustement du stock
- Verification de l'audit

---

### 3.8 Correspondance avec le Cahier de Recette

| Section du Cahier | Test Maestro |
|-------------------|--------------|
| 2. Authentification | 01_authentication.yaml |
| 3.1 Gestion des sites | 02_sites_crud.yaml |
| 4. Gestion des achats | 03_purchase_sale_workflow.yaml |
| 5. Gestion des ventes | 03_purchase_sale_workflow.yaml |
| 6. Logique FIFO | 03_purchase_sale_workflow.yaml |
| 8. Transferts inter-sites | 04_transfers.yaml |
| 9. Inventaire | 05_inventory.yaml |

---

### 3.9 Depannage Maestro

| Probleme | Solution |
|----------|----------|
| App non trouvee | Verifier avec `adb shell pm list packages \| grep medistock` (Android) ou `xcrun simctl listapps booted` (iOS) |
| Element non trouve | Utiliser `maestro studio` pour inspecter l'UI |
| Timeout sur animations | Augmenter le timeout : `timeout: 15000` |
| Simulateur non demarre | `xcrun simctl boot "iPhone 15"` ou `emulator -avd <nom>` |

---

## 5. CI/CD

Le workflow GitHub Actions (`.github/workflows/ci.yml`) execute automatiquement les tests :

| Job | Commande | Plateforme |
|-----|----------|------------|
| `test-shared` | `./gradlew :shared:allTests` | Linux |
| `test-jvm` | `./gradlew :shared:jvmTest` | Linux |
| `build-android` | `./gradlew :app:testDebugUnitTest` | Linux |
| `build-ios` | `./gradlew :shared:iosSimulatorArm64Test` | macOS |

**Declencheurs :**
- Manuel (`workflow_dispatch`)
- Push de tags (`v*`)

---

## 6. Git Hooks

### Pre-commit Hook

Un hook Git `pre-commit` bloque automatiquement les commits si les tests echouent.

**Localisation** : `.git/hooks/pre-commit`

**Comportement** :
- Lance `./gradlew test` avant chaque commit
- Bloque le commit si des tests echouent
- Affiche un message clair avec le resultat

**Desactiver temporairement** (non recommande) :
```bash
git commit --no-verify -m "message"
```

---

## 7. Conventions de Tests

### Nommage

- Format : `methodName_condition_expectedResult()`
- Exemple : `canView_adminUser_returnsTrue()`

### Structure (Given-When-Then)

```kotlin
@Test
fun testName_condition_result() = runTest {
    // Given - Preparation
    val data = createTestData()

    // When - Action
    val result = performAction(data)

    // Then - Assertion
    assertEquals(expected, result)
}
```

---

## 8. Checklist Developpeur

Avant chaque commit :
- [ ] Tests unitaires passent : `./gradlew :shared:allTests`
- [ ] Tests d'integration passent : `./gradlew :shared:jvmTest`
- [ ] Nouveaux tests ajoutes pour nouveau code
- [ ] Tests E2E executes si UI modifiee

---

## 9. Resume des Commandes

```bash
# Tests unitaires shared
./gradlew :shared:allTests

# Tests d'integration JVM
./gradlew :shared:jvmTest

# Tests Android
./gradlew :app:testDebugUnitTest

# Tests E2E Android
maestro test .maestro/android/

# Tests E2E iOS
maestro test .maestro/ios/

# Mode debug Maestro
maestro studio
```

---

## 10. Ressources

- [JUnit 5 Documentation](https://junit.org/junit5/)
- [Maestro Documentation](https://maestro.mobile.dev/)
- [SQLDelight Testing](https://cashapp.github.io/sqldelight/2.0.0/)
- [Kotlin Coroutines Testing](https://kotlinlang.org/docs/coroutines-testing.html)
