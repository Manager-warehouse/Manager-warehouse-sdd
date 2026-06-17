-- Remove expiry_date and grade from batches table.
-- Household goods domain does not track expiry dates or quality grades for batch-level tracking.
-- FIFO ordering is based solely on received_date.
-- Corresponds to AGENTS.md: NEVER add expiry-date or grade tracking for household goods.

DROP VIEW IF EXISTS v_inventory_by_batch CASCADE;

ALTER TABLE batches
    DROP COLUMN IF EXISTS expiry_date,
    DROP COLUMN IF EXISTS grade;

CREATE OR REPLACE VIEW v_inventory_by_batch AS
SELECT
    w.code                                   AS warehouse_code,
    p.sku,
    p.name                                   AS product_name,
    b.batch_number,
    b.received_date,
    wl.code                                  AS location_code,
    i.total_qty,
    i.reserved_qty,
    (i.total_qty - i.reserved_qty)           AS available_qty,
    i.cost_price,
    (i.total_qty * i.cost_price)             AS line_value
FROM inventories         i
JOIN warehouses          w  ON w.id  = i.warehouse_id
JOIN products            p  ON p.id  = i.product_id
JOIN batches             b  ON b.id  = i.batch_id
JOIN warehouse_locations wl ON wl.id = i.location_id
WHERE w.type = 'PHYSICAL'
ORDER BY w.code, p.sku,
         b.received_date ASC NULLS LAST;
