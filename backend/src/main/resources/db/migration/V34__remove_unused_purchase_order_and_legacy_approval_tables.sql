-- Remove tables that have no active application flow or persisted business data.
-- Guard against accidental data loss if an external client writes to them before deployment.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM purchase_order_items)
        OR EXISTS (SELECT 1 FROM purchase_orders)
        OR EXISTS (SELECT 1 FROM delivery_order_approvals) THEN
        RAISE EXCEPTION
            'Cannot remove unused legacy tables because business data is present';
    END IF;
END $$;

-- Drop the dependent purchase-order table first. Do not use CASCADE: unexpected
-- foreign-key dependencies must fail this migration instead of being removed.
DROP TABLE purchase_order_items;
DROP TABLE purchase_orders;
DROP TABLE delivery_order_approvals;
