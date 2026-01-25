-- ============================================================================
-- MEDISTOCK DATABASE SCHEMA FOR SUPABASE (PostgreSQL)
-- Migration from Android Room to Supabase PostgreSQL
-- Includes ALL migrations up to 2026-01-25
-- Schema version: 7
-- ============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- 1. REFERENTIELS DE BASE
-- ============================================================================

-- Sites (pharmacies, depots, etc.)
CREATE TABLE sites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_sites_name ON sites(name);
CREATE INDEX idx_sites_client_id ON sites(client_id);
CREATE INDEX idx_sites_is_active ON sites(is_active);

-- Categories de produits
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_categories_name ON categories(name);
CREATE INDEX idx_categories_client_id ON categories(client_id);
CREATE INDEX idx_categories_is_active ON categories(is_active);

-- Types de conditionnement (Boite/Comprimes, Flacon/ml, etc.)
CREATE TABLE packaging_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    level1_name TEXT NOT NULL,
    level2_name TEXT,
    default_conversion_factor DOUBLE PRECISION,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_packaging_types_active ON packaging_types(is_active);
CREATE INDEX idx_packaging_types_order ON packaging_types(display_order);

-- ============================================================================
-- 2. UTILISATEURS & PERMISSIONS
-- ============================================================================

-- Utilisateurs de l'application
CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL, -- BCrypt hash
    full_name TEXT NOT NULL,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    auth_migrated INTEGER DEFAULT 0,
    language TEXT,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_users_username ON app_users(username);
CREATE INDEX idx_users_active ON app_users(is_active);
CREATE INDEX idx_app_users_client_id ON app_users(client_id);
CREATE INDEX idx_app_users_auth_migrated ON app_users(auth_migrated) WHERE auth_migrated = 0;

COMMENT ON COLUMN app_users.auth_migrated IS '0 = not migrated (still using BCrypt only), 1 = migrated to Supabase Auth';
COMMENT ON COLUMN app_users.language IS 'User preferred language code (e.g., en, fr, de)';

-- Permissions par utilisateur et module
CREATE TABLE user_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    module TEXT NOT NULL,
    can_view BOOLEAN NOT NULL DEFAULT FALSE,
    can_create BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_user_permissions_user ON user_permissions(user_id);
CREATE INDEX idx_user_permissions_module ON user_permissions(module);

-- ============================================================================
-- 3. CLIENTS
-- ============================================================================

CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    phone TEXT,
    address TEXT,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_customers_site ON customers(site_id);
CREATE INDEX idx_customers_name ON customers(name);
CREATE INDEX idx_customers_is_active ON customers(is_active);

-- ============================================================================
-- 4. PRODUITS
-- ============================================================================

-- Produits (medicaments)
-- Note: unit is derived from packaging_types based on selected_level
-- If selected_level = 1: use packaging_type.level1_name
-- If selected_level = 2: use packaging_type.level2_name
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    unit_volume DOUBLE PRECISION NOT NULL,

    -- Systeme de conditionnement (unit derived from packaging_type)
    packaging_type_id UUID NOT NULL REFERENCES packaging_types(id) ON DELETE RESTRICT,
    selected_level INTEGER NOT NULL DEFAULT 1,
    conversion_factor DOUBLE PRECISION,

    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,

    -- Marge
    margin_type TEXT,
    margin_value DOUBLE PRECISION,
    description TEXT,

    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,

    -- Stock min/max
    min_stock DOUBLE PRECISION DEFAULT 0.0,
    max_stock DOUBLE PRECISION DEFAULT 0.0,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_products_site ON products(site_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_packaging_type ON products(packaging_type_id);
CREATE INDEX idx_products_packaging ON products(packaging_type_id);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_client_id ON products(client_id);
CREATE INDEX idx_products_is_active ON products(is_active);

-- Historique des prix des produits
CREATE TABLE product_prices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    effective_date BIGINT NOT NULL,
    purchase_price DOUBLE PRECISION NOT NULL,
    selling_price DOUBLE PRECISION NOT NULL,
    source TEXT NOT NULL, -- "manual" or "calculated"
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_product_prices_product ON product_prices(product_id);
CREATE INDEX idx_product_prices_date ON product_prices(effective_date);

-- ============================================================================
-- 5. GESTION DES STOCKS
-- ============================================================================

-- Lots d'achat (FIFO inventory management)
CREATE TABLE purchase_batches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
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
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_purchase_batches_product ON purchase_batches(product_id);
CREATE INDEX idx_purchase_batches_site ON purchase_batches(site_id);
CREATE INDEX idx_purchase_batches_exhausted ON purchase_batches(is_exhausted);
CREATE INDEX idx_purchase_batches_date ON purchase_batches(purchase_date);
CREATE INDEX idx_purchase_batches_client_id ON purchase_batches(client_id);

-- Mouvements de stock
CREATE TABLE stock_movements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    type TEXT NOT NULL, -- "IN" or "OUT"
    quantity DOUBLE PRECISION NOT NULL,
    date BIGINT NOT NULL,
    purchase_price_at_movement DOUBLE PRECISION NOT NULL,
    selling_price_at_movement DOUBLE PRECISION NOT NULL,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_stock_movements_product ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_site ON stock_movements(site_id);
CREATE INDEX idx_stock_movements_type ON stock_movements(type);
CREATE INDEX idx_stock_movements_date ON stock_movements(date);
CREATE INDEX idx_stock_movements_client_id ON stock_movements(client_id);

-- Inventaires physiques (inventory sessions)
CREATE TABLE inventories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    count_date BIGINT NOT NULL,
    counted_quantity DOUBLE PRECISION NOT NULL,
    theoretical_quantity DOUBLE PRECISION NOT NULL,
    discrepancy DOUBLE PRECISION NOT NULL,
    reason TEXT NOT NULL DEFAULT '',
    counted_by TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_inventories_product ON inventories(product_id);
CREATE INDEX idx_inventories_site ON inventories(site_id);
CREATE INDEX idx_inventories_date ON inventories(count_date);

-- Inventory items (individual product counts during inventory sessions)
CREATE TABLE inventory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id UUID REFERENCES inventories(id) ON DELETE SET NULL,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    count_date BIGINT NOT NULL,
    counted_quantity REAL NOT NULL,
    theoretical_quantity REAL NOT NULL,
    discrepancy REAL NOT NULL DEFAULT 0.0,
    reason TEXT NOT NULL DEFAULT '',
    counted_by TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_inventory_items_site ON inventory_items(site_id);
CREATE INDEX idx_inventory_items_product ON inventory_items(product_id);
CREATE INDEX idx_inventory_items_date ON inventory_items(count_date);
CREATE INDEX idx_inventory_items_inventory ON inventory_items(inventory_id);
CREATE INDEX idx_inventory_items_client_id ON inventory_items(client_id);

-- Transferts de produits entre sites
CREATE TABLE product_transfers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity DOUBLE PRECISION NOT NULL,
    from_site_id UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    to_site_id UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    date BIGINT NOT NULL,
    notes TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_product_transfers_product ON product_transfers(product_id);
CREATE INDEX idx_product_transfers_from_site ON product_transfers(from_site_id);
CREATE INDEX idx_product_transfers_to_site ON product_transfers(to_site_id);
CREATE INDEX idx_product_transfers_date ON product_transfers(date);

-- ============================================================================
-- 6. VENTES
-- ============================================================================

-- En-tetes de ventes
CREATE TABLE sales (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_name TEXT NOT NULL,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    date BIGINT NOT NULL,
    total_amount DOUBLE PRECISION NOT NULL,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_sales_customer ON sales(customer_id);
CREATE INDEX idx_sales_site ON sales(site_id);
CREATE INDEX idx_sales_date ON sales(date);
CREATE INDEX idx_sales_client_id ON sales(client_id);

-- Lignes de vente (details)
CREATE TABLE sale_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sale_id UUID NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_name TEXT NOT NULL DEFAULT '',
    unit TEXT NOT NULL DEFAULT '',
    quantity DOUBLE PRECISION NOT NULL,
    price_per_unit DOUBLE PRECISION NOT NULL,
    subtotal DOUBLE PRECISION NOT NULL,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_sale_items_sale ON sale_items(sale_id);
CREATE INDEX idx_sale_items_product ON sale_items(product_id);

-- Allocations FIFO des lots aux ventes
CREATE TABLE sale_batch_allocations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sale_item_id UUID NOT NULL REFERENCES sale_items(id) ON DELETE CASCADE,
    batch_id UUID NOT NULL REFERENCES purchase_batches(id) ON DELETE RESTRICT,
    quantity_allocated DOUBLE PRECISION NOT NULL,
    purchase_price_at_allocation DOUBLE PRECISION NOT NULL,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by TEXT NOT NULL DEFAULT 'system',
    client_id TEXT
);

CREATE INDEX idx_sale_batch_allocations_sale_item ON sale_batch_allocations(sale_item_id);
CREATE INDEX idx_sale_batch_allocations_batch ON sale_batch_allocations(batch_id);

-- ============================================================================
-- 7. AUDIT & HISTORIQUE
-- ============================================================================

-- Historique d'audit (toutes les modifications)
CREATE TABLE audit_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type TEXT NOT NULL,
    entity_id UUID NOT NULL,
    action_type TEXT NOT NULL, -- "INSERT", "UPDATE", "DELETE"
    field_name TEXT,
    old_value TEXT,
    new_value TEXT,
    changed_by TEXT NOT NULL,
    changed_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    site_id UUID REFERENCES sites(id) ON DELETE SET NULL,
    description TEXT,
    client_id TEXT
);

CREATE INDEX idx_audit_history_entity ON audit_history(entity_type, entity_id);
CREATE INDEX idx_audit_history_user ON audit_history(changed_by);
CREATE INDEX idx_audit_history_date ON audit_history(changed_at);
CREATE INDEX idx_audit_history_site ON audit_history(site_id);

-- ============================================================================
-- 8. SYNC & NOTIFICATIONS
-- ============================================================================

-- Sync queue table for offline sync tracking
CREATE TABLE sync_queue (
    id TEXT NOT NULL PRIMARY KEY,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    operation TEXT NOT NULL,
    payload TEXT NOT NULL,
    local_version INTEGER NOT NULL DEFAULT 1,
    remote_version INTEGER,
    last_known_remote_updated_at INTEGER,
    status TEXT NOT NULL DEFAULT 'pending',
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    last_attempt_at INTEGER,
    created_at INTEGER NOT NULL DEFAULT 0,
    user_id TEXT,
    site_id TEXT
);

CREATE INDEX idx_sync_queue_status ON sync_queue(status);
CREATE INDEX idx_sync_queue_entity ON sync_queue(entity_type, entity_id);
CREATE INDEX idx_sync_queue_created ON sync_queue(created_at);

-- Notification events local cache
CREATE TABLE notification_events_local (
    id TEXT NOT NULL PRIMARY KEY,
    type TEXT NOT NULL,
    priority TEXT NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    reference_id TEXT,
    reference_type TEXT,
    site_id TEXT,
    deep_link TEXT,
    created_at INTEGER NOT NULL,
    is_displayed INTEGER NOT NULL DEFAULT 0,
    is_dismissed INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_notification_events_displayed ON notification_events_local(is_displayed);
CREATE INDEX idx_notification_events_dismissed ON notification_events_local(is_dismissed);
CREATE INDEX idx_notification_events_created_at ON notification_events_local(created_at DESC);

-- ============================================================================
-- 9. APP CONFIGURATION
-- ============================================================================

-- App config table for storing global configuration
CREATE TABLE app_config (
    key TEXT PRIMARY KEY,
    value TEXT,
    description TEXT,
    updated_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_by TEXT
);

COMMENT ON TABLE app_config IS 'Global application configuration. Only accessible via service_role.';
COMMENT ON COLUMN app_config.key IS 'Configuration key (e.g., recovery_secret_key)';
COMMENT ON COLUMN app_config.value IS 'Configuration value';
COMMENT ON COLUMN app_config.description IS 'Human-readable description of the configuration';

-- ============================================================================
-- NOTE: Les triggers d'audit Supabase ont ete desactives.
-- L'audit est gere uniquement cote application Android (Room + AuditLogger)
-- pour eviter la duplication des donnees et reduire le volume de stockage.
--
-- La table audit_history reste disponible pour recevoir les audits de l'app.
-- Voir migration 2026011901_remove_audit_triggers.sql pour plus de details.
-- ============================================================================

-- ============================================================================
-- TRIGGERS POUR UPDATE TIMESTAMPS
-- ============================================================================

-- Fonction pour mettre a jour automatiquement updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Fonction pour definir created_by/updated_by a "system" si absent
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

-- Fonction pour definir created_by a "system" si absent
CREATE OR REPLACE FUNCTION set_created_by_default()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.created_by IS NULL OR NEW.created_by = '' THEN
        NEW.created_by = 'system';
    END IF;
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

CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Appliquer le trigger pour created_by/updated_by
CREATE TRIGGER set_sites_audit_defaults BEFORE INSERT OR UPDATE ON sites
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

CREATE TRIGGER set_categories_audit_defaults BEFORE INSERT OR UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

CREATE TRIGGER set_packaging_types_audit_defaults BEFORE INSERT OR UPDATE ON packaging_types
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

CREATE TRIGGER set_app_users_audit_defaults BEFORE INSERT OR UPDATE ON app_users
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

CREATE TRIGGER set_user_permissions_audit_defaults BEFORE INSERT OR UPDATE ON user_permissions
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

CREATE TRIGGER set_products_audit_defaults BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

CREATE TRIGGER set_product_prices_audit_defaults BEFORE INSERT OR UPDATE ON product_prices
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

CREATE TRIGGER set_purchase_batches_audit_defaults BEFORE INSERT OR UPDATE ON purchase_batches
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

CREATE TRIGGER set_product_transfers_audit_defaults BEFORE INSERT OR UPDATE ON product_transfers
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

CREATE TRIGGER set_customers_audit_defaults BEFORE INSERT OR UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

CREATE TRIGGER set_stock_movements_created_by BEFORE INSERT ON stock_movements
    FOR EACH ROW EXECUTE FUNCTION set_created_by_default();

CREATE TRIGGER set_inventories_created_by BEFORE INSERT ON inventories
    FOR EACH ROW EXECUTE FUNCTION set_created_by_default();

CREATE TRIGGER set_sales_created_by BEFORE INSERT ON sales
    FOR EACH ROW EXECUTE FUNCTION set_created_by_default();

CREATE TRIGGER set_sale_items_created_by BEFORE INSERT ON sale_items
    FOR EACH ROW EXECUTE FUNCTION set_created_by_default();

CREATE TRIGGER set_sale_batch_allocations_created_by BEFORE INSERT ON sale_batch_allocations
    FOR EACH ROW EXECUTE FUNCTION set_created_by_default();

-- ============================================================================
-- HELPER FUNCTIONS
-- ============================================================================

-- Helper function to get currency symbol
CREATE OR REPLACE FUNCTION get_currency_symbol()
RETURNS TEXT AS $$
  SELECT COALESCE(
    (SELECT value FROM app_config WHERE key = 'currency_symbol'),
    'F'
  );
$$ LANGUAGE SQL STABLE;

-- Helper function: Check if user is admin
CREATE OR REPLACE FUNCTION is_admin()
RETURNS BOOLEAN AS $$
BEGIN
    -- Check if user exists in users table and is admin
    RETURN EXISTS (
        SELECT 1 FROM app_users
        WHERE id = auth.uid()
        AND is_admin = TRUE
        AND is_active = TRUE
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- Helper function: Check if user has access to a site
CREATE OR REPLACE FUNCTION has_site_access(p_site_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    -- Admins have access to all sites
    IF is_admin() THEN
        RETURN TRUE;
    END IF;

    -- For now, all authenticated users have access to all sites
    -- TODO: Implement user_sites table for site-based access control
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- Recovery admin function (SECURITY DEFINER, service_role only)
CREATE OR REPLACE FUNCTION create_recovery_admin(
    p_username TEXT DEFAULT 'recovery_admin',
    p_password TEXT DEFAULT 'Recovery123!',
    p_secret_key TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_recovery_key TEXT;
    v_user_id UUID;
    v_password_hash TEXT;
    v_now BIGINT;
BEGIN
    -- 1. Check recovery secret key if configured
    SELECT value INTO v_recovery_key
    FROM app_config
    WHERE key = 'recovery_secret_key';

    IF v_recovery_key IS NOT NULL AND v_recovery_key != '' AND
       (p_secret_key IS NULL OR p_secret_key != v_recovery_key) THEN
        RETURN jsonb_build_object(
            'success', FALSE,
            'error', 'Invalid recovery key. Check app_config.recovery_secret_key'
        );
    END IF;

    -- 2. Validate inputs
    IF length(p_username) < 3 THEN
        RETURN jsonb_build_object(
            'success', FALSE,
            'error', 'Username must be at least 3 characters'
        );
    END IF;

    IF length(p_password) < 8 THEN
        RETURN jsonb_build_object(
            'success', FALSE,
            'error', 'Password must be at least 8 characters'
        );
    END IF;

    -- 3. Generate UUID and hash password
    v_user_id := gen_random_uuid();
    v_password_hash := crypt(p_password, gen_salt('bf'));
    v_now := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;

    -- 4. Create or update user in users table
    INSERT INTO app_users (
        id,
        username,
        password,
        full_name,
        is_admin,
        is_active,
        auth_migrated,
        created_at,
        updated_at
    )
    VALUES (
        v_user_id,
        p_username,
        v_password_hash,
        'Recovery Administrator',
        TRUE,
        TRUE,
        0,
        v_now,
        v_now
    )
    ON CONFLICT (username) DO UPDATE SET
        password = EXCLUDED.password,
        is_admin = TRUE,
        is_active = TRUE,
        auth_migrated = 0,
        updated_at = EXCLUDED.updated_at;

    -- If username already existed, get its ID
    IF NOT FOUND THEN
        SELECT id INTO v_user_id FROM app_users WHERE username = p_username;
    END IF;

    RETURN jsonb_build_object(
        'success', TRUE,
        'message', 'Recovery admin created successfully',
        'username', p_username,
        'temporary_password', p_password,
        'user_id', v_user_id,
        'warning', 'CHANGE PASSWORD IMMEDIATELY AFTER LOGIN!'
    );
END;
$$;

-- CRITICAL: Revoke execute from all roles except service_role
REVOKE EXECUTE ON FUNCTION create_recovery_admin FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION create_recovery_admin FROM anon;
REVOKE EXECUTE ON FUNCTION create_recovery_admin FROM authenticated;

COMMENT ON FUNCTION create_recovery_admin IS
'EMERGENCY USE ONLY - Creates a recovery admin user.
Usage: Supabase Dashboard -> SQL Editor -> SELECT create_recovery_admin();
The user will be migrated to Supabase Auth on first login.
IMPORTANT: Change the password immediately after login!';

-- ============================================================================
-- VUES UTILES
-- ============================================================================

-- Vue pour obtenir le stock actuel par produit et site
CREATE OR REPLACE VIEW current_stock AS
SELECT
    p.id as product_id,
    p.name as product_name,
    p.description,
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
GROUP BY p.id, p.name, p.description, p.site_id, s.name, p.min_stock, p.max_stock;

-- Vue plate pour le reporting Looker Studio (achats, ventes, transferts, corrections)
CREATE OR REPLACE VIEW transaction_flat_view AS
SELECT
    'PURCHASE'::TEXT AS transaction_type,
    pb.id AS transaction_id,
    pb.id AS reference_id,
    pb.purchase_date AS transaction_date,
    p.id AS product_id,
    p.name AS product_name,
    c.name AS category_name,
    ptg.name AS packaging_type_name,
    pb.site_id AS site_id,
    s.name AS site_name,
    NULL::UUID AS from_site_id,
    NULL::TEXT AS from_site_name,
    NULL::UUID AS to_site_id,
    NULL::TEXT AS to_site_name,
    NULL::UUID AS customer_id,
    NULL::TEXT AS customer_name,
    pb.supplier_name AS supplier_name,
    pb.initial_quantity AS quantity_in,
    0::DOUBLE PRECISION AS quantity_out,
    pb.initial_quantity AS quantity_delta,
    CASE WHEN p.selected_level = 2 THEN ptg.level2_name ELSE ptg.level1_name END AS unit,
    pb.purchase_price AS unit_price,
    pb.initial_quantity * pb.purchase_price AS total_amount,
    COALESCE(NULLIF(pb.batch_number, ''), NULL) AS notes,
    'purchase_batches'::TEXT AS source_table
FROM purchase_batches pb
JOIN products p ON pb.product_id = p.id
LEFT JOIN sites s ON pb.site_id = s.id
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN packaging_types ptg ON p.packaging_type_id = ptg.id

UNION ALL
SELECT
    'SALE'::TEXT AS transaction_type,
    si.id AS transaction_id,
    sa.id AS reference_id,
    sa.date AS transaction_date,
    p.id AS product_id,
    p.name AS product_name,
    c.name AS category_name,
    ptg.name AS packaging_type_name,
    sa.site_id AS site_id,
    s.name AS site_name,
    NULL::UUID AS from_site_id,
    NULL::TEXT AS from_site_name,
    NULL::UUID AS to_site_id,
    NULL::TEXT AS to_site_name,
    sa.customer_id AS customer_id,
    COALESCE(sa.customer_name, cust.name) AS customer_name,
    NULL::TEXT AS supplier_name,
    0::DOUBLE PRECISION AS quantity_in,
    si.quantity AS quantity_out,
    -si.quantity AS quantity_delta,
    si.unit AS unit,
    si.price_per_unit AS unit_price,
    si.subtotal AS total_amount,
    NULL::TEXT AS notes,
    'sale_items'::TEXT AS source_table
FROM sale_items si
JOIN sales sa ON si.sale_id = sa.id
JOIN products p ON si.product_id = p.id
LEFT JOIN sites s ON sa.site_id = s.id
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN packaging_types ptg ON p.packaging_type_id = ptg.id
LEFT JOIN customers cust ON sa.customer_id = cust.id

UNION ALL
SELECT
    'TRANSFER_OUT'::TEXT AS transaction_type,
    pt.id AS transaction_id,
    pt.id AS reference_id,
    pt.date AS transaction_date,
    p.id AS product_id,
    p.name AS product_name,
    c.name AS category_name,
    ptg.name AS packaging_type_name,
    pt.from_site_id AS site_id,
    fs.name AS site_name,
    pt.from_site_id AS from_site_id,
    fs.name AS from_site_name,
    pt.to_site_id AS to_site_id,
    ts.name AS to_site_name,
    NULL::UUID AS customer_id,
    NULL::TEXT AS customer_name,
    NULL::TEXT AS supplier_name,
    0::DOUBLE PRECISION AS quantity_in,
    pt.quantity AS quantity_out,
    -pt.quantity AS quantity_delta,
    CASE WHEN p.selected_level = 2 THEN ptg.level2_name ELSE ptg.level1_name END AS unit,
    NULL::DOUBLE PRECISION AS unit_price,
    NULL::DOUBLE PRECISION AS total_amount,
    NULLIF(pt.notes, '') AS notes,
    'product_transfers'::TEXT AS source_table
FROM product_transfers pt
JOIN products p ON pt.product_id = p.id
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN packaging_types ptg ON p.packaging_type_id = ptg.id
LEFT JOIN sites fs ON pt.from_site_id = fs.id
LEFT JOIN sites ts ON pt.to_site_id = ts.id

UNION ALL
SELECT
    'TRANSFER_IN'::TEXT AS transaction_type,
    pt.id AS transaction_id,
    pt.id AS reference_id,
    pt.date AS transaction_date,
    p.id AS product_id,
    p.name AS product_name,
    c.name AS category_name,
    ptg.name AS packaging_type_name,
    pt.to_site_id AS site_id,
    ts.name AS site_name,
    pt.from_site_id AS from_site_id,
    fs.name AS from_site_name,
    pt.to_site_id AS to_site_id,
    ts.name AS to_site_name,
    NULL::UUID AS customer_id,
    NULL::TEXT AS customer_name,
    NULL::TEXT AS supplier_name,
    pt.quantity AS quantity_in,
    0::DOUBLE PRECISION AS quantity_out,
    pt.quantity AS quantity_delta,
    CASE WHEN p.selected_level = 2 THEN ptg.level2_name ELSE ptg.level1_name END AS unit,
    NULL::DOUBLE PRECISION AS unit_price,
    NULL::DOUBLE PRECISION AS total_amount,
    NULLIF(pt.notes, '') AS notes,
    'product_transfers'::TEXT AS source_table
FROM product_transfers pt
JOIN products p ON pt.product_id = p.id
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN packaging_types ptg ON p.packaging_type_id = ptg.id
LEFT JOIN sites fs ON pt.from_site_id = fs.id
LEFT JOIN sites ts ON pt.to_site_id = ts.id

UNION ALL
SELECT
    'INVENTORY_ADJUST'::TEXT AS transaction_type,
    i.id AS transaction_id,
    i.id AS reference_id,
    i.count_date AS transaction_date,
    p.id AS product_id,
    p.name AS product_name,
    c.name AS category_name,
    ptg.name AS packaging_type_name,
    i.site_id AS site_id,
    s.name AS site_name,
    NULL::UUID AS from_site_id,
    NULL::TEXT AS from_site_name,
    NULL::UUID AS to_site_id,
    NULL::TEXT AS to_site_name,
    NULL::UUID AS customer_id,
    NULL::TEXT AS customer_name,
    NULL::TEXT AS supplier_name,
    GREATEST(i.discrepancy, 0) AS quantity_in,
    GREATEST(-i.discrepancy, 0) AS quantity_out,
    i.discrepancy AS quantity_delta,
    CASE WHEN p.selected_level = 2 THEN ptg.level2_name ELSE ptg.level1_name END AS unit,
    NULL::DOUBLE PRECISION AS unit_price,
    NULL::DOUBLE PRECISION AS total_amount,
    NULLIF(COALESCE(NULLIF(i.reason, ''), NULLIF(i.notes, '')), '') AS notes,
    'inventories'::TEXT AS source_table
FROM inventories i
JOIN products p ON i.product_id = p.id
LEFT JOIN sites s ON i.site_id = s.id
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN packaging_types ptg ON p.packaging_type_id = ptg.id;

-- ============================================================================
-- REPORTING VIEWS
-- ============================================================================

-- v_sales_detail: Detailed sales with all dimensions
CREATE OR REPLACE VIEW v_sales_detail AS
SELECT
    s.id AS sale_id,
    s.date AS sale_date,
    DATE(TO_TIMESTAMP(s.date / 1000)) AS sale_date_day,
    EXTRACT(YEAR FROM TO_TIMESTAMP(s.date / 1000)) AS sale_year,
    EXTRACT(MONTH FROM TO_TIMESTAMP(s.date / 1000)) AS sale_month,
    EXTRACT(WEEK FROM TO_TIMESTAMP(s.date / 1000)) AS sale_week,
    s.site_id,
    st.name AS site_name,
    s.customer_id,
    s.customer_name,
    si.id AS sale_item_id,
    si.product_id,
    si.product_name,
    p.category_id,
    c.name AS category_name,
    si.unit,
    si.quantity,
    si.price_per_unit AS unit_price,
    si.subtotal AS total_price,
    get_currency_symbol() AS currency
FROM sales s
JOIN sites st ON s.site_id = st.id
JOIN sale_items si ON s.id = si.sale_id
LEFT JOIN products p ON si.product_id = p.id
LEFT JOIN categories c ON p.category_id = c.id;

-- v_sales_daily: Daily sales aggregation per site
CREATE OR REPLACE VIEW v_sales_daily AS
SELECT
    DATE(TO_TIMESTAMP(s.date / 1000)) AS sale_date,
    EXTRACT(YEAR FROM TO_TIMESTAMP(s.date / 1000))::INTEGER AS sale_year,
    EXTRACT(MONTH FROM TO_TIMESTAMP(s.date / 1000))::INTEGER AS sale_month,
    EXTRACT(WEEK FROM TO_TIMESTAMP(s.date / 1000))::INTEGER AS sale_week,
    TO_CHAR(TO_TIMESTAMP(s.date / 1000), 'Day') AS day_of_week,
    s.site_id,
    st.name AS site_name,
    COUNT(DISTINCT s.id) AS nb_sales,
    COUNT(si.id) AS nb_items,
    SUM(si.quantity) AS total_quantity,
    SUM(si.subtotal) AS total_revenue,
    AVG(s.total_amount) AS avg_sale_amount,
    get_currency_symbol() AS currency
FROM sales s
JOIN sites st ON s.site_id = st.id
JOIN sale_items si ON s.id = si.sale_id
GROUP BY
    DATE(TO_TIMESTAMP(s.date / 1000)),
    EXTRACT(YEAR FROM TO_TIMESTAMP(s.date / 1000)),
    EXTRACT(MONTH FROM TO_TIMESTAMP(s.date / 1000)),
    EXTRACT(WEEK FROM TO_TIMESTAMP(s.date / 1000)),
    TO_CHAR(TO_TIMESTAMP(s.date / 1000), 'Day'),
    s.site_id,
    st.name;

-- v_sales_by_product: Sales performance by product
CREATE OR REPLACE VIEW v_sales_by_product AS
SELECT
    si.product_id,
    si.product_name,
    p.category_id,
    c.name AS category_name,
    s.site_id,
    st.name AS site_name,
    COUNT(DISTINCT s.id) AS nb_transactions,
    SUM(si.quantity) AS total_quantity_sold,
    SUM(si.subtotal) AS total_revenue,
    AVG(si.price_per_unit) AS avg_unit_price,
    MIN(TO_TIMESTAMP(s.date / 1000))::DATE AS first_sale_date,
    MAX(TO_TIMESTAMP(s.date / 1000))::DATE AS last_sale_date,
    get_currency_symbol() AS currency
FROM sale_items si
JOIN sales s ON si.sale_id = s.id
JOIN sites st ON s.site_id = st.id
LEFT JOIN products p ON si.product_id = p.id
LEFT JOIN categories c ON p.category_id = c.id
GROUP BY
    si.product_id,
    si.product_name,
    p.category_id,
    c.name,
    s.site_id,
    st.name;

-- v_sales_by_category: Sales by category
CREATE OR REPLACE VIEW v_sales_by_category AS
SELECT
    p.category_id,
    COALESCE(c.name, 'Non categorise') AS category_name,
    s.site_id,
    st.name AS site_name,
    COUNT(DISTINCT s.id) AS nb_transactions,
    COUNT(DISTINCT si.product_id) AS nb_products_sold,
    SUM(si.quantity) AS total_quantity_sold,
    SUM(si.subtotal) AS total_revenue,
    get_currency_symbol() AS currency
FROM sale_items si
JOIN sales s ON si.sale_id = s.id
JOIN sites st ON s.site_id = st.id
LEFT JOIN products p ON si.product_id = p.id
LEFT JOIN categories c ON p.category_id = c.id
GROUP BY
    p.category_id,
    c.name,
    s.site_id,
    st.name;

-- v_sales_by_customer: Customer analysis
CREATE OR REPLACE VIEW v_sales_by_customer AS
SELECT
    s.customer_id,
    s.customer_name,
    s.site_id,
    st.name AS site_name,
    COUNT(DISTINCT s.id) AS nb_purchases,
    SUM(s.total_amount) AS total_spent,
    AVG(s.total_amount) AS avg_basket,
    MIN(TO_TIMESTAMP(s.date / 1000))::DATE AS first_purchase_date,
    MAX(TO_TIMESTAMP(s.date / 1000))::DATE AS last_purchase_date,
    MAX(TO_TIMESTAMP(s.date / 1000))::DATE - MIN(TO_TIMESTAMP(s.date / 1000))::DATE AS customer_lifetime_days,
    get_currency_symbol() AS currency
FROM sales s
JOIN sites st ON s.site_id = st.id
GROUP BY
    s.customer_id,
    s.customer_name,
    s.site_id,
    st.name;

-- v_stock_current: Current stock levels by product and site
CREATE OR REPLACE VIEW v_stock_current AS
SELECT
    p.id AS product_id,
    p.name AS product_name,
    CASE WHEN p.selected_level = 2 THEN pt.level2_name ELSE pt.level1_name END AS unit,
    p.category_id,
    c.name AS category_name,
    pb.site_id,
    st.name AS site_name,
    p.min_stock,
    p.max_stock,
    COALESCE(SUM(pb.remaining_quantity), 0) AS current_stock,
    COALESCE(SUM(pb.remaining_quantity * pb.purchase_price), 0) AS stock_value_cost,
    COALESCE(SUM(pb.remaining_quantity * pp.selling_price), 0) AS stock_value_selling,
    CASE
        WHEN COALESCE(SUM(pb.remaining_quantity), 0) <= p.min_stock THEN 'LOW'
        WHEN p.max_stock > 0 AND COALESCE(SUM(pb.remaining_quantity), 0) >= p.max_stock THEN 'HIGH'
        ELSE 'OK'
    END AS stock_status,
    get_currency_symbol() AS currency
FROM products p
JOIN sites st ON p.site_id = st.id
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN packaging_types pt ON p.packaging_type_id = pt.id
LEFT JOIN purchase_batches pb ON p.id = pb.product_id AND pb.is_exhausted = false AND pb.site_id = p.site_id
LEFT JOIN LATERAL (
    SELECT selling_price
    FROM product_prices
    WHERE product_id = p.id
    ORDER BY effective_date DESC
    LIMIT 1
) pp ON true
WHERE p.is_active = true
GROUP BY
    p.id,
    p.name,
    p.selected_level,
    pt.level1_name,
    pt.level2_name,
    p.category_id,
    c.name,
    pb.site_id,
    st.name,
    p.min_stock,
    p.max_stock,
    pp.selling_price;

-- v_stock_alerts: Stock alerts (low and high)
CREATE OR REPLACE VIEW v_stock_alerts AS
SELECT
    product_id,
    product_name,
    unit,
    category_name,
    site_id,
    site_name,
    min_stock,
    max_stock,
    current_stock,
    stock_status AS alert_type,
    CASE
        WHEN stock_status = 'LOW' THEN min_stock - current_stock
        WHEN stock_status = 'HIGH' THEN current_stock - max_stock
        ELSE 0
    END AS stock_gap,
    stock_value_cost,
    currency
FROM v_stock_current
WHERE stock_status != 'OK';

-- v_stock_valuation: Stock valuation by site and category
CREATE OR REPLACE VIEW v_stock_valuation AS
SELECT
    site_id,
    site_name,
    category_id,
    COALESCE(category_name, 'Non categorise') AS category_name,
    COUNT(DISTINCT product_id) AS nb_products,
    SUM(current_stock) AS total_quantity,
    SUM(stock_value_cost) AS total_value_cost,
    SUM(stock_value_selling) AS total_value_selling,
    SUM(stock_value_selling) - SUM(stock_value_cost) AS potential_margin,
    CASE
        WHEN SUM(stock_value_cost) > 0
        THEN ROUND(((SUM(stock_value_selling) - SUM(stock_value_cost)) / SUM(stock_value_cost) * 100)::NUMERIC, 2)
        ELSE 0
    END AS margin_percent,
    currency
FROM v_stock_current
GROUP BY
    site_id,
    site_name,
    category_id,
    category_name,
    currency;

-- v_purchases_daily: Daily purchases aggregation
CREATE OR REPLACE VIEW v_purchases_daily AS
SELECT
    DATE(TO_TIMESTAMP(pb.purchase_date / 1000)) AS purchase_date,
    EXTRACT(YEAR FROM TO_TIMESTAMP(pb.purchase_date / 1000))::INTEGER AS purchase_year,
    EXTRACT(MONTH FROM TO_TIMESTAMP(pb.purchase_date / 1000))::INTEGER AS purchase_month,
    pb.site_id,
    st.name AS site_name,
    COUNT(pb.id) AS nb_batches,
    COUNT(DISTINCT pb.product_id) AS nb_products,
    SUM(pb.initial_quantity) AS total_quantity,
    SUM(pb.initial_quantity * pb.purchase_price) AS total_amount,
    get_currency_symbol() AS currency
FROM purchase_batches pb
JOIN sites st ON pb.site_id = st.id
GROUP BY
    DATE(TO_TIMESTAMP(pb.purchase_date / 1000)),
    EXTRACT(YEAR FROM TO_TIMESTAMP(pb.purchase_date / 1000)),
    EXTRACT(MONTH FROM TO_TIMESTAMP(pb.purchase_date / 1000)),
    pb.site_id,
    st.name;

-- v_purchases_by_supplier: Supplier analysis
CREATE OR REPLACE VIEW v_purchases_by_supplier AS
SELECT
    COALESCE(NULLIF(pb.supplier_name, ''), 'Non specifie') AS supplier_name,
    pb.site_id,
    st.name AS site_name,
    COUNT(pb.id) AS nb_batches,
    COUNT(DISTINCT pb.product_id) AS nb_products,
    SUM(pb.initial_quantity) AS total_quantity,
    SUM(pb.initial_quantity * pb.purchase_price) AS total_amount,
    MIN(TO_TIMESTAMP(pb.purchase_date / 1000))::DATE AS first_purchase,
    MAX(TO_TIMESTAMP(pb.purchase_date / 1000))::DATE AS last_purchase,
    get_currency_symbol() AS currency
FROM purchase_batches pb
JOIN sites st ON pb.site_id = st.id
GROUP BY
    pb.supplier_name,
    pb.site_id,
    st.name;

-- v_batches_active: Active (non-exhausted) batches
CREATE OR REPLACE VIEW v_batches_active AS
SELECT
    pb.id AS batch_id,
    pb.batch_number,
    pb.product_id,
    p.name AS product_name,
    CASE WHEN p.selected_level = 2 THEN pt.level2_name ELSE pt.level1_name END AS unit,
    pb.site_id,
    st.name AS site_name,
    pb.supplier_name,
    DATE(TO_TIMESTAMP(pb.purchase_date / 1000)) AS purchase_date,
    DATE(TO_TIMESTAMP(pb.expiry_date / 1000)) AS expiry_date,
    pb.initial_quantity,
    pb.remaining_quantity,
    pb.purchase_price,
    pb.remaining_quantity * pb.purchase_price AS batch_value,
    CASE
        WHEN pb.expiry_date IS NULL THEN NULL
        ELSE (DATE(TO_TIMESTAMP(pb.expiry_date / 1000)) - CURRENT_DATE)
    END AS days_until_expiry,
    get_currency_symbol() AS currency
FROM purchase_batches pb
JOIN products p ON pb.product_id = p.id
JOIN sites st ON pb.site_id = st.id
LEFT JOIN packaging_types pt ON p.packaging_type_id = pt.id
WHERE pb.is_exhausted = false;

-- v_expiry_alerts: Expiry alerts (products expiring soon)
CREATE OR REPLACE VIEW v_expiry_alerts AS
SELECT
    batch_id,
    batch_number,
    product_id,
    product_name,
    unit,
    site_id,
    site_name,
    supplier_name,
    purchase_date,
    expiry_date,
    remaining_quantity,
    purchase_price,
    batch_value AS value_at_risk,
    days_until_expiry,
    CASE
        WHEN days_until_expiry <= 0 THEN 'EXPIRED'
        WHEN days_until_expiry <= 30 THEN 'CRITICAL'
        WHEN days_until_expiry <= 90 THEN 'WARNING'
        ELSE 'OK'
    END AS expiry_status,
    currency
FROM v_batches_active
WHERE expiry_date IS NOT NULL
  AND days_until_expiry <= 90;

-- v_expired_batches: Already expired batches with remaining stock
CREATE OR REPLACE VIEW v_expired_batches AS
SELECT
    batch_id,
    batch_number,
    product_id,
    product_name,
    unit,
    site_id,
    site_name,
    expiry_date,
    remaining_quantity AS quantity_lost,
    batch_value AS value_lost,
    ABS(days_until_expiry) AS days_since_expiry,
    currency
FROM v_batches_active
WHERE expiry_date IS NOT NULL
  AND days_until_expiry < 0;

-- v_profit_by_sale: Profit per sale using FIFO batch allocations
CREATE OR REPLACE VIEW v_profit_by_sale AS
SELECT
    s.id AS sale_id,
    DATE(TO_TIMESTAMP(s.date / 1000)) AS sale_date,
    s.site_id,
    st.name AS site_name,
    s.customer_name,
    s.total_amount AS revenue,
    COALESCE(SUM(sba.quantity_allocated * sba.purchase_price_at_allocation), 0) AS cost_of_goods_sold,
    s.total_amount - COALESCE(SUM(sba.quantity_allocated * sba.purchase_price_at_allocation), 0) AS gross_profit,
    CASE
        WHEN s.total_amount > 0
        THEN ROUND((((s.total_amount - COALESCE(SUM(sba.quantity_allocated * sba.purchase_price_at_allocation), 0)) / s.total_amount) * 100)::NUMERIC, 2)
        ELSE 0
    END AS margin_percent,
    get_currency_symbol() AS currency
FROM sales s
JOIN sites st ON s.site_id = st.id
LEFT JOIN sale_items si ON s.id = si.sale_id
LEFT JOIN sale_batch_allocations sba ON si.id = sba.sale_item_id
GROUP BY
    s.id,
    s.date,
    s.site_id,
    st.name,
    s.customer_name,
    s.total_amount;

-- v_profit_by_product: Profitability by product
CREATE OR REPLACE VIEW v_profit_by_product AS
SELECT
    si.product_id,
    si.product_name,
    p.category_id,
    c.name AS category_name,
    s.site_id,
    st.name AS site_name,
    SUM(si.subtotal) AS total_revenue,
    SUM(sba.quantity_allocated * sba.purchase_price_at_allocation) AS total_cogs,
    SUM(si.subtotal) - COALESCE(SUM(sba.quantity_allocated * sba.purchase_price_at_allocation), 0) AS total_profit,
    CASE
        WHEN SUM(si.subtotal) > 0
        THEN ROUND((((SUM(si.subtotal) - COALESCE(SUM(sba.quantity_allocated * sba.purchase_price_at_allocation), 0)) / SUM(si.subtotal)) * 100)::NUMERIC, 2)
        ELSE 0
    END AS margin_percent,
    SUM(si.quantity) AS total_quantity_sold,
    get_currency_symbol() AS currency
FROM sale_items si
JOIN sales s ON si.sale_id = s.id
JOIN sites st ON s.site_id = st.id
LEFT JOIN products p ON si.product_id = p.id
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN sale_batch_allocations sba ON si.id = sba.sale_item_id
GROUP BY
    si.product_id,
    si.product_name,
    p.category_id,
    c.name,
    s.site_id,
    st.name;

-- v_profit_daily: Daily profit summary
CREATE OR REPLACE VIEW v_profit_daily AS
SELECT
    sale_date,
    EXTRACT(YEAR FROM sale_date)::INTEGER AS sale_year,
    EXTRACT(MONTH FROM sale_date)::INTEGER AS sale_month,
    site_id,
    site_name,
    COUNT(*) AS nb_sales,
    SUM(revenue) AS total_revenue,
    SUM(cost_of_goods_sold) AS total_cogs,
    SUM(gross_profit) AS total_profit,
    CASE
        WHEN SUM(revenue) > 0
        THEN ROUND(((SUM(gross_profit) / SUM(revenue)) * 100)::NUMERIC, 2)
        ELSE 0
    END AS margin_percent,
    currency
FROM v_profit_by_sale
GROUP BY
    sale_date,
    site_id,
    site_name,
    currency;

-- v_inventory_discrepancies: Inventory count discrepancies
CREATE OR REPLACE VIEW v_inventory_discrepancies AS
SELECT
    inv.id AS item_id,
    DATE(TO_TIMESTAMP(inv.count_date / 1000)) AS count_date,
    inv.site_id,
    st.name AS site_name,
    inv.product_id,
    p.name AS product_name,
    CASE WHEN p.selected_level = 2 THEN pt.level2_name ELSE pt.level1_name END AS unit,
    inv.theoretical_quantity,
    inv.counted_quantity,
    inv.discrepancy,
    CASE
        WHEN inv.theoretical_quantity > 0
        THEN ROUND(((inv.discrepancy / inv.theoretical_quantity) * 100)::NUMERIC, 2)
        ELSE 0
    END AS discrepancy_percent,
    inv.reason,
    inv.counted_by,
    inv.notes
FROM inventories inv
JOIN sites st ON inv.site_id = st.id
LEFT JOIN products p ON inv.product_id = p.id
LEFT JOIN packaging_types pt ON p.packaging_type_id = pt.id
WHERE inv.discrepancy != 0;

-- v_inventory_summary: Summary per site and date
CREATE OR REPLACE VIEW v_inventory_summary AS
SELECT
    inv.site_id,
    st.name AS site_name,
    DATE(TO_TIMESTAMP(inv.count_date / 1000)) AS count_date,
    COUNT(inv.id) AS nb_items_counted,
    COUNT(CASE WHEN inv.discrepancy != 0 THEN 1 END) AS nb_discrepancies,
    SUM(CASE WHEN inv.discrepancy > 0 THEN inv.discrepancy ELSE 0 END) AS total_surplus,
    SUM(CASE WHEN inv.discrepancy < 0 THEN ABS(inv.discrepancy) ELSE 0 END) AS total_shortage
FROM inventories inv
JOIN sites st ON inv.site_id = st.id
GROUP BY
    inv.site_id,
    st.name,
    DATE(TO_TIMESTAMP(inv.count_date / 1000));

-- v_transfers_summary: Transfer analytics
CREATE OR REPLACE VIEW v_transfers_summary AS
SELECT
    ptr.id AS transfer_id,
    DATE(TO_TIMESTAMP(ptr.date / 1000)) AS transfer_date,
    EXTRACT(YEAR FROM TO_TIMESTAMP(ptr.date / 1000))::INTEGER AS transfer_year,
    EXTRACT(MONTH FROM TO_TIMESTAMP(ptr.date / 1000))::INTEGER AS transfer_month,
    ptr.from_site_id,
    fs.name AS from_site_name,
    ptr.to_site_id,
    ts.name AS to_site_name,
    ptr.product_id,
    p.name AS product_name,
    CASE WHEN p.selected_level = 2 THEN pt.level2_name ELSE pt.level1_name END AS unit,
    ptr.quantity,
    ptr.notes
FROM product_transfers ptr
JOIN sites fs ON ptr.from_site_id = fs.id
JOIN sites ts ON ptr.to_site_id = ts.id
JOIN products p ON ptr.product_id = p.id
LEFT JOIN packaging_types pt ON p.packaging_type_id = pt.id;

-- v_movements_daily: Daily stock movements
CREATE OR REPLACE VIEW v_movements_daily AS
SELECT
    DATE(TO_TIMESTAMP(sm.date / 1000)) AS movement_date,
    EXTRACT(YEAR FROM TO_TIMESTAMP(sm.date / 1000))::INTEGER AS movement_year,
    EXTRACT(MONTH FROM TO_TIMESTAMP(sm.date / 1000))::INTEGER AS movement_month,
    sm.site_id,
    st.name AS site_name,
    sm.type AS movement_type,
    COUNT(*) AS nb_movements,
    SUM(CASE WHEN UPPER(sm.type) = 'IN' THEN sm.quantity ELSE 0 END) AS total_in,
    SUM(CASE WHEN UPPER(sm.type) = 'OUT' THEN sm.quantity ELSE 0 END) AS total_out,
    SUM(sm.quantity * sm.purchase_price_at_movement) AS total_value,
    get_currency_symbol() AS currency
FROM stock_movements sm
JOIN sites st ON sm.site_id = st.id
GROUP BY
    DATE(TO_TIMESTAMP(sm.date / 1000)),
    EXTRACT(YEAR FROM TO_TIMESTAMP(sm.date / 1000)),
    EXTRACT(MONTH FROM TO_TIMESTAMP(sm.date / 1000)),
    sm.site_id,
    st.name,
    sm.type;

-- v_kpi_current: Current key metrics per site
CREATE OR REPLACE VIEW v_kpi_current AS
WITH
today_sales AS (
    SELECT
        site_id,
        COUNT(*) AS sales_today,
        COALESCE(SUM(total_amount), 0) AS revenue_today
    FROM sales
    WHERE DATE(TO_TIMESTAMP(date / 1000)) = CURRENT_DATE
    GROUP BY site_id
),
week_sales AS (
    SELECT
        site_id,
        COUNT(*) AS sales_week,
        COALESCE(SUM(total_amount), 0) AS revenue_week
    FROM sales
    WHERE DATE(TO_TIMESTAMP(date / 1000)) >= DATE_TRUNC('week', CURRENT_DATE)
    GROUP BY site_id
),
month_sales AS (
    SELECT
        site_id,
        COUNT(*) AS sales_month,
        COALESCE(SUM(total_amount), 0) AS revenue_month
    FROM sales
    WHERE DATE(TO_TIMESTAMP(date / 1000)) >= DATE_TRUNC('month', CURRENT_DATE)
    GROUP BY site_id
),
stock_stats AS (
    SELECT
        site_id,
        SUM(current_stock) AS total_stock_qty,
        SUM(stock_value_cost) AS total_stock_value,
        COUNT(CASE WHEN stock_status = 'LOW' THEN 1 END) AS low_stock_alerts,
        COUNT(CASE WHEN stock_status = 'HIGH' THEN 1 END) AS high_stock_alerts
    FROM v_stock_current
    GROUP BY site_id
),
expiry_stats AS (
    SELECT
        site_id,
        COUNT(CASE WHEN expiry_status = 'EXPIRED' THEN 1 END) AS expired_batches,
        COUNT(CASE WHEN expiry_status = 'CRITICAL' THEN 1 END) AS critical_expiry_batches,
        COUNT(CASE WHEN expiry_status = 'WARNING' THEN 1 END) AS warning_expiry_batches
    FROM v_expiry_alerts
    GROUP BY site_id
)
SELECT
    st.id AS site_id,
    st.name AS site_name,
    -- Today
    COALESCE(ts.sales_today, 0) AS sales_today,
    COALESCE(ts.revenue_today, 0) AS revenue_today,
    -- This week
    COALESCE(ws.sales_week, 0) AS sales_this_week,
    COALESCE(ws.revenue_week, 0) AS revenue_this_week,
    -- This month
    COALESCE(ms.sales_month, 0) AS sales_this_month,
    COALESCE(ms.revenue_month, 0) AS revenue_this_month,
    -- Stock
    COALESCE(ss.total_stock_qty, 0) AS total_stock_quantity,
    COALESCE(ss.total_stock_value, 0) AS total_stock_value,
    COALESCE(ss.low_stock_alerts, 0) AS low_stock_alerts,
    COALESCE(ss.high_stock_alerts, 0) AS high_stock_alerts,
    -- Expiry
    COALESCE(es.expired_batches, 0) AS expired_batches,
    COALESCE(es.critical_expiry_batches, 0) AS critical_expiry_alerts,
    COALESCE(es.warning_expiry_batches, 0) AS warning_expiry_alerts,
    -- Meta
    CURRENT_TIMESTAMP AS last_updated,
    get_currency_symbol() AS currency
FROM sites st
LEFT JOIN today_sales ts ON st.id = ts.site_id
LEFT JOIN week_sales ws ON st.id = ws.site_id
LEFT JOIN month_sales ms ON st.id = ms.site_id
LEFT JOIN stock_stats ss ON st.id = ss.site_id
LEFT JOIN expiry_stats es ON st.id = es.site_id
WHERE st.is_active = true;

-- v_stock_turnover: Stock turnover analysis (last 30 days)
CREATE OR REPLACE VIEW v_stock_turnover AS
WITH sales_30d AS (
    SELECT
        si.product_id,
        s.site_id,
        SUM(si.quantity) AS qty_sold_30d
    FROM sale_items si
    JOIN sales s ON si.sale_id = s.id
    WHERE DATE(TO_TIMESTAMP(s.date / 1000)) >= CURRENT_DATE - INTERVAL '30 days'
    GROUP BY si.product_id, s.site_id
)
SELECT
    sc.product_id,
    sc.product_name,
    sc.unit,
    sc.category_name,
    sc.site_id,
    sc.site_name,
    sc.current_stock,
    COALESCE(s30.qty_sold_30d, 0) AS qty_sold_last_30_days,
    CASE
        WHEN sc.current_stock > 0 AND COALESCE(s30.qty_sold_30d, 0) > 0
        THEN ROUND((sc.current_stock / (s30.qty_sold_30d / 30.0))::NUMERIC, 1)
        ELSE NULL
    END AS days_of_stock,
    CASE
        WHEN sc.current_stock > 0 AND COALESCE(s30.qty_sold_30d, 0) > 0
        THEN ROUND(((s30.qty_sold_30d / 30.0 * 365) / sc.current_stock)::NUMERIC, 2)
        ELSE 0
    END AS annual_turnover_rate,
    sc.currency
FROM v_stock_current sc
LEFT JOIN sales_30d s30 ON sc.product_id = s30.product_id AND sc.site_id = s30.site_id;

-- ============================================================================
-- 10. SYSTEME DE MIGRATION AUTOMATIQUE
-- ============================================================================

-- Table de suivi des migrations
CREATE TABLE IF NOT EXISTS schema_migrations (
    name TEXT PRIMARY KEY,
    checksum TEXT,
    applied_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    applied_by TEXT NOT NULL DEFAULT 'system',
    success BOOLEAN NOT NULL DEFAULT TRUE,
    execution_time_ms INTEGER,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_schema_migrations_applied_at ON schema_migrations(applied_at);

COMMENT ON TABLE schema_migrations IS 'Tracks all applied database migrations';
COMMENT ON COLUMN schema_migrations.name IS 'Unique migration identifier (e.g., 2026011701_migration_system)';
COMMENT ON COLUMN schema_migrations.checksum IS 'MD5 hash of the migration SQL for integrity verification';
COMMENT ON COLUMN schema_migrations.applied_at IS 'Timestamp when migration was applied (ms since epoch)';
COMMENT ON COLUMN schema_migrations.applied_by IS 'User or system that applied the migration';
COMMENT ON COLUMN schema_migrations.success IS 'Whether the migration executed successfully';
COMMENT ON COLUMN schema_migrations.execution_time_ms IS 'Time taken to execute the migration in milliseconds';
COMMENT ON COLUMN schema_migrations.error_message IS 'Error message if migration failed';

-- Fonction pour appliquer une migration (SECURITY DEFINER pour DDL)
CREATE OR REPLACE FUNCTION apply_migration(
    p_name TEXT,
    p_sql TEXT,
    p_checksum TEXT DEFAULT NULL,
    p_applied_by TEXT DEFAULT 'app'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_start_time BIGINT;
    v_end_time BIGINT;
    v_execution_time INTEGER;
    v_result JSONB;
BEGIN
    -- Verifier si la migration a deja ete appliquee
    IF EXISTS (SELECT 1 FROM schema_migrations WHERE name = p_name AND success = TRUE) THEN
        RETURN jsonb_build_object(
            'success', FALSE,
            'already_applied', TRUE,
            'message', format('Migration %s has already been applied', p_name)
        );
    END IF;

    -- Enregistrer le temps de debut
    v_start_time := EXTRACT(EPOCH FROM clock_timestamp())::BIGINT * 1000;

    BEGIN
        -- Executer le SQL de la migration
        EXECUTE p_sql;

        -- Calculer le temps d'execution
        v_end_time := EXTRACT(EPOCH FROM clock_timestamp())::BIGINT * 1000;
        v_execution_time := (v_end_time - v_start_time)::INTEGER;

        -- Enregistrer la migration comme reussie
        INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms)
        VALUES (p_name, p_checksum, p_applied_by, TRUE, v_execution_time)
        ON CONFLICT (name) DO UPDATE SET
            checksum = EXCLUDED.checksum,
            applied_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
            applied_by = EXCLUDED.applied_by,
            success = TRUE,
            execution_time_ms = EXCLUDED.execution_time_ms,
            error_message = NULL;

        v_result := jsonb_build_object(
            'success', TRUE,
            'already_applied', FALSE,
            'message', format('Migration %s applied successfully', p_name),
            'execution_time_ms', v_execution_time
        );

    EXCEPTION WHEN OTHERS THEN
        -- Calculer le temps d'execution meme en cas d'erreur
        v_end_time := EXTRACT(EPOCH FROM clock_timestamp())::BIGINT * 1000;
        v_execution_time := (v_end_time - v_start_time)::INTEGER;

        -- Enregistrer l'echec de la migration
        INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms, error_message)
        VALUES (p_name, p_checksum, p_applied_by, FALSE, v_execution_time, SQLERRM)
        ON CONFLICT (name) DO UPDATE SET
            checksum = EXCLUDED.checksum,
            applied_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
            applied_by = EXCLUDED.applied_by,
            success = FALSE,
            execution_time_ms = EXCLUDED.execution_time_ms,
            error_message = EXCLUDED.error_message;

        v_result := jsonb_build_object(
            'success', FALSE,
            'already_applied', FALSE,
            'message', format('Migration %s failed: %s', p_name, SQLERRM),
            'execution_time_ms', v_execution_time,
            'error', SQLERRM
        );
    END;

    RETURN v_result;
END;
$$;

COMMENT ON FUNCTION apply_migration IS 'Applies a database migration and records the result. Uses SECURITY DEFINER to allow DDL operations.';

-- Fonction pour lister les migrations appliquees
CREATE OR REPLACE FUNCTION get_applied_migrations()
RETURNS TABLE (
    name TEXT,
    checksum TEXT,
    applied_at BIGINT,
    applied_by TEXT,
    success BOOLEAN,
    execution_time_ms INTEGER,
    error_message TEXT
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        sm.name,
        sm.checksum,
        sm.applied_at,
        sm.applied_by,
        sm.success,
        sm.execution_time_ms,
        sm.error_message
    FROM schema_migrations sm
    ORDER BY sm.name;
$$;

COMMENT ON FUNCTION get_applied_migrations IS 'Returns all migrations that have been applied to the database';

-- Fonction pour verifier si une migration a ete appliquee
CREATE OR REPLACE FUNCTION is_migration_applied(p_name TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM schema_migrations
        WHERE name = p_name AND success = TRUE
    );
$$;

COMMENT ON FUNCTION is_migration_applied IS 'Checks if a specific migration has been successfully applied';

-- ============================================================================
-- 11. SYSTEME DE VERSIONING APP/DB
-- ============================================================================

-- Table de version du schema (une seule ligne)
CREATE TABLE IF NOT EXISTS schema_version (
    id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    schema_version INTEGER NOT NULL DEFAULT 1,
    min_app_version INTEGER NOT NULL DEFAULT 1,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_by TEXT NOT NULL DEFAULT 'system'
);

COMMENT ON TABLE schema_version IS 'Single-row table tracking database schema version and minimum required app version';
COMMENT ON COLUMN schema_version.schema_version IS 'Current database schema version (incremented with breaking changes)';
COMMENT ON COLUMN schema_version.min_app_version IS 'Minimum app schema version required to use this database';

-- Fonction pour recuperer la version
CREATE OR REPLACE FUNCTION get_schema_version()
RETURNS TABLE (
    schema_version INTEGER,
    min_app_version INTEGER,
    updated_at BIGINT
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        sv.schema_version,
        sv.min_app_version,
        sv.updated_at
    FROM schema_version sv
    WHERE sv.id = 1;
$$;

COMMENT ON FUNCTION get_schema_version IS 'Returns current schema version and minimum required app version';

-- Fonction pour mettre a jour la version (utilisee par les migrations)
CREATE OR REPLACE FUNCTION update_schema_version(
    p_schema_version INTEGER,
    p_min_app_version INTEGER DEFAULT NULL,
    p_updated_by TEXT DEFAULT 'migration'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_current_schema INTEGER;
    v_current_min_app INTEGER;
BEGIN
    SELECT sv.schema_version, sv.min_app_version
    INTO v_current_schema, v_current_min_app
    FROM schema_version sv WHERE sv.id = 1;

    IF NOT FOUND THEN
        INSERT INTO schema_version (schema_version, min_app_version, updated_by)
        VALUES (p_schema_version, COALESCE(p_min_app_version, p_schema_version), p_updated_by);
        RETURN jsonb_build_object(
            'success', TRUE,
            'message', 'Schema version initialized',
            'schema_version', p_schema_version,
            'min_app_version', COALESCE(p_min_app_version, p_schema_version)
        );
    END IF;

    IF p_schema_version < v_current_schema THEN
        RETURN jsonb_build_object(
            'success', FALSE,
            'message', format('Cannot downgrade schema version from %s to %s', v_current_schema, p_schema_version)
        );
    END IF;

    UPDATE schema_version SET
        schema_version = p_schema_version,
        min_app_version = COALESCE(p_min_app_version, min_app_version),
        updated_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
        updated_by = p_updated_by
    WHERE id = 1;

    RETURN jsonb_build_object(
        'success', TRUE,
        'message', 'Schema version updated',
        'schema_version', p_schema_version,
        'min_app_version', COALESCE(p_min_app_version, v_current_min_app)
    );
END;
$$;

COMMENT ON FUNCTION update_schema_version IS 'Updates the schema version. Used by migrations to bump version numbers.';

-- ============================================================================
-- 12. ROW LEVEL SECURITY (RLS)
-- ============================================================================

-- Enable RLS on all tables
ALTER TABLE sites ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE packaging_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_prices ENABLE ROW LEVEL SECURITY;
ALTER TABLE purchase_batches ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventories ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_transfers ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales ENABLE ROW LEVEL SECURITY;
ALTER TABLE sale_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE sale_batch_allocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE schema_migrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE sync_queue ENABLE ROW LEVEL SECURITY;

-- app_config has RLS with NO policies = service_role only
ALTER TABLE app_config ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- RLS POLICIES: All authenticated users can READ and WRITE all data
-- Security is enforced at the application level through the permissions system
-- ============================================================================

-- SITES
CREATE POLICY "sites_select" ON sites FOR SELECT TO authenticated USING (true);
CREATE POLICY "sites_insert" ON sites FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "sites_update" ON sites FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "sites_delete" ON sites FOR DELETE TO authenticated USING (true);

-- CATEGORIES
CREATE POLICY "categories_select" ON categories FOR SELECT TO authenticated USING (true);
CREATE POLICY "categories_insert" ON categories FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "categories_update" ON categories FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "categories_delete" ON categories FOR DELETE TO authenticated USING (true);

-- PACKAGING_TYPES
CREATE POLICY "packaging_types_select" ON packaging_types FOR SELECT TO authenticated USING (true);
CREATE POLICY "packaging_types_insert" ON packaging_types FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "packaging_types_update" ON packaging_types FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "packaging_types_delete" ON packaging_types FOR DELETE TO authenticated USING (true);

-- APP_USERS
CREATE POLICY "users_select_policy" ON app_users FOR SELECT TO authenticated USING (true);
CREATE POLICY "users_insert" ON app_users FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "users_update" ON app_users FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "users_delete" ON app_users FOR DELETE TO authenticated USING (true);

-- USER_PERMISSIONS
CREATE POLICY "user_permissions_select" ON user_permissions FOR SELECT TO authenticated USING (true);
CREATE POLICY "user_permissions_insert" ON user_permissions FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "user_permissions_update" ON user_permissions FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "user_permissions_delete" ON user_permissions FOR DELETE TO authenticated USING (true);

-- CUSTOMERS
CREATE POLICY "customers_select" ON customers FOR SELECT TO authenticated USING (true);
CREATE POLICY "customers_insert" ON customers FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "customers_update" ON customers FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "customers_delete" ON customers FOR DELETE TO authenticated USING (true);

-- PRODUCTS
CREATE POLICY "products_select" ON products FOR SELECT TO authenticated USING (true);
CREATE POLICY "products_insert" ON products FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "products_update" ON products FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "products_delete" ON products FOR DELETE TO authenticated USING (true);

-- PRODUCT_PRICES
CREATE POLICY "product_prices_select" ON product_prices FOR SELECT TO authenticated USING (true);
CREATE POLICY "product_prices_insert" ON product_prices FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "product_prices_update" ON product_prices FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "product_prices_delete" ON product_prices FOR DELETE TO authenticated USING (true);

-- PURCHASE_BATCHES
CREATE POLICY "purchase_batches_select" ON purchase_batches FOR SELECT TO authenticated USING (true);
CREATE POLICY "purchase_batches_insert" ON purchase_batches FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "purchase_batches_update" ON purchase_batches FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "purchase_batches_delete" ON purchase_batches FOR DELETE TO authenticated USING (true);

-- STOCK_MOVEMENTS
CREATE POLICY "stock_movements_select" ON stock_movements FOR SELECT TO authenticated USING (true);
CREATE POLICY "stock_movements_insert" ON stock_movements FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "stock_movements_update" ON stock_movements FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "stock_movements_delete" ON stock_movements FOR DELETE TO authenticated USING (true);

-- INVENTORIES
CREATE POLICY "inventories_select" ON inventories FOR SELECT TO authenticated USING (true);
CREATE POLICY "inventories_insert" ON inventories FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "inventories_update" ON inventories FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "inventories_delete" ON inventories FOR DELETE TO authenticated USING (true);

-- INVENTORY_ITEMS
CREATE POLICY "inventory_items_all" ON inventory_items FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- PRODUCT_TRANSFERS
CREATE POLICY "product_transfers_select" ON product_transfers FOR SELECT TO authenticated USING (true);
CREATE POLICY "product_transfers_insert" ON product_transfers FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "product_transfers_update" ON product_transfers FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "product_transfers_delete" ON product_transfers FOR DELETE TO authenticated USING (true);

-- SALES
CREATE POLICY "sales_select" ON sales FOR SELECT TO authenticated USING (true);
CREATE POLICY "sales_insert" ON sales FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "sales_update" ON sales FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "sales_delete" ON sales FOR DELETE TO authenticated USING (true);

-- SALE_ITEMS
CREATE POLICY "sale_items_select" ON sale_items FOR SELECT TO authenticated USING (true);
CREATE POLICY "sale_items_insert" ON sale_items FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "sale_items_update" ON sale_items FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "sale_items_delete" ON sale_items FOR DELETE TO authenticated USING (true);

-- SALE_BATCH_ALLOCATIONS
CREATE POLICY "sale_batch_allocations_select" ON sale_batch_allocations FOR SELECT TO authenticated USING (true);
CREATE POLICY "sale_batch_allocations_insert" ON sale_batch_allocations FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "sale_batch_allocations_update" ON sale_batch_allocations FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "sale_batch_allocations_delete" ON sale_batch_allocations FOR DELETE TO authenticated USING (true);

-- AUDIT_HISTORY
CREATE POLICY "audit_history_select" ON audit_history FOR SELECT TO authenticated USING (true);
CREATE POLICY "audit_history_insert" ON audit_history FOR INSERT TO authenticated WITH CHECK (true);

-- SCHEMA_MIGRATIONS
CREATE POLICY "schema_migrations_select" ON schema_migrations FOR SELECT TO authenticated USING (true);
CREATE POLICY "schema_migrations_insert" ON schema_migrations FOR INSERT TO authenticated WITH CHECK (true);

-- SYNC_QUEUE - users need full access to their sync queue
CREATE POLICY "sync_queue_all" ON sync_queue FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ============================================================================
-- GRANT SELECT ON ALL VIEWS TO authenticated
-- ============================================================================
GRANT SELECT ON current_stock TO authenticated;
GRANT SELECT ON transaction_flat_view TO authenticated;
GRANT SELECT ON v_sales_detail TO authenticated;
GRANT SELECT ON v_sales_daily TO authenticated;
GRANT SELECT ON v_sales_by_product TO authenticated;
GRANT SELECT ON v_sales_by_category TO authenticated;
GRANT SELECT ON v_sales_by_customer TO authenticated;
GRANT SELECT ON v_stock_current TO authenticated;
GRANT SELECT ON v_stock_alerts TO authenticated;
GRANT SELECT ON v_stock_valuation TO authenticated;
GRANT SELECT ON v_purchases_daily TO authenticated;
GRANT SELECT ON v_purchases_by_supplier TO authenticated;
GRANT SELECT ON v_batches_active TO authenticated;
GRANT SELECT ON v_expiry_alerts TO authenticated;
GRANT SELECT ON v_expired_batches TO authenticated;
GRANT SELECT ON v_profit_by_sale TO authenticated;
GRANT SELECT ON v_profit_by_product TO authenticated;
GRANT SELECT ON v_profit_daily TO authenticated;
GRANT SELECT ON v_inventory_discrepancies TO authenticated;
GRANT SELECT ON v_inventory_summary TO authenticated;
GRANT SELECT ON v_transfers_summary TO authenticated;
GRANT SELECT ON v_movements_daily TO authenticated;
GRANT SELECT ON v_kpi_current TO authenticated;
GRANT SELECT ON v_stock_turnover TO authenticated;

-- Grant execute on helper function
GRANT EXECUTE ON FUNCTION get_currency_symbol() TO authenticated;

-- ============================================================================
-- DONNEES INITIALES (EXEMPLES)
-- ============================================================================

-- Inserer un site par defaut
INSERT INTO sites (name, created_by) VALUES ('Site Principal', 'system');

-- Inserer quelques categories par defaut
INSERT INTO categories (name, created_by) VALUES
    ('Antibiotiques', 'system'),
    ('Antalgiques', 'system'),
    ('Anti-inflammatoires', 'system'),
    ('Vitamines', 'system');

-- Inserer quelques types de conditionnement
INSERT INTO packaging_types (name, level1_name, level2_name, default_conversion_factor, created_by) VALUES
    ('Boite/Comprimes', 'Boite', 'Comprimes', 30, 'system'),
    ('Flacon/ml', 'Flacon', 'ml', 100, 'system'),
    ('Units', 'Units', NULL, NULL, 'system');

-- Creer un utilisateur admin par defaut (password: admin123)
-- Hash BCrypt de "admin": $2a$12$hNbp4sTlxIZe8pxNbi3uuOtBxZ3K7iiKolTTCDOidr3zalaWNbVUG
INSERT INTO app_users (username, password, full_name, is_admin, is_active, created_by) VALUES
    ('admin', '$2a$12$hNbp4sTlxIZe8pxNbi3uuOtBxZ3K7iiKolTTCDOidr3zalaWNbVUG', 'Administrateur', TRUE, TRUE, 'system');

-- Insert default configuration entries
INSERT INTO app_config (key, value, description)
VALUES
    ('recovery_secret_key', NULL, 'Secret key required to create recovery admin. Set via SQL Editor if needed.'),
    ('instance_name', 'MediStock', 'Name of this MediStock instance'),
    ('setup_completed_at', NULL, 'Timestamp when initial setup was completed'),
    ('currency_symbol', 'F', 'Default currency symbol for reports')
ON CONFLICT (key) DO NOTHING;

-- Enregistrer toutes les migrations comme deja appliquees (car init.sql les inclut)
INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms)
VALUES
    ('2025122601_uuid_migration', NULL, 'init', TRUE, NULL),
    ('2025122602_created_updated_by', NULL, 'init', TRUE, NULL),
    ('2025122603_audit_triggers', NULL, 'init', TRUE, NULL),
    ('2025122604_audit_trigger_null_site', NULL, 'init', TRUE, NULL),
    ('2025122605_add_product_description', NULL, 'init', TRUE, NULL),
    ('2025122605_transaction_flat_view', NULL, 'init', TRUE, NULL),
    ('2026010501_schema_cleanup', NULL, 'init', TRUE, NULL),
    ('2026011701_migration_system', NULL, 'init', TRUE, NULL),
    ('2026011702_schema_version', NULL, 'init', TRUE, NULL),
    ('2026011801_sync_tracking', NULL, 'init', TRUE, NULL),
    ('2026011901_remove_audit_triggers', NULL, 'init', TRUE, NULL),
    ('2026012101_add_client_id', NULL, 'init', TRUE, NULL),
    ('2026012201_inventory_and_schema_updates', NULL, 'init', TRUE, NULL),
    ('2026012202_align_room_schema', NULL, 'init', TRUE, NULL),
    ('2026012203_enhance_sync_queue', NULL, 'init', TRUE, NULL),
    ('2026012301_add_is_active_columns', NULL, 'init', TRUE, NULL),
    ('2026012302_notification_events', NULL, 'init', TRUE, NULL),
    ('2026012401_auth_migration_tracking', NULL, 'init', TRUE, NULL),
    ('2026012402_app_config_table', NULL, 'init', TRUE, NULL),
    ('2026012403_recovery_admin_function', NULL, 'init', TRUE, NULL),
    ('2026012404_supabase_auth_rls', NULL, 'init', TRUE, NULL),
    ('2026012405_fix_rls_security', NULL, 'init', TRUE, NULL),
    ('2026012406_fix_is_admin_boolean', NULL, 'init', TRUE, NULL),
    ('2026012407_fix_rls_allow_sync', NULL, 'init', TRUE, NULL),
    ('2026012408_fix_is_active_boolean_type', NULL, 'init', TRUE, NULL),
    ('2026012501_add_user_language', NULL, 'init', TRUE, NULL),
    ('2026012502_remove_product_unit', NULL, 'init', TRUE, NULL),
    ('20260124001000_reporting_views', NULL, 'init', TRUE, NULL),
    ('20260125000100_record_reporting_views', NULL, 'init', TRUE, NULL),
    ('20260125001000_reporting_readonly_user', NULL, 'init', TRUE, NULL),
    ('20260125002000_seed_demo_data', NULL, 'init', TRUE, NULL)
ON CONFLICT (name) DO NOTHING;

-- Initialiser la version du schema (version 7 = all migrations applied)
INSERT INTO schema_version (schema_version, min_app_version, updated_by)
VALUES (7, 2, 'init')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- FIN DU SCHEMA
-- ============================================================================

-- Afficher un message de confirmation
DO $$
BEGIN
    RAISE NOTICE 'Medistock database schema created successfully!';
    RAISE NOTICE 'Total tables: 22 (including schema_migrations, schema_version, sync_queue, notification_events_local, inventory_items, app_config)';
    RAISE NOTICE 'Total views: 24 (current_stock, transaction_flat_view, plus 22 reporting views)';
    RAISE NOTICE 'Default admin user: admin / admin123';
    RAISE NOTICE 'Migration system initialized with 31 migrations marked as applied';
    RAISE NOTICE 'Schema version: 7, Min app version: 2';
    RAISE NOTICE 'All tables include client_id column for Realtime support';
    RAISE NOTICE 'RLS enabled on all tables with permissive policies for authenticated users';
    RAISE NOTICE 'app_config table has RLS with NO policies (service_role only)';
END $$;
