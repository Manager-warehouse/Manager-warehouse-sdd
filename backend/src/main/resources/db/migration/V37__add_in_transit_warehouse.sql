INSERT INTO warehouses (code, name, type, address, is_active, created_at)
SELECT 'WH-TRANSIT', 'Kho Đang Giao Hàng', 'IN_TRANSIT', 'Hệ thống - Ảo', true, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM warehouses WHERE type = 'IN_TRANSIT');

INSERT INTO warehouse_locations (warehouse_id, code, type, capacity_m3, capacity_kg, is_quarantine, is_active)
SELECT id, 'LOC-TRANSIT-1', 'BIN', 99999, 99999, false, true
FROM warehouses 
WHERE type = 'IN_TRANSIT'
AND NOT EXISTS (SELECT 1 FROM warehouse_locations WHERE warehouse_id = warehouses.id AND type = 'BIN');
