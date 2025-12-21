-- ============================================================================
-- MEDISTOCK DATABASE SCHEMA FOR SUPABASE (PostgreSQL)
-- Migration from Android Room to Supabase PostgreSQL
-- ============================================================================

-- Enable UUID extension (useful for future features)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- 1. RÉFÉRENTIELS DE BASE
-- ============================================================================

-- Sites (pharmacies, dépôts, etc.)
CREATE TABLE sites (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT '',
    updated_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_sites_name ON sites(name);

-- Catégories de produits
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT '',
    updated_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_categories_name ON categories(name);

-- Types de conditionnement (Boîte/Comprimés, Flacon/ml, etc.)
CREATE TABLE packaging_types (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    level1_name TEXT NOT NULL,
    level2_name TEXT,
    default_conversion_factor DOUBLE PRECISION,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT,
    updated_by TEXT
);

CREATE INDEX idx_packaging_types_active ON packaging_types(is_active);
CREATE INDEX idx_packaging_types_order ON packaging_types(display_order);

-- ============================================================================
-- 2. UTILISATEURS & PERMISSIONS
-- ============================================================================

-- Utilisateurs de l'application
CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL, -- BCrypt hash
    full_name TEXT NOT NULL,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT '',
    updated_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_users_username ON app_users(username);
CREATE INDEX idx_users_active ON app_users(is_active);

-- Permissions par utilisateur et module
CREATE TABLE user_permissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    module TEXT NOT NULL,
    can_view BOOLEAN NOT NULL DEFAULT FALSE,
    can_create BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT '',
    updated_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_user_permissions_user ON user_permissions(user_id);
CREATE INDEX idx_user_permissions_module ON user_permissions(module);

-- ============================================================================
-- 3. CLIENTS
-- ============================================================================

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT,
    address TEXT,
    notes TEXT,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_customers_site ON customers(site_id);
CREATE INDEX idx_customers_name ON customers(name);

-- ============================================================================
-- 4. PRODUITS
-- ============================================================================

-- Produits (médicaments)
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    unit TEXT NOT NULL,
    unit_volume DOUBLE PRECISION NOT NULL,

    -- Système de conditionnement
    packaging_type_id BIGINT REFERENCES packaging_types(id) ON DELETE SET NULL,
    selected_level INTEGER,
    conversion_factor DOUBLE PRECISION,

    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,

    -- Marge
    margin_type TEXT,
    margin_value DOUBLE PRECISION,

    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,

    -- Stock min/max
    min_stock DOUBLE PRECISION DEFAULT 0.0,
    max_stock DOUBLE PRECISION DEFAULT 0.0,

    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT '',
    updated_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_products_site ON products(site_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_packaging_type ON products(packaging_type_id);
CREATE INDEX idx_products_name ON products(name);

-- Historique des prix des produits
CREATE TABLE product_prices (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    effective_date BIGINT NOT NULL,
    purchase_price DOUBLE PRECISION NOT NULL,
    selling_price DOUBLE PRECISION NOT NULL,
    source TEXT NOT NULL, -- "manual" or "calculated"
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT '',
    updated_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_product_prices_product ON product_prices(product_id);
CREATE INDEX idx_product_prices_date ON product_prices(effective_date);

-- ============================================================================
-- 5. GESTION DES STOCKS
-- ============================================================================

-- Lots d'achat (FIFO inventory management)
CREATE TABLE purchase_batches (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    batch_number TEXT,
    purchase_date BIGINT NOT NULL,
    initial_quantity DOUBLE PRECISION NOT NULL,
    remaining_quantity DOUBLE PRECISION NOT NULL,
    purchase_price DOUBLE PRECISION NOT NULL,
    supplier_name TEXT NOT NULL DEFAULT '',
    expiry_date BIGINT,
    is_exhausted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT '',
    updated_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_purchase_batches_product ON purchase_batches(product_id);
CREATE INDEX idx_purchase_batches_site ON purchase_batches(site_id);
CREATE INDEX idx_purchase_batches_exhausted ON purchase_batches(is_exhausted);
CREATE INDEX idx_purchase_batches_date ON purchase_batches(purchase_date);

-- Mouvements de stock
CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    type TEXT NOT NULL, -- "IN" or "OUT"
    quantity DOUBLE PRECISION NOT NULL,
    date BIGINT NOT NULL,
    purchase_price_at_movement DOUBLE PRECISION NOT NULL,
    selling_price_at_movement DOUBLE PRECISION NOT NULL,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_stock_movements_product ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_site ON stock_movements(site_id);
CREATE INDEX idx_stock_movements_type ON stock_movements(type);
CREATE INDEX idx_stock_movements_date ON stock_movements(date);

-- Inventaires physiques
CREATE TABLE inventories (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    count_date BIGINT NOT NULL,
    counted_quantity DOUBLE PRECISION NOT NULL,
    theoretical_quantity DOUBLE PRECISION NOT NULL,
    discrepancy DOUBLE PRECISION NOT NULL,
    reason TEXT NOT NULL DEFAULT '',
    counted_by TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_inventories_product ON inventories(product_id);
CREATE INDEX idx_inventories_site ON inventories(site_id);
CREATE INDEX idx_inventories_date ON inventories(count_date);

-- Transferts de produits entre sites
CREATE TABLE product_transfers (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity DOUBLE PRECISION NOT NULL,
    from_site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    to_site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    date BIGINT NOT NULL,
    notes TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT '',
    updated_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_product_transfers_product ON product_transfers(product_id);
CREATE INDEX idx_product_transfers_from_site ON product_transfers(from_site_id);
CREATE INDEX idx_product_transfers_to_site ON product_transfers(to_site_id);
CREATE INDEX idx_product_transfers_date ON product_transfers(date);

-- ============================================================================
-- 6. VENTES
-- ============================================================================

-- En-têtes de ventes
CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    customer_name TEXT NOT NULL,
    customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    date BIGINT NOT NULL,
    total_amount DOUBLE PRECISION NOT NULL,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_sales_customer ON sales(customer_id);
CREATE INDEX idx_sales_site ON sales(site_id);
CREATE INDEX idx_sales_date ON sales(date);

-- Lignes de vente (détails)
CREATE TABLE sale_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_name TEXT NOT NULL,
    unit TEXT NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    price_per_unit DOUBLE PRECISION NOT NULL,
    subtotal DOUBLE PRECISION NOT NULL
);

CREATE INDEX idx_sale_items_sale ON sale_items(sale_id);
CREATE INDEX idx_sale_items_product ON sale_items(product_id);

-- Allocations FIFO des lots aux ventes
CREATE TABLE sale_batch_allocations (
    id BIGSERIAL PRIMARY KEY,
    sale_item_id BIGINT NOT NULL REFERENCES sale_items(id) ON DELETE CASCADE,
    batch_id BIGINT NOT NULL REFERENCES purchase_batches(id) ON DELETE RESTRICT,
    quantity_allocated DOUBLE PRECISION NOT NULL,
    purchase_price_at_allocation DOUBLE PRECISION NOT NULL,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
);

CREATE INDEX idx_sale_batch_allocations_sale_item ON sale_batch_allocations(sale_item_id);
CREATE INDEX idx_sale_batch_allocations_batch ON sale_batch_allocations(batch_id);

-- Ventes produits (ancien système - potentiellement obsolète)
CREATE TABLE product_sales (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity DOUBLE PRECISION NOT NULL,
    price_at_sale DOUBLE PRECISION NOT NULL,
    farmer_name TEXT NOT NULL,
    date BIGINT NOT NULL,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT ''
);

CREATE INDEX idx_product_sales_product ON product_sales(product_id);
CREATE INDEX idx_product_sales_site ON product_sales(site_id);
CREATE INDEX idx_product_sales_date ON product_sales(date);

-- ============================================================================
-- 7. AUDIT & HISTORIQUE
-- ============================================================================

-- Historique d'audit (toutes les modifications)
CREATE TABLE audit_history (
    id BIGSERIAL PRIMARY KEY,
    entity_type TEXT NOT NULL,
    entity_id BIGINT NOT NULL,
    action_type TEXT NOT NULL, -- "INSERT", "UPDATE", "DELETE"
    field_name TEXT,
    old_value TEXT,
    new_value TEXT,
    changed_by TEXT NOT NULL,
    changed_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    site_id BIGINT REFERENCES sites(id) ON DELETE SET NULL,
    description TEXT
);

CREATE INDEX idx_audit_history_entity ON audit_history(entity_type, entity_id);
CREATE INDEX idx_audit_history_user ON audit_history(changed_by);
CREATE INDEX idx_audit_history_date ON audit_history(changed_at);
CREATE INDEX idx_audit_history_site ON audit_history(site_id);

-- ============================================================================
-- TRIGGERS POUR UPDATE TIMESTAMPS
-- ============================================================================

-- Fonction pour mettre à jour automatiquement updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Appliquer le trigger sur toutes les tables avec updated_at
CREATE TRIGGER update_sites_updated_at BEFORE UPDATE ON sites
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_categories_updated_at BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_packaging_types_updated_at BEFORE UPDATE ON packaging_types
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_app_users_updated_at BEFORE UPDATE ON app_users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_permissions_updated_at BEFORE UPDATE ON user_permissions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_product_prices_updated_at BEFORE UPDATE ON product_prices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_purchase_batches_updated_at BEFORE UPDATE ON purchase_batches
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_product_transfers_updated_at BEFORE UPDATE ON product_transfers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- VUES UTILES
-- ============================================================================

-- Vue pour obtenir le stock actuel par produit et site
CREATE OR REPLACE VIEW current_stock AS
SELECT
    p.id as product_id,
    p.name as product_name,
    p.site_id,
    s.name as site_name,
    COALESCE(SUM(pb.remaining_quantity), 0) as current_stock,
    p.min_stock,
    p.max_stock,
    CASE
        WHEN COALESCE(SUM(pb.remaining_quantity), 0) <= p.min_stock THEN 'LOW'
        WHEN COALESCE(SUM(pb.remaining_quantity), 0) >= p.max_stock THEN 'HIGH'
        ELSE 'NORMAL'
    END as stock_status
FROM products p
LEFT JOIN purchase_batches pb ON p.id = pb.product_id AND pb.is_exhausted = FALSE
LEFT JOIN sites s ON p.site_id = s.id
GROUP BY p.id, p.name, p.site_id, s.name, p.min_stock, p.max_stock;

-- ============================================================================
-- DONNÉES INITIALES (EXEMPLES)
-- ============================================================================

-- Insérer un site par défaut
INSERT INTO sites (name, created_by) VALUES ('Site Principal', 'system');

-- Insérer quelques catégories par défaut
INSERT INTO categories (name, created_by) VALUES
    ('Antibiotiques', 'system'),
    ('Antalgiques', 'system'),
    ('Anti-inflammatoires', 'system'),
    ('Vitamines', 'system');

-- Insérer quelques types de conditionnement
INSERT INTO packaging_types (name, level1_name, level2_name, default_conversion_factor, created_by) VALUES
    ('Boîte/Comprimés', 'Boîte', 'Comprimés', 30, 'system'),
    ('Flacon/ml', 'Flacon', 'ml', 100, 'system'),
    ('Units', 'Units', NULL, NULL, 'system');

-- Créer un utilisateur admin par défaut (password: admin123)
-- Hash BCrypt de "admin123": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO app_users (username, password, full_name, is_admin, is_active, created_by) VALUES
    ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Administrateur', TRUE, TRUE, 'system');

-- ============================================================================
-- FIN DU SCHÉMA
-- ============================================================================

-- Afficher un message de confirmation
DO $$
BEGIN
    RAISE NOTICE 'Medistock database schema created successfully!';
    RAISE NOTICE 'Total tables: 17';
    RAISE NOTICE 'Default admin user: admin / admin123';
END $$;
