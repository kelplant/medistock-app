# Système de Conditionnement Administrable

## Vue d'ensemble

Le système de conditionnement a été refactoré pour être entièrement administrable. Au lieu d'avoir des unités fixes hard-codées, les types de conditionnement sont maintenant gérés dans la base de données et configurables via l'interface d'administration.

## Concepts

### PackagingType (Type de Conditionnement)

Un `PackagingType` définit un type de conditionnement qui peut avoir **1 ou 2 niveaux** :

#### Exemples à 2 niveaux :
- **Boîte/Comprimés** :
  - Niveau 1: Boîte
  - Niveau 2: Comprimés
  - Facteur de conversion: 30 (30 comprimés par boîte)

- **Flacon/ml** :
  - Niveau 1: Flacon
  - Niveau 2: ml
  - Facteur de conversion: 100 (100 ml par flacon)

#### Exemples à 1 niveau :
- **Units** :
  - Niveau 1: Units
  - Pas de niveau 2

### Product et Conditionnement

Pour chaque produit, vous pouvez maintenant :
1. Choisir un **type de conditionnement** (ex: Boîte/Comprimés)
2. Choisir le **niveau d'unité** à utiliser (Niveau 1 ou Niveau 2)
3. Définir un **facteur de conversion personnalisé** (optionnel, par défaut utilise celui du type)

#### Exemple :
Un médicament peut être configuré avec :
- Type: "Boîte/Comprimés"
- Unité utilisée: Niveau 2 (Comprimés)
- Facteur de conversion: 30 (30 comprimés par boîte)

## Migration depuis l'ancien système

### Ancien système
- Unités hard-codées: "Bottle", "Box", "Tablet", "ml", "Units"
- Champ `unit` (String)
- Champ `unitVolume` (Double)

### Nouveau système
- Types de conditionnement administrables en base de données
- Champs `packagingTypeId`, `selectedLevel`, `conversionFactor`
- Compatibilité arrière: les anciens champs `unit` et `unitVolume` sont conservés mais marqués comme nullable

## Utilisation

### 1. Créer des types de conditionnement

Aller dans **Administration > Packaging Types** pour créer les types de conditionnement :

1. Cliquer sur "Ajouter un type de conditionnement"
2. Renseigner :
   - Nom du type (ex: "Boîte/Comprimés")
   - Nom du niveau 1 (ex: "Boîte")
   - Cocher "Ce type a 2 niveaux" si nécessaire
   - Si 2 niveaux :
     - Nom du niveau 2 (ex: "Comprimés")
     - Facteur de conversion par défaut (ex: 30)

### Types de conditionnement recommandés à créer :

```
1. Boîte/Comprimés
   - Niveau 1: Boîte
   - Niveau 2: Comprimés
   - Facteur: 30

2. Flacon/ml
   - Niveau 1: Flacon
   - Niveau 2: ml
   - Facteur: 100

3. Sachet/Unités
   - Niveau 1: Sachet
   - Niveau 2: Unités
   - Facteur: 10

4. Units (niveau unique)
   - Niveau 1: Units
   - Pas de niveau 2
```

### 2. Créer/Modifier un produit

Lors de la création ou modification d'un produit :

1. Sélectionner le **Type de conditionnement**
2. Sélectionner l'**Unité utilisée** (Niveau 1 ou Niveau 2)
3. Si le type a 2 niveaux, le **Facteur de conversion** apparaît automatiquement
4. Vous pouvez modifier le facteur de conversion pour ce produit spécifique si nécessaire

### 3. Les ventes et le stock

Le système s'adapte automatiquement :
- Les quantités sont affichées avec l'unité sélectionnée
- Les conversions sont gérées automatiquement selon le facteur de conversion
- Tous les calculs de stock utilisent l'unité sélectionnée du produit

## Structure de la base de données

### Table `packaging_types`

```sql
CREATE TABLE packaging_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    level1Name TEXT NOT NULL,
    level2Name TEXT,
    defaultConversionFactor REAL,
    isActive INTEGER DEFAULT 1,
    displayOrder INTEGER DEFAULT 0,
    createdAt TEXT,
    updatedAt TEXT,
    createdBy TEXT,
    updatedBy TEXT
);
```

### Table `products` (nouveaux champs)

```sql
-- Nouveaux champs
packagingTypeId INTEGER, -- FK vers packaging_types
selectedLevel INTEGER, -- 1 ou 2
conversionFactor REAL, -- Facteur spécifique au produit (peut override celui du type)

-- Anciens champs (deprecated mais conservés pour compatibilité)
unit TEXT,
unitVolume REAL
```

## API

### PackagingTypeDao

```kotlin
- getAllActive(): Flow<List<PackagingType>> // Types actifs uniquement
- getAll(): Flow<List<PackagingType>> // Tous les types
- getById(id: Long): Flow<PackagingType?>
- insert(packagingType: PackagingType): Long
- update(packagingType: PackagingType)
- delete(packagingType: PackagingType)
- deactivate(id: Long) // Désactiver sans supprimer
- activate(id: Long)
- isUsedByProducts(packagingTypeId: Long): Boolean
```

### Product (nouvelles méthodes)

```kotlin
// Vérifie si le produit utilise le nouveau système
fun usesNewPackagingSystem(): Boolean

// Retourne l'unité effective (nouveau ou ancien système)
fun getEffectiveUnit(packagingType: PackagingType?): String

// Retourne le facteur de conversion effectif
fun getEffectiveConversionFactor(packagingType: PackagingType?): Double
```

## Écrans d'administration

### PackagingTypeListActivity
- Liste tous les types de conditionnement
- Permet d'ajouter, modifier, activer/désactiver, supprimer
- Accessible via Administration > Packaging Types

### PackagingTypeAddEditActivity
- Formulaire de création/modification d'un type de conditionnement
- Gestion dynamique des champs selon le nombre de niveaux

## Compatibilité

Le système est rétrocompatible :
- Les produits existants avec `unit` et `unitVolume` continuent de fonctionner
- Les nouveaux produits utilisent `packagingTypeId`, `selectedLevel`, et `conversionFactor`
- Les méthodes `getEffectiveUnit()` et `getEffectiveConversionFactor()` gèrent les deux systèmes

## Prochaines étapes

Pour une migration complète, il faudrait :
1. Créer un script de migration pour convertir les anciens produits vers le nouveau système
2. Adapter tous les écrans de vente et de stock pour utiliser le nouveau système
3. Adapter les rapports et statistiques
4. Supprimer les anciens champs `unit` et `unitVolume` une fois la migration terminée
