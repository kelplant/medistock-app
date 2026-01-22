-- ============================================================================
-- MIGRATION: Inventory Items Table and Schema Updates
-- Date: 2026-01-22
-- Description:
--   1. Add inventory_items table for product-level inventory counts
--   2. Add missing columns to sale_items (product_name, unit)
--   3. Add missing columns to packaging_types (default_conversion_factor, is_active, display_order)
-- ============================================================================

-- ============================================================================
-- 1. CREATE INVENTORY_ITEMS TABLE
-- ============================================================================
-- This table stores individual product counts during inventory sessions.
-- It links to the inventories table (inventory sessions) and tracks
-- counted vs theoretical quantities with discrepancy tracking.

CREATE TABLE IF NOT EXISTS inventory_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    inventory_id UUID REFERENCES inventories(id) ON DELETE SET NULL,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    count_date BIGINT NOT NULL,
    counted_quantity REAL NOT NULL,
    theoretical_quantity REAL NOT NULL,
    discrepancy REAL NOT NULL DEFAULT 0.0,
    reason TEXT NOT NULL DEFAULT '',
    counted_by TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

-- Create indexes for inventory_items
CREATE INDEX IF NOT EXISTS idx_inventory_items_site ON inventory_items(site_id);
CREATE INDEX IF NOT EXISTS idx_inventory_items_product ON inventory_items(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_items_date ON inventory_items(count_date);
CREATE INDEX IF NOT EXISTS idx_inventory_items_inventory ON inventory_items(inventory_id);
CREATE INDEX IF NOT EXISTS idx_inventory_items_client_id ON inventory_items(client_id);

-- ============================================================================
-- 2. ADD MISSING COLUMNS TO SALE_ITEMS
-- ============================================================================
-- Adding product_name and unit for denormalization (display purposes)

ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS product_name TEXT NOT NULL DEFAULT '';
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS unit TEXT NOT NULL DEFAULT '';

-- ============================================================================
-- 3. ADD MISSING COLUMNS TO PACKAGING_TYPES
-- ============================================================================

ALTER TABLE packaging_types ADD COLUMN IF NOT EXISTS default_conversion_factor REAL;
ALTER TABLE packaging_types ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE packaging_types ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0;
