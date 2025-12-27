-- Handle null/empty site column arguments in audit trigger
-- Ensures audit triggers don't attempt to format a "null" column name

CREATE OR REPLACE FUNCTION log_audit_history_trigger()
RETURNS TRIGGER AS $$
DECLARE
    site_column TEXT;
    site_value UUID;
    change_user TEXT;
    target_id UUID;
    new_value TEXT;
    old_value TEXT;
BEGIN
    -- Récupère l'argument facultatif passé par le trigger (nom de colonne site)
    IF TG_NARGS > 0 THEN
        site_column := TG_ARGV[0];
    ELSE
        site_column := NULL;
    END IF;

    -- Normalise les valeurs vides/NULL explicites passées par le trigger helper
    IF site_column IS NULL OR site_column = '' OR lower(site_column) = 'null' THEN
        site_column := NULL;
    END IF;

    change_user := COALESCE(
        current_setting('request.jwt.claim.email', true),
        current_setting('request.jwt.claim.sub', true),
        current_user::text,
        'system'
    );

    target_id := COALESCE(CASE WHEN TG_OP = 'DELETE' THEN OLD.id ELSE NEW.id END, gen_random_uuid());

    IF site_column IS NOT NULL AND site_column <> '' THEN
        IF TG_OP = 'DELETE' THEN
            EXECUTE format('SELECT ($1).%I::uuid', site_column) USING OLD INTO site_value;
        ELSE
            EXECUTE format('SELECT ($1).%I::uuid', site_column) USING NEW INTO site_value;
        END IF;
    ELSE
        site_value := NULL;
    END IF;

    -- Préparer les valeurs JSON pour la déduplication et l'insertion
    IF TG_OP = 'INSERT' THEN
        old_value := NULL;
        new_value := to_jsonb(NEW)::text;
    ELSIF TG_OP = 'UPDATE' THEN
        old_value := to_jsonb(OLD)::text;
        new_value := to_jsonb(NEW)::text;
    ELSE -- DELETE
        old_value := to_jsonb(OLD)::text;
        new_value := NULL;
    END IF;

    -- Éviter les doublons si l'app a déjà écrit dans audit_history pour cette opération
    IF EXISTS (
        SELECT 1
        FROM audit_history ah
        WHERE ah.entity_type = TG_TABLE_NAME
          AND ah.entity_id = target_id
          AND ah.action_type = TG_OP
          AND (
              (ah.old_value IS NOT DISTINCT FROM old_value)
              OR ah.old_value IS NULL AND old_value IS NULL
          )
          AND (
              (ah.new_value IS NOT DISTINCT FROM new_value)
              OR ah.new_value IS NULL AND new_value IS NULL
          )
    ) THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        ELSE
            RETURN NEW;
        END IF;
    END IF;

    IF TG_OP = 'INSERT' THEN
        INSERT INTO audit_history (entity_type, entity_id, action_type, field_name, old_value, new_value, changed_by, site_id, description, changed_at)
        VALUES (TG_TABLE_NAME, target_id, 'INSERT', 'ALL_FIELDS', NULL, new_value, change_user, site_value, 'Supabase trigger audit', EXTRACT(EPOCH FROM NOW())::BIGINT * 1000);
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_history (entity_type, entity_id, action_type, field_name, old_value, new_value, changed_by, site_id, description, changed_at)
        VALUES (TG_TABLE_NAME, target_id, 'UPDATE', 'ALL_FIELDS', old_value, new_value, change_user, site_value, 'Supabase trigger audit', EXTRACT(EPOCH FROM NOW())::BIGINT * 1000);
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO audit_history (entity_type, entity_id, action_type, field_name, old_value, new_value, changed_by, site_id, description, changed_at)
        VALUES (TG_TABLE_NAME, target_id, 'DELETE', 'ALL_FIELDS', old_value, NULL, change_user, site_value, 'Supabase trigger audit', EXTRACT(EPOCH FROM NOW())::BIGINT * 1000);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

