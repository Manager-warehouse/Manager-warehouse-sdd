# Feature Specification: Nhập hàng & QC Inbound (Receiving & Quality Check)

**Spec ID**: 003-inbound-receipt-qc
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-02, US-WMS-03, US-WMS-04, US-WMS-05

---

## 1. Context and Goal

Quy trình nhập hàng là đầu vào của toàn bộ hệ thống tồn kho. Hàng hóa từ Công ty mẹ được thông báo qua Zalo/Email, Planner lập lệnh, Nhân viên kho kiểm đếm thực tế và tạo bản nháp `DRAFT`, Nhân viên kho lấy mẫu QC theo từng lô, Storekeeper xác nhận kết quả QC thành `QC_COMPLETED` hoặc `QC_FAILED`, rồi Trưởng kho duyệt hoặc từ chối phiếu nhập kho.

### Features List
* [US-WMS-02: Tiếp nhận & Lập Lệnh Nhập kho](./features/feature-planner-receipt-drafting.md)
* [Nhân viên kho Tiếp nhận & Đếm hàng thực tế](./features/feature-storekeeper-receipt-receive.md)
* [US-WMS-03: Nhân viên kho Kiểm tra Chất lượng Inbound theo Sample](./features/feature-qc-inbound-inspection.md)
* [US-WMS-04: Xử lý Hàng lỗi trong Quarantine Zone](./features/feature-manager-quarantine-handling.md)
* [US-WMS-05: Duyệt Nhập kho Chính thức](./features/feature-manager-receipt-approval.md)

### Cross-Spec Mapping Notes
- US-WMS-04 trong spec này chỉ bao phủ xử lý hàng lỗi inbound theo hướng Return to Vendor (RTV) và Debit Note. Luồng tiêu hủy hàng lỗi từ Quarantine Zone được đặc tả tại [009-returns-scrap-disposal](../009-returns-scrap-disposal/spec.md) để giữ một nguồn sự thật cho disposal approval thresholds.

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Planner | Maker | Tiếp nhận thông tin hàng về từ Công ty mẹ, tạo Lệnh nhập kho |
| Storekeeper (STOREKEEPER) | Checker nội bộ kho | Rà soát và kết luận kết quả QC do Nhân viên kho ghi nhận, xác nhận phiếu sang `QC_COMPLETED`, chỉ định putaway sau khi phiếu nhập được duyệt |
| Nhân viên kho (WAREHOUSE_STAFF) kiêm QC Staff | Maker | Tiếp nhận, kiểm đếm thực tế, tạo bản nháp `DRAFT`; bốc xếp, di chuyển hàng; kiểm tra ngoại quan/chất lượng, ghi nhận số lượng đạt/lỗi và lý do QC |
| Trưởng kho | Checker | Đối chiếu kết quả QC và phê duyệt Phiếu nhập kho chính thức, quyết định xử lý hàng lỗi tại Quarantine |
| Kế toán viên | Maker | Tạo Debit Note yêu cầu bồi hoàn nếu hàng lỗi được trả về NCC |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Receipt Drafting](./features/feature-planner-receipt-drafting.md#3-functional-requirements-ears)
* [EARS - Receipt Receive](./features/feature-storekeeper-receipt-receive.md#3-functional-requirements-ears)
* [EARS - Inbound QC](./features/feature-qc-inbound-inspection.md#3-functional-requirements-ears)
* [EARS - Quarantine Handling](./features/feature-manager-quarantine-handling.md#3-functional-requirements-ears)
* [EARS - Receipt Approval](./features/feature-manager-receipt-approval.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Receipt creation -> inventory update latency | APPROVED flow: <= 2s |
| NFR-002 | QC result save response time | <= 500ms |
| NFR-003 | Support concurrent receipt processing at 3 warehouses | No deadlock |
| NFR-004 | QC_FAILED / quarantine routing latency | Quarantine inventory update: <= 2s |

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
- `status` (VARCHAR(30), DEFAULT 'PENDING_RECEIPT', CHECK IN ('PENDING_RECEIPT','DRAFT','QC_COMPLETED','QC_FAILED','APPROVED','REJECTED'))
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
- `batch_id` (BIGINT, FK→batches) -- set sau khi APPROVED
- `location_id` (BIGINT, FK→warehouse_locations) -- set khi putaway
- `expected_qty` (DECIMAL(10,2), NOT NULL)
- `actual_qty` (DECIMAL(10,2))
- `sample_qty` (DECIMAL(10,2))
- `sample_passed_qty` (DECIMAL(10,2))
- `sample_failed_qty` (DECIMAL(10,2))
- `qc_sampling_method` (VARCHAR(30)) -- FULL_INSPECTION nếu supplier chưa có 5 receipt APPROVED trước đó; RANDOM_SAMPLE nếu supplier đã có ít nhất 5 receipt APPROVED
- `qc_result` (VARCHAR(20), CHECK IN ('PENDING','PASSED','FAILED','PARTIAL'))
- `qc_failure_reason` (TEXT)
- `unit_cost` (DECIMAL(18,2))

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Receipt Drafting](./features/feature-planner-receipt-drafting.md#4-api-endpoints)
* [APIs - Receipt Receive & Putaway](./features/feature-storekeeper-receipt-receive.md#4-api-endpoints)
* [APIs - Inbound QC](./features/feature-qc-inbound-inspection.md#4-api-endpoints)
* [APIs - Quarantine Handling](./features/feature-manager-quarantine-handling.md#4-api-endpoints)
* [APIs - Receipt Approval](./features/feature-manager-receipt-approval.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| RECEIPT_ALREADY_APPROVED | 409 | Duplicate approval attempt |
| QC_SAMPLE_MISMATCH | 422 | sample_passed_qty + sample_failed_qty != sample_qty |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update |
| NO_QUARANTINE_ITEMS | 400 | No failed items to process |

### QC Sampling Rules
- Nếu supplier chưa có ít nhất 5 receipt ở trạng thái `APPROVED` trước đó, hệ thống SHALL mặc định `qc_sampling_method = FULL_INSPECTION`.
- Nếu supplier đã có ít nhất 5 receipt ở trạng thái `APPROVED` trước đó, hệ thống SHALL mặc định `qc_sampling_method = RANDOM_SAMPLE`.
- Đếm số receipt `APPROVED` được tính theo `supplier_id` trong lịch sử inbound receipts của hệ thống.

### Inventory Timing Rules
- Submitting QC sample results records inspection data only; it SHALL NOT update regular or quarantine inventory.
- Confirming `QC_COMPLETED` holds the lot for Trưởng kho approval; inventory is still unchanged until `APPROVED`.
- Approving a `QC_COMPLETED` receipt increases regular inventory by `actual_qty`.
- Confirming `QC_FAILED` routes the whole lot to quarantine and increases quarantine inventory by `actual_qty`; this inventory is excluded from available selling stock.

### Audit Trail
- Every inbound mutation SHALL create an audit log with `actor`, `action`, `entity_type`, `entity_id`, `entity_code`, `timestamp`, `before`, and `after`.
- `RECEIPT_CREATE`: create receipt header with status `PENDING_RECEIPT`.
- `RECEIPT_RECEIVE`: store `actual_qty`, move receipt to `DRAFT`.
- `RECEIPT_QC_SUBMIT`: store sample quantities, sampling method, and QC reason; do not change inventory.
- `RECEIPT_QC_CONFIRM`: move receipt to `QC_COMPLETED` or `QC_FAILED`.
- `RECEIPT_APPROVE`: move receipt to `APPROVED`, create/update batch, and increase regular inventory.
- `RECEIPT_REJECT`: move receipt to `REJECTED`, store rejection reason, and do not change inventory.
- `QUARANTINE_RTV_CREATE`: create RTV request, `RETURN_TO_VENDOR` adjustment, and Debit Note while quarantine inventory remains unchanged.
- `QUARANTINE_RTV_CONFIRM`: decrease quarantine inventory and mark RTV as completed.
- `INVENTORY_UPDATE`: record before/after values for `total_qty`, `reserved_qty`, and `location_id` on every inventory-affecting inbound transition.

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Receipt Drafting](./features/feature-planner-receipt-drafting.md#5-acceptance-criteria)
* [Acceptance - Receipt Receive](./features/feature-storekeeper-receipt-receive.md#5-acceptance-criteria)
* [Acceptance - Inbound QC](./features/feature-qc-inbound-inspection.md#5-acceptance-criteria)
* [Acceptance - Quarantine Handling](./features/feature-manager-quarantine-handling.md#5-acceptance-criteria)
* [Acceptance - Receipt Approval](./features/feature-manager-receipt-approval.md#5-acceptance-criteria)

## 9. Out of Scope

- Barcode/QR scanning for receiving
- Integration with Công ty mẹ API (Zalo/Email manual for Sprint 1)
- Automated putaway optimization (FEFO bin suggestion)
- Supplier quality rating tracking
