-- Migration: Add language column to app_users table
-- Date: 2026-01-25
-- Purpose: Store user's preferred language on their profile for sync across devices

-- Add language column (nullable for backward compatibility)
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS language TEXT;

-- Comment for documentation
COMMENT ON COLUMN app_users.language IS 'User preferred language code (e.g., en, fr, de)';
