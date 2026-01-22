-- Migration: Enhance sync_queue table to match Room schema
-- Date: 2026-01-22

-- Drop old sync_queue table and recreate with full schema
DROP TABLE IF EXISTS sync_queue;

CREATE TABLE sync_queue (
    id TEXT NOT NULL PRIMARY KEY,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    operation TEXT NOT NULL,
    payload TEXT NOT NULL,
    local_version INTEGER NOT NULL DEFAULT 1,
    remote_version INTEGER,
    last_known_remote_updated_at INTEGER,
    status TEXT NOT NULL DEFAULT 'pending',
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    last_attempt_at INTEGER,
    created_at INTEGER NOT NULL DEFAULT 0,
    user_id TEXT,
    site_id TEXT
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_sync_queue_status ON sync_queue(status);
CREATE INDEX IF NOT EXISTS idx_sync_queue_entity ON sync_queue(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_sync_queue_created ON sync_queue(created_at);
