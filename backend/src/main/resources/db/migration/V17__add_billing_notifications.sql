-- Add billing notifications for Spec 004 US-WMS-10.
-- Spec 004 emits the notification when a DO becomes DELIVERED.
-- Spec 008 consumes it when invoice creation moves the DO to COMPLETED.

DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT c.conname
    INTO constraint_name
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    WHERE n.nspname = 'public'
      AND t.relname = 'delivery_orders'
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) LIKE '%status%'
      AND pg_get_constraintdef(c.oid) LIKE '%DELIVERED%'
      AND pg_get_constraintdef(c.oid) NOT LIKE '%COMPLETED%'
    LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE delivery_orders DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'public'
          AND t.relname = 'delivery_orders'
          AND c.conname = 'delivery_orders_status_check'
    ) THEN
        ALTER TABLE delivery_orders
            ADD CONSTRAINT delivery_orders_status_check
            CHECK (status IN (
                'NEW',
                'PICKING',
                'READY_TO_SHIP',
                'IN_TRANSIT',
                'DELIVERED',
                'COMPLETED',
                'RETURNED',
                'CANCELLED'
            )) NOT VALID;
    END IF;
END $$;

ALTER TABLE delivery_orders VALIDATE CONSTRAINT delivery_orders_status_check;

CREATE TABLE IF NOT EXISTS billing_notifications (
    id BIGSERIAL PRIMARY KEY,
    do_id BIGINT NOT NULL REFERENCES delivery_orders(id),
    do_number VARCHAR(50) NOT NULL,
    dealer_id BIGINT NOT NULL REFERENCES dealers(id),
    dealer_name VARCHAR(255) NOT NULL,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    delivered_at TIMESTAMPTZ NOT NULL,
    total_amount_estimate DECIMAL(18,2) NOT NULL,
    invoice_status VARCHAR(30) NOT NULL DEFAULT 'NOT_INVOICED'
        CHECK (invoice_status IN ('NOT_INVOICED', 'INVOICED')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'READ', 'ARCHIVED')),
    recipient_role VARCHAR(50) NOT NULL DEFAULT 'ACCOUNTANT',
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_billing_notifications_active_do
    ON billing_notifications(do_id)
    WHERE invoice_status = 'NOT_INVOICED' AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_billing_notifications_status_created
    ON billing_notifications(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_billing_notifications_invoice_status
    ON billing_notifications(invoice_status);

CREATE INDEX IF NOT EXISTS idx_billing_notifications_warehouse
    ON billing_notifications(warehouse_id);

CREATE INDEX IF NOT EXISTS idx_billing_notifications_dealer
    ON billing_notifications(dealer_id);
