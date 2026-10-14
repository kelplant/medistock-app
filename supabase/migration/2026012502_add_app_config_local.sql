-- ============================================================================
-- MEDISTOCK: App Configuration Table (Local SQLite)
-- Stores global configuration including currency symbol
-- ============================================================================

-- Create app_config table for storing configuration
CREATE TABLE IF NOT EXISTS app_config (
    key TEXT NOT NULL PRIMARY KEY,
    value TEXT,
    description TEXT,
    updated_at INTEGER NOT NULL DEFAULT 0,
    updated_by TEXT NOT NULL DEFAULT ''
);

-- Insert default currency symbol
INSERT INTO app_config (key, value, description, updated_at, updated_by)
VALUES ('currency_symbol', 'F', 'Currency symbol for prices display', 0, 'system')
ON CONFLICT (key) DO NOTHING;
