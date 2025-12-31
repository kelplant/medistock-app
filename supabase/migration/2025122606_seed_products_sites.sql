-- Seed categories, products, and sites with normalized casing
-- Clears site-dependent data, reinserts requested sites, categories, and seeds the product catalog

-- Clean existing dependent data to allow site reset
TRUNCATE TABLE
    sale_batch_allocations,
    sale_items,
    sales,
    product_sales,
    product_transfers,
    inventories,
    stock_movements,
    purchase_batches,
    product_prices,
    products,
    customers,
    packaging_types
RESTART IDENTITY;

-- Reset sites
DELETE FROM sites;

WITH new_sites(name) AS (
    VALUES
        ('Basanga'),
        ('Iyanda'),
        ('New ngomakaminza'),
        ('Ibula'),
        ('Ntubya')
)
INSERT INTO sites (name)
SELECT upper(left(name, 1)) || lower(substring(name from 2))
FROM new_sites;

-- Prepare categories
WITH new_categories(name) AS (
    VALUES
        ('Deworming'),
        ('Trypanocidal'),
        ('Corticosteroid'),
        ('Dip'),
        ('Antibiotic'),
        ('Anti-inflammatory'),
        ('Booster'),
        ('Material'),
        ('Anti theleriosis'),
        ('Desinfectant'),
        ('Salt')
)
INSERT INTO categories (name)
SELECT upper(left(name, 1)) || lower(substring(name from 2))
FROM new_categories nc
WHERE NOT EXISTS (
    SELECT 1
    FROM categories c
    WHERE lower(c.name) = lower(nc.name)
);

-- Reset packaging types with English hierarchy
INSERT INTO packaging_types (name, level1_name, level2_name, default_conversion_factor, created_by)
VALUES
    ('Bottle/ml', 'Bottle', 'Ml', 100, 'system'),
    ('Unit', 'Unit', NULL, NULL, 'system');

-- Seed products across all sites
WITH category_lookup AS (
    SELECT id, lower(name) AS name_lower FROM categories
),
site_lookup AS (
    SELECT id FROM sites
),
packaging_choice AS (
    SELECT
        (SELECT id FROM packaging_types WHERE lower(name) = 'bottle/ml' LIMIT 1) AS bottle_id,
        (SELECT id FROM packaging_types WHERE lower(name) = 'unit' LIMIT 1) AS unit_id
),
product_data(product_name, product_description, category_name, unit_volume, unit_type, margin_percent) AS (
    VALUES
        ('Albendazole', 'Albendazole', 'Deworming', 1, 'unit', 30),
        ('Berenil', 'Diminazene', 'Trypanocidal', 1, 'unit', 30),
        ('Ivermectin Bottle (50mL)', 'Ivermectin', 'Deworming', 50, 'ml', 30),
        ('Bimectin plus (500ml)', 'Ivermectin + clorsulon', 'Deworming', 500, 'ml', 30),
        ('Paratop 1L', 'Cypermethrin', 'Dip', 1000, 'unit', 30),
        ('Dexajet', 'Dexamethasone 2mg/ml', 'Corticosteroid', 100, 'ml', 30),
        ('Intertrim', 'Sulfamethaxole/Trimethorpim', 'Antibiotic', 100, 'unit', 30),
        ('Ecotraz 250 (500ml)', 'Amitraz', 'Dip', 500, 'ml', 30),
        ('Fosbac/Bactofos + 100g', 'Oytetracycline LA', 'Antibiotic', 1, 'unit', 30),
        ('Intercam', 'Meloxicam', 'Anti-inflammatory', 100, 'unit', 30),
        ('Iron jet', 'Iron', 'Booster', 100, 'unit', 30),
        ('Ivermic F', 'Ivermectin + clorsulon', 'Deworming', 1, 'unit', 30),
        ('Karbadust 500g', 'Carbaryl', 'Dip', 1, 'unit', 30),
        ('Megapen', 'Penicilin G', 'Antibiotic', 50, 'unit', 30),
        ('Multivitamin', 'Vitamins', 'Booster', 50, 'unit', 30),
        ('Needle', 'Material', 'Material', 1, 'unit', 30),
        ('Nilzan', 'Levamisole + oxyclosanide', 'Deworming', 1, 'unit', 30),
        ('Oxyjet LA', 'Oxytetracycline LA', 'Antibiotic', 50, 'unit', 30),
        ('Parvexon', 'Buparvaquone', 'Anti theleriosis', 1, 'unit', 30),
        ('Parvexon plus', 'Buparvaquone + Diurétique', 'Anti theleriosis', 1, 'unit', 30),
        ('Phemyject', 'Phenylbutazone', 'Anti-inflammatory', 50, 'unit', 30),
        ('Plasti syringe (20mL)', 'Material', 'Material', 1, 'ml', 30),
        ('Povidone', 'Iode', 'Desinfectant', 1, 'unit', 30),
        ('Prazivet', 'Albendazole', 'Deworming', 1, 'unit', 30),
        ('Pro inject yellow', 'Closantel', 'Deworming', 1, 'unit', 30),
        ('Samorin', 'Isometamidium Chloride', 'Trypanocidal', 1, 'unit', 30),
        ('Stresspac poultry', 'Vitamins', 'Booster', 1, 'unit', 30),
        ('Eraditick Dip', 'Amitraz', 'Dip', 1, 'unit', 30),
        ('Syringe 20 ml', 'Material', 'Material', 1, 'ml', 30),
        ('Syringe 5ml', 'Material', 'Material', 1, 'ml', 30),
        ('Thermometer', 'Material', 'Material', 1, 'unit', 30),
        ('Weight tape', 'Material', 'Material', 1, 'unit', 30),
        ('Wound spray', 'Oxytetracycline/Chlorexidine', 'Antibiotic', 1, 'unit', 30),
        ('Gloves', 'Material', 'Material', 1, 'unit', 30),
        ('Tetra Oxy', 'oxytetracycline LA', 'Antibiotic', 1, 'unit', 30),
        ('Lignocaine 2%', 'Material', 'Material', 1, 'unit', 30),
        ('Marking stick', 'Material', 'Material', 1, 'unit', 30),
        ('Povidone', 'Povidone', 'Desinfectant', 1, 'unit', 30),
        ('Automatic syringe plastic', 'Material', 'Material', 1, 'unit', 30),
        ('Needle multiple use', 'Material', 'Material', 1, 'unit', 30),
        ('Bimectin 500ml', 'Bimectin', 'Desinfectant', 1, 'ml', 30),
        ('Needle 23G', 'Material', 'Material', 1, 'unit', 30),
        ('Syringue 2 ml', 'Material', 'Material', 1, 'ml', 30),
        ('Flukazole C', 'Triclobendazole / Oxfendazole', 'Deworming', 1, 'unit', 30),
        ('Tetra EyePowder', 'Tetracycline', 'Antibiotic', 1, 'unit', 30),
        ('COARSE SALT 50kg', '-', 'Salt', 1, 'unit', 30)
)
INSERT INTO products (
    name,
    description,
    category_id,
    unit_volume,
    unit,
    packaging_type_id,
    margin_type,
    margin_value,
    site_id,
    min_stock,
    max_stock
)
SELECT
    upper(left(pd.product_name, 1)) || lower(substring(pd.product_name from 2)),
    upper(left(pd.product_description, 1)) || lower(substring(pd.product_description from 2)),
    cl.id,
    pd.unit_volume,
    CASE
        WHEN lower(pd.unit_type) = 'ml' OR pd.unit_volume >= 50 THEN 'Ml'
        ELSE 'Unit'
    END,
    CASE
        WHEN lower(pd.unit_type) = 'ml' OR pd.unit_volume >= 50 THEN pc.bottle_id
        ELSE pc.unit_id
    END,
    'percentage',
    pd.margin_percent,
    s.id,
    0,
    0
FROM product_data pd
JOIN category_lookup cl ON cl.name_lower = lower(pd.category_name)
CROSS JOIN site_lookup s
CROSS JOIN packaging_choice pc;
