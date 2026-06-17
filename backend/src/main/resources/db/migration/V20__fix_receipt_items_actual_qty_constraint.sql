-- Fix receipt_items actual_qty constraint to allow 0 value (allowsZeroCount)
ALTER TABLE receipt_items DROP CONSTRAINT IF EXISTS receipt_items_actual_qty_positive;
ALTER TABLE receipt_items DROP CONSTRAINT IF EXISTS receipt_items_actual_qty_non_negative;

ALTER TABLE receipt_items
    ADD CONSTRAINT receipt_items_actual_qty_non_negative
    CHECK (actual_qty IS NULL OR actual_qty >= 0);
