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
├── android/                        # Tests Android (11 tests)
│   ├── 01_authentication.yaml      # Login, logout, invalid credentials
│   ├── 02_sites_crud.yaml          # Sites CRUD operations
│   ├── 03_products_crud.yaml       # Products CRUD operations
│   ├── 04_categories_crud.yaml     # Categories CRUD operations
│   ├── 05_customers_crud.yaml      # Customers CRUD operations
│   ├── 06_packaging_types_crud.yaml # Packaging types CRUD
│   ├── 07_users_crud.yaml          # Users CRUD operations
│   ├── 08_purchases.yaml           # Purchase products flow
│   ├── 09_sales.yaml               # Sell products flow
│   ├── 10_transfers.yaml           # Transfer products between sites
│   └── 11_inventory.yaml           # Inventory count operations
└── ios/                            # Tests iOS (11 tests)
    ├── 01_authentication.yaml
    ├── 02_sites_crud.yaml
    ├── 03_products_crud.yaml
    ├── 04_categories_crud.yaml
    ├── 05_customers_crud.yaml
    ├── 06_packaging_types_crud.yaml
    ├── 07_users_crud.yaml
    ├── 08_purchases.yaml
    ├── 09_sales.yaml
    ├── 10_transfers.yaml
    └── 11_inventory.yaml
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
maestro test .maestro/android/03_products_crud.yaml
maestro test .maestro/android/04_categories_crud.yaml
maestro test .maestro/android/05_customers_crud.yaml
maestro test .maestro/android/06_packaging_types_crud.yaml
maestro test .maestro/android/07_users_crud.yaml
maestro test .maestro/android/08_purchases.yaml
maestro test .maestro/android/09_sales.yaml
maestro test .maestro/android/10_transfers.yaml
maestro test .maestro/android/11_inventory.yaml

# Utiliser le script de lancement
./scripts/run_tests_android.sh
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

# Tous les tests iOS (important: utiliser -p ios si Android emulator est actif)
maestro -p ios test .maestro/ios/

# Un test specifique
maestro -p ios test .maestro/ios/01_authentication.yaml
maestro -p ios test .maestro/ios/02_sites_crud.yaml
maestro -p ios test .maestro/ios/03_products_crud.yaml
maestro -p ios test .maestro/ios/04_categories_crud.yaml
maestro -p ios test .maestro/ios/05_customers_crud.yaml
maestro -p ios test .maestro/ios/06_packaging_types_crud.yaml
maestro -p ios test .maestro/ios/07_users_crud.yaml
maestro -p ios test .maestro/ios/08_purchases.yaml
maestro -p ios test .maestro/ios/09_sales.yaml
maestro -p ios test .maestro/ios/10_transfers.yaml
maestro -p ios test .maestro/ios/11_inventory.yaml

# Utiliser le script de lancement
./scripts/run_tests_ios.sh

# Lancer Android et iOS en parallele
./scripts/run_tests_all.sh
```

**Note importante** : Lorsque l'emulateur Android et le simulateur iOS sont actifs simultanement, utilisez le flag `-p ios` pour forcer Maestro a cibler iOS.

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

### 3.7 Tests Maestro inclus (22 tests - 11 Android + 11 iOS)

#### 01_authentication.yaml
**Objectif** : Valider le flux d'authentification complet.

| Etape | Action | Validation |
|-------|--------|------------|
| Login valide | Saisie admin/admin, clic Login | Ecran Home affiche "MediStock" et "Operations" |
| Logout | Clic profil puis Logout | Retour ecran login, "Login" visible |
| Login invalide | Saisie wronguser/wrongpassword | Message erreur "not found" affiche |

#### 02_sites_crud.yaml
**Objectif** : Valider les operations CRUD sur les sites.

| Etape | Action | Validation |
|-------|--------|------------|
| Navigation | Clic "Site Management" | Ecran liste sites affiche |
| Create | Clic +, saisie "Test Site A", Save | Site cree, liste mise a jour |
| Read | Retour liste | "Test Site A" visible dans la liste |
| Update | Selection site, modification nom, Save | Nom modifie visible |
| Delete | Selection site, clic Delete | Site supprime de la liste |

#### 03_products_crud.yaml
**Objectif** : Valider les operations CRUD sur les produits.

| Etape | Action | Validation |
|-------|--------|------------|
| Navigation | Clic "Manage Products" | Ecran liste produits affiche |
| Create | Clic +, saisie nom/description/prix, Save | Produit cree |
| Read | Retour liste | Produit visible |
| Update | Selection, modification, Save | Modifications visibles |
| Delete | Selection, clic Delete | Produit supprime |

#### 04_categories_crud.yaml
**Objectif** : Valider les operations CRUD sur les categories.

| Etape | Action | Validation |
|-------|--------|------------|
| Navigation | Clic "Manage Categories" | Ecran liste categories affiche |
| Create | Clic +, saisie nom, Save | Categorie creee |
| Read | Retour liste | Categorie visible |
| Update | Selection, modification nom, Save | Nom modifie |
| Delete | Selection, clic Delete | Categorie supprimee |

#### 05_customers_crud.yaml
**Objectif** : Valider les operations CRUD sur les clients.

| Etape | Action | Validation |
|-------|--------|------------|
| Navigation | Clic "Manage Customers" | Ecran liste clients affiche |
| Create | Clic +, saisie infos client, Save | Client cree |
| Read | Retour liste | Client visible |
| Update | Selection, modification, Save | Modifications visibles |
| Delete | Selection, clic Delete | Client supprime |

#### 06_packaging_types_crud.yaml
**Objectif** : Valider les operations CRUD sur les types d'emballage.

| Etape | Action | Validation |
|-------|--------|------------|
| Navigation | Clic "Packaging Types" | Ecran liste emballages affiche |
| Create | Clic +, saisie nom/unite, Save | Type emballage cree |
| Read | Retour liste | Type visible |
| Update | Selection, modification, Save | Modifications visibles |
| Delete | Selection, clic Delete | Type supprime |

#### 07_users_crud.yaml
**Objectif** : Valider les operations CRUD sur les utilisateurs.

| Etape | Action | Validation |
|-------|--------|------------|
| Navigation | Clic "User Management" | Ecran liste utilisateurs affiche |
| Create | Clic +, saisie username/password/role, Save | Utilisateur cree |
| Read | Retour liste | Utilisateur visible |
| Update | Selection, modification role, Save | Role modifie |
| Delete | Selection, clic Delete | Utilisateur supprime |

#### 08_purchases.yaml
**Objectif** : Valider l'acces a l'ecran d'achat de produits.

| Etape | Action | Validation |
|-------|--------|------------|
| Login | Saisie credentials, Login | Ecran Home affiche |
| Navigation | Clic "Purchase Products" | Ecran achats accessible |
| Retour | Clic retour/MediStock | Retour ecran Home |

#### 09_sales.yaml
**Objectif** : Valider l'acces a l'ecran de vente de produits.

| Etape | Action | Validation |
|-------|--------|------------|
| Login | Saisie credentials, Login | Ecran Home affiche |
| Navigation | Clic "Sell Products" | Ecran ventes accessible |
| Retour | Clic retour/MediStock | Retour ecran Home |

#### 10_transfers.yaml
**Objectif** : Valider l'acces a l'ecran de transferts inter-sites.

| Etape | Action | Validation |
|-------|--------|------------|
| Login | Saisie credentials, Login | Ecran Home affiche |
| Navigation | Clic "Transfer Products" | Ecran transferts accessible |
| Retour | Clic retour | Retour ecran Home |

#### 11_inventory.yaml
**Objectif** : Valider l'acces a l'ecran d'inventaire.

| Etape | Action | Validation |
|-------|--------|------------|
| Login | Saisie credentials, Login | Ecran Home affiche |
| Navigation | Clic "Inventory Stock" | Ecran inventaire accessible |
| Retour | Clic retour | Retour ecran Home |

---

### 3.8 Correspondance avec le Cahier de Recette

| Section du Cahier | Test Maestro |
|-------------------|--------------|
| 2. Authentification | 01_authentication.yaml |
| 3.1 Gestion des sites | 02_sites_crud.yaml |
| 3.2 Gestion des produits | 03_products_crud.yaml |
| 3.3 Gestion des categories | 04_categories_crud.yaml |
| 3.4 Gestion des clients | 05_customers_crud.yaml |
| 3.5 Gestion des emballages | 06_packaging_types_crud.yaml |
| 3.6 Gestion des utilisateurs | 07_users_crud.yaml |
| 4. Gestion des achats | 08_purchases.yaml |
| 5. Gestion des ventes | 09_sales.yaml |
| 8. Transferts inter-sites | 10_transfers.yaml |
| 9. Inventaire | 11_inventory.yaml |

---

### 3.9 Scripts de Lancement

Trois scripts sont fournis dans le dossier `scripts/` :

| Script | Description |
|--------|-------------|
| `run_tests_android.sh` | Lance tous les tests Android (verifie emulateur, installe l'app) |
| `run_tests_ios.sh` | Lance tous les tests iOS (verifie simulateur, utilise `-p ios`) |
| `run_tests_all.sh` | Lance Android et iOS en parallele |

```bash
# Rendre les scripts executables (si necessaire)
chmod +x scripts/run_tests_*.sh

# Lancer les tests
./scripts/run_tests_android.sh
./scripts/run_tests_ios.sh
./scripts/run_tests_all.sh
```

---

### 3.10 Depannage Maestro

| Probleme | Solution |
|----------|----------|
| App non trouvee | Verifier avec `adb shell pm list packages \| grep medistock` (Android) ou `xcrun simctl listapps booted` (iOS) |
| Element non trouve | Utiliser `maestro studio` pour inspecter l'UI |
| Timeout sur animations | Augmenter le timeout : `timeout: 15000` |
| Simulateur non demarre | `xcrun simctl boot "iPhone 15"` ou `emulator -avd <nom>` |
| Maestro cible Android au lieu d'iOS | Utiliser le flag `-p ios` : `maestro -p ios test ...` |
| iOS app affiche du francais | Rebuilder l'app iOS et reinstaller sur le simulateur |
| NavigationLink ne repond pas sur iOS | Utiliser `retryTapIfNoChange: true` dans le test |

---

### 3.11 Phase 11 - Integrite Referentielle & Soft Delete

#### Statut de l'implementation

La Phase 11 introduit la desactivation (soft delete) pour Sites, Categories, Products, et Customers via le champ `is_active`. Le schema et le service `ReferentialIntegrityService` sont implementes, mais **l'UI n'a pas encore ete modifiee** pour utiliser ces fonctionnalites.

#### Impact sur les tests E2E existants

✅ **Aucun impact sur les tests actuels** :
- Les 22 tests E2E existants (11 Android + 11 iOS) continuent de fonctionner
- Les tests creent et suppriment des entites propres (non utilisees ailleurs)
- Le champ `is_active` a une valeur par defaut de 1 (actif)
- L'UI n'integre pas encore les appels au `ReferentialIntegrityService`

#### Tests futurs a ajouter (quand l'UI sera implementee)

Lorsque l'UI de desactivation sera prete, ajouter ~16-18 nouveaux tests :

| Test | Description | Fichiers |
|------|-------------|----------|
| Desactivation Sites | Tenter de supprimer un site utilise, verifier dialogue de desactivation | `12_deactivation_sites.yaml` |
| Desactivation Products | Tenter de supprimer un produit utilise, verifier desactivation | `13_deactivation_products.yaml` |
| Desactivation Categories | Tenter de supprimer une categorie utilisee | `14_deactivation_categories.yaml` |
| Desactivation Customers | Tenter de supprimer un client utilise | `15_deactivation_customers.yaml` |
| Filtres Inactifs | Basculer affichage entites inactives dans ecrans admin | `16_inactive_filters.yaml` |
| Reactivation | Reactiver une entite desactivee | `17_reactivation.yaml` |

#### Scenarios de test Phase 11

**Scenario 1 : Desactivation d'un site utilise**
1. Creer Site A
2. Creer Produit X au Site A
3. Tenter de supprimer Site A
4. Verifier dialogue : "Site utilise dans 1 produit"
5. Confirmer desactivation
6. Verifier Site A marque "Inactive"
7. Verifier Site A absent des dropdowns de creation

**Scenario 2 : Suppression d'une entite propre**
1. Creer Categorie Z
2. Tenter de supprimer Categorie Z (non utilisee)
3. Confirmer suppression directe (pas de desactivation)
4. Verifier Categorie Z completement supprimee

**Scenario 3 : Filtrage des entites inactives**
1. Acceder a l'ecran "Site Management"
2. Activer le toggle "Afficher inactifs"
3. Verifier que les sites inactifs apparaissent avec indicateur
4. Desactiver le toggle
5. Verifier que les sites inactifs sont masques

📋 **Documentation complete** : Voir `.maestro/PHASE11_E2E_ANALYSIS.md` pour l'analyse detaillee, les user journeys complets, et les templates de tests.

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

# Tests E2E Android (11 tests)
maestro test .maestro/android/

# Tests E2E iOS (11 tests)
maestro -p ios test .maestro/ios/

# Tous les tests E2E (Android + iOS)
./scripts/run_tests_all.sh

# Mode debug Maestro
maestro studio
```

---

## 10. Ressources

- [JUnit 5 Documentation](https://junit.org/junit5/)
- [Maestro Documentation](https://maestro.mobile.dev/)
- [SQLDelight Testing](https://cashapp.github.io/sqldelight/2.0.0/)
- [Kotlin Coroutines Testing](https://kotlinlang.org/docs/coroutines-testing.html)
