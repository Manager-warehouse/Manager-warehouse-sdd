UPDATE receipts r
SET status = 'PENDING_MANAGER_APPROVAL',
    updated_at = CURRENT_TIMESTAMP
WHERE r.type = 'PURCHASE'
  AND r.status = 'PENDING_RECEIPT'
  AND r.pre_receive_approved_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM receipt_items ri
      WHERE ri.receipt_id = r.id
        AND ri.actual_qty IS NOT NULL
  );
