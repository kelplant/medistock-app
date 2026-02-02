-- ============================================================================
-- Migration: 2026013001_rename_current_stock
-- Description: Rename current_stock_table to current_stock and remove the
--              now-redundant current_stock VIEW.
-- Schema version: 30
-- ============================================================================

-- 1. Drop the VIEW that depends on current_stock_table
--    (the VIEW was named "current_stock", which is the name we want for the table)
DROP VIEW IF EXISTS current_stock;

-- 2. Rename the table
ALTER TABLE current_stock_table RENAME TO current_stock;

-- 3. Rename RLS policies to match the new table name
ALTER POLICY "current_stock_table_select" ON current_stock RENAME TO "current_stock_select";
ALTER POLICY "current_stock_table_insert" ON current_stock RENAME TO "current_stock_insert";
ALTER POLICY "current_stock_table_update" ON current_stock RENAME TO "current_stock_update";
ALTER POLICY "current_stock_table_delete" ON current_stock RENAME TO "current_stock_delete";

-- 4. Recreate the trigger function to reference the new table name
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

        -- Upsert into current_stock
        INSERT INTO current_stock (product_id, site_id, quantity, last_movement_id, last_updated_at, client_id)
        VALUES (NEW.product_id, NEW.site_id, v_quantity_delta, NEW.id, v_now, NEW.client_id)
        ON CONFLICT (product_id, site_id) DO UPDATE SET
            quantity = current_stock.quantity + v_quantity_delta,
            last_movement_id = NEW.id,
            last_updated_at = v_now,
            client_id = COALESCE(NEW.client_id, current_stock.client_id);

        RETURN NEW;

    ELSIF TG_OP = 'DELETE' THEN
        -- Reverse the movement
        IF OLD.type = 'IN' THEN
            v_quantity_delta := -OLD.quantity;
        ELSE
            v_quantity_delta := OLD.quantity;
        END IF;

        UPDATE current_stock
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

        UPDATE current_stock
        SET quantity = quantity + v_quantity_delta,
            last_movement_id = NEW.id,
            last_updated_at = v_now
        WHERE product_id = NEW.product_id AND site_id = NEW.site_id;

        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 5. Recreate the helper function to reference the new table name
CREATE OR REPLACE FUNCTION get_current_stock(p_product_id UUID, p_site_id UUID)
RETURNS DOUBLE PRECISION AS $$
BEGIN
    RETURN COALESCE(
        (SELECT quantity FROM current_stock
         WHERE product_id = p_product_id AND site_id = p_site_id),
        0
    );
END;
$$ LANGUAGE plpgsql STABLE;

-- 6. Update table comment
COMMENT ON TABLE current_stock IS 'Materialized current stock per product per site. Updated by triggers on stock_movements.';

-- 7. Bump schema version
SELECT update_schema_version(30, 3, '2026013001_rename_current_stock');
