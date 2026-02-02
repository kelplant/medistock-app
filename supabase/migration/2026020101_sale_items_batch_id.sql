-- Migration: Add batch_id column to sale_items
-- This allows users to explicitly select which batch a sale item is sold from,
-- enabling smart batch suggestion (expiring-first, then FIFO) in the UI.

-- Add batch_id column (nullable FK to purchase_batches)
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS batch_id UUID REFERENCES purchase_batches(id) ON DELETE SET NULL;

-- Index for batch lookups
CREATE INDEX IF NOT EXISTS idx_sale_items_batch ON sale_items(batch_id);

-- Update schema version
SELECT update_schema_version(32, 3, 'migration');
