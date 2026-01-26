-- Migration: Add suppliers table and supplier_id to purchase_batches
-- Date: 2026-01-25
-- Description: Adds supplier management similar to customers (Supabase/PostgreSQL)

-- Create suppliers table (identical to customers)
CREATE TABLE IF NOT EXISTS public.suppliers (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT,
    email TEXT,
    address TEXT,
    notes TEXT,
    site_id TEXT REFERENCES public.sites(id),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0,
    created_by TEXT NOT NULL DEFAULT '',
    updated_by TEXT NOT NULL DEFAULT '',
    client_id TEXT
);

-- Add supplier_id to purchase_batches if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
        AND table_name = 'purchase_batches'
        AND column_name = 'supplier_id'
    ) THEN
        ALTER TABLE public.purchase_batches ADD COLUMN supplier_id TEXT REFERENCES public.suppliers(id);
    END IF;
END $$;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_suppliers_site ON public.suppliers(site_id);
CREATE INDEX IF NOT EXISTS idx_suppliers_is_active ON public.suppliers(is_active);
CREATE INDEX IF NOT EXISTS idx_purchase_batches_supplier ON public.purchase_batches(supplier_id);

-- Enable RLS for suppliers table
ALTER TABLE public.suppliers ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for suppliers (same as customers)
DROP POLICY IF EXISTS "suppliers_all" ON public.suppliers;
CREATE POLICY "suppliers_all" ON public.suppliers
    FOR ALL
    TO authenticated
    USING (true)
    WITH CHECK (true);

-- Allow anon access for initial sync
DROP POLICY IF EXISTS "suppliers_anon_select" ON public.suppliers;
CREATE POLICY "suppliers_anon_select" ON public.suppliers
    FOR SELECT
    TO anon
    USING (true);

DROP POLICY IF EXISTS "suppliers_anon_insert" ON public.suppliers;
CREATE POLICY "suppliers_anon_insert" ON public.suppliers
    FOR INSERT
    TO anon
    WITH CHECK (true);

DROP POLICY IF EXISTS "suppliers_anon_update" ON public.suppliers;
CREATE POLICY "suppliers_anon_update" ON public.suppliers
    FOR UPDATE
    TO anon
    USING (true)
    WITH CHECK (true);

DROP POLICY IF EXISTS "suppliers_anon_delete" ON public.suppliers;
CREATE POLICY "suppliers_anon_delete" ON public.suppliers
    FOR DELETE
    TO anon
    USING (true);

-- Enable realtime for suppliers
ALTER PUBLICATION supabase_realtime ADD TABLE public.suppliers;
