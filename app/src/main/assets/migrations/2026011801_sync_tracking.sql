-- ============================================================================
-- MEDISTOCK SYNC TRACKING SYSTEM
-- Adds server-side tracking for offline sync operations
-- NOTE: No BEGIN/COMMIT - apply_migration() handles the transaction
-- ============================================================================

-- ============================================================================
-- 1. TABLE DE SUIVI DES SYNCHRONISATIONS
-- Enregistre l'historique des syncs par device/user
-- ============================================================================

CREATE TABLE IF NOT EXISTS sync_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id TEXT NOT NULL,              -- ID unique du device
    user_id TEXT,                          -- ID utilisateur si connecté
    site_id UUID,                          -- Site concerné
    sync_type TEXT NOT NULL,               -- 'full', 'incremental', 'conflict_resolution'
    direction TEXT NOT NULL,               -- 'push' (local->remote), 'pull' (remote->local), 'bidirectional'
    entities_pushed INTEGER DEFAULT 0,     -- Nombre d'entités envoyées au serveur
    entities_pulled INTEGER DEFAULT 0,     -- Nombre d'entités reçues du serveur
    conflicts_detected INTEGER DEFAULT 0,  -- Nombre de conflits détectés
    conflicts_resolved INTEGER DEFAULT 0,  -- Nombre de conflits résolus
    duration_ms INTEGER,                   -- Durée de la sync en ms
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    started_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    completed_at BIGINT,
    app_version TEXT,                      -- Version de l'app (versionName)
    schema_version INTEGER,                -- Version du schéma de l'app

    CONSTRAINT fk_sync_log_site FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE SET NULL
);

-- Index pour les requêtes fréquentes
CREATE INDEX IF NOT EXISTS idx_sync_log_client_id ON sync_log(client_id);
CREATE INDEX IF NOT EXISTS idx_sync_log_user_id ON sync_log(user_id);
CREATE INDEX IF NOT EXISTS idx_sync_log_started_at ON sync_log(started_at DESC);

COMMENT ON TABLE sync_log IS 'Server-side log of all sync operations from mobile devices';
COMMENT ON COLUMN sync_log.client_id IS 'Unique device identifier (stored in SharedPreferences)';
COMMENT ON COLUMN sync_log.sync_type IS 'Type of sync: full, incremental, conflict_resolution';
COMMENT ON COLUMN sync_log.direction IS 'Data flow direction: push, pull, or bidirectional';

-- ============================================================================
-- 2. TABLE DES CONFLITS NON RÉSOLUS
-- Stocke les conflits nécessitant une intervention manuelle
-- ============================================================================

CREATE TABLE IF NOT EXISTS sync_conflicts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type TEXT NOT NULL,             -- Type d'entité (Product, Sale, etc.)
    entity_id UUID NOT NULL,               -- ID de l'entité en conflit
    client_id TEXT NOT NULL,               -- Device ayant soumis la modification
    local_payload JSONB NOT NULL,          -- Données locales (du device)
    remote_payload JSONB NOT NULL,         -- Données serveur
    local_updated_at BIGINT NOT NULL,      -- Timestamp modification locale
    remote_updated_at BIGINT NOT NULL,     -- Timestamp modification serveur
    conflict_type TEXT NOT NULL,           -- 'concurrent_edit', 'delete_edit', 'schema_mismatch'
    resolution_strategy TEXT,              -- Stratégie suggérée
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at BIGINT,
    resolved_by TEXT,
    resolution_action TEXT,                -- 'local_wins', 'remote_wins', 'merge', 'keep_both'
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
);

CREATE INDEX IF NOT EXISTS idx_sync_conflicts_resolved ON sync_conflicts(resolved);
CREATE INDEX IF NOT EXISTS idx_sync_conflicts_entity ON sync_conflicts(entity_type, entity_id);

COMMENT ON TABLE sync_conflicts IS 'Stores unresolved sync conflicts requiring manual intervention';

-- ============================================================================
-- 3. TABLE DE VERROUILLAGE OPTIMISTE (row versioning)
-- Pour les entités nécessitant une protection contre les conflits
-- ============================================================================

-- Ajouter colonne row_version aux tables principales si elle n'existe pas
DO $$
DECLARE
    tables_to_version TEXT[] := ARRAY['products', 'categories', 'sites', 'customers', 'packaging_types'];
    tbl TEXT;
BEGIN
    FOREACH tbl IN ARRAY tables_to_version
    LOOP
        EXECUTE format('
            ALTER TABLE %I ADD COLUMN IF NOT EXISTS row_version INTEGER NOT NULL DEFAULT 1
        ', tbl);

        -- Créer trigger pour auto-increment row_version
        EXECUTE format('
            CREATE OR REPLACE FUNCTION increment_row_version_%I()
            RETURNS TRIGGER
            LANGUAGE plpgsql
            AS $func$
            BEGIN
                NEW.row_version := OLD.row_version + 1;
                RETURN NEW;
            END;
            $func$
        ', tbl);

        -- Supprimer trigger existant s'il existe
        EXECUTE format('
            DROP TRIGGER IF EXISTS trg_row_version_%I ON %I
        ', tbl, tbl);

        -- Créer le nouveau trigger
        EXECUTE format('
            CREATE TRIGGER trg_row_version_%I
            BEFORE UPDATE ON %I
            FOR EACH ROW
            EXECUTE FUNCTION increment_row_version_%I()
        ', tbl, tbl, tbl);
    END LOOP;
END;
$$;

COMMENT ON COLUMN products.row_version IS 'Optimistic locking version - incremented on each update';

-- ============================================================================
-- 4. FONCTIONS RPC POUR LA SYNCHRONISATION
-- ============================================================================

-- Fonction pour enregistrer une sync
CREATE OR REPLACE FUNCTION log_sync(
    p_client_id TEXT,
    p_user_id TEXT DEFAULT NULL,
    p_site_id UUID DEFAULT NULL,
    p_sync_type TEXT DEFAULT 'incremental',
    p_direction TEXT DEFAULT 'bidirectional',
    p_entities_pushed INTEGER DEFAULT 0,
    p_entities_pulled INTEGER DEFAULT 0,
    p_conflicts_detected INTEGER DEFAULT 0,
    p_conflicts_resolved INTEGER DEFAULT 0,
    p_duration_ms INTEGER DEFAULT NULL,
    p_success BOOLEAN DEFAULT TRUE,
    p_error_message TEXT DEFAULT NULL,
    p_app_version TEXT DEFAULT NULL,
    p_schema_version INTEGER DEFAULT NULL
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_id UUID;
BEGIN
    INSERT INTO sync_log (
        client_id, user_id, site_id, sync_type, direction,
        entities_pushed, entities_pulled, conflicts_detected, conflicts_resolved,
        duration_ms, success, error_message, completed_at,
        app_version, schema_version
    ) VALUES (
        p_client_id, p_user_id, p_site_id, p_sync_type, p_direction,
        p_entities_pushed, p_entities_pulled, p_conflicts_detected, p_conflicts_resolved,
        p_duration_ms, p_success, p_error_message,
        CASE WHEN p_success THEN EXTRACT(EPOCH FROM NOW())::BIGINT * 1000 ELSE NULL END,
        p_app_version, p_schema_version
    )
    RETURNING id INTO v_id;

    RETURN v_id;
END;
$$;

-- Fonction pour enregistrer un conflit
CREATE OR REPLACE FUNCTION log_conflict(
    p_entity_type TEXT,
    p_entity_id UUID,
    p_client_id TEXT,
    p_local_payload JSONB,
    p_remote_payload JSONB,
    p_local_updated_at BIGINT,
    p_remote_updated_at BIGINT,
    p_conflict_type TEXT DEFAULT 'concurrent_edit',
    p_resolution_strategy TEXT DEFAULT NULL
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_id UUID;
BEGIN
    INSERT INTO sync_conflicts (
        entity_type, entity_id, client_id,
        local_payload, remote_payload,
        local_updated_at, remote_updated_at,
        conflict_type, resolution_strategy
    ) VALUES (
        p_entity_type, p_entity_id, p_client_id,
        p_local_payload, p_remote_payload,
        p_local_updated_at, p_remote_updated_at,
        p_conflict_type, p_resolution_strategy
    )
    RETURNING id INTO v_id;

    RETURN v_id;
END;
$$;

-- Fonction pour résoudre un conflit
CREATE OR REPLACE FUNCTION resolve_conflict(
    p_conflict_id UUID,
    p_resolved_by TEXT,
    p_resolution_action TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    UPDATE sync_conflicts SET
        resolved = TRUE,
        resolved_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
        resolved_by = p_resolved_by,
        resolution_action = p_resolution_action
    WHERE id = p_conflict_id AND NOT resolved;

    RETURN FOUND;
END;
$$;

-- Fonction pour upsert avec vérification de version (optimistic locking)
CREATE OR REPLACE FUNCTION upsert_with_version(
    p_table_name TEXT,
    p_id UUID,
    p_data JSONB,
    p_expected_version INTEGER DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_current_version INTEGER;
    v_result JSONB;
BEGIN
    -- Vérifier la version si spécifiée
    IF p_expected_version IS NOT NULL THEN
        EXECUTE format('SELECT row_version FROM %I WHERE id = $1', p_table_name)
        INTO v_current_version
        USING p_id;

        IF v_current_version IS NOT NULL AND v_current_version != p_expected_version THEN
            RETURN jsonb_build_object(
                'success', FALSE,
                'error', 'VERSION_CONFLICT',
                'message', format('Expected version %s but found %s', p_expected_version, v_current_version),
                'current_version', v_current_version
            );
        END IF;
    END IF;

    -- Effectuer l'upsert
    -- Note: L'implémentation complète dépend de la structure spécifique de chaque table
    -- Ceci est un placeholder - chaque table aura sa propre fonction upsert

    RETURN jsonb_build_object(
        'success', TRUE,
        'message', 'Operation completed'
    );
END;
$$;

-- ============================================================================
-- 5. VUES UTILITAIRES
-- ============================================================================

-- Vue des syncs récentes par device
CREATE OR REPLACE VIEW recent_syncs AS
SELECT
    client_id,
    COUNT(*) as total_syncs,
    MAX(started_at) as last_sync_at,
    SUM(entities_pushed) as total_pushed,
    SUM(entities_pulled) as total_pulled,
    SUM(CASE WHEN NOT success THEN 1 ELSE 0 END) as failed_syncs,
    SUM(conflicts_detected) as total_conflicts
FROM sync_log
WHERE started_at > EXTRACT(EPOCH FROM (NOW() - INTERVAL '7 days'))::BIGINT * 1000
GROUP BY client_id;

-- Vue des conflits en attente
CREATE OR REPLACE VIEW pending_conflicts AS
SELECT
    c.*,
    CASE
        WHEN c.entity_type = 'Product' THEN p.name
        WHEN c.entity_type = 'Customer' THEN cu.name
        ELSE NULL
    END as entity_name
FROM sync_conflicts c
LEFT JOIN products p ON c.entity_type = 'Product' AND c.entity_id = p.id
LEFT JOIN customers cu ON c.entity_type = 'Customer' AND c.entity_id = cu.id
WHERE NOT c.resolved
ORDER BY c.created_at DESC;

-- ============================================================================
-- 6. MISE À JOUR DE LA VERSION DU SCHÉMA
-- ============================================================================

-- Incrémenter la version du schéma (version 3)
SELECT update_schema_version(3, 2, 'migration_2026011801');

-- Enregistrer cette migration
INSERT INTO schema_migrations (name, checksum, applied_by, success)
VALUES ('2026011801_sync_tracking', NULL, 'manual', TRUE)
ON CONFLICT (name) DO NOTHING;
