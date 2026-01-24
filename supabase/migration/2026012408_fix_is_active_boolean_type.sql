    -- ============================================================================
    -- MEDISTOCK: Fix is_active column type from INTEGER to BOOLEAN
    -- Some tables have is_active stored as INTEGER (1/0) instead of BOOLEAN
    -- This causes JSON deserialization errors in the app
    -- Must DROP DEFAULT first, then change type, then SET DEFAULT again
    -- ============================================================================

    -- SITES
    ALTER TABLE sites ALTER COLUMN is_active DROP DEFAULT;
    ALTER TABLE sites ALTER COLUMN is_active TYPE BOOLEAN USING CASE WHEN is_active::text = '1' OR is_active::text = 'true' THEN TRUE ELSE FALSE END;
    ALTER TABLE sites ALTER COLUMN is_active SET DEFAULT TRUE;

    -- CATEGORIES
    ALTER TABLE categories ALTER COLUMN is_active DROP DEFAULT;
    ALTER TABLE categories ALTER COLUMN is_active TYPE BOOLEAN USING CASE WHEN is_active::text = '1' OR is_active::text = 'true' THEN TRUE ELSE FALSE END;
    ALTER TABLE categories ALTER COLUMN is_active SET DEFAULT TRUE;

    -- PRODUCTS
    ALTER TABLE products ALTER COLUMN is_active DROP DEFAULT;
    ALTER TABLE products ALTER COLUMN is_active TYPE BOOLEAN USING CASE WHEN is_active::text = '1' OR is_active::text = 'true' THEN TRUE ELSE FALSE END;
    ALTER TABLE products ALTER COLUMN is_active SET DEFAULT TRUE;

    -- CUSTOMERS
    ALTER TABLE customers ALTER COLUMN is_active DROP DEFAULT;
    ALTER TABLE customers ALTER COLUMN is_active TYPE BOOLEAN USING CASE WHEN is_active::text = '1' OR is_active::text = 'true' THEN TRUE ELSE FALSE END;
    ALTER TABLE customers ALTER COLUMN is_active SET DEFAULT TRUE;

    -- PACKAGING_TYPES
    ALTER TABLE packaging_types ALTER COLUMN is_active DROP DEFAULT;
    ALTER TABLE packaging_types ALTER COLUMN is_active TYPE BOOLEAN USING CASE WHEN is_active::text = '1' OR is_active::text = 'true' THEN TRUE ELSE FALSE END;
    ALTER TABLE packaging_types ALTER COLUMN is_active SET DEFAULT TRUE;

    -- APP_USERS is_active
    ALTER TABLE app_users ALTER COLUMN is_active DROP DEFAULT;
    ALTER TABLE app_users ALTER COLUMN is_active TYPE BOOLEAN USING CASE WHEN is_active::text = '1' OR is_active::text = 'true' THEN TRUE ELSE FALSE END;
    ALTER TABLE app_users ALTER COLUMN is_active SET DEFAULT TRUE;

    -- APP_USERS is_admin
    ALTER TABLE app_users ALTER COLUMN is_admin DROP DEFAULT;
    ALTER TABLE app_users ALTER COLUMN is_admin TYPE BOOLEAN USING CASE WHEN is_admin::text = '1' OR is_admin::text = 'true' THEN TRUE ELSE FALSE END;
    ALTER TABLE app_users ALTER COLUMN is_admin SET DEFAULT FALSE;
