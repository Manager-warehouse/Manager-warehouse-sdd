# Feature: Tự động tạo Invoice & Cộng công nợ Đại lý (US-WMS-10)

## 1. Context and Goal

Sau khi tài xế xác nhận giao thành công full Delivery Order bằng POD + OTP hợp lệ, hệ thống tự động tạo invoice thanh toán cho Đại lý và cộng công nợ cho đúng Đại lý. Scope của feature này dừng tại thời điểm invoice/công nợ được tạo thành công.

Feature này không xử lý thông báo cho Kế toán viên. Phần thông báo được xử lý bởi luồng riêng. Feature này cũng không xử lý thanh toán, ghi nhận phiếu thu, phê duyệt thanh toán, cấn trừ công nợ, gia hạn ngày thanh toán hoặc chuyển Delivery Order sang `CLOSED`; các phần đó thuộc luồng tài chính/thanh toán riêng.

## 2. Actors

- **Hệ thống**: Tạo invoice và cộng công nợ tự động sau khi Driver Mobile POD confirmation thành công.

## 3. Functional Requirements (EARS)

- **Ubiquitous:**
  - The system SHALL automatically create invoice and receivable only when Driver Mobile POD confirmation succeeds for a full Delivery Order.
  - The system SHALL NOT create invoices for partial delivery because partial delivery is not supported in Sprint 1.
  - The system SHALL create one invoice per successfully confirmed Delivery Order; confirming one Delivery Order in a trip SHALL NOT create or update invoices for other Delivery Orders in the same trip.
  - The system SHALL treat invoice creation as idempotent per Delivery Order: if an invoice already exists for the Delivery Order, the system SHALL return the existing invoice result and SHALL NOT create another invoice or increase Dealer `current_balance` again.
  - The system SHALL calculate invoice lines from Delivery Order items: product, requested/full delivered quantity, and the `unit_price` snapshot stored on the Delivery Order item.
  - The Delivery Order item `unit_price` SHALL be the product selling price captured when Storekeeper prepares the picking plan; invoice creation SHALL NOT re-query product price at delivery time.
  - The system SHALL increase the Dealer `current_balance` by the invoice amount in the same transaction that confirms successful delivery.
  - IF invoice/receivable persistence fails before commit because of a technical or database error, the system SHALL roll back the whole delivery confirmation transaction; the Delivery Order SHALL remain `IN_TRANSIT`, no invoice SHALL be created, and Dealer `current_balance` SHALL NOT change.
  - The system SHALL set invoice `issue_date` to the real-time local date of the backend at invoice creation time.
  - The system SHALL set invoice `due_date` to exactly 30 calendar days after `issue_date`.
  - Accountant notification is OUT OF SCOPE for this feature and SHALL be handled by a separate notification flow.
  - Manual invoice creation from Delivery Orders is OUT OF SCOPE; invoice creation for outbound delivery is automatic after successful POD + OTP.
  - Payment due-date extension is OUT OF SCOPE for this feature and SHALL be handled by a separate finance/payment flow.
  - Payment collection, payment approval, receivable deduction, and Delivery Order `CLOSED` transition are OUT OF SCOPE for this feature and SHALL be handled by a separate finance/payment flow.
- **Event-driven:**
  - WHEN a Delivery Order is confirmed by POD + OTP, the system SHALL:
    - Validate the Delivery Order is `IN_TRANSIT`.
    - Validate the delivery confirmation is for the full Delivery Order.
    - Return the existing invoice result if the Delivery Order already has an invoice, without increasing Dealer `current_balance` again.
    - If no invoice exists, automatically create an invoice for that Delivery Order only.
    - Calculate invoice amount as the sum of each Delivery Order item delivered quantity multiplied by that item's `unit_price`.
    - Set invoice `issue_date` to the backend's current local date at creation time.
    - Set invoice `due_date` to `issue_date + 30 calendar days`.
    - Set invoice status to `UNPAID`.
    - Increase Dealer receivable/current balance by the invoice amount.
    - Move the Delivery Order to `COMPLETED`.
    - Link the automatic invoice/công nợ creation audit to the driver `CONFIRM_DELIVERY` action with before/after state.

## 4. API Endpoints

- No user-facing endpoint belongs to this feature.
- Invoice and receivable creation SHALL be triggered internally by successful Driver Mobile `confirm-delivery`.
- No notification, due-date extension, payment submission, payment approval, payment rejection, or receivable deduction endpoint belongs to this feature.

## 5. Acceptance Criteria

- **Scenario: Auto-create invoice and receivable after full delivery confirmation**
  - Given a Delivery Order is in `IN_TRANSIT`
  - And the current delivery attempt has valid `goodsImage`, `signDocumentImage`, and verified OTP
  - And every Delivery Order item has a `unit_price` captured when Storekeeper prepared the picking plan
  - When Driver confirms full delivery successfully
  - Then the system SHALL auto-create invoice and receivable for that Delivery Order only, set invoice `due_date` to 30 calendar days after `issue_date`, increase Dealer current balance, link audit to `CONFIRM_DELIVERY`, and move the Delivery Order to `COMPLETED`.

- **Scenario: Invoice already exists for the Delivery Order**
  - Given a Delivery Order already has an invoice
  - When Driver confirmation is retried or invoice creation is triggered again
  - Then the system SHALL return the existing idempotent invoice result and SHALL NOT increase Dealer current balance again.

- **Scenario: Technical failure while creating invoice**
  - Given a Delivery Order is in `IN_TRANSIT`
  - And POD + OTP verification succeeded
  - When invoice/receivable persistence fails before commit because of a technical or database error
  - Then the system SHALL roll back the whole delivery confirmation transaction, keep the Delivery Order in `IN_TRANSIT`, not create invoice, not increase Dealer current balance, and allow the driver confirmation to be retried.

- **Scenario: Other Delivery Orders in the same trip are not invoiced**
  - Given a trip has multiple Delivery Orders in `IN_TRANSIT`
  - When Driver confirms one Delivery Order successfully
  - Then the system SHALL create invoice only for the confirmed Delivery Order and SHALL NOT change invoice state for other Delivery Orders in the trip.

- **Scenario: Notification is out of scope**
  - Given invoice/receivable creation succeeds
  - When the system finishes this feature's transaction
  - Then this feature SHALL NOT send accountant notifications; the separate notification flow SHALL handle notifications.

- **Scenario: Payment processing is out of scope**
  - Given an invoice is `UNPAID`
  - When Accountant wants to extend due date, record payment, or deduct receivable
  - Then this feature SHALL NOT provide those mutation behaviors; the separate finance/payment flow SHALL handle due-date extension, payment receipt, approval, receivable deduction, and DO `CLOSED` transition.
