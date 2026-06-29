ALTER TABLE adjustments ADD COLUMN IF NOT EXISTS allocation_id BIGINT REFERENCES delivery_order_item_allocations(id);
