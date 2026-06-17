-- Remove legacy unit-level QC split and single serial from inbound receipt items.

ALTER TABLE receipt_items
    DROP COLUMN IF EXISTS qc_passed_qty,
    DROP COLUMN IF EXISTS qc_failed_qty,
    DROP COLUMN IF EXISTS serial_number;
