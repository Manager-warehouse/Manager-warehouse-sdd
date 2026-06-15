-- Fix 1: inventories.batch_id phải nullable để quarantine inventory
-- có thể được tạo khi QC_FAILED (trước khi batch được tạo ở bước APPROVED)
ALTER TABLE inventories
    ALTER COLUMN batch_id DROP NOT NULL;

-- Fix 1b: Unique constraint gốc không hoạt động đúng khi batch_id NULL
-- (PostgreSQL treats NULLs as distinct trong UNIQUE, không thể deduplicate quarantine rows)
-- Thay bằng 2 partial indexes: 1 cho có batch, 1 cho không có batch (quarantine)
ALTER TABLE inventories
    DROP CONSTRAINT IF EXISTS inventories_warehouse_id_product_id_batch_id_location_id_key;

CREATE UNIQUE INDEX uk_inventory_with_batch
    ON inventories (warehouse_id, product_id, batch_id, location_id)
    WHERE batch_id IS NOT NULL;

CREATE UNIQUE INDEX uk_inventory_quarantine
    ON inventories (warehouse_id, product_id, location_id)
    WHERE batch_id IS NULL;

-- Fix 2: receipts.source_channel enforce chỉ nhận ZALO hoặc EMAIL
ALTER TABLE receipts
    ADD CONSTRAINT receipts_source_channel_check
    CHECK (source_channel IS NULL OR source_channel IN ('ZALO', 'EMAIL'));

-- Fix 3: audit_logs.action CHECK constraint cần bổ sung các action của spec 003
ALTER TABLE audit_logs
    DROP CONSTRAINT IF EXISTS audit_logs_action_check;

ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_action_check
    CHECK (action IN (
        'LOGIN', 'LOGOUT',
        'CREATE', 'UPDATE', 'STATUS_CHANGE', 'APPROVE', 'REJECT',
        'CANCEL', 'SOFT_DELETE', 'ASSIGN', 'UNASSIGN',
        'RECEIPT_CREATE', 'RECEIPT_RECEIVE',
        'RECEIPT_QC_SUBMIT', 'RECEIPT_QC_CONFIRM',
        'RECEIPT_APPROVE', 'RECEIPT_REJECT', 'RECEIPT_PUTAWAY_COMPLETE',
        'QUARANTINE_RTV_CREATE', 'QUARANTINE_RTV_CONFIRM',
        'INVENTORY_UPDATE'
    ));
