-- ============================================================================
-- MEDISTOCK: Fix is_admin() function for BOOLEAN comparison
-- The original function compared is_admin = 1 but the column is BOOLEAN
-- ============================================================================

CREATE OR REPLACE FUNCTION is_admin()
RETURNS BOOLEAN AS $$
BEGIN
    -- Check if user exists in users table and is admin
    RETURN EXISTS (
        SELECT 1 FROM app_users
        WHERE id = auth.uid()
        AND is_admin = TRUE
        AND is_active = TRUE
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;
