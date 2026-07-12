ALTER TABLE delivery_order_item_allocations
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE delivery_order_item_allocations
    ADD CONSTRAINT ck_delivery_order_item_allocations_status
    CHECK (status IN ('ACTIVE', 'CANCELLED'));

ALTER TABLE delivery_orders
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
