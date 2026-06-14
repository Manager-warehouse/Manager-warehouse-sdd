-- Track excess physical quantity counted beyond the receipt item expected quantity.

ALTER TABLE receipt_items
    ADD COLUMN over_received_qty DECIMAL(10,2) NOT NULL DEFAULT 0;

ALTER TABLE receipt_items
    ADD CONSTRAINT receipt_items_over_received_qty_non_negative
    CHECK (over_received_qty >= 0);
