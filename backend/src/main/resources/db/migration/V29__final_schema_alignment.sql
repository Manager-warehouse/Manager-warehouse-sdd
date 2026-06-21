-- =============================================================================
-- V29: Final schema alignment — remaining missing columns
-- =============================================================================
-- Context: Sau V25-V28, vẫn còn các cột chưa được add vào DB từ các migrations
-- trước do DB restore từ snapshot cũ.
-- Tất cả dùng IF NOT EXISTS / nullable defaults để idempotent.
-- =============================================================================

-- ─── delivery_orders: rejection_reason, cancel_reason, packed_by, qc_by ──────
ALTER TABLE delivery_orders
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS cancel_reason    TEXT,
    ADD COLUMN IF NOT EXISTS packed_by        BIGINT REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS qc_by            BIGINT REFERENCES users(id) ON DELETE SET NULL;

-- ─── receipts: rejection_reason ───────────────────────────────────────────────
ALTER TABLE receipts
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- ─── receipt_items: over_received_qty ─────────────────────────────────────────
ALTER TABLE receipt_items
    ADD COLUMN IF NOT EXISTS over_received_qty DECIMAL(10,2);

-- ─── vehicles: max_volume_m3, warehouse_id ────────────────────────────────────
ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS max_volume_m3 DECIMAL(10,3),
    ADD COLUMN IF NOT EXISTS warehouse_id  BIGINT REFERENCES warehouses(id);

-- ─── drivers: warehouse_id ────────────────────────────────────────────────────
ALTER TABLE drivers
    ADD COLUMN IF NOT EXISTS warehouse_id BIGINT REFERENCES warehouses(id);

-- ─── trips: warehouse_id, cancel_reason, departed_at, completed_at, notes ────
ALTER TABLE trips
    ADD COLUMN IF NOT EXISTS warehouse_id  BIGINT REFERENCES warehouses(id),
    ADD COLUMN IF NOT EXISTS cancel_reason TEXT,
    ADD COLUMN IF NOT EXISTS departed_at   TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS completed_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS notes         TEXT;

-- ─── deliveries: attempt_number, dispatched_at, otp_verified_at ──────────────
ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS attempt_number INTEGER,
    ADD COLUMN IF NOT EXISTS dispatched_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS otp_verified_at TIMESTAMPTZ;

-- Backfill attempt_number
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

CREATE UNIQUE INDEX IF NOT EXISTS ux_deliveries_do_attempt
    ON deliveries(do_id, attempt_number);

-- ─── delivery_otp_attempts: status ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS delivery_otp_attempts (
    id              BIGSERIAL    PRIMARY KEY,
    delivery_id     BIGINT       NOT NULL REFERENCES deliveries(id),
    otp_hash        VARCHAR(255) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    consumed_at     TIMESTAMPTZ,
    attempt_count   INTEGER      NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

ALTER TABLE delivery_otp_attempts
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS ux_delivery_otp_attempts_delivery
    ON delivery_otp_attempts(delivery_id);

CREATE INDEX IF NOT EXISTS idx_delivery_otp_attempts_active
    ON delivery_otp_attempts(delivery_id, expires_at)
    WHERE consumed_at IS NULL;

-- ─── warehouse_product_reservations ──────────────────────────────────────────
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

-- ─── document_sequences ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS document_sequences (
    sequence_key VARCHAR(50) PRIMARY KEY,
    next_value   BIGINT      NOT NULL DEFAULT 1,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── invoice_lines (idempotent) ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS invoice_lines (
    id          BIGSERIAL     PRIMARY KEY,
    invoice_id  BIGINT        NOT NULL REFERENCES invoices(id),
    do_item_id  BIGINT        NOT NULL REFERENCES delivery_order_items(id),
    product_id  BIGINT        NOT NULL REFERENCES products(id),
    quantity    DECIMAL(10,2) NOT NULL CHECK (quantity > 0),
    unit_price  DECIMAL(18,2) NOT NULL CHECK (unit_price >= 0),
    line_amount DECIMAL(18,2) NOT NULL CHECK (line_amount >= 0),
    CONSTRAINT ux_invoice_lines_invoice_do_item UNIQUE (invoice_id, do_item_id)
);

CREATE INDEX IF NOT EXISTS idx_invoice_lines_invoice_id ON invoice_lines(invoice_id);
