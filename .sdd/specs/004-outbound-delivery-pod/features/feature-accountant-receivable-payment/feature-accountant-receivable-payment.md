# Feature: Ke toan Quan ly Invoice, Cong no & Thanh toan (US-WMS-10)

## 1. Context and Goal

Sau khi POD va OTP giao hang thanh cong, he thong tu dong tao invoice va ghi nhan cong no cho dai ly, dong thoi chuyen Delivery Order sang `COMPLETED`. Ke toan vien khong tao invoice thu cong de tranh sai sot nhap lieu; thay vao do, ke toan theo doi invoice/cong no, upload anh giao dich cho tung dot thanh toan va gui len cho Ke toan truong phe duyet. Cong no chi duoc tru sau khi Ke toan truong phe duyet dot thanh toan. Khi invoice duoc thanh toan het, Delivery Order chuyen sang `CLOSED`.

## 2. Actors

- **Ke toan vien**: Xem invoice/cong no, ghi nhan tung dot thanh toan, upload anh thong tin giao dich.
- **Ke toan truong**: Phe duyet hoac tu choi dot thanh toan; chi sau phe duyet thi cong no moi duoc tru.

## 3. Functional Requirements (EARS)

- **Ubiquitous:**
  - The system SHALL automatically create invoice and receivable when POD + OTP confirmation succeeds.
  - The system SHALL create `INVOICE_AUTO_CREATE_FROM_DO`, `PAYMENT_SUBMIT`, `PAYMENT_APPROVE`, and `PAYMENT_REJECT` audit log entries for invoice and payment mutations.
  - Accountant users SHALL NOT manually create invoices from Delivery Orders; invoice creation is automatic after successful POD + OTP.
  - Every payment submission SHALL include a transaction image before it can be submitted for approval.
  - Payment submissions SHALL NOT reduce outstanding receivable until approved by the Chief Accountant.
  - Accounting views SHALL enforce both accounting role permission and warehouse assignment scope unless the role is explicitly configured for company-wide accounting access.
- **Event-driven:**
  - WHEN a Delivery Order is confirmed by POD + OTP, the system SHALL:
    - Automatically create an invoice for the Delivery Order.
    - Increase dealer receivable by the invoice amount.
    - Move the Delivery Order to `COMPLETED`.
  - WHEN an Accountant submits a payment record, the system SHALL:
    - Require payment amount and transaction image.
    - Store the payment in `PENDING_APPROVAL`.
    - Keep invoice outstanding amount unchanged.
  - WHEN the Chief Accountant approves a payment, the system SHALL:
    - Decrease invoice outstanding amount by the approved amount.
    - Update invoice status to `PARTIALLY_PAID` or `PAID`.
    - Move the Delivery Order to `CLOSED` when the invoice is fully paid.
  - WHEN the Chief Accountant rejects a payment, the system SHALL store rejection reason and leave outstanding receivable unchanged.

## 4. API Endpoints

- `GET /api/v1/accounting/invoices?warehouseId={warehouseId}` - List invoices and receivables in the accountant's warehouse scope.
- `POST /api/v1/accounting/invoices/{invoiceId}/payments` - Accountant submits a payment with transaction image.
- `PUT /api/v1/accounting/payments/{paymentId}/approve` - Chief Accountant approves a payment and reduces receivable.
- `PUT /api/v1/accounting/payments/{paymentId}/reject` - Chief Accountant rejects a payment with reason.

## 5. Acceptance Criteria

- **Scenario: Auto-create invoice after delivery confirmation**
  - Given a Delivery Order is in `IN_TRANSIT`
  - When POD evidence and dealer OTP are successfully confirmed
  - Then the system SHALL auto-create invoice and receivable, create an audit log, and move the Delivery Order to `COMPLETED`.

- **Scenario: Accountant submits payment with transaction image**
  - Given an invoice is `OPEN` or `PARTIALLY_PAID`
  - When an Accountant submits payment amount with transaction image
  - Then the system SHALL create a `PENDING_APPROVAL` payment record and keep outstanding receivable unchanged.

- **Scenario: Chief Accountant approves partial payment**
  - Given a payment is `PENDING_APPROVAL`
  - When the Chief Accountant approves payment lower than outstanding amount
  - Then the system SHALL reduce outstanding amount, mark invoice `PARTIALLY_PAID`, and keep Delivery Order in `COMPLETED`.

- **Scenario: Chief Accountant approves final payment**
  - Given a payment is `PENDING_APPROVAL`
  - When the Chief Accountant approves payment that fully settles the invoice
  - Then the system SHALL reduce outstanding amount to zero, mark invoice `PAID`, and move Delivery Order to `CLOSED`.

- **Scenario: Chief Accountant rejects payment**
  - Given a payment is `PENDING_APPROVAL`
  - When the Chief Accountant rejects it with a reason
  - Then the system SHALL keep outstanding receivable unchanged and store the rejection reason.
