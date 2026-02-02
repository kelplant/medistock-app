-- ============================================================================
-- Migration: Fix inventories schema to match SQLDelight (header/detail structure)
-- ============================================================================

-- The inventories table was structured as a flat table (one row per product count)
-- but should be a session header table with inventory_items as detail lines.

-- 1. Backup existing data from inventories to inventory_items if not already there
INSERT INTO inventory_items (id, inventory_id, product_id, site_id, count_date, counted_quantity, theoretical_quantity, discrepancy, reason, counted_by, notes, created_at, created_by, client_id)
SELECT
    gen_random_uuid(),
    NULL, -- No session header yet
    i.product_id,
    i.site_id,
    i.count_date,
    i.counted_quantity,
    i.theoretical_quantity,
    i.discrepancy,
    i.reason,
    i.counted_by,
    i.notes,
    i.created_at,
    i.created_by,
    i.client_id
FROM inventories i
WHERE i.product_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM inventory_items ii
    WHERE ii.product_id = i.product_id
    AND ii.site_id = i.site_id
    AND ii.count_date = i.count_date
);

-- 2. Drop and recreate inventories as session header table
DROP TABLE IF EXISTS inventories CASCADE;

CREATE TABLE inventories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    status TEXT NOT NULL DEFAULT 'in_progress',
    started_at BIGINT NOT NULL,
    completed_at BIGINT,
    notes TEXT,
    created_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_inventories_site ON inventories(site_id);
CREATE INDEX idx_inventories_status ON inventories(status);
CREATE INDEX idx_inventories_started_at ON inventories(started_at);
CREATE INDEX idx_inventories_client_id ON inventories(client_id);

-- 3. Re-enable RLS
ALTER TABLE inventories ENABLE ROW LEVEL SECURITY;

CREATE POLICY "inventories_select" ON inventories FOR SELECT TO authenticated USING (true);
CREATE POLICY "inventories_insert" ON inventories FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "inventories_update" ON inventories FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "inventories_delete" ON inventories FOR DELETE TO authenticated USING (true);

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
