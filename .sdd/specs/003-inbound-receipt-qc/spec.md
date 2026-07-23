# Feature Specification: Nhập hàng & QC Inbound (Receiving & Quality Check)

**Spec ID**: 003-inbound-receipt-qc
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-02, US-WMS-03, US-WMS-04, US-WMS-05, US-WMS-06

---

## 1. Context and Goal

Quy trình nhập hàng là đầu vào của toàn bộ hệ thống tồn kho. Hàng hóa từ Công ty mẹ được thông báo qua Zalo/Email, Planner lập lệnh, Nhân viên kho kiểm đếm thực tế và tạo bản nháp `DRAFT`, Nhân viên kho lấy mẫu QC theo từng lô, Storekeeper xác nhận kết quả QC thành `QC_COMPLETED` hoặc `QC_FAILED`, rồi Trưởng kho duyệt hoặc từ chối phiếu nhập kho. Trưởng kho approval chỉ unlock putaway; hàng vào Bin xong mới tăng `inventories.total_qty`.

## Clarifications

### Session 2026-07-23

- Q: Phân quyền quy trình Nhập hàng Inbound (`003-inbound-receipt-qc`) -> A: Tuân thủ phân quyền 4 giai đoạn theo `role.md`: `PLANNER` tạo Lệnh nhập kho (`Receipt Create`); `STOREKEEPER` và `WH_STAFF` tiếp nhận hàng thực tế (`Receipt Receive`) và QC Inbound; `STOREKEEPER` độc quyền lập kế hoạch xếp bin (`Putaway Plan`). `WH_MANAGER` phê duyệt phiếu nhập và xử lý Quarantine.

### Session 2026-06-11

- Q: Putaway should happen before or after Trưởng kho approval? -> A: After approval; approval unlocks putaway.
- Q: Should inbound QC support PARTIAL pass/fail in Sprint 1? -> A: No PARTIAL; any failed sample makes the whole lot `QC_FAILED`.
- Q: What happens when Trưởng kho rejects a `QC_COMPLETED` receipt? -> A: Move it to `RETURN_TO_SUPPLIER_PENDING`.
- Q: Can a Trưởng kho approve receipts for any warehouse? -> A: No; only assigned warehouse.
- Q: Should duplicate/concurrent approve/reject attempts be blocked? -> A: Yes; use idempotency/state and optimistic locking.
- Q: Does rejecting a `QC_COMPLETED` receipt create RTV or Debit Note? -> A: No; RTV/Debit Note is only for `QC_FAILED` quarantine handling in Sprint 1.
- Q: Can a receipt have multiple pending RTV requests? -> A: No; duplicate RTV creation is rejected while a pending or confirmed RTV already exists for the receipt.
- Q: When does `RETURN_TO_SUPPLIER_PENDING` finish? -> A: Storekeeper confirms physical handover when the supplier's vehicle arrives, then the receipt moves to `RETURNED_TO_SUPPLIER`.
- Q: Can Storekeeper confirm a partial RTV quantity? -> A: No; RTV confirmation must return the full quarantined quantity for the receipt.

### Features List

- [US-WMS-02: Tiếp nhận & Lập Lệnh Nhập kho](./features/feature-planner-receipt-drafting/feature-planner-receipt-drafting.md)
- [US-WMS-03: Nhân viên kho Tiếp nhận & Đếm số lượng hàng thực tế](./features/feature-warehouse-staff-receipt-counting/feature-warehouse-staff-receipt-counting.md)
- [US-WMS-04: Nhân viên kho Kiểm tra Chất lượng Inbound theo Sample](./features/feature-qc-inbound-inspection.md)
- [US-WMS-05: Xử lý Hàng lỗi trong Quarantine Zone](./features/feature-manager-quarantine-handling.md)
- [US-WMS-06: Duyệt Nhập kho Chính thức](./features/feature-manager-receipt-approval.md)

### Cross-Spec Mapping Notes

- US-WMS-04 trong spec này chỉ bao phủ xử lý hàng lỗi inbound theo hướng Return to Vendor (RTV) và Debit Note. Feature 003 chỉ hiển thị nút "Trả NCC"; luồng tiêu hủy hàng lỗi từ Quarantine Zone được đặc tả tại [009-returns-scrap-disposal](../009-returns-scrap-disposal/spec.md) để giữ một nguồn sự thật cho disposal approval thresholds.

## 2. Actors

| Actor                                         | Vai trò            | Nghiệp vụ liên quan                                                                                                                                                                              |
| --------------------------------------------- | ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Planner                                       | Maker              | Tiếp nhận thông tin hàng về từ Công ty mẹ, tạo Lệnh nhập kho                                                                                                                                     |
| Storekeeper (STOREKEEPER)                     | Checker nội bộ kho | Rà soát và kết luận kết quả QC do Nhân viên kho ghi nhận, xác nhận phiếu sang `QC_COMPLETED`, chỉ định putaway sau khi phiếu nhập được duyệt, xác nhận giao trả NCC cho RTV hoặc hàng bị từ chối |
| Nhân viên kho (WAREHOUSE_STAFF) kiêm QC Staff | Maker              | Tiếp nhận, kiểm đếm thực tế, tạo bản nháp `DRAFT`; bốc xếp, di chuyển hàng; kiểm tra ngoại quan/chất lượng, ghi nhận số lượng đạt/lỗi và lý do QC                                                |
| Trưởng kho                                    | Checker            | Đối chiếu kết quả QC và phê duyệt Phiếu nhập kho chính thức; tạo RTV request cho hàng lỗi tại Quarantine trong feature 003                                                                       |
| Kế toán viên                                  | Maker              | Theo dõi Debit Note do hệ thống tự tạo khi hàng lỗi được lập RTV                                                                                                                                 |

## 3. Functional Requirements (EARS)

_Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:_

- [EARS - Receipt Drafting](./features/feature-planner-receipt-drafting/feature-planner-receipt-drafting.md#3-functional-requirements-ears)
- [EARS - Receipt Counting](./features/feature-warehouse-staff-receipt-counting/feature-warehouse-staff-receipt-counting.md#3-functional-requirements-ears)
- [EARS - Inbound QC](./features/feature-qc-inbound-inspection.md#3-functional-requirements-ears)
- [EARS - Quarantine Handling](./features/feature-manager-quarantine-handling.md#3-functional-requirements-ears)
- [EARS - Receipt Approval](./features/feature-manager-receipt-approval.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID      | Requirement                                           | Target                                                           |
| ------- | ----------------------------------------------------- | ---------------------------------------------------------------- |
| NFR-001 | Receipt creation -> inventory update latency          | APPROVED + putaway flow: <= 2s                                   |
| NFR-002 | QC result save response time                          | <= 500ms                                                         |
| NFR-003 | Support concurrent receipt processing at 3 warehouses | No deadlock                                                      |
| NFR-004 | Manager quarantine intake latency                     | Quarantine inventory update after Trưởng kho confirmation: <= 2s |

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
- `expected_qty` (INTEGER, NOT NULL, > 0)
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
- `status` (VARCHAR(30), DEFAULT 'PENDING_RECEIPT', CHECK IN ('PENDING_RECEIPT','DRAFT','QC_COMPLETED','QC_FAILED','APPROVED','RETURN_TO_SUPPLIER_PENDING','RETURNED_TO_SUPPLIER'))
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
- `batch_id` (BIGINT, FK→batches) -- set sau khi APPROVED; batch key theo product + source receipt/date
- `location_id` (BIGINT, FK→warehouse_locations) -- set khi putaway
- `expected_qty` (DECIMAL(10,2), NOT NULL)
- `actual_qty` (DECIMAL(10,2))
- `sample_qty` (DECIMAL(10,2))
- `sample_passed_qty` (DECIMAL(10,2))
- `sample_failed_qty` (DECIMAL(10,2))
- `qc_sampling_method` (VARCHAR(30)) -- FULL_INSPECTION nếu supplier chưa có 5 receipt APPROVED trước đó; RANDOM_SAMPLE nếu supplier đã có ít nhất 5 receipt APPROVED
- `qc_result` (VARCHAR(20), CHECK IN ('PENDING','PASSED','FAILED'))
- `qc_failure_reason` (TEXT)
- `unit_cost` (DECIMAL(18,2))

### adjustments (RTV)

- `id` (BIGSERIAL, PK)
- `adjustment_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches)
- `location_id` (BIGINT, FK→warehouse_locations) -- Quarantine location
- `quantity_adjustment` (DECIMAL(10,2), NOT NULL) -- negative for returned quantity
- `type` (VARCHAR(30), CHECK IN ('RETURN_TO_VENDOR'), NOT NULL)
- `reference_id` (BIGINT) -- source receipt id
- `reference_type` (VARCHAR(50), DEFAULT 'RECEIPT')
- `reason` (TEXT, NOT NULL)
- `approved_by` (BIGINT, FK→users) -- set when Storekeeper confirms physical return
- `approved_at` (TIMESTAMPTZ) -- set when Storekeeper confirms physical return
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `created_by` (BIGINT, FK→users, NOT NULL) -- Trưởng kho who creates RTV request
- `created_at` (TIMESTAMPTZ)

### debit_notes

- `id` (BIGSERIAL, PK)
- `debit_note_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `supplier_id` (BIGINT, FK→suppliers, NOT NULL)
- `receipt_id` (BIGINT, FK→receipts)
- `failed_qty` (DECIMAL(10,2), NOT NULL)
- `amount` (DECIMAL(18,2), NOT NULL)
- `reason` (TEXT, NOT NULL)
- `created_by` (BIGINT, FK→users, NOT NULL) -- authenticated Trưởng kho; record is system-generated from RTV action
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `created_at` (TIMESTAMPTZ)

## 6. API Spec

_Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:_

- [APIs - Receipt Drafting](./features/feature-planner-receipt-drafting/feature-planner-receipt-drafting.md#4-api-endpoints)
- [APIs - Receipt Counting](./features/feature-warehouse-staff-receipt-counting/feature-warehouse-staff-receipt-counting.md#4-api-endpoints)
- [APIs - Inbound QC](./features/feature-qc-inbound-inspection.md#4-api-endpoints)
- [APIs - Quarantine Handling](./features/feature-manager-quarantine-handling.md#4-api-endpoints)
- [APIs - Receipt Approval](./features/feature-manager-receipt-approval.md#4-api-endpoints)

## 7. Error Handling

| Error                       | HTTP | Condition                                                  |
| --------------------------- | ---- | ---------------------------------------------------------- |
| RECEIPT_ALREADY_APPROVED    | 409  | Duplicate approval attempt                                 |
| RECEIPT_ALREADY_DECIDED     | 409  | Duplicate approve/reject attempt after a final decision    |
| FORBIDDEN_RECEIPT_WAREHOUSE | 403  | Trưởng kho is not assigned to the receipt warehouse        |
| QC_SAMPLE_MISMATCH          | 422  | sample_passed_qty + sample_failed_qty != sample_qty        |
| INVENTORY_VERSION_CONFLICT  | 409  | Concurrent inventory update                                |
| NO_QUARANTINE_ITEMS         | 400  | No failed items to process                                 |
| RTV_ALREADY_EXISTS          | 409  | Pending or confirmed RTV already exists for receipt        |
| RTV_ALREADY_CONFIRMED       | 409  | Duplicate RTV confirmation attempt                         |
| RTV_QUANTITY_MISMATCH       | 422  | Returned quantity does not equal full quarantined quantity |

### QC Sampling Rules

- Nếu supplier chưa có ít nhất 5 receipt ở trạng thái `APPROVED` trước đó, hệ thống SHALL mặc định `qc_sampling_method = FULL_INSPECTION`.
- Nếu supplier đã có ít nhất 5 receipt ở trạng thái `APPROVED` trước đó, hệ thống SHALL mặc định `qc_sampling_method = RANDOM_SAMPLE`.
- Đếm số receipt `APPROVED` được tính theo `supplier_id` trong lịch sử inbound receipts của hệ thống.
- Sprint 1 SHALL NOT support `PARTIAL` QC results. If any inspected sample fails the applicable QC threshold, Storekeeper confirmation SHALL move the whole receipt to `QC_FAILED`.

### Inventory Timing Rules

- Submitting QC sample results records inspection data only; it SHALL NOT update regular or quarantine inventory.
- Confirming `QC_COMPLETED` holds the lot for Trưởng kho approval; regular and available inventory remain unchanged until approval and putaway completion.
- Approving a `QC_COMPLETED` receipt creates/resolves the batch and unlocks putaway, but it SHALL NOT increase available stock until Storekeeper completes putaway into a regular Bin location.
- Completing putaway after approval increases regular inventory by `actual_qty` at the selected regular Bin location.
- Confirming `QC_FAILED` routes the whole lot to quarantine and increases quarantine inventory by `actual_qty`; this inventory is excluded from available selling stock.
- Rejecting a `QC_COMPLETED` receipt moves it to `RETURN_TO_SUPPLIER_PENDING`, stores the rejection reason, and SHALL NOT create batches, RTV, Debit Note, or inventory.
- Confirming supplier handover for a `RETURN_TO_SUPPLIER_PENDING` receipt moves it to `RETURNED_TO_SUPPLIER` and SHALL NOT create inventory.
- Creating RTV for a `QC_FAILED` receipt creates a pending `RETURN_TO_VENDOR` adjustment and Debit Note, but it SHALL NOT decrease quarantine inventory.
- Creating RTV for a `QC_FAILED` receipt SHALL be rejected with HTTP 409 if a pending or confirmed RTV already exists for the receipt.
- Confirming physical return to supplier for a pending RTV requires returning the full quarantined quantity, decreases quarantine inventory by that full quantity, and SHALL keep the source receipt status as `QC_FAILED`.
- Trưởng kho approve/reject actions SHALL be limited to receipts in warehouses assigned to the authenticated Trưởng kho.
- Duplicate approve/reject attempts and concurrent stale-version updates SHALL return HTTP 409 without applying duplicate inventory changes.

### Audit Trail

- Every inbound mutation SHALL create an audit log with `actor`, `action`, `entity_type`, `entity_id`, `entity_code`, `timestamp`, `before`, and `after`.
- `RECEIPT_CREATE`: create receipt header with status `PENDING_RECEIPT`.
- `RECEIPT_RECEIVE`: accept `counted_qty`, derive and store `actual_qty`/`over_received_qty`, and move receipt to `DRAFT` only when receiving is completed.
- `RECEIPT_QC_SUBMIT`: store sample quantities, sampling method, and QC reason; do not change inventory.
- `RECEIPT_QC_CONFIRM`: move receipt to `QC_COMPLETED` or `QC_FAILED`.
- `RECEIPT_APPROVE`: move receipt to `APPROVED`, create/update batch using product plus source receipt/date, and unlock putaway.
- `RECEIPT_PUTAWAY_COMPLETE`: assign regular Bin location after approval and increase regular inventory.
- `RECEIPT_REJECT`: move receipt to `RETURN_TO_SUPPLIER_PENDING`, store rejection reason, and do not create batch, RTV, Debit Note, or inventory.
- `RECEIPT_RETURN_CONFIRM`: Storekeeper confirms physical handover of rejected goods to supplier, moving receipt to `RETURNED_TO_SUPPLIER` without inventory changes.
- `QUARANTINE_RTV_CREATE`: Trưởng kho creates RTV request, creates a pending `RETURN_TO_VENDOR` adjustment used as the RTV document, and creates Debit Note automatically while quarantine inventory remains unchanged.
- `QUARANTINE_RTV_CONFIRM`: Storekeeper confirms physical return of the full quarantined quantity to supplier, decreases quarantine inventory by that full quantity, and leaves the source receipt status as `QC_FAILED`.
- `INVENTORY_UPDATE`: record before/after values for `total_qty`, `reserved_qty`, and `location_id` on every inventory-affecting inbound transition.

## 8. Acceptance Criteria

_Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:_

- [Acceptance - Receipt Drafting](./features/feature-planner-receipt-drafting/feature-planner-receipt-drafting.md#5-acceptance-criteria)
- [Acceptance - Receipt Counting](./features/feature-warehouse-staff-receipt-counting/feature-warehouse-staff-receipt-counting.md#6-acceptance-criteria)
- [Acceptance - Inbound QC](./features/feature-qc-inbound-inspection.md#5-acceptance-criteria)
- [Acceptance - Quarantine Handling](./features/feature-manager-quarantine-handling.md#5-acceptance-criteria)
- [Acceptance - Receipt Approval](./features/feature-manager-receipt-approval.md#5-acceptance-criteria)

## 9. Out of Scope

- Barcode/QR scanning for receiving
- Integration with Công ty mẹ API (Zalo/Email manual for Sprint 1)
- Automated putaway/picking optimization beyond FIFO display order
- Supplier quality rating tracking
