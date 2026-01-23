-- ================================================
-- MediStock Notification System - Admin Settings
-- ================================================
-- Ce script crée la table des paramètres de notification configurables.
-- À exécuter dans l'éditeur SQL de Supabase APRÈS 001_notification_tables.sql.
-- ================================================

-- ================================================
-- Table: notification_settings
-- Paramètres globaux configurables par les administrateurs
-- ================================================
CREATE TABLE IF NOT EXISTS notification_settings (
    id TEXT PRIMARY KEY DEFAULT 'global',  -- Single row for global settings

    -- Expiry alerts
    expiry_alert_enabled INTEGER DEFAULT 1,
    expiry_warning_days INTEGER DEFAULT 7,  -- Days before expiry to warn
    expiry_dedup_days INTEGER DEFAULT 3,    -- Deduplication period for expiring soon
    expired_dedup_days INTEGER DEFAULT 7,   -- Deduplication period for expired

    -- Low stock alerts
    low_stock_alert_enabled INTEGER DEFAULT 1,
    low_stock_dedup_days INTEGER DEFAULT 7, -- Deduplication period for low stock

    -- Message templates (with variable placeholders)
    -- Variables: {{product_name}}, {{days_until}}, {{expiry_date}}, {{current_stock}}, {{min_stock}}
    template_expired_title TEXT DEFAULT 'Produit expiré',
    template_expired_message TEXT DEFAULT '{{product_name}} a expiré',

    template_expiring_title TEXT DEFAULT 'Expiration proche',
    template_expiring_message TEXT DEFAULT '{{product_name}} expire dans {{days_until}} jour(s)',

    template_low_stock_title TEXT DEFAULT 'Stock faible',
    template_low_stock_message TEXT DEFAULT '{{product_name}}: {{current_stock}}/{{min_stock}}',

    -- Metadata
    updated_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_by TEXT
);

-- Insert default settings if not exists
INSERT INTO notification_settings (id) VALUES ('global') ON CONFLICT (id) DO NOTHING;

-- ================================================
-- Row Level Security (RLS)
-- ================================================
ALTER TABLE notification_settings ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Admins can read notification_settings" ON notification_settings;
DROP POLICY IF EXISTS "Admins can update notification_settings" ON notification_settings;

-- Only authenticated users can read settings (needed by cron function and apps)
CREATE POLICY "Admins can read notification_settings"
    ON notification_settings FOR SELECT TO authenticated USING (true);

-- Only admins can update settings (enforced at database level)
-- Checks the users table for is_admin = 1
CREATE POLICY "Admins can update notification_settings"
    ON notification_settings FOR UPDATE TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM users
            WHERE users.id = auth.uid()::text
            AND users.is_admin = 1
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM users
            WHERE users.id = auth.uid()::text
            AND users.is_admin = 1
        )
    );

-- ================================================
-- Helper function to replace template variables
-- ================================================
CREATE OR REPLACE FUNCTION replace_template_vars(
    template TEXT,
    product_name TEXT DEFAULT NULL,
    days_until INTEGER DEFAULT NULL,
    expiry_date TEXT DEFAULT NULL,
    current_stock NUMERIC DEFAULT NULL,
    min_stock NUMERIC DEFAULT NULL
) RETURNS TEXT AS $$
BEGIN
    RETURN REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(
                    REPLACE(template, '{{product_name}}', COALESCE(product_name, '')),
                    '{{days_until}}', COALESCE(days_until::TEXT, '')
                ),
                '{{expiry_date}}', COALESCE(expiry_date, '')
            ),
            '{{current_stock}}', COALESCE(current_stock::TEXT, '')
        ),
        '{{min_stock}}', COALESCE(min_stock::TEXT, '')
    );
END;
$$ LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION replace_template_vars IS
'Replaces template variables in notification messages:
- {{product_name}}: Name of the product
- {{days_until}}: Days until expiry
- {{expiry_date}}: Expiry date formatted
- {{current_stock}}: Current stock quantity
- {{min_stock}}: Minimum stock threshold';

-- ================================================
-- Vérification
-- ================================================
-- SELECT * FROM notification_settings;
-- SELECT replace_template_vars('Le produit {{product_name}} expire dans {{days_until}} jours', 'Paracetamol', 5);
