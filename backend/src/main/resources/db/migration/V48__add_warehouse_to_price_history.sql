-- Add warehouse_id to price_history so prices are scoped per warehouse.
-- Existing rows (if any) default to the first warehouse; the column is NOT NULL
-- after the backfill so the constraint is safe.
ALTER TABLE price_history
    ADD COLUMN IF NOT EXISTS warehouse_id BIGINT REFERENCES warehouses(id);

-- Backfill any existing rows to the warehouse with the lowest id so the NOT NULL
-- constraint can be applied without errors on a pre-populated database.
UPDATE price_history
SET warehouse_id = (SELECT MIN(id) FROM warehouses)
WHERE warehouse_id IS NULL;

ALTER TABLE price_history
    ALTER COLUMN warehouse_id SET NOT NULL;

-- Replace the existing index with warehouse-scoped versions.
DROP INDEX IF EXISTS idx_price_history_product_status;
DROP INDEX IF EXISTS idx_price_history_product_created;

CREATE INDEX IF NOT EXISTS idx_price_history_product_warehouse_status
    ON price_history (product_id, warehouse_id, status, effective_date, end_date);

CREATE INDEX IF NOT EXISTS idx_price_history_product_warehouse_created
    ON price_history (product_id, warehouse_id, created_at DESC);
