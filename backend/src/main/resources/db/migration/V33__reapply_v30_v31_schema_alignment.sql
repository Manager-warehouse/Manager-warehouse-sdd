-- V33: Comprehensive schema alignment — add all columns required by Hibernate entity validation
-- All statements are IF NOT EXISTS / idempotent.
-- This migration exists because V30 was previously marked DELETED in flyway_schema_history
-- and may not have been applied to the target database.

-- ─── products: has_expiry, has_serial, shelf_life_days, reorder_point, etc. ───
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS has_expiry      BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS has_serial      BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS shelf_life_days INTEGER,
    ADD COLUMN IF NOT EXISTS reorder_point   DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS unit_per_pack   INTEGER,
    ADD COLUMN IF NOT EXISTS description     TEXT,
    ADD COLUMN IF NOT EXISTS image_url       VARCHAR(500),
    ADD COLUMN IF NOT EXISTS weight_kg       DECIMAL(10,3),
    ADD COLUMN IF NOT EXISTS volume_m3       DECIMAL(10,5),
    ADD COLUMN IF NOT EXISTS is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS created_by      BIGINT        REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by      BIGINT        REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_at      TIMESTAMPTZ;

UPDATE products SET updated_at = created_at WHERE updated_at IS NULL;

DO $$
BEGIN
    ALTER TABLE products ALTER COLUMN updated_at SET NOT NULL;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- ─── users: auth + profile extra columns ──────────────────────────────────────
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS job_title               VARCHAR(100),
    ADD COLUMN IF NOT EXISTS shift                   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS region                  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS refresh_token_hash      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS refresh_token_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS otp_hash                VARCHAR(255),
    ADD COLUMN IF NOT EXISTS otp_expires_at          TIMESTAMPTZ;

-- ─── warehouses: phone, manager_id, created_by, updated_by ───────────────────
ALTER TABLE warehouses
    ADD COLUMN IF NOT EXISTS phone      VARCHAR(20),
    ADD COLUMN IF NOT EXISTS manager_id BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users(id);

-- ─── dealers: email ───────────────────────────────────────────────────────────
ALTER TABLE dealers
    ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- ─── suppliers: contact_person, address, tax_code, created_by, updated_by ────
ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS contact_person VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address        TEXT,
    ADD COLUMN IF NOT EXISTS tax_code       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS created_by     BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by     BIGINT REFERENCES users(id);

-- ─── warehouse_locations: capacity, quarantine, audit columns ─────────────────
ALTER TABLE warehouse_locations
    ADD COLUMN IF NOT EXISTS capacity_m3       DECIMAL(10,3),
    ADD COLUMN IF NOT EXISTS capacity_kg       DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS current_volume_m3 DECIMAL(10,3) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS current_weight_kg DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_quarantine     BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS created_by        BIGINT        REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by        BIGINT        REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_at        TIMESTAMPTZ;

UPDATE warehouse_locations SET updated_at = created_at WHERE updated_at IS NULL;
DO $$
BEGIN
    ALTER TABLE warehouse_locations ALTER COLUMN updated_at SET NOT NULL;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- ─── vehicles: max_volume_m3, warehouse_id, audit columns ─────────────────────
ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS max_volume_m3 DECIMAL(10,3),
    ADD COLUMN IF NOT EXISTS warehouse_id  BIGINT REFERENCES warehouses(id),
    ADD COLUMN IF NOT EXISTS created_by    BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by    BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_at    TIMESTAMPTZ;

UPDATE vehicles SET updated_at = created_at WHERE updated_at IS NULL;
DO $$
BEGIN
    ALTER TABLE vehicles ALTER COLUMN updated_at SET NOT NULL;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- ─── drivers: license_expiry, warehouse_id, audit columns ─────────────────────
ALTER TABLE drivers
    ADD COLUMN IF NOT EXISTS license_expiry DATE,
    ADD COLUMN IF NOT EXISTS warehouse_id   BIGINT REFERENCES warehouses(id),
    ADD COLUMN IF NOT EXISTS created_by     BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by     BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_at     TIMESTAMPTZ;

UPDATE drivers SET license_expiry = CURRENT_DATE + INTERVAL '1 year' WHERE license_expiry IS NULL;
DO $$
BEGIN
    ALTER TABLE drivers ALTER COLUMN license_expiry SET NOT NULL;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

UPDATE drivers SET updated_at = created_at WHERE updated_at IS NULL;
DO $$
BEGIN
    ALTER TABLE drivers ALTER COLUMN updated_at SET NOT NULL;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- ─── purchase_orders: updated_at ─────────────────────────────────────────────
ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE purchase_orders SET updated_at = created_at WHERE updated_at IS NULL;
DO $$
BEGIN
    ALTER TABLE purchase_orders ALTER COLUMN updated_at SET NOT NULL;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- ─── receipt_items: over_received_qty ────────────────────────────────────────
ALTER TABLE receipt_items
    ADD COLUMN IF NOT EXISTS over_received_qty DECIMAL(10,2);

-- ─── transfers: missing columns (create if table exists) ─────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'transfers') THEN
        ALTER TABLE transfers ADD COLUMN IF NOT EXISTS discrepancy_reason TEXT;
        ALTER TABLE transfers ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
        UPDATE transfers SET updated_at = created_at WHERE updated_at IS NULL;
        ALTER TABLE transfers ALTER COLUMN updated_at SET NOT NULL;
    END IF;
END $$;

-- ─── stock_takes: updated_at ──────────────────────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'stock_takes') THEN
        ALTER TABLE stock_takes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
        UPDATE stock_takes SET updated_at = created_at WHERE updated_at IS NULL;
        ALTER TABLE stock_takes ALTER COLUMN updated_at SET NOT NULL;
    END IF;
END $$;

-- ─── adjustments: extra outbound QC columns ──────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'adjustments') THEN
        ALTER TABLE adjustments ADD COLUMN IF NOT EXISTS delivery_order_id    BIGINT;
        ALTER TABLE adjustments ADD COLUMN IF NOT EXISTS do_item_id           BIGINT;
        ALTER TABLE adjustments ADD COLUMN IF NOT EXISTS allocation_id        BIGINT;
        ALTER TABLE adjustments ADD COLUMN IF NOT EXISTS outbound_qc_record_id BIGINT;
        ALTER TABLE adjustments ADD COLUMN IF NOT EXISTS quarantine_record_id  BIGINT;
    END IF;
END $$;

-- ─── invoices: updated_at ────────────────────────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'invoices') THEN
        ALTER TABLE invoices ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
        UPDATE invoices SET updated_at = created_at WHERE updated_at IS NULL;
        ALTER TABLE invoices ALTER COLUMN updated_at SET NOT NULL;
    END IF;
END $$;

-- ─── trips: updated_at ────────────────────────────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'trips') THEN
        ALTER TABLE trips ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
        UPDATE trips SET updated_at = created_at WHERE updated_at IS NULL;
        ALTER TABLE trips ALTER COLUMN updated_at SET NOT NULL;
    END IF;
END $$;

-- ─── Recreate transfers/stock_takes/adjustments if not exists (V31 content) ──
CREATE TABLE IF NOT EXISTS transfers (
    id                       BIGSERIAL   PRIMARY KEY,
    transfer_number          VARCHAR(50) UNIQUE NOT NULL,
    source_warehouse_id      BIGINT      NOT NULL REFERENCES warehouses(id),
    destination_warehouse_id BIGINT      NOT NULL REFERENCES warehouses(id),
    status                   VARCHAR(40) NOT NULL DEFAULT 'NEW',
    created_by               BIGINT      NOT NULL REFERENCES users(id),
    approved_by              BIGINT      REFERENCES users(id),
    approved_at              TIMESTAMPTZ,
    confirmed_by             BIGINT      REFERENCES users(id),
    confirmed_at             TIMESTAMPTZ,
    planned_date             DATE,
    actual_received_date     DATE,
    discrepancy_reason       TEXT,
    document_date            DATE        NOT NULL,
    accounting_period_id     BIGINT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS stock_takes (
    id                   BIGSERIAL     PRIMARY KEY,
    stock_take_number    VARCHAR(50)   UNIQUE NOT NULL,
    warehouse_id         BIGINT        NOT NULL REFERENCES warehouses(id),
    conducted_by         BIGINT        NOT NULL REFERENCES users(id),
    approved_by          BIGINT        REFERENCES users(id),
    approved_at          TIMESTAMPTZ,
    status               VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    total_variance_value DECIMAL(18,2) DEFAULT 0,
    stock_take_date      DATE          NOT NULL,
    document_date        DATE          NOT NULL,
    accounting_period_id BIGINT,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS adjustments (
    id                   BIGSERIAL     PRIMARY KEY,
    adjustment_number    VARCHAR(50)   UNIQUE NOT NULL,
    warehouse_id         BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id           BIGINT        NOT NULL REFERENCES products(id),
    batch_id             BIGINT        REFERENCES batches(id),
    location_id          BIGINT        REFERENCES warehouse_locations(id),
    quantity_adjustment  DECIMAL(10,2) NOT NULL,
    type                 VARCHAR(30)   NOT NULL,
    reference_id         BIGINT,
    reference_type       VARCHAR(50),
    reason               TEXT          NOT NULL,
    approved_by          BIGINT        REFERENCES users(id),
    approved_at          TIMESTAMPTZ,
    document_date        DATE          NOT NULL,
    accounting_period_id BIGINT,
    created_by           BIGINT        NOT NULL REFERENCES users(id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    delivery_order_id    BIGINT,
    do_item_id           BIGINT,
    allocation_id        BIGINT,
    outbound_qc_record_id BIGINT,
    quarantine_record_id  BIGINT
);
