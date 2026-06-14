-- Align Sprint 1 master data schema with the current SDD.
-- This migration is for databases that already applied V1__init_schema.sql.

-- =============================================================================
-- Product master data
-- =============================================================================

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS shelf_life_days INTEGER,
    ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users(id);

DROP TABLE IF EXISTS product_units;

COMMENT ON COLUMN products.unit IS 'Base inventory unit. Sprint 1 uses cái as the base unit.';
COMMENT ON COLUMN products.unit_per_pack IS 'Fixed conversion from thùng to cái for Sprint 1.';
COMMENT ON COLUMN products.shelf_life_days IS 'Shelf life in days for products that require expiry tracking.';

-- =============================================================================
-- Warehouse and location master data
-- =============================================================================

ALTER TABLE warehouses
    ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE warehouse_locations
    ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM warehouse_locations
        WHERE type IN ('RACK', 'SHELF')
    ) THEN
        RAISE EXCEPTION 'Cannot migrate warehouse_locations to Zone -> Bin while RACK/SHELF rows still exist. Clean or map those rows before running V2.';
    END IF;
END $$;

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
          AND rel.relname = 'warehouse_locations'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%type%'
    LOOP
        EXECUTE format('ALTER TABLE warehouse_locations DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE warehouse_locations
    ADD CONSTRAINT chk_warehouse_locations_type
        CHECK (type IN ('ZONE', 'BIN')),
    ADD CONSTRAINT chk_warehouse_locations_zone_bin_parent
        CHECK (
            (type = 'ZONE' AND parent_id IS NULL)
            OR (type = 'BIN' AND parent_id IS NOT NULL)
        );

COMMENT ON TABLE warehouse_locations IS 'Warehouse location hierarchy for Sprint 1: Zone -> Bin.';
COMMENT ON COLUMN warehouse_locations.capacity_m3 IS 'Capacity is configured at BIN level only.';
COMMENT ON COLUMN warehouse_locations.capacity_kg IS 'Capacity is configured at BIN level only.';

-- =============================================================================
-- Partner and fleet master data
-- =============================================================================

ALTER TABLE dealers
    ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users(id);

ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE drivers
    ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE drivers
SET status = 'ON_TRIP'
WHERE status = 'ON_DELIVERY';

UPDATE drivers
SET status = 'UNAVAILABLE'
WHERE status = 'MAINTENANCE';

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
          AND rel.relname = 'drivers'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE drivers DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE drivers
    ADD CONSTRAINT chk_drivers_status
        CHECK (status IN ('AVAILABLE', 'ON_TRIP', 'UNAVAILABLE'));

COMMENT ON COLUMN vehicles.status IS 'Vehicle status: AVAILABLE, ON_TRIP, MAINTENANCE.';
COMMENT ON COLUMN drivers.status IS 'Driver status: AVAILABLE, ON_TRIP, UNAVAILABLE.';

-- =============================================================================
-- Trips
-- =============================================================================

ALTER TABLE trips
    ADD COLUMN IF NOT EXISTS trip_type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY';

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
          AND pg_get_constraintdef(con.oid) ILIKE '%trip_type%'
    LOOP
        EXECUTE format('ALTER TABLE trips DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE trips
    ADD CONSTRAINT chk_trips_trip_type
        CHECK (trip_type IN ('DELIVERY', 'TRANSFER'));

COMMENT ON COLUMN trips.trip_type IS 'Trip business type. Driver and vehicle busy status is represented by ON_TRIP.';
