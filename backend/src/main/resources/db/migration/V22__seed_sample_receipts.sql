-- Seed sample suppliers if not present
INSERT INTO suppliers (code, name, contact_person, phone, email, is_active)
VALUES
    ('SUP-001', 'Công Ty Cung Cấp Chảo Sunhouse', 'Nguyen van B', '0912345678', 'contact@sunhouse.vn', true),
    ('SUP-002', 'Công Ty Thiết Bị Gia Dụng Việt', 'Trần Văn C', '0987654321', 'contact@giadungviet.vn', true)
ON CONFLICT (code) DO NOTHING;

-- Seed sample receipts for physical warehouses
INSERT INTO receipts (
    receipt_number, source_order_code, type, warehouse_id, supplier_id,
    contact_person, source_channel, status, document_date, accounting_period_id,
    created_by, notes, created_at, updated_at
)
VALUES
    (
        'RN-20260722-000001', 'PO-24126', 'PURCHASE',
        (SELECT id FROM warehouses WHERE code = 'WH-HP'),
        (SELECT id FROM suppliers WHERE code = 'SUP-001'),
        'Nguyen van B', 'Zalo', 'PENDING_RECEIPT', CURRENT_DATE,
        (SELECT id FROM accounting_periods ORDER BY start_date DESC LIMIT 1),
        (SELECT id FROM users WHERE code = 'NV-001'),
        'Lệnh nhập chảo inox mẫu cho kho Hải Phòng', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'RN-20260722-000002', 'PO-24127', 'PURCHASE',
        (SELECT id FROM warehouses WHERE code = 'WH-HN'),
        (SELECT id FROM suppliers WHERE code = 'SUP-002'),
        'Trần Văn C', 'Email', 'APPROVED', CURRENT_DATE,
        (SELECT id FROM accounting_periods ORDER BY start_date DESC LIMIT 1),
        (SELECT id FROM users WHERE code = 'NV-001'),
        'Lệnh nhập đồ gia dụng cho kho Hà Nội', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    )
ON CONFLICT (receipt_number) DO NOTHING;

-- Seed sample receipt items
INSERT INTO receipt_items (
    receipt_id, product_id, expected_qty, actual_qty, over_received_qty, unit_cost
)
VALUES
    (
        (SELECT id FROM receipts WHERE receipt_number = 'RN-20260722-000001'),
        (SELECT id FROM products WHERE sku = 'PROD-001'),
        10, 10, 0, 10000.00
    ),
    (
        (SELECT id FROM receipts WHERE receipt_number = 'RN-20260722-000002'),
        (SELECT id FROM products WHERE sku = 'PROD-001'),
        20, 20, 0, 15000.00
    )
ON CONFLICT DO NOTHING;
