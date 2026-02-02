-- Migration: Remove unit column from products table
-- The unit is now derived from packaging_types based on selected_level
-- packaging_type_id becomes NOT NULL, selected_level defaults to 1

-- Step 1: Ensure all products have a packaging_type_id
-- If they don't have one, assign the first available packaging type
UPDATE products
SET packaging_type_id = (SELECT id FROM packaging_types LIMIT 1)
WHERE packaging_type_id IS NULL;

-- Step 2: Ensure all products have a selected_level (default to 1)
UPDATE products
SET selected_level = 1
WHERE selected_level IS NULL;

-- Step 3: SQLite doesn't support DROP COLUMN directly in older versions
-- We need to recreate the table without the unit column

-- Create new table without unit column
CREATE TABLE products_new (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    unit_volume REAL NOT NULL,
    packaging_type_id TEXT NOT NULL,
    selected_level INTEGER NOT NULL DEFAULT 1,
    conversion_factor REAL,
    category_id TEXT,
    margin_type TEXT,
    margin_value REAL,
    description TEXT,
    site_id TEXT NOT NULL,
    min_stock REAL DEFAULT 0.0,
    max_stock REAL DEFAULT 0.0,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0,
    created_by TEXT NOT NULL DEFAULT '',
    updated_by TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (site_id) REFERENCES sites(id),
    FOREIGN KEY (packaging_type_id) REFERENCES packaging_types(id)
);

-- Copy data from old table (excluding unit column)
INSERT INTO products_new (id, name, unit_volume, packaging_type_id, selected_level, conversion_factor, category_id, margin_type, margin_value, description, site_id, min_stock, max_stock, is_active, created_at, updated_at, created_by, updated_by)
SELECT id, name, unit_volume, packaging_type_id, COALESCE(selected_level, 1), conversion_factor, category_id, margin_type, margin_value, description, site_id, min_stock, max_stock, is_active, created_at, updated_at, created_by, updated_by
FROM products;

-- Drop old table
DROP TABLE products;

-- Rename new table
ALTER TABLE products_new RENAME TO products;

-- Recreate indexes
CREATE INDEX idx_products_site ON products(site_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_packaging ON products(packaging_type_id);
