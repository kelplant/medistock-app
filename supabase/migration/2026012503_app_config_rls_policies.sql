-- ============================================================================
-- MEDISTOCK: App Config RLS Policies
-- Adds RLS policies for app_config table to allow read/write access
-- ============================================================================

-- Drop existing policies if any
DROP POLICY IF EXISTS "Allow read app_config" ON app_config;
DROP POLICY IF EXISTS "Allow update app_config" ON app_config;
DROP POLICY IF EXISTS "Allow insert app_config" ON app_config;

-- Policy to read configs (all users)
CREATE POLICY "Allow read app_config" ON app_config
    FOR SELECT USING (true);

-- Policy to update configs (all users - app handles authorization)
CREATE POLICY "Allow update app_config" ON app_config
    FOR UPDATE USING (true);

-- Policy to insert configs (all users - app handles authorization)
CREATE POLICY "Allow insert app_config" ON app_config
    FOR INSERT WITH CHECK (true);

-- Ensure currency_symbol exists with default value
INSERT INTO app_config (key, value, description, updated_at, updated_by)
VALUES ('currency_symbol', 'F', 'Currency symbol for prices display', EXTRACT(EPOCH FROM NOW()) * 1000, 'system')
ON CONFLICT (key) DO NOTHING;

-- ============================================================================
-- Verification
-- ============================================================================
DO $$
BEGIN
    RAISE NOTICE 'app_config RLS policies created';
    RAISE NOTICE 'currency_symbol default value ensured';
END $$;
