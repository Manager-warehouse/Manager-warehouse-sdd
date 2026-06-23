-- Align receipt lifecycle with inbound receipt approval and quarantine handling specs.

UPDATE receipts
SET status = 'RETURN_TO_SUPPLIER_PENDING'
WHERE status = 'REJECTED';

ALTER TABLE receipts
    DROP CONSTRAINT IF EXISTS receipts_status_check;

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
