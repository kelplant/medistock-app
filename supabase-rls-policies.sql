-- ============================================================================
-- MEDISTOCK ROW LEVEL SECURITY (RLS) POLICIES
-- Configuration de la sécurité au niveau des lignes pour Supabase
-- ============================================================================

-- IMPORTANT: Ces politiques permettent un accès complet via la clé anon pour le moment
-- Dans une version production, vous devrez les adapter selon vos besoins de sécurité

-- ============================================================================
-- 1. ACTIVER RLS SUR TOUTES LES TABLES
-- ============================================================================

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
ALTER TABLE product_transfers ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales ENABLE ROW LEVEL SECURITY;
ALTER TABLE sale_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE sale_batch_allocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_sales ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_history ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- 2. POLITIQUES PERMISSIVES POUR LE DÉVELOPPEMENT
-- ============================================================================
-- Ces politiques permettent un accès complet pour le développement
-- À ADAPTER selon vos besoins de sécurité en production

-- Sites
CREATE POLICY "Allow all operations on sites"
  ON sites FOR ALL
  USING (true)
  WITH CHECK (true);

-- Categories
CREATE POLICY "Allow all operations on categories"
  ON categories FOR ALL
  USING (true)
  WITH CHECK (true);

-- Packaging Types
CREATE POLICY "Allow all operations on packaging_types"
  ON packaging_types FOR ALL
  USING (true)
  WITH CHECK (true);

-- App Users
CREATE POLICY "Allow all operations on app_users"
  ON app_users FOR ALL
  USING (true)
  WITH CHECK (true);

-- User Permissions
CREATE POLICY "Allow all operations on user_permissions"
  ON user_permissions FOR ALL
  USING (true)
  WITH CHECK (true);

-- Customers
CREATE POLICY "Allow all operations on customers"
  ON customers FOR ALL
  USING (true)
  WITH CHECK (true);

-- Products
CREATE POLICY "Allow all operations on products"
  ON products FOR ALL
  USING (true)
  WITH CHECK (true);

-- Product Prices
CREATE POLICY "Allow all operations on product_prices"
  ON product_prices FOR ALL
  USING (true)
  WITH CHECK (true);

-- Purchase Batches
CREATE POLICY "Allow all operations on purchase_batches"
  ON purchase_batches FOR ALL
  USING (true)
  WITH CHECK (true);

-- Stock Movements
CREATE POLICY "Allow all operations on stock_movements"
  ON stock_movements FOR ALL
  USING (true)
  WITH CHECK (true);

-- Inventories
CREATE POLICY "Allow all operations on inventories"
  ON inventories FOR ALL
  USING (true)
  WITH CHECK (true);

-- Product Transfers
CREATE POLICY "Allow all operations on product_transfers"
  ON product_transfers FOR ALL
  USING (true)
  WITH CHECK (true);

-- Sales
CREATE POLICY "Allow all operations on sales"
  ON sales FOR ALL
  USING (true)
  WITH CHECK (true);

-- Sale Items
CREATE POLICY "Allow all operations on sale_items"
  ON sale_items FOR ALL
  USING (true)
  WITH CHECK (true);

-- Sale Batch Allocations
CREATE POLICY "Allow all operations on sale_batch_allocations"
  ON sale_batch_allocations FOR ALL
  USING (true)
  WITH CHECK (true);

-- Product Sales
CREATE POLICY "Allow all operations on product_sales"
  ON product_sales FOR ALL
  USING (true)
  WITH CHECK (true);

-- Audit History
CREATE POLICY "Allow all operations on audit_history"
  ON audit_history FOR ALL
  USING (true)
  WITH CHECK (true);

-- ============================================================================
-- 3. EXEMPLE DE POLITIQUES RESTRICTIVES (COMMENTÉES)
-- ============================================================================
-- Décommentez et adaptez ces politiques pour une sécurité plus stricte en production

/*
-- Exemple: Accès aux produits limité par site
-- Nécessite que l'utilisateur ait un claim JWT avec son site_id

DROP POLICY IF EXISTS "Allow all operations on products" ON products;

CREATE POLICY "Users can view products from their site"
  ON products FOR SELECT
  USING (
    site_id = (current_setting('request.jwt.claims', true)::json->>'site_id')::bigint
  );

CREATE POLICY "Users can create products for their site"
  ON products FOR INSERT
  WITH CHECK (
    site_id = (current_setting('request.jwt.claims', true)::json->>'site_id')::bigint
  );

CREATE POLICY "Users can update products from their site"
  ON products FOR UPDATE
  USING (
    site_id = (current_setting('request.jwt.claims', true)::json->>'site_id')::bigint
  )
  WITH CHECK (
    site_id = (current_setting('request.jwt.claims', true)::json->>'site_id')::bigint
  );

CREATE POLICY "Users can delete products from their site"
  ON products FOR DELETE
  USING (
    site_id = (current_setting('request.jwt.claims', true)::json->>'site_id')::bigint
  );
*/

/*
-- Exemple: Accès aux ventes limité par site

DROP POLICY IF EXISTS "Allow all operations on sales" ON sales;

CREATE POLICY "Users can view sales from their site"
  ON sales FOR SELECT
  USING (
    site_id = (current_setting('request.jwt.claims', true)::json->>'site_id')::bigint
  );

CREATE POLICY "Users can create sales for their site"
  ON sales FOR INSERT
  WITH CHECK (
    site_id = (current_setting('request.jwt.claims', true)::json->>'site_id')::bigint
  );
*/

/*
-- Exemple: Les admins ont accès à tout

CREATE POLICY "Admins have full access to all tables"
  ON products FOR ALL
  USING (
    (current_setting('request.jwt.claims', true)::json->>'is_admin')::boolean = true
  )
  WITH CHECK (
    (current_setting('request.jwt.claims', true)::json->>'is_admin')::boolean = true
  );
*/

-- ============================================================================
-- 4. FONCTION HELPER POUR VÉRIFIER LES PERMISSIONS
-- ============================================================================

-- Fonction pour vérifier si un utilisateur a une permission spécifique
CREATE OR REPLACE FUNCTION has_permission(
  p_user_id BIGINT,
  p_module TEXT,
  p_action TEXT -- 'view', 'create', 'edit', 'delete'
)
RETURNS BOOLEAN AS $$
DECLARE
  v_has_permission BOOLEAN;
  v_is_admin BOOLEAN;
BEGIN
  -- Vérifier si l'utilisateur est admin
  SELECT is_admin INTO v_is_admin
  FROM app_users
  WHERE id = p_user_id;

  -- Les admins ont toutes les permissions
  IF v_is_admin THEN
    RETURN TRUE;
  END IF;

  -- Vérifier la permission spécifique
  SELECT
    CASE p_action
      WHEN 'view' THEN can_view
      WHEN 'create' THEN can_create
      WHEN 'edit' THEN can_edit
      WHEN 'delete' THEN can_delete
      ELSE FALSE
    END INTO v_has_permission
  FROM user_permissions
  WHERE user_id = p_user_id AND module = p_module;

  RETURN COALESCE(v_has_permission, FALSE);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- NOTES IMPORTANTES
-- ============================================================================

-- 1. DÉVELOPPEMENT vs PRODUCTION:
--    Les politiques actuelles sont PERMISSIVES (accès total) pour faciliter le développement.
--    En production, décommentez et adaptez les politiques restrictives ci-dessus.

-- 2. AUTHENTIFICATION:
--    Pour utiliser les politiques restrictives, vous devrez implémenter l'authentification
--    Supabase et générer des JWT tokens avec les claims nécessaires (site_id, is_admin, etc.)

-- 3. SERVICE ROLE:
--    Les requêtes faites avec la clé service_role bypassent RLS automatiquement.
--    Utilisez cette clé côté serveur (Edge Functions) uniquement.

-- 4. ANON KEY:
--    Les requêtes avec la clé anon respectent les politiques RLS.
--    C'est cette clé que vous utiliserez depuis l'application Android.

-- ============================================================================
-- FIN DES POLITIQUES RLS
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE 'RLS policies configured successfully!';
    RAISE NOTICE 'WARNING: Current policies are PERMISSIVE (allow all).';
    RAISE NOTICE 'Adapt policies for production use.';
END $$;
