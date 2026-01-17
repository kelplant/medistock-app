-- Add description field to products and expose it in the current_stock view

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS description TEXT;

-- Update current_stock view to include the description
DROP VIEW IF EXISTS current_stock;

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
