-- Seed document sequences for invoices and payments (Spec 008)
INSERT INTO document_sequences (sequence_key, next_value)
VALUES ('INVOICE', 1), ('PAYMENT', 1)
ON CONFLICT (sequence_key) DO NOTHING;

-- Seed admin user NV-001 (if not exists)
INSERT INTO users (code, full_name, email, phone, password_hash, role, job_title, shift, region, is_active)
VALUES
    ('NV-001', 'Hệ Thống Admin', 'admin@phucanh.vn', '0900000001', '$2a$12$zA4J/HnTBBwyUZeMqlVRhulkNPNQMRrAUk/mFXFYYJ2B9JwyVOMRC', 'ADMIN', 'Quản trị viên', 'Hành chính', 'Toàn quốc', true)
ON CONFLICT (code) DO NOTHING;

-- Seed accountant users for Spec 008 testing
INSERT INTO users (code, full_name, email, phone, password_hash, role, job_title, shift, region, is_active)
VALUES
    ('NV-005', 'Nguyễn Kế Toán', 'accountant@phucanh.vn', '0911222333', '$2a$12$zA4J/HnTBBwyUZeMqlVRhulkNPNQMRrAUk/mFXFYYJ2B9JwyVOMRC', 'ACCOUNTANT', 'Kế toán viên', 'Hành chính', 'Toàn quốc', true),
    ('NV-006', 'Phạm Kế Toán Trưởng', 'acc_manager@phucanh.vn', '0922333444', '$2a$12$zA4J/HnTBBwyUZeMqlVRhulkNPNQMRrAUk/mFXFYYJ2B9JwyVOMRC', 'ACCOUNTANT_MANAGER', 'Kế toán trưởng', 'Hành chính', 'Toàn quốc', true)
ON CONFLICT (code) DO NOTHING;

-- Assign warehouses (WH-HP, WH-HN, WH-HCM) to the accountants
INSERT INTO user_warehouse_assignments (user_id, warehouse_id, assigned_by)
SELECT u.id, w.id, (SELECT id FROM users WHERE code = 'NV-001')
FROM users u, warehouses w
WHERE u.code IN ('NV-005', 'NV-006') AND w.code IN ('WH-HP', 'WH-HN', 'WH-HCM')
ON CONFLICT (user_id, warehouse_id) DO NOTHING;

-- Seed a test dealer if not exists
INSERT INTO dealers (code, name, phone, default_delivery_address, region, email, payment_term_days, credit_limit, current_balance, credit_status, is_active, created_by)
VALUES
    ('DL-001', 'Đại lý Máy Tính Phúc Anh', '0123456789', '85 Trần Đại Nghĩa, Hai Bà Trưng, Hà Nội', 'Hà Nội', 'contact@phucanh.com.vn', 30, 200000000.00, 0.00, 'ACTIVE', true, (SELECT id FROM users WHERE code = 'NV-001'))
ON CONFLICT (code) DO NOTHING;

-- Seed a test product if not exists
INSERT INTO products (sku, name, unit, unit_per_pack, description, weight_kg, volume_m3, has_expiry, has_serial, reorder_point, is_active, created_by)
VALUES
    ('PROD-001', 'Nồi lẩu điện Sunhouse 3L', 'cái', 4, 'Nồi lẩu điện đa năng Sunhouse dung tích 3 lít', 2.50, 0.01200, false, false, 10.00, true, (SELECT id FROM users WHERE code = 'NV-001'))
ON CONFLICT (sku) DO NOTHING;

-- Seed a test delivery order (DO) at status 'DELIVERED'
INSERT INTO delivery_orders (do_number, dealer_id, warehouse_id, type, expected_delivery_date, status, created_by, document_date, accounting_period_id, notes)
VALUES
    ('DO-202607-0001', 
     (SELECT id FROM dealers WHERE code = 'DL-001'), 
     (SELECT id FROM warehouses WHERE code = 'WH-HN'), 
     'SALE', 
     CURRENT_DATE, 
     'DELIVERED', 
     (SELECT id FROM users WHERE code = 'NV-001'), 
     CURRENT_DATE, 
     (SELECT id FROM accounting_periods ORDER BY start_date DESC LIMIT 1), 
     'Đơn hàng bán lẻ chạy thử nghiệm lập hóa đơn')
ON CONFLICT (do_number) DO NOTHING;

-- Seed delivery order items (to avoid validation issues if any)
INSERT INTO delivery_order_items (do_id, product_id, requested_qty, planned_qty, picked_qty, qc_pass_qty, qc_fail_qty, reserved_qty, issued_qty, unit_price, unit_cost)
VALUES
    ((SELECT id FROM delivery_orders WHERE do_number = 'DO-202607-0001'),
     (SELECT id FROM products WHERE sku = 'PROD-001'),
     10.00, 10.00, 10.00, 10.00, 0.00, 0.00, 10.00, 500000.00, 350000.00)
ON CONFLICT DO NOTHING;

-- Seed billing notification for the delivered order to trigger frontend invoicing notification
INSERT INTO billing_notifications (do_id, do_number, dealer_id, dealer_name, warehouse_id, delivered_at, total_amount_estimate, invoice_status, status, recipient_role)
VALUES
    ((SELECT id FROM delivery_orders WHERE do_number = 'DO-202607-0001'),
     'DO-202607-0001',
     (SELECT id FROM dealers WHERE code = 'DL-001'),
     'Đại lý Máy Tính Phúc Anh',
     (SELECT id FROM warehouses WHERE code = 'WH-HN'),
     CURRENT_TIMESTAMP,
     5000000.00, -- 10 cái * 500.000 VNĐ
     'NOT_INVOICED',
     'ACTIVE',
     'ACCOUNTANT')
ON CONFLICT DO NOTHING;
