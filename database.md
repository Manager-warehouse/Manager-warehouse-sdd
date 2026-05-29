# DATABASE DESIGN — WMS Phúc Anh
## Phiên bản: 1.0 | Cập nhật: 2026-05-29
## Nguồn: AGENTS.md · CLAUDE.md · Kiến trúc phân tầng các Actors.md · README.md · Userstory.md

---

## 1. TỔNG QUAN

| Thông tin | Giá trị |
|---|---|
| RDBMS | PostgreSQL 18 |
| ORM | Spring Data JPA / Hibernate |
| Charset | UTF-8 |
| Timezone | Asia/Ho_Chi_Minh (UTC+7) |
| Đơn vị tiền tệ | VND — DECIMAL(18,2) |
| Tổng số bảng | 36 |
| Nguyên tắc xóa | Soft-delete (`is_active = false` cho master data; `status = CANCELLED` cho giao dịch) |
| Audit | Mọi thao tác ghi Audit Log (actor, action, entity, old/new value, timestamp) |
| Optimistic Locking | Bảng `inventories` dùng cột `version` để tránh ghi đè cạnh tranh |

**Phạm vi hệ thống:**
- 3 kho vật lý: Hải Phòng, Hà Nội, TP.HCM
- 1 kho ảo In-Transit (điều chuyển nội bộ)
- Quarantine Zone (hàng lỗi QC) là loại `warehouse_locations`
- Hệ thống **không có** Module Sản xuất, HRM, Barcode/QR, cổng B2B/B2C
- Toàn bộ giao hàng bằng xe nội bộ Phúc Anh — không phát sinh chi phí 3PL

---

## 2. SƠ ĐỒ DOMAIN (ERD Tổng quan)

```
┌─────────────────────────────────────────────────────────────────────────┐
│  MASTER DATA                                                            │
│  users ──── user_warehouse_assignments ──── warehouses                  │
│               │                               │                        │
│           products                    warehouse_locations               │
│               │                                                        │
│          price_history                dealers ── suppliers              │
│                                        │                               │
│                            vehicles ── drivers                          │
│                                                                         │
│  INVENTORY                                                              │
│  batches ──── inventories ────────────────────────────────────          │
│                   (warehouse + product + batch + location)              │
│                                                                         │
│  INBOUND FLOW                                                           │
│  purchase_orders ── purchase_order_items                                │
│  receipts ── receipt_items                                              │
│                                                                         │
│  OUTBOUND FLOW                                                          │
│  delivery_orders ── delivery_order_items                                │
│      │── delivery_order_approvals (Kế toán)                            │
│      └── delivery_order_warehouse_approvals (Trưởng kho)               │
│                                                                         │
│  TRANSPORT                                                              │
│  trips ── trip_delivery_orders ── delivery_orders                       │
│  deliveries (POD)                                                       │
│                                                                         │
│  TRANSFER                                                               │
│  transfers ── transfer_items                                            │
│                                                                         │
│  STOCK MANAGEMENT                                                       │
│  stock_takes ── stock_take_items                                        │
│  adjustments                                                            │
│  damage_reports                                                         │
│                                                                         │
│  FINANCE                                                                │
│  invoices ── payment_receipts                                           │
│  credit_notes                                                           │
│  debit_notes                                                            │
│                                                                         │
│  ACCOUNTING & CONFIG                                                    │
│  accounting_periods                                                     │
│  system_configs                                                         │
│  audit_logs                                                             │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. BẢNG DỮ LIỆU CHI TIẾT

---

### DOMAIN 1: XÁC THỰC & PHÂN QUYỀN

---

#### 1.1 `users`
> Nguồn: README.md (WarehouseStaff, Dispatcher) · Actors.md (10 Actors) · AGENTS.md (JWT + bcrypt cost 12) · US-WMS-09 (Tài xế đăng nhập smartphone) · US-WMS-21 (RBAC)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `code` | VARCHAR(50) | UNIQUE NOT NULL | Mã nhân viên |
| `full_name` | VARCHAR(255) | NOT NULL | Họ và tên |
| `email` | VARCHAR(255) | UNIQUE NOT NULL | Email đăng nhập |
| `phone` | VARCHAR(20) | | SĐT |
| `password_hash` | VARCHAR(255) | NOT NULL | bcrypt, cost factor ≥ 12 |
| `role` | VARCHAR(50) | NOT NULL | Xem Enum `UserRole` |
| `job_title` | VARCHAR(100) | | Chức danh (cho Storekeeper/WarehouseStaff) |
| `shift` | VARCHAR(50) | | Ca làm việc (sáng/chiều/tối — cho nhân viên kho) |
| `region` | VARCHAR(100) | | Khu vực phụ trách (cho Dispatcher) |
| `is_active` | BOOLEAN | NOT NULL DEFAULT true | Soft-delete |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `UserRole`:** `ADMIN` · `CEO` · `WAREHOUSE_MANAGER` · `STOREKEEPER` · `WAREHOUSE_STAFF` · `ACCOUNTANT` · `ACCOUNTANT_MANAGER` · `PLANNER` · `DISPATCHER` · `DRIVER` · `REPORT_VIEWER`

---

#### 1.2 `user_warehouse_assignments`
> Nguồn: US-WMS-21 (phân quyền theo Chi nhánh Kho) · LESSON-003 (phải check BOTH role AND warehouse)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `user_id` | BIGINT | FK → users(id) NOT NULL | Nhân viên được gán |
| `warehouse_id` | BIGINT | FK → warehouses(id) NOT NULL | Kho được phép truy cập |
| `assigned_by` | BIGINT | FK → users(id) NOT NULL | Admin thực hiện gán |
| `assigned_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Unique:** `(user_id, warehouse_id)`

---

### DOMAIN 2: DANH MỤC NỀN TẢNG (MASTER DATA)

---

#### 2.1 `warehouses`
> Nguồn: README.md (Key Entities: Kho) · CLAUDE.md (ADR-004 In-Transit, ADR-005 Quarantine) · US-WMS-20

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `code` | VARCHAR(20) | UNIQUE NOT NULL | Mã kho (HP / HN / HCM / IN_TRANSIT) |
| `name` | VARCHAR(255) | NOT NULL | Tên kho |
| `address` | TEXT | | Địa chỉ vật lý |
| `phone` | VARCHAR(20) | | SĐT liên hệ |
| `manager_id` | BIGINT | FK → users(id) | Người quản lý kho |
| `type` | VARCHAR(20) | NOT NULL | Xem Enum `WarehouseType` |
| `is_active` | BOOLEAN | NOT NULL DEFAULT true | — |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `WarehouseType`:** `PHYSICAL` · `IN_TRANSIT`

> Lưu ý: In-Transit là kho ảo, không có địa chỉ vật lý. Quarantine Zone được quản lý qua `warehouse_locations.is_quarantine = true`.

---

#### 2.2 `warehouse_locations`
> Nguồn: README.md (Zone/Rack/Shelf/Bin) · US-WMS-20 (sức chứa m³, kg · mã vị trí duy nhất)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `warehouse_id` | BIGINT | FK → warehouses(id) NOT NULL | — |
| `code` | VARCHAR(50) | UNIQUE NOT NULL | Ví dụ: `WH-HP.A.01.1.01` |
| `type` | VARCHAR(10) | NOT NULL | Xem Enum `LocationType` |
| `parent_id` | BIGINT | FK → warehouse_locations(id) | Cấu trúc phân cấp Zone→Rack→Shelf→Bin |
| `capacity_m3` | DECIMAL(10,3) | | Sức chứa tối đa (m³) |
| `capacity_kg` | DECIMAL(10,2) | | Sức chứa tối đa (kg) |
| `current_volume_m3` | DECIMAL(10,3) | DEFAULT 0 | Thể tích đang sử dụng |
| `current_weight_kg` | DECIMAL(10,2) | DEFAULT 0 | Khối lượng đang lưu trữ |
| `is_quarantine` | BOOLEAN | NOT NULL DEFAULT false | Khu cách ly hàng lỗi QC |
| `is_active` | BOOLEAN | NOT NULL DEFAULT true | — |

**Enum `LocationType`:** `ZONE` · `RACK` · `SHELF` · `BIN`

---

#### 2.3 `products`
> Nguồn: README.md (Key Entities: Sản phẩm) · US-WMS-19 · FR-A01 · FR-D01–FR-D04 · AGENTS.md (has_serial)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `sku` | VARCHAR(50) | UNIQUE NOT NULL | Mã sản phẩm duy nhất |
| `name` | VARCHAR(255) | NOT NULL | Tên sản phẩm |
| `unit` | VARCHAR(30) | NOT NULL | Đơn vị tính (cái, thùng, kg,...) |
| `unit_per_pack` | INTEGER | | Số lượng đơn vị nhỏ trong 1 đơn vị đóng gói (quy đổi đơn vị) |
| `description` | TEXT | | Mô tả sản phẩm |
| `image_url` | VARCHAR(500) | | URL hình ảnh |
| `weight_kg` | DECIMAL(10,3) | | Khối lượng (kg/đơn vị) |
| `volume_m3` | DECIMAL(10,5) | | Thể tích (m³/đơn vị) |
| `has_serial` | BOOLEAN | NOT NULL DEFAULT false | Sản phẩm có serial number không |
| `reorder_point` | DECIMAL(10,2) | | Định mức tồn tối thiểu — ngưỡng cảnh báo (US-WMS-26) |
| `is_active` | BOOLEAN | NOT NULL DEFAULT true | Soft-delete theo FR-A01 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

---

#### 2.4 `dealers`
> Nguồn: README.md (Key Entities: Đại lý) · US-WMS-22 · US-WMS-06 (Credit Check) · Actors.md (Credit Check mechanism)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `code` | VARCHAR(50) | UNIQUE NOT NULL | Mã đại lý |
| `name` | VARCHAR(255) | NOT NULL | Tên đại lý |
| `phone` | VARCHAR(20) | | SĐT |
| `default_delivery_address` | TEXT | | Địa chỉ giao hàng mặc định |
| `region` | VARCHAR(100) | | Khu vực phụ trách |
| `payment_term_days` | INTEGER | NOT NULL DEFAULT 30 | Kỳ hạn thanh toán (30 hoặc 60 ngày) |
| `credit_limit` | DECIMAL(18,2) | NOT NULL DEFAULT 0 | Hạn mức tín dụng — chỉ Kế toán trưởng được sửa |
| `current_balance` | DECIMAL(18,2) | NOT NULL DEFAULT 0 | Công nợ hiện tại (tự động cập nhật) |
| `credit_status` | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' | Xem Enum `CreditStatus` |
| `is_active` | BOOLEAN | NOT NULL DEFAULT true | Soft-delete |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `CreditStatus`:** `ACTIVE` · `CREDIT_HOLD`

**Business rules:**
- `CREDIT_HOLD` khi: `current_balance + giá_trị_đơn_mới >= credit_limit` HOẶC có hóa đơn quá hạn > 30 ngày
- Mở khóa về `ACTIVE` khi: `current_balance < credit_limit × 0.8`

---

#### 2.5 `suppliers`
> Nguồn: README.md (Key Entities: Nhà cung cấp) · US-WMS-22

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `code` | VARCHAR(50) | UNIQUE NOT NULL | Mã nhà cung cấp |
| `company_name` | VARCHAR(255) | NOT NULL | Tên công ty |
| `tax_code` | VARCHAR(20) | | Mã số thuế |
| `phone` | VARCHAR(20) | | SĐT |
| `contact_person` | VARCHAR(255) | | Người liên hệ |
| `address` | TEXT | | Địa chỉ |
| `is_active` | BOOLEAN | NOT NULL DEFAULT true | Soft-delete |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

---

#### 2.6 `vehicles`
> Nguồn: README.md (Key Entities: Xe vận chuyển) · US-WMS-23

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `plate_number` | VARCHAR(20) | UNIQUE NOT NULL | Biển số xe |
| `vehicle_type` | VARCHAR(100) | NOT NULL | Loại xe (Xe tải 5 tấn,...) |
| `max_weight_kg` | DECIMAL(10,2) | NOT NULL | Tải trọng tối đa (kg) |
| `max_volume_m3` | DECIMAL(10,3) | | Thể tích thùng xe (m³) |
| `status` | VARCHAR(20) | NOT NULL DEFAULT 'AVAILABLE' | Xem Enum `VehicleStatus` |
| `is_active` | BOOLEAN | NOT NULL DEFAULT true | — |

**Enum `VehicleStatus`:** `AVAILABLE` · `ON_TRIP` · `MAINTENANCE`

---

#### 2.7 `drivers`
> Nguồn: README.md (Key Entities: Tài xế) · US-WMS-23 · US-WMS-09 (Tài xế đăng nhập smartphone)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `user_id` | BIGINT | FK → users(id) NOT NULL | Tài khoản đăng nhập hệ thống |
| `full_name` | VARCHAR(255) | NOT NULL | Họ tên |
| `phone` | VARCHAR(20) | | SĐT |
| `license_number` | VARCHAR(50) | UNIQUE NOT NULL | Số giấy phép lái xe (GPLX) |
| `license_expiry` | DATE | NOT NULL | Ngày hết hạn GPLX |
| `status` | VARCHAR(20) | NOT NULL DEFAULT 'AVAILABLE' | Xem Enum `DriverStatus` |
| `is_active` | BOOLEAN | NOT NULL DEFAULT true | — |

**Enum `DriverStatus`:** `AVAILABLE` · `ON_DELIVERY` · `MAINTENANCE`

---

### DOMAIN 3: GIÁ & LỊCH SỬ GIÁ

---

#### 3.1 `price_history`
> Nguồn: US-WMS-14 (cột được đặt tên chính xác trong tài liệu) · CLAUDE.md (PriceHistory entity) · FR-D01–FR-D04

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `product_id` | BIGINT | FK → products(id) NOT NULL | — |
| `effective_date` | DATE | NOT NULL | Ngày bắt đầu áp dụng giá |
| `end_date` | DATE | | Ngày hết hạn giá (NULL = còn hiệu lực) |
| `cost_price` | DECIMAL(18,2) | NOT NULL | Giá vốn (do Sản xuất cung cấp) |
| `selling_price` | DECIMAL(18,2) | NOT NULL | Giá bán buôn (do Sản xuất cung cấp) |
| `status` | VARCHAR(20) | NOT NULL DEFAULT 'PENDING' | Xem Enum `PriceStatus` |
| `created_by` | BIGINT | FK → users(id) NOT NULL | Kế toán viên tạo bảng giá |
| `approved_by` | BIGINT | FK → users(id) | Kế toán trưởng phê duyệt |
| `approved_at` | TIMESTAMPTZ | | — |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `PriceStatus`:** `PENDING` · `APPROVED`

**Ghi chú:** FR-D03 — Kho **không được** tự thay đổi giá. Chỉ Admin hoặc Warehouse Manager nhập/cập nhật giá sau khi Kế toán trưởng duyệt.

---

### DOMAIN 4: LÔ HÀNG & TỒN KHO

---

#### 4.1 `batches`
> Nguồn: README.md (Key Entities: Lô hàng) · CLAUDE.md (Batch entity · LESSON-002 · ADR-005) · AGENTS.md (Batch rules)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `batch_number` | VARCHAR(100) | UNIQUE NOT NULL | Mã lô hàng |
| `product_id` | BIGINT | FK → products(id) NOT NULL | — |
| `warehouse_id` | BIGINT | FK → warehouses(id) NOT NULL | Kho nhận lô hàng |
| `received_date` | DATE | NOT NULL | Ngày nhập kho (dùng cho FIFO) |
| `expiry_date` | DATE | | Ngày hết hạn (dùng cho FEFO; NULL = không có hạn) |
| `grade` | VARCHAR(1) | NOT NULL | `A` / `B` / `C` — **bất biến sau khi tạo** |
| `quantity` | DECIMAL(10,2) | NOT NULL CHECK (quantity >= 0) | Tổng số lượng còn lại trong lô |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Business rules (AGENTS.md):**
- Mỗi batch chỉ có **1 grade** — khác grade phải tạo batch mới
- `grade` là immutable sau khi tạo
- FEFO: chọn batch theo `expiry_date ASC` (có expiry)
- FIFO: chọn batch theo `received_date ASC` (không có expiry)
- Batch hết hạn không được xuất kho thông thường

---

#### 4.2 `inventories`
> Nguồn: README.md (Key Entities: Tồn kho) · CLAUDE.md (Inventory entity) · AGENTS.md (Inventory rules · optimistic locking) · US-WMS-06 (reserved_qty)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `warehouse_id` | BIGINT | FK → warehouses(id) NOT NULL | — |
| `product_id` | BIGINT | FK → products(id) NOT NULL | — |
| `batch_id` | BIGINT | FK → batches(id) NOT NULL | — |
| `location_id` | BIGINT | FK → warehouse_locations(id) NOT NULL | Bin vị trí cụ thể |
| `total_qty` | DECIMAL(10,2) | NOT NULL DEFAULT 0 CHECK (total_qty >= 0) | **KHÔNG BAO GIỜ âm** |
| `reserved_qty` | DECIMAL(10,2) | NOT NULL DEFAULT 0 CHECK (reserved_qty >= 0) | Đã giữ chỗ cho đơn xuất |
| `cost_price` | DECIMAL(18,2) | NOT NULL | Giá vốn tại thời điểm nhập kho |
| `version` | INTEGER | NOT NULL DEFAULT 0 | Optimistic locking — tăng +1 mỗi lần UPDATE |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Computed (không lưu):** `available_qty = total_qty - reserved_qty`

**Unique:** `(warehouse_id, product_id, batch_id, location_id)`

**Constraint:** `CHECK (total_qty - reserved_qty >= 0)` — available không bao giờ âm

---

### DOMAIN 5: NHẬP KHO (INBOUND)

---

#### 5.1 `purchase_orders`
> Nguồn: README.md (Key Entities: Purchase Order) · US-WMS-02

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `po_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã PO (tự động sinh) |
| `supplier_id` | BIGINT | FK → suppliers(id) NOT NULL | Nhà cung cấp |
| `warehouse_id` | BIGINT | FK → warehouses(id) NOT NULL | Kho nhận hàng dự kiến |
| `expected_receipt_date` | DATE | | Ngày nhận dự kiến |
| `status` | VARCHAR(30) | NOT NULL | Xem Enum `POStatus` |
| `created_by` | BIGINT | FK → users(id) NOT NULL | Planner tạo |
| `notes` | TEXT | | — |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `POStatus`:** `OPEN` · `PARTIALLY_RECEIVED` · `COMPLETED` · `CANCELLED`

---

#### 5.2 `purchase_order_items`
> Nguồn: README.md (PO có "chi tiết")

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `po_id` | BIGINT | FK → purchase_orders(id) NOT NULL | — |
| `product_id` | BIGINT | FK → products(id) NOT NULL | — |
| `expected_qty` | DECIMAL(10,2) | NOT NULL | Số lượng đặt |
| `unit_price` | DECIMAL(18,2) | | Giá đặt hàng |

---

#### 5.3 `receipts`
> Nguồn: README.md (Key Entities: Phiếu nhập kho) · CLAUDE.md (Receipt entity · swimlane 1 · status flow) · US-WMS-02 · US-WMS-03 · US-WMS-05

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `receipt_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã phiếu nhập (tự động sinh) |
| `source_order_code` | VARCHAR(100) | | Mã đơn hàng nguồn (PO hoặc DO hoàn) |
| `type` | VARCHAR(20) | NOT NULL | Xem Enum `ReceiptType` |
| `warehouse_id` | BIGINT | FK → warehouses(id) NOT NULL | Kho nhận |
| `supplier_id` | BIGINT | FK → suppliers(id) | NCC (nullable cho loại RETURN) |
| `dealer_id` | BIGINT | FK → dealers(id) | Đại lý hoàn (nullable cho loại PURCHASE) |
| `contact_person` | VARCHAR(255) | | Người giao hàng |
| `source_channel` | VARCHAR(50) | | Kênh tiếp nhận (Zalo / Email) |
| `status` | VARCHAR(30) | NOT NULL DEFAULT 'PENDING_RECEIPT' | Xem Enum `ReceiptStatus` |
| `approved_by` | BIGINT | FK → users(id) | Trưởng kho phê duyệt |
| `approved_at` | TIMESTAMPTZ | | — |
| `rejection_reason` | TEXT | | Lý do từ chối |
| `document_date` | DATE | NOT NULL | Ngày nghiệp vụ |
| `accounting_period_id` | BIGINT | FK → accounting_periods(id) | Kỳ kế toán |
| `created_by` | BIGINT | FK → users(id) NOT NULL | Planner tạo lệnh |
| `notes` | TEXT | | — |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `ReceiptType`:** `PURCHASE` · `RETURN`
**Enum `ReceiptStatus`:** `PENDING_RECEIPT` · `DRAFT` · `QC_COMPLETED` · `APPROVED` · `REJECTED`

---

#### 5.4 `receipt_items`
> Nguồn: README.md (Phiếu nhập có "chi tiết sản phẩm") · US-WMS-03 (QC Inbound) · AGENTS.md (grade per batch)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `receipt_id` | BIGINT | FK → receipts(id) NOT NULL | — |
| `product_id` | BIGINT | FK → products(id) NOT NULL | — |
| `batch_id` | BIGINT | FK → batches(id) | Gán sau khi QC (có thể NULL trước QC) |
| `location_id` | BIGINT | FK → warehouse_locations(id) | Bin cất hàng (sau putaway) |
| `expected_qty` | DECIMAL(10,2) | NOT NULL | Số lượng dự kiến |
| `actual_qty` | DECIMAL(10,2) | | Số lượng thực đếm (Thủ kho nhập) |
| `qc_passed_qty` | DECIMAL(10,2) | | Số lượng QC đạt |
| `qc_failed_qty` | DECIMAL(10,2) | | Số lượng QC lỗi → Quarantine Zone |
| `qc_result` | VARCHAR(20) | | Xem Enum `QCResult` |
| `qc_failure_reason` | TEXT | | Lý do lỗi chi tiết (vỡ, móp, sai quy cách,...) |
| `grade` | VARCHAR(1) | | A / B / C — grade của lô hàng này |
| `unit_cost` | DECIMAL(18,2) | | Giá vốn đơn vị tại thời điểm nhập |
| `serial_number` | VARCHAR(100) | | Serial number (nếu product.has_serial = true) |

**Enum `QCResult`:** `PENDING` · `PASSED` · `FAILED` · `PARTIAL`

---

### DOMAIN 6: XUẤT KHO (OUTBOUND)

---

#### 6.1 `delivery_orders`
> Nguồn: CLAUDE.md (Issue entity · swimlane 2 · status flow) · README.md (SaleOrder · Key Entities) · Actors.md (Luồng trạng thái đơn hàng xuất) · US-WMS-06 · US-WMS-07

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `do_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã đơn xuất (tự động sinh) |
| `dealer_id` | BIGINT | FK → dealers(id) NOT NULL | Đại lý nhận hàng |
| `warehouse_id` | BIGINT | FK → warehouses(id) NOT NULL | Kho xuất |
| `type` | VARCHAR(30) | NOT NULL | Xem Enum `DeliveryOrderType` |
| `expected_delivery_date` | DATE | | Ngày giao dự kiến |
| `status` | VARCHAR(30) | NOT NULL DEFAULT 'NEW' | Xem Enum `DeliveryOrderStatus` |
| `created_by` | BIGINT | FK → users(id) NOT NULL | Planner tạo đơn |
| `cancel_reason` | TEXT | | Lý do hủy |
| `document_date` | DATE | NOT NULL | Ngày nghiệp vụ |
| `accounting_period_id` | BIGINT | FK → accounting_periods(id) | Kỳ kế toán |
| `notes` | TEXT | | — |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `DeliveryOrderType`:** `SALE` · `DELIVERY` · `ADJUSTMENT`
**Enum `DeliveryOrderStatus`:** `NEW` · `PICKING` · `READY_TO_SHIP` · `IN_TRANSIT` · `OUT_FOR_DELIVERY` · `DELIVERED` · `COMPLETED` · `CLOSED` · `CANCELLED`

---

#### 6.2 `delivery_order_items`
> Nguồn: README.md (SaleOrder có "chi tiết") · US-WMS-06 (reserved_qty · FEFO/FIFO) · US-WMS-07

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `do_id` | BIGINT | FK → delivery_orders(id) NOT NULL | — |
| `product_id` | BIGINT | FK → products(id) NOT NULL | — |
| `batch_id` | BIGINT | FK → batches(id) | Lô được chọn (FEFO/FIFO) |
| `location_id` | BIGINT | FK → warehouse_locations(id) | Bin xuất hàng |
| `requested_qty` | DECIMAL(10,2) | NOT NULL | Số lượng yêu cầu |
| `reserved_qty` | DECIMAL(10,2) | NOT NULL DEFAULT 0 | Số lượng đang giữ chỗ |
| `issued_qty` | DECIMAL(10,2) | NOT NULL DEFAULT 0 | Số lượng thực tế xuất |
| `unit_price` | DECIMAL(18,2) | | Giá bán tại ngày giao (tra từ price_history) |
| `serial_number` | VARCHAR(100) | | Serial number (nếu product.has_serial = true) |

---

#### 6.3 `delivery_order_approvals`
> Nguồn: README.md (Key Entities: SaleOrderApproval — Kế toán duyệt, ảnh hợp đồng bắt buộc)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `do_id` | BIGINT | FK → delivery_orders(id) NOT NULL | — |
| `approver_id` | BIGINT | FK → users(id) NOT NULL | Kế toán thực hiện duyệt |
| `result` | VARCHAR(20) | NOT NULL | Xem Enum `ApprovalResult` |
| `contract_image_url` | VARCHAR(500) | | Ảnh hợp đồng — **bắt buộc khi result = APPROVED** |
| `rejection_reason` | TEXT | | **Bắt buộc khi result = REJECTED** |
| `approved_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `ApprovalResult`:** `APPROVED` · `REJECTED`

**Ghi chú:** Tạo record cho cả trường hợp APPROVED lẫn REJECTED để đảm bảo audit trail đầy đủ.

---

#### 6.4 `delivery_order_warehouse_approvals`
> Nguồn: README.md (Key Entities: SaleOrderWarehouseApproval — Kho duyệt)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `do_id` | BIGINT | FK → delivery_orders(id) NOT NULL | — |
| `approver_id` | BIGINT | FK → users(id) NOT NULL | Warehouse Manager duyệt |
| `result` | VARCHAR(20) | NOT NULL | `APPROVED` / `REJECTED` |
| `notes` | TEXT | | Ghi chú |
| `approved_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

---

### DOMAIN 7: VẬN TẢI (TRANSPORT)

---

#### 7.1 `trips`
> Nguồn: US-WMS-08 (Trip Log / Chuyến xe · kiểm tra tải trọng · Dispatcher lập)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `trip_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã chuyến xe (tự động sinh) |
| `vehicle_id` | BIGINT | FK → vehicles(id) NOT NULL | Xe nội bộ Phúc Anh |
| `driver_id` | BIGINT | FK → drivers(id) NOT NULL | Tài xế |
| `dispatcher_id` | BIGINT | FK → users(id) NOT NULL | Dispatcher lập chuyến |
| `planned_date` | DATE | NOT NULL | Ngày giao dự kiến |
| `status` | VARCHAR(20) | NOT NULL DEFAULT 'PLANNED' | Xem Enum `TripStatus` |
| `total_weight_kg` | DECIMAL(10,2) | DEFAULT 0 | Tổng khối lượng hàng (kiểm tra tải trọng) |
| `total_volume_m3` | DECIMAL(10,3) | DEFAULT 0 | Tổng thể tích hàng |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `TripStatus`:** `PLANNED` · `IN_TRANSIT` · `COMPLETED`

---

#### 7.2 `trip_delivery_orders`
> Nguồn: US-WMS-08 (Gom nhiều Delivery Orders vào 1 Chuyến xe · sắp xếp Stop Order)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `trip_id` | BIGINT | FK → trips(id) NOT NULL | — |
| `do_id` | BIGINT | FK → delivery_orders(id) NOT NULL UNIQUE | Mỗi DO chỉ thuộc 1 chuyến |
| `stop_order` | INTEGER | NOT NULL | Thứ tự giao hàng trong chuyến |

**Unique:** `(trip_id, stop_order)`

---

#### 7.3 `deliveries`
> Nguồn: README.md (Key Entities: Vận đơn) · US-WMS-09 (POD: Hình ảnh + Chữ ký + Timestamp · giao thất bại)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `delivery_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã vận đơn |
| `do_id` | BIGINT | FK → delivery_orders(id) NOT NULL | Phiếu xuất kho |
| `trip_id` | BIGINT | FK → trips(id) | Chuyến xe thực hiện |
| `vehicle_id` | BIGINT | FK → vehicles(id) NOT NULL | Xe giao hàng |
| `driver_id` | BIGINT | FK → drivers(id) NOT NULL | Tài xế |
| `status` | VARCHAR(30) | NOT NULL | Xem Enum `DeliveryStatus` |
| `pod_image_url` | VARCHAR(500) | | Ảnh hàng hóa bàn giao |
| `pod_signature_url` | VARCHAR(500) | | Ảnh chữ ký Đại lý |
| `pod_timestamp` | TIMESTAMPTZ | | Thời điểm giao hàng thành công |
| `failure_reason` | TEXT | | Lý do giao thất bại (vắng mặt / từ chối / sai địa chỉ) |
| `delivered_at` | TIMESTAMPTZ | | Thời điểm xác nhận giao thành công |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `DeliveryStatus`:** `PENDING` · `IN_TRANSIT` · `OUT_FOR_DELIVERY` · `DELIVERED` · `RETURNED`

**Ghi chú:** Khi `status = RETURNED` → hệ thống tự động tạo `receipts` loại `RETURN` vào Quarantine Zone.

---

### DOMAIN 8: ĐIỀU CHUYỂN (TRANSFER)

---

#### 8.1 `transfers`
> Nguồn: README.md (Key Entities: Phiếu điều chuyển) · CLAUDE.md (Transfer entity · ADR-004 In-Transit · swimlane 3) · US-WMS-12

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `transfer_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã phiếu điều chuyển |
| `source_warehouse_id` | BIGINT | FK → warehouses(id) NOT NULL | Kho nguồn |
| `destination_warehouse_id` | BIGINT | FK → warehouses(id) NOT NULL | Kho đích |
| `status` | VARCHAR(40) | NOT NULL DEFAULT 'NEW' | Xem Enum `TransferStatus` |
| `created_by` | BIGINT | FK → users(id) NOT NULL | Planner tạo |
| `approved_by` | BIGINT | FK → users(id) | Trưởng kho nguồn phê duyệt |
| `approved_at` | TIMESTAMPTZ | | — |
| `confirmed_by` | BIGINT | FK → users(id) | Trưởng kho đích xác nhận nhận hàng |
| `confirmed_at` | TIMESTAMPTZ | | — |
| `planned_date` | DATE | | Ngày điều chuyển dự kiến |
| `actual_received_date` | DATE | | Ngày nhận hàng thực tế |
| `discrepancy_reason` | TEXT | | Lý do chênh lệch (nếu COMPLETED_WITH_DISCREPANCY) |
| `document_date` | DATE | NOT NULL | Ngày nghiệp vụ |
| `accounting_period_id` | BIGINT | FK → accounting_periods(id) | Kỳ kế toán |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `TransferStatus`:** `NEW` · `APPROVED` · `IN_TRANSIT` · `COMPLETED` · `COMPLETED_WITH_DISCREPANCY` · `CANCELLED`

**Nghiệp vụ:**
- Khi `IN_TRANSIT`: trừ tồn kho nguồn, cộng vào kho ảo `IN_TRANSIT`
- Khi `COMPLETED`: trừ kho ảo `IN_TRANSIT`, cộng vào kho đích
- Khi `COMPLETED_WITH_DISCREPANCY`: tạo thêm `adjustments` bù trừ chênh lệch

---

#### 8.2 `transfer_items`
> Nguồn: README.md (Phiếu điều chuyển có "chi tiết") · US-WMS-12 (quantity_sent vs quantity_received)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `transfer_id` | BIGINT | FK → transfers(id) NOT NULL | — |
| `product_id` | BIGINT | FK → products(id) NOT NULL | — |
| `batch_id` | BIGINT | FK → batches(id) NOT NULL | — |
| `source_location_id` | BIGINT | FK → warehouse_locations(id) | Bin xuất tại kho nguồn |
| `destination_location_id` | BIGINT | FK → warehouse_locations(id) | Bin đặt tại kho đích |
| `planned_qty` | DECIMAL(10,2) | NOT NULL | Số lượng dự kiến điều chuyển |
| `sent_qty` | DECIMAL(10,2) | | Số lượng Thủ kho nguồn xác nhận xuất |
| `received_qty` | DECIMAL(10,2) | | Số lượng Trưởng kho đích xác nhận nhận |
| `variance_qty` | DECIMAL(10,2) | | `= received_qty - sent_qty` (âm = thiếu) |

---

### DOMAIN 9: KIỂM KÊ & ĐIỀU CHỈNH

---

#### 9.1 `stock_takes`
> Nguồn: README.md (Key Entities: Phiếu kiểm kê) · US-WMS-13 · Actors.md (swimlane 6 · thẩm quyền duyệt)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `stock_take_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã phiếu kiểm kê |
| `warehouse_id` | BIGINT | FK → warehouses(id) NOT NULL | Kho kiểm kê |
| `conducted_by` | BIGINT | FK → users(id) NOT NULL | Thủ kho thực hiện |
| `approved_by` | BIGINT | FK → users(id) | Trưởng kho hoặc CEO phê duyệt |
| `approved_at` | TIMESTAMPTZ | | — |
| `status` | VARCHAR(30) | NOT NULL DEFAULT 'DRAFT' | Xem Enum `StockTakeStatus` |
| `total_variance_value` | DECIMAL(18,2) | DEFAULT 0 | Tổng giá trị chênh lệch (xác định người duyệt) |
| `stock_take_date` | DATE | NOT NULL | Ngày kiểm kê |
| `document_date` | DATE | NOT NULL | Ngày nghiệp vụ |
| `accounting_period_id` | BIGINT | FK → accounting_periods(id) | Kỳ kế toán |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `StockTakeStatus`:** `DRAFT` · `IN_PROGRESS` · `PENDING_APPROVAL` · `APPROVED` · `CANCELLED`

**Thẩm quyền duyệt (Actors.md):**
- Giá trị lệch **5 – 100 triệu VNĐ** → Trưởng kho duyệt
- Giá trị lệch **> 100 triệu VNĐ** hoặc lỗi do nhân viên → CEO duyệt

---

#### 9.2 `stock_take_items`
> Nguồn: README.md (Phiếu kiểm kê có "chi tiết chênh lệch") · US-WMS-13 (Variance = Thực tế - Hệ thống)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `stock_take_id` | BIGINT | FK → stock_takes(id) NOT NULL | — |
| `product_id` | BIGINT | FK → products(id) NOT NULL | — |
| `batch_id` | BIGINT | FK → batches(id) NOT NULL | — |
| `location_id` | BIGINT | FK → warehouse_locations(id) NOT NULL | — |
| `system_qty` | DECIMAL(10,2) | NOT NULL | Số lượng theo hệ thống |
| `actual_qty` | DECIMAL(10,2) | NOT NULL | Số lượng thực đếm |
| `variance_qty` | DECIMAL(10,2) | NOT NULL | `= actual_qty - system_qty` |
| `variance_value` | DECIMAL(18,2) | NOT NULL | `= variance_qty × cost_price` |
| `notes` | TEXT | | — |

---

#### 9.3 `adjustments`
> Nguồn: README.md (Key Entities: Phiếu điều chỉnh) · US-WMS-13 · US-WMS-17 (Adjustment Voucher cho kỳ đã chốt) · US-WMS-04 (tiêu hủy hàng lỗi · RTV)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `adjustment_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã phiếu điều chỉnh |
| `warehouse_id` | BIGINT | FK → warehouses(id) NOT NULL | Kho liên quan |
| `product_id` | BIGINT | FK → products(id) NOT NULL | Sản phẩm điều chỉnh |
| `batch_id` | BIGINT | FK → batches(id) | Lô hàng (nếu áp dụng) |
| `location_id` | BIGINT | FK → warehouse_locations(id) | Vị trí kho |
| `quantity_adjustment` | DECIMAL(10,2) | NOT NULL | Số lượng (dương = tăng, âm = giảm) |
| `type` | VARCHAR(30) | NOT NULL | Xem Enum `AdjustmentType` |
| `reference_id` | BIGINT | | ID chứng từ liên quan (stock_take, transfer,...) |
| `reference_type` | VARCHAR(50) | | Loại chứng từ liên quan |
| `reason` | TEXT | NOT NULL | Lý do điều chỉnh |
| `approved_by` | BIGINT | FK → users(id) | Người phê duyệt |
| `approved_at` | TIMESTAMPTZ | | — |
| `document_date` | DATE | NOT NULL | Ngày nghiệp vụ gốc |
| `accounting_period_id` | BIGINT | FK → accounting_periods(id) | Kỳ kế toán (kỳ mở hiện tại) |
| `created_by` | BIGINT | FK → users(id) NOT NULL | — |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `AdjustmentType`:** `STOCK_TAKE` · `TRANSFER_DISCREPANCY` · `DISPOSAL` · `RETURN_TO_VENDOR` · `CORRECTION_VOUCHER`

---

#### 9.4 `damage_reports`
> Nguồn: README.md (Key Entities: Báo cáo hư hỏng) · FR-F02

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `report_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã báo cáo hư hỏng |
| `warehouse_id` | BIGINT | FK → warehouses(id) NOT NULL | Kho phát sinh |
| `product_id` | BIGINT | FK → products(id) NOT NULL | Sản phẩm hư hỏng |
| `batch_id` | BIGINT | FK → batches(id) | Lô hàng |
| `quantity` | DECIMAL(10,2) | NOT NULL | Số lượng hư hỏng |
| `cause` | TEXT | NOT NULL | Nguyên nhân hư hỏng |
| `image_url` | VARCHAR(500) | | Hình ảnh ghi nhận |
| `reported_by` | BIGINT | FK → users(id) NOT NULL | Người lập báo cáo |
| `report_date` | DATE | NOT NULL | Ngày ghi nhận |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

---

### DOMAIN 10: TÀI CHÍNH & CÔNG NỢ

---

#### 10.1 `invoices`
> Nguồn: CLAUDE.md (Invoice entity) · US-WMS-10 · Actors.md (Finance Cycle)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `invoice_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã hóa đơn (tự động sinh) |
| `do_id` | BIGINT | FK → delivery_orders(id) NOT NULL | Đơn hàng đã giao thành công |
| `dealer_id` | BIGINT | FK → dealers(id) NOT NULL | Đại lý |
| `total_amount` | DECIMAL(18,2) | NOT NULL | Tổng giá trị (theo bảng giá tại ngày giao) |
| `issue_date` | DATE | NOT NULL | Ngày xuất hóa đơn |
| `due_date` | DATE | NOT NULL | Hạn thanh toán (Net 30/60 theo hồ sơ Đại lý) |
| `status` | VARCHAR(20) | NOT NULL DEFAULT 'UNPAID' | Xem Enum `InvoiceStatus` |
| `created_by` | BIGINT | FK → users(id) NOT NULL | Kế toán viên lập |
| `document_date` | DATE | NOT NULL | Ngày nghiệp vụ |
| `accounting_period_id` | BIGINT | FK → accounting_periods(id) | Kỳ kế toán |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `InvoiceStatus`:** `UNPAID` · `PARTIALLY_PAID` · `PAID`

**Tự động:** Sau khi tạo invoice → `dealers.current_balance += total_amount` → kiểm tra Credit Limit.

---

#### 10.2 `payment_receipts`
> Nguồn: CLAUDE.md (PaymentReceipt entity) · US-WMS-15 · Actors.md (Finance Cycle - Thu nợ)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `payment_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã phiếu thu (tự động sinh) |
| `dealer_id` | BIGINT | FK → dealers(id) NOT NULL | Đại lý thanh toán |
| `invoice_id` | BIGINT | FK → invoices(id) NOT NULL | Hóa đơn cần cấn trừ |
| `amount` | DECIMAL(18,2) | NOT NULL | Số tiền đã thu |
| `payment_date` | DATE | NOT NULL | Ngày thu tiền |
| `payment_method` | VARCHAR(30) | NOT NULL | Xem Enum `PaymentMethod` |
| `created_by` | BIGINT | FK → users(id) NOT NULL | Kế toán viên ghi nhận |
| `document_date` | DATE | NOT NULL | Ngày nghiệp vụ |
| `accounting_period_id` | BIGINT | FK → accounting_periods(id) | Kỳ kế toán |
| `notes` | TEXT | | — |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Enum `PaymentMethod`:** `BANK_TRANSFER` · `CASH`

**Tự động:** Sau khi tạo → `dealers.current_balance -= amount` → kiểm tra mở khóa Credit (`< credit_limit × 0.8`).

---

#### 10.3 `credit_notes`
> Nguồn: CLAUDE.md (CreditNote entity) · US-WMS-24 (hàng hoàn trả từ Đại lý)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `credit_note_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã phiếu ghi giảm công nợ |
| `dealer_id` | BIGINT | FK → dealers(id) NOT NULL | Đại lý hoàn hàng |
| `receipt_id` | BIGINT | FK → receipts(id) | Phiếu nhập hàng hoàn liên quan |
| `amount` | DECIMAL(18,2) | NOT NULL | Giá trị hàng hoàn |
| `reason` | TEXT | NOT NULL | Lý do hoàn trả |
| `created_by` | BIGINT | FK → users(id) NOT NULL | Kế toán viên lập |
| `document_date` | DATE | NOT NULL | Ngày nghiệp vụ |
| `accounting_period_id` | BIGINT | FK → accounting_periods(id) | Kỳ kế toán |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Tự động:** `dealers.current_balance -= amount`

---

#### 10.4 `debit_notes`
> Nguồn: CLAUDE.md (DebitNote entity) · US-WMS-04 (hàng lỗi QC từ NCC)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `debit_note_number` | VARCHAR(50) | UNIQUE NOT NULL | Mã phiếu đòi bồi hoàn NCC |
| `supplier_id` | BIGINT | FK → suppliers(id) NOT NULL | Nhà cung cấp có hàng lỗi |
| `receipt_id` | BIGINT | FK → receipts(id) | Phiếu nhập liên quan |
| `failed_qty` | DECIMAL(10,2) | NOT NULL | Số lượng hàng lỗi |
| `amount` | DECIMAL(18,2) | NOT NULL | Giá trị yêu cầu bồi hoàn |
| `reason` | TEXT | NOT NULL | Lý do (mô tả lỗi chi tiết) |
| `created_by` | BIGINT | FK → users(id) NOT NULL | Kế toán viên lập |
| `document_date` | DATE | NOT NULL | Ngày nghiệp vụ |
| `accounting_period_id` | BIGINT | FK → accounting_periods(id) | Kỳ kế toán |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

---

### DOMAIN 11: KỲ KẾ TOÁN

---

#### 11.1 `accounting_periods`
> Nguồn: US-WMS-17 (Chốt sổ Kế toán · khóa cứng kỳ quá khứ · CLOSED)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `period_name` | VARCHAR(20) | UNIQUE NOT NULL | Ví dụ: `2026-05` |
| `start_date` | DATE | NOT NULL | Ngày bắt đầu kỳ |
| `end_date` | DATE | NOT NULL | Ngày kết thúc kỳ |
| `status` | VARCHAR(10) | NOT NULL DEFAULT 'OPEN' | `OPEN` · `CLOSED` |
| `closed_by` | BIGINT | FK → users(id) | Kế toán trưởng chốt sổ |
| `closed_at` | TIMESTAMPTZ | | — |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Business rule (US-WMS-17):**
- Khi `status = CLOSED` → **khóa cứng** toàn bộ chứng từ có `document_date` trong kỳ
- Chứng từ nhập muộn → hạch toán vào kỳ OPEN hiện tại, giữ `document_date` gốc
- Sai sót sau chốt → tạo `adjustments` với `type = CORRECTION_VOUCHER` tại kỳ OPEN

---

### DOMAIN 12: CẤU HÌNH & AUDIT

---

#### 12.1 `system_configs`
> Nguồn: US-WMS-01 (Admin cấu hình tham số hệ thống: hạn mức công nợ mặc định, tồn kho tối thiểu, kỳ hạn thanh toán, bảng định mức phê duyệt)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `config_key` | VARCHAR(100) | UNIQUE NOT NULL | Tên tham số |
| `config_value` | TEXT | NOT NULL | Giá trị |
| `description` | TEXT | | Mô tả mục đích |
| `updated_by` | BIGINT | FK → users(id) NOT NULL | Admin thực hiện thay đổi |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | — |

**Các config_key tiêu chuẩn:**

| config_key | Ví dụ | Mô tả |
|---|---|---|
| `DEFAULT_CREDIT_LIMIT` | `50000000` | Hạn mức công nợ mặc định (VNĐ) |
| `DEFAULT_PAYMENT_TERM_DAYS` | `30` | Kỳ hạn thanh toán mặc định |
| `CREDIT_HOLD_OVERDUE_DAYS` | `30` | Số ngày quá hạn trước khi khóa tín dụng |
| `CREDIT_UNLOCK_BUFFER_PCT` | `0.8` | Ngưỡng mở khóa (80% credit_limit) |
| `STOCKTAKE_APPROVAL_THRESHOLD_MANAGER` | `5000000` | Dưới ngưỡng này không cần duyệt |
| `STOCKTAKE_APPROVAL_THRESHOLD_CEO` | `100000000` | Trên ngưỡng này CEO duyệt |

---

#### 12.2 `audit_logs`
> Nguồn: README.md (Key Entities: Audit Log) · Actors.md (cấu trúc bắt buộc) · FR-H03 · AGENTS.md (audit logging bắt buộc)

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PK | — |
| `actor_id` | BIGINT | FK → users(id) | Người thực hiện (NULL = system job) |
| `actor_role` | VARCHAR(50) | NOT NULL | Vai trò tại thời điểm thực hiện |
| `action` | VARCHAR(50) | NOT NULL | `CREATE` · `UPDATE` · `APPROVE` · `REJECT` · `CANCEL` · `DELETE` |
| `entity_type` | VARCHAR(100) | NOT NULL | Tên bảng bị ảnh hưởng |
| `entity_id` | BIGINT | NOT NULL | ID bản ghi bị ảnh hưởng |
| `old_value` | JSONB | | Dữ liệu trước thay đổi |
| `new_value` | JSONB | | Dữ liệu sau thay đổi |
| `timestamp` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Thời gian chính xác đến ms |
| `ip_address` | VARCHAR(45) | | IP của client |

**Truy cập (FR-H03):** Admin xem toàn bộ log; Warehouse Manager chỉ xem log của kho mình.

---

## 4. ENUM REFERENCE TỔNG HỢP

| Enum | Giá trị |
|---|---|
| `UserRole` | ADMIN · CEO · WAREHOUSE_MANAGER · STOREKEEPER · WAREHOUSE_STAFF · ACCOUNTANT · ACCOUNTANT_MANAGER · PLANNER · DISPATCHER · DRIVER · REPORT_VIEWER |
| `WarehouseType` | PHYSICAL · IN_TRANSIT |
| `LocationType` | ZONE · RACK · SHELF · BIN |
| `VehicleStatus` | AVAILABLE · ON_TRIP · MAINTENANCE |
| `DriverStatus` | AVAILABLE · ON_DELIVERY · MAINTENANCE |
| `CreditStatus` | ACTIVE · CREDIT_HOLD |
| `PriceStatus` | PENDING · APPROVED |
| `POStatus` | OPEN · PARTIALLY_RECEIVED · COMPLETED · CANCELLED |
| `ReceiptType` | PURCHASE · RETURN |
| `ReceiptStatus` | PENDING_RECEIPT · DRAFT · QC_COMPLETED · APPROVED · REJECTED |
| `QCResult` | PENDING · PASSED · FAILED · PARTIAL |
| `DeliveryOrderType` | SALE · DELIVERY · ADJUSTMENT |
| `DeliveryOrderStatus` | NEW · PICKING · READY_TO_SHIP · IN_TRANSIT · OUT_FOR_DELIVERY · DELIVERED · COMPLETED · CLOSED · CANCELLED |
| `ApprovalResult` | APPROVED · REJECTED |
| `TripStatus` | PLANNED · IN_TRANSIT · COMPLETED |
| `DeliveryStatus` | PENDING · IN_TRANSIT · OUT_FOR_DELIVERY · DELIVERED · RETURNED |
| `TransferStatus` | NEW · APPROVED · IN_TRANSIT · COMPLETED · COMPLETED_WITH_DISCREPANCY · CANCELLED |
| `StockTakeStatus` | DRAFT · IN_PROGRESS · PENDING_APPROVAL · APPROVED · CANCELLED |
| `AdjustmentType` | STOCK_TAKE · TRANSFER_DISCREPANCY · DISPOSAL · RETURN_TO_VENDOR · CORRECTION_VOUCHER |
| `InvoiceStatus` | UNPAID · PARTIALLY_PAID · PAID |
| `PaymentMethod` | BANK_TRANSFER · CASH |
| `AccountingPeriodStatus` | OPEN · CLOSED |

---

## 5. BUSINESS RULES TẠI DB LEVEL

| Rule | Bảng / Cột | Constraint |
|---|---|---|
| Tồn kho không âm | `inventories.total_qty` | `CHECK (total_qty >= 0)` |
| Available không âm | `inventories` | `CHECK (total_qty - reserved_qty >= 0)` |
| Grade bất biến | `batches.grade` | Không cập nhật sau INSERT (enforce tại Service layer) |
| Unique lô theo kho-sản phẩm-vị trí | `inventories` | `UNIQUE (warehouse_id, product_id, batch_id, location_id)` |
| Optimistic locking | `inventories.version` | Tăng +1 mỗi UPDATE, check trước khi ghi |
| Một DO chỉ thuộc 1 chuyến xe | `trip_delivery_orders.do_id` | `UNIQUE` |
| Một user–kho gán một lần | `user_warehouse_assignments` | `UNIQUE (user_id, warehouse_id)` |
| Kỳ đã CLOSED không cho phép sửa | `accounting_periods.status` | Enforce tại Service layer — kiểm tra `accounting_period.status = OPEN` trước mọi ghi |
| Ảnh hợp đồng bắt buộc khi Kế toán duyệt | `delivery_order_approvals.contract_image_url` | NOT NULL khi `result = APPROVED` (enforce tại Service layer) |
| Credit Check trước khi tạo DO | `dealers.credit_status` | Kiểm tra tại Service layer trước khi INSERT delivery_orders |

---

## 6. LUỒNG DỮ LIỆU THEO QUY TRÌNH

### Luồng Nhập hàng
```
purchase_orders → receipts (PENDING_RECEIPT)
               → receipt_items (QC → PASSED/FAILED)
               → inventories (cộng total_qty)
               → batches (tạo lô mới)
               → warehouse_locations (Quarantine nếu FAILED)
               → debit_notes (nếu RTV → Return to Vendor)
               → adjustments (DISPOSAL nếu tiêu hủy)
               → audit_logs
```

### Luồng Xuất hàng & Giao hàng
```
delivery_orders (NEW) → dealers [Credit Check]
                     → delivery_order_approvals (Kế toán)
                     → delivery_order_warehouse_approvals (Kho)
                     → delivery_order_items (PICKING → reserved_qty)
                     → trips + trip_delivery_orders (Dispatcher)
                     → deliveries (POD → DELIVERED / RETURNED)
                     → inventories (trừ total_qty, xóa reserved_qty)
                     → invoices → dealers.current_balance
                     → payment_receipts → dealers.current_balance
                     → audit_logs
```

### Luồng Điều chuyển
```
transfers (NEW) → approved_by Trưởng kho nguồn
               → transfer_items (sent_qty → trừ kho nguồn)
               → inventories kho IN_TRANSIT (cộng)
               → confirmed_by Trưởng kho đích
               → inventories kho đích (cộng)
               → inventories IN_TRANSIT (trừ)
               → adjustments (TRANSFER_DISCREPANCY nếu lệch)
               → audit_logs
```

### Luồng Tài chính (Credit Cycle)
```
delivery_orders [DELIVERED]
  → invoices [UNPAID] → dealers.current_balance += amount
  → [Daily Job] quá hạn > 30 ngày → dealers.credit_status = CREDIT_HOLD
  → payment_receipts → dealers.current_balance -= amount
  → IF current_balance < credit_limit × 0.8 → credit_status = ACTIVE
  → invoices [PAID]
  → delivery_orders [CLOSED]
```

---

*Tổng cộng: 36 bảng | Nguồn dữ liệu: AGENTS.md · CLAUDE.md · Kiến trúc phân tầng các Actors.md · README.md · Userstory.md*
