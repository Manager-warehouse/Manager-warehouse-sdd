-- =============================================================================
-- UPDATE DELIVERY ORDERS STATUS CHECK CONSTRAINT
-- =============================================================================

-- 1. Drop existing status check constraint if it exists (handles explicit name)
ALTER TABLE delivery_orders
    DROP CONSTRAINT IF EXISTS delivery_orders_status_check;

-- 2. Drop any auto-generated status check constraints on delivery_orders table
DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT c.conname
    INTO constraint_name
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    WHERE n.nspname = current_schema()
      AND t.relname = 'delivery_orders'
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) LIKE '%status%'
    LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE delivery_orders DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

-- 3. Add updated status check constraint matching DeliveryOrderStatus enum
ALTER TABLE delivery_orders
    ADD CONSTRAINT delivery_orders_status_check
    CHECK (status IN (
        'NEW',
        'PICKING_PLANNED',
        'WAITING_PICKING',
        'PICKING',
        'QC_PENDING_APPROVAL',
        'QC_COMPLETED',
        'WAREHOUSE_APPROVED',
        'IN_TRANSIT',
        'DELIVERED',
        'RETURNED',
        'DELIVERY_FAILED',
        'COMPLETED',
        'CLOSED',
        'REJECTED',
        'CANCELLED'
    ));
