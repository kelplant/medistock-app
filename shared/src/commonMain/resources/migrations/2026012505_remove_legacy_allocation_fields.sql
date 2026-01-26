-- ============================================================================
-- Migration: Remove legacy fields from sale_batch_allocations
-- The quantity and unit_cost fields duplicated the canonical fields
-- ============================================================================

-- Note: SQLite doesn't support DROP COLUMN directly in all versions
-- For older SQLite, we would need to recreate the table
-- But since Supabase never had these columns, this migration is mainly
-- for documentation and local SQLite cleanup if needed

-- For SQLite 3.35.0+ (Android API 31+):
-- ALTER TABLE sale_batch_allocations DROP COLUMN quantity;
-- ALTER TABLE sale_batch_allocations DROP COLUMN unit_cost;

-- For backward compatibility with older SQLite, the app code has been updated
-- to no longer read or write these legacy fields. The columns may remain
-- in existing databases but will be ignored.

-- The canonical fields are:
-- - quantity_allocated (use this instead of quantity)
-- - purchase_price_at_allocation (use this instead of unit_cost)

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
