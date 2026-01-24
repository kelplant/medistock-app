-- ============================================================================
-- MEDISTOCK: Fix RLS policies to allow sync for all authenticated users
--
-- Problem: Previous policies restricted SELECT based on is_admin() which
-- prevented normal users from syncing data.
--
-- Solution: All authenticated users can READ all data (sync needs this).
-- Permissions are enforced at the app/UI level, not at the database level.
-- ============================================================================

-- Drop restrictive SELECT policies and replace with permissive ones

-- SITES
DROP POLICY IF EXISTS "sites_select" ON sites;
CREATE POLICY "sites_select" ON sites FOR SELECT TO authenticated USING (true);

-- CATEGORIES
DROP POLICY IF EXISTS "categories_select" ON categories;
CREATE POLICY "categories_select" ON categories FOR SELECT TO authenticated USING (true);

-- PACKAGING_TYPES
DROP POLICY IF EXISTS "packaging_types_select" ON packaging_types;
CREATE POLICY "packaging_types_select" ON packaging_types FOR SELECT TO authenticated USING (true);

-- APP_USERS
DROP POLICY IF EXISTS "users_select_policy" ON app_users;
CREATE POLICY "users_select_policy" ON app_users FOR SELECT TO authenticated USING (true);

-- USER_PERMISSIONS
DROP POLICY IF EXISTS "user_permissions_select" ON user_permissions;
CREATE POLICY "user_permissions_select" ON user_permissions FOR SELECT TO authenticated USING (true);

-- CUSTOMERS
DROP POLICY IF EXISTS "customers_select" ON customers;
CREATE POLICY "customers_select" ON customers FOR SELECT TO authenticated USING (true);

-- PRODUCTS
DROP POLICY IF EXISTS "products_select" ON products;
CREATE POLICY "products_select" ON products FOR SELECT TO authenticated USING (true);

-- PRODUCT_PRICES
DROP POLICY IF EXISTS "product_prices_select" ON product_prices;
CREATE POLICY "product_prices_select" ON product_prices FOR SELECT TO authenticated USING (true);

-- PURCHASE_BATCHES
DROP POLICY IF EXISTS "purchase_batches_select" ON purchase_batches;
CREATE POLICY "purchase_batches_select" ON purchase_batches FOR SELECT TO authenticated USING (true);

-- STOCK_MOVEMENTS
DROP POLICY IF EXISTS "stock_movements_select" ON stock_movements;
CREATE POLICY "stock_movements_select" ON stock_movements FOR SELECT TO authenticated USING (true);

-- INVENTORIES
DROP POLICY IF EXISTS "inventories_select" ON inventories;
CREATE POLICY "inventories_select" ON inventories FOR SELECT TO authenticated USING (true);

-- PRODUCT_TRANSFERS
DROP POLICY IF EXISTS "product_transfers_select" ON product_transfers;
CREATE POLICY "product_transfers_select" ON product_transfers FOR SELECT TO authenticated USING (true);

-- SALES
DROP POLICY IF EXISTS "sales_select" ON sales;
CREATE POLICY "sales_select" ON sales FOR SELECT TO authenticated USING (true);

-- SALE_ITEMS
DROP POLICY IF EXISTS "sale_items_all" ON sale_items;
DROP POLICY IF EXISTS "sale_items_select" ON sale_items;
DROP POLICY IF EXISTS "sale_items_insert" ON sale_items;
DROP POLICY IF EXISTS "sale_items_update" ON sale_items;
DROP POLICY IF EXISTS "sale_items_delete" ON sale_items;
CREATE POLICY "sale_items_select" ON sale_items FOR SELECT TO authenticated USING (true);
CREATE POLICY "sale_items_insert" ON sale_items FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "sale_items_update" ON sale_items FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "sale_items_delete" ON sale_items FOR DELETE TO authenticated USING (true);

-- AUDIT_HISTORY (already permissive, but ensure it)
DROP POLICY IF EXISTS "audit_history_select" ON audit_history;
CREATE POLICY "audit_history_select" ON audit_history FOR SELECT TO authenticated USING (true);

-- SCHEMA_MIGRATIONS
DROP POLICY IF EXISTS "schema_migrations_select" ON schema_migrations;
CREATE POLICY "schema_migrations_select" ON schema_migrations FOR SELECT TO authenticated USING (true);

-- SYNC_QUEUE - users need full access to their sync queue
DROP POLICY IF EXISTS "sync_queue_select" ON sync_queue;
DROP POLICY IF EXISTS "sync_queue_insert" ON sync_queue;
DROP POLICY IF EXISTS "sync_queue_update" ON sync_queue;
DROP POLICY IF EXISTS "sync_queue_delete" ON sync_queue;
CREATE POLICY "sync_queue_all" ON sync_queue FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ============================================================================
-- Also make INSERT/UPDATE permissive for authenticated users
-- App-level permissions handle who can do what in the UI
-- ============================================================================

-- SITES
DROP POLICY IF EXISTS "sites_admin_all" ON sites;
CREATE POLICY "sites_insert" ON sites FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "sites_update" ON sites FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "sites_delete" ON sites FOR DELETE TO authenticated USING (true);

-- CATEGORIES
DROP POLICY IF EXISTS "categories_admin_all" ON categories;
CREATE POLICY "categories_insert" ON categories FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "categories_update" ON categories FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "categories_delete" ON categories FOR DELETE TO authenticated USING (true);

-- PACKAGING_TYPES
DROP POLICY IF EXISTS "packaging_types_admin_all" ON packaging_types;
CREATE POLICY "packaging_types_insert" ON packaging_types FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "packaging_types_update" ON packaging_types FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "packaging_types_delete" ON packaging_types FOR DELETE TO authenticated USING (true);

-- APP_USERS
DROP POLICY IF EXISTS "users_admin_insert" ON app_users;
DROP POLICY IF EXISTS "users_admin_update" ON app_users;
DROP POLICY IF EXISTS "users_admin_delete" ON app_users;
CREATE POLICY "users_insert" ON app_users FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "users_update" ON app_users FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "users_delete" ON app_users FOR DELETE TO authenticated USING (true);

-- USER_PERMISSIONS
DROP POLICY IF EXISTS "user_permissions_admin_all" ON user_permissions;
CREATE POLICY "user_permissions_insert" ON user_permissions FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "user_permissions_update" ON user_permissions FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "user_permissions_delete" ON user_permissions FOR DELETE TO authenticated USING (true);

-- CUSTOMERS
DROP POLICY IF EXISTS "customers_insert" ON customers;
DROP POLICY IF EXISTS "customers_update" ON customers;
DROP POLICY IF EXISTS "customers_delete" ON customers;
CREATE POLICY "customers_insert" ON customers FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "customers_update" ON customers FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "customers_delete" ON customers FOR DELETE TO authenticated USING (true);

-- PRODUCTS
DROP POLICY IF EXISTS "products_insert" ON products;
DROP POLICY IF EXISTS "products_update" ON products;
DROP POLICY IF EXISTS "products_delete" ON products;
CREATE POLICY "products_insert" ON products FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "products_update" ON products FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "products_delete" ON products FOR DELETE TO authenticated USING (true);

-- PRODUCT_PRICES
DROP POLICY IF EXISTS "product_prices_admin_all" ON product_prices;
CREATE POLICY "product_prices_insert" ON product_prices FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "product_prices_update" ON product_prices FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "product_prices_delete" ON product_prices FOR DELETE TO authenticated USING (true);

-- PURCHASE_BATCHES
DROP POLICY IF EXISTS "purchase_batches_insert" ON purchase_batches;
DROP POLICY IF EXISTS "purchase_batches_update" ON purchase_batches;
DROP POLICY IF EXISTS "purchase_batches_delete" ON purchase_batches;
CREATE POLICY "purchase_batches_insert" ON purchase_batches FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "purchase_batches_update" ON purchase_batches FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "purchase_batches_delete" ON purchase_batches FOR DELETE TO authenticated USING (true);

-- STOCK_MOVEMENTS
DROP POLICY IF EXISTS "stock_movements_insert" ON stock_movements;
DROP POLICY IF EXISTS "stock_movements_update" ON stock_movements;
DROP POLICY IF EXISTS "stock_movements_delete" ON stock_movements;
CREATE POLICY "stock_movements_insert" ON stock_movements FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "stock_movements_update" ON stock_movements FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "stock_movements_delete" ON stock_movements FOR DELETE TO authenticated USING (true);

-- INVENTORIES
DROP POLICY IF EXISTS "inventories_insert" ON inventories;
DROP POLICY IF EXISTS "inventories_update" ON inventories;
DROP POLICY IF EXISTS "inventories_delete" ON inventories;
CREATE POLICY "inventories_insert" ON inventories FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "inventories_update" ON inventories FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "inventories_delete" ON inventories FOR DELETE TO authenticated USING (true);

-- PRODUCT_TRANSFERS
DROP POLICY IF EXISTS "product_transfers_insert" ON product_transfers;
DROP POLICY IF EXISTS "product_transfers_update" ON product_transfers;
DROP POLICY IF EXISTS "product_transfers_delete" ON product_transfers;
CREATE POLICY "product_transfers_insert" ON product_transfers FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "product_transfers_update" ON product_transfers FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "product_transfers_delete" ON product_transfers FOR DELETE TO authenticated USING (true);

-- SALES
DROP POLICY IF EXISTS "sales_insert" ON sales;
DROP POLICY IF EXISTS "sales_update" ON sales;
DROP POLICY IF EXISTS "sales_delete" ON sales;
CREATE POLICY "sales_insert" ON sales FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "sales_update" ON sales FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "sales_delete" ON sales FOR DELETE TO authenticated USING (true);

-- ============================================================================
-- Summary: All authenticated users can now read AND write all data.
-- Security is enforced at the application level through the permissions system.
-- ============================================================================
