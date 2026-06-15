-- Seed mock users for authentication and authorization matching frontend constants
-- Default password for all users is: password123

INSERT INTO users (code, full_name, email, phone, password_hash, role, job_title, shift, region, is_active)
VALUES
    ('NV-001', 'Anh Phương Đẹp Trai', 'admin@phucanh.vn', '0912345678', '$2a$12$zA4J/HnTBBwyUZeMqlVRhulkNPNQMRrAUk/mFXFYYJ2B9JwyVOMRC', 'ADMIN', 'Quản trị hệ thống', 'Hành chính', 'Toàn quốc', true),
    ('NV-002', 'Trần Phúc Anh', 'ceo@phucanh.vn', '0987654321', '$2a$12$zA4J/HnTBBwyUZeMqlVRhulkNPNQMRrAUk/mFXFYYJ2B9JwyVOMRC', 'CEO', 'Giám đốc điều hành', 'Hành chính', 'Toàn quốc', true),
    ('NV-003', 'Lê Văn Trưởng Kho', 'manager.hp@phucanh.vn', '0901234567', '$2a$12$zA4J/HnTBBwyUZeMqlVRhulkNPNQMRrAUk/mFXFYYJ2B9JwyVOMRC', 'WAREHOUSE_MANAGER', 'Trưởng kho Hải Phòng', 'Ca sáng', 'Hải Phòng', true),
    ('NV-004', 'Phạm Thủ Kho', 'keeper.hn@phucanh.vn', '0934567890', '$2a$12$zA4J/HnTBBwyUZeMqlVRhulkNPNQMRrAUk/mFXFYYJ2B9JwyVOMRC', 'STOREKEEPER', 'Thủ kho Hà Nội', 'Ca chiều', 'Hà Nội', true)
ON CONFLICT (email) DO NOTHING;

-- Assign warehouses to users
-- Gán kho cho CEO (Trần Phúc Anh)
INSERT INTO user_warehouse_assignments (user_id, warehouse_id, assigned_by)
SELECT u.id, w.id, (SELECT id FROM users WHERE code = 'NV-001')
FROM users u, warehouses w
WHERE u.code = 'NV-002' AND w.code IN ('WH-HP', 'WH-HN', 'WH-HCM')
ON CONFLICT (user_id, warehouse_id) DO NOTHING;

-- Gán kho cho Trưởng kho (Lê Văn Trưởng Kho)
INSERT INTO user_warehouse_assignments (user_id, warehouse_id, assigned_by)
SELECT u.id, w.id, (SELECT id FROM users WHERE code = 'NV-001')
FROM users u, warehouses w
WHERE u.code = 'NV-003' AND w.code = 'WH-HP'
ON CONFLICT (user_id, warehouse_id) DO NOTHING;

-- Gán kho cho Thủ kho (Phạm Thủ Kho)
INSERT INTO user_warehouse_assignments (user_id, warehouse_id, assigned_by)
SELECT u.id, w.id, (SELECT id FROM users WHERE code = 'NV-001')
FROM users u, warehouses w
WHERE u.code = 'NV-004' AND w.code = 'WH-HN'
ON CONFLICT (user_id, warehouse_id) DO NOTHING;
