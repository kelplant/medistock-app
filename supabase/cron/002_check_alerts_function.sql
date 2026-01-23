-- ================================================
-- MediStock Notification System - Alert Function
-- ================================================
-- Ce script crée la fonction qui vérifie les alertes.
-- À exécuter dans l'éditeur SQL de Supabase APRÈS 001_notification_tables.sql
-- ET 004_notification_settings.sql.
-- ================================================

-- ================================================
-- Fonction: check_notification_alerts()
-- Appelée par pg_cron quotidiennement
-- Lit les paramètres depuis notification_settings
-- ================================================
CREATE OR REPLACE FUNCTION check_notification_alerts()
RETURNS void AS $$
DECLARE
    now_ms BIGINT := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    settings RECORD;
    threshold_ms BIGINT;
    expired_dedup_ms BIGINT;
    expiry_dedup_ms BIGINT;
    low_stock_dedup_ms BIGINT;
    batch RECORD;
    product RECORD;
    notifications_created INTEGER := 0;
    computed_title TEXT;
    computed_message TEXT;
BEGIN
    RAISE NOTICE 'check_notification_alerts: Starting at %', NOW();

    -- Load settings from notification_settings table
    SELECT * INTO settings FROM notification_settings WHERE id = 'global';

    -- If no settings found, use defaults
    IF NOT FOUND THEN
        RAISE NOTICE 'No notification_settings found, using defaults';
        settings := ROW(
            'global', 1, 7, 3, 7, 1, 7,
            'Produit expiré', '{{product_name}} a expiré',
            'Expiration proche', '{{product_name}} expire dans {{days_until}} jour(s)',
            'Stock faible', '{{product_name}}: {{current_stock}}/{{min_stock}}',
            now_ms, NULL
        );
    END IF;

    -- Calculate thresholds in milliseconds
    threshold_ms := now_ms + (settings.expiry_warning_days * 24 * 60 * 60 * 1000);
    expired_dedup_ms := settings.expired_dedup_days * 24 * 60 * 60 * 1000;
    expiry_dedup_ms := settings.expiry_dedup_days * 24 * 60 * 60 * 1000;
    low_stock_dedup_ms := settings.low_stock_dedup_days * 24 * 60 * 60 * 1000;

    RAISE NOTICE 'Settings loaded: expiry_warning_days=%, expiry_alert_enabled=%, low_stock_alert_enabled=%',
        settings.expiry_warning_days, settings.expiry_alert_enabled, settings.low_stock_alert_enabled;

    -- ================================================
    -- 1. Produits EXPIRÉS (date < maintenant)
    -- Priorité: CRITICAL
    -- ================================================
    IF settings.expiry_alert_enabled = 1 THEN
        FOR batch IN
            SELECT pb.id, pb.product_id, pb.site_id, pb.expiry_date, p.name as product_name
            FROM purchase_batches pb
            JOIN products p ON pb.product_id = p.id
            WHERE pb.expiry_date < now_ms
              AND pb.is_exhausted = 0
              AND NOT EXISTS (
                  SELECT 1 FROM notification_events ne
                  WHERE ne.reference_id = pb.id::text
                    AND ne.type = 'PRODUCT_EXPIRED'
                    AND ne.created_at > now_ms - expired_dedup_ms
              )
        LOOP
            computed_title := replace_template_vars(settings.template_expired_title, batch.product_name);
            computed_message := replace_template_vars(settings.template_expired_message, batch.product_name);

            INSERT INTO notification_events (type, priority, title, message, reference_id, reference_type, site_id, deep_link)
            VALUES (
                'PRODUCT_EXPIRED',
                'CRITICAL',
                computed_title,
                computed_message,
                batch.id::text,
                'batch',
                batch.site_id,
                'medistock://stock/' || batch.product_id
            );
            notifications_created := notifications_created + 1;
            RAISE NOTICE 'Created PRODUCT_EXPIRED notification for: %', batch.product_name;
        END LOOP;

        -- ================================================
        -- 2. Produits expirant dans X jours (configurable)
        -- Priorité: HIGH
        -- ================================================
        FOR batch IN
            SELECT pb.id, pb.product_id, pb.site_id, pb.expiry_date, p.name as product_name,
                   CEIL((pb.expiry_date - now_ms) / (24 * 60 * 60 * 1000.0))::INTEGER as days_until
            FROM purchase_batches pb
            JOIN products p ON pb.product_id = p.id
            WHERE pb.expiry_date BETWEEN now_ms AND threshold_ms
              AND pb.is_exhausted = 0
              AND NOT EXISTS (
                  SELECT 1 FROM notification_events ne
                  WHERE ne.reference_id = pb.id::text
                    AND ne.type = 'PRODUCT_EXPIRING_SOON'
                    AND ne.created_at > now_ms - expiry_dedup_ms
              )
        LOOP
            computed_title := replace_template_vars(settings.template_expiring_title, batch.product_name, batch.days_until);
            computed_message := replace_template_vars(settings.template_expiring_message, batch.product_name, batch.days_until);

            INSERT INTO notification_events (type, priority, title, message, reference_id, reference_type, site_id, deep_link)
            VALUES (
                'PRODUCT_EXPIRING_SOON',
                'HIGH',
                computed_title,
                computed_message,
                batch.id::text,
                'batch',
                batch.site_id,
                'medistock://stock/' || batch.product_id
            );
            notifications_created := notifications_created + 1;
            RAISE NOTICE 'Created PRODUCT_EXPIRING_SOON notification for: % (% days)', batch.product_name, batch.days_until;
        END LOOP;
    END IF;

    -- ================================================
    -- 3. Stock faible (stock actuel < stock minimum)
    -- Priorité: MEDIUM
    -- Utilise le min_stock défini par produit
    -- ================================================
    IF settings.low_stock_alert_enabled = 1 THEN
        FOR product IN
            SELECT p.id, p.name, p.site_id, p.min_stock,
                   COALESCE(SUM(pb.remaining_quantity), 0) as current_stock
            FROM products p
            LEFT JOIN purchase_batches pb ON pb.product_id = p.id AND pb.is_exhausted = 0
            WHERE p.min_stock > 0 AND p.is_active = 1
            GROUP BY p.id, p.name, p.site_id, p.min_stock
            HAVING COALESCE(SUM(pb.remaining_quantity), 0) < p.min_stock
              AND NOT EXISTS (
                  SELECT 1 FROM notification_events ne
                  WHERE ne.reference_id = p.id
                    AND ne.type = 'LOW_STOCK'
                    AND ne.created_at > now_ms - low_stock_dedup_ms
              )
        LOOP
            computed_title := replace_template_vars(settings.template_low_stock_title, product.name, NULL, NULL, product.current_stock, product.min_stock);
            computed_message := replace_template_vars(settings.template_low_stock_message, product.name, NULL, NULL, product.current_stock, product.min_stock);

            INSERT INTO notification_events (type, priority, title, message, reference_id, reference_type, site_id, deep_link)
            VALUES (
                'LOW_STOCK',
                'MEDIUM',
                computed_title,
                computed_message,
                product.id,
                'product',
                product.site_id,
                'medistock://stock/' || product.id
            );
            notifications_created := notifications_created + 1;
            RAISE NOTICE 'Created LOW_STOCK notification for: % (%/%)', product.name, product.current_stock, product.min_stock;
        END LOOP;
    END IF;

    RAISE NOTICE 'check_notification_alerts: Completed. Created % notifications.', notifications_created;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- ================================================
-- Commentaire sur la fonction
-- ================================================
COMMENT ON FUNCTION check_notification_alerts() IS
'Vérifie les conditions d''alerte et crée des notifications:
- PRODUCT_EXPIRED: Produits avec date d''expiration passée (CRITICAL)
- PRODUCT_EXPIRING_SOON: Produits expirant dans X jours (HIGH) - configurable
- LOW_STOCK: Produits sous le stock minimum défini par produit (MEDIUM)
Paramètres configurables via la table notification_settings:
- expiry_warning_days: nombre de jours avant expiration pour alerter
- templates personnalisables avec variables {{product_name}}, {{days_until}}, etc.
- périodes de déduplication configurables
Appelée par pg_cron quotidiennement à 9h UTC.';

-- ================================================
-- Test manuel (optionnel)
-- ================================================
-- SELECT check_notification_alerts();
-- SELECT * FROM notification_events ORDER BY created_at DESC LIMIT 10;
