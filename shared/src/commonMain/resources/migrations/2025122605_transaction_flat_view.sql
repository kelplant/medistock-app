-- ============================================================================
-- Migration: flattened transaction view for BI (achats, ventes, transferts, inventaires)
-- ============================================================================

BEGIN;

DROP VIEW IF EXISTS transaction_flat_view;

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
    COALESCE(CASE WHEN p.selected_level = 2 THEN ptg.level2_name ELSE ptg.level1_name END, 'unit') AS unit,
    NULL::DOUBLE PRECISION AS unit_price,
    NULL::DOUBLE PRECISION AS total_amount,
    NULLIF(COALESCE(NULLIF(i.reason, ''), NULLIF(i.notes, '')), '') AS notes,
    'inventories'::TEXT AS source_table
FROM inventories i
JOIN products p ON i.product_id = p.id
LEFT JOIN sites s ON i.site_id = s.id
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN packaging_types ptg ON p.packaging_type_id = ptg.id;

COMMIT;

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
