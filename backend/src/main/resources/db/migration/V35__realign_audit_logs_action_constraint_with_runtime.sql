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
    ADD CONSTRAINT audit_logs_action_check
        CHECK (action IN (
            'LOGIN',
            'LOGOUT',
            'CREATE',
            'UPDATE',
            'STATUS_CHANGE',
            'APPROVE',
            'REJECT',
            'CANCEL',
            'SOFT_DELETE',
            'ASSIGN',
            'UNASSIGN',
            'UPLOAD_POD',
            'REQUEST_OTP',
            'CONFIRM_DELIVERY',
            'RESET_DELIVERY_OTP',
            'FAIL_DELIVERY',
            'TRIP_CREATE',
            'TRIP_UPDATE',
            'TRIP_CANCEL',
            'TRIP_DEPART',
            'DELIVERY_ATTEMPT_CREATE',
            'COMPLETE_TRIP',
            'PICKING_PLAN_SAVE',
            'PICKED_GOODS_RETURN_TO_BIN',
            'PICKING_REPLACEMENT_SAVE',
            'DELIVERY_ORDER_PICK_COMPLETE',
            'OUTBOUND_QC_FAIL_QUARANTINE',
            'DELIVERY_ORDER_QC_APPROVE',
            'DELIVERY_ORDER_WAREHOUSE_APPROVE',
            'DELIVERY_ORDER_WAREHOUSE_REJECT',
            'INVOICE_AUTO_CREATE'
        ));
