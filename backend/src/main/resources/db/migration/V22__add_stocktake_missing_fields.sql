-- V22: Add missing fields for Spec 006 StockTake & Adjustment
-- Adds approval_level, is_employee_fault, rejection_reason to stock_takes,
-- adds REJECTED status, adds location-locking columns to warehouse_locations,
-- seeds document sequence for stocktake numbering, and extends audit log actions.

-- 1. Add missing columns to stock_takes
ALTER TABLE stock_takes
    ADD COLUMN IF NOT EXISTS approval_level     VARCHAR(10)
        CHECK (approval_level IN ('AUTO', 'MANAGER', 'CEO')),
    ADD COLUMN IF NOT EXISTS is_employee_fault  BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS rejection_reason   TEXT;

-- 2. Drop and recreate status CHECK to include REJECTED
ALTER TABLE stock_takes DROP CONSTRAINT IF EXISTS stock_takes_status_check;
ALTER TABLE stock_takes
    ADD CONSTRAINT stock_takes_status_check
    CHECK (status IN ('DRAFT', 'IN_PROGRESS', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED'));

-- 3. Add location-locking columns to warehouse_locations
ALTER TABLE warehouse_locations
    ADD COLUMN IF NOT EXISTS is_locked               BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS locked_by_stock_take_id BIGINT
        REFERENCES stock_takes(id) ON DELETE SET NULL;

-- 4. Index for fast lookup of locked locations by stocktake
CREATE INDEX IF NOT EXISTS idx_wh_locations_locked_by
    ON warehouse_locations(locked_by_stock_take_id)
    WHERE locked_by_stock_take_id IS NOT NULL;

-- 4b. Allow stock_take_items.actual_qty to be NULL until the count is recorded.
-- The create→start→count flow inserts items (with system_qty) before any actual count
-- exists, so the original NOT NULL constraint would break stocktake creation.
ALTER TABLE stock_take_items ALTER COLUMN actual_qty DROP NOT NULL;
ALTER TABLE stock_take_items DROP CONSTRAINT IF EXISTS stock_take_items_actual_qty_check;
ALTER TABLE stock_take_items
    ADD CONSTRAINT stock_take_items_actual_qty_check
    CHECK (actual_qty IS NULL OR actual_qty >= 0);

-- 5. Seed document sequence for stocktake numbers (prefix: ST)
INSERT INTO document_sequences (sequence_key, next_value, updated_at)
VALUES ('ST', 1, NOW())
ON CONFLICT (sequence_key) DO NOTHING;

-- 6. Extend audit_logs action CHECK to include stocktake lifecycle actions
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;
ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_action_check
    CHECK (action IN (
        'LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'STATUS_CHANGE', 'APPROVE', 'REJECT',
        'CANCEL', 'SOFT_DELETE', 'ASSIGN', 'UNASSIGN',
        'RECEIPT_APPROVE', 'RECEIPT_REJECT', 'RECEIPT_RETURN_CONFIRM', 'RECEIPT_PUTAWAY_COMPLETE',
        'QUARANTINE_RTV_CREATE', 'QUARANTINE_RTV_CONFIRM',
        'INVENTORY_UPDATE',
        'TRANSFER_CREATE', 'TRANSFER_UPDATE', 'TRANSFER_APPROVE', 'TRANSFER_REJECT',
        'TRANSFER_TRIP_ASSIGN', 'TRANSFER_SHIP', 'TRANSFER_DEPART',
        'TRANSFER_RECEIVE_COUNT', 'TRANSFER_RECEIVE_CONFIRM',
        'TRANSFER_DISCREPANCY_CREATE', 'TRANSFER_CANCEL',
        'BILLING_NOTIFICATION_CREATE', 'BILLING_NOTIFICATION_READ',
        'RECEIPT_QC_SUBMIT', 'RECEIPT_QC_CONFIRM',
        'STOCKTAKE_CREATE', 'STOCKTAKE_START', 'STOCKTAKE_COUNT_UPDATE',
        'STOCKTAKE_COMPLETE', 'STOCKTAKE_AUTO_APPROVE', 'STOCKTAKE_APPROVE',
        'STOCKTAKE_REJECT', 'STOCKTAKE_CANCEL'
    ));
