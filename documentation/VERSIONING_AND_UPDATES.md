# Systeme de versioning, migrations et mises a jour

## Vue d'ensemble

MediStock utilise un systeme de versioning bidirectionnel qui garantit la compatibilite
entre l'application (Android/iOS) et la base de donnees Supabase (PostgreSQL).

### Flux au demarrage de l'application

```
1. [BLOQUANT]  Verification GitHub (nouvelle version disponible ?)
       |           -> Si oui : ecran de telechargement (versions reelles ex: 0.8.0 -> 0.9.0)
       v
2. [Silencieux] Execution des migrations Supabase en attente
       |
       v
3. [BLOQUANT]  Verification de compatibilite schema
       |           - AppTooOld : l'app doit etre mise a jour
       |             -> Ecran de telechargement (meme ecran que etape 1)
       |           - DbTooOld : la DB est incompatible apres migrations
       |             -> Ecran "Base de donnees incompatible" (pas de telechargement,
       |                contactez l'administrateur)
       v
4. [Normal]    Demarrage de la synchronisation
```

Les etapes 1 et 3 sont bloquantes : l'app ne peut pas etre utilisee tant que
le probleme n'est pas resolu.

**Important :** La verification GitHub compare les versions reelles de l'app
(ex: `0.8.0` vs `0.9.0`) via `https://api.github.com/repos/kelplant/medistock-app/releases/latest`.
La verification schema compare les numeros internes (`APP_SCHEMA_VERSION`, `MIN_SCHEMA_VERSION`)
avec la table `schema_version` de Supabase. Les numeros internes ne sont jamais affiches
a l'utilisateur.

---

## Numeros de version

### Cote Supabase (PostgreSQL)

| Champ             | Description                                                     | Table           |
|-------------------|-----------------------------------------------------------------|-----------------|
| `schema_version`  | Numero de revision du schema DB. Incremente a chaque migration. | `schema_version`|
| `min_app_version` | Version minimale de l'app requise pour utiliser cette DB.       | `schema_version`|

La table `schema_version` est un singleton (une seule ligne, `id = 1`).

**Fonctions RPC :**
- `get_schema_version()` : retourne `schema_version`, `min_app_version`, `updated_at`
- `update_schema_version(p_schema_version, p_min_app_version, p_updated_by)` : met a jour

### Cote Application (Android + iOS)

| Constante            | Description                                            | Fichier                          |
|----------------------|--------------------------------------------------------|----------------------------------|
| `APP_SCHEMA_VERSION` | Version du schema supportee par cette app.             | `CompatibilityChecker.kt` (shared) |
| `MIN_SCHEMA_VERSION` | Version minimale du schema DB requise par cette app.   | `CompatibilityChecker.kt` (shared) |

Duplications necessaires (doivent rester synchronisees) :
- iOS : `MigrationManager.swift` (`APP_SCHEMA_VERSION`)
- Android : `build.gradle` (`buildConfigField`)

### Verification bidirectionnelle

```
DB.min_app_version > APP_SCHEMA_VERSION  -->  App trop ancienne (AppTooOld)
                                              L'utilisateur doit mettre a jour l'app.

DB.schema_version < MIN_SCHEMA_VERSION   -->  DB trop ancienne (DbTooOld)
                                              Les migrations n'ont pas ete appliquees.
                                              L'administrateur doit les executer.

Sinon                                    -->  Compatible
```

---

## Systeme de migrations

### Architecture

Les migrations SQL sont stockees dans deux emplacements :
- `supabase/migration/` : fichiers source des migrations
- `shared/src/commonMain/resources/migrations/` : copies pour le bundle app

Un script Gradle (`copySharedMigrations`) copie les migrations dans `app/src/main/assets/migrations/`
au moment du build Android.

### Table de suivi

```sql
CREATE TABLE schema_migrations (
    name TEXT PRIMARY KEY,          -- Nom du fichier (sans .sql)
    checksum TEXT,                  -- SHA-256 du contenu SQL
    applied_at BIGINT,              -- Timestamp d'application
    applied_by TEXT,                -- Qui a applique (app, ios_app, manual, init)
    success BOOLEAN,                -- Reussite ou echec
    execution_time_ms INTEGER,      -- Temps d'execution
    error_message TEXT              -- Message d'erreur si echec
);
```

### Flux d'execution

1. L'app charge les fichiers `.sql` depuis son bundle
2. Recupere les migrations deja appliquees via `get_applied_migrations()` RPC
3. Compare et identifie les migrations en attente
4. Execute chaque migration en attente dans l'ordre alphabetique
5. S'arrete a la premiere erreur

### Convention de nommage

```
YYYYMMDDNN_description.sql
```

Exemples :
- `2026012901_version_catchup.sql`
- `2026012507_add_suppliers_table.sql`
- `20260125002000_seed_demo_data.sql` (format long pour garantir l'ordre)

### Regles pour les nouvelles migrations

**IMPORTANT : A chaque nouvelle migration, vous DEVEZ :**

1. Creer le fichier `.sql` dans `supabase/migration/`
2. Copier le fichier dans `shared/src/commonMain/resources/migrations/`
3. Ajouter `SELECT update_schema_version(N, M, 'migration_XXXX');` a la fin du fichier
   - `N` = `schema_version` actuel + 1
   - `M` = `min_app_version` (incrementer UNIQUEMENT si la migration est incompatible
     avec les anciennes versions de l'app)
4. Mettre a jour `init.sql` :
   - Ajouter l'entree dans `schema_migrations` INSERT
   - Mettre a jour le `VALUES` de `schema_version`
   - Mettre a jour les `RAISE NOTICE` finaux
5. Si `min_app_version` est incremente :
   - Mettre a jour `APP_SCHEMA_VERSION` dans `CompatibilityChecker.kt`
   - Mettre a jour `APP_SCHEMA_VERSION` dans `MigrationManager.swift` (iOS)
   - Mettre a jour `buildConfigField` dans `app/build.gradle` (Android)
6. Si la migration ajoute des dependances cote app :
   - Mettre a jour `MIN_SCHEMA_VERSION` dans `CompatibilityChecker.kt`
   - Mettre a jour `MIN_SCHEMA_VERSION` dans `CompatibilityManager.swift` (iOS)
   - Mettre a jour `buildConfigField` dans `app/build.gradle` (Android)

---

## Verification GitHub (mise a jour de l'app)

### Android

Fichier : `app/src/main/java/com/medistock/util/AppUpdateManager.kt`

- URL : `https://api.github.com/repos/kelplant/medistock-app/releases/latest`
- Compare `versionName` avec le `tag_name` de la release (format `vX.Y.Z`)
- Telecharge l'APK depuis les assets de la release
- Installe via `ACTION_VIEW` + FileProvider

Declenchement :
- Au demarrage de l'app (**bloquant** : redirige vers `AppUpdateRequiredActivity`)
- Toutes les 5 minutes quand l'app revient au premier plan

Ecran de blocage (`AppUpdateRequiredActivity`) — deux modes :
- `MODE_UPDATE` : mise a jour disponible ou app trop ancienne.
  Affiche les versions reelles + boutons de telechargement GitHub / Play Store.
- `MODE_DB_INCOMPATIBLE` : base de donnees incompatible apres migrations.
  Affiche un message "contactez l'administrateur", pas de bouton de telechargement.

### iOS

Fichier : `iosApp/iosApp/ContentView.swift` (methode `checkGitHubUpdate()`)

- Meme URL GitHub API
- Compare `CFBundleShortVersionString` avec le tag de la release
- Affiche une alerte SwiftUI avec lien vers la page de releases

---

## Migration locale SQLite

Le schema SQLite local (sur l'appareil) est gere separement du schema Supabase.

### Android

Fichier : `Platform.android.kt` - methode `preMigrateDatabase()`

Avant que SQLDelight ouvre la DB, des DDL idempotents sont executes :
- `ALTER TABLE ... ADD COLUMN` (ignore si la colonne existe deja)
- `CREATE TABLE IF NOT EXISTS` (ignore si la table existe deja)

Ce mecanisme est sans version : il s'execute a chaque demarrage.

### iOS

Fichier : `Platform.ios.kt` - wrapper `migratingSchema`

Utilise le `PRAGMA user_version` de SQLite (gere par NativeSqliteDriver) :
- Version 1 : schema original cree par SQLDelight
- Version 2 : ajout tables app_config, suppliers + colonne app_users.language

Le callback `upgrade` du NativeSqliteDriver execute les DDL idempotents
quand `user_version < 2`.

**NOTE :** Ce numero de version est LOCAL et n'a aucun rapport avec
le `schema_version` de Supabase.

---

## Historique des versions

### schema_version (Supabase)

| Version | Migration                           | Description                        |
|---------|-------------------------------------|------------------------------------|
| 2       | 2026011702_schema_version           | Systeme de versioning              |
| 3       | 2026011801_sync_tracking            | Tracking de synchronisation        |
| 4       | 2026011901_remove_audit_triggers    | Suppression triggers d'audit       |
| 5-29    | *(catch-up via 2026012901)*         | 25 migrations non-incrementees     |
| 29      | 2026012901_version_catchup          | Rattrapage de toutes les versions  |

### APP_SCHEMA_VERSION (Application)

| Version | Description                                                    |
|---------|----------------------------------------------------------------|
| 1       | Schema initial                                                 |
| 2       | Systeme de migration et versioning                             |
| 3       | Suppression unit produit, ajout packaging types, suppliers     |

### MIN_SCHEMA_VERSION (Application)

| Version | Description                                                    |
|---------|----------------------------------------------------------------|
| 29      | Valeur initiale - requiert toutes les migrations jusqu'a v29   |

---

## Fichiers cles

| Fichier | Role |
|---------|------|
| `shared/.../CompatibilityChecker.kt` | Logique partagee de verification bidirectionnelle |
| `shared/.../Platform.android.kt` | Migration locale SQLite (Android) |
| `shared/.../Platform.ios.kt` | Migration locale SQLite (iOS) |
| `app/.../MedistockApplication.kt` | Flux de demarrage Android (GitHub + migrations + compat) |
| `app/.../LoginActivity.kt` | Logique de blocage au login (GitHub, AppTooOld, DbTooOld) |
| `app/.../AppUpdateManager.kt` | Check GitHub releases (Android) |
| `app/.../AppUpdateRequiredActivity.kt` | Ecran de blocage (MODE_UPDATE / MODE_DB_INCOMPATIBLE) |
| `app/.../MigrationManager.kt` | Execution migrations Supabase (Android) |
| `app/.../SchemaVersionChecker.kt` | Cache local de la version schema (Android) |
| `iosApp/.../ContentView.swift` | Flux de demarrage iOS + check GitHub |
| `iosApp/.../CompatibilityManager.swift` | Check compatibilite (iOS) |
| `iosApp/.../MigrationManager.swift` | Execution migrations Supabase (iOS) |
| `iosApp/.../AppUpdateRequiredView.swift` | Ecran de blocage (iOS) |
| `supabase/migration/` | Fichiers source des migrations |
| `supabase/init.sql` | Schema complet pour installation fresh |
