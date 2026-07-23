-- Ensure production databases that already recorded V23 still have the QC photo column.
ALTER TABLE inter_warehouse_transfers
    ADD COLUMN IF NOT EXISTS receive_qc_photo_ref TEXT;
