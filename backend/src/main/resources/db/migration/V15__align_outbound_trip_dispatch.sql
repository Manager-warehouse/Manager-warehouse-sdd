-- Align outbound trip dispatch with current SDD.

-- Fleet and drivers must be assigned to a physical warehouse so outbound trip
-- validation can enforce dispatcher/vehicle/driver warehouse scope.
ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS warehouse_id BIGINT REFERENCES warehouses(id);

ALTER TABLE drivers
    ADD COLUMN IF NOT EXISTS warehouse_id BIGINT REFERENCES warehouses(id);

UPDATE vehicles
SET warehouse_id = (
    SELECT id
    FROM warehouses
    WHERE type = 'PHYSICAL'
    ORDER BY id
    LIMIT 1
)
WHERE warehouse_id IS NULL;

UPDATE drivers
SET warehouse_id = (
    SELECT id
    FROM warehouses
    WHERE type = 'PHYSICAL'
    ORDER BY id
    LIMIT 1
)
WHERE warehouse_id IS NULL;

ALTER TABLE vehicles
    ALTER COLUMN warehouse_id SET NOT NULL;

ALTER TABLE drivers
    ALTER COLUMN warehouse_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_vehicles_warehouse_status
    ON vehicles(warehouse_id, status, is_active);

CREATE INDEX IF NOT EXISTS idx_drivers_warehouse_status
    ON drivers(warehouse_id, status, is_active);

COMMENT ON COLUMN vehicles.warehouse_id IS 'Assigned physical warehouse for dispatcher trip planning.';
COMMENT ON COLUMN drivers.warehouse_id IS 'Assigned physical warehouse for dispatcher trip planning.';

-- Outbound trips keep warehouse and lifecycle metadata directly on the trip.
ALTER TABLE trips
    ADD COLUMN IF NOT EXISTS warehouse_id BIGINT REFERENCES warehouses(id),
    ADD COLUMN IF NOT EXISTS cancel_reason TEXT,
    ADD COLUMN IF NOT EXISTS departed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS notes TEXT;

UPDATE trips t
SET warehouse_id = v.warehouse_id
FROM vehicles v
WHERE t.vehicle_id = v.id
  AND t.warehouse_id IS NULL;

UPDATE trips
SET warehouse_id = (
    SELECT id
    FROM warehouses
    WHERE type = 'PHYSICAL'
    ORDER BY id
    LIMIT 1
)
WHERE warehouse_id IS NULL;

ALTER TABLE trips
    ALTER COLUMN warehouse_id SET NOT NULL;

DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = current_schema()
          AND rel.relname = 'trips'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE trips DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE trips
    ADD CONSTRAINT chk_trips_status
        CHECK (status IN ('PLANNED', 'IN_TRANSIT', 'COMPLETED', 'CANCELLED'));

CREATE INDEX IF NOT EXISTS idx_trips_warehouse_status
    ON trips(warehouse_id, status);

COMMENT ON COLUMN trips.warehouse_id IS 'Source warehouse for outbound DELIVERY trips.';
COMMENT ON COLUMN trips.cancel_reason IS 'Cancellation reason for PLANNED trips cancelled before departure.';
COMMENT ON COLUMN trips.departed_at IS 'Server timestamp when the assigned driver confirms vehicle departure.';
COMMENT ON COLUMN trips.completed_at IS 'Server timestamp when the assigned driver confirms vehicle return to source warehouse.';

-- A Delivery Order may be reassigned after its previous planned trip is cancelled,
-- so do not keep a permanent unique constraint on trip_delivery_orders.do_id.
DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = current_schema()
          AND rel.relname = 'trip_delivery_orders'
          AND con.contype = 'u'
          AND pg_get_constraintdef(con.oid) ILIKE '%do_id%'
          AND pg_get_constraintdef(con.oid) NOT ILIKE '%trip_id%'
    LOOP
        EXECUTE format('ALTER TABLE trip_delivery_orders DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

CREATE INDEX IF NOT EXISTS idx_trip_delivery_orders_do_id
    ON trip_delivery_orders(do_id);
