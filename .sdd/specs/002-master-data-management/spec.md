# Feature Specification: Danh mục Nền tảng (Master Data Management)

**Spec ID**: 002-master-data-management
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-19, US-WMS-20, US-WMS-22, US-WMS-23

---

## 1. Context and Goal

Hệ thống WMS vận hành dựa trên các danh mục nền tảng: sản phẩm (SKU), kho bãi và vị trí lưu trữ (Bin Location), đối tác (Đại lý, Nhà cung cấp), phương tiện vận tải nội bộ và tài xế. Các danh mục này là xương sống cho mọi nghiệp vụ nhập, xuất, điều chuyển, kiểm kê.

### Features List
* [US-WMS-19: Quản lý SKU & Danh mục Sản phẩm](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-admin-products.md)
* [US-WMS-20: Vị trí Kho & Sức chứa Kệ](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-admin-warehouses.md)
* [US-WMS-22: Danh mục Đối tác & Hạn mức Tín dụng](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-accountant-partners.md)
* [US-WMS-23: Danh mục Xe tải & Tài xế Nội bộ](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-dispatcher-fleet-drivers.md)

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Planner | Maker | Quản lý SKU, danh mục sản phẩm |
| Kế toán viên | Maker | Quản lý thông tin Đại lý |
| Kế toán trưởng | Checker | Thiết lập Credit Limit, kỳ hạn thanh toán cho Đại lý |
| System Admin | Admin | Quản lý tài khoản, phân quyền, cấu hình hệ thống, quản lý Nhà cung cấp |
| Dispatcher | Maker | Quản lý Danh mục Xe tải & Tài xế nội bộ |
| Thủ kho | Maker | Cấu hình vị trí kho, Bin location, kiểm tra sức chứa kệ |
| Trưởng kho kiêm Trưởng QC | Checker | Phê duyệt cấu hình kho |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Products](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-admin-products.md#3-functional-requirements-ears)
* [EARS - Warehouses](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-admin-warehouses.md#3-functional-requirements-ears)
* [EARS - Partners & Credit Limit](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-accountant-partners.md#3-functional-requirements-ears)
* [EARS - Fleet & Drivers](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-dispatcher-fleet-drivers.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Product search response time | ≤ 500ms for 1000+ SKUs |
| NFR-002 | Master data CRUD response (p95) | ≤ 300ms |
| NFR-003 | Bin capacity validation overhead | ≤ 50ms |
| NFR-004 | Support product import from Excel | ≤ 5s for 1000 rows |

## 5. Data Model

### products
- `id` (BIGSERIAL, PK)
- `sku` (VARCHAR(50), UNIQUE, NOT NULL)
- `name` (VARCHAR(255), NOT NULL)
- `unit` (VARCHAR(30), NOT NULL)
- `unit_per_pack` (INTEGER)
- `description` (TEXT)
- `image_url` (VARCHAR(500))
- `weight_kg` (DECIMAL(10,3))
- `volume_m3` (DECIMAL(10,5))
- `has_serial` (BOOLEAN, DEFAULT false)
- `reorder_point` (DECIMAL(10,2))
- `is_active` (BOOLEAN, DEFAULT true)
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
- `created_at` (TIMESTAMPTZ)

### warehouse_locations
- `id` (BIGSERIAL, PK)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `code` (VARCHAR(50), UNIQUE, NOT NULL)
- `type` (VARCHAR(10), CHECK IN ('ZONE','RACK','SHELF','BIN'))
- `parent_id` (BIGINT, FK→warehouse_locations)
- `capacity_m3` (DECIMAL(10,3))
- `capacity_kg` (DECIMAL(10,2))
- `current_volume_m3` (DECIMAL(10,3), DEFAULT 0)
- `current_weight_kg` (DECIMAL(10,2), DEFAULT 0)
- `is_quarantine` (BOOLEAN, DEFAULT false)
- `is_active` (BOOLEAN, DEFAULT true)

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
- `created_at` (TIMESTAMPTZ)

### vehicles
- `id` (BIGSERIAL, PK)
- `plate_number` (VARCHAR(20), UNIQUE, NOT NULL)
- `vehicle_type` (VARCHAR(100), NOT NULL)
- `max_weight_kg` (DECIMAL(10,2), NOT NULL)
- `max_volume_m3` (DECIMAL(10,3))
- `status` (VARCHAR(20), DEFAULT 'AVAILABLE', CHECK IN ('AVAILABLE','ON_TRIP','MAINTENANCE'))
- `is_active` (BOOLEAN, DEFAULT true)

### drivers
- `id` (BIGSERIAL, PK)
- `user_id` (BIGINT, FK→users, UNIQUE, NOT NULL)
- `full_name` (VARCHAR(255), NOT NULL)
- `phone` (VARCHAR(20))
- `license_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `license_expiry` (DATE, NOT NULL)
- `status` (VARCHAR(20), DEFAULT 'AVAILABLE', CHECK IN ('AVAILABLE','ON_DELIVERY','MAINTENANCE'))
- `is_active` (BOOLEAN, DEFAULT true)

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Products](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-admin-products.md#4-api-endpoints)
* [APIs - Warehouses](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-admin-warehouses.md#4-api-endpoints)
* [APIs - Partners & Credit Limit](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-accountant-partners.md#4-api-endpoints)
* [APIs - Fleet & Drivers](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-dispatcher-fleet-drivers.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| DUPLICATE_SKU | 409 | SKU already exists |
| BIN_OVER_CAPACITY | 422 | Putaway exceeds bin capacity |
| DEALER_IS_INACTIVE | 400 | Transaction on inactive dealer |
| MISSING_SERIAL | 400 | has_serial product without serial |
| INVALID_UNIT_CONVERSION | 400 | Unit conversion not configured |

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Products](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-admin-products.md#5-acceptance-criteria)
* [Acceptance - Warehouses](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-admin-warehouses.md#5-acceptance-criteria)
* [Acceptance - Partners & Credit Limit](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-accountant-partners.md#5-acceptance-criteria)
* [Acceptance - Fleet & Drivers](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-dispatcher-fleet-drivers.md#5-acceptance-criteria)

## 9. Out of Scope

- Barcode / QR code generation for Bin Locations
- Product image management
- Integration with external supplier databases
- Automated bin suggestion based on product velocity (future optimization)
- Multi-unit conversion matrix (thùng→cái→kiện)
