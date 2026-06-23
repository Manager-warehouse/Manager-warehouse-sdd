-- Remove serial_number column from delivery_order_items table.
ALTER TABLE delivery_order_items
    DROP COLUMN IF EXISTS serial_number;
