-- V40: Set sample_passed_qty = actual_qty for APPROVED receipts that skipped the QC sampling step
-- These demo receipts were approved directly without going through the QC_COMPLETED flow
UPDATE receipt_items ri
SET sample_passed_qty = ri.actual_qty,
    qc_result         = 'PASSED'
FROM receipts r
WHERE ri.receipt_id = r.id
  AND r.status = 'APPROVED'
  AND (ri.sample_passed_qty IS NULL OR ri.sample_passed_qty = 0)
  AND ri.actual_qty > 0;
