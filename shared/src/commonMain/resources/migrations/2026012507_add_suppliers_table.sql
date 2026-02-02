-- Migration: Add suppliers table and supplier_id to purchase_batches
-- Date: 2026-01-25
-- Description: Adds supplier management similar to customers

-- Create suppliers table (no site_id - suppliers are global, not site-specific)
CREATE TABLE IF NOT EXISTS suppliers (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    phone TEXT,
    email TEXT,
    address TEXT,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0,
    created_by TEXT NOT NULL DEFAULT '',
    updated_by TEXT NOT NULL DEFAULT '',
    client_id TEXT
);

-- Add supplier_id to purchase_batches
ALTER TABLE purchase_batches ADD COLUMN IF NOT EXISTS supplier_id UUID REFERENCES suppliers(id);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_suppliers_is_active ON suppliers(is_active);
CREATE INDEX IF NOT EXISTS idx_purchase_batches_supplier ON purchase_batches(supplier_id);
