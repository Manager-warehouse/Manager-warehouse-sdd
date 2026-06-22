UPDATE warehouses 
SET is_active = true 
WHERE type = 'IN_TRANSIT';

UPDATE warehouse_locations 
SET is_active = true 
WHERE warehouse_id IN (SELECT id FROM warehouses WHERE type = 'IN_TRANSIT');
