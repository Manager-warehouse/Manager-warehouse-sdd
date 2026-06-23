-- V23: Add columns missing from Supabase due to divergent migration history

-- delivery_order_items: serial_number for product-level serial tracking
ALTER TABLE delivery_order_items
    ADD COLUMN IF NOT EXISTS serial_number VARCHAR(100);

-- products: has_serial and has_expiry flags (existed in V1 but may be absent on some DB instances)
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS has_serial  BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS has_expiry  BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS shelf_life_days INTEGER;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS unit_per_pack INTEGER;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS weight_kg NUMERIC(10,3);

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS volume_m3 NUMERIC(10,5);

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS reorder_point NUMERIC(10,2);
