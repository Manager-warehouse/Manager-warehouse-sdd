-- =============================================================================
-- V30: Align core/master-data tables with V1 schema + V2 auth columns
-- =============================================================================
-- Context: DB được khởi tạo từ schema rất cũ, thiếu nhiều cột V1 core.
-- Tất cả ADD COLUMN IF NOT EXISTS để idempotent.
-- =============================================================================

-- ─── products: has_expiry, has_serial, shelf_life_days, reorder_point ────────
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
ALTER TABLE products ALTER COLUMN updated_at SET NOT NULL;

-- ─── users: job_title, shift, region, refresh_token_hash, otp fields ─────────
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

-- ─── suppliers: contact_person, address, created_by, updated_by ──────────────
ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS contact_person VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address        TEXT,
    ADD COLUMN IF NOT EXISTS tax_code       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS created_by     BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by     BIGINT REFERENCES users(id);

-- ─── warehouse_locations: extra columns ──────────────────────────────────────
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
ALTER TABLE warehouse_locations ALTER COLUMN updated_at SET NOT NULL;

-- ─── vehicles: max_volume_m3, warehouse_id, created_by, updated_by ───────────
ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS max_volume_m3 DECIMAL(10,3),
    ADD COLUMN IF NOT EXISTS warehouse_id  BIGINT REFERENCES warehouses(id),
    ADD COLUMN IF NOT EXISTS created_by    BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by    BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_at    TIMESTAMPTZ;

UPDATE vehicles SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE vehicles ALTER COLUMN updated_at SET NOT NULL;

-- ─── drivers: license_expiry, warehouse_id, created_by, updated_by ───────────
ALTER TABLE drivers
    ADD COLUMN IF NOT EXISTS license_expiry DATE,
    ADD COLUMN IF NOT EXISTS warehouse_id   BIGINT REFERENCES warehouses(id),
    ADD COLUMN IF NOT EXISTS created_by     BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by     BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_at     TIMESTAMPTZ;

-- Set NOT NULL trên license_expiry sau khi đã có default
UPDATE drivers SET license_expiry = CURRENT_DATE + INTERVAL '1 year' WHERE license_expiry IS NULL;
ALTER TABLE drivers ALTER COLUMN license_expiry SET NOT NULL;

UPDATE drivers SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE drivers ALTER COLUMN updated_at SET NOT NULL;

-- ─── purchase_orders: updated_at ─────────────────────────────────────────────
ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE purchase_orders SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE purchase_orders ALTER COLUMN updated_at SET NOT NULL;

-- ─── receipt_items: over_received_qty (nullable) ──────────────────────────────
ALTER TABLE receipt_items
    ADD COLUMN IF NOT EXISTS over_received_qty DECIMAL(10,2);

-- ─── transfers: extra columns (if table exists) ───────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'transfers') THEN
        ALTER TABLE transfers ADD COLUMN IF NOT EXISTS discrepancy_reason TEXT;
    END IF;
END $$;

-- ─── stock_takes: updated_at (if table exists) ────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'stock_takes') THEN
        ALTER TABLE stock_takes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
        UPDATE stock_takes SET updated_at = created_at WHERE updated_at IS NULL;
        ALTER TABLE stock_takes ALTER COLUMN updated_at SET NOT NULL;
    END IF;
END $$;

-- ─── invoices: updated_at (if table exists) ───────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'invoices') THEN
        ALTER TABLE invoices ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
        UPDATE invoices SET updated_at = created_at WHERE updated_at IS NULL;
        ALTER TABLE invoices ALTER COLUMN updated_at SET NOT NULL;
    END IF;
END $$;

-- ─── deliveries: updated_at (if table exists) ────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'deliveries') THEN
        ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
        UPDATE deliveries SET updated_at = created_at WHERE updated_at IS NULL;
        ALTER TABLE deliveries ALTER COLUMN updated_at SET NOT NULL;
    END IF;
END $$;

-- ─── trips: updated_at (if table exists) ─────────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'trips') THEN
        ALTER TABLE trips ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
        UPDATE trips SET updated_at = created_at WHERE updated_at IS NULL;
        ALTER TABLE trips ALTER COLUMN updated_at SET NOT NULL;
    END IF;
END $$;

-- ─── audit_logs: description NOT NULL alignment ───────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'audit_logs') THEN
        UPDATE audit_logs SET description = '' WHERE description IS NULL;
    END IF;
END $$;
