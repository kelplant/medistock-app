# MediStock - Brainstorming & Roadmap Future

> Document de réflexion sur les améliorations et nouvelles fonctionnalités potentielles pour MediStock.
> Dernière mise à jour : 23 Janvier 2026

---

## Résumé de l'Application Actuelle

### Type d'Application

**MediStock** est un système de gestion de stock pharmaceutique multi-plateforme (Android + iOS) conçu pour :
- Pharmacies et hôpitaux
- Réseaux multi-sites
- Environnements avec connectivité intermittente (offline-first)

### Architecture Technique

| Composant | Technologie |
|-----------|-------------|
| Architecture | Kotlin Multiplatform (KMP) |
| Base locale | SQLDelight |
| Base distante | Supabase (PostgreSQL) |
| UI Android | Activities + ViewBinding |
| UI iOS | SwiftUI |
| Sync | Bidirectionnelle + Realtime |

### Fonctionnalités Existantes

#### Gestion des Stocks (FIFO)
- Achats avec lots/batches et dates d'expiration
- Ventes avec allocation FIFO automatique
- Transferts inter-sites (préserve dates d'achat)
- Inventaires physiques avec ajustements
- Mouvements de stock complets

#### Administration
- 13 modules avec permissions CRUD granulaires
- Gestion utilisateurs avec BCrypt
- Audit trail complet
- Configuration Supabase

#### Multi-plateforme
- Offline-first avec queue de sync
- Realtime via Supabase
- 8 langues supportées (EN, FR, DE, ES, IT, RU, Bemba, Nyanja)

---

## Améliorations Proposées

### 1. UX/UI

| ID | Amélioration | Description | Effort | Impact |
|----|--------------|-------------|--------|--------|
| UX-01 | **Dashboard analytics** | Tableau de bord avec KPIs (ventes du jour, stock faible, expirations proches, graphiques tendances) | Moyen | Élevé |
| UX-02 | **Recherche globale** | Barre de recherche unifiée pour produits, clients, lots, fournisseurs | Faible | Moyen |
| UX-03 | **Mode sombre** | Support du dark mode natif sur iOS et Android | Moyen | Moyen |
| UX-04 | **Notifications push** | Alertes stock faible, expirations imminentes, sync échouée | Moyen | Élevé |
| UX-05 | **Onboarding** | Guide interactif pour nouveaux utilisateurs | Faible | Moyen |
| UX-06 | **Raccourcis clavier** | Navigation rapide pour utilisateurs desktop (tablettes) | Faible | Faible |
| UX-07 | **Thèmes personnalisables** | Couleurs de l'interface personnalisables par entreprise | Faible | Faible |

### 2. Performance & Technique

| ID | Amélioration | Description | Effort | Impact |
|----|--------------|-------------|--------|--------|
| PERF-01 | **Pagination listes** | Listes paginées pour gros volumes de données (>1000 items) | Moyen | Élevé |
| PERF-02 | **Cache images** | Mise en cache intelligente des images produits | Faible | Moyen |
| PERF-03 | **Compression sync** | Compression gzip pour réduire la bande passante | Faible | Moyen |
| PERF-04 | **Background sync iOS** | Sync en arrière-plan comme Android (BackgroundTasks) | Moyen | Élevé |
| PERF-05 | **Lazy loading** | Chargement différé des données non visibles | Moyen | Moyen |
| PERF-06 | **Database indexes** | Optimisation des index pour requêtes fréquentes | Faible | Moyen |

### 3. Sécurité

| ID | Amélioration | Description | Effort | Impact |
|----|--------------|-------------|--------|--------|
| SEC-01 | **2FA/MFA** | Authentification à deux facteurs (TOTP, SMS) | Élevé | Élevé |
| SEC-02 | **Session timeout** | Déconnexion automatique après X minutes d'inactivité | Faible | Moyen |
| SEC-03 | **Biométrie** | Login par empreinte digitale / Face ID | Moyen | Élevé |
| SEC-04 | **Logs connexion** | Historique des connexions avec IP, appareil, localisation | Faible | Moyen |
| SEC-05 | **Verrouillage compte** | Blocage après X tentatives échouées | Faible | Moyen |
| SEC-06 | **Chiffrement local** | Chiffrement de la base SQLite locale | Moyen | Élevé |
| SEC-07 | **Audit export** | Journaux d'audit exportables pour conformité | Faible | Moyen |

---

## Nouvelles Fonctionnalités

### Priorité Haute (Quick Wins & High Value)

| ID | Fonctionnalité | Description | Valeur Métier | Effort |
|----|----------------|-------------|---------------|--------|
| F-001 | **Scanner code-barres** | Scan EAN/QR pour achats, ventes, inventaires via caméra | Gain de temps énorme, réduction erreurs | Moyen |
| F-002 | **Alertes expiration** | Notifications produits expirant dans X jours (configurable) | Conformité pharmaceutique, réduction pertes | Faible |
| F-003 | **Rapports PDF/Excel** | Export des ventes, stocks, mouvements en PDF et Excel | Comptabilité, audits, reporting | Moyen |
| F-004 | **Tableau de bord** | KPIs temps réel, graphiques tendances, widgets personnalisables | Aide à la décision, visibilité | Moyen |
| F-005 | **Gestion fournisseurs** | CRUD fournisseurs, liaison avec achats, historique | Traçabilité complète | Faible |

### Priorité Moyenne (Business Value)

| ID | Fonctionnalité | Description | Valeur Métier | Effort |
|----|----------------|-------------|---------------|--------|
| F-006 | **Commandes fournisseurs** | Bons de commande, suivi livraisons, réceptions partielles | Workflow achat complet | Élevé |
| F-007 | **Retours produits** | Gestion retours clients et fournisseurs avec motifs | Complétude métier | Moyen |
| F-008 | **Réservations stock** | Réserver stock pour commande future, libération auto | Gestion anticipée | Moyen |
| F-009 | **Multi-devise** | Support EUR, USD, XAF, XOF avec taux de change | Déploiement international | Moyen |
| F-010 | **Photos produits** | Image par produit avec stockage Supabase Storage | UX, identification visuelle | Faible |
| F-011 | **Étiquettes produits** | Génération et impression d'étiquettes avec code-barres | Opérations terrain | Moyen |
| F-012 | **Historique client** | Voir toutes les ventes d'un client, total dépensé | Fidélisation, analyse | Faible |
| F-013 | **Remises/Promotions** | Système de remises (%, fixe) par produit ou panier | Ventes, marketing | Moyen |
| F-014 | **Stock minimum auto** | Calcul automatique du stock min basé sur historique ventes | Optimisation | Moyen |

### Priorité Basse (Nice to Have)

| ID | Fonctionnalité | Description | Valeur Métier | Effort |
|----|----------------|-------------|---------------|--------|
| F-015 | **API publique** | REST API documentée pour intégrations tierces | Extensibilité, partenariats | Élevé |
| F-016 | **Mode caisse (POS)** | Interface simplifiée pour ventes rapides en comptoir | Retail, pharmacies de détail | Élevé |
| F-017 | **Historique prix** | Graphique évolution prix achat/vente dans le temps | Analytics, négociation | Faible |
| F-018 | **Prévisions stock** | ML pour prédire besoins réapprovisionnement | Optimisation avancée | Élevé |
| F-019 | **Import/Export CSV** | Import produits/clients en masse, export données | Migration, backup | Moyen |
| F-020 | **Multi-entreprise** | Plusieurs entreprises dans une instance (SaaS) | Modèle SaaS | Élevé |
| F-021 | **Intégration comptable** | Export vers logiciels comptables (Sage, QuickBooks) | Automatisation | Élevé |
| F-022 | **Chat/Support intégré** | Messagerie intégrée pour support utilisateur | Support client | Moyen |
| F-023 | **Mode hors-ligne avancé** | Sync sélective, mode avion explicite | Connectivité limitée | Moyen |
| F-024 | **Widgets mobiles** | Widgets Android/iOS pour accès rapide aux KPIs | UX mobile | Moyen |

---

## Détails des Fonctionnalités Prioritaires

### F-001 : Scanner Code-Barres

**Description complète :**
Permettre le scan de codes-barres (EAN-13, EAN-8, UPC, QR Code) via la caméra du téléphone pour :
- Rechercher un produit rapidement
- Ajouter un produit à un achat
- Ajouter un produit à une vente
- Faire l'inventaire plus rapidement
- Identifier un lot par son code

**Implémentation technique :**
- Android : ML Kit Barcode Scanning ou ZXing
- iOS : Vision framework (natif)
- Ajouter champ `barcode` à la table `products`
- Interface caméra intégrée dans les écrans concernés

**User Stories :**
1. En tant que pharmacien, je veux scanner un produit pour l'ajouter à une vente sans chercher dans la liste
2. En tant que gestionnaire de stock, je veux scanner les produits pendant l'inventaire pour gagner du temps
3. En tant qu'acheteur, je veux scanner un nouveau produit pour pré-remplir ses informations

---

### F-002 : Alertes Expiration

**Description complète :**
Système d'alertes pour les produits dont la date d'expiration approche.

**Fonctionnalités :**
- Configuration du seuil d'alerte (ex: 30, 60, 90 jours)
- Liste des produits expirant bientôt
- Notifications push quotidiennes
- Badge sur l'icône de l'app
- Export de la liste pour action

**Implémentation technique :**
- Job quotidien pour vérifier les expirations
- Table `user_preferences` pour stocker les seuils
- Service de notification (Firebase Cloud Messaging / APNs)
- Nouvel écran "Alertes Expiration"

**User Stories :**
1. En tant que pharmacien, je veux être alerté des produits qui expirent dans 30 jours
2. En tant que gestionnaire, je veux voir la liste des produits à écouler en priorité
3. En tant qu'admin, je veux configurer le seuil d'alerte par défaut

---

### F-003 : Rapports PDF/Excel

**Description complète :**
Génération et export de rapports au format PDF et Excel.

**Types de rapports :**
1. **Rapport de stock** - Stock actuel par produit/site/catégorie
2. **Rapport de ventes** - Ventes par période, produit, client
3. **Rapport de mouvements** - Historique des mouvements de stock
4. **Rapport d'inventaire** - Résultats d'inventaire avec écarts
5. **Rapport d'expiration** - Produits expirant par période
6. **Rapport d'achats** - Achats par fournisseur, période
7. **Rapport de profit** - Marges et profits par produit/période

**Implémentation technique :**
- Android : iText ou Apache POI
- iOS : PDFKit (natif) + libXlsxWriter
- Partage via Intent/Share Sheet
- Stockage temporaire avant envoi

**User Stories :**
1. En tant que comptable, je veux exporter les ventes du mois en Excel
2. En tant que pharmacien, je veux générer un PDF de l'état du stock pour l'audit
3. En tant que gestionnaire, je veux envoyer le rapport de profit par email

---

### F-004 : Tableau de Bord (Dashboard)

**Description complète :**
Écran d'accueil avec vue d'ensemble de l'activité et des indicateurs clés.

**KPIs proposés :**
- Ventes du jour / semaine / mois
- Nombre de transactions
- Chiffre d'affaires
- Marge brute
- Stock total valorisé
- Produits en stock faible
- Produits expirant bientôt
- Dernières transactions

**Graphiques :**
- Évolution des ventes (ligne)
- Répartition par catégorie (camembert)
- Top 10 produits vendus (barres)
- Stock par site (barres)

**Implémentation technique :**
- Android : MPAndroidChart
- iOS : Swift Charts (iOS 16+) ou Charts library
- Calculs côté shared module (UseCases)
- Cache des métriques pour performance

**User Stories :**
1. En tant que gérant, je veux voir les ventes du jour dès l'ouverture de l'app
2. En tant que pharmacien, je veux voir rapidement les produits en rupture
3. En tant qu'admin, je veux comparer les performances des différents sites

---

### F-005 : Gestion Fournisseurs

**Description complète :**
Module complet de gestion des fournisseurs.

**Fonctionnalités :**
- CRUD fournisseurs (nom, contact, téléphone, email, adresse)
- Association fournisseur ↔ achat
- Historique des achats par fournisseur
- Notes et informations de paiement
- Statut actif/inactif

**Modèle de données :**
```sql
CREATE TABLE suppliers (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    contact_name TEXT,
    phone TEXT,
    email TEXT,
    address TEXT,
    notes TEXT,
    payment_terms TEXT,
    is_active INTEGER DEFAULT 1,
    created_at INTEGER,
    updated_at INTEGER,
    created_by TEXT,
    updated_by TEXT
);

-- Modifier purchase_batches
ALTER TABLE purchase_batches ADD COLUMN supplier_id TEXT REFERENCES suppliers(id);
```

**User Stories :**
1. En tant qu'acheteur, je veux sélectionner un fournisseur lors d'un achat
2. En tant que gestionnaire, je veux voir l'historique des achats d'un fournisseur
3. En tant qu'admin, je veux gérer la liste des fournisseurs autorisés

---

## Priorisation Recommandée

### Phase 15 - Quick Wins (2-3 semaines)
1. F-002 : Alertes expiration
2. F-005 : Gestion fournisseurs
3. UX-02 : Recherche globale
4. SEC-02 : Session timeout

### Phase 16 - Core Features (4-6 semaines)
1. F-001 : Scanner code-barres
2. F-004 : Tableau de bord
3. PERF-01 : Pagination listes
4. UX-04 : Notifications push

### Phase 17 - Reporting (2-3 semaines)
1. F-003 : Rapports PDF/Excel
2. F-012 : Historique client
3. F-017 : Historique prix

### Phase 18 - Advanced Features (4-6 semaines)
1. F-006 : Commandes fournisseurs
2. F-007 : Retours produits
3. F-013 : Remises/Promotions
4. SEC-03 : Biométrie

### Phase 19 - Enterprise (6-8 semaines)
1. F-009 : Multi-devise
2. F-015 : API publique
3. F-020 : Multi-entreprise
4. SEC-01 : 2FA/MFA

---

## Notes Techniques

### Dépendances à Ajouter

**Scanner code-barres :**
```kotlin
// Android - build.gradle
implementation("com.google.mlkit:barcode-scanning:17.2.0")

// iOS - Natif (Vision framework)
```

**Graphiques Dashboard :**
```kotlin
// Android - build.gradle
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

// iOS - Package.swift
.package(url: "https://github.com/danielgindi/Charts.git", from: "5.0.0")
```

**Export PDF/Excel :**
```kotlin
// Android - build.gradle
implementation("com.itextpdf:itext7-core:7.2.5")
implementation("org.apache.poi:poi-ooxml:5.2.3")
```

### Considérations KMP

Pour les nouvelles fonctionnalités, privilégier :
1. Logique métier dans `shared/domain/`
2. UseCases pour les calculs (Dashboard, Alertes)
3. Repositories pour l'accès données
4. UI platform-specific uniquement

---

## Feedback & Contributions

Ce document est vivant et sera mis à jour régulièrement.

Pour proposer une nouvelle fonctionnalité :
1. Créer une issue GitHub avec le label `feature-request`
2. Décrire le besoin métier
3. Proposer une solution technique si possible

---

*Document créé le 23 Janvier 2026*
