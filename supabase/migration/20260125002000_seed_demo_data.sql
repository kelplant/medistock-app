-- ============================================
-- MEDISTOCK DEMO DATA SEED
-- Uses EXISTING categories, sites, and packaging types
-- ============================================

-- ============================================
-- 0. CLEANUP - Delete previous seed data
-- ============================================
-- Order matters due to foreign key constraints

DELETE FROM stock_movements WHERE created_by = 'seed';
DELETE FROM sale_batch_allocations WHERE created_by = 'seed';
DELETE FROM sale_items WHERE created_by = 'seed';
DELETE FROM sales WHERE created_by = 'seed';
DELETE FROM purchase_batches WHERE created_by = 'seed';
DELETE FROM customers WHERE created_by = 'seed';
DELETE FROM products WHERE created_by = 'seed';

-- ============================================
-- CONSTANTS - Using existing UUIDs from database
-- ============================================
-- Categories:
--   Anti theleriosis: 94b28e1f-878c-4223-9ac6-d6a17cab3770
--   Anti-inflammatory: b3b38340-9fb8-44f7-8f45-1fbe0aec82e6
--   Antibiotic: 124c2490-1a36-4783-9c41-1ad6fec12a49
--   Booster: 55203526-972f-4154-9cf9-9d70124a561d
--   Corticosteroid: e547ad35-7629-41f4-9abf-ca206a8b2744
--   Desinfectant: 5c1fb686-05e5-418b-87ef-1114eec9e5ce
--   Deworming: 29b9d3fa-e7b2-4774-9575-339d825f548a
--   Dip: dfb96546-e29b-4b6a-8af3-7bade77d8dd4
--   Material: 3e0c7899-8eb2-455c-8f1d-225a55c9a267
--   Salt: 25e401fd-45e3-4370-a2d7-e7e7a789e67c
--   Trypanocidal: 7b61288f-19fd-499b-96df-8bb235e3d9c3
--
-- Sites:
--   Basanga: 9f1f33e4-43a3-48b7-9375-8d8ab5ca8341
--   Ibula: f4e79a53-62af-4116-8732-7c39abab588b
--   Iyanda: 3c8cd1fa-63dd-4583-9004-9cef91ba8fae
--   New ngomakaminza: 6773b7ff-6afe-49fa-bf90-02a685e3e10d
--   Ntubya: d0fba16e-bc14-4970-97c5-a2a9636822d3
--
-- Packaging Types (lookup by name):
--   Bottle/ml, Flacon/ml, Unit, Units, Boîte/Comprimés
-- ============================================

-- ============================================
-- 1. PRODUCTS (using existing categories, sites, packaging)
-- ============================================

DO $$
DECLARE
    -- Category UUIDs (existing)
    v_cat_anti_theleriosis UUID := '94b28e1f-878c-4223-9ac6-d6a17cab3770';
    v_cat_anti_inflammatory UUID := 'b3b38340-9fb8-44f7-8f45-1fbe0aec82e6';
    v_cat_antibiotic UUID := '124c2490-1a36-4783-9c41-1ad6fec12a49';
    v_cat_booster UUID := '55203526-972f-4154-9cf9-9d70124a561d';
    v_cat_corticosteroid UUID := 'e547ad35-7629-41f4-9abf-ca206a8b2744';
    v_cat_desinfectant UUID := '5c1fb686-05e5-418b-87ef-1114eec9e5ce';
    v_cat_deworming UUID := '29b9d3fa-e7b2-4774-9575-339d825f548a';
    v_cat_dip UUID := 'dfb96546-e29b-4b6a-8af3-7bade77d8dd4';
    v_cat_material UUID := '3e0c7899-8eb2-455c-8f1d-225a55c9a267';
    v_cat_salt UUID := '25e401fd-45e3-4370-a2d7-e7e7a789e67c';
    v_cat_trypanocidal UUID := '7b61288f-19fd-499b-96df-8bb235e3d9c3';

    -- Site UUIDs (existing)
    v_site_basanga UUID := '9f1f33e4-43a3-48b7-9375-8d8ab5ca8341';
    v_site_ibula UUID := 'f4e79a53-62af-4116-8732-7c39abab588b';
    v_site_iyanda UUID := '3c8cd1fa-63dd-4583-9004-9cef91ba8fae';
    v_site_ngomakaminza UUID := '6773b7ff-6afe-49fa-bf90-02a685e3e10d';
    v_site_ntubya UUID := 'd0fba16e-bc14-4970-97c5-a2a9636822d3';

    -- Packaging UUIDs (lookup from existing)
    v_pkg_bottle_ml UUID;
    v_pkg_flacon_ml UUID;
    v_pkg_unit UUID;
    v_pkg_units UUID;
    v_pkg_boite UUID;

    v_now BIGINT := EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;
BEGIN
    -- Lookup packaging types by exact name (existing in database)
    SELECT id INTO v_pkg_bottle_ml FROM packaging_types WHERE name = 'Bottle/ml' LIMIT 1;
    SELECT id INTO v_pkg_flacon_ml FROM packaging_types WHERE name = 'Flacon/ml' LIMIT 1;
    SELECT id INTO v_pkg_unit FROM packaging_types WHERE name = 'Unit' LIMIT 1;
    SELECT id INTO v_pkg_units FROM packaging_types WHERE name = 'Units' LIMIT 1;
    SELECT id INTO v_pkg_boite FROM packaging_types WHERE name = 'Boîte/Comprimés' LIMIT 1;

    -- Fallback: use bottle/ml or unit if specific ones not found
    IF v_pkg_bottle_ml IS NULL THEN
        SELECT id INTO v_pkg_bottle_ml FROM packaging_types WHERE name ILIKE '%bottle%' LIMIT 1;
    END IF;
    IF v_pkg_flacon_ml IS NULL THEN v_pkg_flacon_ml := v_pkg_bottle_ml; END IF;
    IF v_pkg_unit IS NULL THEN
        SELECT id INTO v_pkg_unit FROM packaging_types WHERE name ILIKE '%unit%' LIMIT 1;
    END IF;
    IF v_pkg_units IS NULL THEN v_pkg_units := v_pkg_unit; END IF;

    -- Debug: raise notice if packaging types not found
    IF v_pkg_bottle_ml IS NULL THEN
        RAISE NOTICE 'WARNING: No packaging type found for Bottle/ml';
    END IF;
    IF v_pkg_unit IS NULL THEN
        RAISE NOTICE 'WARNING: No packaging type found for Unit';
    END IF;

    -- ============================================
    -- DEWORMING products
    -- ============================================
    INSERT INTO products (id, name, description, unit_volume, category_id, packaging_type_id, selected_level, site_id, margin_type, margin_value, min_stock, max_stock, is_active, created_at, updated_at, created_by, updated_by)
    VALUES
        (gen_random_uuid(), 'Albendazole', 'Albendazole - Deworming', 1, v_cat_deworming, v_pkg_unit, 1, v_site_basanga, 'percentage', 25, 50, 500, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Bimectin Bottle 50mL', 'Ivermectin - Deworming injectable', 50, v_cat_deworming, v_pkg_bottle_ml, 1, v_site_basanga, 'percentage', 30, 10, 100, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Bimectin Plus', 'Ivermectin + Clorsulon - Deworming', 50, v_cat_deworming, v_pkg_bottle_ml, 1, v_site_iyanda, 'percentage', 30, 10, 100, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Nilzan', 'Levamisole + Oxyclosanide - Deworming', 100, v_cat_deworming, v_pkg_bottle_ml, 1, v_site_ngomakaminza, 'percentage', 25, 10, 100, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Prazivet', 'Praziquantel - Deworming tablets', 1, v_cat_deworming, v_pkg_unit, 1, v_site_ntubya, 'percentage', 25, 50, 500, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Pro inject yellow', 'Closantel - Deworming injectable', 100, v_cat_deworming, v_pkg_bottle_ml, 1, v_site_basanga, 'percentage', 25, 10, 100, true, v_now, v_now, 'seed', 'seed')
    ON CONFLICT DO NOTHING;

    -- ============================================
    -- TRYPANOCIDAL products
    -- ============================================
    INSERT INTO products (id, name, description, unit_volume, category_id, packaging_type_id, selected_level, site_id, margin_type, margin_value, min_stock, max_stock, is_active, created_at, updated_at, created_by, updated_by)
    VALUES
        (gen_random_uuid(), 'Berenil', 'Diminazene aceturate - Trypanocidal', 1, v_cat_trypanocidal, v_pkg_unit, 1, v_site_basanga, 'percentage', 35, 20, 200, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Samorin', 'Isometamidium chloride - Trypanocidal', 100, v_cat_trypanocidal, v_pkg_bottle_ml, 1, v_site_ibula, 'percentage', 35, 10, 100, true, v_now, v_now, 'seed', 'seed')
    ON CONFLICT DO NOTHING;

    -- ============================================
    -- DIP products
    -- ============================================
    INSERT INTO products (id, name, description, unit_volume, category_id, packaging_type_id, selected_level, site_id, margin_type, margin_value, min_stock, max_stock, is_active, created_at, updated_at, created_by, updated_by)
    VALUES
        (gen_random_uuid(), 'Paratop 1L', 'Cypermethrin - Dip concentrate', 1, v_cat_dip, v_pkg_bottle_ml, 1, v_site_basanga, 'percentage', 20, 5, 50, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Ecotraz Bottle', 'Amitraz - Dip concentrate', 100, v_cat_dip, v_pkg_bottle_ml, 1, v_site_ibula, 'percentage', 20, 10, 100, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Karbadust', 'Carbaryl powder - Dip', 1, v_cat_dip, v_pkg_unit, 1, v_site_ngomakaminza, 'percentage', 20, 10, 100, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Supadip', 'Amitraz - Dip concentrate', 100, v_cat_dip, v_pkg_bottle_ml, 1, v_site_ntubya, 'percentage', 20, 10, 100, true, v_now, v_now, 'seed', 'seed')
    ON CONFLICT DO NOTHING;

    -- ============================================
    -- CORTICOSTEROID products
    -- ============================================
    INSERT INTO products (id, name, description, unit_volume, category_id, packaging_type_id, selected_level, site_id, margin_type, margin_value, min_stock, max_stock, is_active, created_at, updated_at, created_by, updated_by)
    VALUES
        (gen_random_uuid(), 'Dexajet Bottle 100mL', 'Dexamethasone 2mg/ml - Corticosteroid', 100, v_cat_corticosteroid, v_pkg_bottle_ml, 1, v_site_basanga, 'percentage', 30, 5, 50, true, v_now, v_now, 'seed', 'seed')
    ON CONFLICT DO NOTHING;

    -- ============================================
    -- ANTIBIOTIC products
    -- ============================================
    INSERT INTO products (id, name, description, unit_volume, category_id, packaging_type_id, selected_level, site_id, margin_type, margin_value, min_stock, max_stock, is_active, created_at, updated_at, created_by, updated_by)
    VALUES
        (gen_random_uuid(), 'Sulfatrim Bottle 100mL', 'Sulfamethoxazole/Trimethoprim - Antibiotic', 100, v_cat_antibiotic, v_pkg_bottle_ml, 1, v_site_basanga, 'percentage', 25, 10, 100, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Fosbac', 'Fosfomycin - Antibiotic', 100, v_cat_antibiotic, v_pkg_bottle_ml, 1, v_site_iyanda, 'percentage', 25, 10, 100, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Megapen Bottle', 'Penicillin/Streptomycin - Antibiotic', 100, v_cat_antibiotic, v_pkg_bottle_ml, 1, v_site_ngomakaminza, 'percentage', 25, 10, 100, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Oxyjet LA Bottle', 'Oxytetracycline LA - Antibiotic', 100, v_cat_antibiotic, v_pkg_bottle_ml, 1, v_site_basanga, 'percentage', 25, 10, 100, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Wound spray', 'Oxytetracycline spray - Antibiotic topical', 200, v_cat_antibiotic, v_pkg_bottle_ml, 1, v_site_iyanda, 'percentage', 25, 10, 100, true, v_now, v_now, 'seed', 'seed')
    ON CONFLICT DO NOTHING;

    -- ============================================
    -- ANTI-INFLAMMATORY products
    -- ============================================
    INSERT INTO products (id, name, description, unit_volume, category_id, packaging_type_id, selected_level, site_id, margin_type, margin_value, min_stock, max_stock, is_active, created_at, updated_at, created_by, updated_by)
    VALUES
        (gen_random_uuid(), 'Intercam Bottle (100mL)', 'Meloxicam - Anti-inflammatory', 100, v_cat_anti_inflammatory, v_pkg_bottle_ml, 1, v_site_basanga, 'percentage', 30, 5, 50, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Phemyject Bottle', 'Phenylbutazone - Anti-inflammatory', 100, v_cat_anti_inflammatory, v_pkg_bottle_ml, 1, v_site_iyanda, 'percentage', 30, 5, 50, true, v_now, v_now, 'seed', 'seed')
    ON CONFLICT DO NOTHING;

    -- ============================================
    -- BOOSTER products
    -- ============================================
    INSERT INTO products (id, name, description, unit_volume, category_id, packaging_type_id, selected_level, site_id, margin_type, margin_value, min_stock, max_stock, is_active, created_at, updated_at, created_by, updated_by)
    VALUES
        (gen_random_uuid(), 'Iron jet Bottle', 'Iron dextran - Booster', 100, v_cat_booster, v_pkg_bottle_ml, 1, v_site_basanga, 'percentage', 20, 10, 100, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Multivitamin Bottle', 'ADE vitamins - Booster', 100, v_cat_booster, v_pkg_bottle_ml, 1, v_site_iyanda, 'percentage', 20, 10, 100, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Stresspac poultry', 'Electrolytes/vitamins - Booster poultry', 1, v_cat_booster, v_pkg_unit, 1, v_site_ntubya, 'percentage', 20, 20, 200, true, v_now, v_now, 'seed', 'seed')
    ON CONFLICT DO NOTHING;

    -- ============================================
    -- ANTI-THELERIOSIS products
    -- ============================================
    INSERT INTO products (id, name, description, unit_volume, category_id, packaging_type_id, selected_level, site_id, margin_type, margin_value, min_stock, max_stock, is_active, created_at, updated_at, created_by, updated_by)
    VALUES
        (gen_random_uuid(), 'Parvexon', 'Buparvaquone - Anti-theleriosis', 100, v_cat_anti_theleriosis, v_pkg_bottle_ml, 1, v_site_basanga, 'percentage', 35, 5, 50, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Parvexon plus', 'Buparvaquone + supplement - Anti-theleriosis', 100, v_cat_anti_theleriosis, v_pkg_bottle_ml, 1, v_site_ibula, 'percentage', 35, 5, 50, true, v_now, v_now, 'seed', 'seed')
    ON CONFLICT DO NOTHING;

    -- ============================================
    -- DESINFECTANT products
    -- ============================================
    INSERT INTO products (id, name, description, unit_volume, category_id, packaging_type_id, selected_level, site_id, margin_type, margin_value, min_stock, max_stock, is_active, created_at, updated_at, created_by, updated_by)
    VALUES
        (gen_random_uuid(), 'Povidone', 'Povidone iodine - Desinfectant', 500, v_cat_desinfectant, v_pkg_bottle_ml, 1, v_site_basanga, 'percentage', 20, 5, 50, true, v_now, v_now, 'seed', 'seed')
    ON CONFLICT DO NOTHING;

    -- ============================================
    -- MATERIAL products
    -- ============================================
    INSERT INTO products (id, name, description, unit_volume, category_id, packaging_type_id, selected_level, site_id, margin_type, margin_value, min_stock, max_stock, is_active, created_at, updated_at, created_by, updated_by)
    VALUES
        (gen_random_uuid(), 'Needle', 'Hypodermic needles', 1, v_cat_material, v_pkg_units, 1, v_site_basanga, 'percentage', 50, 100, 1000, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Plasti syringe (20mL)', 'Plastic disposable syringe 20mL', 1, v_cat_material, v_pkg_units, 1, v_site_ibula, 'percentage', 50, 50, 500, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Syringe 20 ml', 'Reusable syringe 20mL', 1, v_cat_material, v_pkg_units, 1, v_site_iyanda, 'percentage', 50, 50, 500, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Syringe 5ml', 'Reusable syringe 5mL', 1, v_cat_material, v_pkg_units, 1, v_site_ngomakaminza, 'percentage', 50, 50, 500, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Thermometer', 'Veterinary thermometer', 1, v_cat_material, v_pkg_units, 1, v_site_ntubya, 'percentage', 40, 5, 50, true, v_now, v_now, 'seed', 'seed'),
        (gen_random_uuid(), 'Weight tape', 'Cattle weight estimation tape', 1, v_cat_material, v_pkg_units, 1, v_site_basanga, 'percentage', 40, 5, 50, true, v_now, v_now, 'seed', 'seed')
    ON CONFLICT DO NOTHING;

END $$;

-- ============================================
-- 2. PURCHASE BATCHES (initial stock)
-- ============================================

DO $$
DECLARE
    v_product RECORD;
    v_now BIGINT := EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;
    v_30_days_ago BIGINT := (EXTRACT(EPOCH FROM NOW()) - (30 * 24 * 60 * 60))::BIGINT * 1000;
    v_60_days_ago BIGINT := (EXTRACT(EPOCH FROM NOW()) - (60 * 24 * 60 * 60))::BIGINT * 1000;
    v_1_year_future BIGINT := (EXTRACT(EPOCH FROM NOW()) + (365 * 24 * 60 * 60))::BIGINT * 1000;
    v_6_months_future BIGINT := (EXTRACT(EPOCH FROM NOW()) + (180 * 24 * 60 * 60))::BIGINT * 1000;
    v_batch_qty DOUBLE PRECISION;
    v_purchase_price DOUBLE PRECISION;
BEGIN
    FOR v_product IN
        SELECT id, name, min_stock, max_stock, site_id
        FROM products
        WHERE created_by = 'seed'
    LOOP
        -- Generate random initial quantity and price
        v_batch_qty := v_product.min_stock + (random() * (v_product.max_stock - v_product.min_stock));
        v_purchase_price := 5 + (random() * 95); -- Price between 5 and 100 ZMW

        -- Create 2 batches per product
        INSERT INTO purchase_batches (id, product_id, site_id, batch_number, purchase_date, initial_quantity, remaining_quantity, purchase_price, supplier_name, expiry_date, is_exhausted, created_at, updated_at, created_by, updated_by)
        VALUES
            (gen_random_uuid(), v_product.id, v_product.site_id, 'LOT-' || SUBSTRING(v_product.id::text, 1, 8), v_60_days_ago, v_batch_qty * 0.4, v_batch_qty * 0.2, v_purchase_price * 0.95, 'VetSupplies Zambia', v_1_year_future, false, v_now, v_now, 'seed', 'seed'),
            (gen_random_uuid(), v_product.id, v_product.site_id, 'LOT-' || SUBSTRING(gen_random_uuid()::text, 1, 8), v_30_days_ago, v_batch_qty * 0.6, v_batch_qty * 0.5, v_purchase_price, 'AgriMed Africa', v_6_months_future, false, v_now, v_now, 'seed', 'seed');
    END LOOP;
END $$;

-- ============================================
-- 3. CUSTOMERS (using existing sites)
-- ============================================

INSERT INTO customers (id, name, phone, address, notes, site_id, created_at, updated_at, created_by, updated_by)
VALUES
    (gen_random_uuid(), 'Kasama Cattle Ranch', '+260 97 123 4567', 'Plot 45, Kasama', 'Large cattle farm - regular customer', '9f1f33e4-43a3-48b7-9375-8d8ab5ca8341', EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, 'seed', 'seed'),
    (gen_random_uuid(), 'Chisamba Cooperative', '+260 96 234 5678', 'Chisamba District', 'Smallholder farmers cooperative', 'f4e79a53-62af-4116-8732-7c39abab588b', EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, 'seed', 'seed'),
    (gen_random_uuid(), 'Sunrise Poultry Farm', '+260 95 345 6789', 'Kafue Road, Lusaka', 'Large poultry operation - bulk orders', '3c8cd1fa-63dd-4583-9004-9cef91ba8fae', EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, 'seed', 'seed'),
    (gen_random_uuid(), 'Makeni Smallholders', '+260 97 456 7890', 'Makeni Area', 'Group of small farmers', '6773b7ff-6afe-49fa-bf90-02a685e3e10d', EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, 'seed', 'seed'),
    (gen_random_uuid(), 'Dr. Mwanza Vet Clinic', '+260 96 567 8901', 'Cairo Road, Lusaka', 'Veterinary clinic - wholesale', 'd0fba16e-bc14-4970-97c5-a2a9636822d3', EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, 'seed', 'seed'),
    (gen_random_uuid(), 'Chipata Farmers Union', '+260 97 678 9012', 'Chipata', 'Eastern province farmers group', '9f1f33e4-43a3-48b7-9375-8d8ab5ca8341', EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, 'seed', 'seed'),
    (gen_random_uuid(), 'Ndola Dairy Farm', '+260 96 789 0123', 'Ndola Industrial', 'Dairy cattle operation', 'f4e79a53-62af-4116-8732-7c39abab588b', EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, 'seed', 'seed'),
    (gen_random_uuid(), 'Mumbwa Goat Project', '+260 95 890 1234', 'Mumbwa District', 'Goat farming project', '3c8cd1fa-63dd-4583-9004-9cef91ba8fae', EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, EXTRACT(EPOCH FROM NOW())::BIGINT * 1000, 'seed', 'seed')
ON CONFLICT DO NOTHING;

-- ============================================
-- 4. SALES (last 30 days)
-- ============================================

DO $$
DECLARE
    v_customer RECORD;
    v_product RECORD;
    v_sale_id UUID;
    v_sale_item_id UUID;
    v_now BIGINT := EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;
    v_sale_date BIGINT;
    v_qty DOUBLE PRECISION;
    v_selling_price DOUBLE PRECISION;
    v_day INTEGER;
BEGIN
    -- Generate sales for the last 30 days
    FOR v_day IN 1..30 LOOP
        v_sale_date := (EXTRACT(EPOCH FROM NOW()) - (v_day * 24 * 60 * 60))::BIGINT * 1000;

        -- 2-5 sales per day
        FOR i IN 1..(2 + floor(random() * 4)::integer) LOOP
            -- Get random customer
            SELECT * INTO v_customer FROM customers WHERE created_by = 'seed' ORDER BY random() LIMIT 1;

            IF v_customer IS NOT NULL THEN
                -- Create sale
                v_sale_id := gen_random_uuid();

                -- Get a random product's site for the sale
                SELECT site_id INTO v_product FROM products WHERE created_by = 'seed' ORDER BY random() LIMIT 1;

                INSERT INTO sales (id, customer_name, customer_id, date, total_amount, site_id, created_at, created_by)
                VALUES (v_sale_id, v_customer.name, v_customer.id, v_sale_date, 0, v_product.site_id, v_now, 'seed');

                -- Add 1-4 items per sale
                FOR j IN 1..(1 + floor(random() * 4)::integer) LOOP
                    -- Get random product with stock (derive unit from packaging_types)
                    SELECT p.*, pb.id as batch_id, pb.purchase_price, pb.remaining_quantity, pb.site_id as batch_site_id,
                           CASE WHEN p.selected_level = 2 THEN pt.level2_name ELSE pt.level1_name END as derived_unit
                    INTO v_product
                    FROM products p
                    JOIN purchase_batches pb ON p.id = pb.product_id
                    LEFT JOIN packaging_types pt ON p.packaging_type_id = pt.id
                    WHERE p.created_by = 'seed'
                      AND pb.remaining_quantity > 0
                      AND pb.is_exhausted = false
                    ORDER BY random()
                    LIMIT 1;

                    IF v_product IS NOT NULL THEN
                        v_qty := 1 + floor(random() * 5);
                        IF v_qty > v_product.remaining_quantity THEN
                            v_qty := v_product.remaining_quantity;
                        END IF;

                        v_selling_price := v_product.purchase_price * (1 + (v_product.margin_value / 100));
                        v_sale_item_id := gen_random_uuid();

                        -- Insert sale item (unit is denormalized from packaging_types)
                        INSERT INTO sale_items (id, sale_id, product_id, product_name, unit, quantity, price_per_unit, subtotal, created_at, created_by)
                        VALUES (v_sale_item_id, v_sale_id, v_product.id, v_product.name, v_product.derived_unit, v_qty, v_selling_price, v_qty * v_selling_price, v_now, 'seed');

                        -- Insert batch allocation
                        INSERT INTO sale_batch_allocations (id, sale_item_id, batch_id, quantity_allocated, purchase_price_at_allocation, created_at, created_by)
                        VALUES (gen_random_uuid(), v_sale_item_id, v_product.batch_id, v_qty, v_product.purchase_price, v_now, 'seed');

                        -- Update batch remaining quantity
                        UPDATE purchase_batches
                        SET remaining_quantity = remaining_quantity - v_qty,
                            is_exhausted = CASE WHEN remaining_quantity - v_qty <= 0 THEN true ELSE false END
                        WHERE id = v_product.batch_id;
                    END IF;
                END LOOP;

                -- Update sale total
                UPDATE sales
                SET total_amount = (SELECT COALESCE(SUM(subtotal), 0) FROM sale_items WHERE sale_id = v_sale_id)
                WHERE id = v_sale_id;
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- ============================================
-- 5. STOCK MOVEMENTS (from sales)
-- ============================================

INSERT INTO stock_movements (id, product_id, site_id, quantity, type, date, purchase_price_at_movement, selling_price_at_movement, created_at, created_by)
SELECT
    gen_random_uuid(),
    si.product_id,
    s.site_id,
    si.quantity,
    'OUT',
    s.date,
    sba.purchase_price_at_allocation,
    si.price_per_unit,
    EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    'seed'
FROM sale_items si
JOIN sales s ON si.sale_id = s.id
JOIN sale_batch_allocations sba ON si.id = sba.sale_item_id
WHERE s.created_by = 'seed';

-- ============================================
-- 6. RECORD MIGRATION
-- ============================================

INSERT INTO schema_migrations (name, checksum, applied_by, success, execution_time_ms)
VALUES ('20260125002000_seed_demo_data', NULL, 'supabase_cli', TRUE, NULL)
ON CONFLICT (name) DO NOTHING;
