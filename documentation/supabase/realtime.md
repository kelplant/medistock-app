# 📡 Supabase Realtime — Activation et configuration

## Prérequis
- Realtime activé sur le projet Supabase.
- Un rôle disposant du droit `replication` sur les tables concernées : service key ou anon si vous ouvrez la réplication sur l'anon (à ajuster selon votre politique de sécurité).
- Tables déjà créées dans votre base (voir `supabase/init.sql`).

## Script SQL à exécuter dans le SQL Editor
Copiez/collez le script suivant dans le SQL Editor Supabase pour activer la publication Realtime sur les tables cibles (à adapter selon vos besoins) :

```sql
-- Activer Realtime sur les tables
alter publication supabase_realtime add table sites, packaging_types, categories, products, customers;
-- Optionnel : colonne client_id pour filtrer les échos
alter table products add column if not exists client_id text;
-- Répéter pour chaque table concernée
```

### Variante : activer Realtime sur toutes les tables du schéma `public`
Si vous souhaitez ajouter automatiquement **toutes les tables** du schéma `public` à la publication `supabase_realtime` (sans dupliquer celles déjà présentes), exécutez ce bloc anonyme :

```sql
do $$
declare
  rec record;
begin
  for rec in
    select schemaname, tablename
    from pg_tables pt
    where schemaname = 'public'
      and not exists (
        select 1
        from pg_publication_tables ppt
        where ppt.pubname = 'supabase_realtime'
          and ppt.schemaname = pt.schemaname
          and ppt.tablename = pt.tablename
      )
  loop
    execute format('alter publication supabase_realtime add table %I.%I;', rec.schemaname, rec.tablename);
  end loop;
end $$;
```

## Variables à renseigner dans l'application
- **URL Supabase** (`SUPABASE_URL`) : `https://<PROJECT_ID>.supabase.co`
- **Anon key** (`SUPABASE_ANON_KEY`) : clé publique disponible dans `Settings > API`

## Notes réseau
- Autorisez les connexions WebSocket sortantes vers votre domaine Supabase (`wss://<PROJECT_ID>.supabase.co/realtime/v1`).
- Si l'application tourne derrière un proxy/pare-feu, assurez-vous que les ports HTTPS standard (443) et WebSocket ne sont pas filtrés.
