-- =============================================================================
-- HỆ THỐNG QUẢN LÝ KHO (WMS) — PostgreSQL 18 — Schema Hoàn Chỉnh
-- Nguồn sự thật: database.md v1.0 (2026-05-29)
-- Tổng bảng core: 36  |  Extra operational: 1 (stock_alerts)
-- =============================================================================
-- Thứ tự section theo dependency (bảng được FK tới phải tạo trước):
--   1. Users          2. Warehouses & Locations   3. Products & Dealers
--   4. Suppliers / Vehicles / Drivers             5. Kỳ kế toán
--   6. Price History  7. Batches & Inventories
--   8. Inbound (PO → Receipt)
--   9. Outbound (Delivery Order)                  10. Transport (Trip)
--  11. Transfer       12. Stock Mgmt (StockTake / Adjustment / Damage)
--  13. Finance (Invoice → Payment → Credit/Debit Note)
--  14. Config & Audit                             15. Operational (stock_alerts)
--  16. Indexes        17. Triggers & Functions    18. Views
--  19. Seed Data
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- SECTION 1: XÁC THỰC & PHÂN QUYỀN
-- =============================================================================

-- §1.1 users
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    code          VARCHAR(50)  UNIQUE NOT NULL,             -- Mã nhân viên
    full_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    phone         VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,                    -- bcrypt cost >= 12
    role          VARCHAR(50)  NOT NULL
                  CHECK (role IN (
                      'ADMIN',             -- Toàn quyền hệ thống
                      'CEO',               -- Duyệt chi/điều chỉnh > 100M VNĐ
                      'WAREHOUSE_MANAGER', -- Quản lý kho, duyệt nhập/xuất
                      'STOREKEEPER',       -- Thủ kho: tiếp nhận, soạn, cất Bin
                      'WAREHOUSE_STAFF',   -- Nhân viên kho: bốc xếp, QC
                      'ACCOUNTANT',        -- Lập HĐ, ghi nhận thanh toán
                      'ACCOUNTANT_MANAGER',-- Duyệt giá, Credit Limit, chốt sổ
                      'PLANNER',           -- Lập lệnh nhập / đơn xuất
                      'DISPATCHER',        -- Điều phối xe nội bộ Phúc Anh
                      'DRIVER',            -- Nhận chuyến, POD smartphone
                      'REPORT_VIEWER'      -- Chỉ xem báo cáo
                  )),
    job_title     VARCHAR(100),            -- Cho Storekeeper / WarehouseStaff
    shift         VARCHAR(50),             -- Ca làm việc (cho nhân viên kho)
    region        VARCHAR(100),            -- Khu vực phụ trách (cho Dispatcher)
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- SECTION 2: KHO & VỊ TRÍ
-- =============================================================================

-- §2.1 warehouses
CREATE TABLE warehouses (
    id         BIGSERIAL    PRIMARY KEY,
    code       VARCHAR(20)  UNIQUE NOT NULL,  -- HP / HN / HCM / IN_TRANSIT
    name       VARCHAR(255) NOT NULL,
    address    TEXT,
    phone      VARCHAR(20),
    manager_id BIGINT       REFERENCES users(id),
    type       VARCHAR(20)  NOT NULL
               CHECK (type IN ('PHYSICAL','IN_TRANSIT')),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- §1.2 user_warehouse_assignments  (phụ thuộc users + warehouses)
CREATE TABLE user_warehouse_assignments (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users(id),
    warehouse_id BIGINT      NOT NULL REFERENCES warehouses(id),
    assigned_by  BIGINT      NOT NULL REFERENCES users(id),
    assigned_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, warehouse_id)
);

-- §2.2 warehouse_locations  (phân cấp: Zone → Rack → Shelf → Bin)
CREATE TABLE warehouse_locations (
    id                BIGSERIAL     PRIMARY KEY,
    warehouse_id      BIGINT        NOT NULL REFERENCES warehouses(id),
    code              VARCHAR(50)   UNIQUE NOT NULL,  -- e.g. WH-HP.A.01.1.01
    type              VARCHAR(10)   NOT NULL
                      CHECK (type IN ('ZONE','RACK','SHELF','BIN')),
    parent_id         BIGINT        REFERENCES warehouse_locations(id),
    capacity_m3       DECIMAL(10,3),
    capacity_kg       DECIMAL(10,2),
    current_volume_m3 DECIMAL(10,3) NOT NULL DEFAULT 0,
    current_weight_kg DECIMAL(10,2) NOT NULL DEFAULT 0,
    is_quarantine     BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE
);

-- =============================================================================
-- SECTION 3: DANH MỤC HÀNG HÓA & ĐỐI TÁC
-- =============================================================================

-- §2.3 products
CREATE TABLE products (
    id            BIGSERIAL     PRIMARY KEY,
    sku           VARCHAR(50)   UNIQUE NOT NULL,
    name          VARCHAR(255)  NOT NULL,
    unit          VARCHAR(30)   NOT NULL,         -- cái / thùng / kg ...
    unit_per_pack INTEGER,                        -- Quy đổi đơn vị đóng gói
    description   TEXT,
    image_url     VARCHAR(500),
    weight_kg     DECIMAL(10,3),
    volume_m3     DECIMAL(10,5),
    has_serial    BOOLEAN       NOT NULL DEFAULT FALSE,
    reorder_point DECIMAL(10,2),                 -- Ngưỡng cảnh báo tồn kho thấp
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- §2.4 dealers  (Critical 2: thêm 4 cột công nợ)
CREATE TABLE dealers (
    id                       BIGSERIAL     PRIMARY KEY,
    code                     VARCHAR(50)   UNIQUE NOT NULL,
    name                     VARCHAR(255)  NOT NULL,
    phone                    VARCHAR(20),
    default_delivery_address TEXT,
    region                   VARCHAR(100),
    payment_term_days        INTEGER       NOT NULL DEFAULT 30,
    credit_limit             DECIMAL(18,2) NOT NULL DEFAULT 0,
    current_balance          DECIMAL(18,2) NOT NULL DEFAULT 0,
    credit_status            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                             CHECK (credit_status IN ('ACTIVE','CREDIT_HOLD')),
    is_active                BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- §2.5 suppliers
CREATE TABLE suppliers (
    id             BIGSERIAL    PRIMARY KEY,
    code           VARCHAR(50)  UNIQUE NOT NULL,
    company_name   VARCHAR(255) NOT NULL,
    tax_code       VARCHAR(20),
    phone          VARCHAR(20),
    contact_person VARCHAR(255),
    address        TEXT,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- §2.6 vehicles  (Medium 4: thêm max_volume_m3)
CREATE TABLE vehicles (
    id            BIGSERIAL     PRIMARY KEY,
    plate_number  VARCHAR(20)   UNIQUE NOT NULL,
    vehicle_type  VARCHAR(100)  NOT NULL,
    max_weight_kg DECIMAL(10,2) NOT NULL,
    max_volume_m3 DECIMAL(10,3),
    status        VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE'
                  CHECK (status IN ('AVAILABLE','ON_TRIP','MAINTENANCE')),
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE
);

-- §2.7 drivers  (Medium 7: license_expiry NOT NULL per database.md)
CREATE TABLE drivers (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL UNIQUE REFERENCES users(id),
    full_name      VARCHAR(255) NOT NULL,
    phone          VARCHAR(20),
    license_number VARCHAR(50)  UNIQUE NOT NULL,
    license_expiry DATE         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE'
                   CHECK (status IN ('AVAILABLE','ON_DELIVERY','MAINTENANCE')),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE
);

-- =============================================================================
-- SECTION 4: KỲ KẾ TOÁN  (tạo sớm — nhiều bảng FK tới đây)
-- =============================================================================

-- §11.1 accounting_periods  (Critical 4)
CREATE TABLE accounting_periods (
    id          BIGSERIAL   PRIMARY KEY,
    period_name VARCHAR(20) UNIQUE NOT NULL,  -- format: YYYY-MM  e.g. 2026-05
    start_date  DATE        NOT NULL,
    end_date    DATE        NOT NULL,
    status      VARCHAR(10) NOT NULL DEFAULT 'OPEN'
                CHECK (status IN ('OPEN','CLOSED')),
    closed_by   BIGINT      REFERENCES users(id),
    closed_at   TIMESTAMPTZ,
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (end_date >= start_date)
);

-- =============================================================================
-- SECTION 5: GIÁ & LỊCH SỬ GIÁ
-- =============================================================================

-- §3.1 price_history  (Medium 6: thêm status, approved_by, approved_at)
CREATE TABLE price_history (
    id             BIGSERIAL     PRIMARY KEY,
    product_id     BIGINT        NOT NULL REFERENCES products(id),
    effective_date DATE          NOT NULL,
    end_date       DATE,                        -- NULL = đang hiệu lực
    cost_price     DECIMAL(18,2) NOT NULL,
    selling_price  DECIMAL(18,2) NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                   CHECK (status IN ('PENDING','APPROVED')),
    created_by     BIGINT        NOT NULL REFERENCES users(id),
    approved_by    BIGINT        REFERENCES users(id),
    approved_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- SECTION 6: LÔ HÀNG & TỒN KHO
-- =============================================================================

-- §4.1 batches
CREATE TABLE batches (
    id            BIGSERIAL     PRIMARY KEY,
    batch_number  VARCHAR(100)  UNIQUE NOT NULL,
    product_id    BIGINT        NOT NULL REFERENCES products(id),
    warehouse_id  BIGINT        NOT NULL REFERENCES warehouses(id),
    received_date DATE          NOT NULL,       -- Dùng cho FIFO
    expiry_date   DATE,                         -- NULL = không có hạn; dùng cho FEFO
    grade         VARCHAR(1)    NOT NULL
                  CHECK (grade IN ('A','B','C')),  -- Bất biến sau khi tạo
    quantity      DECIMAL(10,2) NOT NULL
                  CHECK (quantity >= 0),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- §4.2 inventories
CREATE TABLE inventories (
    id           BIGSERIAL     PRIMARY KEY,
    warehouse_id BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id   BIGINT        NOT NULL REFERENCES products(id),
    batch_id     BIGINT        NOT NULL REFERENCES batches(id),
    location_id  BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    total_qty    DECIMAL(10,2) NOT NULL DEFAULT 0,
    reserved_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    cost_price   DECIMAL(18,2) NOT NULL,        -- Giá vốn tại thời điểm nhập
    version      INTEGER       NOT NULL DEFAULT 0, -- Optimistic locking (+1 mỗi UPDATE)
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (warehouse_id, product_id, batch_id, location_id),
    CHECK (total_qty    >= 0),
    CHECK (reserved_qty >= 0),
    CHECK (total_qty - reserved_qty >= 0)        -- available_qty không bao giờ âm
);
-- available_qty = total_qty - reserved_qty  (computed, không lưu DB)

-- =============================================================================
-- SECTION 7: NHẬP KHO (INBOUND)
-- =============================================================================

-- §5.1 purchase_orders
CREATE TABLE purchase_orders (
    id                    BIGSERIAL   PRIMARY KEY,
    po_number             VARCHAR(50) UNIQUE NOT NULL,
    supplier_id           BIGINT      NOT NULL REFERENCES suppliers(id),
    warehouse_id          BIGINT      NOT NULL REFERENCES warehouses(id),
    expected_receipt_date DATE,
    status                VARCHAR(30) NOT NULL
                          CHECK (status IN ('OPEN','PARTIALLY_RECEIVED','COMPLETED','CANCELLED')),
    created_by            BIGINT      NOT NULL REFERENCES users(id),
    notes                 TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- §5.2 purchase_order_items
CREATE TABLE purchase_order_items (
    id           BIGSERIAL     PRIMARY KEY,
    po_id        BIGINT        NOT NULL REFERENCES purchase_orders(id),
    product_id   BIGINT        NOT NULL REFERENCES products(id),
    expected_qty DECIMAL(10,2) NOT NULL,
    unit_price   DECIMAL(18,2)
);

-- §5.3 receipts
CREATE TABLE receipts (
    id                   BIGSERIAL   PRIMARY KEY,
    receipt_number       VARCHAR(50) UNIQUE NOT NULL,
    source_order_code    VARCHAR(100),            -- Mã PO hoặc DO hoàn
    type                 VARCHAR(20) NOT NULL
                         CHECK (type IN ('PURCHASE','RETURN')),
    warehouse_id         BIGINT      NOT NULL REFERENCES warehouses(id),
    supplier_id          BIGINT      REFERENCES suppliers(id),
    dealer_id            BIGINT      REFERENCES dealers(id),
    contact_person       VARCHAR(255),
    source_channel       VARCHAR(50),             -- Zalo / Email
    status               VARCHAR(30) NOT NULL DEFAULT 'PENDING_RECEIPT'
                         CHECK (status IN (
                             'PENDING_RECEIPT','DRAFT','QC_COMPLETED','APPROVED','REJECTED'
                         )),
    approved_by          BIGINT      REFERENCES users(id),
    approved_at          TIMESTAMPTZ,
    rejection_reason     TEXT,
    document_date        DATE        NOT NULL,
    accounting_period_id BIGINT      REFERENCES accounting_periods(id),
    created_by           BIGINT      NOT NULL REFERENCES users(id),
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- §5.4 receipt_items
CREATE TABLE receipt_items (
    id                BIGSERIAL     PRIMARY KEY,
    receipt_id        BIGINT        NOT NULL REFERENCES receipts(id),
    product_id        BIGINT        NOT NULL REFERENCES products(id),
    batch_id          BIGINT        REFERENCES batches(id),        -- NULL trước QC
    location_id       BIGINT        REFERENCES warehouse_locations(id),
    expected_qty      DECIMAL(10,2) NOT NULL,
    actual_qty        DECIMAL(10,2),
    qc_passed_qty     DECIMAL(10,2),
    qc_failed_qty     DECIMAL(10,2),                               -- → Quarantine Zone
    qc_result         VARCHAR(20)
                      CHECK (qc_result IN ('PENDING','PASSED','FAILED','PARTIAL')),
    qc_failure_reason TEXT,
    grade             VARCHAR(1),                -- A / B / C (grade của lô hàng này)
    unit_cost         DECIMAL(18,2),
    serial_number     VARCHAR(100)               -- Nếu product.has_serial = true
);

-- =============================================================================
-- SECTION 8: XUẤT KHO (OUTBOUND)
-- =============================================================================

-- §6.1 delivery_orders  (Medium 2: đủ 9 trạng thái theo database.md)
CREATE TABLE delivery_orders (
    id                   BIGSERIAL   PRIMARY KEY,
    do_number            VARCHAR(50) UNIQUE NOT NULL,
    dealer_id            BIGINT      NOT NULL REFERENCES dealers(id),
    warehouse_id         BIGINT      NOT NULL REFERENCES warehouses(id),
    type                 VARCHAR(30) NOT NULL
                         CHECK (type IN ('SALE','DELIVERY','ADJUSTMENT')),
    expected_delivery_date DATE,
    status               VARCHAR(30) NOT NULL DEFAULT 'NEW'
                         CHECK (status IN (
                             'NEW','PICKING','READY_TO_SHIP','IN_TRANSIT',
                             'OUT_FOR_DELIVERY','DELIVERED','COMPLETED','CLOSED','CANCELLED'
                         )),
    created_by           BIGINT      NOT NULL REFERENCES users(id),
    cancel_reason        TEXT,
    document_date        DATE        NOT NULL,
    accounting_period_id BIGINT      REFERENCES accounting_periods(id),
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- §6.2 delivery_order_items
CREATE TABLE delivery_order_items (
    id            BIGSERIAL     PRIMARY KEY,
    do_id         BIGINT        NOT NULL REFERENCES delivery_orders(id),
    product_id    BIGINT        NOT NULL REFERENCES products(id),
    batch_id      BIGINT        REFERENCES batches(id),
    location_id   BIGINT        REFERENCES warehouse_locations(id),
    requested_qty DECIMAL(10,2) NOT NULL,
    reserved_qty  DECIMAL(10,2) NOT NULL DEFAULT 0
                  CHECK (reserved_qty >= 0),   -- Đang giữ chỗ trong inventories
    issued_qty    DECIMAL(10,2) NOT NULL DEFAULT 0,
    unit_price    DECIMAL(18,2),               -- Từ price_history tại ngày giao
    serial_number VARCHAR(100)
);

-- §6.3 delivery_order_approvals  (Kế toán duyệt)
CREATE TABLE delivery_order_approvals (
    id                 BIGSERIAL   PRIMARY KEY,
    do_id              BIGINT      NOT NULL REFERENCES delivery_orders(id),
    approver_id        BIGINT      NOT NULL REFERENCES users(id),
    result             VARCHAR(20) NOT NULL
                       CHECK (result IN ('APPROVED','REJECTED')),
    contract_image_url VARCHAR(500),           -- Bắt buộc khi result = APPROVED
    rejection_reason   TEXT,                   -- Bắt buộc khi result = REJECTED
    approved_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- §6.4 delivery_order_warehouse_approvals  (Trưởng kho duyệt)
CREATE TABLE delivery_order_warehouse_approvals (
    id          BIGSERIAL   PRIMARY KEY,
    do_id       BIGINT      NOT NULL REFERENCES delivery_orders(id),
    approver_id BIGINT      NOT NULL REFERENCES users(id),
    result      VARCHAR(20) NOT NULL
                CHECK (result IN ('APPROVED','REJECTED')),
    notes       TEXT,
    approved_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- SECTION 9: VẬN TẢI (TRANSPORT)
-- =============================================================================

-- §7.1 trips
CREATE TABLE trips (
    id              BIGSERIAL     PRIMARY KEY,
    trip_number     VARCHAR(50)   UNIQUE NOT NULL,
    vehicle_id      BIGINT        NOT NULL REFERENCES vehicles(id),
    driver_id       BIGINT        NOT NULL REFERENCES drivers(id),
    dispatcher_id   BIGINT        NOT NULL REFERENCES users(id),
    planned_date    DATE          NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PLANNED'
                    CHECK (status IN ('PLANNED','IN_TRANSIT','COMPLETED')),
    total_weight_kg DECIMAL(10,2) DEFAULT 0,   -- Tổng khối lượng (kiểm tra tải trọng)
    total_volume_m3 DECIMAL(10,3) DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- §7.2 trip_delivery_orders
CREATE TABLE trip_delivery_orders (
    id         BIGSERIAL PRIMARY KEY,
    trip_id    BIGINT    NOT NULL REFERENCES trips(id),
    do_id      BIGINT    NOT NULL UNIQUE REFERENCES delivery_orders(id),  -- 1 DO chỉ thuộc 1 chuyến
    stop_order INTEGER   NOT NULL,
    UNIQUE (trip_id, stop_order)
);

-- §7.3 deliveries  (POD)
CREATE TABLE deliveries (
    id                BIGSERIAL    PRIMARY KEY,
    delivery_number   VARCHAR(50)  UNIQUE NOT NULL,
    do_id             BIGINT       NOT NULL REFERENCES delivery_orders(id),
    trip_id           BIGINT       REFERENCES trips(id),
    vehicle_id        BIGINT       NOT NULL REFERENCES vehicles(id),
    driver_id         BIGINT       NOT NULL REFERENCES drivers(id),
    status            VARCHAR(30)  NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN (
                          'PENDING','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','RETURNED'
                      )),
    pod_image_url     VARCHAR(500),
    pod_signature_url VARCHAR(500),
    pod_timestamp     TIMESTAMPTZ,
    failure_reason    TEXT,                    -- Vắng / từ chối / sai địa chỉ
    delivered_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- SECTION 10: ĐIỀU CHUYỂN NỘI BỘ
-- =============================================================================

-- §8.1 transfers
CREATE TABLE transfers (
    id                       BIGSERIAL   PRIMARY KEY,
    transfer_number          VARCHAR(50) UNIQUE NOT NULL,
    source_warehouse_id      BIGINT      NOT NULL REFERENCES warehouses(id),
    destination_warehouse_id BIGINT      NOT NULL REFERENCES warehouses(id),
    status                   VARCHAR(40) NOT NULL DEFAULT 'NEW'
                             CHECK (status IN (
                                 'NEW','APPROVED','IN_TRANSIT',
                                 'COMPLETED','COMPLETED_WITH_DISCREPANCY','CANCELLED'
                             )),
    created_by               BIGINT      NOT NULL REFERENCES users(id),
    approved_by              BIGINT      REFERENCES users(id),
    approved_at              TIMESTAMPTZ,
    confirmed_by             BIGINT      REFERENCES users(id),
    confirmed_at             TIMESTAMPTZ,
    planned_date             DATE,
    actual_received_date     DATE,
    discrepancy_reason       TEXT,
    document_date            DATE        NOT NULL,
    accounting_period_id     BIGINT      REFERENCES accounting_periods(id),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- §8.2 transfer_items
CREATE TABLE transfer_items (
    id                      BIGSERIAL     PRIMARY KEY,
    transfer_id             BIGINT        NOT NULL REFERENCES transfers(id),
    product_id              BIGINT        NOT NULL REFERENCES products(id),
    batch_id                BIGINT        NOT NULL REFERENCES batches(id),
    source_location_id      BIGINT        REFERENCES warehouse_locations(id),
    destination_location_id BIGINT        REFERENCES warehouse_locations(id),
    planned_qty             DECIMAL(10,2) NOT NULL,
    sent_qty                DECIMAL(10,2),
    received_qty            DECIMAL(10,2),
    variance_qty            DECIMAL(10,2)  -- received_qty - sent_qty  (âm = thiếu)
);

-- =============================================================================
-- SECTION 11: KIỂM KÊ & ĐIỀU CHỈNH
-- =============================================================================

-- §9.1 stock_takes
CREATE TABLE stock_takes (
    id                   BIGSERIAL     PRIMARY KEY,
    stock_take_number    VARCHAR(50)   UNIQUE NOT NULL,
    warehouse_id         BIGINT        NOT NULL REFERENCES warehouses(id),
    conducted_by         BIGINT        NOT NULL REFERENCES users(id),
    approved_by          BIGINT        REFERENCES users(id),
    approved_at          TIMESTAMPTZ,
    status               VARCHAR(30)   NOT NULL DEFAULT 'DRAFT'
                         CHECK (status IN (
                             'DRAFT','IN_PROGRESS','PENDING_APPROVAL','APPROVED','CANCELLED'
                         )),
    total_variance_value DECIMAL(18,2) DEFAULT 0,  -- Xác định thẩm quyền duyệt
    stock_take_date      DATE          NOT NULL,
    document_date        DATE          NOT NULL,
    accounting_period_id BIGINT        REFERENCES accounting_periods(id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- §9.2 stock_take_items
CREATE TABLE stock_take_items (
    id             BIGSERIAL     PRIMARY KEY,
    stock_take_id  BIGINT        NOT NULL REFERENCES stock_takes(id),
    product_id     BIGINT        NOT NULL REFERENCES products(id),
    batch_id       BIGINT        NOT NULL REFERENCES batches(id),
    location_id    BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    system_qty     DECIMAL(10,2) NOT NULL,
    actual_qty     DECIMAL(10,2) NOT NULL,
    variance_qty   DECIMAL(10,2) NOT NULL,     -- actual_qty - system_qty
    variance_value DECIMAL(18,2) NOT NULL,     -- variance_qty × cost_price
    notes          TEXT
);

-- §9.3 adjustments  (Critical 1: tên adjustments, cột type, enum 5 giá trị)
CREATE TABLE adjustments (
    id                   BIGSERIAL     PRIMARY KEY,
    adjustment_number    VARCHAR(50)   UNIQUE NOT NULL,
    warehouse_id         BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id           BIGINT        NOT NULL REFERENCES products(id),
    batch_id             BIGINT        REFERENCES batches(id),
    location_id          BIGINT        REFERENCES warehouse_locations(id),
    quantity_adjustment  DECIMAL(10,2) NOT NULL,  -- dương = tăng, âm = giảm
    type                 VARCHAR(30)   NOT NULL
                         CHECK (type IN (
                             'STOCK_TAKE',            -- Từ kiểm kê định kỳ
                             'TRANSFER_DISCREPANCY',  -- Chênh lệch điều chuyển
                             'DISPOSAL',              -- Tiêu hủy hàng lỗi/hết hạn
                             'RETURN_TO_VENDOR',      -- Trả hàng cho NCC
                             'CORRECTION_VOUCHER'     -- Sai sót kỳ kế toán đã chốt
                         )),
    reference_id         BIGINT,               -- ID chứng từ liên quan
    reference_type       VARCHAR(50),          -- Loại chứng từ liên quan
    reason               TEXT          NOT NULL,
    approved_by          BIGINT        REFERENCES users(id),
    approved_at          TIMESTAMPTZ,
    document_date        DATE          NOT NULL,
    accounting_period_id BIGINT        REFERENCES accounting_periods(id),
    created_by           BIGINT        NOT NULL REFERENCES users(id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- §9.4 damage_reports
CREATE TABLE damage_reports (
    id            BIGSERIAL     PRIMARY KEY,
    report_number VARCHAR(50)   UNIQUE NOT NULL,
    warehouse_id  BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id    BIGINT        NOT NULL REFERENCES products(id),
    batch_id      BIGINT        REFERENCES batches(id),
    quantity      DECIMAL(10,2) NOT NULL,
    cause         TEXT          NOT NULL,
    image_url     VARCHAR(500),
    reported_by   BIGINT        NOT NULL REFERENCES users(id),
    report_date   DATE          NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- SECTION 12: TÀI CHÍNH & CÔNG NỢ
-- =============================================================================

-- §10.1 invoices  (Critical 3)
CREATE TABLE invoices (
    id                   BIGSERIAL     PRIMARY KEY,
    invoice_number       VARCHAR(50)   UNIQUE NOT NULL,
    do_id                BIGINT        NOT NULL REFERENCES delivery_orders(id),
    dealer_id            BIGINT        NOT NULL REFERENCES dealers(id),
    total_amount         DECIMAL(18,2) NOT NULL,
    issue_date           DATE          NOT NULL,
    due_date             DATE          NOT NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'UNPAID'
                         CHECK (status IN ('UNPAID','PARTIALLY_PAID','PAID')),
    created_by           BIGINT        NOT NULL REFERENCES users(id),
    document_date        DATE          NOT NULL,
    accounting_period_id BIGINT        REFERENCES accounting_periods(id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- §10.2 payment_receipts  (Critical 3)
CREATE TABLE payment_receipts (
    id                   BIGSERIAL     PRIMARY KEY,
    payment_number       VARCHAR(50)   UNIQUE NOT NULL,
    dealer_id            BIGINT        NOT NULL REFERENCES dealers(id),
    invoice_id           BIGINT        NOT NULL REFERENCES invoices(id),
    amount               DECIMAL(18,2) NOT NULL,
    payment_date         DATE          NOT NULL,
    payment_method       VARCHAR(30)   NOT NULL
                         CHECK (payment_method IN ('BANK_TRANSFER','CASH')),
    created_by           BIGINT        NOT NULL REFERENCES users(id),
    document_date        DATE          NOT NULL,
    accounting_period_id BIGINT        REFERENCES accounting_periods(id),
    notes                TEXT,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- §10.3 credit_notes
CREATE TABLE credit_notes (
    id                   BIGSERIAL     PRIMARY KEY,
    credit_note_number   VARCHAR(50)   UNIQUE NOT NULL,
    dealer_id            BIGINT        NOT NULL REFERENCES dealers(id),
    receipt_id           BIGINT        REFERENCES receipts(id),
    amount               DECIMAL(18,2) NOT NULL,
    reason               TEXT          NOT NULL,
    created_by           BIGINT        NOT NULL REFERENCES users(id),
    document_date        DATE          NOT NULL,
    accounting_period_id BIGINT        REFERENCES accounting_periods(id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- §10.4 debit_notes
CREATE TABLE debit_notes (
    id                   BIGSERIAL     PRIMARY KEY,
    debit_note_number    VARCHAR(50)   UNIQUE NOT NULL,
    supplier_id          BIGINT        NOT NULL REFERENCES suppliers(id),
    receipt_id           BIGINT        REFERENCES receipts(id),
    failed_qty           DECIMAL(10,2) NOT NULL,
    amount               DECIMAL(18,2) NOT NULL,
    reason               TEXT          NOT NULL,
    created_by           BIGINT        NOT NULL REFERENCES users(id),
    document_date        DATE          NOT NULL,
    accounting_period_id BIGINT        REFERENCES accounting_periods(id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- SECTION 13: CẤU HÌNH & AUDIT
-- =============================================================================

-- §12.1 system_configs
-- updated_by nullable: system-managed entries không có user (admin được seed ở V2)
CREATE TABLE system_configs (
    id           BIGSERIAL    PRIMARY KEY,
    config_key   VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT         NOT NULL,
    description  TEXT,
    updated_by   BIGINT       REFERENCES users(id),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- §12.2 audit_logs
CREATE TABLE audit_logs (
    id          BIGSERIAL    PRIMARY KEY,
    actor_id    BIGINT       REFERENCES users(id),   -- NULL = system job
    actor_role  VARCHAR(50),                         -- Snapshot role tại thời điểm thực hiện
    action      VARCHAR(50)  NOT NULL
                CHECK (action IN ('CREATE','UPDATE','APPROVE','REJECT','CANCEL','DELETE')),
    entity_type VARCHAR(100) NOT NULL,
    entity_id   BIGINT       NOT NULL,
    old_value   JSONB,
    new_value   JSONB,
    timestamp   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ip_address  VARCHAR(45)
);

-- =============================================================================
-- SECTION 14: BẢNG HỖ TRỢ NGHIỆP VỤ
-- =============================================================================

-- stock_alerts: cảnh báo tồn kho thấp (hỗ trợ trigger fn_check_stock_alert)
-- Medium 5: UNIQUE (warehouse_id, product_id, alert_type, is_resolved)
--           để ON CONFLICT DO NOTHING trong trigger hoạt động đúng
CREATE TABLE stock_alerts (
    id            BIGSERIAL     PRIMARY KEY,
    warehouse_id  BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id    BIGINT        NOT NULL REFERENCES products(id),
    current_qty   DECIMAL(10,2) NOT NULL,
    reorder_point DECIMAL(10,2) NOT NULL,
    alert_type    VARCHAR(20)   NOT NULL DEFAULT 'LOW_STOCK'
                  CHECK (alert_type IN ('LOW_STOCK','OUT_OF_STOCK')),
    is_resolved   BOOLEAN       NOT NULL DEFAULT FALSE,
    resolved_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (warehouse_id, product_id, alert_type, is_resolved)
);

-- =============================================================================
-- SECTION 15: PERFORMANCE INDEXES
-- =============================================================================

-- inventories
CREATE INDEX idx_inventories_warehouse_product ON inventories(warehouse_id, product_id);
CREATE INDEX idx_inventories_batch             ON inventories(batch_id);
CREATE INDEX idx_inventories_location          ON inventories(location_id);

-- batches — FEFO (expiry_date ASC) + FIFO (received_date ASC)
CREATE INDEX idx_batches_product_expiry    ON batches(product_id, expiry_date ASC NULLS LAST);
CREATE INDEX idx_batches_product_received  ON batches(product_id, received_date ASC);
CREATE INDEX idx_batches_grade             ON batches(product_id, grade);

-- warehouse_locations (phân cấp)
CREATE INDEX idx_wh_locations_warehouse   ON warehouse_locations(warehouse_id);
CREATE INDEX idx_wh_locations_parent      ON warehouse_locations(parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_wh_locations_type        ON warehouse_locations(warehouse_id, type);
CREATE INDEX idx_wh_locations_quarantine  ON warehouse_locations(is_quarantine) WHERE is_quarantine = TRUE;

-- products
CREATE INDEX idx_products_sku_active ON products(sku, is_active);

-- dealers
CREATE INDEX idx_dealers_credit_status ON dealers(credit_status);

-- delivery_orders
CREATE INDEX idx_do_dealer_status     ON delivery_orders(dealer_id, status);
CREATE INDEX idx_do_warehouse_status  ON delivery_orders(warehouse_id, status);

-- receipts
CREATE INDEX idx_receipts_warehouse_status ON receipts(warehouse_id, status);

-- invoices
CREATE INDEX idx_invoices_dealer_status ON invoices(dealer_id, status);
CREATE INDEX idx_invoices_due_date      ON invoices(due_date);

-- transfers
CREATE INDEX idx_transfers_src_status ON transfers(source_warehouse_id, status);

-- stock_takes
CREATE INDEX idx_stock_takes_warehouse ON stock_takes(warehouse_id, status);

-- audit_logs
CREATE INDEX idx_audit_entity     ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_actor      ON audit_logs(actor_id);
CREATE INDEX idx_audit_actor_role ON audit_logs(actor_role) WHERE actor_role IS NOT NULL;
CREATE INDEX idx_audit_timestamp  ON audit_logs(timestamp DESC);

-- =============================================================================
-- SECTION 16: TRIGGERS & FUNCTIONS
-- =============================================================================

-- ── Trigger: auto-update updated_at ─────────────────────────────────────────
CREATE OR REPLACE FUNCTION fn_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'users', 'products', 'dealers',
        'purchase_orders', 'receipts', 'delivery_orders',
        'transfers', 'stock_takes', 'trips', 'deliveries', 'invoices'
    ] LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%s_updated_at '
            'BEFORE UPDATE ON %s '
            'FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp()',
            t, t
        );
    END LOOP;
END;
$$;

-- ── Trigger: cập nhật sức chứa location khi inventories thay đổi ────────────
CREATE OR REPLACE FUNCTION fn_update_bin_capacity()
RETURNS TRIGGER AS $$
DECLARE
    v_volume NUMERIC;
    v_weight NUMERIC;
    v_loc_id BIGINT;
BEGIN
    v_loc_id := COALESCE(NEW.location_id, OLD.location_id);

    SELECT
        COALESCE(SUM(i.total_qty * p.volume_m3),  0),
        COALESCE(SUM(i.total_qty * p.weight_kg), 0)
    INTO v_volume, v_weight
    FROM inventories i
    JOIN products    p ON p.id = i.product_id
    WHERE i.location_id = v_loc_id;

    UPDATE warehouse_locations
    SET current_volume_m3 = v_volume,
        current_weight_kg = v_weight
    WHERE id = v_loc_id;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_inventories_bin_capacity
    AFTER INSERT OR UPDATE OR DELETE ON inventories
    FOR EACH ROW EXECUTE FUNCTION fn_update_bin_capacity();

-- ── Trigger: cảnh báo tồn kho thấp ──────────────────────────────────────────
CREATE OR REPLACE FUNCTION fn_check_stock_alert()
RETURNS TRIGGER AS $$
DECLARE
    v_reorder   DECIMAL;
    v_total_qty DECIMAL;
BEGIN
    SELECT reorder_point INTO v_reorder
    FROM products WHERE id = NEW.product_id;

    SELECT COALESCE(SUM(total_qty), 0) INTO v_total_qty
    FROM inventories
    WHERE warehouse_id = NEW.warehouse_id
      AND product_id   = NEW.product_id;

    IF v_total_qty <= 0 THEN
        INSERT INTO stock_alerts
            (warehouse_id, product_id, current_qty, reorder_point, alert_type)
        VALUES
            (NEW.warehouse_id, NEW.product_id, v_total_qty, COALESCE(v_reorder,0), 'OUT_OF_STOCK')
        ON CONFLICT (warehouse_id, product_id, alert_type, is_resolved) DO NOTHING;
    ELSIF v_reorder IS NOT NULL AND v_total_qty <= v_reorder THEN
        INSERT INTO stock_alerts
            (warehouse_id, product_id, current_qty, reorder_point, alert_type)
        VALUES
            (NEW.warehouse_id, NEW.product_id, v_total_qty, v_reorder, 'LOW_STOCK')
        ON CONFLICT (warehouse_id, product_id, alert_type, is_resolved) DO NOTHING;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_inventories_stock_alert
    AFTER INSERT OR UPDATE OF total_qty ON inventories
    FOR EACH ROW EXECUTE FUNCTION fn_check_stock_alert();

-- =============================================================================
-- SECTION 17: VIEWS
-- =============================================================================

-- Tồn kho tổng hợp theo kho + sản phẩm
CREATE VIEW v_inventory_summary AS
SELECT
    w.code                                               AS warehouse_code,
    w.name                                               AS warehouse_name,
    p.sku,
    p.name                                               AS product_name,
    p.unit,
    COALESCE(SUM(i.total_qty),  0)                       AS total_qty,
    COALESCE(SUM(i.reserved_qty), 0)                     AS reserved_qty,
    COALESCE(SUM(i.total_qty - i.reserved_qty), 0)       AS available_qty,
    p.reorder_point,
    COALESCE(SUM(i.total_qty * i.cost_price), 0)         AS inventory_value
FROM warehouses  w
JOIN inventories i ON i.warehouse_id = w.id
JOIN products    p ON p.id = i.product_id
WHERE w.type = 'PHYSICAL'
GROUP BY w.id, w.code, w.name, p.id, p.sku, p.name, p.unit, p.reorder_point;

-- Tồn kho chi tiết theo lô hàng (FEFO: expiry_date ASC / FIFO: received_date ASC)
CREATE VIEW v_inventory_by_batch AS
SELECT
    w.code                                   AS warehouse_code,
    p.sku,
    p.name                                   AS product_name,
    b.batch_number,
    b.grade,
    b.received_date,
    b.expiry_date,
    wl.code                                  AS location_code,
    i.total_qty,
    i.reserved_qty,
    (i.total_qty - i.reserved_qty)           AS available_qty,
    i.cost_price,
    (i.total_qty * i.cost_price)             AS line_value
FROM inventories         i
JOIN warehouses          w  ON w.id  = i.warehouse_id
JOIN products            p  ON p.id  = i.product_id
JOIN batches             b  ON b.id  = i.batch_id
JOIN warehouse_locations wl ON wl.id = i.location_id
WHERE w.type = 'PHYSICAL'
ORDER BY w.code, p.sku,
         b.expiry_date   ASC NULLS LAST,
         b.received_date ASC NULLS LAST;

-- Đơn xuất kho đang chờ xử lý tại kho
CREATE VIEW v_pending_delivery_orders AS
SELECT
    do.do_number,
    do.status,
    do.type,
    d.name            AS dealer_name,
    d.phone           AS dealer_phone,
    do.expected_delivery_date,
    u.full_name       AS created_by_name,
    do.document_date,
    do.created_at
FROM delivery_orders do
JOIN dealers d ON d.id = do.dealer_id
JOIN users   u ON u.id = do.created_by
WHERE do.status IN ('NEW','PICKING','READY_TO_SHIP')
ORDER BY do.expected_delivery_date ASC NULLS LAST, do.created_at ASC;

-- Cảnh báo tồn kho thấp chưa giải quyết
CREATE VIEW v_low_stock_alerts AS
SELECT
    sa.alert_type,
    w.code    AS warehouse_code,
    w.name    AS warehouse_name,
    p.sku,
    p.name    AS product_name,
    sa.current_qty,
    sa.reorder_point,
    sa.created_at AS alerted_at
FROM stock_alerts sa
JOIN warehouses w ON w.id = sa.warehouse_id
JOIN products   p ON p.id = sa.product_id
WHERE sa.is_resolved = FALSE
ORDER BY sa.alert_type, sa.created_at DESC;

-- =============================================================================
-- SECTION 18: SEED DATA
-- =============================================================================

-- Kho vật lý (3) + kho ảo In-Transit (1)
-- Quarantine Zone được quản lý qua warehouse_locations.is_quarantine = true
INSERT INTO warehouses (code, name, type, address, phone) VALUES
    ('WH-HP',      'Kho Hải Phòng',        'PHYSICAL',   'Hải Phòng, Việt Nam',        '0225-000001'),
    ('WH-HN',      'Kho Hà Nội',           'PHYSICAL',   'Hà Nội, Việt Nam',           '024-000001'),
    ('WH-HCM',     'Kho Hồ Chí Minh',      'PHYSICAL',   'TP. Hồ Chí Minh, Việt Nam',  '028-000001'),
    ('IN_TRANSIT', 'Kho ảo In-Transit',     'IN_TRANSIT', NULL,                         NULL);

-- Kỳ kế toán hiện tại
INSERT INTO accounting_periods (period_name, start_date, end_date, status)
VALUES (
    TO_CHAR(NOW(), 'YYYY-MM'),
    DATE_TRUNC('month', NOW())::DATE,
    (DATE_TRUNC('month', NOW()) + INTERVAL '1 month' - INTERVAL '1 day')::DATE,
    'OPEN'
);

-- Tham số hệ thống (updated_by NULL — admin user được tạo sau khi seed)
INSERT INTO system_configs (config_key, config_value, description) VALUES
    ('DEFAULT_CREDIT_LIMIT',                 '50000000',  'Hạn mức công nợ mặc định (VNĐ)'),
    ('DEFAULT_PAYMENT_TERM_DAYS',            '30',        'Kỳ hạn thanh toán mặc định (ngày)'),
    ('CREDIT_HOLD_OVERDUE_DAYS',             '30',        'Số ngày quá hạn trước khi khóa tín dụng'),
    ('CREDIT_UNLOCK_BUFFER_PCT',             '0.8',       'Ngưỡng mở khóa tín dụng (80% credit_limit)'),
    ('STOCKTAKE_APPROVAL_THRESHOLD_MANAGER', '5000000',   'Ngưỡng chênh lệch Trưởng kho duyệt (VNĐ)'),
    ('STOCKTAKE_APPROVAL_THRESHOLD_CEO',     '100000000', 'Ngưỡng chênh lệch CEO duyệt (VNĐ)');

-- =============================================================================
-- Tổng cộng:
--   36 bảng core (theo database.md): users, user_warehouse_assignments,
--     warehouses, warehouse_locations, products, dealers, suppliers, vehicles,
--     drivers, accounting_periods, price_history, batches, inventories,
--     purchase_orders, purchase_order_items, receipts, receipt_items,
--     delivery_orders, delivery_order_items, delivery_order_approvals,
--     delivery_order_warehouse_approvals, trips, trip_delivery_orders,
--     deliveries, transfers, transfer_items, stock_takes, stock_take_items,
--     adjustments, damage_reports, invoices, payment_receipts, credit_notes,
--     debit_notes, system_configs, audit_logs
--    1 bảng extra: stock_alerts (hỗ trợ trigger cảnh báo tồn kho)
-- =============================================================================