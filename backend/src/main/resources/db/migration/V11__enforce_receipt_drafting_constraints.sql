-- Enforce planner receipt drafting invariants at the database boundary.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM receipt_items
        WHERE expected_qty <> TRUNC(expected_qty)
    ) THEN
        RAISE EXCEPTION 'Cannot convert receipt_items.expected_qty to INTEGER while fractional values exist';
    END IF;
END $$;

UPDATE receipts
SET source_channel = UPPER(TRIM(source_channel))
WHERE source_channel IS NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM receipts
        WHERE source_channel IS NOT NULL
          AND source_channel NOT IN ('ZALO', 'EMAIL')
    ) THEN
        RAISE EXCEPTION 'Cannot enforce receipts.source_channel check while invalid values exist';
    END IF;
END $$;

ALTER TABLE receipt_items
    ALTER COLUMN expected_qty TYPE INTEGER
    USING expected_qty::INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'receipt_items_expected_qty_positive'
    ) THEN
        ALTER TABLE receipt_items
            ADD CONSTRAINT receipt_items_expected_qty_positive
            CHECK (expected_qty > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'receipts_source_channel_check'
    ) THEN
        ALTER TABLE receipts
            ADD CONSTRAINT receipts_source_channel_check
            CHECK (source_channel IS NULL OR source_channel IN ('ZALO', 'EMAIL'));
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_receipts_purchase_supplier_warehouse_source
    ON receipts (supplier_id, warehouse_id, source_order_code)
    WHERE type = 'PURCHASE'
      AND status <> 'REJECTED'
      AND source_order_code IS NOT NULL;

CREATE TABLE IF NOT EXISTS document_sequences (
    sequence_key VARCHAR(50) PRIMARY KEY,
    next_value BIGINT NOT NULL CHECK (next_value > 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO document_sequences (sequence_key, next_value)
VALUES ('RECEIPT', 1)
ON CONFLICT (sequence_key) DO NOTHING;
