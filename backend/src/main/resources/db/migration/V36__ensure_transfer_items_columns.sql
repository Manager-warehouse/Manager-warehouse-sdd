-- V36: Ensure all transfer_items QC columns exist (V33 may have been skipped)
ALTER TABLE transfer_items
    ADD COLUMN IF NOT EXISTS qc_passed_qty     DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS qc_failed_qty     DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS qc_result         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS qc_failure_reason TEXT;
