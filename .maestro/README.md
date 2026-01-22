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
│   └── login.yaml           # Flow de connexion
├── android/                 # Tests Android
│   ├── 01_authentication.yaml
│   ├── 02_sites_crud.yaml
│   └── 03_purchase_sale_workflow.yaml
└── ios/                     # Tests iOS
    ├── 01_authentication.yaml
    ├── 02_sites_crud.yaml
    └── 03_purchase_sale_workflow.yaml
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

### 1. Authentication (01_authentication.yaml)
- Connexion avec identifiants valides
- Déconnexion
- Connexion avec identifiants invalides

### 2. Sites CRUD (02_sites_crud.yaml)
- Création d'un site
- Modification d'un site
- Suppression d'un site

### 3. Purchase/Sale Workflow (03_purchase_sale_workflow.yaml)
- Création d'un achat (ajout de stock)
- Vérification du stock après achat
- Création d'une vente (FIFO)
- Vérification du stock après vente
- Vérification du calcul des bénéfices

### 4. Transfers (04_transfers.yaml)
- Création de sites source et destination
- Création d'un transfert inter-sites
- Vérification du stock après transfert

### 5. Inventory (05_inventory.yaml)
- Démarrage d'une session d'inventaire
- Comptage avec écart (discrepancy)
- Validation et ajustement du stock
- Vérification de l'audit

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
