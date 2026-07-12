-- Spec 007 (Session 2026-07-09): price_history moves to an effective-date-only
-- model. A price stays in effect from effective_date until a later APPROVED
-- entry for the same (product_id, warehouse_id) supersedes it, so end_date is
-- no longer needed and the old row never has to be edited to make room for a
-- correction.
ALTER TABLE price_history DROP COLUMN end_date;
