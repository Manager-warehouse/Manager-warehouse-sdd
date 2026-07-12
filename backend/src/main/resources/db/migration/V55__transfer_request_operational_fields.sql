ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS needed_by_date DATE;
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS business_reason TEXT;
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS converted_transfer_id BIGINT REFERENCES inter_warehouse_transfers(id);
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS converted_by BIGINT REFERENCES users(id);
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS converted_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS ux_transfer_requests_converted_transfer
    ON transfer_requests(converted_transfer_id)
    WHERE converted_transfer_id IS NOT NULL;
