-- ============================================================================
-- MEDISTOCK: RLS Security Fixes
-- Fixes security issues identified in code review:
-- 1. Add is_active check in has_site_access function
-- 2. Restrict inventory_items policy to check site access via parent inventory
-- ============================================================================

-- ============================================================================
-- FIX 1: Update has_site_access to check if user is active
-- ============================================================================
CREATE OR REPLACE FUNCTION has_site_access(p_site_id TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    -- Admins have access to all sites
    IF is_admin() THEN
        RETURN TRUE;
    END IF;

    -- Check that the user is active (prevents access with stale JWT tokens)
    IF NOT EXISTS (
        SELECT 1 FROM app_users
        WHERE id = auth.uid()::text
        AND is_active = 1
    ) THEN
        RETURN FALSE;
    END IF;

    -- Check user_sites table for access
    RETURN EXISTS (
        SELECT 1 FROM user_sites
        WHERE user_id = auth.uid()::text
        AND site_id = p_site_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- ============================================================================
-- FIX 2: Update inventory_items policy to check site access via parent inventory
-- ============================================================================

-- Drop the overly permissive policy
DROP POLICY IF EXISTS "inventory_items_all" ON inventory_items;

-- Create proper policies with site access check
CREATE POLICY "inventory_items_select" ON inventory_items FOR SELECT TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM inventories i
        WHERE i.id = inventory_items.inventory_id
        AND (is_admin() OR has_site_access(i.site_id))
    )
);

CREATE POLICY "inventory_items_insert" ON inventory_items FOR INSERT TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1 FROM inventories i
        WHERE i.id = inventory_items.inventory_id
        AND (is_admin() OR has_site_access(i.site_id))
    )
);

CREATE POLICY "inventory_items_update" ON inventory_items FOR UPDATE TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM inventories i
        WHERE i.id = inventory_items.inventory_id
        AND (is_admin() OR has_site_access(i.site_id))
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1 FROM inventories i
        WHERE i.id = inventory_items.inventory_id
        AND (is_admin() OR has_site_access(i.site_id))
    )
);

CREATE POLICY "inventory_items_delete" ON inventory_items FOR DELETE TO authenticated
USING (is_admin());

-- ============================================================================
-- FIX 3: Update sale_items policy similarly (was also too permissive)
-- ============================================================================

-- Drop the overly permissive policy
DROP POLICY IF EXISTS "sale_items_all" ON sale_items;

-- Create proper policies with site access check
CREATE POLICY "sale_items_select" ON sale_items FOR SELECT TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM sales s
        WHERE s.id = sale_items.sale_id
        AND (is_admin() OR has_site_access(s.site_id))
    )
);

CREATE POLICY "sale_items_insert" ON sale_items FOR INSERT TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1 FROM sales s
        WHERE s.id = sale_items.sale_id
        AND (is_admin() OR has_site_access(s.site_id))
    )
);

CREATE POLICY "sale_items_update" ON sale_items FOR UPDATE TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM sales s
        WHERE s.id = sale_items.sale_id
        AND (is_admin() OR has_site_access(s.site_id))
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1 FROM sales s
        WHERE s.id = sale_items.sale_id
        AND (is_admin() OR has_site_access(s.site_id))
    )
);

CREATE POLICY "sale_items_delete" ON sale_items FOR DELETE TO authenticated
USING (is_admin());

-- ============================================================================
-- Verification
-- ============================================================================
DO $$
BEGIN
    RAISE NOTICE '============================================================';
    RAISE NOTICE 'RLS Security Fixes Applied';
    RAISE NOTICE '============================================================';
    RAISE NOTICE '';
    RAISE NOTICE 'Changes:';
    RAISE NOTICE '  1. has_site_access now checks user is_active status';
    RAISE NOTICE '  2. inventory_items: restricted to site access via inventory';
    RAISE NOTICE '  3. sale_items: restricted to site access via sale';
    RAISE NOTICE '';
    RAISE NOTICE 'Security improvements:';
    RAISE NOTICE '  - Deactivated users with stale JWTs cannot access data';
    RAISE NOTICE '  - Users can only access inventory/sale items from their sites';
    RAISE NOTICE '============================================================';
END $$;
