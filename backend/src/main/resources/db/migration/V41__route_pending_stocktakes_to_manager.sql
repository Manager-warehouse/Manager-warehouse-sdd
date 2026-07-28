-- Spec 006 requires every pending stocktake to be reviewed by the assigned warehouse manager.
UPDATE stock_takes
SET approval_level = 'MANAGER',
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'PENDING_APPROVAL'
  AND approval_level IS DISTINCT FROM 'MANAGER';
