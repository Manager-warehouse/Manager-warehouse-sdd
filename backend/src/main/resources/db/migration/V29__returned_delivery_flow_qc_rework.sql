ALTER TABLE returned_delivery_flows DROP CONSTRAINT IF EXISTS returned_delivery_flows_status_check;

ALTER TABLE returned_delivery_flows
    ADD COLUMN IF NOT EXISTS received_confirmed_by_storekeeper_id BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS rejected_by_storekeeper_id BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

ALTER TABLE returned_delivery_flow_items
    ADD COLUMN IF NOT EXISTS actual_qty NUMERIC(10, 2) CHECK (actual_qty >= 0),
    ADD COLUMN IF NOT EXISTS quality_pass_qty NUMERIC(10, 2) CHECK (quality_pass_qty >= 0),
    ADD COLUMN IF NOT EXISTS quality_fail_qty NUMERIC(10, 2) CHECK (quality_fail_qty >= 0),
    ADD COLUMN IF NOT EXISTS quality_failure_reason TEXT;

UPDATE returned_delivery_flow_items
SET actual_qty = counted_qty,
    quality_pass_qty = CASE WHEN quality_result = 'PASSED' THEN counted_qty ELSE 0 END,
    quality_fail_qty = CASE WHEN quality_result = 'FAILED' THEN counted_qty ELSE 0 END,
    quality_failure_reason = quality_reason
WHERE actual_qty IS NULL
  AND counted_qty IS NOT NULL;

UPDATE returned_delivery_flows
SET status = 'QC_APPROVED'
WHERE status = 'APPROVED';

ALTER TABLE returned_delivery_flows
    ADD CONSTRAINT returned_delivery_flows_status_check CHECK (status IN (
        'COUNT_QC_PENDING',
        'COUNT_QC_SUBMITTED',
        'QC_REJECTED',
        'QC_APPROVED',
        'PUTAWAY_PLANNED',
        'PUTAWAY_COMPLETED'
    ));
