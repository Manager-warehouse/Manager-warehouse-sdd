-- Track excess physical quantity counted beyond the receipt item expected quantity.

ALTER TABLE receipt_items
    ADD COLUMN IF NOT EXISTS over_received_qty DECIMAL(10,2) NOT NULL DEFAULT 0;

ALTER TABLE receipt_items DROP CONSTRAINT IF EXISTS receipt_items_over_received_qty_non_negative;

ALTER TABLE receipt_items
    ADD CONSTRAINT receipt_items_over_received_qty_non_negative
    CHECK (over_received_qty >= 0);
