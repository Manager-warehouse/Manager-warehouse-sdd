-- Add optimistic locking to receipts for concurrent approval/rejection flows.

ALTER TABLE receipts
    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
