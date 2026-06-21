-- =============================================================================
-- V28: Re-apply all incremental schema changes from V6–V19
-- =============================================================================
-- Context: DB không có nhiều cột/bảng được thêm từ V6-V19 do baseline lệch.
-- Tất cả statements dùng IF NOT EXISTS / ADD COLUMN IF NOT EXISTS để idempotent.
-- =============================================================================

-- ─── V6: deliveries columns & delivery_otp_attempts ──────────────────────────
ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS otp_verified_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS delivery_otp_attempts (
    id              BIGSERIAL    PRIMARY KEY,
    delivery_id     BIGINT       NOT NULL REFERENCES deliveries(id),
    otp_hash        VARCHAR(255) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    consumed_at     TIMESTAMPTZ,
    attempt_count   INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_delivery_otp_attempts_active
    ON delivery_otp_attempts(delivery_id, expires_at)
    WHERE consumed_at IS NULL;

-- ─── V7: deliveries.attempt_number, dispatched_at ────────────────────────────
ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS attempt_number INTEGER,
    ADD COLUMN IF NOT EXISTS dispatched_at  TIMESTAMPTZ;

-- Gán attempt_number cho rows hiện có (nếu có)
WITH numbered_attempts AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY do_id ORDER BY created_at, id) AS rn
    FROM deliveries
    WHERE attempt_number IS NULL
)
UPDATE deliveries d
SET attempt_number = numbered_attempts.rn
FROM numbered_attempts
WHERE d.id = numbered_attempts.id;

-- Set NOT NULL chỉ khi tất cả rows đã có giá trị
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'deliveries'
          AND column_name = 'attempt_number'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE deliveries ALTER COLUMN attempt_number SET NOT NULL;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_deliveries_do_attempt
    ON deliveries(do_id, attempt_number);

-- ─── V9: receipt_items.over_received_qty ─────────────────────────────────────
ALTER TABLE receipt_items
    ADD COLUMN IF NOT EXISTS over_received_qty DECIMAL(10,2);

-- ─── V14: warehouse_product_reservations ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS warehouse_product_reservations (
    id           BIGSERIAL     PRIMARY KEY,
    warehouse_id BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id   BIGINT        NOT NULL REFERENCES products(id),
    reserved_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    version      INTEGER       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (warehouse_id, product_id),
    CHECK (reserved_qty >= 0)
);

CREATE INDEX IF NOT EXISTS idx_wpr_warehouse_product
    ON warehouse_product_reservations(warehouse_id, product_id);

-- ─── V15: vehicles.warehouse_id, drivers.warehouse_id, trips extra cols ───────
ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS warehouse_id BIGINT REFERENCES warehouses(id);

ALTER TABLE drivers
    ADD COLUMN IF NOT EXISTS warehouse_id BIGINT REFERENCES warehouses(id);

-- Default warehouse_id cho các rows hiện có (nếu có)
DO $$
DECLARE
    first_physical_wh BIGINT;
BEGIN
    SELECT id INTO first_physical_wh FROM warehouses WHERE type = 'PHYSICAL' ORDER BY id LIMIT 1;
    IF first_physical_wh IS NOT NULL THEN
        UPDATE vehicles SET warehouse_id = first_physical_wh WHERE warehouse_id IS NULL;
        UPDATE drivers  SET warehouse_id = first_physical_wh WHERE warehouse_id IS NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_vehicles_warehouse_status
    ON vehicles(warehouse_id, status, is_active);

CREATE INDEX IF NOT EXISTS idx_drivers_warehouse_status
    ON drivers(warehouse_id, status, is_active);

ALTER TABLE trips
    ADD COLUMN IF NOT EXISTS warehouse_id  BIGINT REFERENCES warehouses(id),
    ADD COLUMN IF NOT EXISTS cancel_reason TEXT,
    ADD COLUMN IF NOT EXISTS departed_at   TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS completed_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS notes         TEXT;

-- Default trips.warehouse_id
DO $$
DECLARE
    first_physical_wh BIGINT;
BEGIN
    SELECT id INTO first_physical_wh FROM warehouses WHERE type = 'PHYSICAL' ORDER BY id LIMIT 1;
    IF first_physical_wh IS NOT NULL THEN
        UPDATE trips SET warehouse_id = first_physical_wh WHERE warehouse_id IS NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_trip_delivery_orders_do_id
    ON trip_delivery_orders(do_id);

CREATE INDEX IF NOT EXISTS idx_trips_warehouse_status
    ON trips(warehouse_id, status);

-- ─── V16: delivery_otp_attempts.status ───────────────────────────────────────
ALTER TABLE delivery_otp_attempts
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS ux_delivery_otp_attempts_delivery
    ON delivery_otp_attempts(delivery_id);

-- ─── V18 & V19 (duplicated in V24/V27 — idempotent, safe to re-run) ──────────
-- delivery_order_items extra columns
ALTER TABLE delivery_order_items
    ADD COLUMN IF NOT EXISTS zone_id     BIGINT        REFERENCES warehouse_locations(id),
    ADD COLUMN IF NOT EXISTS planned_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS picked_qty  DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS qc_pass_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS qc_fail_qty DECIMAL(10,2) NOT NULL DEFAULT 0;

-- New operational tables (all IF NOT EXISTS — safe even if already created by V27)
CREATE TABLE IF NOT EXISTS delivery_order_item_allocations (
    id                     BIGSERIAL     PRIMARY KEY,
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
    id                   BIGSERIAL     PRIMARY KEY,
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
    id                       BIGSERIAL     PRIMARY KEY,
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
    id                    BIGSERIAL     PRIMARY KEY,
    warehouse_id          BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id            BIGINT        NOT NULL REFERENCES products(id),
    batch_id              BIGINT        NOT NULL REFERENCES batches(id),
    location_id           BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    delivery_order_id     BIGINT        NOT NULL REFERENCES delivery_orders(id),
    do_item_id            BIGINT        NOT NULL REFERENCES delivery_order_items(id),
    allocation_id         BIGINT        NOT NULL REFERENCES delivery_order_item_allocations(id),
    outbound_qc_record_id BIGINT,
    quantity              DECIMAL(10,2) NOT NULL,
    reason                TEXT          NOT NULL,
    created_by            BIGINT        NOT NULL REFERENCES users(id),
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_quarantine_records_do
    ON quarantine_records(delivery_order_id);
CREATE INDEX IF NOT EXISTS idx_quarantine_records_allocation
    ON quarantine_records(allocation_id);

CREATE TABLE IF NOT EXISTS outbound_qc_records (
    id                     BIGSERIAL     PRIMARY KEY,
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

-- ─── adjustments extra columns (from V24/V27 — idempotent) ───────────────────
ALTER TABLE adjustments
    ADD COLUMN IF NOT EXISTS delivery_order_id     BIGINT REFERENCES delivery_orders(id),
    ADD COLUMN IF NOT EXISTS do_item_id            BIGINT REFERENCES delivery_order_items(id),
    ADD COLUMN IF NOT EXISTS allocation_id         BIGINT REFERENCES delivery_order_item_allocations(id),
    ADD COLUMN IF NOT EXISTS outbound_qc_record_id BIGINT REFERENCES outbound_qc_records(id),
    ADD COLUMN IF NOT EXISTS quarantine_record_id  BIGINT REFERENCES quarantine_records(id);

-- ─── document_sequences table ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS document_sequences (
    sequence_key VARCHAR(50) PRIMARY KEY,
    next_value   BIGINT      NOT NULL DEFAULT 1,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
