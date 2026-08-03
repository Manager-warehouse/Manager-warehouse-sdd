-- Fix receipts that have a supplier_billing_notification (proof putaway was done)
-- but whose status was not correctly set to PUTAWAY_COMPLETED.
-- Safe guard: only update if inventory records exist for the receipt's products
-- (confirming goods are physically in the warehouse).
UPDATE receipts r
SET status             = 'PUTAWAY_COMPLETED',
    putaway_completed_at = COALESCE(r.putaway_completed_at, r.updated_at),
    updated_at         = NOW()
WHERE r.status IN ('APPROVED', 'PARTIALLY_APPROVED')
  AND EXISTS (
      SELECT 1
      FROM supplier_billing_notifications sbn
      WHERE sbn.receipt_id = r.id
        AND sbn.status        = 'ACTIVE'
        AND sbn.invoice_status = 'NOT_INVOICED'
  )
  AND EXISTS (
      SELECT 1
      FROM receipt_items ri
               JOIN inventories i
                    ON i.product_id    = ri.product_id
                    AND i.warehouse_id = r.warehouse_id
                    AND i.total_qty    > 0
      WHERE ri.receipt_id = r.id
        AND COALESCE(ri.approved_qty, 0) > 0
  );
