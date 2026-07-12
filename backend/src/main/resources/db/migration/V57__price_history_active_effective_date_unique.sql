-- Spec 007 (Session 2026-07-12): the conflicting-effective-date check
-- (PENDING or APPROVED cannot share a (product_id, warehouse_id, effective_date))
-- was only enforced in application code via a SELECT-then-INSERT check, which
-- leaves a race window between two concurrent creates. A partial unique index
-- makes the invariant atomic at the database level; CANCELLED rows are excluded
-- since a cancelled entry never blocks reuse of its date.

-- Data existing before this constraint predates the rule and can already violate
-- it (duplicate PENDING/APPROVED rows for the same product/warehouse/date). Do
-- not delete them — cancel every duplicate except the one worth keeping per
-- group (an APPROVED row over a PENDING one; otherwise the most recently
-- created), so the unique index below can be created and no business history is
-- physically removed.
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY product_id, warehouse_id, effective_date
               ORDER BY (status = 'APPROVED') DESC, created_at DESC, id DESC
           ) AS rn
    FROM price_history
    WHERE status <> 'CANCELLED'
)
UPDATE price_history
SET status = 'CANCELLED',
    cancelled_at = NOW(),
    updated_at = NOW()
WHERE id IN (SELECT id FROM ranked WHERE rn > 1);

CREATE UNIQUE INDEX uq_price_history_active_effective_date
    ON price_history (product_id, warehouse_id, effective_date)
    WHERE status <> 'CANCELLED';
