-- V28__correction_voucher_adjustments.sql
-- Support for Correction Voucher (US-WMS-29, Spec 008): financial-only corrections
-- posted against invoices/payment_receipts/supplier_invoices/supplier_payments whose
-- original accounting period is already CLOSED. Reuses the existing adjustments table
-- and its already-present type = 'CORRECTION_VOUCHER' value instead of a new table.

-- warehouse_id/product_id/quantity_adjustment stay required for the four inventory
-- adjustment types (STOCK_TAKE, TRANSFER_DISCREPANCY, DISPOSAL, RETURN_TO_VENDOR);
-- only CORRECTION_VOUCHER rows are allowed to omit them.
ALTER TABLE adjustments ALTER COLUMN warehouse_id DROP NOT NULL;
ALTER TABLE adjustments ALTER COLUMN product_id DROP NOT NULL;
ALTER TABLE adjustments ALTER COLUMN quantity_adjustment DROP NOT NULL;

ALTER TABLE adjustments ADD CONSTRAINT chk_adjustments_inventory_fields_required
    CHECK (
        type = 'CORRECTION_VOUCHER'
        OR (warehouse_id IS NOT NULL AND product_id IS NOT NULL AND quantity_adjustment IS NOT NULL)
    );

-- Signed monetary delta applied to dealers.current_balance / suppliers.current_balance.
-- Only populated for type = 'CORRECTION_VOUCHER'; quantity_adjustment is not reused for
-- this because it is a unit quantity, not a currency amount.
ALTER TABLE adjustments ADD COLUMN amount_delta NUMERIC(18,2);
