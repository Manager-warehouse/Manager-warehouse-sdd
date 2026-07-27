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
          AND rel.relname = 'adjustments'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%type%'
    LOOP
        EXECUTE format('ALTER TABLE adjustments DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE adjustments
    ADD CONSTRAINT adjustments_type_check
    CHECK (type IN (
        'STOCK_TAKE',
        'TRANSFER_DISCREPANCY',
        'DISPOSAL',
        'RETURN_TO_VENDOR',
        'CORRECTION_VOUCHER',
        'QC_FAIL_OUTBOUND'
    ));
