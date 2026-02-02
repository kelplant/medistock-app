-- Migration: 2026013002_sale_items_base_quantity
-- Description: Add base_quantity column to sale_items table
-- base_quantity stores the level 1 (base unit) equivalent of the display quantity.
-- When selling 2 boxes (level 2) of a product with conversionFactor=10,
-- quantity=2, base_quantity=20.
-- For level 1 sales, base_quantity is NULL (quantity already is the base unit).

ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS base_quantity DOUBLE PRECISION;

COMMENT ON COLUMN sale_items.base_quantity IS 'Level 1 (base unit) equivalent of the display quantity. NULL when selling at level 1.';

-- Update schema version
SELECT update_schema_version(31, 3, 'migration');
