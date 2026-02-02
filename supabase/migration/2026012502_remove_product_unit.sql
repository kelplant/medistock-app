-- Migration: Remove unit column from products table
-- The unit is now derived from packaging_types based on selected_level
-- packaging_type_id becomes NOT NULL, selected_level defaults to 1

-- Step 1: Ensure all products have a packaging_type_id
UPDATE products
SET packaging_type_id = (SELECT id FROM packaging_types LIMIT 1)
WHERE packaging_type_id IS NULL;

-- Step 2: Ensure all products have a selected_level (default to 1)
UPDATE products
SET selected_level = 1
WHERE selected_level IS NULL;

-- Step 3: Make packaging_type_id NOT NULL
ALTER TABLE products ALTER COLUMN packaging_type_id SET NOT NULL;

-- Step 4: Make selected_level NOT NULL with default
ALTER TABLE products ALTER COLUMN selected_level SET NOT NULL;
ALTER TABLE products ALTER COLUMN selected_level SET DEFAULT 1;

-- Step 5: Drop the unit column (PostgreSQL supports this directly)
ALTER TABLE products DROP COLUMN IF EXISTS unit;

-- Step 6: Create index on packaging_type_id if not exists
CREATE INDEX IF NOT EXISTS idx_products_packaging ON products(packaging_type_id);

-- Step 7: Record migration
INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms)
VALUES ('2026012502_remove_product_unit', NULL, 'supabase_cli', TRUE, NULL)
ON CONFLICT (name) DO NOTHING;
