-- =============================================================================
-- Migration: 2026012901_version_catchup
-- Description: Catch-up migration to align schema_version with actual migration count.
--
-- Migrations 2026012101 through 20260125002000 did not call update_schema_version().
-- This migration bumps schema_version from 4 (last explicitly set value) to 29,
-- reflecting all 25 migrations applied since version 4, plus the 3 new migrations
-- added in this branch (fix_schema_consistency, complete_unit_removal, add_suppliers_table).
--
-- Also bumps min_app_version to 3 because the remove_product_unit and packaging_types
-- changes are breaking: older app versions cannot work with this schema.
-- =============================================================================

-- Bump schema_version to 29 and min_app_version to 3
SELECT update_schema_version(29, 3, 'migration_2026012901_version_catchup');

-- Record this migration
INSERT INTO schema_migrations (name, checksum, applied_by, success)
VALUES ('2026012901_version_catchup', NULL, 'migration', TRUE)
ON CONFLICT (name) DO NOTHING;
