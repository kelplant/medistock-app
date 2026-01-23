-- ============================================================================
-- MEDISTOCK: App Configuration Table
-- Stores global configuration including recovery secret key
-- ============================================================================

-- Create app_config table for storing configuration
CREATE TABLE IF NOT EXISTS app_config (
    key TEXT PRIMARY KEY,
    value TEXT,
    description TEXT,
    updated_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_by TEXT
);

COMMENT ON TABLE app_config IS 'Global application configuration. Only accessible via service_role.';
COMMENT ON COLUMN app_config.key IS 'Configuration key (e.g., recovery_secret_key)';
COMMENT ON COLUMN app_config.value IS 'Configuration value';
COMMENT ON COLUMN app_config.description IS 'Human-readable description of the configuration';

-- Enable RLS with NO policies = only service_role can access
ALTER TABLE app_config ENABLE ROW LEVEL SECURITY;

-- Insert default configuration entries
INSERT INTO app_config (key, value, description)
VALUES
    ('recovery_secret_key', NULL, 'Secret key required to create recovery admin. Set via SQL Editor if needed.'),
    ('instance_name', 'MediStock', 'Name of this MediStock instance'),
    ('setup_completed_at', NULL, 'Timestamp when initial setup was completed')
ON CONFLICT (key) DO NOTHING;

-- ============================================================================
-- Verification
-- ============================================================================
DO $$
BEGIN
    RAISE NOTICE 'app_config table created';
    RAISE NOTICE 'RLS enabled with no policies = service_role only access';
END $$;
