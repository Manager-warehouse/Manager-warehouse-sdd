-- Fix receipts status check constraint to include RETURN_TO_SUPPLIER_PENDING and RETURNED_TO_SUPPLIER.
-- This constraint was incorrectly overwritten by V3__add_receipt_qc_sampling.sql running out-of-order.

ALTER TABLE receipts DROP CONSTRAINT IF EXISTS receipts_status_check;

ALTER TABLE receipts
    ADD CONSTRAINT receipts_status_check
    CHECK (status IN (
        'PENDING_RECEIPT',
        'DRAFT',
        'QC_COMPLETED',
        'QC_FAILED',
        'APPROVED',
        'RETURN_TO_SUPPLIER_PENDING',
        'RETURNED_TO_SUPPLIER'
    ));
