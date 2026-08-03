ALTER TABLE outbound_qc_records
    ADD COLUMN inventory_moved_at TIMESTAMPTZ;

-- Records created before this migration already moved inventory during Staff submission.
UPDATE outbound_qc_records
SET inventory_moved_at = created_at;
