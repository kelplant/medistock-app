# pg_cron - Tâches Planifiées Supabase

## Qu'est-ce que pg_cron ?

pg_cron est une extension PostgreSQL qui permet de planifier des tâches SQL directement dans la base de données, similaire à cron sur Linux.

## Activation sur Supabase

pg_cron est pré-installé sur Supabase. Pour l'activer :

1. Aller dans **Database → Extensions**
2. Rechercher **"pg_cron"**
3. Activer l'extension

## Syntaxe Cron

```
┌───────────── minute (0 - 59)
│ ┌───────────── heure (0 - 23)
│ │ ┌───────────── jour du mois (1 - 31)
│ │ │ ┌───────────── mois (1 - 12)
│ │ │ │ ┌───────────── jour de la semaine (0 - 6) (Dimanche = 0)
│ │ │ │ │
* * * * *
```

### Exemples

| Expression | Description |
|------------|-------------|
| `0 9 * * *` | Tous les jours à 9h00 UTC |
| `*/15 * * * *` | Toutes les 15 minutes |
| `0 0 * * 0` | Tous les dimanches à minuit |
| `0 8 1 * *` | Le 1er de chaque mois à 8h00 |
| `30 14 * * 1-5` | Du lundi au vendredi à 14h30 |

## Jobs MediStock

### check-notifications-daily

| Propriété | Valeur |
|-----------|--------|
| **Schedule** | `0 9 * * *` (9h00 UTC quotidien) |
| **Fonction** | `check_notification_alerts()` |
| **Description** | Vérifie les produits expirés, expirants et stock faible |

**Ce que fait ce job :**
1. Détecte les produits **expirés** (date < maintenant) → notification CRITICAL
2. Détecte les produits **expirant dans 7 jours** → notification HIGH
3. Détecte les produits avec **stock < minimum** → notification MEDIUM

## Commandes Utiles

### Lister les jobs

```sql
SELECT jobid, jobname, schedule, command, active
FROM cron.job;
```

### Voir l'historique d'exécution

```sql
SELECT jobid, runid, job_pid, status, return_message, start_time, end_time
FROM cron.job_run_details
ORDER BY start_time DESC
LIMIT 10;
```

### Exécuter manuellement

```sql
SELECT check_notification_alerts();
```

### Supprimer un job

```sql
SELECT cron.unschedule('check-notifications-daily');
```

### Modifier un job

```sql
-- Supprimer l'ancien
SELECT cron.unschedule('check-notifications-daily');

-- Créer le nouveau avec un horaire différent
SELECT cron.schedule(
    'check-notifications-daily',
    '0 10 * * *',  -- Nouveau: 10h00 UTC
    $$SELECT check_notification_alerts()$$
);
```

### Désactiver temporairement un job

```sql
UPDATE cron.job SET active = false WHERE jobname = 'check-notifications-daily';
```

### Réactiver un job

```sql
UPDATE cron.job SET active = true WHERE jobname = 'check-notifications-daily';
```

## Timezone

pg_cron utilise **UTC par défaut**. Voici la correspondance avec l'heure locale :

| UTC | France (hiver) | France (été) |
|-----|----------------|--------------|
| 09:00 | 10:00 | 11:00 |
| 00:00 | 01:00 | 02:00 |
| 12:00 | 13:00 | 14:00 |

## Troubleshooting

### Le job ne s'exécute pas

1. **Vérifier que l'extension est activée**
   ```sql
   SELECT * FROM pg_extension WHERE extname = 'pg_cron';
   ```

2. **Vérifier que le job existe et est actif**
   ```sql
   SELECT * FROM cron.job WHERE jobname = 'check-notifications-daily';
   ```

3. **Vérifier les erreurs dans l'historique**
   ```sql
   SELECT * FROM cron.job_run_details
   WHERE jobid = (SELECT jobid FROM cron.job WHERE jobname = 'check-notifications-daily')
   ORDER BY start_time DESC
   LIMIT 5;
   ```

### Erreur de permission

Les jobs s'exécutent avec les droits du propriétaire de la fonction. Si vous avez des erreurs de permission :

```sql
-- Vérifier le propriétaire de la fonction
SELECT proname, proowner::regrole
FROM pg_proc
WHERE proname = 'check_notification_alerts';

-- La fonction doit utiliser SECURITY DEFINER pour accéder aux tables
CREATE OR REPLACE FUNCTION check_notification_alerts()
RETURNS void AS $$
...
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### Le job tourne mais ne crée pas de notifications

1. Vérifier qu'il y a des données à traiter :
   ```sql
   -- Produits expirés
   SELECT * FROM purchase_batches
   WHERE expiry_date < (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
     AND is_exhausted = 0;

   -- Produits sous le stock minimum
   SELECT p.name, p.min_stock, COALESCE(SUM(pb.remaining_quantity), 0) as current
   FROM products p
   LEFT JOIN purchase_batches pb ON pb.product_id = p.id AND pb.is_exhausted = 0
   WHERE p.min_stock > 0 AND p.is_active = 1
   GROUP BY p.id
   HAVING COALESCE(SUM(pb.remaining_quantity), 0) < p.min_stock;
   ```

2. Vérifier les doublons (la fonction évite les doublons sur 7 jours) :
   ```sql
   SELECT * FROM notification_events
   ORDER BY created_at DESC
   LIMIT 20;
   ```

## Scripts SQL

Les scripts de création sont dans le répertoire `supabase/cron/` :

| Fichier | Description |
|---------|-------------|
| `001_notification_tables.sql` | Tables notification_events, dismissals, preferences |
| `002_check_alerts_function.sql` | Fonction PL/pgSQL de vérification |
| `003_schedule_cron_job.sql` | Planification du job pg_cron |

## Références

- [Documentation pg_cron](https://github.com/citusdata/pg_cron)
- [Supabase pg_cron Guide](https://supabase.com/docs/guides/database/extensions/pg_cron)
