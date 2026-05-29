# Feature Specification: Danh mục Nền tảng (Master Data Management)

**Spec ID**: 002-master-data
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-19, US-WMS-20, US-WMS-22, US-WMS-23

---

## 1. Context and Goal

Hệ thống WMS vận hành dựa trên các danh mục nền tảng: sản phẩm (SKU), kho bãi
và vị trí lưu trữ (Bin Location), đối tác (Đại lý, Nhà cung cấp), phương tiện
vận tải nội bộ và tài xế. Các danh mục này là xương sống cho mọi nghiệp vụ
nhập, xuất, điều chuyển, kiểm kê.

**Goal:** Xây dựng module quản lý danh mục tập trung, cho phép CRUD, soft-delete,
và kiểm tra ràng buộc nghiệp vụ (sức chứa Bin, quy đổi đơn vị tính, unique SKU).

## 2. Actors

| Actor | Feature |
|-------|---------|
| Planner | Quản lý SKU, sản phẩm |
| Kế toán viên | Quản lý Đại lý |
| Kế toán trưởng | Thiết lập Credit Limit cho Đại lý |
| System Admin | Quản lý Nhà cung cấp |
| Dispatcher | Quản lý Xe tải, Tài xế |
| Thủ kho / Trưởng kho | Cấu hình Bin Location |

## 3. Functional Requirements (EARS)

**Ubiquitous:**
- The system SHALL always generate a unique code for every master data entity
  (SKU, warehouse, dealer, supplier, vehicle).
- The system SHALL always enforce soft-delete via `is_active = false` for all
  master data; the system SHALL NOT physically DELETE master data records.
- The system SHALL always validate that a product SKU is unique before creation.

**Event-driven:**
- WHEN a user creates a product, the system SHALL require: SKU, name, unit,
  and optional fields: category, has_serial, has_expiry, weight, volume.
- WHEN a user creates a Bin Location, the system SHALL auto-generate the
  bin_code from format: `{warehouse_code}-{zone}-{aisle}-{rack}-{level}`.
- WHEN a user attempts to put away goods into a Bin, the system SHALL verify:
  `current_occupied + incoming_qty ≤ bin_capacity` for BOTH volume and weight.
- WHEN a Kế toán trưởng creates or updates a dealer's credit_limit, the system
  SHALL log this change with values before/after.

**State-driven:**
- WHILE a product is `is_active = false`, the system SHALL prevent new
  transactions (receipt, issue, transfer) referencing that product.
- WHILE a dealer is `is_active = false`, the system SHALL prevent creating
  new delivery orders for that dealer.
- WHILE a vehicle status is "Đang bảo trì", the system SHALL exclude it from
  trip assignment dropdown.

**Optional:**
- WHERE a product has `has_serial = true`, the system SHALL require serial
  number input during receipt and issue operations.
- WHERE a product has `has_expiry = true`, the system SHALL apply FEFO
  batch selection logic.

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Product search response time | ≤ 500ms for 1000+ SKUs |
| NFR-002 | Master data CRUD response (p95) | ≤ 300ms |
| NFR-003 | Bin capacity validation overhead | ≤ 50ms |
| NFR-004 | Support product import from Excel | ≤ 5s for 1000 rows |

## 5. Data Model

### Product
- `id`, `sku` (UNIQUE), `name`, `category`, `unit`, `has_serial`, `has_expiry`,
  `weight_kg`, `volume_m3`, `is_active`, `version`

### Warehouse
- `id`, `code` (UNIQUE: HP, HN, HCM), `name`, `address`, `is_active`, `version`

### BinLocation
- `id`, `warehouse_id` (FK), `zone`, `aisle`, `rack`, `level`, `bin_code` (UNIQUE),
  `capacity_qty`, `capacity_weight_kg`, `occupied_qty`, `occupied_weight_kg`,
  `is_active`, `version`

### Dealer
- `id`, `code` (UNIQUE), `name`, `credit_limit`, `current_balance`,
  `credit_status` (ACTIVE/CREDIT_HOLD), `payment_terms` (NET30/NET60),
  `is_active`, `version`

### Supplier
- `id`, `code` (UNIQUE), `company_name`, `tax_code`, `contact_person`,
  `phone`, `address`, `is_active`

### Vehicle
- `id`, `plate_number` (UNIQUE), `vehicle_type`, `max_weight_kg`,
  `max_volume_m3`, `status` (AVAILABLE/IN_TRANSIT/MAINTENANCE), `is_active`

### Driver
- `id`, `full_name`, `phone`, `license_number`, `license_expiry_date`,
  `status` (AVAILABLE/IN_TRANSIT), `is_active`

## 6. API Spec

### Products
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/products | Bearer | List products (paginated, filterable) |
| POST | /api/v1/products | PLANNER, SYSTEM_ADMIN | Create product |
| GET | /api/v1/products/{id} | Bearer | Get product detail |
| PUT | /api/v1/products/{id} | PLANNER, SYSTEM_ADMIN | Update product |
| DELETE | /api/v1/products/{id} | SYSTEM_ADMIN | Soft-delete (is_active=false) |

### Warehouses & Bins
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/warehouses | Bearer | List warehouses |
| GET | /api/v1/bin-locations | Bearer | List bins (filter by warehouse) |
| POST | /api/v1/bin-locations | WAREHOUSE_MANAGER | Create bin location |
| PUT | /api/v1/bin-locations/{id} | WAREHOUSE_MANAGER | Update bin |
| GET | /api/v1/bin-locations/{id}/capacity | Bearer | Check remaining capacity |

### Dealers
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/dealers | Bearer | List dealers |
| POST | /api/v1/dealers | SYSTEM_ADMIN | Create dealer |
| PUT | /api/v1/dealers/{id}/credit-limit | ACCOUNTANT_MANAGER | Set credit limit |

### Suppliers, Vehicles, Drivers
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET/POST/PUT | /api/v1/suppliers | SYSTEM_ADMIN | CRUD suppliers |
| GET/POST/PUT | /api/v1/vehicles | DISPATCHER | CRUD vehicles |
| GET/POST/PUT | /api/v1/drivers | DISPATCHER | CRUD drivers |

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| DUPLICATE_SKU | 409 | SKU already exists |
| BIN_OVER_CAPACITY | 422 | Putaway exceeds bin capacity |
| DEALER_IS_INACTIVE | 400 | Transaction on inactive dealer |
| MISSING_SERIAL | 400 | has_serial product without serial |
| INVALID_UNIT_CONVERSION | 400 | Unit conversion not configured |

## 8. Acceptance Criteria

1. Given a product with `has_serial = true`, when creating a receipt without
   serial numbers, the system SHALL reject with error MISSING_SERIAL.
2. Given a Bin with capacity 100 and current occupied 80, when putting away
   30 units, the system SHALL reject with error BIN_OVER_CAPACITY.
3. Given a dealer with credit_limit = 500M and current_balance = 0,
   when ACCOUNTANT_MANAGER sets credit_limit to 800M,
   then the system SHALL update successfully and record the change in audit log.
4. Given a vehicle with status "Đang bảo trì", when a Dispatcher creates
   a trip, the system SHALL NOT list this vehicle in the selection.

## 9. Out of Scope

- Barcode / QR code generation for Bin Locations
- Product image management
- Integration with external supplier databases
- Automated bin suggestion based on product velocity (future optimization)
- Multi-unit conversion matrix (thùng→cái→kiện)
