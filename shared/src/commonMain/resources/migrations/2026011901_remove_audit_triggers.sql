-- ============================================================================
-- REMOVE SUPABASE AUDIT TRIGGERS
-- L'audit est géré uniquement côté app Android (Room) pour éviter la duplication
-- des données et réduire le volume de stockage.
-- NOTE: No BEGIN/COMMIT - apply_migration() handles the transaction
-- ============================================================================

-- ============================================================================
-- 1. SUPPRIMER TOUS LES TRIGGERS D'AUDIT
-- ============================================================================

DROP TRIGGER IF EXISTS audit_sites_trigger ON sites;
DROP TRIGGER IF EXISTS audit_categories_trigger ON categories;
DROP TRIGGER IF EXISTS audit_packaging_types_trigger ON packaging_types;
DROP TRIGGER IF EXISTS audit_app_users_trigger ON app_users;
DROP TRIGGER IF EXISTS audit_user_permissions_trigger ON user_permissions;
DROP TRIGGER IF EXISTS audit_customers_trigger ON customers;
DROP TRIGGER IF EXISTS audit_products_trigger ON products;
DROP TRIGGER IF EXISTS audit_product_prices_trigger ON product_prices;
DROP TRIGGER IF EXISTS audit_purchase_batches_trigger ON purchase_batches;
DROP TRIGGER IF EXISTS audit_stock_movements_trigger ON stock_movements;
DROP TRIGGER IF EXISTS audit_inventories_trigger ON inventories;
DROP TRIGGER IF EXISTS audit_product_transfers_trigger ON product_transfers;
DROP TRIGGER IF EXISTS audit_sales_trigger ON sales;
DROP TRIGGER IF EXISTS audit_sale_items_trigger ON sale_items;
DROP TRIGGER IF EXISTS audit_sale_batch_allocations_trigger ON sale_batch_allocations;
DROP TRIGGER IF EXISTS audit_product_sales_trigger ON product_sales;

-- ============================================================================
-- 2. SUPPRIMER LA FONCTION DE TRIGGER (optionnel, mais nettoie la DB)
-- ============================================================================

DROP FUNCTION IF EXISTS log_audit_history_trigger() CASCADE;
DROP FUNCTION IF EXISTS ensure_audit_trigger(TEXT, TEXT);

-- ============================================================================
-- 3. NETTOYER LES ANCIENNES DONNÉES D'AUDIT SUPABASE (optionnel)
-- Décommentez si vous voulez supprimer les audits existants créés par Supabase
-- ============================================================================

-- DELETE FROM audit_history WHERE description = 'Supabase trigger audit';

-- ============================================================================
-- 4. NOTE: La table audit_history reste en place pour l'audit côté app
-- L'app Android continue d'écrire dans cette table via AuditLogger
-- ============================================================================

-- Mettre à jour la version du schéma
SELECT update_schema_version(4, 2, 'migration_2026011901');

-- Enregistrer cette migration
INSERT INTO schema_migrations (name, checksum, applied_by, success)
VALUES ('2026011901_remove_audit_triggers', NULL, 'manual', TRUE)
ON CONFLICT (name) DO NOTHING;
