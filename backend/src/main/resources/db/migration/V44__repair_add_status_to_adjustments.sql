-- =============================================================================
-- V44: Repair — ensure adjustments.status column exists
-- =============================================================================
-- Context:
--   V43 (add_adjustment_status) added this column, but production environments
--   that were restored from a pre-V43 snapshot report Flyway version = 43 yet
--   the column is absent, causing Hibernate schema-validation to fail at startup.
--   This migration is a safe, idempotent repair that re-applies the column DDL
--   without modifying any already-applied migration file.
--
-- Safety:
--   • ADD COLUMN IF NOT EXISTS — no-op when the column already exists.
--   • DROP CONSTRAINT IF EXISTS before re-adding — avoids duplicate-constraint error
--     if V43 partially succeeded (column absent but constraint registered).
--   • Backfills existing rows before adding NOT NULL constraint.
-- =============================================================================

-- Step 1: Add column (idempotent)
ALTER TABLE adjustments
    ADD COLUMN IF NOT EXISTS status VARCHAR(30);

-- Step 2: Backfill existing rows so NOT NULL can be applied
UPDATE adjustments
SET status = CASE
    WHEN approved_at IS NOT NULL THEN 'APPROVED'
    ELSE 'PENDING_APPROVAL'
END
WHERE status IS NULL;

-- Step 3: Enforce NOT NULL and set server-side default
ALTER TABLE adjustments
    ALTER COLUMN status SET DEFAULT 'PENDING_APPROVAL',
    ALTER COLUMN status SET NOT NULL;

-- Step 4: Add CHECK constraint (idempotent — drop first if already registered)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   pg_constraint c
        JOIN   pg_class      t ON t.oid = c.conrelid
        WHERE  t.relname = 'adjustments'
          AND  c.conname = 'chk_adjustments_status'
    ) THEN
        ALTER TABLE adjustments
            ADD CONSTRAINT chk_adjustments_status
            CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED'));
    END IF;
END
$$;
