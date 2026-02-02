# MediStock E2E Tests with Maestro

Ce dossier contient les tests End-to-End (E2E) automatisés pour MediStock utilisant [Maestro](https://maestro.mobile.dev/).

## Installation de Maestro

### macOS (Homebrew)
```bash
brew tap mobile-dev-inc/tap
brew install maestro
```

### Autres systèmes
Voir la [documentation officielle](https://maestro.mobile.dev/getting-started/installing-maestro).

## Structure des Tests

```
.maestro/
├── config.yaml              # Configuration globale
├── shared/                  # Flows réutilisables
│   ├── login.yaml           # Flow de connexion
│   ├── logout.yaml          # Flow de déconnexion
│   └── clear_app_data.yaml  # Clear app data (first-time login state)
├── android/                 # Tests Android (17 tests)
│   ├── 01_authentication.yaml
│   ├── 02_sites_crud.yaml
│   ├── 03_products_crud.yaml
│   ├── 04_categories_crud.yaml
│   ├── 05_customers_crud.yaml
│   ├── 06_packaging_types_crud.yaml
│   ├── 07_users_crud.yaml
│   ├── 08_purchases.yaml
│   ├── 09_sales.yaml
│   ├── 10_transfers.yaml
│   ├── 11_inventory.yaml
│   ├── 12_language_switching.yaml
│   ├── 13_password_complexity.yaml
│   ├── 14_notification_center.yaml
│   ├── 15_online_first_auth.yaml
│   ├── 16_suppliers_crud.yaml
│   └── 17_purchase_with_supplier.yaml
├── ios/                     # Tests iOS (17 tests)
│   ├── 01_authentication.yaml
│   ├── 02_sites_crud.yaml
│   ├── 03_products_crud.yaml
│   ├── 04_categories_crud.yaml
│   ├── 05_customers_crud.yaml
│   ├── 06_packaging_types_crud.yaml
│   ├── 07_users_crud.yaml
│   ├── 08_purchases.yaml
│   ├── 09_sales.yaml
│   ├── 10_transfers.yaml
│   ├── 11_inventory.yaml
│   ├── 12_language_switching.yaml
│   ├── 13_password_complexity.yaml
│   ├── 14_notification_center.yaml
│   ├── 15_online_first_auth.yaml
│   ├── 16_suppliers_crud.yaml
│   └── 17_purchase_with_supplier.yaml
└── permissions/             # Tests de permissions (26 tests)
    ├── README.md            # Documentation des tests de permissions
    ├── android/
    │   └── visibility/      # Tests de visibilité Android (13 tests)
    │       ├── 01_no_permission.yaml
    │       ├── 02_sites_only.yaml
    │       ├── 03_products_only.yaml
    │       ├── 04_categories_only.yaml
    │       ├── 05_customers_only.yaml
    │       ├── 06_packaging_only.yaml
    │       ├── 07_stock_only.yaml
    │       ├── 08_purchases_only.yaml
    │       ├── 09_sales_only.yaml
    │       ├── 10_transfers_only.yaml
    │       ├── 11_inventory_only.yaml
    │       ├── 12_users_only.yaml
    │       └── 13_audit_only.yaml
    └── ios/
        └── visibility/      # Tests de visibilité iOS (13 tests)
            ├── 01_no_permission.yaml
            ├── 02_sites_only.yaml
            ├── 03_products_only.yaml
            ├── 04_categories_only.yaml
            ├── 05_customers_only.yaml
            ├── 06_packaging_only.yaml
            ├── 07_stock_only.yaml
            ├── 08_purchases_only.yaml
            ├── 09_sales_only.yaml
            ├── 10_transfers_only.yaml
            ├── 11_inventory_only.yaml
            ├── 12_users_only.yaml
            └── 13_audit_only.yaml
```

## Prérequis

### Android
1. Un émulateur Android doit être démarré ou un appareil connecté
2. L'application doit être installée :
   ```bash
   ./gradlew :app:installDebug
   ```

### iOS
1. Un simulateur iOS doit être démarré
2. L'application doit être installée :
   ```bash
   cd iosApp
   xcodebuild -workspace iosApp.xcworkspace -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15' build
   ```

## Exécution des Tests

### Tous les tests
```bash
cd /path/to/medistock-app
maestro test .maestro/
```

### Tests Android uniquement
```bash
maestro test .maestro/android/
```

### Tests iOS uniquement
```bash
maestro test .maestro/ios/
```

### Un test spécifique
```bash
maestro test .maestro/android/01_authentication.yaml
```

### Mode interactif (développement)
```bash
maestro studio
```

## Variables d'environnement

Les tests utilisent des variables qui peuvent être personnalisées :

| Variable | Valeur par défaut | Description |
|----------|-------------------|-------------|
| TEST_USERNAME | admin | Nom d'utilisateur pour les tests |
| TEST_PASSWORD | admin | Mot de passe pour les tests |
| TEST_SITE | Site Test | Nom du site de test |
| TEST_PRODUCT | Produit Test | Nom du produit de test |

Pour personnaliser :
```bash
maestro test -e TEST_USERNAME=myuser -e TEST_PASSWORD=mypass .maestro/
```

## Rapports et Screenshots

Les screenshots sont sauvegardés dans le dossier courant avec le préfixe spécifié dans chaque test.

Pour générer un rapport HTML :
```bash
maestro test .maestro/ --format junit --output report.xml
```

## Tests Inclus

### Tests Fonctionnels (34 tests - 17 Android + 17 iOS)

#### 1. Authentication (01_authentication.yaml)
- Connexion avec identifiants valides
- Déconnexion
- Connexion avec identifiants invalides

#### 2. Sites CRUD (02_sites_crud.yaml)
- Création d'un site
- Modification d'un site
- Suppression d'un site

#### 3. Products CRUD (03_products_crud.yaml)
- Création d'un produit
- Modification d'un produit
- Suppression d'un produit

#### 4. Categories CRUD (04_categories_crud.yaml)
- Création d'une catégorie
- Modification d'une catégorie
- Suppression d'une catégorie

#### 5. Customers CRUD (05_customers_crud.yaml)
- Création d'un client
- Modification d'un client
- Suppression d'un client

#### 6. Packaging Types CRUD (06_packaging_types_crud.yaml)
- Création d'un type d'emballage
- Modification d'un type d'emballage
- Suppression d'un type d'emballage

#### 7. Users CRUD (07_users_crud.yaml)
- Création d'un utilisateur
- Modification d'un utilisateur
- Suppression d'un utilisateur

#### 8. Purchases (08_purchases.yaml)
- Navigation vers l'écran d'achats
- Vérification d'accès à l'interface

#### 9. Sales (09_sales.yaml)
- Navigation vers l'écran de ventes
- Création d'une nouvelle vente avec saisie du nom du client
- Ajout de produits à la vente avec support multi-niveaux d'emballage
- Sélection du niveau d'emballage (Level 1 / Level 2) pour les produits à 2 niveaux
- Vérification de l'affichage du prix d'achat
- Vérification de l'affichage des informations de marge
- Vérification du prix de vente pré-calculé (modifiable)
- Saisie de la quantité et modification du prix de vente
- Ajout de plusieurs articles avec différents niveaux d'emballage
- Vérification de l'affichage des informations de stock (niveau 1 + équivalent niveau 2)

#### 10. Transfers (10_transfers.yaml)
- Navigation vers l'écran de transferts
- Vérification d'accès à l'interface

#### 11. Inventory (11_inventory.yaml)
- Navigation vers l'écran d'inventaire
- Vérification d'accès à l'interface

#### 12. Language Switching (12_language_switching.yaml)
- Navigation vers les paramètres de langue
- Vérification de toutes les langues disponibles (EN, FR, DE, ES, IT, RU, BM, NY)
- Changement de langue vers français
- Changement de langue vers allemand
- Changement de langue vers espagnol
- Retour à l'anglais
- Test de persistance après redémarrage de l'application

#### 13. Password Complexity (13_password_complexity.yaml)
- Validation des règles de complexité des mots de passe
- Tentatives avec mots de passe faibles
- Validation avec mots de passe conformes

#### 14. Notification Center (14_notification_center.yaml)
- Navigation vers le centre de notifications
- Vérification de l'accès aux notifications

#### 15. Online-First Authentication (15_online_first_auth.yaml)
- **First-time login (online required)**:
  - First login without network (should show "requires internet connection" error)
  - First login with valid credentials (online) - triggers full sync
  - First login with invalid credentials (online)
- **Subsequent login (offline capable)**:
  - Returning user can login offline (uses local BCrypt authentication)
  - Returning user can login online (uses Supabase Auth with fallback)
  - Invalid password shows error
- **Error scenarios**:
  - User not found error message
  - Account disabled error message
  - Network error handling
- **Authentication flow**:
  - Tests the online-first authentication requiring internet for first login
  - Tests offline authentication capability for subsequent logins
  - Validates proper error messages for all failure scenarios

#### 16. Suppliers CRUD (16_suppliers_crud.yaml)
- Navigation vers Manage Suppliers depuis Administration
- Vérification de l'état initial (liste vide ou avec données)
- Création d'un nouveau fournisseur avec tous les champs:
  - Nom (requis)
  - Téléphone
  - Email
  - Adresse
  - Notes
- Recherche de fournisseurs par nom
- Modification d'un fournisseur existant
- Vérification des données mises à jour
- Suppression d'un fournisseur
- Navigation de retour

#### 17. Purchase with Supplier (17_purchase_with_supplier.yaml)
- Création d'un fournisseur de test
- Navigation vers Purchase Products
- Vérification de la présence du sélecteur de fournisseur (spinner/picker)
- Sélection d'un fournisseur depuis le dropdown
- Sélection d'un site
- Sélection d'un produit
- Saisie de la quantité et du prix
- Vérification que le fournisseur reste sélectionné
- Nettoyage: suppression du fournisseur de test

### Tests de Permissions (26 tests - 13 Android + 13 iOS)

Les tests de permissions valident que le système de permissions granulaires fonctionne correctement en vérifiant que chaque utilisateur ne peut voir et accéder qu'aux modules pour lesquels il a les permissions.

#### Modules testés

**Opérations (Home Screen):**
- STOCK - Visualisation des stocks
- PURCHASES - Gestion des achats
- SALES - Gestion des ventes
- TRANSFERS - Transferts inter-sites
- INVENTORY - Comptage d'inventaire

**Administration (Admin Menu):**
- SITES - Gestion des sites
- PRODUCTS - Gestion des produits
- CATEGORIES - Gestion des catégories
- PACKAGING_TYPES - Types d'emballage
- CUSTOMERS - Gestion des clients
- USERS - Gestion des utilisateurs
- AUDIT - Historique d'audit

#### Tests de visibilité (13 tests par plateforme)

1. **01_no_permission.yaml** - Aucune permission: vérification qu'aucun module n'est visible
2. **02_sites_only.yaml** - Permission SITES uniquement
3. **03_products_only.yaml** - Permission PRODUCTS uniquement
4. **04_categories_only.yaml** - Permission CATEGORIES uniquement
5. **05_customers_only.yaml** - Permission CUSTOMERS uniquement
6. **06_packaging_only.yaml** - Permission PACKAGING_TYPES uniquement
7. **07_stock_only.yaml** - Permission STOCK uniquement
8. **08_purchases_only.yaml** - Permission PURCHASES uniquement
9. **09_sales_only.yaml** - Permission SALES uniquement
10. **10_transfers_only.yaml** - Permission TRANSFERS uniquement
11. **11_inventory_only.yaml** - Permission INVENTORY uniquement
12. **12_users_only.yaml** - Permission USERS uniquement
13. **13_audit_only.yaml** - Permission AUDIT uniquement

#### Utilisateurs de test

Tous les utilisateurs de test sont automatiquement créés en mode debug avec le mot de passe: `Test123!`

| Username | Permission | Visibilité attendue |
|----------|-----------|---------------------|
| test_no_permission | Aucune | Aucun module visible |
| test_sites_only | SITES | Seul "Site Management" visible dans Admin |
| test_products_only | PRODUCTS | Seul "Manage Products" visible dans Admin |
| test_categories_only | CATEGORIES | Seul "Manage Products" visible dans Admin |
| test_customers_only | CUSTOMERS | Seul "Manage Customers" visible dans Admin |
| test_packaging_only | PACKAGING_TYPES | Seul "Packaging Types" visible dans Admin |
| test_stock_only | STOCK | Seul "View Stock" visible sur l'écran d'accueil |
| test_purchases_only | PURCHASES | Seul "Purchase Products" visible sur l'écran d'accueil |
| test_sales_only | SALES | Seul "Sell Products" visible sur l'écran d'accueil |
| test_transfers_only | TRANSFERS | Seul "Transfer Products" visible sur l'écran d'accueil |
| test_inventory_only | INVENTORY | Seul "Inventory Stock" visible sur l'écran d'accueil |
| test_users_only | USERS | Seul "User Management" visible dans Admin |
| test_audit_only | AUDIT | Seul "Audit History" visible dans Admin |

#### Exécution des tests de permissions

```bash
# Tous les tests de permissions Android (13 tests)
maestro test .maestro/permissions/android/visibility/

# Tous les tests de permissions iOS (13 tests)
maestro -p ios test .maestro/permissions/ios/visibility/

# Un test spécifique
maestro test .maestro/permissions/android/visibility/01_no_permission.yaml
maestro -p ios test .maestro/permissions/ios/visibility/07_stock_only.yaml
```

Pour plus de détails sur les tests de permissions, consultez [permissions/README.md](permissions/README.md).

## Correspondance avec le Cahier de Recette

| Section du Cahier | Test Maestro |
|-------------------|--------------|
| 2. Authentification | 01_authentication.yaml |
| 3.1 Gestion des sites | 02_sites_crud.yaml |
| 4. Gestion des achats | 03_purchase_sale_workflow.yaml |
| 5. Gestion des ventes | 03_purchase_sale_workflow.yaml |
| 6. Logique FIFO | 03_purchase_sale_workflow.yaml |
| 8. Transferts inter-sites | 04_transfers.yaml |
| 9. Inventaire | 05_inventory.yaml |

## Phase 11 - Intégrité Référentielle & Soft Delete

### Statut de l'implémentation

La Phase 11 ajoute le soft delete (`is_active`) pour Sites, Categories, Products, et Customers. Le schéma et le service `ReferentialIntegrityService` sont implémentés, mais **l'UI n'est pas encore modifiée**.

### Impact sur les tests E2E existants

✅ **Les tests existants continuent de fonctionner** sans modification car :
- Les tests créent et suppriment des entités propres (non utilisées)
- Le champ `is_active` a une valeur par défaut de 1 (actif)
- L'UI n'appelle pas encore le `ReferentialIntegrityService`

### Tests futurs à ajouter (quand l'UI sera implémentée)

Lorsque l'UI de désactivation sera implémentée, il faudra ajouter :

1. **Tests de désactivation** (`12_deactivation_*.yaml`) :
   - Tenter de supprimer une entité utilisée
   - Vérifier le dialogue de désactivation
   - Confirmer la désactivation
   - Vérifier l'indicateur "Inactive"

2. **Tests de filtrage** (`16_inactive_filters.yaml`) :
   - Basculer l'affichage des entités inactives
   - Vérifier que les dropdowns excluent les entités inactives
   - Vérifier que l'historique affiche les entités inactives

3. **Tests de réactivation** (`17_reactivation.yaml`) :
   - Réactiver une entité désactivée
   - Vérifier qu'elle réapparaît dans les dropdowns

📋 **Documentation détaillée** : Voir `PHASE11_E2E_ANALYSIS.md` pour l'analyse complète, les scénarios de test détaillés, et les user journeys.

## Dépannage

### L'application ne se lance pas
Vérifiez que l'application est bien installée :
```bash
# Android
adb shell pm list packages | grep medistock

# iOS
xcrun simctl listapps booted | grep medistock
```

### Les éléments ne sont pas trouvés
Utilisez le mode studio pour inspecter l'UI :
```bash
maestro studio
```

### Timeout sur les animations
Augmentez le timeout dans les tests :
```yaml
- waitForAnimationToEnd:
    timeout: 15000
```

## CI/CD Integration

Pour intégrer dans GitHub Actions, ajoutez à votre workflow :

```yaml
- name: Install Maestro
  run: |
    curl -Ls "https://get.maestro.mobile.dev" | bash
    echo "$HOME/.maestro/bin" >> $GITHUB_PATH

- name: Run E2E Tests
  run: maestro test .maestro/android/
```
