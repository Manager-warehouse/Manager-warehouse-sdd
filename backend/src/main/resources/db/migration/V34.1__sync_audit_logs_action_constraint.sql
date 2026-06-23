-- 1. Xóa bỏ constraint cũ (nếu có)
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;

-- 2. Xóa bỏ luôn cả constraint tên mới nếu nó đã vô tình được tạo từ trước
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS chk_audit_logs_action;

-- 3. Thêm lại constraint với danh sách hành động đầy đủ
ALTER TABLE audit_logs
ADD CONSTRAINT chk_audit_logs_action
CHECK (
    action IN (
        -- Cơ bản & Tài khoản
        'LOGIN',
        'LOGOUT',
        'CREATE',
        'UPDATE',
        'STATUS_CHANGE',
        'APPROVE',
        'REJECT',
        'CANCEL',
        'DELETE',
        'SOFT_DELETE',
        'ASSIGN',
        'UNASSIGN',

        -- Vận chuyển & Giao hàng (Trip / Outbound)
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

        -- Hóa đơn & Thông báo tài chính (Billing / Invoice)
        'INVOICE_AUTO_CREATE',
        'BILLING_NOTIFICATION_CREATE',
        'BILLING_NOTIFICATION_READ',
        'CREDIT_NOTE_CREATE',

        -- Cấu hình giá sản phẩm (Price)
        'PRICE_CREATE',
        'PRICE_UPDATE',
        'PRICE_IMPORT',
        'PRICE_CANCEL',
        'PRICE_APPROVE',

        -- Cập nhật Kho hàng tổng
        'INVENTORY_UPDATE'
    )
);