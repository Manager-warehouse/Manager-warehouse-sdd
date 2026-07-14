-- Seed accountant users for Spec 008 testing
-- Default password for all users is: password123
INSERT INTO users (code, full_name, email, phone, password_hash, role, job_title, shift, region, is_active)
VALUES
    ('NV-005', 'Nguyễn Kế Toán', 'accountant@phucanh.vn', '0911222333', '$2a$12$zA4J/HnTBBwyUZeMqlVRhulkNPNQMRrAUk/mFXFYYJ2B9JwyVOMRC', 'ACCOUNTANT', 'Kế toán viên', 'Hành chính', 'Toàn quốc', true),
    ('NV-006', 'Phạm Kế Toán Trưởng', 'acc_manager@phucanh.vn', '0922333444', '$2a$12$zA4J/HnTBBwyUZeMqlVRhulkNPNQMRrAUk/mFXFYYJ2B9JwyVOMRC', 'ACCOUNTANT_MANAGER', 'Kế toán trưởng', 'Hành chính', 'Toàn quốc', true)
ON CONFLICT (email) DO NOTHING;

-- Assign warehouses (WH-HP, WH-HN, WH-HCM) to the accountants
INSERT INTO user_warehouse_assignments (user_id, warehouse_id, assigned_by)
SELECT u.id, w.id, (SELECT id FROM users WHERE code = 'NV-001')
FROM users u, warehouses w
WHERE u.code IN ('NV-005', 'NV-006') AND w.code IN ('WH-HP', 'WH-HN', 'WH-HCM')
ON CONFLICT (user_id, warehouse_id) DO NOTHING;
