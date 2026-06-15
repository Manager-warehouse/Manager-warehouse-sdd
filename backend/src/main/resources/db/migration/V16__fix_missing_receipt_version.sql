-- Fix missing version column on receipts table to resolve JPA startup validation errors.
ALTER TABLE receipts
    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
