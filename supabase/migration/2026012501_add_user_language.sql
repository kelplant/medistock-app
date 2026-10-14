-- Migration: Add language column to app_users table
-- Date: 2026-01-25
-- Purpose: Store user's preferred language on their profile for sync across devices

-- Add language column (nullable for backward compatibility)
-- Note: This will be skipped if column already exists (handled by MigrationManager)
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS language TEXT;
