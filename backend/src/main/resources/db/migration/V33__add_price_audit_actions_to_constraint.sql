-- Consolidate audit_logs_action_check to include PRICE_* actions (spec 007).
-- Previous attempts at V24/V25 were pre-empted by other branches' migrations at the
-- same version numbers on the shared Supabase DB. V33 runs fresh above the current
-- DB schema version (32) and is guaranteed to execute.
-- Includes all actions from V21, V24 (009-returns-scrap-disposal), and spec 007.
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;

ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_action_check
    CHECK (action IN (
        'LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'STATUS_CHANGE', 'APPROVE', 'REJECT', 'CANCEL', 'SOFT_DELETE', 'ASSIGN', 'UNASSIGN',
        'RECEIPT_APPROVE', 'RECEIPT_REJECT', 'RECEIPT_RETURN_CONFIRM', 'RECEIPT_PUTAWAY_COMPLETE',
        'QUARANTINE_RTV_CREATE', 'QUARANTINE_RTV_CONFIRM', 'INVENTORY_UPDATE',
        'TRANSFER_CREATE', 'TRANSFER_UPDATE', 'TRANSFER_APPROVE', 'TRANSFER_REJECT', 'TRANSFER_TRIP_ASSIGN', 'TRANSFER_SHIP', 'TRANSFER_DEPART', 'TRANSFER_RECEIVE_COUNT', 'TRANSFER_RECEIVE_CONFIRM', 'TRANSFER_DISCREPANCY_CREATE', 'TRANSFER_CANCEL',
        'BILLING_NOTIFICATION_CREATE', 'BILLING_NOTIFICATION_READ',
        'RECEIPT_QC_SUBMIT', 'RECEIPT_QC_CONFIRM',
        'QUARANTINE_DISPOSAL_CREATE', 'QUARANTINE_DISPOSAL_APPROVE', 'CREDIT_NOTE_CREATE',
        'TRANSFER_FINAL_RECEIVE', 'TRANSFER_UNSHIP', 'TRANSFER_RECEIVE_CHECK',
        'PRICE_CREATE', 'PRICE_UPDATE', 'PRICE_CANCEL', 'PRICE_APPROVE', 'PRICE_IMPORT'
    )) NOT VALID;

-- Ensure unit_price snapshot column exists on delivery_order_items (idempotent).
-- Was intended in V24 but may not have executed on this DB instance.
ALTER TABLE delivery_order_items
    ADD COLUMN IF NOT EXISTS unit_price DECIMAL(18,2);
