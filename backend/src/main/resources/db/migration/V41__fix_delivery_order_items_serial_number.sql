-- V41: Ensure serial_number column exists on delivery_order_items
-- V38 may have been recorded in Flyway history without the DDL executing on this DB instance
ALTER TABLE delivery_order_items
    ADD COLUMN IF NOT EXISTS serial_number VARCHAR(100);
