-- Demo seed data for inter-warehouse transfer testing
-- Adds master data and inventory in HP/HN so planner/transfer flows have stock to move.

-- Warehouse locations
INSERT INTO warehouse_locations (
    warehouse_id, code, type, parent_id, capacity_m3, capacity_kg, current_volume_m3, current_weight_kg,
    is_quarantine, is_active, created_at, updated_at
)
SELECT w.id, x.code, x.type, NULL, x.capacity_m3, x.capacity_kg, 0, 0, x.is_quarantine, true, NOW(), NOW()
FROM warehouses w
JOIN (
    VALUES
        ('WH-HP',  'WH-HP-Z01', 'ZONE', 50.000, 5000.00, false),
        ('WH-HN',  'WH-HN-Z01', 'ZONE', 50.000, 5000.00, false),
        ('WH-HCM', 'WH-HCM-Z01', 'ZONE', 50.000, 5000.00, false),
        ('IN_TRANSIT', 'INT-Z01', 'ZONE', 100.000, 10000.00, false)
) AS x(warehouse_code, code, type, capacity_m3, capacity_kg, is_quarantine)
ON w.code = x.warehouse_code
ON CONFLICT (code) DO NOTHING;

INSERT INTO warehouse_locations (
    warehouse_id, code, type, parent_id, capacity_m3, capacity_kg, current_volume_m3, current_weight_kg,
    is_quarantine, is_active, created_at, updated_at
)
SELECT w.id, x.code, x.type, parent.id, x.capacity_m3, x.capacity_kg, 0, 0, x.is_quarantine, true, NOW(), NOW()
FROM warehouses w
JOIN (
    VALUES
        ('WH-HP',  'WH-HP-B01', 'BIN',  10.000, 1000.00, false, 'WH-HP-Z01'),
        ('WH-HP',  'WH-HP-Q01', 'BIN',   8.000,  800.00, true,  'WH-HP-Z01'),
        ('WH-HN',  'WH-HN-B01', 'BIN',  10.000, 1000.00, false, 'WH-HN-Z01'),
        ('WH-HN',  'WH-HN-Q01', 'BIN',   8.000,  800.00, true,  'WH-HN-Z01'),
        ('WH-HCM', 'WH-HCM-B01', 'BIN',  10.000, 1000.00, false, 'WH-HCM-Z01'),
        ('WH-HCM', 'WH-HCM-Q01', 'BIN',   8.000,  800.00, true,  'WH-HCM-Z01'),
        ('IN_TRANSIT', 'INT-01', 'BIN', 100.000, 10000.00, false, 'INT-Z01')
) AS x(warehouse_code, code, type, capacity_m3, capacity_kg, is_quarantine, parent_code)
    ON w.code = x.warehouse_code
JOIN warehouse_locations parent ON parent.code = x.parent_code
ON CONFLICT (code) DO NOTHING;

-- Products for household-goods transfer testing
INSERT INTO products (
    sku, name, unit, unit_per_pack, description, weight_kg, volume_m3, has_expiry, has_serial,
    reorder_point, is_active, created_at, updated_at
)
VALUES
    ('SKU-TRF-001', 'Nồi chống dính 24cm', 'cái', NULL, 'Demo transfer stock', 1.400, 0.00450, false, false, 10, true, NOW(), NOW()),
    ('SKU-TRF-002', 'Chảo inox 28cm',       'cái', NULL, 'Demo transfer stock', 1.200, 0.00400, false, false, 10, true, NOW(), NOW()),
    ('SKU-TRF-003', 'Hộp nhựa 10L',         'cái', NULL, 'Demo transfer stock', 0.300, 0.00250, false, false, 20, true, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Batches and inventories for HP/HN so transfer can be tested immediately
WITH batch_seed AS (
    SELECT * FROM (VALUES
        ('BATCH-HP-001', 'SKU-TRF-001', 'WH-HP', DATE '2026-06-10', 35.00, 150.00, 'WH-HP-B01'),
        ('BATCH-HP-002', 'SKU-TRF-002', 'WH-HP', DATE '2026-06-11', 25.00, 180.00, 'WH-HP-B01'),
        ('BATCH-HN-001', 'SKU-TRF-001', 'WH-HN', DATE '2026-06-09', 20.00, 148.00, 'WH-HN-B01'),
        ('BATCH-HN-002', 'SKU-TRF-003', 'WH-HN', DATE '2026-06-12', 60.00, 20.00,  'WH-HN-B01')
    ) AS t(batch_number, sku, warehouse_code, received_date, quantity, cost_price, location_code)
)
INSERT INTO batches (
    batch_number, product_id, warehouse_id, received_date, grade, quantity, created_at
)
SELECT b.batch_number,
       p.id,
       w.id,
       b.received_date,
       'A',
       b.quantity,
       NOW()
FROM batch_seed b
JOIN products p ON p.sku = b.sku
JOIN warehouses w ON w.code = b.warehouse_code
ON CONFLICT (batch_number) DO NOTHING;

INSERT INTO inventories (
    warehouse_id, product_id, batch_id, location_id, total_qty, reserved_qty, cost_price, version, updated_at
)
SELECT
    w.id,
    p.id,
    bt.id,
    loc.id,
    seed.quantity,
    0,
    seed.cost_price,
    0,
    NOW()
FROM (
    VALUES
        ('BATCH-HP-001', 'SKU-TRF-001', 'WH-HP', 'WH-HP-B01', 35.00, 150.00),
        ('BATCH-HP-002', 'SKU-TRF-002', 'WH-HP', 'WH-HP-B01', 25.00, 180.00),
        ('BATCH-HN-001', 'SKU-TRF-001', 'WH-HN', 'WH-HN-B01', 20.00, 148.00),
        ('BATCH-HN-002', 'SKU-TRF-003', 'WH-HN', 'WH-HN-B01', 60.00, 20.00)
) AS seed(batch_number, sku, warehouse_code, location_code, quantity, cost_price)
JOIN products p ON p.sku = seed.sku
JOIN warehouses w ON w.code = seed.warehouse_code
JOIN batches bt ON bt.batch_number = seed.batch_number
JOIN warehouse_locations loc ON loc.code = seed.location_code
ON CONFLICT (warehouse_id, product_id, batch_id, location_id) DO NOTHING;
