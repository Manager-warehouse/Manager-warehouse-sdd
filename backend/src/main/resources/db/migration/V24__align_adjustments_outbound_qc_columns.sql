ALTER TABLE delivery_order_items
    ADD COLUMN IF NOT EXISTS zone_id BIGINT REFERENCES warehouse_locations(id),
    ADD COLUMN IF NOT EXISTS planned_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS picked_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS qc_pass_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS qc_fail_qty DECIMAL(10,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS delivery_order_item_allocations (
    id                     BIGSERIAL PRIMARY KEY,
    do_item_id             BIGINT        NOT NULL REFERENCES delivery_order_items(id),
    inventory_id           BIGINT        NOT NULL REFERENCES inventories(id),
    batch_id               BIGINT        NOT NULL REFERENCES batches(id),
    location_id            BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    zone_id                BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    planned_qty            DECIMAL(10,2) NOT NULL,
    picked_qty             DECIMAL(10,2) NOT NULL DEFAULT 0,
    is_replacement         BOOLEAN       NOT NULL DEFAULT FALSE,
    replaced_allocation_id BIGINT        REFERENCES delivery_order_item_allocations(id),
    created_by             BIGINT        NOT NULL REFERENCES users(id),
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (planned_qty > 0),
    CHECK (picked_qty >= 0)
);

CREATE INDEX IF NOT EXISTS idx_do_item_allocations_do_item
    ON delivery_order_item_allocations(do_item_id);

CREATE INDEX IF NOT EXISTS idx_do_item_allocations_inventory
    ON delivery_order_item_allocations(inventory_id);

CREATE TABLE IF NOT EXISTS delivery_order_item_return_to_bin_records (
    id                   BIGSERIAL PRIMARY KEY,
    do_item_id           BIGINT        NOT NULL REFERENCES delivery_order_items(id),
    allocation_id        BIGINT        NOT NULL REFERENCES delivery_order_item_allocations(id),
    product_id           BIGINT        NOT NULL REFERENCES products(id),
    batch_id             BIGINT        NOT NULL REFERENCES batches(id),
    original_location_id BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    original_zone_id     BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    source_location_id   BIGINT        REFERENCES warehouse_locations(id),
    returned_qty         DECIMAL(10,2) NOT NULL,
    reason               TEXT,
    created_by           BIGINT        NOT NULL REFERENCES users(id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (returned_qty > 0)
);

CREATE INDEX IF NOT EXISTS idx_return_to_bin_allocation
    ON delivery_order_item_return_to_bin_records(allocation_id);

CREATE TABLE IF NOT EXISTS delivery_order_item_replacements (
    id                       BIGSERIAL PRIMARY KEY,
    do_item_id               BIGINT        NOT NULL REFERENCES delivery_order_items(id),
    failed_inventory_id      BIGINT        NOT NULL REFERENCES inventories(id),
    replacement_inventory_id BIGINT        NOT NULL REFERENCES inventories(id),
    failed_batch_id          BIGINT        NOT NULL REFERENCES batches(id),
    failed_location_id       BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    replacement_batch_id     BIGINT        NOT NULL REFERENCES batches(id),
    replacement_location_id  BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    quantity                 DECIMAL(10,2) NOT NULL,
    reason                   TEXT          NOT NULL,
    created_by               BIGINT        NOT NULL REFERENCES users(id),
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_do_item_replacements_do_item
    ON delivery_order_item_replacements(do_item_id);

CREATE TABLE IF NOT EXISTS quarantine_records (
    id                     BIGSERIAL PRIMARY KEY,
    warehouse_id           BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id             BIGINT        NOT NULL REFERENCES products(id),
    batch_id               BIGINT        NOT NULL REFERENCES batches(id),
    location_id            BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    delivery_order_id      BIGINT        NOT NULL REFERENCES delivery_orders(id),
    do_item_id             BIGINT        NOT NULL REFERENCES delivery_order_items(id),
    allocation_id          BIGINT        NOT NULL REFERENCES delivery_order_item_allocations(id),
    outbound_qc_record_id  BIGINT,
    quantity               DECIMAL(10,2) NOT NULL,
    reason                 TEXT          NOT NULL,
    created_by             BIGINT        NOT NULL REFERENCES users(id),
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
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

ALTER TABLE adjustments
    ADD COLUMN IF NOT EXISTS delivery_order_id BIGINT REFERENCES delivery_orders(id),
    ADD COLUMN IF NOT EXISTS do_item_id BIGINT REFERENCES delivery_order_items(id),
    ADD COLUMN IF NOT EXISTS allocation_id BIGINT REFERENCES delivery_order_item_allocations(id),
    ADD COLUMN IF NOT EXISTS outbound_qc_record_id BIGINT REFERENCES outbound_qc_records(id),
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
          AND rel.relname = 'adjustments'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%type%'
    LOOP
        EXECUTE format('ALTER TABLE adjustments DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE adjustments
    ADD CONSTRAINT chk_adjustments_type
        CHECK (type IN (
            'STOCK_TAKE',
            'TRANSFER_DISCREPANCY',
            'DISPOSAL',
            'RETURN_TO_VENDOR',
            'CORRECTION_VOUCHER',
            'QC_FAIL_OUTBOUND'
        ));
