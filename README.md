# Medistock App

Application Android de gestion de stock pharmaceutique (sites, produits, achats, ventes, transferts, inventaires) avec synchronisation Supabase.

## Fonctionnalités principales

- Gestion multi-sites (création, sélection, transferts)
- Gestion des catégories, conditionnements, produits et prix
- Achats (lots FIFO), mouvements de stock, inventaires
- Ventes avec allocations FIFO et clients
- Gestion utilisateurs et permissions
- Synchronisation Supabase avec mode offline
- Audit trail complet des modifications

## Prérequis

- Android Studio (ou Gradle CLI)
- JDK 17
- Accès Supabase si vous activez la synchronisation (URL + clé API)

## Configuration du projet

1. Cloner le dépôt
2. Ouvrir le projet dans Android Studio
3. (Optionnel) Configurer Supabase via l'écran **Admin > Supabase** dans l'app :
   - URL Supabase
   - Clé API

## Lancer l'application

- Depuis Android Studio : **Run** sur un appareil/émulateur
- En ligne de commande :
  ```bash
  ./gradlew assembleDebug
  ```

L'APK debug se trouve dans `app/build/outputs/apk/debug/`.

## Structure du projet

```
medistock-app/
├── app/src/main/java/com/medistock/   # Code Android (UI, data, sync)
├── app/src/main/res/                   # Ressources (layouts, strings, styles)
├── app/src/main/assets/migrations/     # Migrations SQL Supabase
├── supabase/                           # Scripts SQL initiaux
└── documentation/                      # Documentation complète
```

---

## Documentation

### Guide de démarrage

| Document | Description |
|----------|-------------|
| [SUPABASE-SETUP-GUIDE.md](documentation/SUPABASE-SETUP-GUIDE.md) | Guide de configuration initiale de Supabase |
| [SUPABASE-README.md](documentation/SUPABASE-README.md) | Vue d'ensemble de l'intégration Supabase |
| [TESTING.md](documentation/TESTING.md) | Guide pour exécuter les tests |
| [RELEASE_GUIDE.md](documentation/RELEASE_GUIDE.md) | Guide de publication des releases |

### Architecture et fonctionnalités

| Document | Description |
|----------|-------------|
| [SUPABASE-INTEGRATION-GUIDE.md](documentation/SUPABASE-INTEGRATION-GUIDE.md) | Guide complet d'intégration Supabase |
| [OFFLINE_SYNC_STRATEGY.md](documentation/OFFLINE_SYNC_STRATEGY.md) | Stratégie de synchronisation offline/online |
| [AUDIT_HISTORY_SYSTEM.md](documentation/AUDIT_HISTORY_SYSTEM.md) | Système d'audit et traçabilité |
| [DATABASE_SCHEMA_AUDIT.md](documentation/DATABASE_SCHEMA_AUDIT.md) | Schéma de base de données complet |
| [PACKAGING_SYSTEM.md](documentation/PACKAGING_SYSTEM.md) | Système de conditionnement (niveaux 1 & 2) |

### Supabase - Détails techniques

| Document | Description |
|----------|-------------|
| [supabase/realtime.md](documentation/supabase/realtime.md) | Configuration Realtime Supabase |

### Migrations SQL

Les migrations Supabase sont dans `app/src/main/assets/migrations/` :

| Migration | Description |
|-----------|-------------|
| `2025122601_uuid_migration.sql` | Migration vers UUID |
| `2025122602_created_updated_by.sql` | Champs created_by/updated_by |
| `2025122603_audit_triggers.sql` | Triggers d'audit |
| `2025122604_audit_trigger_null_site.sql` | Gestion site null dans audit |
| `2025122605_add_product_description.sql` | Ajout description produit |
| `2025122605_transaction_flat_view.sql` | Vue transactions plate |
| `2026010501_schema_cleanup.sql` | Nettoyage schéma |
| `2026011701_migration_system.sql` | Système de migrations |
| `2026011702_schema_version.sql` | Versioning du schéma |
| `2026011801_sync_tracking.sql` | Tracking synchronisation offline |

---

## Scripts Supabase initiaux

Les scripts SQL initiaux sont dans le dossier `supabase/` :

- `supabase/init.sql` : schéma complet (tables, triggers)
- `supabase/rls-policies.sql` : politiques RLS

## Notes

- Les champs `created_by` / `updated_by` sont remplis automatiquement par l'app et sécurisés côté base via defaults + triggers
- Le mode offline stocke les modifications localement et les synchronise au retour de la connexion
- Les conflits de synchronisation sont résolus selon des stratégies par type d'entité
