-- Spec 007 follow-up: add missing unit_price snapshot column and extend
-- audit_logs_action_check to include PRICE_* actions.
--
-- NOTE: end_date NOT NULL enforcement is intentionally skipped (see V22 comment:
-- "skip enforcing NOT NULL at DB level to avoid breaking existing rows").
-- The entity layer enforces this via @Column(nullable = false) and DTO validation.

-- 1. Add unit_price snapshot column to delivery_order_items (spec 007, section 6)
--    unit_cost was added in V22; unit_price was missed.
ALTER TABLE delivery_order_items




-- 2. Extend audit_logs_action_check to include PRICE_* actions (spec 007).
--    V21 created the constraint without these values; drop and recreate it.
