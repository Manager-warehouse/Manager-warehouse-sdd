-- V32__update_audit_logs_stocktake_actions.sql
-- Originally intended to extend audit_logs_action_check with STOCKTAKE_* actions.
-- This file exists in target/classes from another branch build; the actual changes
-- were applied to the shared DB by that branch. Running it out-of-order here would
-- drop the V38 constraint and re-add a narrower one, violating existing rows.
-- This migration is intentionally a no-op.
SELECT 1;
