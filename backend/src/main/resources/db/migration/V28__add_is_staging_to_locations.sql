-- V28__add_is_staging_to_locations.sql
-- Add is_staging boolean flag to warehouse_locations for outbound QC staging differentiation

ALTER TABLE warehouse_locations
ADD COLUMN is_staging BOOLEAN NOT NULL DEFAULT FALSE;
