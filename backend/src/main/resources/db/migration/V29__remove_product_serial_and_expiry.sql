-- Remove has_serial, has_expiry, and shelf_life_days columns from products table.

ALTER TABLE products
    DROP COLUMN IF EXISTS has_serial,
    DROP COLUMN IF EXISTS has_expiry,
    DROP COLUMN IF EXISTS shelf_life_days;
