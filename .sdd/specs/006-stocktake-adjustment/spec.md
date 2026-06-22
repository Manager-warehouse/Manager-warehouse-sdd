# Feature Specification: Kiểm kê & Điều chỉnh Tồn kho (StockTake & Adjustment)

**Spec ID**: 006-stocktake-adjustment
**Created**: 2026-05-30
**Updated**: 2026-06-17
**Status**: Draft
**Features**: US-WMS-13

---

## 1. Context and Goal

Quy trình đối chiếu và điều chỉnh số liệu hệ thống khớp với số đếm thực tế của thủ kho định kỳ. Hệ thống áp dụng khóa vị trí tạm thời và phân cấp duyệt Maker-Checker dựa trên **giá trị tuyệt đối** của tổng chênh lệch.

### Features List
* [Thủ kho Kiểm kê kho & Đếm hàng Thực tế](./features/feature-storekeeper-stocktake-count.md)
* [Trưởng kho / CEO Phê duyệt Điều chỉnh Chênh lệch Kiểm kê](./features/feature-manager-stocktake-approval.md)

---

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Thủ kho | Maker | Lập phiếu kiểm kê, bắt đầu đếm, nhập số đếm thực tế, hủy phiếu khi còn DRAFT/IN_PROGRESS. |
| Trưởng kho | Checker | Duyệt hoặc từ chối chênh lệch kiểm kê khi \|tổng chênh lệch\| từ 5M đến 100M VND. |
| CEO | Checker cấp cao | Duyệt hoặc từ chối chênh lệch kiểm kê khi \|tổng chênh lệch\| > 100M VND **hoặc** được đánh dấu là do lỗi nhân viên. |

---

## 3. Functional Requirements (EARS) 
*Xem chi tiết tại các tài liệu đặc tả tính năng:*
* [EARS - Stocktake Count](./features/feature-storekeeper-stocktake-count.md#3-functional-requirements-ears)
* [EARS - Stocktake Approval](./features/feature-manager-stocktake-approval.md#3-functional-requirements-ears)

---

## 4. State Machine

```
DRAFT ──[start]──► IN_PROGRESS ──[complete]──► PENDING_APPROVAL
  │                    │                              │
  │[cancel]            │[cancel]               [route by threshold]
  ▼                    ▼                              │
CANCELLED           CANCELLED              ┌──────────┴──────────┐
                                           │                     │
                                   |chênh lệch| ≤ 100M    |chênh lệch| > 100M
                                   AND NOT lỗi NV         OR lỗi nhân viên
                                           │                     │
                                    Trưởng kho              CEO duyệt
                                    duyệt/từ chối          duyệt/từ chối
                                           │                     │
                                    ┌──────┴──────┐       ┌──────┴──────┐
                                 APPROVED      REJECTED APPROVED     REJECTED
                                                  │                     │
                                                  └──────────┬──────────┘
                                                             ▼
                                                  Về DRAFT (location lock giải phóng,
                                                  Thủ kho có thể sửa và re-submit)
```

### Quy tắc routing phê duyệt

| Điều kiện | Người duyệt |
|-----------|-------------|
| \|total_variance_value\| < 5,000,000 VND | Tự động phê duyệt (system auto-approve) |
| 5,000,000 ≤ \|total_variance_value\| ≤ 100,000,000 VND AND is_employee_fault = false | Trưởng kho |
| \|total_variance_value\| > 100,000,000 VND OR is_employee_fault = true | CEO |

---

## 5. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Stocktake count record save response | ≤ 200ms |
| NFR-002 | Inventory balance update upon approval | ≤ 1s |
| NFR-003 | Adjustment log write latency | ≤ 500ms |

---

## 6. Data Model

### stock_takes
- `id` (BIGSERIAL, PK)
- `stock_take_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `conducted_by` (BIGINT, FK→users, NOT NULL)
- `approved_by` (BIGINT, FK→users)
- `approved_at` (TIMESTAMPTZ)
- `status` (VARCHAR(30), DEFAULT 'DRAFT', CHECK IN ('DRAFT','IN_PROGRESS','PENDING_APPROVAL','APPROVED','REJECTED','CANCELLED'), NOT NULL)
- `approval_level` (VARCHAR(10), CHECK IN ('AUTO','MANAGER','CEO')) — được tính khi complete, NULL khi chưa submit
- `is_employee_fault` (BOOLEAN, DEFAULT false, NOT NULL) — Thủ kho đánh dấu khi nhập số đếm, bắt buộc ghi lý do nếu true
- `total_variance_value` (DECIMAL(18,2), DEFAULT 0) — SUM của variance_value tất cả items (có dấu âm/dương)
- `stock_take_date` (DATE, NOT NULL)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods, NOT NULL) — phải là kỳ đang OPEN
- `rejection_reason` (TEXT) — bắt buộc khi status = REJECTED
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### stock_take_items
- `id` (BIGSERIAL, PK)
- `stock_take_id` (BIGINT, FK→stock_takes, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches, NOT NULL)
- `location_id` (BIGINT, FK→warehouse_locations, NOT NULL) — chỉ các location thuộc zone != QUARANTINE
- `system_qty` (DECIMAL(10,2), NOT NULL) — số lượng hệ thống tại thời điểm bắt đầu kiểm kê, không tính hàng trong Quarantine
- `actual_qty` (DECIMAL(10,2), NOT NULL, CHECK >= 0)
- `variance_qty` (DECIMAL(10,2), NOT NULL) — actual_qty - system_qty (âm = hao hụt, dương = thừa)
- `variance_value` (DECIMAL(18,2), NOT NULL) — variance_qty × cost_price tại thời điểm kiểm kê
- `notes` (TEXT) — **bắt buộc** khi `variance_qty ≠ 0` (lý do chênh lệch)

### adjustments
- `id` (BIGSERIAL, PK)
- `adjustment_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches)
- `location_id` (BIGINT, FK→warehouse_locations)
- `quantity_adjustment` (DECIMAL(10,2), NOT NULL) — âm = giảm tồn, dương = tăng tồn
- `type` (VARCHAR(30), CHECK IN ('STOCK_TAKE','TRANSFER_DISCREPANCY','DISPOSAL','RETURN_TO_VENDOR','CORRECTION_VOUCHER'), NOT NULL)
- `reference_id` (BIGINT) — FK tới stock_takes.id hoặc entity tương ứng
- `reference_type` (VARCHAR(50)) — 'STOCK_TAKE' | 'TRANSFER' | ...
- `reason` (TEXT, NOT NULL)
- `approved_by` (BIGINT, FK→users)
- `approved_at` (TIMESTAMPTZ)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods, NOT NULL)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `created_at` (TIMESTAMPTZ)

---

## 7. API Spec
*Xem chi tiết tại các tài liệu đặc tả tính năng:*
* [APIs - Stocktake Count](./features/feature-storekeeper-stocktake-count.md#4-api-endpoints)
* [APIs - Stocktake Approval](./features/feature-manager-stocktake-approval.md#4-api-endpoints)

---

## 8. Error Handling

| Error Code | HTTP | Điều kiện |
|------------|------|-----------|
| STOCK_TAKE_ALREADY_APPROVED | 409 | Phê duyệt trùng lặp |
| STOCK_TAKE_NOT_CANCELLABLE | 422 | Hủy phiếu khi status không phải DRAFT hoặc IN_PROGRESS |
| LOCATION_LOCKED | 422 | Giao dịch trên location đang bị khóa bởi stocktake |
| INVALID_COUNT_QTY | 400 | Số đếm thực tế âm |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update khi approve |
| ACCOUNTING_PERIOD_CLOSED | 422 | Tạo hoặc phê duyệt stocktake vào kỳ kế toán đã CLOSED |
| APPROVAL_LEVEL_MISMATCH | 403 | Actor không đúng cấp duyệt (vd. Trưởng kho duyệt phiếu cần CEO) |
| EMPLOYEE_FAULT_REASON_REQUIRED | 400 | is_employee_fault = true nhưng không có notes giải thích |
| VARIANCE_REASON_REQUIRED | 400 | actual_qty ≠ system_qty nhưng không nhập lý do chênh lệch (notes bắt buộc khi có variance) |

---

## 9. Acceptance Criteria
*Xem chi tiết tại các tài liệu đặc tả tính năng:*
* [Acceptance - Stocktake Count](./features/feature-storekeeper-stocktake-count.md#5-acceptance-criteria)
* [Acceptance - Stocktake Approval](./features/feature-manager-stocktake-approval.md#5-acceptance-criteria)

---

## 10. Out of Scope

- Automated cycle counting scheduling
- Integration with external finance ERP ledger (local adjustment only)
- Physical tag tracking
- Tiêu hủy hàng lỗi (tách sang Spec 009)
