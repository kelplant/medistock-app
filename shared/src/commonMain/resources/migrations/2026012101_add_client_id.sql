-- ============================================================================
-- MEDISTOCK MIGRATION: Add client_id column for Realtime support
-- This column allows filtering Realtime events to ignore self-triggered changes
-- ============================================================================

-- ============================================================================
-- 1. AJOUTER LA COLONNE client_id À TOUTES LES TABLES
-- ============================================================================

-- Sites
ALTER TABLE sites ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Categories
ALTER TABLE categories ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Packaging Types
ALTER TABLE packaging_types ADD COLUMN IF NOT EXISTS client_id TEXT;

-- App Users
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS client_id TEXT;

-- User Permissions
ALTER TABLE user_permissions ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Customers
ALTER TABLE customers ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Products
ALTER TABLE products ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Product Prices
ALTER TABLE product_prices ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Purchase Batches
ALTER TABLE purchase_batches ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Stock Movements
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Inventories
ALTER TABLE inventories ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Product Transfers
ALTER TABLE product_transfers ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Sales
ALTER TABLE sales ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Sale Items
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Sale Batch Allocations
ALTER TABLE sale_batch_allocations ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Audit History
ALTER TABLE audit_history ADD COLUMN IF NOT EXISTS client_id TEXT;

-- ============================================================================
-- 2. CRÉER LES INDEX POUR OPTIMISER LES REQUÊTES REALTIME
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_sites_client_id ON sites(client_id);
CREATE INDEX IF NOT EXISTS idx_categories_client_id ON categories(client_id);
CREATE INDEX IF NOT EXISTS idx_app_users_client_id ON app_users(client_id);
CREATE INDEX IF NOT EXISTS idx_products_client_id ON products(client_id);
CREATE INDEX IF NOT EXISTS idx_purchase_batches_client_id ON purchase_batches(client_id);
CREATE INDEX IF NOT EXISTS idx_sales_client_id ON sales(client_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_client_id ON stock_movements(client_id);
