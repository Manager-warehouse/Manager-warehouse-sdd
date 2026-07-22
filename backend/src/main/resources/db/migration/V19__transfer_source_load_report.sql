-- V19__transfer_source_load_report.sql
-- Add source worker loaded-quantity reporting before outbound QC for Spec 005.

ALTER TABLE inter_warehouse_transfers
    ADD COLUMN IF NOT EXISTS source_loaded_reported_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS source_loaded_reported_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS source_load_rework_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS source_load_rework_reason TEXT;

ALTER TABLE inter_warehouse_transfer_items
    ADD COLUMN IF NOT EXISTS loaded_qty NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS loaded_reported_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS loaded_reported_at TIMESTAMPTZ;
