-- V33: Add missing QC columns to transfer_items
-- These columns were defined in V18 but not applied to production DB
ALTER TABLE transfer_items
    ADD COLUMN IF NOT EXISTS qc_passed_qty DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS qc_failed_qty DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS qc_result VARCHAR(20),
    ADD COLUMN IF NOT EXISTS qc_failure_reason TEXT;
