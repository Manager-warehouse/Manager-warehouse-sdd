-- Remove legacy serial/expiry/grade schema from the household-goods WMS domain.
-- Sprint 1 tracks batches by product, source document, warehouse, and received date only.

DROP INDEX IF EXISTS idx_batches_product_expiry;
DROP INDEX IF EXISTS idx_batches_grade;

ALTER TABLE products
    DROP COLUMN IF EXISTS has_expiry,
    DROP COLUMN IF EXISTS shelf_life_days,
    DROP COLUMN IF EXISTS has_serial;

ALTER TABLE batches
    DROP COLUMN IF EXISTS expiry_date,
    DROP COLUMN IF EXISTS grade;

ALTER TABLE receipt_items
    DROP COLUMN IF EXISTS grade,
    DROP COLUMN IF EXISTS serial_number;

ALTER TABLE delivery_order_items
    DROP COLUMN IF EXISTS serial_number;
