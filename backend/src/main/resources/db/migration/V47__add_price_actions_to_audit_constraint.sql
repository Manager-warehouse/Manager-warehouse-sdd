-- V47__add_price_actions_to_audit_constraint.sql
-- Originally intended to extend audit_logs_action_check with PRICE_* actions.
-- DB reached V38 via other branches; V38 already contains a complete constraint.
-- Dropping and re-adding here would replace V38's constraint with a narrower one.
-- This migration is intentionally a no-op.
SELECT 1;
