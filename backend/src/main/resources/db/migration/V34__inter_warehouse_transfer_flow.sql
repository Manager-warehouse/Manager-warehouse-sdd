ALTER TABLE transfers
    ADD COLUMN IF NOT EXISTS external_instruction_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS rejected_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS trip_id BIGINT UNIQUE REFERENCES trips(id),
    ADD COLUMN IF NOT EXISTS notes TEXT;

UPDATE transfers
SET external_instruction_code = transfer_number
WHERE external_instruction_code IS NULL;

ALTER TABLE transfers
    ALTER COLUMN external_instruction_code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_transfers_external_instruction_active
    ON transfers (external_instruction_code, source_warehouse_id, destination_warehouse_id, document_date)
    WHERE status NOT IN ('REJECTED', 'CANCELLED');

ALTER TABLE transfer_items
    ALTER COLUMN batch_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS worker_received_qty DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS issue_reason TEXT,
    ADD COLUMN IF NOT EXISTS checked_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS checked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS checker_note TEXT,
    ADD COLUMN IF NOT EXISTS qc_passed_qty DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS qc_failed_qty DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS qc_failure_reason TEXT;

CREATE TABLE IF NOT EXISTS transfer_allocations (
    id BIGSERIAL PRIMARY KEY,
    transfer_item_id BIGINT NOT NULL REFERENCES transfer_items(id) ON DELETE CASCADE,
    inventory_id BIGINT NOT NULL REFERENCES inventories(id),
    allocated_qty DECIMAL(10,2) NOT NULL CHECK (allocated_qty > 0),
    UNIQUE (transfer_item_id, inventory_id)
);

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    SELECT con.conname INTO constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    WHERE rel.relname = 'transfers'
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) ILIKE '%status%'
    LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE transfers DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE transfers
    ADD CONSTRAINT chk_transfers_status
        CHECK (status IN (
            'NEW','APPROVED','IN_TRANSIT','COMPLETED',
            'COMPLETED_WITH_DISCREPANCY','REJECTED','CANCELLED'
        ));

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    SELECT con.conname INTO constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    WHERE rel.relname = 'audit_logs'
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) ILIKE '%action%'
    LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE audit_logs DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;


