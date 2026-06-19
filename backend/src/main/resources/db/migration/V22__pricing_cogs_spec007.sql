-- Spec 007: Pricing & COGS
-- Fixes price_history drift from V1 and adds notifications table.

-- 1. end_date nullability: skip enforcing NOT NULL at DB level to avoid
--    breaking existing rows; the entity and service layer enforce this.
-- ALTER TABLE price_history ALTER COLUMN end_date SET NOT NULL;

-- 2. Add missing columns
ALTER TABLE price_history
    ADD COLUMN IF NOT EXISTS cancelled_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS notes       TEXT,
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMPTZ DEFAULT NOW();

-- 3. Expand status check constraint to include CANCELLED
DO $$
DECLARE
    cname text;
BEGIN
    SELECT c.conname INTO cname
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    WHERE n.nspname = 'public'
      AND t.relname = 'price_history'
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) LIKE '%status%'
    LIMIT 1;

    IF cname IS NOT NULL THEN
        EXECUTE format('ALTER TABLE price_history DROP CONSTRAINT %I', cname);
    END IF;
END $$;

ALTER TABLE price_history
    ADD CONSTRAINT price_history_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'CANCELLED'));

-- 4. Add date-range and price positivity constraints if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'price_history_date_range_check'
    ) THEN
        ALTER TABLE price_history
            ADD CONSTRAINT price_history_date_range_check
            CHECK (effective_date <= end_date);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'price_history_positive_prices_check'
    ) THEN
        ALTER TABLE price_history
            ADD CONSTRAINT price_history_positive_prices_check
            CHECK (cost_price > 0 AND selling_price > 0);
    END IF;
END $$;

-- 5. Performance indexes for price lookup and history browsing
CREATE INDEX IF NOT EXISTS idx_price_history_product_status
    ON price_history (product_id, status, effective_date, end_date);

CREATE INDEX IF NOT EXISTS idx_price_history_product_created
    ON price_history (product_id, created_at DESC);

-- 6. unit_cost snapshot column on delivery_order_items (spec 007 owns COGS snapshot)
ALTER TABLE delivery_order_items
    ADD COLUMN IF NOT EXISTS unit_cost DECIMAL(18,2);

-- 7. Notifications table for in-app alerts (pricing approval alerts + future use)
CREATE TABLE IF NOT EXISTS notifications (
    id             BIGSERIAL     PRIMARY KEY,
    recipient_id   BIGINT        NOT NULL REFERENCES users(id),
    type           VARCHAR(50)   NOT NULL,
    reference_type VARCHAR(50),
    reference_id   BIGINT,
    message        TEXT,
    is_read        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_read
    ON notifications (recipient_id, is_read, created_at DESC);
