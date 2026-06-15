-- Align internal transfer schema with Sprint 1 transfer planning rules.
-- Planner enters external HQ instructions; source warehouse manager approves or rejects.

ALTER TABLE transfers
    ADD COLUMN IF NOT EXISTS external_instruction_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS rejected_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS trip_id BIGINT REFERENCES trips(id),
    ADD COLUMN IF NOT EXISTS notes TEXT;

ALTER TABLE transfers
    ALTER COLUMN status SET DEFAULT 'NEW';

ALTER TABLE transfers
    DROP CONSTRAINT IF EXISTS transfers_status_check;

ALTER TABLE transfers
    ADD CONSTRAINT transfers_status_check
        CHECK (status IN (
            'NEW',
            'APPROVED',
            'REJECTED',
            'IN_TRANSIT',
            'COMPLETED',
            'COMPLETED_WITH_DISCREPANCY',
            'CANCELLED'
        ));

CREATE UNIQUE INDEX IF NOT EXISTS ux_transfers_trip_id
    ON transfers(trip_id)
    WHERE trip_id IS NOT NULL;

ALTER TABLE transfer_items
    ALTER COLUMN batch_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS qc_passed_qty DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS qc_failed_qty DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS qc_result VARCHAR(20),
    ADD COLUMN IF NOT EXISTS qc_failure_reason TEXT;

ALTER TABLE transfer_items
    DROP CONSTRAINT IF EXISTS transfer_items_qc_result_check;

ALTER TABLE transfer_items
    ADD CONSTRAINT transfer_items_qc_result_check
        CHECK (qc_result IS NULL OR qc_result IN ('PENDING', 'PASSED', 'FAILED', 'PARTIAL'));

ALTER TABLE trips
    DROP CONSTRAINT IF EXISTS trips_status_check;

ALTER TABLE trips
    ADD CONSTRAINT trips_status_check
        CHECK (status IN ('PLANNED', 'IN_TRANSIT', 'COMPLETED', 'CANCELLED'));
