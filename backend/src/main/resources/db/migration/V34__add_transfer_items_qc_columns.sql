-- V34: Add remaining missing QC columns to transfer_items
ALTER TABLE transfer_items
    ADD COLUMN IF NOT EXISTS qc_passed_qty DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS qc_result VARCHAR(20),
    ADD COLUMN IF NOT EXISTS qc_failure_reason TEXT;
