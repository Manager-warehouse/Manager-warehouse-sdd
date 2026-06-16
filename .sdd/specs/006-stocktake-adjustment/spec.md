# Feature Specification: Kiểm kê & Điều chỉnh Tồn kho (StockTake & Adjustment)

**Spec ID**: 006-stocktake-adjustment
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-13

---

## 1. Context and Goal

Quy trình đối chiếu và điều chỉnh số liệu hệ thống khớp với số đếm thực tế của thủ kho định kỳ. Hệ thống áp dụng khóa vị trí tạm thời và phân cấp duyệt Maker-Checker dựa trên tổng giá trị chênh lệch.

### Features List
* [Thủ kho Kiểm kê kho & Đếm hàng Thực tế](./features/feature-storekeeper-stocktake-count.md)
* [US-WMS-13: Trưởng kho Phê duyệt Điều chỉnh Chênh lệch Kiểm kê](./features/feature-manager-stocktake-approval.md)

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Thủ kho | Maker | Lập phiếu kiểm kê, đếm thực tế và nhập số đếm. |
| Trưởng kho | Checker | Duyệt chênh lệch kiểm kê trị giá 5M - 100M. |
| CEO | Checker cấp cao | Duyệt chênh lệch kiểm kê trị giá > 100M hoặc do lỗi nhân viên. |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Stocktake Count](./features/feature-storekeeper-stocktake-count.md#3-functional-requirements-ears)
* [EARS - Stocktake Approval](./features/feature-manager-stocktake-approval.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Stocktake count record save response | ≤ 200ms |
| NFR-002 | Inventory balance update upon approval | ≤ 1s |
| NFR-003 | Adjustment log write latency | ≤ 500ms |

## 5. Data Model

### stock_takes
- `id` (BIGSERIAL, PK)
- `stock_take_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `conducted_by` (BIGINT, FK→users, NOT NULL)
- `approved_by` (BIGINT, FK→users)
- `approved_at` (TIMESTAMPTZ)
- `status` (VARCHAR(30), DEFAULT 'DRAFT', CHECK IN ('DRAFT','IN_PROGRESS','PENDING_APPROVAL','APPROVED','CANCELLED'), NOT NULL)
- `total_variance_value` (DECIMAL(18,2), DEFAULT 0)
- `stock_take_date` (DATE, NOT NULL)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### stock_take_items
- `id` (BIGSERIAL, PK)
- `stock_take_id` (BIGINT, FK→stock_takes, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches, NOT NULL)
- `location_id` (BIGINT, FK→warehouse_locations, NOT NULL)
- `system_qty` (DECIMAL(10,2), NOT NULL)
- `actual_qty` (DECIMAL(10,2), NOT NULL)
- `variance_qty` (DECIMAL(10,2), NOT NULL) -- actual_qty - system_qty
- `variance_value` (DECIMAL(18,2), NOT NULL) -- variance_qty × cost_price
- `notes` (TEXT)

### adjustments
- `id` (BIGSERIAL, PK)
- `adjustment_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches)
- `location_id` (BIGINT, FK→warehouse_locations)
- `quantity_adjustment` (DECIMAL(10,2), NOT NULL)
- `type` (VARCHAR(30), CHECK IN ('STOCK_TAKE','TRANSFER_DISCREPANCY','DISPOSAL','RETURN_TO_VENDOR','CORRECTION_VOUCHER'), NOT NULL)
- `reference_id` (BIGINT)
- `reference_type` (VARCHAR(50))
- `reason` (TEXT, NOT NULL)
- `approved_by` (BIGINT, FK→users)
- `approved_at` (TIMESTAMPTZ)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `created_at` (TIMESTAMPTZ)

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Stocktake Count](./features/feature-storekeeper-stocktake-count.md#4-api-endpoints)
* [APIs - Stocktake Approval](./features/feature-manager-stocktake-approval.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| STOCK_TAKE_ALREADY_APPROVED | 409 | Duplicate approval attempt |
| LOCATION_LOCKED | 422 | Attempt to transact on a locked location |
| INVALID_COUNT_QTY | 400 | Counted quantity is negative |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update |

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Stocktake Count](./features/feature-storekeeper-stocktake-count.md#5-acceptance-criteria)
* [Acceptance - Stocktake Approval](./features/feature-manager-stocktake-approval.md#5-acceptance-criteria)

## 9. Out of Scope

- Automated cycle counting scheduling
- Integration with external finance ERP ledger (local adjustment only)
- Physical tag tracking
