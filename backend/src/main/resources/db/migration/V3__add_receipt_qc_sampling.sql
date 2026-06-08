-- Add sample-based inbound QC fields and status.

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
        'REJECTED'
    ));

ALTER TABLE receipt_items
    ADD COLUMN sample_qty DECIMAL(10,2),
    ADD COLUMN sample_passed_qty DECIMAL(10,2),
    ADD COLUMN sample_failed_qty DECIMAL(10,2),
    ADD COLUMN qc_sampling_method VARCHAR(30);

ALTER TABLE receipt_items
    ADD CONSTRAINT receipt_items_qc_sampling_method_check
    CHECK (qc_sampling_method IS NULL OR qc_sampling_method IN ('FULL_INSPECTION','RANDOM_SAMPLE'));
