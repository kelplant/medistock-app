-- ============================================================================
-- Migration: Create materialized current_stock table (PostgreSQL version)
-- Replaces computed stock queries with a maintained table for O(1) lookups
-- ============================================================================

-- Create current_stock
CREATE TABLE IF NOT EXISTS current_stock (
    id TEXT PRIMARY KEY,
    product_id TEXT NOT NULL,
    site_id TEXT NOT NULL,
    quantity REAL NOT NULL DEFAULT 0,
    last_movement_id TEXT,
    last_updated_at BIGINT NOT NULL DEFAULT 0,
    client_id TEXT,
    UNIQUE(product_id, site_id)
);

CREATE INDEX IF NOT EXISTS idx_current_stock_product ON current_stock(product_id);
CREATE INDEX IF NOT EXISTS idx_current_stock_site ON current_stock(site_id);
CREATE INDEX IF NOT EXISTS idx_current_stock_quantity ON current_stock(quantity);
CREATE INDEX IF NOT EXISTS idx_current_stock_client_id ON current_stock(client_id);

-- Initialize current_stock from existing stock_movements
INSERT INTO current_stock (id, product_id, site_id, quantity, last_updated_at)
SELECT
    product_id || '_' || site_id AS id,
    product_id,
    site_id,
    SUM(CASE
        WHEN type IN ('IN', 'PURCHASE', 'ADJUSTMENT_IN', 'TRANSFER_IN', 'INVENTORY_ADJUSTMENT') THEN quantity
        ELSE -ABS(quantity)
    END) AS quantity,
    EXTRACT(EPOCH FROM NOW())::bigint * 1000 AS last_updated_at
FROM stock_movements
GROUP BY product_id, site_id
ON CONFLICT (product_id, site_id) DO UPDATE SET
    quantity = EXCLUDED.quantity,
    last_updated_at = EXCLUDED.last_updated_at;

-- Also ensure products without movements have 0 stock entries
INSERT INTO current_stock (id, product_id, site_id, quantity, last_updated_at)
SELECT
    p.id::text || '_' || p.site_id::text AS id,
    p.id::text AS product_id,
    p.site_id::text,
    0 AS quantity,
    EXTRACT(EPOCH FROM NOW())::bigint * 1000 AS last_updated_at
FROM products p
WHERE NOT EXISTS (
    SELECT 1 FROM current_stock cs
    WHERE cs.product_id = p.id::text AND cs.site_id = p.site_id::text
)
ON CONFLICT (product_id, site_id) DO NOTHING;
