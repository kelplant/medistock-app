-- Supabase migration: add audit triggers with deduplication
-- Apply via Supabase SQL editor or CLI

-- Ensure function exists/updated
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
    IF TG_NARGS > 0 THEN
        site_column := TG_ARGV[0];
    ELSE
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

    IF TG_OP = 'INSERT' THEN
        old_value := NULL;
        new_value := to_jsonb(NEW)::text;
    ELSIF TG_OP = 'UPDATE' THEN
        old_value := to_jsonb(OLD)::text;
        new_value := to_jsonb(NEW)::text;
    ELSE
        old_value := to_jsonb(OLD)::text;
        new_value := NULL;
    END IF;

    -- Skip if an identical audit row already exists (prevents double logging when app writes first)
    IF EXISTS (
        SELECT 1
        FROM audit_history ah
        WHERE ah.entity_type = TG_TABLE_NAME
          AND ah.entity_id = target_id
          AND ah.action_type = TG_OP
          AND ((ah.old_value IS NOT DISTINCT FROM old_value) OR (ah.old_value IS NULL AND old_value IS NULL))
          AND ((ah.new_value IS NOT DISTINCT FROM new_value) OR (ah.new_value IS NULL AND new_value IS NULL))
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

-- Utility to drop/recreate audit triggers for a table
CREATE OR REPLACE FUNCTION ensure_audit_trigger(table_name TEXT, site_column TEXT DEFAULT NULL) RETURNS VOID AS $$
BEGIN
    EXECUTE format('DROP TRIGGER IF EXISTS audit_%I_trigger ON %I', table_name, table_name);
    EXECUTE format(
        'CREATE TRIGGER audit_%1$I_trigger AFTER INSERT OR UPDATE OR DELETE ON %1$I
         FOR EACH ROW EXECUTE FUNCTION log_audit_history_trigger(%2$L);',
        table_name,
        site_column
    );
END;
$$ LANGUAGE plpgsql;

-- (Re)install triggers for all tables
SELECT ensure_audit_trigger('sites');
SELECT ensure_audit_trigger('categories');
SELECT ensure_audit_trigger('packaging_types');
SELECT ensure_audit_trigger('app_users');
SELECT ensure_audit_trigger('user_permissions');
SELECT ensure_audit_trigger('customers', 'site_id');
SELECT ensure_audit_trigger('products', 'site_id');
SELECT ensure_audit_trigger('product_prices');
SELECT ensure_audit_trigger('purchase_batches', 'site_id');
SELECT ensure_audit_trigger('stock_movements', 'site_id');
SELECT ensure_audit_trigger('inventories', 'site_id');
SELECT ensure_audit_trigger('product_transfers', 'from_site_id');
SELECT ensure_audit_trigger('sales', 'site_id');
SELECT ensure_audit_trigger('sale_items');
SELECT ensure_audit_trigger('sale_batch_allocations');
SELECT ensure_audit_trigger('product_sales', 'site_id');

-- Clean up helper to avoid leaving extra functions around if not wanted
DROP FUNCTION IF EXISTS ensure_audit_trigger(TEXT, TEXT);
