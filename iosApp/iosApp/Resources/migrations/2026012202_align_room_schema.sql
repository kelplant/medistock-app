-- ============================================================================
-- MIGRATION: Align SQLDelight schema with Room schema
-- Date: 2026-01-22
-- Description:
--   1. Update product_prices to match Room schema (effective_date, purchase_price, selling_price, source)
--   2. Update stock_movements to match Room schema (date, purchase_price_at_movement, selling_price_at_movement)
--   3. Update sale_batch_allocations to match Room schema (quantity_allocated, purchase_price_at_allocation, created_at, created_by)
-- ============================================================================

-- ============================================================================
-- 1. UPDATE PRODUCT_PRICES TABLE
-- ============================================================================
-- Room uses: effectiveDate, purchasePrice, sellingPrice, source
-- SQLDelight uses: site_id, price
-- We add the Room fields, keep old fields for backward compatibility

ALTER TABLE product_prices ADD COLUMN IF NOT EXISTS effective_date BIGINT;
ALTER TABLE product_prices ADD COLUMN IF NOT EXISTS purchase_price REAL;
ALTER TABLE product_prices ADD COLUMN IF NOT EXISTS selling_price REAL;
ALTER TABLE product_prices ADD COLUMN IF NOT EXISTS source TEXT DEFAULT 'manual';

-- Migrate existing data: use price as selling_price, 0 as purchase_price, created_at as effective_date
UPDATE product_prices
SET effective_date = created_at,
    selling_price = price,
    purchase_price = 0
WHERE effective_date IS NULL;

-- ============================================================================
-- 2. UPDATE STOCK_MOVEMENTS TABLE
-- ============================================================================
-- Room uses: type, date, purchasePriceAtMovement, sellingPriceAtMovement
-- SQLDelight uses: movement_type (same concept as type), no date/prices
-- We add the Room fields

ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS date BIGINT;
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS purchase_price_at_movement REAL DEFAULT 0;
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS selling_price_at_movement REAL DEFAULT 0;

-- Rename movement_type to type for consistency with Room
-- Note: PostgreSQL doesn't support RENAME COLUMN IF EXISTS, so we add 'type' and keep both
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS type TEXT;

-- Migrate existing data
UPDATE stock_movements
SET date = created_at,
    type = movement_type
WHERE date IS NULL OR type IS NULL;

-- ============================================================================
-- 3. UPDATE SALE_BATCH_ALLOCATIONS TABLE
-- ============================================================================
-- Room uses: quantityAllocated, purchasePriceAtAllocation, createdAt, createdBy
-- SQLDelight uses: quantity, unit_cost (no audit fields)
-- We add the Room fields

ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS quantity_allocated REAL;
ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS purchase_price_at_allocation REAL;
ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS created_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;
ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS created_by TEXT DEFAULT 'system';
ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Migrate existing data
UPDATE sale_batch_allocations
SET quantity_allocated = quantity,
    purchase_price_at_allocation = unit_cost
WHERE quantity_allocated IS NULL;

-- Create index for client_id on sale_batch_allocations
CREATE INDEX IF NOT EXISTS idx_sale_batch_allocations_client_id ON sale_batch_allocations(client_id);
