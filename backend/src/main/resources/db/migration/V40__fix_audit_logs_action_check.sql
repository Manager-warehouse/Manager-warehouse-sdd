-- V34__fix_audit_logs_action_check.sql
-- Originally intended to fix audit_logs_action_check constraint.
-- Superseded: DB reached V37 via other branches before this ran out-of-order.
-- V37 already contains a complete constraint. This migration is intentionally a no-op
-- to avoid dropping the V37 constraint and re-adding a narrower version.
SELECT 1;
