-- V38__supplier_invoice_calculated_amount_estimate.sql
-- Keep the auto-calculated (unit_cost x actualQty) estimate alongside the
-- Accountant-confirmed total_amount, so an override is traceable against what
-- the system originally computed from the receipt.

ALTER TABLE supplier_invoices
    ADD COLUMN IF NOT EXISTS calculated_amount_estimate NUMERIC(18,2);
