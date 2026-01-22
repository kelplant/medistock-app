-- Migration: Add is_active column for soft delete (referential integrity)
-- Date: 2026-01-23
-- Phase 11: Referential Integrity
-- Note: This migration adds is_active columns for soft delete support.
-- The migration system tracks applied migrations, so each statement runs only once.

-- Add is_active column to sites table (default 1 = active)
ALTER TABLE sites ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1;

-- Add is_active column to categories table (default 1 = active)
ALTER TABLE categories ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1;

-- Add is_active column to products table (default 1 = active)
ALTER TABLE products ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1;

-- Add is_active column to customers table (default 1 = active)
ALTER TABLE customers ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1;

-- Note: packaging_types and app_users already have is_active column
