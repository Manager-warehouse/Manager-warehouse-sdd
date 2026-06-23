-- =============================================================================
-- V27: Comprehensive schema alignment — re-apply all incremental column additions
-- =============================================================================
-- Context: DB có thể được restore từ backup cũ sau khi các migration V20-V24 đã
-- được ghi vào flyway_schema_history nhưng chưa apply được vào DB thực.
-- Migration này là idempotent (IF NOT EXISTS) nên an toàn để chạy lại.
--
-- Covers:
--   V20: dealers.email, delivery_otp_attempts constraint
--   V21: invoice_lines table (đảm bảo idempotent)
--   V23: audit_logs columns (description, warehouse_id, old_value, new_value, ip_address)
--   V24: delivery_order_items columns, các bảng outbound picking/QC
--   Các cột thêm vào bởi entity hiện tại mà chưa có migration tương ứng
-- =============================================================================

-- ─── 1. dealers.email (V20) ──────────────────────────────────────────────────
ALTER TABLE dealers
    ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- ─── 2. audit_logs columns (V23) ─────────────────────────────────────────────
ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS description TEXT;

UPDATE audit_logs SET description = '' WHERE description IS NULL;

ALTER TABLE audit_logs
    ALTER COLUMN description SET NOT NULL,
    ALTER COLUMN description SET DEFAULT '';

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS warehouse_id BIGINT REFERENCES warehouses(id),
    ADD COLUMN IF NOT EXISTS old_value JSONB,
    ADD COLUMN IF NOT EXISTS new_value JSONB,
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);

-- ─── 3. invoice_lines table (V21) ─────────────────────────────────────────────
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

-- ─── 4. delivery_order_items columns (V24) ───────────────────────────────────
ALTER TABLE delivery_order_items
    ADD COLUMN IF NOT EXISTS zone_id     BIGINT        REFERENCES warehouse_locations(id),
    ADD COLUMN IF NOT EXISTS planned_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS picked_qty  DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS qc_pass_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS qc_fail_qty DECIMAL(10,2) NOT NULL DEFAULT 0;

-- ─── 5. New tables from V24 (idempotent IF NOT EXISTS) ───────────────────────

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

-- ─── 6. adjustments extra columns (V24) ──────────────────────────────────────
ALTER TABLE adjustments
    ADD COLUMN IF NOT EXISTS delivery_order_id    BIGINT REFERENCES delivery_orders(id),
    ADD COLUMN IF NOT EXISTS do_item_id           BIGINT REFERENCES delivery_order_items(id),
    ADD COLUMN IF NOT EXISTS allocation_id        BIGINT REFERENCES delivery_order_item_allocations(id),
    ADD COLUMN IF NOT EXISTS outbound_qc_record_id BIGINT REFERENCES outbound_qc_records(id),
    ADD COLUMN IF NOT EXISTS quarantine_record_id  BIGINT REFERENCES quarantine_records(id);
