ALTER TABLE returned_delivery_flow_items
    ADD COLUMN IF NOT EXISTS shortage_qty NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS shortage_reason TEXT;

UPDATE returned_delivery_flow_items
SET shortage_qty = GREATEST(expected_qty - actual_qty, 0),
    shortage_reason = CASE
        WHEN expected_qty > actual_qty
            THEN 'Legacy record: shortage reason was not captured'
        ELSE NULL
    END
WHERE actual_qty IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'returned_delivery_flow_items'::regclass
          AND conname = 'chk_returned_flow_item_shortage_non_negative'
    ) THEN
        ALTER TABLE returned_delivery_flow_items
            ADD CONSTRAINT chk_returned_flow_item_shortage_non_negative
            CHECK (shortage_qty IS NULL OR shortage_qty >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'returned_delivery_flow_items'::regclass
          AND conname = 'chk_returned_flow_item_shortage_matches_actual'
    ) THEN
        ALTER TABLE returned_delivery_flow_items
            ADD CONSTRAINT chk_returned_flow_item_shortage_matches_actual
            CHECK (actual_qty IS NULL OR shortage_qty = GREATEST(expected_qty - actual_qty, 0));
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'returned_delivery_flow_items'::regclass
          AND conname = 'chk_returned_flow_item_shortage_reason'
    ) THEN
        ALTER TABLE returned_delivery_flow_items
            ADD CONSTRAINT chk_returned_flow_item_shortage_reason
            CHECK (shortage_qty IS NULL OR shortage_qty = 0 OR NULLIF(BTRIM(shortage_reason), '') IS NOT NULL);
    END IF;
END $$;

ALTER TABLE adjustments DROP CONSTRAINT IF EXISTS adjustments_type_check;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'adjustments'::regclass
          AND conname = 'adjustments_type_check'
    ) THEN
        ALTER TABLE adjustments
            ADD CONSTRAINT adjustments_type_check
            CHECK (type IN (
                'STOCK_TAKE',
                'TRANSFER_DISCREPANCY',
                'DISPOSAL',
                'RETURN_TO_VENDOR',
                'CORRECTION_VOUCHER',
                'QC_FAIL_OUTBOUND',
                'RETURN_SHORTAGE'
            ));
    END IF;
END $$;
