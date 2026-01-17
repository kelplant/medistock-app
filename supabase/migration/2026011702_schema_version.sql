-- ============================================================================
-- MEDISTOCK SCHEMA VERSION TRACKING
-- Adds version tracking for app/database compatibility checking
-- ============================================================================

BEGIN;

-- ============================================================================
-- 1. TABLE DE VERSION DU SCHÉMA
-- ============================================================================

CREATE TABLE IF NOT EXISTS schema_version (
    id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1), -- Force une seule ligne
    schema_version INTEGER NOT NULL DEFAULT 1,
    min_app_version INTEGER NOT NULL DEFAULT 1,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_by TEXT NOT NULL DEFAULT 'system'
);

COMMENT ON TABLE schema_version IS 'Single-row table tracking database schema version and minimum required app version';
COMMENT ON COLUMN schema_version.schema_version IS 'Current database schema version (incremented with breaking changes)';
COMMENT ON COLUMN schema_version.min_app_version IS 'Minimum app schema version required to use this database';

-- ============================================================================
-- 2. FONCTION POUR RÉCUPÉRER LA VERSION
-- ============================================================================

CREATE OR REPLACE FUNCTION get_schema_version()
RETURNS TABLE (
    schema_version INTEGER,
    min_app_version INTEGER,
    updated_at BIGINT
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        sv.schema_version,
        sv.min_app_version,
        sv.updated_at
    FROM schema_version sv
    WHERE sv.id = 1;
$$;

COMMENT ON FUNCTION get_schema_version IS 'Returns current schema version and minimum required app version';

-- ============================================================================
-- 3. FONCTION POUR METTRE À JOUR LA VERSION (utilisée par les migrations)
-- ============================================================================

CREATE OR REPLACE FUNCTION update_schema_version(
    p_schema_version INTEGER,
    p_min_app_version INTEGER DEFAULT NULL,
    p_updated_by TEXT DEFAULT 'migration'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_current_schema INTEGER;
    v_current_min_app INTEGER;
BEGIN
    -- Récupérer les versions actuelles
    SELECT sv.schema_version, sv.min_app_version
    INTO v_current_schema, v_current_min_app
    FROM schema_version sv WHERE sv.id = 1;

    -- Si pas de ligne, insérer
    IF NOT FOUND THEN
        INSERT INTO schema_version (schema_version, min_app_version, updated_by)
        VALUES (p_schema_version, COALESCE(p_min_app_version, p_schema_version), p_updated_by);

        RETURN jsonb_build_object(
            'success', TRUE,
            'message', 'Schema version initialized',
            'schema_version', p_schema_version,
            'min_app_version', COALESCE(p_min_app_version, p_schema_version)
        );
    END IF;

    -- Vérifier qu'on n'essaie pas de régresser
    IF p_schema_version < v_current_schema THEN
        RETURN jsonb_build_object(
            'success', FALSE,
            'message', format('Cannot downgrade schema version from %s to %s', v_current_schema, p_schema_version)
        );
    END IF;

    -- Mettre à jour
    UPDATE schema_version SET
        schema_version = p_schema_version,
        min_app_version = COALESCE(p_min_app_version, min_app_version),
        updated_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
        updated_by = p_updated_by
    WHERE id = 1;

    RETURN jsonb_build_object(
        'success', TRUE,
        'message', 'Schema version updated',
        'schema_version', p_schema_version,
        'min_app_version', COALESCE(p_min_app_version, v_current_min_app)
    );
END;
$$;

COMMENT ON FUNCTION update_schema_version IS 'Updates the schema version. Used by migrations to bump version numbers.';

-- ============================================================================
-- 4. INITIALISER LA VERSION
-- ============================================================================

-- Insérer la version initiale (version 2 car c'est la 2ème migration du système)
INSERT INTO schema_version (schema_version, min_app_version, updated_by)
VALUES (2, 2, 'migration_2026011702')
ON CONFLICT (id) DO UPDATE SET
    schema_version = GREATEST(schema_version.schema_version, 2),
    min_app_version = GREATEST(schema_version.min_app_version, 2),
    updated_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;

-- Enregistrer cette migration
INSERT INTO schema_migrations (name, checksum, applied_by, success)
VALUES ('2026011702_schema_version', NULL, 'manual', TRUE)
ON CONFLICT (name) DO NOTHING;

COMMIT;

-- ============================================================================
-- FIN DE LA MIGRATION
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE 'Schema version tracking installed!';
    RAISE NOTICE 'Current schema_version: 2';
    RAISE NOTICE 'Minimum app version required: 2';
END $$;
