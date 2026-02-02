-- ============================================================================
-- MEDISTOCK MIGRATION SYSTEM
-- Creates the schema_migrations table and apply_migration() function
-- This enables automatic migration execution from the mobile app
-- ============================================================================

BEGIN;

-- ============================================================================
-- 1. TABLE DE SUIVI DES MIGRATIONS
-- ============================================================================

CREATE TABLE IF NOT EXISTS schema_migrations (
    name TEXT PRIMARY KEY,
    checksum TEXT,
    applied_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    applied_by TEXT NOT NULL DEFAULT 'system',
    success BOOLEAN NOT NULL DEFAULT TRUE,
    execution_time_ms INTEGER,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_schema_migrations_applied_at ON schema_migrations(applied_at);

COMMENT ON TABLE schema_migrations IS 'Tracks all applied database migrations';
COMMENT ON COLUMN schema_migrations.name IS 'Unique migration identifier (e.g., 2026011701_migration_system)';
COMMENT ON COLUMN schema_migrations.checksum IS 'MD5 hash of the migration SQL for integrity verification';
COMMENT ON COLUMN schema_migrations.applied_at IS 'Timestamp when migration was applied (ms since epoch)';
COMMENT ON COLUMN schema_migrations.applied_by IS 'User or system that applied the migration';
COMMENT ON COLUMN schema_migrations.success IS 'Whether the migration executed successfully';
COMMENT ON COLUMN schema_migrations.execution_time_ms IS 'Time taken to execute the migration in milliseconds';
COMMENT ON COLUMN schema_migrations.error_message IS 'Error message if migration failed';

-- ============================================================================
-- 2. FONCTION POUR APPLIQUER UNE MIGRATION
-- ============================================================================

-- Cette fonction utilise SECURITY DEFINER pour s'exécuter avec les droits
-- du propriétaire (superuser), permettant ainsi d'exécuter des DDL
CREATE OR REPLACE FUNCTION apply_migration(
    p_name TEXT,
    p_sql TEXT,
    p_checksum TEXT DEFAULT NULL,
    p_applied_by TEXT DEFAULT 'app'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_start_time BIGINT;
    v_end_time BIGINT;
    v_execution_time INTEGER;
    v_result JSONB;
BEGIN
    -- Vérifier si la migration a déjà été appliquée
    IF EXISTS (SELECT 1 FROM schema_migrations WHERE name = p_name AND success = TRUE) THEN
        RETURN jsonb_build_object(
            'success', FALSE,
            'already_applied', TRUE,
            'message', format('Migration %s has already been applied', p_name)
        );
    END IF;

    -- Enregistrer le temps de début
    v_start_time := EXTRACT(EPOCH FROM clock_timestamp())::BIGINT * 1000;

    BEGIN
        -- Exécuter le SQL de la migration
        EXECUTE p_sql;

        -- Calculer le temps d'exécution
        v_end_time := EXTRACT(EPOCH FROM clock_timestamp())::BIGINT * 1000;
        v_execution_time := (v_end_time - v_start_time)::INTEGER;

        -- Enregistrer la migration comme réussie
        INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms)
        VALUES (p_name, p_checksum, p_applied_by, TRUE, v_execution_time)
        ON CONFLICT (name) DO UPDATE SET
            checksum = EXCLUDED.checksum,
            applied_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
            applied_by = EXCLUDED.applied_by,
            success = TRUE,
            execution_time_ms = EXCLUDED.execution_time_ms,
            error_message = NULL;

        v_result := jsonb_build_object(
            'success', TRUE,
            'already_applied', FALSE,
            'message', format('Migration %s applied successfully', p_name),
            'execution_time_ms', v_execution_time
        );

    EXCEPTION WHEN OTHERS THEN
        -- Calculer le temps d'exécution même en cas d'erreur
        v_end_time := EXTRACT(EPOCH FROM clock_timestamp())::BIGINT * 1000;
        v_execution_time := (v_end_time - v_start_time)::INTEGER;

        -- Enregistrer l'échec de la migration
        INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms, error_message)
        VALUES (p_name, p_checksum, p_applied_by, FALSE, v_execution_time, SQLERRM)
        ON CONFLICT (name) DO UPDATE SET
            checksum = EXCLUDED.checksum,
            applied_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
            applied_by = EXCLUDED.applied_by,
            success = FALSE,
            execution_time_ms = EXCLUDED.execution_time_ms,
            error_message = EXCLUDED.error_message;

        v_result := jsonb_build_object(
            'success', FALSE,
            'already_applied', FALSE,
            'message', format('Migration %s failed: %s', p_name, SQLERRM),
            'execution_time_ms', v_execution_time,
            'error', SQLERRM
        );
    END;

    RETURN v_result;
END;
$$;

COMMENT ON FUNCTION apply_migration IS 'Applies a database migration and records the result. Uses SECURITY DEFINER to allow DDL operations.';

-- ============================================================================
-- 3. FONCTION POUR LISTER LES MIGRATIONS APPLIQUÉES
-- ============================================================================

CREATE OR REPLACE FUNCTION get_applied_migrations()
RETURNS TABLE (
    name TEXT,
    checksum TEXT,
    applied_at BIGINT,
    applied_by TEXT,
    success BOOLEAN,
    execution_time_ms INTEGER,
    error_message TEXT
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        sm.name,
        sm.checksum,
        sm.applied_at,
        sm.applied_by,
        sm.success,
        sm.execution_time_ms,
        sm.error_message
    FROM schema_migrations sm
    ORDER BY sm.name;
$$;

COMMENT ON FUNCTION get_applied_migrations IS 'Returns all migrations that have been applied to the database';

-- ============================================================================
-- 4. FONCTION POUR VÉRIFIER SI UNE MIGRATION A ÉTÉ APPLIQUÉE
-- ============================================================================

CREATE OR REPLACE FUNCTION is_migration_applied(p_name TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM schema_migrations
        WHERE name = p_name AND success = TRUE
    );
$$;

COMMENT ON FUNCTION is_migration_applied IS 'Checks if a specific migration has been successfully applied';

-- ============================================================================
-- 5. ENREGISTRER LES MIGRATIONS PASSÉES COMME DÉJÀ APPLIQUÉES
-- ============================================================================

-- Ces migrations ont été appliquées manuellement avant la mise en place
-- du système de migration automatique

INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms)
VALUES
    ('2025122601_uuid_migration', NULL, 'manual', TRUE, NULL),
    ('2025122602_created_updated_by', NULL, 'manual', TRUE, NULL),
    ('2025122603_audit_triggers', NULL, 'manual', TRUE, NULL),
    ('2025122604_audit_trigger_null_site', NULL, 'manual', TRUE, NULL),
    ('2025122605_add_product_description', NULL, 'manual', TRUE, NULL),
    ('2025122605_transaction_flat_view', NULL, 'manual', TRUE, NULL),
    ('2026010501_schema_cleanup', NULL, 'manual', TRUE, NULL),
    ('2026011701_migration_system', NULL, 'manual', TRUE, NULL)
ON CONFLICT (name) DO NOTHING;

COMMIT;

-- ============================================================================
-- FIN DE LA MIGRATION
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE 'Migration system installed successfully!';
    RAISE NOTICE 'Table schema_migrations created';
    RAISE NOTICE 'Functions: apply_migration(), get_applied_migrations(), is_migration_applied()';
    RAISE NOTICE '8 historical migrations registered as applied';
END $$;
