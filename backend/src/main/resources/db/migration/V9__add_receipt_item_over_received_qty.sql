-- Track excess physical quantity counted beyond the receipt item expected quantity.

ALTER TABLE receipt_items
    ADD COLUMN IF NOT EXISTS over_received_qty DECIMAL(10,2) NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'receipt_items_over_received_qty_non_negative'
    ) THEN
        ALTER TABLE receipt_items
            ADD CONSTRAINT receipt_items_over_received_qty_non_negative
            CHECK (over_received_qty >= 0);
    END IF;
END $$;
