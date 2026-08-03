ALTER TABLE returned_delivery_flow_items
    ADD COLUMN shortage_qty NUMERIC(10, 2),
    ADD COLUMN shortage_reason TEXT;

UPDATE returned_delivery_flow_items
SET shortage_qty = GREATEST(expected_qty - actual_qty, 0),
    shortage_reason = CASE
        WHEN expected_qty > actual_qty
            THEN 'Legacy record: shortage reason was not captured'
        ELSE NULL
    END
WHERE actual_qty IS NOT NULL;

ALTER TABLE returned_delivery_flow_items
    ADD CONSTRAINT chk_returned_flow_item_shortage_non_negative
        CHECK (shortage_qty IS NULL OR shortage_qty >= 0),
    ADD CONSTRAINT chk_returned_flow_item_shortage_matches_actual
        CHECK (actual_qty IS NULL OR shortage_qty = GREATEST(expected_qty - actual_qty, 0)),
    ADD CONSTRAINT chk_returned_flow_item_shortage_reason
        CHECK (shortage_qty IS NULL OR shortage_qty = 0 OR NULLIF(BTRIM(shortage_reason), '') IS NOT NULL);

ALTER TABLE adjustments DROP CONSTRAINT IF EXISTS adjustments_type_check;

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
