-- ============================================================================
-- MEDISTOCK SUPABASE UUID MIGRATION
-- Converts BIGSERIAL/BIGINT primary keys + foreign keys to UUIDs
-- ============================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- --------------------------------------------------------------------------
-- 1) PRIMARY TABLES: add UUID columns and backfill
-- --------------------------------------------------------------------------

ALTER TABLE sites ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE sites SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE categories ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE categories SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE packaging_types ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE packaging_types SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE app_users ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE app_users SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE customers ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE customers SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE products ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE products SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE product_prices ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE product_prices SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE purchase_batches ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE purchase_batches SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE stock_movements SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE inventories ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE inventories SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE product_transfers ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE product_transfers SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE sales ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE sales SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE sale_items SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE sale_batch_allocations SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE product_sales ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE product_sales SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE audit_history ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE audit_history SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

ALTER TABLE user_permissions ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE user_permissions SET id_uuid = uuid_generate_v4() WHERE id_uuid IS NULL;

-- --------------------------------------------------------------------------
-- 2) FOREIGN KEYS: add UUID FK columns + backfill using old IDs
-- --------------------------------------------------------------------------

ALTER TABLE user_permissions ADD COLUMN IF NOT EXISTS user_id_uuid UUID;
UPDATE user_permissions up
SET user_id_uuid = u.id_uuid
FROM app_users u
WHERE up.user_id = u.id;

ALTER TABLE customers ADD COLUMN IF NOT EXISTS site_id_uuid UUID;
UPDATE customers c
SET site_id_uuid = s.id_uuid
FROM sites s
WHERE c.site_id = s.id;

ALTER TABLE products ADD COLUMN IF NOT EXISTS packaging_type_id_uuid UUID;
UPDATE products p
SET packaging_type_id_uuid = pt.id_uuid
FROM packaging_types pt
WHERE p.packaging_type_id = pt.id;

ALTER TABLE products ADD COLUMN IF NOT EXISTS category_id_uuid UUID;
UPDATE products p
SET category_id_uuid = c.id_uuid
FROM categories c
WHERE p.category_id = c.id;

ALTER TABLE products ADD COLUMN IF NOT EXISTS site_id_uuid UUID;
UPDATE products p
SET site_id_uuid = s.id_uuid
FROM sites s
WHERE p.site_id = s.id;

ALTER TABLE product_prices ADD COLUMN IF NOT EXISTS product_id_uuid UUID;
UPDATE product_prices pp
SET product_id_uuid = p.id_uuid
FROM products p
WHERE pp.product_id = p.id;

ALTER TABLE purchase_batches ADD COLUMN IF NOT EXISTS product_id_uuid UUID;
UPDATE purchase_batches pb
SET product_id_uuid = p.id_uuid
FROM products p
WHERE pb.product_id = p.id;

ALTER TABLE purchase_batches ADD COLUMN IF NOT EXISTS site_id_uuid UUID;
UPDATE purchase_batches pb
SET site_id_uuid = s.id_uuid
FROM sites s
WHERE pb.site_id = s.id;

ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS product_id_uuid UUID;
UPDATE stock_movements sm
SET product_id_uuid = p.id_uuid
FROM products p
WHERE sm.product_id = p.id;

ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS site_id_uuid UUID;
UPDATE stock_movements sm
SET site_id_uuid = s.id_uuid
FROM sites s
WHERE sm.site_id = s.id;

ALTER TABLE inventories ADD COLUMN IF NOT EXISTS product_id_uuid UUID;
UPDATE inventories i
SET product_id_uuid = p.id_uuid
FROM products p
WHERE i.product_id = p.id;

ALTER TABLE inventories ADD COLUMN IF NOT EXISTS site_id_uuid UUID;
UPDATE inventories i
SET site_id_uuid = s.id_uuid
FROM sites s
WHERE i.site_id = s.id;

ALTER TABLE product_transfers ADD COLUMN IF NOT EXISTS product_id_uuid UUID;
UPDATE product_transfers pt
SET product_id_uuid = p.id_uuid
FROM products p
WHERE pt.product_id = p.id;

ALTER TABLE product_transfers ADD COLUMN IF NOT EXISTS from_site_id_uuid UUID;
UPDATE product_transfers pt
SET from_site_id_uuid = s.id_uuid
FROM sites s
WHERE pt.from_site_id = s.id;

ALTER TABLE product_transfers ADD COLUMN IF NOT EXISTS to_site_id_uuid UUID;
UPDATE product_transfers pt
SET to_site_id_uuid = s.id_uuid
FROM sites s
WHERE pt.to_site_id = s.id;

ALTER TABLE sales ADD COLUMN IF NOT EXISTS customer_id_uuid UUID;
UPDATE sales s
SET customer_id_uuid = c.id_uuid
FROM customers c
WHERE s.customer_id = c.id;

ALTER TABLE sales ADD COLUMN IF NOT EXISTS site_id_uuid UUID;
UPDATE sales s
SET site_id_uuid = st.id_uuid
FROM sites st
WHERE s.site_id = st.id;

ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS sale_id_uuid UUID;
UPDATE sale_items si
SET sale_id_uuid = s.id_uuid
FROM sales s
WHERE si.sale_id = s.id;

ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS product_id_uuid UUID;
UPDATE sale_items si
SET product_id_uuid = p.id_uuid
FROM products p
WHERE si.product_id = p.id;

ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS sale_item_id_uuid UUID;
UPDATE sale_batch_allocations sba
SET sale_item_id_uuid = si.id_uuid
FROM sale_items si
WHERE sba.sale_item_id = si.id;

ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS batch_id_uuid UUID;
UPDATE sale_batch_allocations sba
SET batch_id_uuid = pb.id_uuid
FROM purchase_batches pb
WHERE sba.batch_id = pb.id;

ALTER TABLE product_sales ADD COLUMN IF NOT EXISTS product_id_uuid UUID;
UPDATE product_sales ps
SET product_id_uuid = p.id_uuid
FROM products p
WHERE ps.product_id = p.id;

ALTER TABLE product_sales ADD COLUMN IF NOT EXISTS site_id_uuid UUID;
UPDATE product_sales ps
SET site_id_uuid = s.id_uuid
FROM sites s
WHERE ps.site_id = s.id;

ALTER TABLE audit_history ADD COLUMN IF NOT EXISTS entity_id_uuid UUID;
-- Map entity_id based on entity_type (adjust if you have custom entity types)
UPDATE audit_history ah
SET entity_id_uuid = p.id_uuid
FROM products p
WHERE ah.entity_type = 'Product' AND ah.entity_id = p.id;

UPDATE audit_history ah
SET entity_id_uuid = s.id_uuid
FROM sales s
WHERE ah.entity_type = 'Sale' AND ah.entity_id = s.id;

UPDATE audit_history ah
SET entity_id_uuid = pb.id_uuid
FROM purchase_batches pb
WHERE ah.entity_type = 'PurchaseBatch' AND ah.entity_id = pb.id;

UPDATE audit_history ah
SET entity_id_uuid = u.id_uuid
FROM app_users u
WHERE ah.entity_type = 'User' AND ah.entity_id = u.id;

ALTER TABLE audit_history ADD COLUMN IF NOT EXISTS site_id_uuid UUID;
UPDATE audit_history ah
SET site_id_uuid = s.id_uuid
FROM sites s
WHERE ah.site_id = s.id;

-- --------------------------------------------------------------------------
-- 3) DROP OLD CONSTRAINTS + COLUMNS, RENAME UUID COLUMNS
-- --------------------------------------------------------------------------

ALTER TABLE user_permissions DROP CONSTRAINT IF EXISTS user_permissions_user_id_fkey;
ALTER TABLE user_permissions DROP CONSTRAINT IF EXISTS user_permissions_pkey;
ALTER TABLE user_permissions DROP COLUMN user_id;
ALTER TABLE user_permissions DROP COLUMN id;
ALTER TABLE user_permissions RENAME COLUMN id_uuid TO id;
ALTER TABLE user_permissions RENAME COLUMN user_id_uuid TO user_id;
ALTER TABLE user_permissions ADD PRIMARY KEY (id);
ALTER TABLE user_permissions ADD CONSTRAINT user_permissions_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE;

ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_site_id_fkey;
ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_pkey;
ALTER TABLE customers DROP COLUMN site_id;
ALTER TABLE customers DROP COLUMN id;
ALTER TABLE customers RENAME COLUMN id_uuid TO id;
ALTER TABLE customers RENAME COLUMN site_id_uuid TO site_id;
ALTER TABLE customers ADD PRIMARY KEY (id);
ALTER TABLE customers ADD CONSTRAINT customers_site_id_fkey
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE RESTRICT;

ALTER TABLE products DROP CONSTRAINT IF EXISTS products_packaging_type_id_fkey;
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_category_id_fkey;
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_site_id_fkey;
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_pkey;
ALTER TABLE products DROP COLUMN packaging_type_id;
ALTER TABLE products DROP COLUMN category_id;
ALTER TABLE products DROP COLUMN site_id;
ALTER TABLE products DROP COLUMN id;
ALTER TABLE products RENAME COLUMN id_uuid TO id;
ALTER TABLE products RENAME COLUMN packaging_type_id_uuid TO packaging_type_id;
ALTER TABLE products RENAME COLUMN category_id_uuid TO category_id;
ALTER TABLE products RENAME COLUMN site_id_uuid TO site_id;
ALTER TABLE products ADD PRIMARY KEY (id);
ALTER TABLE products ADD CONSTRAINT products_packaging_type_id_fkey
    FOREIGN KEY (packaging_type_id) REFERENCES packaging_types(id) ON DELETE SET NULL;
ALTER TABLE products ADD CONSTRAINT products_category_id_fkey
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;
ALTER TABLE products ADD CONSTRAINT products_site_id_fkey
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE RESTRICT;

ALTER TABLE product_prices DROP CONSTRAINT IF EXISTS product_prices_product_id_fkey;
ALTER TABLE product_prices DROP CONSTRAINT IF EXISTS product_prices_pkey;
ALTER TABLE product_prices DROP COLUMN product_id;
ALTER TABLE product_prices DROP COLUMN id;
ALTER TABLE product_prices RENAME COLUMN id_uuid TO id;
ALTER TABLE product_prices RENAME COLUMN product_id_uuid TO product_id;
ALTER TABLE product_prices ADD PRIMARY KEY (id);
ALTER TABLE product_prices ADD CONSTRAINT product_prices_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

ALTER TABLE purchase_batches DROP CONSTRAINT IF EXISTS purchase_batches_product_id_fkey;
ALTER TABLE purchase_batches DROP CONSTRAINT IF EXISTS purchase_batches_site_id_fkey;
ALTER TABLE purchase_batches DROP CONSTRAINT IF EXISTS purchase_batches_pkey;
ALTER TABLE purchase_batches DROP COLUMN product_id;
ALTER TABLE purchase_batches DROP COLUMN site_id;
ALTER TABLE purchase_batches DROP COLUMN id;
ALTER TABLE purchase_batches RENAME COLUMN id_uuid TO id;
ALTER TABLE purchase_batches RENAME COLUMN product_id_uuid TO product_id;
ALTER TABLE purchase_batches RENAME COLUMN site_id_uuid TO site_id;
ALTER TABLE purchase_batches ADD PRIMARY KEY (id);
ALTER TABLE purchase_batches ADD CONSTRAINT purchase_batches_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT;
ALTER TABLE purchase_batches ADD CONSTRAINT purchase_batches_site_id_fkey
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE RESTRICT;

ALTER TABLE stock_movements DROP CONSTRAINT IF EXISTS stock_movements_product_id_fkey;
ALTER TABLE stock_movements DROP CONSTRAINT IF EXISTS stock_movements_site_id_fkey;
ALTER TABLE stock_movements DROP CONSTRAINT IF EXISTS stock_movements_pkey;
ALTER TABLE stock_movements DROP COLUMN product_id;
ALTER TABLE stock_movements DROP COLUMN site_id;
ALTER TABLE stock_movements DROP COLUMN id;
ALTER TABLE stock_movements RENAME COLUMN id_uuid TO id;
ALTER TABLE stock_movements RENAME COLUMN product_id_uuid TO product_id;
ALTER TABLE stock_movements RENAME COLUMN site_id_uuid TO site_id;
ALTER TABLE stock_movements ADD PRIMARY KEY (id);
ALTER TABLE stock_movements ADD CONSTRAINT stock_movements_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT;
ALTER TABLE stock_movements ADD CONSTRAINT stock_movements_site_id_fkey
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE RESTRICT;

ALTER TABLE inventories DROP CONSTRAINT IF EXISTS inventories_product_id_fkey;
ALTER TABLE inventories DROP CONSTRAINT IF EXISTS inventories_site_id_fkey;
ALTER TABLE inventories DROP CONSTRAINT IF EXISTS inventories_pkey;
ALTER TABLE inventories DROP COLUMN product_id;
ALTER TABLE inventories DROP COLUMN site_id;
ALTER TABLE inventories DROP COLUMN id;
ALTER TABLE inventories RENAME COLUMN id_uuid TO id;
ALTER TABLE inventories RENAME COLUMN product_id_uuid TO product_id;
ALTER TABLE inventories RENAME COLUMN site_id_uuid TO site_id;
ALTER TABLE inventories ADD PRIMARY KEY (id);
ALTER TABLE inventories ADD CONSTRAINT inventories_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT;
ALTER TABLE inventories ADD CONSTRAINT inventories_site_id_fkey
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE RESTRICT;

ALTER TABLE product_transfers DROP CONSTRAINT IF EXISTS product_transfers_product_id_fkey;
ALTER TABLE product_transfers DROP CONSTRAINT IF EXISTS product_transfers_from_site_id_fkey;
ALTER TABLE product_transfers DROP CONSTRAINT IF EXISTS product_transfers_to_site_id_fkey;
ALTER TABLE product_transfers DROP CONSTRAINT IF EXISTS product_transfers_pkey;
ALTER TABLE product_transfers DROP COLUMN product_id;
ALTER TABLE product_transfers DROP COLUMN from_site_id;
ALTER TABLE product_transfers DROP COLUMN to_site_id;
ALTER TABLE product_transfers DROP COLUMN id;
ALTER TABLE product_transfers RENAME COLUMN id_uuid TO id;
ALTER TABLE product_transfers RENAME COLUMN product_id_uuid TO product_id;
ALTER TABLE product_transfers RENAME COLUMN from_site_id_uuid TO from_site_id;
ALTER TABLE product_transfers RENAME COLUMN to_site_id_uuid TO to_site_id;
ALTER TABLE product_transfers ADD PRIMARY KEY (id);
ALTER TABLE product_transfers ADD CONSTRAINT product_transfers_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT;
ALTER TABLE product_transfers ADD CONSTRAINT product_transfers_from_site_id_fkey
    FOREIGN KEY (from_site_id) REFERENCES sites(id) ON DELETE RESTRICT;
ALTER TABLE product_transfers ADD CONSTRAINT product_transfers_to_site_id_fkey
    FOREIGN KEY (to_site_id) REFERENCES sites(id) ON DELETE RESTRICT;

ALTER TABLE sales DROP CONSTRAINT IF EXISTS sales_customer_id_fkey;
ALTER TABLE sales DROP CONSTRAINT IF EXISTS sales_site_id_fkey;
ALTER TABLE sales DROP CONSTRAINT IF EXISTS sales_pkey;
ALTER TABLE sales DROP COLUMN customer_id;
ALTER TABLE sales DROP COLUMN site_id;
ALTER TABLE sales DROP COLUMN id;
ALTER TABLE sales RENAME COLUMN id_uuid TO id;
ALTER TABLE sales RENAME COLUMN customer_id_uuid TO customer_id;
ALTER TABLE sales RENAME COLUMN site_id_uuid TO site_id;
ALTER TABLE sales ADD PRIMARY KEY (id);
ALTER TABLE sales ADD CONSTRAINT sales_customer_id_fkey
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;
ALTER TABLE sales ADD CONSTRAINT sales_site_id_fkey
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE RESTRICT;

ALTER TABLE sale_items DROP CONSTRAINT IF EXISTS sale_items_sale_id_fkey;
ALTER TABLE sale_items DROP CONSTRAINT IF EXISTS sale_items_product_id_fkey;
ALTER TABLE sale_items DROP CONSTRAINT IF EXISTS sale_items_pkey;
ALTER TABLE sale_items DROP COLUMN sale_id;
ALTER TABLE sale_items DROP COLUMN product_id;
ALTER TABLE sale_items DROP COLUMN id;
ALTER TABLE sale_items RENAME COLUMN id_uuid TO id;
ALTER TABLE sale_items RENAME COLUMN sale_id_uuid TO sale_id;
ALTER TABLE sale_items RENAME COLUMN product_id_uuid TO product_id;
ALTER TABLE sale_items ADD PRIMARY KEY (id);
ALTER TABLE sale_items ADD CONSTRAINT sale_items_sale_id_fkey
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE;
ALTER TABLE sale_items ADD CONSTRAINT sale_items_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT;

ALTER TABLE sale_batch_allocations DROP CONSTRAINT IF EXISTS sale_batch_allocations_sale_item_id_fkey;
ALTER TABLE sale_batch_allocations DROP CONSTRAINT IF EXISTS sale_batch_allocations_batch_id_fkey;
ALTER TABLE sale_batch_allocations DROP CONSTRAINT IF EXISTS sale_batch_allocations_pkey;
ALTER TABLE sale_batch_allocations DROP COLUMN sale_item_id;
ALTER TABLE sale_batch_allocations DROP COLUMN batch_id;
ALTER TABLE sale_batch_allocations DROP COLUMN id;
ALTER TABLE sale_batch_allocations RENAME COLUMN id_uuid TO id;
ALTER TABLE sale_batch_allocations RENAME COLUMN sale_item_id_uuid TO sale_item_id;
ALTER TABLE sale_batch_allocations RENAME COLUMN batch_id_uuid TO batch_id;
ALTER TABLE sale_batch_allocations ADD PRIMARY KEY (id);
ALTER TABLE sale_batch_allocations ADD CONSTRAINT sale_batch_allocations_sale_item_id_fkey
    FOREIGN KEY (sale_item_id) REFERENCES sale_items(id) ON DELETE CASCADE;
ALTER TABLE sale_batch_allocations ADD CONSTRAINT sale_batch_allocations_batch_id_fkey
    FOREIGN KEY (batch_id) REFERENCES purchase_batches(id) ON DELETE RESTRICT;

ALTER TABLE product_sales DROP CONSTRAINT IF EXISTS product_sales_product_id_fkey;
ALTER TABLE product_sales DROP CONSTRAINT IF EXISTS product_sales_site_id_fkey;
ALTER TABLE product_sales DROP CONSTRAINT IF EXISTS product_sales_pkey;
ALTER TABLE product_sales DROP COLUMN product_id;
ALTER TABLE product_sales DROP COLUMN site_id;
ALTER TABLE product_sales DROP COLUMN id;
ALTER TABLE product_sales RENAME COLUMN id_uuid TO id;
ALTER TABLE product_sales RENAME COLUMN product_id_uuid TO product_id;
ALTER TABLE product_sales RENAME COLUMN site_id_uuid TO site_id;
ALTER TABLE product_sales ADD PRIMARY KEY (id);
ALTER TABLE product_sales ADD CONSTRAINT product_sales_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT;
ALTER TABLE product_sales ADD CONSTRAINT product_sales_site_id_fkey
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE RESTRICT;

ALTER TABLE audit_history DROP CONSTRAINT IF EXISTS audit_history_pkey;
ALTER TABLE audit_history DROP COLUMN id;
ALTER TABLE audit_history DROP COLUMN entity_id;
ALTER TABLE audit_history DROP COLUMN site_id;
ALTER TABLE audit_history RENAME COLUMN id_uuid TO id;
ALTER TABLE audit_history RENAME COLUMN entity_id_uuid TO entity_id;
ALTER TABLE audit_history RENAME COLUMN site_id_uuid TO site_id;
ALTER TABLE audit_history ADD PRIMARY KEY (id);
ALTER TABLE audit_history ADD CONSTRAINT audit_history_site_id_fkey
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE SET NULL;

ALTER TABLE sites DROP CONSTRAINT IF EXISTS sites_pkey;
ALTER TABLE sites DROP COLUMN id;
ALTER TABLE sites RENAME COLUMN id_uuid TO id;
ALTER TABLE sites ADD PRIMARY KEY (id);

ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_pkey;
ALTER TABLE categories DROP COLUMN id;
ALTER TABLE categories RENAME COLUMN id_uuid TO id;
ALTER TABLE categories ADD PRIMARY KEY (id);

ALTER TABLE packaging_types DROP CONSTRAINT IF EXISTS packaging_types_pkey;
ALTER TABLE packaging_types DROP COLUMN id;
ALTER TABLE packaging_types RENAME COLUMN id_uuid TO id;
ALTER TABLE packaging_types ADD PRIMARY KEY (id);

ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_pkey;
ALTER TABLE app_users DROP COLUMN id;
ALTER TABLE app_users RENAME COLUMN id_uuid TO id;
ALTER TABLE app_users ADD PRIMARY KEY (id);

-- --------------------------------------------------------------------------
-- 4) DEFAULTS FOR UUID PKs
-- --------------------------------------------------------------------------

ALTER TABLE sites ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE categories ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE packaging_types ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE app_users ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE user_permissions ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE customers ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE products ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE product_prices ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE purchase_batches ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE stock_movements ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE inventories ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE product_transfers ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE sales ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE sale_items ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE sale_batch_allocations ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE product_sales ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE audit_history ALTER COLUMN id SET DEFAULT uuid_generate_v4();

COMMIT;

-- ============================================================================
-- END OF UUID MIGRATION
-- ============================================================================
