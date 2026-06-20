CREATE TABLE IF NOT EXISTS quarantine_records (
    id                BIGSERIAL PRIMARY KEY,
    warehouse_id      BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id        BIGINT        NOT NULL REFERENCES products(id),
    batch_id          BIGINT        NOT NULL REFERENCES batches(id),
    location_id       BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    delivery_order_id BIGINT        NOT NULL REFERENCES delivery_orders(id),
    do_item_id        BIGINT        NOT NULL REFERENCES delivery_order_items(id),
    allocation_id     BIGINT        NOT NULL REFERENCES delivery_order_item_allocations(id),
    outbound_qc_record_id BIGINT,
    quantity          DECIMAL(10,2) NOT NULL,
    reason            TEXT          NOT NULL,
    created_by        BIGINT        NOT NULL REFERENCES users(id),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_quarantine_records_do
    ON quarantine_records(delivery_order_id);

CREATE INDEX IF NOT EXISTS idx_quarantine_records_allocation
    ON quarantine_records(allocation_id);

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

CREATE UNIQUE INDEX IF NOT EXISTS uq_outbound_qc_records_allocation
    ON outbound_qc_records(allocation_id);

CREATE INDEX IF NOT EXISTS idx_outbound_qc_records_do_key
    ON outbound_qc_records(do_id, idempotency_key);

ALTER TABLE delivery_orders
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

ALTER TABLE adjustments
    ADD COLUMN IF NOT EXISTS delivery_order_id BIGINT REFERENCES delivery_orders(id),
    ADD COLUMN IF NOT EXISTS do_item_id BIGINT REFERENCES delivery_order_items(id),
    ADD COLUMN IF NOT EXISTS allocation_id BIGINT REFERENCES delivery_order_item_allocations(id),
    ADD COLUMN IF NOT EXISTS outbound_qc_record_id BIGINT,
    ADD COLUMN IF NOT EXISTS quarantine_record_id BIGINT REFERENCES quarantine_records(id);

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

ALTER TABLE audit_logs
    ADD CONSTRAINT chk_audit_logs_action
        CHECK (action IN (
            'LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'STATUS_CHANGE',
            'APPROVE', 'REJECT', 'CANCEL', 'SOFT_DELETE', 'ASSIGN',
            'UNASSIGN', 'UPLOAD_POD', 'REQUEST_OTP', 'CONFIRM_DELIVERY',
            'RESET_DELIVERY_OTP', 'FAIL_DELIVERY', 'COMPLETE_TRIP',
            'PICKING_PLAN_SAVE', 'PICKED_GOODS_RETURN_TO_BIN',
            'PICKING_REPLACEMENT_SAVE', 'DELIVERY_ORDER_PICK_COMPLETE',
            'OUTBOUND_QC_FAIL_QUARANTINE', 'DELIVERY_ORDER_QC_APPROVE',
            'DELIVERY_ORDER_WAREHOUSE_APPROVE', 'DELIVERY_ORDER_WAREHOUSE_REJECT'
        ));
