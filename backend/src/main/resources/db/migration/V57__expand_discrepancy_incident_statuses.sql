-- V57: mở rộng trạng thái xử lý hồ sơ chênh lệch điều chuyển.
-- CEO có thể kết luận lỗi thuộc kho nguồn, kho đích, vận chuyển hoặc hướng xử lý cũ.

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
          AND rel.relname = 'discrepancy_incidents'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE discrepancy_incidents DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE discrepancy_incidents
    ADD CONSTRAINT discrepancy_incidents_status_check
    CHECK (status IN (
        'OPEN',
        'RESOLVED_ACCEPTED',
        'RESOLVED_RETURNED',
        'RESOLVED_CARRIER_FAULT',
        'RESOLVED_SOURCE_FAULT',
        'RESOLVED_DESTINATION_COUNT_ERROR'
    ));
