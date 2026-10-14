-- Record the reporting views migration in schema_migrations
INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms)
VALUES ('20260124001000_reporting_views', NULL, 'supabase_cli', TRUE, NULL)
ON CONFLICT (name) DO UPDATE SET
    applied_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    applied_by = 'supabase_cli',
    success = TRUE;
