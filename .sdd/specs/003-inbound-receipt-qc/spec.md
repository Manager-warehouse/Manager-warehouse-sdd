# Feature Specification: Nhập hàng & QC Inbound (Receiving & Quality Check)

**Spec ID**: 003-inbound-receipt-qc
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-02, US-WMS-03, US-WMS-04, US-WMS-05

---

## 1. Context and Goal

Quy trình nhập hàng là đầu vào của toàn bộ hệ thống tồn kho. Hàng hóa từ Công ty mẹ được thông báo qua Zalo/Email → Planner lập lệnh → Thủ kho đếm thực tế → QC kiểm tra chất lượng → Hàng đạt nhập kho, hàng lỗi vào Quarantine → Trưởng kho duyệt chính thức.

### Features List
* [US-WMS-02: Tiếp nhận & Lập Lệnh Nhập kho](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-planner-receipt-drafting.md)
* [Thủ kho Tiếp nhận & Đếm hàng thực tế](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-storekeeper-receipt-receive.md)
* [US-WMS-03: Nhân viên QC Kiểm tra chất lượng Inbound](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-qc-inbound-inspection.md)
* [US-WMS-04: Xử lý Hàng lỗi trong Quarantine Zone](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-manager-quarantine-handling.md)
* [US-WMS-05: Duyệt Nhập kho Chính thức](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-manager-receipt-approval.md)

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Planner | Maker | Tiếp nhận thông tin hàng về từ Công ty mẹ, tạo Lệnh nhập kho |
| Thủ kho | Maker | Tiếp nhận và đếm hàng thực tế, cất hàng vào vị trí kệ chỉ định (Putaway) |
| Nhân viên kho | Maker | Bốc xếp hàng hóa, di chuyển hàng lỗi vào Quarantine, kiểm tra ngoại quan và nhập kết quả QC |
| Trưởng kho kiêm Trưởng QC | Checker | Đối chiếu kết quả QC và phê duyệt Phiếu nhập kho chính thức, quyết định xử lý hàng lỗi tại Quarantine |
| Kế toán viên | Maker | Tạo Debit Note yêu cầu bồi hoàn nếu hàng lỗi được trả về NCC |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Receipt Drafting](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-planner-receipt-drafting.md#3-functional-requirements-ears)
* [EARS - Receipt Receive](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-storekeeper-receipt-receive.md#3-functional-requirements-ears)
* [EARS - Inbound QC](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-qc-inbound-inspection.md#3-functional-requirements-ears)
* [EARS - Quarantine Handling](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-manager-quarantine-handling.md#3-functional-requirements-ears)
* [EARS - Receipt Approval](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-manager-receipt-approval.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Receipt creation → inventory update latency | ≤ 2s after approval |
| NFR-002 | QC result save response time | ≤ 500ms |
| NFR-003 | Support concurrent receipt processing at 3 warehouses | No deadlock |

## 5. Data Model

### purchase_orders
- `id` (BIGSERIAL, PK)
- `po_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `supplier_id` (BIGINT, FK→suppliers, NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `expected_receipt_date` (DATE)
- `status` (VARCHAR(30), CHECK IN ('OPEN','PARTIALLY_RECEIVED','COMPLETED','CANCELLED'))
- `created_by` (BIGINT, FK→users)
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### purchase_order_items
- `id` (BIGSERIAL, PK)
- `po_id` (BIGINT, FK→purchase_orders, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `expected_qty` (DECIMAL(10,2), NOT NULL)
- `unit_price` (DECIMAL(18,2))

### receipts
- `id` (BIGSERIAL, PK)
- `receipt_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `source_order_code` (VARCHAR(100)) -- PO number hoặc DO hoàn
- `type` (VARCHAR(20), CHECK IN ('PURCHASE','RETURN'), NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `supplier_id` (BIGINT, FK→suppliers)
- `dealer_id` (BIGINT, FK→dealers)
- `contact_person` (VARCHAR(255))
- `source_channel` (VARCHAR(50)) -- Zalo / Email
- `status` (VARCHAR(30), DEFAULT 'PENDING_RECEIPT', CHECK IN ('PENDING_RECEIPT','DRAFT','QC_COMPLETED','APPROVED','REJECTED'))
- `approved_by` (BIGINT, FK→users)
- `approved_at` (TIMESTAMPTZ)
- `rejection_reason` (TEXT)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### receipt_items
- `id` (BIGSERIAL, PK)
- `receipt_id` (BIGINT, FK→receipts, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches) -- set sau QC
- `location_id` (BIGINT, FK→warehouse_locations) -- set khi putaway
- `expected_qty` (DECIMAL(10,2), NOT NULL)
- `actual_qty` (DECIMAL(10,2))
- `qc_passed_qty` (DECIMAL(10,2))
- `qc_failed_qty` (DECIMAL(10,2))
- `qc_result` (VARCHAR(20), CHECK IN ('PENDING','PASSED','FAILED','PARTIAL'))
- `qc_failure_reason` (TEXT)
- `grade` (VARCHAR(1)) -- A / B / C
- `unit_cost` (DECIMAL(18,2))
- `serial_number` (VARCHAR(100)) -- bắt buộc nếu product.has_serial = true

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Receipt Drafting](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-planner-receipt-drafting.md#4-api-endpoints)
* [APIs - Receipt Receive & Putaway](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-storekeeper-receipt-receive.md#4-api-endpoints)
* [APIs - Inbound QC](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-qc-inbound-inspection.md#4-api-endpoints)
* [APIs - Quarantine Handling](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-manager-quarantine-handling.md#4-api-endpoints)
* [APIs - Receipt Approval](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-manager-receipt-approval.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| RECEIPT_ALREADY_APPROVED | 409 | Duplicate approval attempt |
| QC_PASSED_FAILED_MISMATCH | 422 | passed + failed ≠ received |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update |
| NO_QUARANTINE_ITEMS | 400 | No failed items to process |
| APPROVAL_THRESHOLD_EXCEEDED | 403 | Exceeds user's approval authority |

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Receipt Drafting](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-planner-receipt-drafting.md#5-acceptance-criteria)
* [Acceptance - Receipt Receive](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-storekeeper-receipt-receive.md#5-acceptance-criteria)
* [Acceptance - Inbound QC](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-qc-inbound-inspection.md#5-acceptance-criteria)
* [Acceptance - Quarantine Handling](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-manager-quarantine-handling.md#5-acceptance-criteria)
* [Acceptance - Receipt Approval](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-manager-receipt-approval.md#5-acceptance-criteria)

## 9. Out of Scope

- Barcode/QR scanning for receiving
- Integration with Công ty mẹ API (Zalo/Email manual for Sprint 1)
- Automated putaway optimization (FEFO bin suggestion)
- Supplier quality rating tracking
