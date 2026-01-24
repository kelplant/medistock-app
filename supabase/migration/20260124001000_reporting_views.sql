-- ============================================
-- MEDISTOCK REPORTING VIEWS
-- Prefix: v_ (to isolate from regular tables)
-- Created: 2026-01-24
-- ============================================

-- Helper function to get currency symbol
CREATE OR REPLACE FUNCTION get_currency_symbol()
RETURNS TEXT AS $$
  SELECT COALESCE(
    (SELECT value FROM app_config WHERE key = 'currency_symbol'),
    'F'
  );
$$ LANGUAGE SQL STABLE;

-- ============================================
-- 1. SALES VIEWS
-- ============================================

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
    COALESCE(c.name, 'Non catégorisé') AS category_name,
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

-- ============================================
-- 2. STOCK VIEWS
-- ============================================

-- v_stock_current: Current stock levels by product and site
CREATE OR REPLACE VIEW v_stock_current AS
SELECT
    p.id AS product_id,
    p.name AS product_name,
    p.unit,
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
    p.unit,
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
    COALESCE(category_name, 'Non catégorisé') AS category_name,
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

-- ============================================
-- 3. PURCHASE VIEWS
-- ============================================

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
    COALESCE(NULLIF(pb.supplier_name, ''), 'Non spécifié') AS supplier_name,
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
    p.unit,
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
WHERE pb.is_exhausted = false;

-- ============================================
-- 4. EXPIRY VIEWS
-- ============================================

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

-- ============================================
-- 5. PROFITABILITY VIEWS
-- ============================================

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

-- ============================================
-- 6. INVENTORY COUNT VIEWS
-- ============================================

-- v_inventory_discrepancies: Inventory count discrepancies
-- Note: Supabase uses flat 'inventories' table (no header/detail split)
CREATE OR REPLACE VIEW v_inventory_discrepancies AS
SELECT
    inv.id AS item_id,
    DATE(TO_TIMESTAMP(inv.count_date / 1000)) AS count_date,
    inv.site_id,
    st.name AS site_name,
    inv.product_id,
    p.name AS product_name,
    p.unit,
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

-- ============================================
-- 7. TRANSFERS VIEW
-- ============================================

-- v_transfers_summary: Transfer analytics
CREATE OR REPLACE VIEW v_transfers_summary AS
SELECT
    pt.id AS transfer_id,
    DATE(TO_TIMESTAMP(pt.date / 1000)) AS transfer_date,
    EXTRACT(YEAR FROM TO_TIMESTAMP(pt.date / 1000))::INTEGER AS transfer_year,
    EXTRACT(MONTH FROM TO_TIMESTAMP(pt.date / 1000))::INTEGER AS transfer_month,
    pt.from_site_id,
    fs.name AS from_site_name,
    pt.to_site_id,
    ts.name AS to_site_name,
    pt.product_id,
    p.name AS product_name,
    p.unit,
    pt.quantity,
    pt.notes
FROM product_transfers pt
JOIN sites fs ON pt.from_site_id = fs.id
JOIN sites ts ON pt.to_site_id = ts.id
JOIN products p ON pt.product_id = p.id;

-- ============================================
-- 8. STOCK MOVEMENTS VIEW
-- ============================================

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

-- ============================================
-- 9. KPI DASHBOARD VIEW
-- ============================================

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

-- ============================================
-- 10. STOCK TURNOVER VIEW
-- ============================================

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

-- ============================================
-- GRANT SELECT ON ALL VIEWS TO authenticated users
-- ============================================
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
