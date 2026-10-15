# Conception : calcul et synchronisation du stock

Statut : proposition, à valider avant implémentation.
Portée : Android, iOS, code partagé KMP, Supabase.

---

## 1. Constat

### 1.1 Comment le stock fonctionne aujourd'hui

| Élément | Où | Rôle actuel |
|---|---|---|
| `current_stock` (SQLite) | local uniquement | Total affiché par l'écran Stock. Mis à jour par `StockRepository.updateStockDelta`. Jamais envoyé ni reçu. |
| `stock_movements` | local + Supabase | Historique des entrées/sorties. Envoyé à Supabase. |
| `purchase_batches.remaining_quantity` | local + Supabase | Quantité restante de chaque lot (FIFO). Envoyée **en valeur absolue**. |
| `sale_batch_allocations` | local | Lien vente → lots consommés. |
| `current_stock` (Supabase) | Supabase | Table orpheline : aucun trigger ne l'alimente, dernière mise à jour le 25/01/2026. |
| `v_stock_current` (Supabase) | Supabase | Vue de reporting : somme des `remaining_quantity` des lots. |

### 1.2 Problèmes

1. **Des écrans contournent la logique partagée.** Ils écrivent un mouvement sans toucher aux lots ni à `current_stock` :
   - Android : achat (`PurchaseActivity`), mouvement manuel (`StockMovementActivity`), inventaire (`InventoryActivity`), suppression de vente (`SaleListActivity`) ;
   - iOS : mouvement manuel (`StockViews.swift`), validation de transfert (`TransfersViews.swift`).
2. **Le stock d'un terminal ignore les autres terminaux.** Les lots et mouvements reçus par la synchro ne recalculent pas `current_stock`.
3. **Les quantités des lots s'écrasent entre terminaux.** Deux terminaux qui vendent sur le même lot envoient chacun une valeur absolue : la dernière écriture gagne, une vente disparaît du stock.
4. **La synchro complète renvoie toute la base toutes les 30 s** (`SyncManager.syncLocalToRemote`). Un terminal en retard écrase donc les bonnes valeurs du serveur avec ses anciennes.
5. **Les mouvements ne remontent quasiment pas** : Supabase ne contient qu'un seul mouvement. Les stocks actuels viennent des données de démonstration (lots), sans historique de mouvements.
6. **Deux conventions de signe coexistent.** `BUSINESS_RULES.md` décrit `type = in/out` avec quantité positive ; le code partagé utilise des types métier (`SALE`, `PURCHASE`…) avec une quantité signée.

---

## 2. Principes retenus

1. **Les mouvements de stock sont la seule source de vérité.**
   Stock d'un produit sur un site = `SUM(quantity)` de ses mouvements.
2. **Les mouvements sont en ajout seul.** On ne modifie ni ne supprime jamais un mouvement. Une annulation crée un mouvement inverse.
   Conséquence : deux terminaux ne peuvent jamais être en conflit sur un mouvement (identifiants UUID distincts).
3. **Quantité signée, en unité de base.** Entrée > 0, sortie < 0. Le champ `type` décrit l'origine métier, pas le sens.
4. **Stock négatif autorisé** (règle n° 1 de `BUSINESS_RULES.md`, inchangée).
5. **Tout ce qui est calculé est recalculable.** `current_stock` (local et serveur) et les quantités restantes des lots sont des caches dérivés des mouvements et des consommations de lots. On peut les reconstruire à tout moment.
6. **Une seule implémentation.** Toute opération de stock passe par un cas d'usage du module partagé. Les écrans Android et iOS n'écrivent jamais directement dans `stock_movements`, `purchase_batches` ou `current_stock`.

---

## 3. Règles par opération

| Opération | Mouvement(s) créé(s) | Effet sur les lots | Cas d'usage |
|---|---|---|---|
| Achat | `PURCHASE`, +q | Crée un lot (`initial_quantity = q`) | `PurchaseUseCase` |
| Vente | `SALE`, −q par article | Consommations FIFO sur les lots du site | `SaleUseCase` |
| Annulation / suppression de vente | `SALE_REVERSAL`, +q | Consommations inverses (−q) sur les mêmes lots | **nouveau** `SaleReversalUseCase` |
| Modification de vente | annulation + nouvelle vente | idem | `SaleUseCase` + `SaleReversalUseCase` |
| Transfert | `TRANSFER_OUT` −q (site A), `TRANSFER_IN` +q (site B) | Consommations FIFO sur A ; lot créé sur B au prix de revient moyen consommé | `TransferUseCase` |
| Correction manuelle entrée | `MANUAL_IN`, +q | Crée un lot d'ajustement (prix = dernier prix d'achat connu, 0 sinon) | **nouveau** `StockAdjustmentUseCase` |
| Correction manuelle sortie | `MANUAL_OUT`, −q | Consommations FIFO | `StockAdjustmentUseCase` |
| Inventaire | `INVENTORY`, ± écart | Écart > 0 : lot d'ajustement. Écart < 0 : consommations FIFO | `InventoryUseCase` (s'appuie sur `StockAdjustmentUseCase`) |

Règles communes :
- Chaque mouvement porte `reference_id` (id de la vente, du transfert, de l'inventaire…) pour la traçabilité.
- Une opération et toutes ses écritures (mouvements, lots, consommations, cache) se font dans **une seule transaction SQLite**.

---

## 4. Modèle de données

### 4.1 Mouvements (`stock_movements`, local et Supabase)

Inchangé dans sa structure. Évolutions :
- `quantity` signée, unité de base (déjà le cas dans les cas d'usage).
- `type` parmi : `PURCHASE`, `SALE`, `SALE_REVERSAL`, `TRANSFER_IN`, `TRANSFER_OUT`, `MANUAL_IN`, `MANUAL_OUT`, `INVENTORY`, `OPENING_BALANCE`.
- Supabase : contrainte `CHECK` sur `type`, et contrainte de cohérence de signe (`PURCHASE`/`*_IN`/`SALE_REVERSAL` > 0, `SALE`/`*_OUT` < 0, `INVENTORY` et `OPENING_BALANCE` libres).
- Supabase : RLS en insertion seule pour le rôle `authenticated` (pas d'`UPDATE` ni de `DELETE`).

Les anciennes lignes `in`/`out` sont converties par la migration (§ 7).

### 4.2 Consommations de lots (nouvelle table `batch_consumptions`, local et Supabase)

Généralise `sale_batch_allocations` à toutes les sorties.

```sql
CREATE TABLE batch_consumptions (
    id TEXT PRIMARY KEY,
    batch_id TEXT NOT NULL REFERENCES purchase_batches(id),
    movement_id TEXT NOT NULL REFERENCES stock_movements(id),
    quantity REAL NOT NULL,          -- > 0 consommation, < 0 annulation
    unit_cost REAL NOT NULL,         -- prix de revient du lot au moment de la consommation
    created_at INTEGER NOT NULL,
    created_by TEXT NOT NULL
);
```

Également en ajout seul. `sale_batch_allocations` est conservée en lecture pour l'historique, puis migrée (§ 7).

### 4.3 Lots (`purchase_batches`)

- Seuls les champs immuables sont synchronisés : produit, site, date, `initial_quantity`, prix, fournisseur, péremption.
- `remaining_quantity` et `is_exhausted` deviennent des **caches locaux** :
  `remaining_quantity = initial_quantity − SUM(batch_consumptions.quantity)`.
- Ils ne sont plus envoyés à Supabase. Côté serveur, ils sont calculés (§ 4.5).
- Si deux terminaux consomment le même lot hors ligne, le lot peut devenir négatif. C'est accepté (cohérent avec le stock négatif autorisé) et visible dans le reporting.

### 4.4 Cache local `current_stock` (SQLite)

- Structure inchangée.
- Mis à jour dans la transaction de chaque cas d'usage (comme aujourd'hui avec `updateStockDelta`).
- Recalculé pour les couples (produit, site) touchés, après chaque réception de mouvements par la synchro :
  ```sql
  INSERT OR REPLACE INTO current_stock(id, product_id, site_id, quantity, last_movement_id, last_updated_at)
  SELECT ..., product_id, site_id, SUM(quantity), ...
  FROM stock_movements WHERE product_id = ? AND site_id = ?
  GROUP BY product_id, site_id;
  ```
- Reconstruction complète disponible (`StockRepository.rebuildAll()`) : au premier lancement après mise à jour, et depuis l'écran Supabase.

### 4.5 Supabase : stock calculé en base

- `current_stock` est alimentée par un trigger `AFTER INSERT ON stock_movements` (`quantity = quantity + NEW.quantity`, ligne créée au besoin). C'est le stock « calculé en direct dans la base ».
- Les écritures directes des clients sur `current_stock` sont interdites (RLS lecture seule).
- `remaining_quantity` des lots est calculé par une vue `v_batch_remaining` (`initial_quantity − SUM(batch_consumptions)`).
- `v_stock_current` et les autres vues de reporting sont réécrites sur `current_stock` / `stock_movements` au lieu des lots.
- Une fonction `rebuild_current_stock()` permet de reconstruire la table depuis les mouvements (contrôle, réparation).

---

## 5. Synchronisation

| Donnée | Envoi | Réception | Conflit possible |
|---|---|---|---|
| `stock_movements` | INSERT via la file, jamais d'UPDATE | Incrémentale (`created_at > dernier reçu`) | Non |
| `batch_consumptions` | INSERT via la file | Incrémentale | Non |
| `purchase_batches` | INSERT via la file (champs immuables) | Incrémentale | Non |
| `current_stock` | jamais | jamais (recalculé localement) | Non |

Changements dans le moteur de synchro :
1. **Arrêt du renvoi complet automatique.** `AutoSyncWorker` traite la file d'attente puis fait une réception incrémentale. `syncLocalToRemote` (tout renvoyer) ne reste disponible que via le bouton de l'écran Supabase, et n'envoie plus jamais `remaining_quantity`.
2. **Réception incrémentale** par table, avec un curseur `last_pulled_at` stocké localement. Après réception de mouvements, recalcul de `current_stock` pour les couples touchés.
3. **Ordre d'envoi** : lots → mouvements → consommations (clés étrangères).
4. Le même comportement est appliqué sur iOS (`BidirectionalSyncManager`, `SyncQueueProcessor`).

---

## 6. Corrections des écrans

| Écran | Plateforme | Correction |
|---|---|---|
| Achat | Android | `PurchaseActivity` → `PurchaseUseCase` |
| Mouvement manuel | Android, iOS | → `StockAdjustmentUseCase` |
| Inventaire | Android | `InventoryActivity` → `InventoryUseCase` |
| Suppression de vente | Android (iOS à vérifier) | → `SaleReversalUseCase` |
| Modification de vente | Android `SaleActivity` (édition) | → annulation + nouvelle vente |
| Validation de transfert | iOS `TransfersViews.completeTransfer` | → `TransferUseCase` |

Garde-fou : un test de code statique (lint/grep en CI) échoue si un fichier hors `shared/.../usecase` appelle `stockMovementRepository.insert`, `purchaseBatchRepository.updateQuantity` ou `stockRepository.updateStockDelta`.

---

## 7. Migration des données existantes

### 7.1 Serveur (migration SQL `2026101501_stock_from_movements.sql`)

1. Créer `batch_consumptions`, contraintes et RLS (§ 4).
2. Convertir les mouvements existants `in`/`out` en quantité signée et type métier.
3. Créer un mouvement `OPENING_BALANCE` par (produit, site) pour amener la somme des mouvements au stock réel actuel :
   `quantité = SUM(remaining_quantity des lots) − SUM(mouvements existants)`.
   Identifiant déterministe (`md5('opening:' || product_id || ':' || site_id)`) pour que la migration soit rejouable.
4. Créer une consommation d'ouverture par lot (`initial_quantity − remaining_quantity`) pour que les vues de lots retombent sur les valeurs actuelles.
5. Reconstruire `current_stock` depuis les mouvements, puis installer le trigger.
6. Monter `schema_version` et `min_app_version` : les anciennes versions de l'app (qui envoient `remaining_quantity` en absolu) sont bloquées par l'écran de mise à jour existant.

### 7.2 Terminaux (premier lancement de la nouvelle version)

1. Envoyer ce qui reste dans la file d'attente.
2. Vider les tables locales de stock (`stock_movements`, `batch_consumptions`, `current_stock`, `purchase_batches`).
3. Tout recevoir depuis Supabase, puis `StockRepository.rebuildAll()`.

Précaution : avant la migration serveur, chaque terminal en service doit avoir été synchronisé, sinon ses opérations locales non envoyées sont perdues à l'étape 2. La mise en production se fait donc à un moment sans activité, après une synchro manuelle de chaque terminal.

---

## 8. Tests

Code partagé (`shared/src/commonTest`), base SQLite en mémoire :
- un test par ligne du tableau § 3 : mouvements créés, lots, consommations, `current_stock` ;
- annulation de vente : stock et lots reviennent exactement à l'état initial ;
- deux « terminaux » simulés vendent hors ligne sur le même lot, puis échangent leurs données : le stock final est la somme exacte des deux ventes ;
- `rebuildAll()` redonne le même `current_stock` que les mises à jour incrémentales ;
- stock négatif accepté sans erreur.

Supabase : script de vérification `SELECT` comparant `current_stock` à `SUM(stock_movements)` (doit retourner 0 ligne).

---

## 9. Plan de livraison

| Étape | Contenu | Livrable |
|---|---|---|
| 1 | Cas d'usage partagés : `StockAdjustmentUseCase`, `SaleReversalUseCase`, `batch_consumptions`, `rebuildAll()`, tests | Branche, tests verts |
| 2 | Branchement de tous les écrans Android et iOS sur les cas d'usage | Branche, garde-fou CI |
| 3 | Synchro : arrêt du renvoi complet, réception incrémentale, recalcul après réception | Branche |
| 4 | Migration Supabase (§ 7.1), sur une branche Supabase de test d'abord | Migration validée |
| 5 | Mise en production : synchro manuelle des terminaux, migration, release avec `min_app_version` | Version 0.9.0 |

---

## 10. Questions ouvertes

1. **Prix d'un lot d'ajustement (entrée manuelle, surplus d'inventaire)** : dernier prix d'achat connu (proposé), prix moyen, ou 0 ?
2. **Modification d'une vente** : autorisée seulement le jour même, ou toujours ?
3. **Transferts « en attente »** : `TransferUseCase` crée les transferts directement à l'état `completed` (stock déplacé à la création). iOS garde un bouton de validation pour les transferts `pending`, qui ajoute une seconde paire de mouvements sans toucher aux lots. Faut-il un vrai workflow en deux temps (envoi puis réception), ou supprimer l'état `pending` ?
4. **Lots négatifs après conflit hors ligne** : simple signalement dans le reporting (proposé), ou rééquilibrage automatique vers le lot suivant côté serveur ?
5. **Historique** : faut-il conserver les données de démonstration (lots `seed`) ou repartir d'une base vide en production ?
