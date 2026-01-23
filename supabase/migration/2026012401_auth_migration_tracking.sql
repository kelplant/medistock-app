-- ============================================================================
-- MEDISTOCK: Auth Migration Tracking
-- Adds column to track which users have been migrated to Supabase Auth
-- ============================================================================

-- Add auth_migrated column to users table
-- 0 = not migrated (still using BCrypt only)
-- 1 = migrated to Supabase Auth
ALTER TABLE users ADD COLUMN IF NOT EXISTS
    auth_migrated INTEGER DEFAULT 0;

-- Index for finding users not yet migrated
CREATE INDEX IF NOT EXISTS idx_users_auth_migrated
    ON users(auth_migrated)
    WHERE auth_migrated = 0;

-- Update existing users: mark as not migrated
-- They will be migrated on first login via migrate-user-to-auth Edge Function
UPDATE users SET auth_migrated = 0 WHERE auth_migrated IS NULL;

-- ============================================================================
-- Verification
-- ============================================================================
DO $$
BEGIN
    RAISE NOTICE 'Auth migration tracking column added to users table';
    RAISE NOTICE 'Existing users will be migrated on first login';
END $$;
