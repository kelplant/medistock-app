-- ============================================================================
-- MEDISTOCK: Recovery Admin Function
-- Creates a function to generate a recovery admin user in emergency situations
-- This function is ONLY accessible via SQL Editor (service_role)
-- ============================================================================

-- Enable pgcrypto for password hashing
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Create the recovery admin function
CREATE OR REPLACE FUNCTION create_recovery_admin(
    p_username TEXT DEFAULT 'recovery_admin',
    p_password TEXT DEFAULT 'Recovery123!',
    p_secret_key TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_recovery_key TEXT;
    v_user_id UUID;
    v_password_hash TEXT;
    v_now BIGINT;
BEGIN
    -- 1. Check recovery secret key if configured
    SELECT value INTO v_recovery_key
    FROM app_config
    WHERE key = 'recovery_secret_key';

    IF v_recovery_key IS NOT NULL AND v_recovery_key != '' AND
       (p_secret_key IS NULL OR p_secret_key != v_recovery_key) THEN
        RETURN jsonb_build_object(
            'success', FALSE,
            'error', 'Invalid recovery key. Check app_config.recovery_secret_key'
        );
    END IF;

    -- 2. Validate inputs
    IF length(p_username) < 3 THEN
        RETURN jsonb_build_object(
            'success', FALSE,
            'error', 'Username must be at least 3 characters'
        );
    END IF;

    IF length(p_password) < 8 THEN
        RETURN jsonb_build_object(
            'success', FALSE,
            'error', 'Password must be at least 8 characters'
        );
    END IF;

    -- 3. Generate UUID and hash password
    v_user_id := gen_random_uuid();
    v_password_hash := crypt(p_password, gen_salt('bf'));
    v_now := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;

    -- 4. Create or update user in users table
    INSERT INTO app_users (
        id,
        username,
        password_hash,
        name,
        is_admin,
        is_active,
        auth_migrated,
        created_at,
        updated_at
    )
    VALUES (
        v_user_id::text,
        p_username,
        v_password_hash,
        'Recovery Administrator',
        1,  -- is_admin
        1,  -- is_active
        0,  -- Not migrated to Supabase Auth yet
        v_now,
        v_now
    )
    ON CONFLICT (username) DO UPDATE SET
        password_hash = EXCLUDED.password_hash,
        is_admin = 1,
        is_active = 1,
        auth_migrated = 0,
        updated_at = EXCLUDED.updated_at;

    -- If username already existed, get its ID
    IF NOT FOUND THEN
        SELECT id::uuid INTO v_user_id FROM app_users WHERE username = p_username;
    END IF;

    RETURN jsonb_build_object(
        'success', TRUE,
        'message', 'Recovery admin created successfully',
        'username', p_username,
        'temporary_password', p_password,
        'user_id', v_user_id,
        'warning', 'CHANGE PASSWORD IMMEDIATELY AFTER LOGIN!'
    );
END;
$$;

-- CRITICAL: Revoke execute from all roles except service_role
-- This ensures the function can ONLY be called via SQL Editor
REVOKE EXECUTE ON FUNCTION create_recovery_admin FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION create_recovery_admin FROM anon;
REVOKE EXECUTE ON FUNCTION create_recovery_admin FROM authenticated;

COMMENT ON FUNCTION create_recovery_admin IS
'EMERGENCY USE ONLY - Creates a recovery admin user.
Usage: Supabase Dashboard → SQL Editor → SELECT create_recovery_admin();
The user will be migrated to Supabase Auth on first login.
IMPORTANT: Change the password immediately after login!';

-- ============================================================================
-- Verification & Usage Instructions
-- ============================================================================
DO $$
BEGIN
    RAISE NOTICE '============================================================';
    RAISE NOTICE 'Recovery Admin Function Created';
    RAISE NOTICE '============================================================';
    RAISE NOTICE '';
    RAISE NOTICE 'USAGE (SQL Editor only):';
    RAISE NOTICE '  SELECT create_recovery_admin();';
    RAISE NOTICE '  SELECT create_recovery_admin(''my_admin'', ''MyPass123!'');';
    RAISE NOTICE '';
    RAISE NOTICE 'If recovery_secret_key is set in app_config:';
    RAISE NOTICE '  SELECT create_recovery_admin(''admin'', ''Pass123!'', ''secret'');';
    RAISE NOTICE '';
    RAISE NOTICE 'This function is NOT accessible from the app (REVOKE applied).';
    RAISE NOTICE '============================================================';
END $$;
