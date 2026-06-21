-- V37: Ensure products table has all columns required by the Product entity
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS has_expiry      BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS has_serial      BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS shelf_life_days INTEGER,
    ADD COLUMN IF NOT EXISTS unit_per_pack   INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS weight_kg       NUMERIC(10,3) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS volume_m3       NUMERIC(10,6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reorder_point   NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS image_url       TEXT,
    ADD COLUMN IF NOT EXISTS description     TEXT,
    ADD COLUMN IF NOT EXISTS created_by      BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by      BIGINT REFERENCES users(id);
