# Feature Specification: Hàng hoàn trả & Tiêu hủy (Returns & Disposal)

**Spec ID**: 009-returns-disposal
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-24, US-WMS-04 (Disposal sub-flow)

---

## 1. Context and Goal

Đại lý có thể hoàn trả hàng (hàng lỗi, sai quy cách, tồn kho) và hàng trong Quarantine
Zone cần được xử lý (trả NCC hoặc tiêu hủy). Các quy trình này ảnh hưởng đến inventory
và công nợ, cần kiểm soát chặt chẽ.

**Goal:** Xây dựng quy trình xử lý hàng hoàn trả từ Đại lý (Inbound Returns) và tiêu hủy
hàng lỗi (Disposal), bao gồm QC, Credit Note, và phê duyệt theo định mức.

## 2. Actors

| Actor | Vai trò |
|-------|---------|
| Thủ kho | Lập phiếu nhập hàng hoàn |
| Nhân viên kho (QC) | QC hàng hoàn trả |
| Trưởng kho | Quyết định xử lý hàng lỗi/tồn |
| Kế toán viên | Tạo Credit Note (giảm công nợ) |
| CEO | Duyệt tiêu hủy >100M |

## 3. Functional Requirements (EARS)

**Ubiquitous:**
- The system SHALL always route returned goods through QC before admitting
  them back into inventory.

**Event-driven:**
- WHEN a Thủ kho creates a Return Receipt from a dealer, the system SHALL
  require: dealer_id, items (product + quantity + return_reason), original_DO_ref.
- WHEN QC inspects returned goods:
  - IF goods pass QC: add to regular inventory, update Bin Location
  - IF goods fail QC: move to Quarantine Zone
- WHEN Kế toán viên creates a Credit Note for returned goods, the system SHALL:
  - Decrease dealer's current_balance by the credit amount
  - Reference the original invoice and delivery order
- WHEN a Disposal document is created for quarantine goods:
  - Apply approval thresholds (Trưởng kho for 5-100M, CEO for >100M)
  - Once approved, decrease quarantine inventory
  - Record audit log with destruction reason and approving authority

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Credit Note creation + balance update | ≤ 1s |

## 5. Data Model

### ReturnReceipt
- `id`, `return_code` (UNIQUE), `dealer_id` (FK), `warehouse_id` (FK),
  `original_do_id` (FK), `status` (PENDING → QC_IN_PROGRESS → COMPLETED / REJECTED)

### CreditNote
- `id`, `credit_note_code` (UNIQUE), `dealer_id` (FK), `invoice_id` (FK),
  `return_receipt_id` (FK), `amount`, `reason`, `created_by`

## 6. API Spec

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/v1/returns | STORE_KEEPER | Create return receipt |
| PUT | /api/v1/returns/{id}/qc | WAREHOUSE_STAFF | QC returned goods |
| POST | /api/v1/returns/{id}/credit-note | ACCOUNTANT | Create Credit Note |
| POST | /api/v1/disposal | WAREHOUSE_MANAGER | Create disposal document |
| PUT | /api/v1/disposal/{id}/approve | WAREHOUSE_MANAGER / CEO | Approve disposal |

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| RETURN_EXCEEDS_ORIGINAL_SALE | 422 | Return qty > original DO qty |
| MISSING_CREDIT_NOTE_REASON | 400 | No reason for credit |
| DISPOSAL_EXCEEDS_THRESHOLD | 403 | Exceeds user's approval authority |

## 8. Acceptance Criteria

1. Given a dealer returned 10 units and QC passed 8,
   when the return is completed,
   then inventory SHALL increase by 8 and 2 SHALL go to quarantine.
2. Given a Credit Note of 5M for a dealer with balance = 100M,
   when the Credit Note is created,
   then dealer balance SHALL = 95M.

## 9. Out of Scope

- Return shipping logistics
- Restocking fees
- Automated return authorization
