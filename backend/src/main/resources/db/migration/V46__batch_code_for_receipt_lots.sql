ALTER TABLE batches ADD COLUMN IF NOT EXISTS batch_code VARCHAR(100);

UPDATE batches
SET batch_code = batch_number
WHERE batch_code IS NULL;

ALTER TABLE batches ALTER COLUMN batch_code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_batches_batch_code ON batches(batch_code);
CREATE INDEX IF NOT EXISTS idx_batches_warehouse_received_date ON batches(warehouse_id, received_date);
