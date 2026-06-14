ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS attempt_number INTEGER,
    ADD COLUMN IF NOT EXISTS dispatched_at TIMESTAMPTZ;

WITH numbered_attempts AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY do_id ORDER BY created_at, id) AS rn
    FROM deliveries
    WHERE attempt_number IS NULL
)
UPDATE deliveries d
SET attempt_number = numbered_attempts.rn
FROM numbered_attempts
WHERE d.id = numbered_attempts.id;

ALTER TABLE deliveries
    ALTER COLUMN attempt_number SET NOT NULL;

ALTER TABLE deliveries
    DROP CONSTRAINT IF EXISTS deliveries_status_check;

ALTER TABLE deliveries
    ADD CONSTRAINT deliveries_status_check
    CHECK (status IN ('PENDING','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','FAILED','RETURNED'));

CREATE UNIQUE INDEX IF NOT EXISTS ux_deliveries_do_attempt
    ON deliveries(do_id, attempt_number);

ALTER TABLE delivery_orders
    DROP CONSTRAINT IF EXISTS delivery_orders_status_check;

ALTER TABLE delivery_orders
    ADD CONSTRAINT delivery_orders_status_check
    CHECK (status IN (
        'NEW','PICKING','PENDING_WAREHOUSE_APPROVAL','READY_TO_SHIP',
        'IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','RETURNED',
        'COMPLETED','CLOSED','CANCELLED'
    ));
