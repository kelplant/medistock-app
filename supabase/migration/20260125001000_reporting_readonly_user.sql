-- ============================================
-- CREATE READ-ONLY USER FOR REPORTING / BI
-- ============================================

-- 1. Create the role (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'reporting_user') THEN
        CREATE ROLE reporting_user WITH LOGIN PASSWORD 'CHANGE_ME_SECURE_PASSWORD';
    END IF;
END
$$;

-- 2. Grant connect to database
GRANT CONNECT ON DATABASE postgres TO reporting_user;

-- 3. Grant usage on public schema
GRANT USAGE ON SCHEMA public TO reporting_user;

-- 4. Grant SELECT on all reporting views (v_*)
GRANT SELECT ON v_sales_detail TO reporting_user;
GRANT SELECT ON v_sales_daily TO reporting_user;
GRANT SELECT ON v_sales_by_product TO reporting_user;
GRANT SELECT ON v_sales_by_category TO reporting_user;
GRANT SELECT ON v_sales_by_customer TO reporting_user;
GRANT SELECT ON v_stock_current TO reporting_user;
GRANT SELECT ON v_stock_alerts TO reporting_user;
GRANT SELECT ON v_stock_valuation TO reporting_user;
GRANT SELECT ON v_stock_turnover TO reporting_user;
GRANT SELECT ON v_purchases_daily TO reporting_user;
GRANT SELECT ON v_purchases_by_supplier TO reporting_user;
GRANT SELECT ON v_batches_active TO reporting_user;
GRANT SELECT ON v_expiry_alerts TO reporting_user;
GRANT SELECT ON v_expired_batches TO reporting_user;
GRANT SELECT ON v_profit_by_sale TO reporting_user;
GRANT SELECT ON v_profit_by_product TO reporting_user;
GRANT SELECT ON v_profit_daily TO reporting_user;
GRANT SELECT ON v_inventory_discrepancies TO reporting_user;
GRANT SELECT ON v_inventory_summary TO reporting_user;
GRANT SELECT ON v_transfers_summary TO reporting_user;
GRANT SELECT ON v_movements_daily TO reporting_user;
GRANT SELECT ON v_kpi_current TO reporting_user;

-- 5. Grant execute on helper function
GRANT EXECUTE ON FUNCTION get_currency_symbol() TO reporting_user;

-- 6. Ensure no access to other tables
-- (By default, new roles have no permissions, so this is implicit)

-- 7. Record migration
INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms)
VALUES ('20260125001000_reporting_readonly_user', NULL, 'supabase_cli', TRUE, NULL)
ON CONFLICT (name) DO NOTHING;
