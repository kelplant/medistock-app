-- Migration: Add language column to app_users table
-- Date: 2026-01-25
-- Purpose: Store user's preferred language on their profile for sync across devices

-- Add language column (nullable for backward compatibility)
ALTER TABLE app_users ADD COLUMN language TEXT;

-- Update migration tracking
INSERT OR REPLACE INTO schema_migrations (version, name, applied_at)
VALUES ('2026012501', 'add_user_language', strftime('%s', 'now') * 1000);
