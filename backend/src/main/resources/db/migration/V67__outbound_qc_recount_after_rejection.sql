ALTER TABLE outbound_qc_records
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS rejected_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_outbound_qc_records_active_do
    ON outbound_qc_records(do_id, is_active);

CREATE UNIQUE INDEX IF NOT EXISTS uq_outbound_qc_records_active_allocation
    ON outbound_qc_records(allocation_id)
    WHERE is_active = TRUE;
