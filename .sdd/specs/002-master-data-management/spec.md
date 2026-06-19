# Feature Specification: Danh mục Nền tảng (Master Data Management)

**Spec ID**: 002-master-data-management
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-19, US-WMS-20, US-WMS-22, US-WMS-23

---

## 1. Context and Goal

Hệ thống WMS vận hành dựa trên các danh mục nền tảng: sản phẩm (SKU), kho bãi và vị trí lưu trữ (Bin Location), đối tác (Đại lý, Nhà cung cấp), phương tiện vận tải nội bộ và tài xế. Các danh mục này là xương sống cho mọi nghiệp vụ nhập, xuất, điều chuyển, kiểm kê.

### Features List

- [US-WMS-19: Quản lý SKU & Danh mục Sản phẩm](./features/feature-admin-products.md)
- [US-WMS-20: Vị trí Kho & Sức chứa Kệ](./features/feature-admin-warehouses.md)
- [US-WMS-22: Danh mục Đối tác & Hạn mức Tín dụng](./features/feature-accountant-partners.md)
- [US-WMS-23: Danh mục Xe tải & Tài xế Nội bộ](./features/feature-dispatcher-fleet-drivers.md)

## 2. Actors

| Actor                     | Vai trò | Nghiệp vụ liên quan                                                                      |
| ------------------------- | ------- | ---------------------------------------------------------------------------------------- |
| Planner                   | Maker   | Tiếp nhận yêu cầu xuất/nhập kho từ Công ty mẹ hoặc bên thứ ba, nhập yêu cầu lên hệ thống |
| Kế toán viên              | Maker   | Quản lý hồ sơ Nhà cung cấp; lập hóa đơn, ghi nhận thanh toán, cấn trừ công nợ            |
| Kế toán trưởng            | Checker | Thiết lập Credit Limit, kỳ hạn thanh toán cho Đại lý                                     |
| System Admin              | Admin   | Quản lý tài khoản, phân quyền, cấu hình hệ thống                                         |
| Dispatcher                | Maker   | Quản lý Danh mục Xe tải & Tài xế nội bộ                                                  |
| Thủ kho kiêm QC           | Maker   | Quản lý SKU, danh mục sản phẩm; cấu hình vị trí kho, Bin location, kiểm tra sức chứa kệ; kiểm QC sản phẩm |
| Trưởng kho                | Checker | Phê duyệt cấu hình kho                                                                   |
| CEO                       | Checker | Phê duyệt cấu hình hệ thống quan trọng (tạo kho mới) và xem dashboard chiến lược         |


## 3. Functional Requirements (EARS)

_Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:_

- [EARS - Products](./features/feature-admin-products.md#3-functional-requirements-ears)
- [EARS - Warehouses](./features/feature-admin-warehouses.md#3-functional-requirements-ears)
- [EARS - Partners & Credit Limit](./features/feature-accountant-partners.md#3-functional-requirements-ears)
- [EARS - Fleet & Drivers](./features/feature-dispatcher-fleet-drivers.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID      | Requirement                      | Target                 |
| ------- | -------------------------------- | ---------------------- |
| NFR-001 | Product search response time     | ≤ 500ms for 1000+ SKUs |
| NFR-002 | Master data CRUD response (p95)  | ≤ 300ms                |
| NFR-003 | Bin capacity validation overhead | ≤ 50ms                 |

## 5. Data Model

### products

- `id` (BIGSERIAL, PK)
- `sku` (VARCHAR(50), UNIQUE, NOT NULL)
- `name` (VARCHAR(255), NOT NULL)
- `unit` (VARCHAR(30), NOT NULL) -- base counting unit, Sprint 1 supports "cái"
- `unit_per_pack` (INTEGER) -- number of base units per one "thùng"; only supports thùng → cái conversion
- `description` (TEXT)
- `image_url` (VARCHAR(500))
- `weight_kg` (DECIMAL(10,3))
- `volume_m3` (DECIMAL(10,5))
- `reorder_point` (DECIMAL(10,2))
- `is_active` (BOOLEAN, DEFAULT true)
- `created_by` (BIGINT, FK→users)
- `updated_by` (BIGINT, FK→users)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### warehouses

- `id` (BIGSERIAL, PK)
- `code` (VARCHAR(20), UNIQUE, NOT NULL) -- HP / HN / HCM / IN_TRANSIT
- `name` (VARCHAR(255), NOT NULL)
- `address` (TEXT)
- `phone` (VARCHAR(20))
- `manager_id` (BIGINT, FK→users)
- `type` (VARCHAR(20), CHECK IN ('PHYSICAL','IN_TRANSIT'))
- `is_active` (BOOLEAN, DEFAULT true)
- `created_by` (BIGINT, FK→users)
- `updated_by` (BIGINT, FK→users)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### warehouse_locations

- `id` (BIGSERIAL, PK)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `code` (VARCHAR(50), UNIQUE, NOT NULL)
- `type` (VARCHAR(10), CHECK IN ('ZONE','BIN'))
- `parent_id` (BIGINT, FK→warehouse_locations) -- NULL for ZONE; required for BIN and must reference a ZONE in same warehouse
- `capacity_m3` (DECIMAL(10,3)) -- only required/applicable for BIN
- `capacity_kg` (DECIMAL(10,2)) -- only required/applicable for BIN
- `current_volume_m3` (DECIMAL(10,3), DEFAULT 0) -- tracked for BIN only
- `current_weight_kg` (DECIMAL(10,2), DEFAULT 0) -- tracked for BIN only
- `is_quarantine` (BOOLEAN, DEFAULT false)
- `is_active` (BOOLEAN, DEFAULT true)
- `created_by` (BIGINT, FK→users)
- `updated_by` (BIGINT, FK→users)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### dealers

- `id` (BIGSERIAL, PK)
- `code` (VARCHAR(50), UNIQUE, NOT NULL)
- `name` (VARCHAR(255), NOT NULL)
- `phone` (VARCHAR(20))
- `default_delivery_address` (TEXT)
- `region` (VARCHAR(100))
- `payment_term_days` (INTEGER, DEFAULT 30)
- `credit_limit` (DECIMAL(18,2), DEFAULT 0)
- `current_balance` (DECIMAL(18,2), DEFAULT 0)
- `credit_status` (VARCHAR(20), DEFAULT 'ACTIVE', CHECK IN ('ACTIVE','CREDIT_HOLD'))
- `is_active` (BOOLEAN, DEFAULT true)
- `created_by` (BIGINT, FK→users)
- `updated_by` (BIGINT, FK→users)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### suppliers

- `id` (BIGSERIAL, PK)
- `code` (VARCHAR(50), UNIQUE, NOT NULL)
- `company_name` (VARCHAR(255), NOT NULL)
- `tax_code` (VARCHAR(20))
- `phone` (VARCHAR(20))
- `contact_person` (VARCHAR(255))
- `address` (TEXT)
- `is_active` (BOOLEAN, DEFAULT true)
- `created_by` (BIGINT, FK→users)
- `updated_by` (BIGINT, FK→users)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### vehicles

- `id` (BIGSERIAL, PK)
- `plate_number` (VARCHAR(20), UNIQUE, NOT NULL)
- `vehicle_type` (VARCHAR(100), NOT NULL)
- `max_weight_kg` (DECIMAL(10,2), NOT NULL)
- `max_volume_m3` (DECIMAL(10,3))
- `status` (VARCHAR(20), DEFAULT 'AVAILABLE', CHECK IN ('AVAILABLE','ON_TRIP','MAINTENANCE'))
- `is_active` (BOOLEAN, DEFAULT true)
- `created_by` (BIGINT, FK→users)
- `updated_by` (BIGINT, FK→users)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### drivers

- `id` (BIGSERIAL, PK)
- `user_id` (BIGINT, FK→users, UNIQUE, NOT NULL)
- `full_name` (VARCHAR(255), NOT NULL)
- `phone` (VARCHAR(20))
- `license_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `license_expiry` (DATE, NOT NULL)
- `status` (VARCHAR(20), DEFAULT 'AVAILABLE', CHECK IN ('AVAILABLE','ON_TRIP','UNAVAILABLE'))
- `is_active` (BOOLEAN, DEFAULT true)
- `created_by` (BIGINT, FK→users)
- `updated_by` (BIGINT, FK→users)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### Master data constraints

- Product unit conversion in Sprint 1 supports only one fixed conversion: `thùng → cái`, using `unit_per_pack` as the number of base units (`cái`) in one pack (`thùng`).
- Product master data for Sprint 1 household goods does not include serial tracking, expiry tracking, or shelf-life configuration.
- Warehouse location hierarchy supports only two levels: `ZONE → BIN`.
- `ZONE.parent_id` MUST be null and capacity fields are not used for capacity enforcement.
- `BIN.parent_id` MUST reference a `ZONE` in the same warehouse.
- Bin capacity validation applies only to `BIN` locations using `capacity_m3`, `capacity_kg`, `current_volume_m3`, and `current_weight_kg`.
- Vehicle status values are `AVAILABLE`, `ON_TRIP`, and `MAINTENANCE`.
- Driver status values are `AVAILABLE`, `ON_TRIP`, and `UNAVAILABLE`.
- Trip purpose MUST be represented by `trips.trip_type` (`DELIVERY` or `TRANSFER`) instead of overloading vehicle or driver status.

## 6. API Spec

_Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:_

- [APIs - Products](./features/feature-admin-products.md#4-api-endpoints)
- [APIs - Warehouses](./features/feature-admin-warehouses.md#4-api-endpoints)
- [APIs - Partners & Credit Limit](./features/feature-accountant-partners.md#4-api-endpoints)
- [APIs - Fleet & Drivers](./features/feature-dispatcher-fleet-drivers.md#4-api-endpoints)

## 7. Error Handling

| Error                   | HTTP | Condition                         |
| ----------------------- | ---- | --------------------------------- |
| DUPLICATE_SKU           | 409  | SKU already exists                |
| BIN_OVER_CAPACITY       | 422  | Putaway exceeds bin capacity      |
| DEALER_IS_INACTIVE      | 400  | Transaction on inactive dealer    |
| INVALID_UNIT_CONVERSION | 400  | Invalid or missing thùng → cái conversion |

## 8. Acceptance Criteria

_Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:_

- [Acceptance - Products](./features/feature-admin-products.md#5-acceptance-criteria)
- [Acceptance - Warehouses](./features/feature-admin-warehouses.md#5-acceptance-criteria)
- [Acceptance - Partners & Credit Limit](./features/feature-accountant-partners.md#5-acceptance-criteria)
- [Acceptance - Fleet & Drivers](./features/feature-dispatcher-fleet-drivers.md#5-acceptance-criteria)

## 9. Out of Scope

- Barcode / QR code generation for Bin Locations
- Product image management
- Integration with external supplier databases
- Automated bin suggestion based on product velocity (future optimization)
- Multi-unit conversion matrix beyond thùng → cái (for example thùng → cái → kiện)
- Product import from Excel
