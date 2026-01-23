-- ================================================
-- Migration: 2026012302_notification_events.sql
-- Description: Add notification_events_local table for caching Supabase notifications
-- Date: 2026-01-23
-- ================================================

-- Table locale pour cache des notifications (sync depuis Supabase)
CREATE TABLE IF NOT EXISTS notification_events_local (
    id TEXT NOT NULL PRIMARY KEY,
    type TEXT NOT NULL,
    priority TEXT NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    reference_id TEXT,
    reference_type TEXT,
    site_id TEXT,
    deep_link TEXT,
    created_at INTEGER NOT NULL,
    is_displayed INTEGER NOT NULL DEFAULT 0,
    is_dismissed INTEGER NOT NULL DEFAULT 0
);

-- Index pour requêtes fréquentes
CREATE INDEX IF NOT EXISTS idx_notification_events_displayed ON notification_events_local(is_displayed);
CREATE INDEX IF NOT EXISTS idx_notification_events_dismissed ON notification_events_local(is_dismissed);
CREATE INDEX IF NOT EXISTS idx_notification_events_created_at ON notification_events_local(created_at DESC);

-- ================================================
-- Vérification
-- ================================================
-- SELECT name FROM sqlite_master WHERE type='table' AND name='notification_events_local';
