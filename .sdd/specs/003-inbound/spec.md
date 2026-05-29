# Feature Specification: Nhập hàng & QC Inbound (Receiving & Quality Check)

**Spec ID**: 003-inbound
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-02, US-WMS-03, US-WMS-04, US-WMS-05

---

## 1. Context and Goal

Quy trình nhập hàng là đầu vào của toàn bộ hệ thống tồn kho. Hàng hóa từ Công ty mẹ
được thông báo qua Zalo/Email → Planner lập lệnh → Thủ kho đếm thực tế → QC kiểm tra
chất lượng → Hàng đạt nhập kho, hàng lỗi vào Quarantine → Trưởng kho duyệt chính thức.

**Goal:** Xây dựng luồng nhập hàng hoàn chỉnh từ tiếp nhận thông tin đến duyệt nhập
chính thức, đảm bảo kiểm soát chất lượng (QC) và xử lý hàng lỗi, audit trail đầy đủ.

## 2. Actors

| Actor | Vai trò |
|-------|---------|
| Planner | Tiếp nhận thông tin, lập Lệnh nhập |
| Thủ kho | Đếm hàng thực tế, cất hàng vào Bin |
| Nhân viên kho (QC) | Kiểm tra QC Inbound, nhập kết quả |
| Trưởng kho kiêm Trưởng QC | Duyệt hàng lỗi, duyệt nhập chính thức |
| Kế toán viên | Tạo Debit Note khi trả hàng NCC |

## 3. Functional Requirements (EARS)

**Ubiquitous:**
- The system SHALL always generate a unique receipt code for every receipt document.
- The system SHALL always record an audit log for every state transition in the
  inbound flow (PENDING → QC_IN_PROGRESS → APPROVED → COMPLETED / REJECTED).

**Event-driven:**
- WHEN a Planner creates a receipt, the system SHALL require: warehouse_id,
  expected items (product_id + expected_qty), and source reference.
- WHEN a Thủ kho enters the actual received quantity, the system SHALL update
  the receipt status to DRAFT (ready for QC).
- WHEN a Nhân viên kho submits QC results, the system SHALL:
  - Split quantity: qc_passed → kept for approval; qc_failed → quarantine zone
  - IF qc_failed > 0, the system SHALL flag the receipt for Trưởng kho review
- WHEN a Trưởng kho approves a receipt, the system SHALL:
  - Increase inventory.quantity by qc_passed_qty
  - Set inventory.is_quarantine = false for passed items
  - Update receipt status to APPROVED
- WHEN a Trưởng kho selects "Trả NCC" for quarantine items, the system SHALL:
  - Create a Return-to-Vendor (RTV) document
  - Decrease quarantine inventory
  - Notify Kế toán viên to create a Debit Note
- WHEN a Trưởng kho selects "Tiêu hủy" for quarantine items, the system SHALL:
  - Apply approval threshold rules (5-100M: Trưởng kho, >100M: CEO)
  - Create a disposal document
  - Decrease quarantine inventory

**State-driven:**
- WHILE a receipt is in PENDING status, the system SHALL NOT update inventory.
- WHILE goods are in Quarantine Zone (is_quarantine = true), the system SHALL
  exclude them from available inventory calculations.

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Receipt creation → inventory update latency | ≤ 2s after approval |
| NFR-002 | QC result save response time | ≤ 500ms |
| NFR-003 | Support concurrent receipt processing at 3 warehouses | No deadlock |

## 5. Data Model

### Receipt
- `id`, `receipt_code` (UNIQUE), `warehouse_id` (FK), `created_by` (FK→User),
  `status` (PENDING → QC_IN_PROGRESS → APPROVED → COMPLETED / REJECTED),
  `notes`, `version`

### ReceiptItem
- `id`, `receipt_id` (FK), `product_id` (FK), `batch_id` (FK, set during QC),
  `expected_qty`, `received_qty`, `qc_passed_qty`, `qc_failed_qty`,
  `qc_status` (PENDING/PASSED/FAILED/PARTIAL), `qc_notes`,
  `bin_location_id` (FK)

### AuditLog
- (same as 001-system-admin)

## 6. API Spec

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/receipts | Bearer | List receipts (filter by warehouse, status) |
| POST | /api/v1/receipts | PLANNER | Create receipt (PENDING) |
| GET | /api/v1/receipts/{id} | Bearer | Get receipt detail with items |
| PUT | /api/v1/receipts/{id}/qc | WAREHOUSE_STAFF | Submit QC results |
| PUT | /api/v1/receipts/{id}/approve | WAREHOUSE_MANAGER | Approve receipt → update inventory |
| PUT | /api/v1/receipts/{id}/reject | WAREHOUSE_MANAGER | Reject receipt, no inventory impact |
| PUT | /api/v1/receipts/{id}/complete | STORE_KEEPER | Putaway to bins, mark COMPLETED |
| POST | /api/v1/receipts/{id}/rtv | WAREHOUSE_MANAGER | Return to vendor from quarantine |
| POST | /api/v1/receipts/{id}/dispose | WAREHOUSE_MANAGER | Dispose quarantine goods |

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| RECEIPT_ALREADY_APPROVED | 409 | Duplicate approval attempt |
| QC_PASSED_FAILED_MISMATCH | 422 | passed + failed ≠ received |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update |
| NO_QUARANTINE_ITEMS | 400 | No failed items to process |
| APPROVAL_THRESHOLD_EXCEEDED | 403 | Exceeds user's approval authority |

## 8. Acceptance Criteria

1. Given a receipt with 100 units expected and 100 received,
   when QC reports 80 passed + 20 failed,
   then system SHALL move 20 to quarantine and hold 80 for approval.
2. Given a receipt with 80 passed units,
   when Trưởng kho approves,
   then inventory.quantity SHALL increase by exactly 80.
3. Given 20 units in quarantine,
   when Trưởng kho selects "Tiêu hủy" with value 50M (< 100M),
   then system SHALL auto-approve (Trưởng kho's authority).
4. Given 20 units in quarantine, value 150M (> 100M),
   when Trưởng kho selects "Tiêu hủy",
   then system SHALL route approval to CEO.

## 9. Out of Scope

- Barcode/QR scanning for receiving
- Integration with Công ty mẹ API (Zalo/Email manual for Sprint 1)
- Automated putaway optimization (FEFO bin suggestion)
- Supplier quality rating tracking
