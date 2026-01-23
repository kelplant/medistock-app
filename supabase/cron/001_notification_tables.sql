-- ================================================
-- MediStock Notification System - Tables
-- ================================================
-- Ce script crée les tables nécessaires pour le système de notifications.
-- À exécuter dans l'éditeur SQL de Supabase.
-- ================================================

-- ================================================
-- Table: notification_events
-- Générée par pg_cron, synchronisée vers les devices
-- ================================================
CREATE TABLE IF NOT EXISTS notification_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type TEXT NOT NULL,              -- 'PRODUCT_EXPIRED', 'PRODUCT_EXPIRING_SOON', 'LOW_STOCK'
    priority TEXT NOT NULL,          -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    reference_id TEXT,               -- ID du produit/batch concerné
    reference_type TEXT,             -- 'product', 'batch'
    site_id TEXT REFERENCES sites(id),
    deep_link TEXT,                  -- 'medistock://stock/{id}'
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    expires_at BIGINT,               -- Optionnel: notification expire après X jours
    is_active INTEGER DEFAULT 1
);

-- Index pour requêtes fréquentes
CREATE INDEX IF NOT EXISTS idx_notification_events_site ON notification_events(site_id);
CREATE INDEX IF NOT EXISTS idx_notification_events_created ON notification_events(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_events_type ON notification_events(type);
CREATE INDEX IF NOT EXISTS idx_notification_events_reference ON notification_events(reference_id, type);

-- ================================================
-- Table: notification_dismissals
-- Track quel user a vu/acquitté quelle notification
-- ================================================
CREATE TABLE IF NOT EXISTS notification_dismissals (
    notification_id UUID REFERENCES notification_events(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL,
    dismissed_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    PRIMARY KEY (notification_id, user_id)
);

-- Index pour requêtes fréquentes
CREATE INDEX IF NOT EXISTS idx_notification_dismissals_user ON notification_dismissals(user_id);

-- ================================================
-- Table: notification_preferences (optionnel)
-- Préférences par user
-- ================================================
CREATE TABLE IF NOT EXISTS notification_preferences (
    user_id TEXT PRIMARY KEY,
    expiry_alert_enabled INTEGER DEFAULT 1,
    expiry_days_threshold INTEGER DEFAULT 30,
    low_stock_alert_enabled INTEGER DEFAULT 1,
    quiet_hours_start TEXT,          -- '22:00'
    quiet_hours_end TEXT,            -- '07:00'
    sound_enabled INTEGER DEFAULT 1,
    updated_at BIGINT DEFAULT 0
);

-- ================================================
-- Row Level Security (RLS)
-- ================================================

-- Enable RLS
ALTER TABLE notification_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_dismissals ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_preferences ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist (for re-running the script)
DROP POLICY IF EXISTS "Users can read notification_events" ON notification_events;
DROP POLICY IF EXISTS "Users can read their dismissals" ON notification_dismissals;
DROP POLICY IF EXISTS "Users can insert their dismissals" ON notification_dismissals;
DROP POLICY IF EXISTS "Users can read their preferences" ON notification_preferences;
DROP POLICY IF EXISTS "Users can insert their preferences" ON notification_preferences;
DROP POLICY IF EXISTS "Users can update their preferences" ON notification_preferences;

-- Policies for notification_events
-- SÉCURITÉ: Actuellement, tous les utilisateurs authentifiés peuvent lire toutes les notifications.
-- Dans une future version, implémenter un filtrage par site si nécessaire:
--   USING (site_id IS NULL OR site_id IN (SELECT site_id FROM user_site_access WHERE user_id = auth.uid()::text))
-- Pour l'instant, les utilisateurs de l'app peuvent accéder à tous les sites via le sélecteur.
CREATE POLICY "Users can read notification_events"
    ON notification_events FOR SELECT TO authenticated USING (true);

-- Policies for notification_dismissals
-- Les utilisateurs peuvent lire leurs propres acquittements
CREATE POLICY "Users can read their dismissals"
    ON notification_dismissals FOR SELECT TO authenticated
    USING (user_id = auth.uid()::text);

-- Les utilisateurs peuvent ajouter leurs propres acquittements
CREATE POLICY "Users can insert their dismissals"
    ON notification_dismissals FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid()::text);

-- Policies for notification_preferences
-- Les utilisateurs peuvent lire leurs propres préférences
CREATE POLICY "Users can read their preferences"
    ON notification_preferences FOR SELECT TO authenticated
    USING (user_id = auth.uid()::text);

-- Les utilisateurs peuvent créer leurs propres préférences
CREATE POLICY "Users can insert their preferences"
    ON notification_preferences FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid()::text);

-- Les utilisateurs peuvent mettre à jour leurs propres préférences
CREATE POLICY "Users can update their preferences"
    ON notification_preferences FOR UPDATE TO authenticated
    USING (user_id = auth.uid()::text)
    WITH CHECK (user_id = auth.uid()::text);

-- ================================================
-- Enable Realtime for notification_events
-- ================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'notification_events'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE notification_events;
    END IF;
END $$;

-- ================================================
-- Vérification
-- ================================================
-- SELECT table_name FROM information_schema.tables
-- WHERE table_schema = 'public' AND table_name LIKE 'notification%';
