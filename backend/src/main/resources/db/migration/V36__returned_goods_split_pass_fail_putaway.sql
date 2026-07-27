ALTER TABLE returned_delivery_flow_items
    ADD COLUMN failed_destination_location_id BIGINT REFERENCES warehouse_locations(id),
    ADD COLUMN failed_planned_qty NUMERIC(10, 2) CHECK (failed_planned_qty >= 0),
    ADD COLUMN failed_putaway_completed_qty NUMERIC(10, 2) CHECK (failed_putaway_completed_qty >= 0);
