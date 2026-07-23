-- V23__transfer_receive_qc_photo.sql
-- Store photo evidence for transfer receiving QC/check, including return-to-source receiving.

ALTER TABLE inter_warehouse_transfers
    ADD COLUMN IF NOT EXISTS receive_qc_photo_ref TEXT;
