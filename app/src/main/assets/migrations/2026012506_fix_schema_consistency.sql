-- ============================================================================
-- Migration: Fix schema consistency between SQLDelight and PostgreSQL
-- 1. sale_items: rename price_per_unit -> unit_price, subtotal -> total_price
-- 2. stock_movements: add missing columns movement_type, reference_id, notes
-- ============================================================================

-- Note: SQLite ALTER TABLE RENAME COLUMN is supported since SQLite 3.25.0 (API 28+)
-- For older versions, a table recreation would be needed

-- For sale_items, the columns were already correctly named in SQLDelight
-- This migration is primarily for Supabase/PostgreSQL alignment
-- The local SQLite schema already uses unit_price/total_price

-- For stock_movements, the columns already exist in SQLDelight
-- This migration documents the PostgreSQL alignment

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
