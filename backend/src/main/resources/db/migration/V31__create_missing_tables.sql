-- V31: Create all tables missing from DB (transfers, stock_takes, adjustments, etc.)
-- All IF NOT EXISTS — idempotent.

CREATE TABLE IF NOT EXISTS transfers (
    id                       BIGSERIAL   PRIMARY KEY,
    transfer_number          VARCHAR(50) UNIQUE NOT NULL,
    source_warehouse_id      BIGINT      NOT NULL REFERENCES warehouses(id),
    destination_warehouse_id BIGINT      NOT NULL REFERENCES warehouses(id),
    status                   VARCHAR(40) NOT NULL DEFAULT 'NEW'
                             CHECK (status IN ('NEW','APPROVED','IN_TRANSIT','COMPLETED','COMPLETED_WITH_DISCREPANCY','CANCELLED')),
    created_by               BIGINT      NOT NULL REFERENCES users(id),
    approved_by              BIGINT      REFERENCES users(id),
    approved_at              TIMESTAMPTZ,
    confirmed_by             BIGINT      REFERENCES users(id),
    confirmed_at             TIMESTAMPTZ,
    planned_date             DATE,
    actual_received_date     DATE,
    discrepancy_reason       TEXT,
    document_date            DATE        NOT NULL,
    accounting_period_id     BIGINT      REFERENCES accounting_periods(id),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS transfer_items (
    id                      BIGSERIAL     PRIMARY KEY,
    transfer_id             BIGINT        NOT NULL REFERENCES transfers(id),
    product_id              BIGINT        NOT NULL REFERENCES products(id),
    batch_id                BIGINT        NOT NULL REFERENCES batches(id),
    source_location_id      BIGINT        REFERENCES warehouse_locations(id),
    destination_location_id BIGINT        REFERENCES warehouse_locations(id),
    planned_qty             DECIMAL(10,2) NOT NULL,
    sent_qty                DECIMAL(10,2),
    received_qty            DECIMAL(10,2),
    variance_qty            DECIMAL(10,2)
);

CREATE TABLE IF NOT EXISTS stock_takes (
    id                   BIGSERIAL     PRIMARY KEY,
    stock_take_number    VARCHAR(50)   UNIQUE NOT NULL,
    warehouse_id         BIGINT        NOT NULL REFERENCES warehouses(id),
    conducted_by         BIGINT        NOT NULL REFERENCES users(id),
    approved_by          BIGINT        REFERENCES users(id),
    approved_at          TIMESTAMPTZ,
    status               VARCHAR(30)   NOT NULL DEFAULT 'DRAFT'
                         CHECK (status IN ('DRAFT','IN_PROGRESS','PENDING_APPROVAL','APPROVED','CANCELLED')),
    total_variance_value DECIMAL(18,2) DEFAULT 0,
    stock_take_date      DATE          NOT NULL,
    document_date        DATE          NOT NULL,
    accounting_period_id BIGINT        REFERENCES accounting_periods(id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS stock_take_items (
    id             BIGSERIAL     PRIMARY KEY,
    stock_take_id  BIGINT        NOT NULL REFERENCES stock_takes(id),
    product_id     BIGINT        NOT NULL REFERENCES products(id),
    batch_id       BIGINT        NOT NULL REFERENCES batches(id),
    location_id    BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    system_qty     DECIMAL(10,2) NOT NULL,
    actual_qty     DECIMAL(10,2) NOT NULL,
    variance_qty   DECIMAL(10,2) NOT NULL,
    variance_value DECIMAL(18,2) NOT NULL,
    notes          TEXT
);

CREATE TABLE IF NOT EXISTS adjustments (
    id                   BIGSERIAL     PRIMARY KEY,
    adjustment_number    VARCHAR(50)   UNIQUE NOT NULL,
    warehouse_id         BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id           BIGINT        NOT NULL REFERENCES products(id),
    batch_id             BIGINT        REFERENCES batches(id),
    location_id          BIGINT        REFERENCES warehouse_locations(id),
    quantity_adjustment  DECIMAL(10,2) NOT NULL,
    type                 VARCHAR(30)   NOT NULL
                         CHECK (type IN ('STOCK_TAKE','TRANSFER_DISCREPANCY','DISPOSAL','RETURN_TO_VENDOR','CORRECTION_VOUCHER','QC_FAIL_OUTBOUND')),
    reference_id         BIGINT,
    reference_type       VARCHAR(50),
    reason               TEXT          NOT NULL,
    approved_by          BIGINT        REFERENCES users(id),
    approved_at          TIMESTAMPTZ,
    document_date        DATE          NOT NULL,
    accounting_period_id BIGINT        REFERENCES accounting_periods(id),
    created_by           BIGINT        NOT NULL REFERENCES users(id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS damage_reports (
    id            BIGSERIAL     PRIMARY KEY,
    report_number VARCHAR(50)   UNIQUE NOT NULL,
    warehouse_id  BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id    BIGINT        NOT NULL REFERENCES products(id),
    batch_id      BIGINT        REFERENCES batches(id),
    quantity      DECIMAL(10,2) NOT NULL,
    cause         TEXT          NOT NULL,
    image_url     VARCHAR(500),
    reported_by   BIGINT        NOT NULL REFERENCES users(id),
    report_date   DATE          NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- adjustments extra columns (V24)
ALTER TABLE adjustments
    ADD COLUMN IF NOT EXISTS delivery_order_id    BIGINT,
    ADD COLUMN IF NOT EXISTS do_item_id           BIGINT,
    ADD COLUMN IF NOT EXISTS allocation_id        BIGINT,
    ADD COLUMN IF NOT EXISTS outbound_qc_record_id BIGINT,
    ADD COLUMN IF NOT EXISTS quarantine_record_id  BIGINT;

CREATE INDEX IF NOT EXISTS idx_transfers_src_status ON transfers(source_warehouse_id, status);
CREATE INDEX IF NOT EXISTS idx_stock_takes_warehouse ON stock_takes(warehouse_id, status);
