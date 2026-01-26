-- ============================================================================
-- Migration: Fix schema consistency between SQLDelight and PostgreSQL
-- 1. sale_items: rename price_per_unit -> unit_price, subtotal -> total_price
-- 2. stock_movements: add missing columns movement_type, reference_id, notes
-- ============================================================================

-- Rename sale_items columns to match SQLDelight schema
ALTER TABLE sale_items RENAME COLUMN price_per_unit TO unit_price;
ALTER TABLE sale_items RENAME COLUMN subtotal TO total_price;

-- Add missing columns to stock_movements
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS movement_type TEXT;
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS reference_id TEXT;
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS notes TEXT;

-- ============================================================================
-- Update views that reference old column names
-- ============================================================================

-- v_sale_details
CREATE OR REPLACE VIEW v_sale_details AS
SELECT
    s.id AS sale_id,
    s.date AS sale_date,
    s.customer_name,
    s.customer_id,
    s.total_amount AS sale_total,
    s.site_id,
    st.name AS site_name,
    si.id AS sale_item_id,
    si.product_id,
    si.product_name,
    si.unit,
    si.quantity,
    si.unit_price AS unit_price,
    si.total_price AS total_amount,
    sba.batch_id,
    sba.quantity_allocated,
    sba.purchase_price_at_allocation,
    get_currency_symbol() AS currency
FROM sales s
JOIN sites st ON s.site_id = st.id
JOIN sale_items si ON s.id = si.sale_id
LEFT JOIN sale_batch_allocations sba ON si.id = sba.sale_item_id;

-- v_sales_overview
CREATE OR REPLACE VIEW v_sales_overview AS
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
    SUM(si.total_price) AS total_revenue,
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

-- v_sales_by_product
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
    SUM(si.total_price) AS total_revenue,
    AVG(si.unit_price) AS avg_unit_price,
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

-- v_sales_by_category
CREATE OR REPLACE VIEW v_sales_by_category AS
SELECT
    p.category_id,
    COALESCE(c.name, 'Non categorise') AS category_name,
    s.site_id,
    st.name AS site_name,
    COUNT(DISTINCT s.id) AS nb_transactions,
    COUNT(DISTINCT si.product_id) AS nb_products_sold,
    SUM(si.quantity) AS total_quantity_sold,
    SUM(si.total_price) AS total_revenue,
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

-- v_profit_by_product
CREATE OR REPLACE VIEW v_profit_by_product AS
SELECT
    si.product_id,
    si.product_name,
    p.category_id,
    c.name AS category_name,
    s.site_id,
    st.name AS site_name,
    SUM(si.total_price) AS total_revenue,
    SUM(sba.quantity_allocated * sba.purchase_price_at_allocation) AS total_cogs,
    SUM(si.total_price) - COALESCE(SUM(sba.quantity_allocated * sba.purchase_price_at_allocation), 0) AS total_profit,
    CASE
        WHEN SUM(si.total_price) > 0
        THEN ROUND((((SUM(si.total_price) - COALESCE(SUM(sba.quantity_allocated * sba.purchase_price_at_allocation), 0)) / SUM(si.total_price)) * 100)::NUMERIC, 2)
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

-- ============================================================================
-- Record migration
-- ============================================================================
INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms)
VALUES ('2026012507_fix_schema_consistency', NULL, 'supabase_cli', TRUE, NULL)
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
