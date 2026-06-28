-- =============================================================================
-- 0. RENAME BẢNG TRANSFERS VÀ TRANSFER_ITEMS NẾU CHƯA ĐÚNG CONVENTION
-- =============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = current_schema() AND tablename = 'transfers')
       AND NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = current_schema() AND tablename = 'inter_warehouse_transfers') THEN
        ALTER TABLE transfers RENAME TO inter_warehouse_transfers;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = current_schema() AND tablename = 'transfer_items')
       AND NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = current_schema() AND tablename = 'inter_warehouse_transfer_items') THEN
        ALTER TABLE transfer_items RENAME TO inter_warehouse_transfer_items;
    END IF;
END $$;

-- =============================================================================
-- 1. TẠO HOẶC CẬP NHẬT BẢNG QUARANTINE_RECORDS VÀ OUTBOUND_QC_RECORDS (NẾU CHƯA CÓ)
-- =============================================================================

CREATE TABLE IF NOT EXISTS quarantine_records (
    id                BIGSERIAL PRIMARY KEY,
    warehouse_id      BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id        BIGINT        NOT NULL REFERENCES products(id),
    batch_id          BIGINT        NOT NULL REFERENCES batches(id),
    location_id       BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    delivery_order_id BIGINT        REFERENCES delivery_orders(id), -- Nullable cho transfer/return
    do_item_id        BIGINT        REFERENCES delivery_order_items(id), -- Nullable cho transfer/return
    allocation_id     BIGINT        REFERENCES delivery_order_item_allocations(id), -- Nullable cho transfer/return
    outbound_qc_record_id BIGINT,
    quantity          DECIMAL(10,2) NOT NULL,
    reason            TEXT          NOT NULL,
    created_by        BIGINT        NOT NULL REFERENCES users(id),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (quantity > 0)
);

-- Đảm bảo các cột DO trong quarantine_records là nullable nếu bảng đã tồn tại từ trước
ALTER TABLE quarantine_records ALTER COLUMN delivery_order_id DROP NOT NULL;
ALTER TABLE quarantine_records ALTER COLUMN do_item_id DROP NOT NULL;
ALTER TABLE quarantine_records ALTER COLUMN allocation_id DROP NOT NULL;

-- Thêm các cột cho điều chuyển nội bộ vào quarantine_records
ALTER TABLE quarantine_records ADD COLUMN IF NOT EXISTS transfer_id BIGINT REFERENCES inter_warehouse_transfers(id);
ALTER TABLE quarantine_records ADD COLUMN IF NOT EXISTS transfer_item_id BIGINT REFERENCES inter_warehouse_transfer_items(id);
ALTER TABLE quarantine_records ADD COLUMN IF NOT EXISTS origin_type VARCHAR(50) NOT NULL DEFAULT 'OUTBOUND_QC';
ALTER TABLE quarantine_records ADD COLUMN IF NOT EXISTS remaining_quantity DECIMAL(10,2);

-- Khởi tạo remaining_quantity bằng quantity cho các bản ghi cũ
UPDATE quarantine_records SET remaining_quantity = quantity WHERE remaining_quantity IS NULL;
ALTER TABLE quarantine_records ALTER COLUMN remaining_quantity SET NOT NULL;

-- Tạo bảng outbound_qc_records nếu chưa có (để Hibernate validate pass)
CREATE TABLE IF NOT EXISTS outbound_qc_records (
    id                     BIGSERIAL PRIMARY KEY,
    do_id                  BIGINT        NOT NULL REFERENCES delivery_orders(id),
    do_item_id             BIGINT        NOT NULL REFERENCES delivery_order_items(id),
    allocation_id          BIGINT        NOT NULL REFERENCES delivery_order_item_allocations(id),
    batch_id               BIGINT        NOT NULL REFERENCES batches(id),
    location_id            BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    zone_id                BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    staging_location_id    BIGINT        REFERENCES warehouse_locations(id),
    quarantine_location_id BIGINT        REFERENCES warehouse_locations(id),
    quarantine_record_id   BIGINT        REFERENCES quarantine_records(id),
    picked_qty             DECIMAL(10,2) NOT NULL,
    qc_pass_qty            DECIMAL(10,2) NOT NULL,
    qc_fail_qty            DECIMAL(10,2) NOT NULL,
    qc_fail_reason         TEXT,
    idempotency_key        VARCHAR(100),
    request_hash           VARCHAR(128),
    notes                  TEXT,
    created_by             BIGINT        NOT NULL REFERENCES users(id),
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (picked_qty >= 0),
    CHECK (qc_pass_qty >= 0),
    CHECK (qc_fail_qty >= 0)
);

-- =============================================================================
-- 2. CẬP NHẬT BẢNG INTER_WAREHOUSE_TRANSFERS CHO WRONG-SKU RETURN
-- =============================================================================

ALTER TABLE inter_warehouse_transfers ADD COLUMN IF NOT EXISTS return_requested BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE inter_warehouse_transfers ADD COLUMN IF NOT EXISTS return_reason TEXT;
ALTER TABLE inter_warehouse_transfers ADD COLUMN IF NOT EXISTS return_requested_by BIGINT REFERENCES users(id);
ALTER TABLE inter_warehouse_transfers ADD COLUMN IF NOT EXISTS return_requested_at TIMESTAMPTZ;
ALTER TABLE inter_warehouse_transfers ADD COLUMN IF NOT EXISTS return_approved_by BIGINT REFERENCES users(id);
ALTER TABLE inter_warehouse_transfers ADD COLUMN IF NOT EXISTS return_approved_at TIMESTAMPTZ;
ALTER TABLE inter_warehouse_transfers ADD COLUMN IF NOT EXISTS return_rejected_by BIGINT REFERENCES users(id);
ALTER TABLE inter_warehouse_transfers ADD COLUMN IF NOT EXISTS return_rejected_at TIMESTAMPTZ;
ALTER TABLE inter_warehouse_transfers ADD COLUMN IF NOT EXISTS return_rejection_reason TEXT;
ALTER TABLE inter_warehouse_transfers ADD COLUMN IF NOT EXISTS is_returned BOOLEAN DEFAULT FALSE NOT NULL;

-- =============================================================================
-- 3. TẠO MỚI CÁC BẢNG YÊU CẦU ĐIỀU CHUYỂN (TRANSFER_REQUESTS)
-- =============================================================================

CREATE TABLE IF NOT EXISTS transfer_requests (
    id                       BIGSERIAL PRIMARY KEY,
    request_number          VARCHAR(50) UNIQUE NOT NULL,
    source_warehouse_id      BIGINT NOT NULL REFERENCES warehouses(id),
    destination_warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    status                   VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
                             CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CONVERTED')),
    created_by               BIGINT NOT NULL REFERENCES users(id),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_by             BIGINT REFERENCES users(id),
    submitted_at             TIMESTAMPTZ,
    approved_by              BIGINT REFERENCES users(id),
    approved_at              TIMESTAMPTZ,
    rejected_by              BIGINT REFERENCES users(id),
    rejected_at              TIMESTAMPTZ,
    rejection_reason         TEXT,
    notes                    TEXT
);

CREATE TABLE IF NOT EXISTS transfer_request_items (
    id                  BIGSERIAL PRIMARY KEY,
    transfer_request_id BIGINT NOT NULL REFERENCES transfer_requests(id) ON DELETE CASCADE,
    product_id          BIGINT NOT NULL REFERENCES products(id),
    requested_qty       DECIMAL(10,2) NOT NULL CHECK (requested_qty > 0)
);

ALTER TABLE inter_warehouse_transfers ADD COLUMN IF NOT EXISTS transfer_request_id BIGINT REFERENCES transfer_requests(id);

-- =============================================================================
-- 4. LIÊN KẾT ADJUSTMENTS VỚI QUARANTINE_RECORDS
-- =============================================================================

ALTER TABLE adjustments ADD COLUMN IF NOT EXISTS quarantine_record_id BIGINT REFERENCES quarantine_records(id);

-- =============================================================================
-- 5. CẬP NHẬT AUDIT LOG ACTIONS
-- =============================================================================

DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = current_schema()
          AND rel.relname = 'audit_logs'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%action%'
    LOOP
        EXECUTE format('ALTER TABLE audit_logs DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE audit_logs ADD CONSTRAINT chk_audit_logs_action CHECK (action IN (
    'LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'STATUS_CHANGE', 'APPROVE', 'REJECT', 'CANCEL', 'SOFT_DELETE', 'ASSIGN', 'UNASSIGN',
    'UPLOAD_POD', 'REQUEST_OTP', 'CONFIRM_DELIVERY', 'RESET_DELIVERY_OTP', 'FAIL_DELIVERY', 'TRIP_CREATE', 'TRIP_UPDATE',
    'TRIP_CANCEL', 'TRIP_DEPART', 'DELIVERY_ATTEMPT_CREATE', 'COMPLETE_TRIP', 'PICKING_PLAN_SAVE', 'PICKED_GOODS_RETURN_TO_BIN',
    'PICKING_REPLACEMENT_SAVE', 'DELIVERY_ORDER_PICK_COMPLETE', 'OUTBOUND_QC_FAIL_QUARANTINE', 'DELIVERY_ORDER_QC_APPROVE',
    'DELIVERY_ORDER_WAREHOUSE_APPROVE', 'DELIVERY_ORDER_WAREHOUSE_REJECT', 'INVOICE_AUTO_CREATE', 'RECEIPT_RETURN_CONFIRM',
    'RECEIPT_REJECT', 'RECEIPT_APPROVE', 'QUARANTINE_RTV_CREATE', 'QUARANTINE_RTV_CONFIRM', 'RECEIPT_QC_CONFIRM',
    'RECEIPT_QC_SUBMIT', 'RECEIPT_PUTAWAY_COMPLETE', 'INVENTORY_UPDATE', 'TRANSFER_APPROVE', 'TRANSFER_DEPART',
    'TRANSFER_UNSHIP', 'TRANSFER_SHIP', 'TRANSFER_TRIP_ASSIGN', 'TRANSFER_DISCREPANCY_CREATE', 'TRANSFER_RETURN_TO_SOURCE',
    'TRANSFER_QUARANTINE_REJECT', 'TRANSFER_FINAL_RECEIVE', 'TRANSFER_RECEIVE_CHECK', 'TRANSFER_RECEIVE_COUNT',
    'TRANSFER_CANCEL', 'TRANSFER_REJECT', 'STOCKTAKE_CANCEL', 'STOCKTAKE_REJECT', 'STOCKTAKE_APPROVE', 'STOCKTAKE_COMPLETE',
    'STOCKTAKE_AUTO_APPROVE', 'STOCKTAKE_COUNT_UPDATE', 'STOCKTAKE_START', 'STOCKTAKE_CREATE', 'PRICE_IMPORT',
    'PRICE_APPROVE', 'PRICE_CANCEL', 'PRICE_UPDATE', 'PRICE_CREATE', 'RECEIPT_CREATE', 'CREDIT_NOTE_CREATE',
    'QUARANTINE_DISPOSAL_CREATE', 'QUARANTINE_DISPOSAL_APPROVE',
    'TRANSFER_REQUEST_CREATE', 'TRANSFER_REQUEST_UPDATE', 'TRANSFER_REQUEST_SUBMIT',
    'TRANSFER_REQUEST_CEO_APPROVE', 'TRANSFER_REQUEST_CEO_REJECT', 'TRANSFER_REQUEST_CONVERT'
));
