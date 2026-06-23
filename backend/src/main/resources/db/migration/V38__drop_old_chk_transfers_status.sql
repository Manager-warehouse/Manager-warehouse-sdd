-- Drop the duplicate and restrictive chk_transfers_status constraint from V34
ALTER TABLE transfers DROP CONSTRAINT IF EXISTS chk_transfers_status;
