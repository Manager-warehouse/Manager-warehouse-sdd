-- Preserve item-level traceability for automatic outbound invoices.

CREATE TABLE IF NOT EXISTS invoice_lines (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    do_item_id BIGINT NOT NULL REFERENCES delivery_order_items(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity DECIMAL(10,2) NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(18,2) NOT NULL CHECK (unit_price >= 0),
    line_amount DECIMAL(18,2) NOT NULL CHECK (line_amount >= 0),
    CONSTRAINT ux_invoice_lines_invoice_do_item UNIQUE (invoice_id, do_item_id)
);

CREATE INDEX IF NOT EXISTS idx_invoice_lines_invoice_id ON invoice_lines(invoice_id);

DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = current_schema()
          AND rel.relname = 'audit_logs'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%action%'
    LOOP
        EXECUTE format('ALTER TABLE audit_logs DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE audit_logs
    ADD CONSTRAINT chk_audit_logs_action
        CHECK (action IN (
            'LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'STATUS_CHANGE',
            'APPROVE', 'REJECT', 'CANCEL', 'SOFT_DELETE', 'ASSIGN',
            'UNASSIGN', 'UPLOAD_POD', 'REQUEST_OTP', 'CONFIRM_DELIVERY',
            'RESET_DELIVERY_OTP', 'FAIL_DELIVERY', 'TRIP_CREATE',
            'TRIP_UPDATE', 'TRIP_CANCEL', 'TRIP_DEPART',
            'DELIVERY_ATTEMPT_CREATE', 'COMPLETE_TRIP',
            'PICKING_PLAN_SAVE', 'PICKED_GOODS_RETURN_TO_BIN',
            'PICKING_REPLACEMENT_SAVE', 'DELIVERY_ORDER_PICK_COMPLETE',
            'OUTBOUND_QC_FAIL_QUARANTINE', 'DELIVERY_ORDER_QC_APPROVE',
            'DELIVERY_ORDER_WAREHOUSE_APPROVE', 'DELIVERY_ORDER_WAREHOUSE_REJECT',
            'INVOICE_AUTO_CREATE'
        ));
