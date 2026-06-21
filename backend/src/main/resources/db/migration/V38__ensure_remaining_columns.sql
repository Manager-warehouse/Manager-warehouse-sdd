-- V38: Ensure all columns that V23/V35 were supposed to add but may not have run

-- delivery_order_items
ALTER TABLE delivery_order_items
    ADD COLUMN IF NOT EXISTS serial_number VARCHAR(100);

-- transfers
ALTER TABLE transfers
    ADD COLUMN IF NOT EXISTS external_instruction_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS rejected_by               BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS rejected_at               TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason          TEXT,
    ADD COLUMN IF NOT EXISTS trip_id                   BIGINT REFERENCES trips(id),
    ADD COLUMN IF NOT EXISTS notes                     TEXT;
