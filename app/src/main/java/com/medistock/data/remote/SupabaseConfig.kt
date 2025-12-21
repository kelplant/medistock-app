package com.medistock.data.remote

/**
 * Configuration Supabase pour Medistock
 *
 * IMPORTANT: Remplacez ces valeurs par vos vraies credentials Supabase
 *
 * Pour trouver vos credentials:
 * 1. Allez sur https://app.supabase.com
 * 2. Sélectionnez votre projet
 * 3. Allez dans Settings > API
 * 4. Copiez l'URL et la clé anon
 */
object SupabaseConfig {
    /**
     * URL du projet Supabase
     * Format: https://xxxxxxxxxxxxx.supabase.co
     */
    const val SUPABASE_URL = "https://YOUR_PROJECT_ID.supabase.co"

    /**
     * Clé anon/public Supabase
     * Cette clé peut être exposée dans l'app car elle respecte les politiques RLS
     */
    const val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"

    /**
     * Active/désactive le mode debug pour les logs Supabase
     */
    const val DEBUG_MODE = true
}
