# Feature: Kế toán Quản lý Invoice, Công nợ & Thanh toán (US-WMS-10)

## 1. Context and Goal

Sau khi tài xế xác nhận giao thành công full Delivery Order bằng POD + OTP hợp lệ, hệ thống tự động tạo invoice và ghi nhận công nợ cho đúng Delivery Order đó, đồng thời chuyển Delivery Order sang `COMPLETED`. Kế toán viên không tạo invoice thủ công từ Delivery Order để tránh sai sót nhập liệu; thay vào đó, kế toán theo dõi invoice/công nợ, upload ảnh giao dịch cho từng đợt thanh toán và gửi lên Kế toán trưởng phê duyệt.

Công nợ chỉ được trừ sau khi Kế toán trưởng phê duyệt đợt thanh toán. Khi invoice được thanh toán hết, Delivery Order chuyển sang `CLOSED`.

## 2. Actors

- **Kế toán viên**: Xem invoice/công nợ trong phạm vi kho được gán, ghi nhận từng đợt thanh toán, upload ảnh thông tin giao dịch.
- **Kế toán trưởng**: Phê duyệt hoặc từ chối đợt thanh toán; chỉ sau phê duyệt thì công nợ mới được trừ.

## 3. Functional Requirements (EARS)

- **Ubiquitous:**
  - The system SHALL automatically create invoice and receivable only when Driver Mobile POD confirmation succeeds for a full Delivery Order.
  - The system SHALL NOT create invoices for partial delivery because partial delivery is not supported in Sprint 1.
  - The system SHALL create one invoice per successfully confirmed Delivery Order; confirming one Delivery Order in a trip SHALL NOT create or update invoices for other Delivery Orders in the same trip.
  - The user audit for automatic invoice creation SHALL be linked to the driver `CONFIRM_DELIVERY` action; invoice/payment mutations SHALL still keep before/after audit data.
  - Accountant users SHALL NOT manually create invoices from Delivery Orders; invoice creation is automatic after successful POD + OTP.
  - Every payment submission SHALL include a transaction image before it can be submitted for approval.
  - Payment submissions SHALL NOT reduce outstanding receivable until approved by the Chief Accountant.
  - Accounting views SHALL enforce both accounting role permission and warehouse assignment scope unless the role is explicitly configured for company-wide accounting access.
- **Event-driven:**
  - WHEN a Delivery Order is confirmed by POD + OTP, the system SHALL:
    - Validate the Delivery Order is `IN_TRANSIT`.
    - Validate the delivery confirmation is for the full Delivery Order.
    - Automatically create an invoice for that Delivery Order only.
    - Increase dealer receivable by the invoice amount.
    - Move the Delivery Order to `COMPLETED`.
  - WHEN an Accountant submits a payment record, the system SHALL:
    - Require payment amount and transaction image.
    - Store the payment in `PENDING_APPROVAL`.
    - Keep invoice outstanding amount unchanged.
    - Create payment submission audit with before/after state.
  - WHEN the Chief Accountant approves a payment, the system SHALL:
    - Decrease invoice outstanding amount by the approved amount.
    - Update invoice status to `PARTIALLY_PAID` or `PAID`.
    - Move the Delivery Order to `CLOSED` when the invoice is fully paid.
    - Create payment approval audit with before/after state.
  - WHEN the Chief Accountant rejects a payment, the system SHALL:
    - Require rejection reason.
    - Store rejection reason.
    - Leave outstanding receivable unchanged.
    - Create payment rejection audit with before/after state.

## 4. API Endpoints

- `GET /api/v1/accounting/invoices?warehouseId={warehouseId}` - List invoices and receivables in the accountant's warehouse scope.
- `POST /api/v1/accounting/invoices/{invoiceId}/payments` - Accountant submits a payment with transaction image.
- `PUT /api/v1/accounting/payments/{paymentId}/approve` - Chief Accountant approves a payment and reduces receivable.
- `PUT /api/v1/accounting/payments/{paymentId}/reject` - Chief Accountant rejects a payment with reason.

## 5. Acceptance Criteria

- **Scenario: Auto-create invoice after full delivery confirmation**
  - Given a Delivery Order is in `IN_TRANSIT`
  - And the current delivery attempt has valid `goodsImage`, `signDocumentImage`, and verified OTP
  - When Driver confirms full delivery successfully
  - Then the system SHALL auto-create invoice and receivable for that Delivery Order only, link audit to `CONFIRM_DELIVERY`, and move the Delivery Order to `COMPLETED`.

- **Scenario: Other Delivery Orders in the same trip are not invoiced**
  - Given a trip has multiple Delivery Orders in `IN_TRANSIT`
  - When Driver confirms one Delivery Order successfully
  - Then the system SHALL create invoice only for the confirmed Delivery Order and SHALL NOT change invoice state for other Delivery Orders in the trip.

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
