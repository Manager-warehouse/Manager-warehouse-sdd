-- V24: Add product columns missing from Supabase due to divergent migration history
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS has_serial       BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS has_expiry       BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS shelf_life_days  INTEGER,
    ADD COLUMN IF NOT EXISTS unit_per_pack    INTEGER,
    ADD COLUMN IF NOT EXISTS weight_kg        NUMERIC(10,3),
    ADD COLUMN IF NOT EXISTS volume_m3        NUMERIC(10,5),
    ADD COLUMN IF NOT EXISTS reorder_point    NUMERIC(10,2);
