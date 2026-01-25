-- ============================================================================
-- Migration: Create materialized current_stock table
-- Replaces computed stock queries with a maintained table for O(1) lookups
-- ============================================================================

-- ============================================================================
-- 1. CREATE CURRENT_STOCK TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS current_stock_table (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 0,
    last_movement_id UUID REFERENCES stock_movements(id) ON DELETE SET NULL,
    last_updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    client_id TEXT,
    UNIQUE(product_id, site_id)
);

CREATE INDEX idx_current_stock_product ON current_stock_table(product_id);
CREATE INDEX idx_current_stock_site ON current_stock_table(site_id);
CREATE INDEX idx_current_stock_quantity ON current_stock_table(quantity);
CREATE INDEX idx_current_stock_client_id ON current_stock_table(client_id);

COMMENT ON TABLE current_stock_table IS 'Materialized current stock per product per site. Updated by triggers on stock_movements.';

-- ============================================================================
-- 2. FUNCTION TO UPDATE CURRENT STOCK
-- ============================================================================

CREATE OR REPLACE FUNCTION update_current_stock()
RETURNS TRIGGER AS $$
DECLARE
    v_quantity_delta DOUBLE PRECISION;
    v_now BIGINT;
BEGIN
    v_now := EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;

    IF TG_OP = 'INSERT' THEN
        -- Calculate delta based on movement type
        IF NEW.type = 'IN' THEN
            v_quantity_delta := NEW.quantity;
        ELSE
            v_quantity_delta := -NEW.quantity;
        END IF;

        -- Upsert into current_stock_table
        INSERT INTO current_stock_table (product_id, site_id, quantity, last_movement_id, last_updated_at, client_id)
        VALUES (NEW.product_id, NEW.site_id, v_quantity_delta, NEW.id, v_now, NEW.client_id)
        ON CONFLICT (product_id, site_id) DO UPDATE SET
            quantity = current_stock_table.quantity + v_quantity_delta,
            last_movement_id = NEW.id,
            last_updated_at = v_now,
            client_id = COALESCE(NEW.client_id, current_stock_table.client_id);

        RETURN NEW;

    ELSIF TG_OP = 'DELETE' THEN
        -- Reverse the movement
        IF OLD.type = 'IN' THEN
            v_quantity_delta := -OLD.quantity;
        ELSE
            v_quantity_delta := OLD.quantity;
        END IF;

        UPDATE current_stock_table
        SET quantity = quantity + v_quantity_delta,
            last_updated_at = v_now
        WHERE product_id = OLD.product_id AND site_id = OLD.site_id;

        RETURN OLD;

    ELSIF TG_OP = 'UPDATE' THEN
        -- Reverse old movement
        IF OLD.type = 'IN' THEN
            v_quantity_delta := -OLD.quantity;
        ELSE
            v_quantity_delta := OLD.quantity;
        END IF;

        -- Apply new movement
        IF NEW.type = 'IN' THEN
            v_quantity_delta := v_quantity_delta + NEW.quantity;
        ELSE
            v_quantity_delta := v_quantity_delta - NEW.quantity;
        END IF;

        UPDATE current_stock_table
        SET quantity = quantity + v_quantity_delta,
            last_movement_id = NEW.id,
            last_updated_at = v_now
        WHERE product_id = NEW.product_id AND site_id = NEW.site_id;

        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 3. CREATE TRIGGER ON STOCK_MOVEMENTS
-- ============================================================================

DROP TRIGGER IF EXISTS trigger_update_current_stock ON stock_movements;

CREATE TRIGGER trigger_update_current_stock
    AFTER INSERT OR UPDATE OR DELETE ON stock_movements
    FOR EACH ROW
    EXECUTE FUNCTION update_current_stock();

-- ============================================================================
-- 4. INITIALIZE CURRENT_STOCK FROM EXISTING DATA
-- ============================================================================

-- Clear existing data (in case of re-run)
TRUNCATE current_stock_table;

-- Populate from stock_movements
INSERT INTO current_stock_table (product_id, site_id, quantity, last_updated_at)
SELECT
    sm.product_id,
    sm.site_id,
    SUM(CASE WHEN sm.type = 'IN' THEN sm.quantity ELSE -sm.quantity END) AS quantity,
    EXTRACT(EPOCH FROM NOW())::BIGINT * 1000 AS last_updated_at
FROM stock_movements sm
GROUP BY sm.product_id, sm.site_id
ON CONFLICT (product_id, site_id) DO UPDATE SET
    quantity = EXCLUDED.quantity,
    last_updated_at = EXCLUDED.last_updated_at;

-- Also ensure products without movements have 0 stock entries
INSERT INTO current_stock_table (product_id, site_id, quantity, last_updated_at)
SELECT p.id, p.site_id, 0, EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
FROM products p
WHERE NOT EXISTS (
    SELECT 1 FROM current_stock_table cs
    WHERE cs.product_id = p.id AND cs.site_id = p.site_id
)
ON CONFLICT (product_id, site_id) DO NOTHING;

-- ============================================================================
-- 5. UPDATE CURRENT_STOCK VIEW TO USE TABLE
-- ============================================================================

-- Drop and recreate the current_stock view to use the table
DROP VIEW IF EXISTS current_stock CASCADE;

CREATE OR REPLACE VIEW current_stock AS
SELECT
    p.id as product_id,
    p.name as product_name,
    p.description,
    cst.site_id,
    s.name as site_name,
    COALESCE(cst.quantity, 0) as current_stock,
    p.min_stock,
    p.max_stock,
    CASE
        WHEN COALESCE(cst.quantity, 0) <= p.min_stock THEN 'LOW'
        WHEN COALESCE(cst.quantity, 0) >= p.max_stock AND p.max_stock > 0 THEN 'HIGH'
        ELSE 'NORMAL'
    END as stock_status
FROM products p
LEFT JOIN current_stock_table cst ON p.id = cst.product_id
LEFT JOIN sites s ON cst.site_id = s.id
WHERE p.is_active = true;

-- ============================================================================
-- 6. HELPER FUNCTION FOR QUICK STOCK LOOKUP
-- ============================================================================

CREATE OR REPLACE FUNCTION get_current_stock(p_product_id UUID, p_site_id UUID)
RETURNS DOUBLE PRECISION AS $$
BEGIN
    RETURN COALESCE(
        (SELECT quantity FROM current_stock_table
         WHERE product_id = p_product_id AND site_id = p_site_id),
        0
    );
END;
$$ LANGUAGE plpgsql STABLE;

-- ============================================================================
-- 7. RLS POLICIES FOR CURRENT_STOCK_TABLE
-- ============================================================================

ALTER TABLE current_stock_table ENABLE ROW LEVEL SECURITY;

CREATE POLICY "current_stock_table_select" ON current_stock_table
    FOR SELECT TO authenticated USING (true);
CREATE POLICY "current_stock_table_insert" ON current_stock_table
    FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "current_stock_table_update" ON current_stock_table
    FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "current_stock_table_delete" ON current_stock_table
    FOR DELETE TO authenticated USING (true);

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
