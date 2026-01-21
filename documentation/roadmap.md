Roadmap technique — Parité Android/iOS et consolidation `shared`

> Objectif : rendre l’application iOS conforme à Android **sans duplication de logique métier**, en centralisant tout ce qui doit l’être dans `shared`. Cette roadmap compile l’ensemble des écarts, faiblesses et recommandations identifiés.

## Principes métier (non négociables)

1. **Stock négatif autorisé** : une vente ne doit jamais être bloquée par un stock insuffisant. Un avertissement non bloquant est acceptable.
2. **Source de vérité unique** : règles métier et validations doivent vivre dans `shared`.
3. **Parité fonctionnelle** : Android et iOS doivent avoir les mêmes flux métier, mêmes règles de permissions et même comportement de sync.

---

## Phase 0 — Cadrage & spécification (1 semaine)

### Objectifs
- Aligner les règles métier et les conventions de synchronisation.
- Définir un socle partagé pour modules, permissions, compatibilité et authentification.

### Tâches
- Formaliser les **règles métier** (achats, ventes, transferts, inventaires, stock négatif).
- Définir la **stratégie de sync** (bidirectionnelle, offline-first, résolution de conflits).
- Établir une **liste unique de modules** (permissions) commune Android/iOS.
- Définir la **politique “app too old”** et le comportement de mise à jour minimum requis.

### Livrables
- Document “Business Rules & Sync Policy”.
- Enum des modules unique (spécification).

---

## Phase 1 — Centraliser la logique métier dans `shared` (3–5 semaines)

> But : déplacer les workflows transactionnels dans `shared` (achats/ventes/transferts/stock/produits/sites) et ne laisser que la présentation/UX aux apps.

### 1.1. Création d’une couche **UseCases** dans `shared`

**Nouveaux modules proposés :**
- `PurchaseUseCase`
- `SaleUseCase`
- `TransferUseCase`
- `InventoryUseCase`
- `StockMovementUseCase`
- `ProductUseCase`
- `SiteUseCase`
- `UserUseCase` (auth/permissions côté métier)

**Contraintes :**
- Inputs normalisés (DTO commun).
- Outputs = entités + warnings métier (ex. stock négatif).
- Toutes les mutations (insert/update/delete) passent par ces UseCases.

### 1.2. Workflow **Achats**

**Doit encapsuler :**
- Création `PurchaseBatch`.
- Augmentation de stock.
- Création `StockMovement` type `PURCHASE`.
- Écriture d’audit (si applicable).

### 1.3. Workflow **Ventes**

**Doit encapsuler :**
- Création `Sale` + `SaleItem`.
- Décrément stock **même si insuffisant** (stock négatif autorisé).
- Création `StockMovement` type `SALE`.
- Allocation de lots (si stratégie existante).
- Écriture d’audit.

### 1.4. Workflow **Transferts**

**Doit encapsuler :**
- Validation sites source/destination.
- Décrément stock site A + incrément site B.
- Double `StockMovement` (OUT/IN).
- Écriture d’audit.

### 1.5. Workflow **Inventaires**

**Doit encapsuler :**
- Création inventaire.
- Ajustement stock si besoin.
- StockMovement type `INVENTORY`.
- Audit.

### 1.6. **Produits / Sites / Clients**

**Doit encapsuler :**
- Validation des champs obligatoires (ex. `siteId` obligatoire).
- Normalisation des valeurs par défaut (unités, timestamps, etc.).

### 1.7. **Audit partagé**

- Toute action métier (UseCase) doit générer une entrée audit.
- Les triggers Room Android sont conservés, mais `shared` doit être la source principale des écritures audit métier.

### Livrables
- Ensemble des UseCases partagés.
- Tests unitaires de règles métier.
- Migration Android/iOS : tous les écrans utilisent les UseCases.

---

## Phase 2 — Auth & Permissions unifiées (2–3 semaines)

### 2.1. Authentification partagée
- Déplacer la logique d’auth dans `shared`.
- Uniformiser les règles (Supabase si configuré, sinon local).
- Éviter les divergences Android/iOS.

### 2.2. Modules permissions partagés
- Créer un enum de modules unique dans `shared`.
- Android et iOS se branchent dessus.

### 2.3. Permissions offline-first
- Définir une stratégie commune (cache local + refresh distant).

### Livrables
- Auth partagée.
- Modules unifiés.
- Permissions cohérentes dans les deux apps.

---

## Phase 3 — Synchronisation & Offline parity (4–6 semaines)

### 3.1. Sync bidirectionnelle iOS
- Reproduire `SyncManager` Android côté iOS (push local + pull remote).
- Respecter l’ordre d’import/export des entités.

### 3.2. Queue offline iOS
- Implémenter une file d’opérations (insert/update/delete) alignée sur Android.
- Optimisation de queue (fusion insert/update, suppression obsolète).

### 3.3. Realtime cohérent
- Filtrage `client_id` côté iOS.
- Résolution de conflits explicite (server wins).

### 3.4. Scheduler unifié
- Android : valider orchestration WorkManager.
- iOS : trigger sur app resume + polling si besoin.

### Livrables
- Sync bidirectionnelle iOS.
- Queue offline iOS.
- Règles realtime cohérentes.

---

## Phase 4 — UX / Écrans manquants (2–3 semaines)

### 4.1. Mouvements de stock iOS
- Ajouter un écran de création (in/out) aligné Android.
- **Ne jamais bloquer** si stock insuffisant.
- Avertissement non bloquant possible.

### 4.2. Update flow iOS
- Ajouter un écran “version obsolète” similaire au flow Android.
- Définir politique de distribution iOS (App Store / MDM / etc.).

### Livrables
- UI iOS alignée.
- Parité fonctionnelle complète.

---

## Phase 5 — Durcissement Android (1–2 semaines)

### 5.1. Auth Android alignée shared
- Remplacer la logique locale pure par celle de `shared`.

### 5.2. Résolution de conflits explicite
- Centraliser la policy de résolution et l’exposer dans `shared`.

### 5.3. Audit & sync
- Garantir que toutes les opérations syncées génèrent un audit.

### Livrables
- Android conforme aux mêmes règles que iOS.
- Cohérence audit & sync.

---

## Critères de sortie globaux

- ✅ Toutes les opérations métier passent par `shared`.
- ✅ Sync bidirectionnelle et offline-first sur les deux plateformes.
- ✅ Auth / permissions identiques Android et iOS.
- ✅ Règle “stock négatif autorisé” appliquée partout.
- ✅ Parité UI complète (écrans principaux).