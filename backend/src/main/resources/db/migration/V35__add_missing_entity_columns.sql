-- V35: Add all missing columns to fix Hibernate schema-validation failures
-- transfers: missing external_instruction_code, rejected_by, rejected_at, rejection_reason, trip_id, notes
ALTER TABLE transfers
    ADD COLUMN IF NOT EXISTS external_instruction_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS rejected_by               BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS rejected_at               TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason          TEXT,
    ADD COLUMN IF NOT EXISTS trip_id                   BIGINT REFERENCES trips(id),
    ADD COLUMN IF NOT EXISTS notes                     TEXT;

-- delivery_order_items: missing serial_number (already exists in DB but just in case)
ALTER TABLE delivery_order_items
    ADD COLUMN IF NOT EXISTS serial_number VARCHAR(100);
