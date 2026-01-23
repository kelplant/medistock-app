-- ================================================
-- MediStock Notification System - pg_cron Job
-- ================================================
-- Ce script planifie le job pg_cron.
-- À exécuter dans l'éditeur SQL de Supabase APRÈS 002_check_alerts_function.sql.
--
-- PRÉREQUIS: L'extension pg_cron doit être activée.
-- Database → Extensions → pg_cron → Enable
-- ================================================

-- ================================================
-- Vérifier que pg_cron est activé
-- ================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        RAISE EXCEPTION 'pg_cron extension is not enabled. Please enable it first in Database → Extensions → pg_cron';
    END IF;
END $$;

-- ================================================
-- Supprimer le job existant si présent
-- ================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'check-notifications-daily') THEN
        PERFORM cron.unschedule('check-notifications-daily');
        RAISE NOTICE 'Existing job "check-notifications-daily" unscheduled.';
    END IF;
END $$;

-- ================================================
-- Créer le job pg_cron
-- Schedule: Tous les jours à 9h00 UTC
-- ================================================
SELECT cron.schedule(
    'check-notifications-daily',     -- Nom du job
    '0 9 * * *',                     -- Cron expression: 9h00 UTC tous les jours
    $$SELECT check_notification_alerts()$$  -- Commande SQL
);

-- ================================================
-- Vérification
-- ================================================
SELECT
    jobid,
    jobname,
    schedule,
    command,
    active,
    database,
    username
FROM cron.job
WHERE jobname = 'check-notifications-daily';

-- ================================================
-- Informations
-- ================================================
-- Le job est maintenant planifié pour s'exécuter tous les jours à 9h00 UTC.
--
-- Pour voir l'historique des exécutions:
--   SELECT * FROM cron.job_run_details ORDER BY start_time DESC LIMIT 10;
--
-- Pour exécuter manuellement:
--   SELECT check_notification_alerts();
--
-- Pour désactiver temporairement:
--   UPDATE cron.job SET active = false WHERE jobname = 'check-notifications-daily';
--
-- Pour réactiver:
--   UPDATE cron.job SET active = true WHERE jobname = 'check-notifications-daily';
--
-- Pour supprimer:
--   SELECT cron.unschedule('check-notifications-daily');
-- ================================================
