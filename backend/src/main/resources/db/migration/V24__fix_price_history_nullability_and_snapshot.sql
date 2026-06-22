-- V24__fix_price_history_nullability_and_snapshot.sql
-- Originally intended to add unit_price column to delivery_order_items.
-- File was incomplete (ALTER TABLE statement was never finished).
-- DB reached V38 via other branches; the column was added in V22 (unit_cost)
-- and unit_price is handled there. This migration is intentionally a no-op.
SELECT 1;
