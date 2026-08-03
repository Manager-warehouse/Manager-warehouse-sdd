ALTER TABLE outbound_qc_records
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN rejected_by BIGINT REFERENCES users(id),
    ADD COLUMN rejected_at TIMESTAMPTZ,
    ADD COLUMN rejection_reason TEXT;

CREATE INDEX idx_outbound_qc_records_active_do
    ON outbound_qc_records(do_id, is_active);

CREATE UNIQUE INDEX uq_outbound_qc_records_active_allocation
    ON outbound_qc_records(allocation_id)
    WHERE is_active = TRUE;
