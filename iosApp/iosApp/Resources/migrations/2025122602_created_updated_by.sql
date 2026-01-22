-- ============================================================================
-- Migration: set default created_by/updated_by to 'system' and backfill data
-- ============================================================================

BEGIN;

-- Backfill existing records
UPDATE sites SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';
UPDATE sites SET updated_by = 'system' WHERE updated_by IS NULL OR updated_by = '';

UPDATE categories SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';
UPDATE categories SET updated_by = 'system' WHERE updated_by IS NULL OR updated_by = '';

UPDATE packaging_types SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';
UPDATE packaging_types SET updated_by = 'system' WHERE updated_by IS NULL OR updated_by = '';

UPDATE app_users SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';
UPDATE app_users SET updated_by = 'system' WHERE updated_by IS NULL OR updated_by = '';

UPDATE user_permissions SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';
UPDATE user_permissions SET updated_by = 'system' WHERE updated_by IS NULL OR updated_by = '';

UPDATE customers SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';

UPDATE products SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';
UPDATE products SET updated_by = 'system' WHERE updated_by IS NULL OR updated_by = '';

UPDATE product_prices SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';
UPDATE product_prices SET updated_by = 'system' WHERE updated_by IS NULL OR updated_by = '';

UPDATE purchase_batches SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';
UPDATE purchase_batches SET updated_by = 'system' WHERE updated_by IS NULL OR updated_by = '';

UPDATE stock_movements SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';

UPDATE inventories SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';

UPDATE product_transfers SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';
UPDATE product_transfers SET updated_by = 'system' WHERE updated_by IS NULL OR updated_by = '';

UPDATE sales SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';

UPDATE product_sales SET created_by = 'system' WHERE created_by IS NULL OR created_by = '';

-- Set defaults
ALTER TABLE sites ALTER COLUMN created_by SET DEFAULT 'system';
ALTER TABLE sites ALTER COLUMN updated_by SET DEFAULT 'system';

ALTER TABLE categories ALTER COLUMN created_by SET DEFAULT 'system';
ALTER TABLE categories ALTER COLUMN updated_by SET DEFAULT 'system';

ALTER TABLE packaging_types ALTER COLUMN created_by SET DEFAULT 'system';
ALTER TABLE packaging_types ALTER COLUMN updated_by SET DEFAULT 'system';
ALTER TABLE packaging_types ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE packaging_types ALTER COLUMN updated_by SET NOT NULL;

ALTER TABLE app_users ALTER COLUMN created_by SET DEFAULT 'system';
ALTER TABLE app_users ALTER COLUMN updated_by SET DEFAULT 'system';

ALTER TABLE user_permissions ALTER COLUMN created_by SET DEFAULT 'system';
ALTER TABLE user_permissions ALTER COLUMN updated_by SET DEFAULT 'system';

ALTER TABLE customers ALTER COLUMN created_by SET DEFAULT 'system';

ALTER TABLE products ALTER COLUMN created_by SET DEFAULT 'system';
ALTER TABLE products ALTER COLUMN updated_by SET DEFAULT 'system';

ALTER TABLE product_prices ALTER COLUMN created_by SET DEFAULT 'system';
ALTER TABLE product_prices ALTER COLUMN updated_by SET DEFAULT 'system';

ALTER TABLE purchase_batches ALTER COLUMN created_by SET DEFAULT 'system';
ALTER TABLE purchase_batches ALTER COLUMN updated_by SET DEFAULT 'system';

ALTER TABLE stock_movements ALTER COLUMN created_by SET DEFAULT 'system';

ALTER TABLE inventories ALTER COLUMN created_by SET DEFAULT 'system';

ALTER TABLE product_transfers ALTER COLUMN created_by SET DEFAULT 'system';
ALTER TABLE product_transfers ALTER COLUMN updated_by SET DEFAULT 'system';

ALTER TABLE sales ALTER COLUMN created_by SET DEFAULT 'system';

ALTER TABLE product_sales ALTER COLUMN created_by SET DEFAULT 'system';

-- Functions and triggers
CREATE OR REPLACE FUNCTION set_audit_user_defaults()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.created_by IS NULL OR NEW.created_by = '' THEN
        NEW.created_by = 'system';
    END IF;

    IF TG_OP = 'INSERT' THEN
        IF NEW.updated_by IS NULL OR NEW.updated_by = '' THEN
            NEW.updated_by = NEW.created_by;
        END IF;
    ELSE
        IF NEW.updated_by IS NULL OR NEW.updated_by = '' THEN
            NEW.updated_by = 'system';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE OR REPLACE FUNCTION set_created_by_default()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.created_by IS NULL OR NEW.created_by = '' THEN
        NEW.created_by = 'system';
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS set_sites_audit_defaults ON sites;
CREATE TRIGGER set_sites_audit_defaults BEFORE INSERT OR UPDATE ON sites
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

DROP TRIGGER IF EXISTS set_categories_audit_defaults ON categories;
CREATE TRIGGER set_categories_audit_defaults BEFORE INSERT OR UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

DROP TRIGGER IF EXISTS set_packaging_types_audit_defaults ON packaging_types;
CREATE TRIGGER set_packaging_types_audit_defaults BEFORE INSERT OR UPDATE ON packaging_types
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

DROP TRIGGER IF EXISTS set_app_users_audit_defaults ON app_users;
CREATE TRIGGER set_app_users_audit_defaults BEFORE INSERT OR UPDATE ON app_users
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

DROP TRIGGER IF EXISTS set_user_permissions_audit_defaults ON user_permissions;
CREATE TRIGGER set_user_permissions_audit_defaults BEFORE INSERT OR UPDATE ON user_permissions
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

DROP TRIGGER IF EXISTS set_products_audit_defaults ON products;
CREATE TRIGGER set_products_audit_defaults BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

DROP TRIGGER IF EXISTS set_product_prices_audit_defaults ON product_prices;
CREATE TRIGGER set_product_prices_audit_defaults BEFORE INSERT OR UPDATE ON product_prices
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

DROP TRIGGER IF EXISTS set_purchase_batches_audit_defaults ON purchase_batches;
CREATE TRIGGER set_purchase_batches_audit_defaults BEFORE INSERT OR UPDATE ON purchase_batches
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

DROP TRIGGER IF EXISTS set_product_transfers_audit_defaults ON product_transfers;
CREATE TRIGGER set_product_transfers_audit_defaults BEFORE INSERT OR UPDATE ON product_transfers
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

DROP TRIGGER IF EXISTS set_customers_created_by ON customers;
CREATE TRIGGER set_customers_created_by BEFORE INSERT ON customers
    FOR EACH ROW EXECUTE FUNCTION set_created_by_default();

DROP TRIGGER IF EXISTS set_stock_movements_created_by ON stock_movements;
CREATE TRIGGER set_stock_movements_created_by BEFORE INSERT ON stock_movements
    FOR EACH ROW EXECUTE FUNCTION set_created_by_default();

DROP TRIGGER IF EXISTS set_inventories_created_by ON inventories;
CREATE TRIGGER set_inventories_created_by BEFORE INSERT ON inventories
    FOR EACH ROW EXECUTE FUNCTION set_created_by_default();

DROP TRIGGER IF EXISTS set_sales_created_by ON sales;
CREATE TRIGGER set_sales_created_by BEFORE INSERT ON sales
    FOR EACH ROW EXECUTE FUNCTION set_created_by_default();

DROP TRIGGER IF EXISTS set_product_sales_created_by ON product_sales;
CREATE TRIGGER set_product_sales_created_by BEFORE INSERT ON product_sales
    FOR EACH ROW EXECUTE FUNCTION set_created_by_default();

COMMIT;
