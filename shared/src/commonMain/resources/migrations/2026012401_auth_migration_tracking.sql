-- ============================================================================
-- MEDISTOCK: Auth Migration Tracking
-- Adds column to track which users have been migrated to Supabase Auth
-- ============================================================================

-- Add auth_migrated column to app_users table
-- 0 = not migrated (still using BCrypt only)
-- 1 = migrated to Supabase Auth
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS
    auth_migrated INTEGER DEFAULT 0;

-- Index for finding users not yet migrated
CREATE INDEX IF NOT EXISTS idx_app_users_auth_migrated
    ON app_users(auth_migrated)
    WHERE auth_migrated = 0;

-- Update existing users: mark as not migrated
-- They will be migrated on first login via migrate-user-to-auth Edge Function
UPDATE app_users SET auth_migrated = 0 WHERE auth_migrated IS NULL;
