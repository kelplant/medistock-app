-- ============================================================================
-- MIGRATION: Schema Cleanup & Audit Field Harmonization
-- Date: 2026-01-05
-- Description:
--   1. Drop obsolete table product_sales (replaced by sales + sale_items)
--   2. Add missing audit fields to customers, sale_items, sale_batch_allocations
--   3. Create proper triggers for new audit fields
-- ============================================================================

-- ============================================================================
-- 1. DROP OBSOLETE TABLE: product_sales
-- ============================================================================

-- Drop the audit trigger first
DROP TRIGGER IF EXISTS audit_product_sales_trigger ON product_sales;

-- Drop the created_by trigger
DROP TRIGGER IF EXISTS set_product_sales_created_by ON product_sales;

-- Drop indexes
DROP INDEX IF EXISTS idx_product_sales_product;
DROP INDEX IF EXISTS idx_product_sales_site;
DROP INDEX IF EXISTS idx_product_sales_date;

-- Drop the table itself
DROP TABLE IF EXISTS product_sales;

-- ============================================================================
-- 2. ADD MISSING AUDIT FIELDS TO customers
-- ============================================================================

-- Add updated_at and updated_by columns to customers
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS updated_by TEXT NOT NULL DEFAULT 'system';

-- Create trigger for customers updated_at
DROP TRIGGER IF EXISTS update_customers_updated_at ON customers;
CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create trigger for customers audit defaults (replace existing trigger if any)
DROP TRIGGER IF EXISTS set_customers_created_by ON customers;
DROP TRIGGER IF EXISTS set_customers_audit_defaults ON customers;
CREATE TRIGGER set_customers_audit_defaults BEFORE INSERT OR UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION set_audit_user_defaults();

-- ============================================================================
-- 3. ADD MISSING AUDIT FIELDS TO sale_items
-- ============================================================================

-- Add created_at and created_by columns to sale_items
ALTER TABLE sale_items
    ADD COLUMN IF NOT EXISTS created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;

ALTER TABLE sale_items
    ADD COLUMN IF NOT EXISTS created_by TEXT NOT NULL DEFAULT 'system';

-- Create trigger for sale_items created_by default
DROP TRIGGER IF EXISTS set_sale_items_created_by ON sale_items;
CREATE TRIGGER set_sale_items_created_by BEFORE INSERT ON sale_items
    FOR EACH ROW EXECUTE FUNCTION set_created_by_default();

-- ============================================================================
-- 4. ADD MISSING AUDIT FIELDS TO sale_batch_allocations
-- ============================================================================

-- Add created_by column to sale_batch_allocations
ALTER TABLE sale_batch_allocations
    ADD COLUMN IF NOT EXISTS created_by TEXT NOT NULL DEFAULT 'system';

-- Create trigger for sale_batch_allocations created_by default
DROP TRIGGER IF EXISTS set_sale_batch_allocations_created_by ON sale_batch_allocations;
CREATE TRIGGER set_sale_batch_allocations_created_by BEFORE INSERT ON sale_batch_allocations
    FOR EACH ROW EXECUTE FUNCTION set_created_by_default();

-- ============================================================================
-- 5. CLEAN UP AUDIT_HISTORY REFERENCES TO product_sales
-- ============================================================================

-- Note: We keep the audit_history records for historical purposes
-- but add a comment to indicate the table no longer exists
-- No actual deletion of historical data is performed

-- ============================================================================
-- 6. VERIFICATION QUERIES (run manually to verify migration)
-- ============================================================================

-- Verify product_sales is dropped
-- SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'product_sales');

-- Verify customers has all audit fields
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'customers' AND column_name IN ('created_at', 'updated_at', 'created_by', 'updated_by');

-- Verify sale_items has audit fields
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'sale_items' AND column_name IN ('created_at', 'created_by');

-- Verify sale_batch_allocations has audit fields
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'sale_batch_allocations' AND column_name IN ('created_at', 'created_by');

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE 'Migration 2026010501_schema_cleanup completed successfully!';
    RAISE NOTICE 'Changes:';
    RAISE NOTICE '  - Dropped obsolete table: product_sales';
    RAISE NOTICE '  - Added updated_at, updated_by to: customers';
    RAISE NOTICE '  - Added created_at, created_by to: sale_items';
    RAISE NOTICE '  - Added created_by to: sale_batch_allocations';
END $$;
