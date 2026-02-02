-- ============================================================================
-- COMPLETE MIGRATION: Remove unit column from products and recreate all views
-- ============================================================================

-- STEP 1: Drop all dependent views
DROP VIEW IF EXISTS v_kpi_current CASCADE;
DROP VIEW IF EXISTS v_stock_alerts CASCADE;
DROP VIEW IF EXISTS v_stock_valuation CASCADE;
DROP VIEW IF EXISTS v_stock_turnover CASCADE;
DROP VIEW IF EXISTS v_expiry_alerts CASCADE;
DROP VIEW IF EXISTS v_expired_batches CASCADE;
DROP VIEW IF EXISTS v_stock_current CASCADE;
DROP VIEW IF EXISTS v_batches_active CASCADE;
DROP VIEW IF EXISTS transaction_flat_view CASCADE;
DROP VIEW IF EXISTS v_inventory_discrepancies CASCADE;
DROP VIEW IF EXISTS v_transfers_summary CASCADE;

-- STEP 2: Drop the unit column
ALTER TABLE products DROP COLUMN IF EXISTS unit;

-- STEP 3: Ensure constraints
UPDATE products SET packaging_type_id = (SELECT id FROM packaging_types LIMIT 1) WHERE packaging_type_id IS NULL;
UPDATE products SET selected_level = 1 WHERE selected_level IS NULL;
ALTER TABLE products ALTER COLUMN packaging_type_id SET NOT NULL;
ALTER TABLE products ALTER COLUMN selected_level SET NOT NULL;
ALTER TABLE products ALTER COLUMN selected_level SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_products_packaging ON products(packaging_type_id);

-- ============================================================================
-- STEP 4: Recreate all views with derived unit
-- ============================================================================

-- transaction_flat_view
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
    COALESCE(CASE WHEN p.selected_level = 2 THEN ptg.level2_name ELSE ptg.level1_name END, 'unit') AS unit,
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
    si.unit_price AS unit_price,
    si.total_price AS total_amount,
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
    COALESCE(CASE WHEN p.selected_level = 2 THEN ptg.level2_name ELSE ptg.level1_name END, 'unit') AS unit,
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
    COALESCE(CASE WHEN p.selected_level = 2 THEN ptg.level2_name ELSE ptg.level1_name END, 'unit') AS unit,
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
    ii.id AS transaction_id,
    COALESCE(ii.inventory_id, ii.id) AS reference_id,
    ii.count_date AS transaction_date,
    p.id AS product_id,
    p.name AS product_name,
    c.name AS category_name,
    ptg.name AS packaging_type_name,
    ii.site_id AS site_id,
    s.name AS site_name,
    NULL::UUID AS from_site_id,
    NULL::TEXT AS from_site_name,
    NULL::UUID AS to_site_id,
    NULL::TEXT AS to_site_name,
    NULL::UUID AS customer_id,
    NULL::TEXT AS customer_name,
    NULL::TEXT AS supplier_name,
    GREATEST(ii.discrepancy, 0) AS quantity_in,
    GREATEST(-ii.discrepancy, 0) AS quantity_out,
    ii.discrepancy AS quantity_delta,
    COALESCE(CASE WHEN p.selected_level = 2 THEN ptg.level2_name ELSE ptg.level1_name END, 'unit') AS unit,
    NULL::DOUBLE PRECISION AS unit_price,
    NULL::DOUBLE PRECISION AS total_amount,
    NULLIF(COALESCE(NULLIF(ii.reason, ''), NULLIF(ii.notes, '')), '') AS notes,
    'inventory_items'::TEXT AS source_table
FROM inventory_items ii
JOIN products p ON ii.product_id = p.id
LEFT JOIN sites s ON ii.site_id = s.id
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN packaging_types ptg ON p.packaging_type_id = ptg.id;

-- v_stock_current
CREATE OR REPLACE VIEW v_stock_current AS
SELECT
    p.id AS product_id,
    p.name AS product_name,
    COALESCE(CASE WHEN p.selected_level = 2 THEN pt.level2_name ELSE pt.level1_name END, 'unit') AS unit,
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
    SELECT selling_price FROM product_prices WHERE product_id = p.id ORDER BY effective_date DESC LIMIT 1
) pp ON true
WHERE p.is_active = true
GROUP BY p.id, p.name, p.selected_level, pt.level1_name, pt.level2_name, p.category_id, c.name, pb.site_id, st.name, p.min_stock, p.max_stock, pp.selling_price;

-- v_stock_alerts
CREATE OR REPLACE VIEW v_stock_alerts AS
SELECT product_id, product_name, unit, category_name, site_id, site_name, min_stock, max_stock, current_stock, stock_status AS alert_type,
    CASE WHEN stock_status = 'LOW' THEN min_stock - current_stock WHEN stock_status = 'HIGH' THEN current_stock - max_stock ELSE 0 END AS stock_gap,
    stock_value_cost, currency
FROM v_stock_current WHERE stock_status != 'OK';

-- v_stock_valuation
CREATE OR REPLACE VIEW v_stock_valuation AS
SELECT site_id, site_name, category_id, COALESCE(category_name, 'Non categorise') AS category_name, COUNT(DISTINCT product_id) AS nb_products,
    SUM(current_stock) AS total_quantity, SUM(stock_value_cost) AS total_value_cost, SUM(stock_value_selling) AS total_value_selling,
    SUM(stock_value_selling) - SUM(stock_value_cost) AS potential_margin,
    CASE WHEN SUM(stock_value_cost) > 0 THEN ROUND(((SUM(stock_value_selling) - SUM(stock_value_cost)) / SUM(stock_value_cost) * 100)::NUMERIC, 2) ELSE 0 END AS margin_percent, currency
FROM v_stock_current GROUP BY site_id, site_name, category_id, category_name, currency;

-- v_batches_active
CREATE OR REPLACE VIEW v_batches_active AS
SELECT pb.id AS batch_id, pb.batch_number, pb.product_id, p.name AS product_name,
    COALESCE(CASE WHEN p.selected_level = 2 THEN pt.level2_name ELSE pt.level1_name END, 'unit') AS unit,
    pb.site_id, st.name AS site_name, pb.supplier_name, DATE(TO_TIMESTAMP(pb.purchase_date / 1000)) AS purchase_date,
    DATE(TO_TIMESTAMP(pb.expiry_date / 1000)) AS expiry_date, pb.initial_quantity, pb.remaining_quantity, pb.purchase_price,
    pb.remaining_quantity * pb.purchase_price AS batch_value,
    CASE WHEN pb.expiry_date IS NULL THEN NULL ELSE (DATE(TO_TIMESTAMP(pb.expiry_date / 1000)) - CURRENT_DATE) END AS days_until_expiry,
    get_currency_symbol() AS currency
FROM purchase_batches pb JOIN products p ON pb.product_id = p.id JOIN sites st ON pb.site_id = st.id
LEFT JOIN packaging_types pt ON p.packaging_type_id = pt.id WHERE pb.is_exhausted = false;

-- v_expiry_alerts
CREATE OR REPLACE VIEW v_expiry_alerts AS
SELECT batch_id, batch_number, product_id, product_name, unit, site_id, site_name, supplier_name, purchase_date, expiry_date,
    remaining_quantity, purchase_price, batch_value AS value_at_risk, days_until_expiry,
    CASE WHEN days_until_expiry <= 0 THEN 'EXPIRED' WHEN days_until_expiry <= 30 THEN 'CRITICAL' WHEN days_until_expiry <= 90 THEN 'WARNING' ELSE 'OK' END AS expiry_status, currency
FROM v_batches_active WHERE expiry_date IS NOT NULL AND days_until_expiry <= 90;

-- v_expired_batches
CREATE OR REPLACE VIEW v_expired_batches AS
SELECT batch_id, batch_number, product_id, product_name, unit, site_id, site_name, expiry_date, remaining_quantity AS quantity_lost,
    batch_value AS value_lost, ABS(days_until_expiry) AS days_since_expiry, currency
FROM v_batches_active WHERE expiry_date IS NOT NULL AND days_until_expiry < 0;

-- v_inventory_discrepancies
CREATE OR REPLACE VIEW v_inventory_discrepancies AS
SELECT ii.id AS item_id, ii.inventory_id, DATE(TO_TIMESTAMP(ii.count_date / 1000)) AS count_date, ii.site_id, st.name AS site_name,
    ii.product_id, p.name AS product_name,
    COALESCE(CASE WHEN p.selected_level = 2 THEN pt.level2_name ELSE pt.level1_name END, 'unit') AS unit,
    ii.theoretical_quantity, ii.counted_quantity, ii.discrepancy,
    CASE WHEN ii.theoretical_quantity > 0 THEN ROUND(((ii.discrepancy / ii.theoretical_quantity) * 100)::NUMERIC, 2) ELSE 0 END AS discrepancy_percent,
    ii.reason, ii.counted_by, ii.notes
FROM inventory_items ii JOIN sites st ON ii.site_id = st.id LEFT JOIN products p ON ii.product_id = p.id
LEFT JOIN packaging_types pt ON p.packaging_type_id = pt.id WHERE ii.discrepancy != 0;

-- v_transfers_summary
CREATE OR REPLACE VIEW v_transfers_summary AS
SELECT ptr.id AS transfer_id, DATE(TO_TIMESTAMP(ptr.date / 1000)) AS transfer_date,
    EXTRACT(YEAR FROM TO_TIMESTAMP(ptr.date / 1000))::INTEGER AS transfer_year,
    EXTRACT(MONTH FROM TO_TIMESTAMP(ptr.date / 1000))::INTEGER AS transfer_month,
    ptr.from_site_id, fs.name AS from_site_name, ptr.to_site_id, ts.name AS to_site_name,
    ptr.product_id, p.name AS product_name,
    COALESCE(CASE WHEN p.selected_level = 2 THEN pt.level2_name ELSE pt.level1_name END, 'unit') AS unit,
    ptr.quantity, ptr.notes
FROM product_transfers ptr JOIN sites fs ON ptr.from_site_id = fs.id JOIN sites ts ON ptr.to_site_id = ts.id
JOIN products p ON ptr.product_id = p.id LEFT JOIN packaging_types pt ON p.packaging_type_id = pt.id;

-- v_stock_turnover
CREATE OR REPLACE VIEW v_stock_turnover AS
WITH sales_30d AS (
    SELECT si.product_id, s.site_id, SUM(si.quantity) AS qty_sold_30d
    FROM sale_items si JOIN sales s ON si.sale_id = s.id
    WHERE DATE(TO_TIMESTAMP(s.date / 1000)) >= CURRENT_DATE - INTERVAL '30 days'
    GROUP BY si.product_id, s.site_id
)
SELECT sc.product_id, sc.product_name, sc.unit, sc.category_name, sc.site_id, sc.site_name, sc.current_stock,
    COALESCE(s30.qty_sold_30d, 0) AS qty_sold_last_30_days,
    CASE WHEN sc.current_stock > 0 AND COALESCE(s30.qty_sold_30d, 0) > 0 THEN ROUND((sc.current_stock / (s30.qty_sold_30d / 30.0))::NUMERIC, 1) ELSE NULL END AS days_of_stock,
    CASE WHEN sc.current_stock > 0 AND COALESCE(s30.qty_sold_30d, 0) > 0 THEN ROUND(((s30.qty_sold_30d / 30.0 * 365) / sc.current_stock)::NUMERIC, 2) ELSE 0 END AS annual_turnover_rate,
    sc.currency
FROM v_stock_current sc LEFT JOIN sales_30d s30 ON sc.product_id = s30.product_id AND sc.site_id = s30.site_id;

-- v_kpi_current
CREATE OR REPLACE VIEW v_kpi_current AS
WITH
today_sales AS (SELECT site_id, COUNT(*) AS sales_today, COALESCE(SUM(total_amount), 0) AS revenue_today FROM sales WHERE DATE(TO_TIMESTAMP(date / 1000)) = CURRENT_DATE GROUP BY site_id),
week_sales AS (SELECT site_id, COUNT(*) AS sales_week, COALESCE(SUM(total_amount), 0) AS revenue_week FROM sales WHERE DATE(TO_TIMESTAMP(date / 1000)) >= DATE_TRUNC('week', CURRENT_DATE) GROUP BY site_id),
month_sales AS (SELECT site_id, COUNT(*) AS sales_month, COALESCE(SUM(total_amount), 0) AS revenue_month FROM sales WHERE DATE(TO_TIMESTAMP(date / 1000)) >= DATE_TRUNC('month', CURRENT_DATE) GROUP BY site_id),
stock_stats AS (SELECT site_id, SUM(current_stock) AS total_stock_qty, SUM(stock_value_cost) AS total_stock_value, COUNT(CASE WHEN stock_status = 'LOW' THEN 1 END) AS low_stock_alerts, COUNT(CASE WHEN stock_status = 'HIGH' THEN 1 END) AS high_stock_alerts FROM v_stock_current GROUP BY site_id),
expiry_stats AS (SELECT site_id, COUNT(CASE WHEN expiry_status = 'EXPIRED' THEN 1 END) AS expired_batches, COUNT(CASE WHEN expiry_status = 'CRITICAL' THEN 1 END) AS critical_expiry_batches, COUNT(CASE WHEN expiry_status = 'WARNING' THEN 1 END) AS warning_expiry_batches FROM v_expiry_alerts GROUP BY site_id)
SELECT st.id AS site_id, st.name AS site_name,
    COALESCE(ts.sales_today, 0) AS sales_today, COALESCE(ts.revenue_today, 0) AS revenue_today,
    COALESCE(ws.sales_week, 0) AS sales_this_week, COALESCE(ws.revenue_week, 0) AS revenue_this_week,
    COALESCE(ms.sales_month, 0) AS sales_this_month, COALESCE(ms.revenue_month, 0) AS revenue_this_month,
    COALESCE(ss.total_stock_qty, 0) AS total_stock_quantity, COALESCE(ss.total_stock_value, 0) AS total_stock_value,
    COALESCE(ss.low_stock_alerts, 0) AS low_stock_alerts, COALESCE(ss.high_stock_alerts, 0) AS high_stock_alerts,
    COALESCE(es.expired_batches, 0) AS expired_batches, COALESCE(es.critical_expiry_batches, 0) AS critical_expiry_alerts, COALESCE(es.warning_expiry_batches, 0) AS warning_expiry_alerts,
    CURRENT_TIMESTAMP AS last_updated, get_currency_symbol() AS currency
FROM sites st LEFT JOIN today_sales ts ON st.id = ts.site_id LEFT JOIN week_sales ws ON st.id = ws.site_id
LEFT JOIN month_sales ms ON st.id = ms.site_id LEFT JOIN stock_stats ss ON st.id = ss.site_id LEFT JOIN expiry_stats es ON st.id = es.site_id
WHERE st.is_active = true;

-- ============================================================================
-- STEP 5: Record migrations
-- ============================================================================
INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms)
VALUES ('2026012502_remove_product_unit', NULL, 'supabase_cli', TRUE, NULL)
ON CONFLICT (name) DO NOTHING;

INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms)
VALUES ('20260124001000_reporting_views', NULL, 'supabase_cli', TRUE, NULL)
ON CONFLICT (name) DO UPDATE SET applied_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
