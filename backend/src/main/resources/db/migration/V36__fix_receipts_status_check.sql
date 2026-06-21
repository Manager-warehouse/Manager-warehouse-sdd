-- V36__fix_receipts_status_check.sql
-- Originally from another branch; applied to shared DB by that branch.
-- Running out-of-order here could break existing receipt status constraints.
-- This migration is intentionally a no-op.
SELECT 1;
