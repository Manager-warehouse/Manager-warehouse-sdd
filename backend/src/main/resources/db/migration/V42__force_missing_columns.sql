-- V42: Force-add all columns that prior migrations (V23, V37, V38, V41) may have
-- recorded in Flyway history without actually executing on this Supabase instance.

-- products
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS has_expiry       BOOLEAN      NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS has_serial       BOOLEAN      NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS shelf_life_days  INTEGER,
    ADD COLUMN IF NOT EXISTS unit_per_pack    INTEGER      NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS weight_kg        NUMERIC(10,3) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS volume_m3        NUMERIC(10,6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reorder_point    NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS image_url        TEXT,
    ADD COLUMN IF NOT EXISTS description      TEXT,
    ADD COLUMN IF NOT EXISTS created_by       BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by       BIGINT REFERENCES users(id);

-- delivery_order_items
ALTER TABLE delivery_order_items
    ADD COLUMN IF NOT EXISTS serial_number VARCHAR(100);

-- transfers
ALTER TABLE transfers
    ADD COLUMN IF NOT EXISTS external_instruction_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS rejected_by               BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS rejected_at               TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason          TEXT,
    ADD COLUMN IF NOT EXISTS trip_id                   BIGINT REFERENCES trips(id),
    ADD COLUMN IF NOT EXISTS notes                     TEXT;

-- transfer_items
ALTER TABLE transfer_items
    ADD COLUMN IF NOT EXISTS qc_passed_qty     DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS qc_failed_qty     DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS qc_result         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS qc_failure_reason TEXT;
