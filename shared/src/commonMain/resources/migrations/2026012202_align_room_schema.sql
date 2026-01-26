-- ============================================================================
-- MIGRATION: Align SQLDelight schema with Room schema
-- Date: 2026-01-22
-- Description: Add columns if they don't exist (idempotent)
-- Note: For Supabase, these columns already exist so this is a no-op
-- ============================================================================

-- 1. PRODUCT_PRICES - Add columns if not exists
ALTER TABLE product_prices ADD COLUMN IF NOT EXISTS effective_date BIGINT;
ALTER TABLE product_prices ADD COLUMN IF NOT EXISTS purchase_price REAL;
ALTER TABLE product_prices ADD COLUMN IF NOT EXISTS selling_price REAL;
ALTER TABLE product_prices ADD COLUMN IF NOT EXISTS source TEXT DEFAULT 'manual';

-- 2. STOCK_MOVEMENTS - Add columns if not exists
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS date BIGINT;
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS purchase_price_at_movement REAL DEFAULT 0;
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS selling_price_at_movement REAL DEFAULT 0;
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS type TEXT;

-- 3. SALE_BATCH_ALLOCATIONS - Add columns if not exists
ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS quantity_allocated REAL;
ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS purchase_price_at_allocation REAL;
ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS created_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;
ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS created_by TEXT DEFAULT 'system';
ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Create index if not exists
CREATE INDEX IF NOT EXISTS idx_sale_batch_allocations_client_id ON sale_batch_allocations(client_id);
