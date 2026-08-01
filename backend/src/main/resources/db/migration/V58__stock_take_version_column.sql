-- Add optimistic locking version column to stock_takes
ALTER TABLE stock_takes ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
