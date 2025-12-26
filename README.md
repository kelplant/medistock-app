# Medistock App

Application Android de gestion de stock pharmaceutique (sites, produits, achats, ventes, transferts, inventaires) avec synchronisation Supabase.

## Fonctionnalités principales

- Gestion multi-sites (création, sélection, transferts).
- Gestion des catégories, conditionnements, produits et prix.
- Achats (lots FIFO), mouvements de stock, inventaires.
- Ventes avec allocations FIFO et clients.
- Gestion utilisateurs et permissions.
- Synchronisation Supabase (schéma + politiques RLS fournis).

## Prérequis

- Android Studio (ou Gradle CLI)
- JDK 17
- Accès Supabase si vous activez la synchronisation (URL + clé API)

## Configuration du projet

1. Cloner le dépôt.
2. Ouvrir le projet dans Android Studio.
3. (Optionnel) Configurer Supabase via l’écran **Admin > Supabase** dans l’app :
   - URL Supabase
   - Clé API

## Lancer l’application

- Depuis Android Studio : **Run** sur un appareil/émulateur.
- En ligne de commande :
  ```bash
  ./gradlew assembleDebug
  ```

L’APK debug se trouve dans `app/build/outputs/apk/debug/`.

## Scripts Supabase

Les scripts SQL sont regroupés dans le dossier `supabase/` :

- `supabase/init.sql` : schéma complet (tables, triggers).
- `supabase/rls-policies.sql` : politiques RLS.
- `supabase/migration/2025122601_uuid_migration.sql` : migration UUID.
- `supabase/migration/2025122602_created_updated_by.sql` : backfill et defaults `created_by`/`updated_by`.
- `supabase/migration/2025122603_audit_triggers.sql` : triggers d’audit avec déduplication.

## Structure du projet

- `app/src/main/java/com/medistock` : code Android (UI, data, sync).
- `app/src/main/res` : ressources (layouts, strings, styles).
- `supabase/` : scripts SQL de base de données.

## Notes

- Les champs `created_by` / `updated_by` sont remplis automatiquement par l’app et sécurisés côté base via defaults + triggers.
