-- =============================================================================
-- SCRIPT TRUNCATE DỮ LIỆU & GIỮ NGUYÊN TÀI KHOẢN ADMIN (WMS DATABASE RESET)
-- =============================================================================
-- Script này thực hiện:
-- 1. Lưu tạm các tài khoản có role = 'ADMIN' hiện tại
-- 2. Truncate toàn bộ các bảng trong schema 'public' (trừ flyway_schema_history)
-- 3. Khôi phục lại các tài khoản Admin đã lưu và đồng bộ sequence của ID
-- 4. Re-seed dữ liệu khởi tạo thiết yếu (Warehouses, Configs, Accounting Period)
-- =============================================================================

DO $$
DECLARE
    tbl_record RECORD;
    stmt TEXT;
BEGIN
    -- 1. Lưu tạm tài khoản ADMIN mặc định (admin@phucanh.vn) hiện có trước khi truncate
    CREATE TEMP TABLE temp_admin_users AS 
    SELECT * FROM users WHERE role = 'ADMIN' AND email = 'admin@phucanh.vn';

    -- 2. Lặp và Truncate toàn bộ bảng thuộc schema public (ngoại trừ flyway_schema_history)
    FOR tbl_record IN
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public' 
          AND tablename <> 'flyway_schema_history'
    LOOP
        stmt := format('TRUNCATE TABLE public.%I RESTART IDENTITY CASCADE;', tbl_record.tablename);
        EXECUTE stmt;
        RAISE NOTICE 'Truncated table: %', tbl_record.tablename;
    END LOOP;

    -- 3. Khôi phục lại tài khoản Admin (admin@phucanh.vn) đã lưu tạm
    IF EXISTS (SELECT 1 FROM temp_admin_users) THEN
        INSERT INTO users (id, code, full_name, email, phone, password_hash, role, job_title, shift, region, otp_hash, otp_expires_at, refresh_token_hash, refresh_token_expires_at, is_active, created_at, updated_at)
        SELECT id, code, full_name, email, phone, password_hash, role, job_title, shift, region, otp_hash, otp_expires_at, refresh_token_hash, refresh_token_expires_at, is_active, created_at, updated_at
        FROM temp_admin_users;

        -- Đặt lại sequence ID của bảng users theo MAX(id) hiện có
        PERFORM setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
        RAISE NOTICE 'Restored existing admin@phucanh.vn user.';
    ELSE
        -- Nếu chưa có Admin nào trong DB, seed tài khoản Admin mặc định (Mật khẩu: Admin@123)
        INSERT INTO users (code, full_name, email, phone, password_hash, role, job_title, shift, region, is_active)
        VALUES ('NV-001', 'Hệ Thống Admin', 'admin@phucanh.vn', '0900000001', '$2a$12$zA4J/HnTBBwyUZeMqlVRhulkNPNQMRrAUk/mFXFYYJ2B9JwyVOMRC', 'ADMIN', 'Quản trị viên', 'Hành chính', 'Toàn quốc', true);
        RAISE NOTICE 'Seeded default ADMIN user.';
    END IF;
END $$;

-- =============================================================================
-- RE-SEED CORE MASTER DATA CẦN THIẾT DÀNH CHO CÁC MODULE KHÁC
-- =============================================================================

-- 1. Kho vật lý & Kho ảo In-Transit
INSERT INTO warehouses (code, name, type, address, phone) VALUES
    ('WH-HP',      'Kho Hải Phòng',        'PHYSICAL',   'Hải Phòng, Việt Nam',        '0225-000001'),
    ('WH-HN',      'Kho Hà Nội',           'PHYSICAL',   'Hà Nội, Việt Nam',           '024-000001'),
    ('WH-HCM',     'Kho Hồ Chí Minh',      'PHYSICAL',   'TP. Hồ Chí Minh, Việt Nam',  '028-000001'),
    ('IN_TRANSIT', 'Kho ảo In-Transit',     'IN_TRANSIT', NULL,                         NULL);

-- 2. Kỳ kế toán hiện tại
INSERT INTO accounting_periods (period_name, start_date, end_date, status)
VALUES (
    TO_CHAR(NOW(), 'YYYY-MM'),
    DATE_TRUNC('month', NOW())::DATE,
    (DATE_TRUNC('month', NOW()) + INTERVAL '1 month' - INTERVAL '1 day')::DATE,
    'OPEN'
);

-- 3. Tham số hệ thống
INSERT INTO system_configs (config_key, config_value, description) VALUES
    ('DEFAULT_CREDIT_LIMIT',                 '50000000',  'Hạn mức công nợ mặc định (VNĐ)'),
    ('DEFAULT_PAYMENT_TERM_DAYS',            '30',        'Kỳ hạn thanh toán mặc định (ngày)'),
    ('CREDIT_HOLD_OVERDUE_DAYS',             '30',        'Số ngày quá hạn trước khi khóa tín dụng'),
    ('CREDIT_UNLOCK_BUFFER_PCT',             '0.8',       'Ngưỡng mở khóa tín dụng (80% credit_limit)'),
    ('MONTHLY_CLOSING_DAY',                  '25',        'Ngày khóa sổ kỳ kế toán hàng tháng'),
    ('MIN_INVENTORY_WARNING_THRESHOLD',      '10',        'Ngưỡng cảnh báo tồn kho tối thiểu mặc định');

-- 4. Document Sequences
INSERT INTO document_sequences (sequence_key, next_value)
VALUES ('RECEIPT', 1), ('ST', 1), ('INVOICE', 1), ('PAYMENT', 1)
ON CONFLICT (sequence_key) DO NOTHING;

-- 5. Nhà cung cấp mẫu
INSERT INTO suppliers (code, name, contact_person, phone, email, is_active)
VALUES
    ('SUP-001', 'Công Ty Cung Cấp Chảo Sunhouse', 'Nguyen van B', '0912345678', 'contact@sunhouse.vn', true),
    ('SUP-002', 'Công Ty Thiết Bị Gia Dụng Việt', 'Trần Văn C', '0987654321', 'contact@giadungviet.vn', true)
ON CONFLICT (code) DO NOTHING;



