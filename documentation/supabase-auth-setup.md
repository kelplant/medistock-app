# MediStock - Guide de Configuration Supabase Auth

Ce guide explique comment configurer Supabase Auth pour une nouvelle instance MediStock.

## Table des Matières

1. [Pré-requis](#pré-requis)
2. [Configuration Dashboard](#configuration-dashboard)
3. [Déploiement Edge Functions](#déploiement-edge-functions)
4. [Premier Lancement App](#premier-lancement-app)
5. [Procédure de Récupération](#procédure-de-récupération)
6. [Architecture](#architecture)
7. [Troubleshooting](#troubleshooting)

---

## Pré-requis

- [ ] Projet Supabase créé sur [supabase.com](https://supabase.com)
- [ ] Supabase CLI installé: `npm install -g supabase`
- [ ] Accès au repository GitHub MediStock
- [ ] (Optionnel) GitHub Token pour repos privés

---

## Configuration Dashboard

### 1. Authentication Settings

Aller dans **Dashboard → Authentication → Providers → Email**:

| Paramètre | Valeur | Raison |
|-----------|--------|--------|
| Confirm email | **Désactivé** | Les comptes sont créés par les admins |
| Secure email change | **Désactivé** | On utilise UUID-based emails |
| Secure password change | Activé | Sécurité recommandée |

### 2. Disable Public Signup

Aller dans **Dashboard → Authentication → Settings**:

| Paramètre | Valeur | Raison |
|-----------|--------|--------|
| Enable email signup | **Désactivé** | Seuls les Edge Functions créent des comptes |

### 3. JWT Settings (optionnel)

Aller dans **Dashboard → Settings → API → JWT Settings**:

| Paramètre | Valeur Recommandée |
|-----------|-------------------|
| JWT expiry | 3600 (1 heure) |

### 4. Edge Functions Secrets

Aller dans **Dashboard → Edge Functions → Secrets**:

| Secret | Valeur | Obligatoire |
|--------|--------|-------------|
| `GITHUB_REPO` | `votre-org/medistock-app` | Oui |
| `GITHUB_BRANCH` | `main` | Oui |
| `GITHUB_TOKEN` | `ghp_xxxxx` | Si repo privé |

**Note:** `SUPABASE_URL` et `SUPABASE_SERVICE_ROLE_KEY` sont automatiquement disponibles.

---

## Déploiement Edge Functions

### Via CLI Supabase

```bash
# 1. Se connecter
supabase login

# 2. Lier le projet
supabase link --project-ref <votre-project-ref>

# 3. Déployer toutes les fonctions
supabase functions deploy bootstrap-instance
supabase functions deploy create-user
supabase functions deploy update-user
supabase functions deploy delete-user
supabase functions deploy apply-migration
supabase functions deploy migrate-user-to-auth
```

### Vérification

```bash
# Lister les fonctions déployées
supabase functions list
```

---

## Premier Lancement App

### Flow de Bootstrap

```
┌─────────────────────────────────────────────┐
│ 1. Configurer URL Supabase dans l'app       │
│    (Settings → Supabase Configuration)      │
└─────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────┐
│ 2. L'app détecte que l'instance est vierge  │
│    → Affiche écran "Setup Initial"          │
└─────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────┐
│ 3. Créer le compte administrateur           │
│    • Username                               │
│    • Password (min 8 car, 1 maj, 1 chiffre) │
│    • Nom                                    │
└─────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────┐
│ 4. L'app appelle bootstrap-instance         │
│    • Exécute les migrations                 │
│    • Crée l'admin dans Supabase Auth        │
│    • Crée l'admin dans la table users       │
│    • Retourne une session                   │
└─────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────┐
│ 5. App prête à utiliser !                   │
└─────────────────────────────────────────────┘
```

### Checklist Setup

- [ ] URL Supabase configurée
- [ ] Edge Functions déployées
- [ ] Compte admin créé via l'app
- [ ] Migrations exécutées avec succès
- [ ] Test de connexion réussi

---

## Procédure de Récupération

### Quand utiliser ?

- Plus aucun admin accessible
- Mot de passe admin oublié
- Compte admin corrompu

### Comment faire ?

1. Aller sur **Supabase Dashboard → SQL Editor**

2. Exécuter:
```sql
-- Sans clé secrète
SELECT create_recovery_admin('admin_urgence', 'TempPass123!');

-- Avec clé secrète (si configurée)
SELECT create_recovery_admin('admin_urgence', 'TempPass123!', 'votre-cle-secrete');
```

3. Se connecter dans l'app:
   - Username: `admin_urgence`
   - Password: `TempPass123!`

4. **IMPORTANT: Changer le mot de passe immédiatement !**

### Configurer une clé secrète (optionnel)

Pour ajouter une couche de protection:

```sql
UPDATE app_config
SET value = 'votre-cle-secrete-complexe'
WHERE key = 'recovery_secret_key';
```

---

## Architecture

### Authentification UUID-based

```
Login Flow:

User tape "jean.dupont" + password
          │
          ▼
App cherche en local: username → UUID
    "jean.dupont" → "550e8400-e29b-41d4-a716-..."
          │
          ▼
Supabase Auth avec:
    "550e8400-e29b-41d4-a716-...@medistock.local"
          │
          ▼
JWT Token retourné
```

### Edge Functions

| Function | Auth Required | Description |
|----------|---------------|-------------|
| `bootstrap-instance` | Non | Premier setup uniquement |
| `create-user` | Admin | Créer un utilisateur |
| `update-user` | Admin/Self | Modifier un utilisateur |
| `delete-user` | Admin | Supprimer un utilisateur |
| `apply-migration` | Authenticated | Exécuter une migration |
| `migrate-user-to-auth` | Non | Migration transparente login |

### RLS Policies

| Table | Lecture | Écriture |
|-------|---------|----------|
| users | Self ou Admin | Admin only |
| products | Site access | Site access (delete: admin) |
| sales | Site access | Site access |
| categories | Tous | Admin only |
| audit_history | Tous | Insert only |

---

## Troubleshooting

### "Instance already initialized"

**Cause:** Bootstrap appelé sur une instance avec des users existants.

**Solution:** Utiliser le login normal ou la procédure de récupération.

### "Migration verification failed"

**Cause:** Le SQL envoyé ne correspond pas au fichier GitHub.

**Solutions:**
1. Vérifier que l'app est à jour
2. Vérifier que `GITHUB_REPO` et `GITHUB_BRANCH` sont corrects
3. Vérifier le `GITHUB_TOKEN` si repo privé

### "Authentication required"

**Cause:** Tentative d'accès sans session valide.

**Solution:** Se reconnecter via l'app.

### "Admin access required"

**Cause:** Opération admin tentée par un non-admin.

**Solution:** Contacter un administrateur.

### User ne peut pas se connecter après changement de username

**Cause:** Désynchronisation entre table users et Supabase Auth.

**Solution:** L'app gère automatiquement via `migrate-user-to-auth`. Si persiste, utiliser la récupération admin.

---

## Commandes Utiles SQL

### Voir les utilisateurs

```sql
SELECT id, username, name, is_admin, is_active, auth_migrated
FROM users
ORDER BY created_at;
```

### Voir les migrations appliquées

```sql
SELECT name, applied_at, applied_by, success
FROM schema_migrations
ORDER BY applied_at DESC;
```

### Vérifier la configuration

```sql
SELECT * FROM app_config;
```

### Réinitialiser un mot de passe (via SQL Editor)

```sql
-- Met à jour le hash BCrypt et force re-migration
UPDATE users
SET password_hash = crypt('nouveau_mdp', gen_salt('bf')),
    auth_migrated = 0
WHERE username = 'le_username';
```

---

## Support

Pour toute question ou problème:
- Ouvrir une issue sur le repository GitHub
- Consulter la documentation Supabase: https://supabase.com/docs
