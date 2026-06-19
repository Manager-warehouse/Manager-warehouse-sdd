ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS warehouse_id BIGINT REFERENCES warehouses(id);

UPDATE vehicles
SET warehouse_id = (
    SELECT id
    FROM warehouses
    WHERE type = 'PHYSICAL'
      AND is_active = TRUE
    ORDER BY id
    LIMIT 1
)
WHERE warehouse_id IS NULL;

ALTER TABLE vehicles
    ALTER COLUMN warehouse_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_vehicles_warehouse_status
    ON vehicles (warehouse_id, status, is_active);
