-- Allow cancelled planned trips to release Delivery Orders while preserving trip membership history.

ALTER TABLE trip_delivery_orders
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE trip_delivery_orders tdo
SET is_active = FALSE
FROM trips t
WHERE tdo.trip_id = t.id
  AND t.status = 'CANCELLED';

ALTER TABLE trip_delivery_orders
    DROP CONSTRAINT IF EXISTS trip_delivery_orders_do_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS trip_delivery_orders_do_id_key
    ON trip_delivery_orders (do_id)
    WHERE is_active;
