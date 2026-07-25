ALTER TABLE returned_delivery_flow_items
    ALTER COLUMN counted_qty DROP NOT NULL,
    ALTER COLUMN quality_result DROP NOT NULL;
