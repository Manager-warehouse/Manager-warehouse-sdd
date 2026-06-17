-- Align Driver Mobile POD flow with current SDD.

UPDATE deliveries
SET status = 'IN_TRANSIT'
WHERE status = 'OUT_FOR_DELIVERY';

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
          AND rel.relname = 'deliveries'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE deliveries DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE deliveries
    ADD CONSTRAINT chk_deliveries_status
        CHECK (status IN ('PENDING', 'IN_TRANSIT', 'DELIVERED', 'FAILED', 'RETURNED'));

ALTER TABLE delivery_otp_attempts
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE delivery_otp_attempts
SET status = 'VERIFIED'
WHERE consumed_at IS NOT NULL;

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
          AND rel.relname = 'delivery_otp_attempts'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE delivery_otp_attempts DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE delivery_otp_attempts
    ADD CONSTRAINT chk_delivery_otp_attempts_status
        CHECK (status IN ('ACTIVE', 'VERIFIED', 'EXPIRED'));

COMMENT ON COLUMN delivery_otp_attempts.status IS 'ACTIVE, VERIFIED, or EXPIRED for dealer delivery OTP.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_delivery_otp_attempts_delivery
    ON delivery_otp_attempts(delivery_id);

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
            'RESET_DELIVERY_OTP', 'FAIL_DELIVERY', 'COMPLETE_TRIP'
        ));
