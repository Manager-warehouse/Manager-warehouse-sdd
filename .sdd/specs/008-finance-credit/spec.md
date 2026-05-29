# Feature Specification: Tài chính & Công nợ Đại lý (Finance & Credit)

**Spec ID**: 008-finance-credit
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-10, US-WMS-15, US-WMS-16, US-WMS-17

---

## 1. Context and Goal

Sau khi đơn hàng được giao thành công (DELIVERED), Kế toán viên lập hóa đơn, cộng dồn
công nợ Đại lý. Hệ thống tự động kiểm tra hạn mức tín dụng (Credit Limit) và khóa/chặn
đơn mới nếu vi phạm. Cuối tháng, Kế toán trưởng chốt sổ, khóa cứng kỳ quá khứ.

**Goal:** Xây dựng hệ thống tài chính kế toán nội bộ: lập hóa đơn, ghi nhận thanh toán,
theo dõi công nợ, aging report, chốt sổ tháng, và cơ chế mở khóa tín dụng tự động.

## 2. Actors

| Actor | Vai trò |
|-------|---------|
| Kế toán viên | Lập Invoice, ghi nhận Payment, cấn trừ công nợ |
| Kế toán trưởng | Xem Aging Report, chốt sổ, thiết lập Credit Limit |
| Hệ thống (Daily Job) | Tự động CREDIT_HOLD, cảnh báo quá hạn |

## 3. Functional Requirements (EARS)

**Ubiquitous:**
- The system SHALL always maintain a running `current_balance` for each dealer
  that is updated with every invoice and payment transaction.
- The system SHALL always perform a credit check before allowing new delivery
  order creation.

**Event-driven:**
- WHEN a delivery order status changes to DELIVERED, the system SHALL
  notify Kế toán viên to create an invoice.
- WHEN a Kế toán viên creates an invoice, the system SHALL:
  - Calculate total from DO items × prices valid at shipment date
  - Set payment terms (Net 30/60 from dealer profile)
  - Increase dealer's current_balance by invoice total
  - IF current_balance ≥ credit_limit: auto-set dealer status to CREDIT_HOLD
- WHEN a payment is recorded, the system SHALL:
  - Decrease dealer's current_balance by payment amount
  - Mark matched invoices as PAID
  - IF current_balance < credit_limit × 0.8: auto-set dealer status to ACTIVE
- WHEN a daily job runs, the system SHALL scan all unpaid invoices past their
  due date by >30 days and auto-set the dealer to CREDIT_HOLD.

**State-driven:**
- WHILE dealer status is CREDIT_HOLD, the system SHALL block creation of
  new delivery orders for that dealer.
- WHILE an accounting period is CLOSED, the system SHALL reject any CREATE,
  UPDATE, or DELETE of transactions with transaction_date in that period.

**Optional:**
- WHERE invoice has been overdue >60 days, the system SHALL flag it for
  Kế toán trưởng's special attention.

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Invoice creation from DO | ≤ 1s |
| NFR-002 | Payment recording + balance update | ≤ 1s |
| NFR-003 | Aging report generation | ≤ 3s for 50+ dealers |
| NFR-004 | Monthly closing process | ≤ 30s |
| NFR-005 | Daily credit check batch job | ≤ 5s |

## 5. Data Model

### Invoice
- `id`, `invoice_code` (UNIQUE), `dealer_id` (FK), `delivery_order_id` (FK),
  `total_amount`, `status` (UNPAID / PAID / OVERDUE / CANCELLED),
  `due_date`, `created_at`

### Payment
- `id`, `invoice_id` (FK), `dealer_id` (FK), `amount`, `payment_date`,
  `payment_method` (TRANSFER/CASH), `reference`, `notes`

### AccountingPeriod
- `id`, `period_key` (UNIQUE: YYYY-MM), `status` (OPEN / CLOSED),
  `closed_by` (FK), `closed_at`, `notes`

## 6. API Spec

### Invoices
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/invoices | Bearer | List invoices (filterable) |
| POST | /api/v1/invoices | ACCOUNTANT | Create invoice from DO |
| GET | /api/v1/invoices/{id} | Bearer | Get invoice detail |

### Payments
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/v1/payments | ACCOUNTANT | Record payment, allocate to invoices |
| GET | /api/v1/payments | Bearer | List payments |

### Credit & Reports
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/dealers/{id}/credit-history | Bearer | Credit status changes |
| GET | /api/v1/credit/aging-report | ACCOUNTANT_MANAGER | Aging report |
| PUT | /api/v1/accounting/periods/{period}/close | ACCOUNTANT_MANAGER | Close period |

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| CREDIT_HOLD | 422 | Dealer is CREDIT_HOLD, blocked |
| PERIOD_CLOSED | 422 | Cannot modify closed period |
| INVOICE_ALREADY_PAID | 409 | Payment on already paid invoice |
| OVERPAYMENT_EXCEEDS_INVOICE | 422 | Payment > invoice remaining balance |
| EXCESSIVE_CREDIT_CHANGE | 422 | Credit limit change exceeds policy |

## 8. Acceptance Criteria

1. Given a dealer with credit_limit = 500M, current_balance = 0,
   when an invoice of 600M is created,
   then dealer status SHALL be CREDIT_HOLD (600M ≥ 500M).
2. Given a dealer with CREDIT_HOLD and current_balance = 600M,
   when a payment of 200M is recorded,
   then current_balance SHALL = 400M, and dealer status SHALL be ACTIVE
   (400M < 500M×0.8 = 400M — equal, so still HOLD, need >400M).
   Correction: payment of 200M → balance = 400M = 500M×0.8 → still HOLD.
   Payment of 201M → balance = 399M < 400M → ACTIVE.
3. Given a closed accounting period 2026-04,
   when a user attempts to create a transaction with transaction_date = 2026-04-15,
   then system SHALL reject with PERIOD_CLOSED.

## 9. Out of Scope

- Full General Ledger (GL) double-entry accounting
- VAT / Tax reporting
- Bank reconciliation
- Automated payment collection
- Credit scoring / credit rating models
