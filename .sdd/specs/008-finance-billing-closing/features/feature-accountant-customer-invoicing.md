# Feature: Kế toán Lập Hóa đơn Bán hàng & Ghi nhận Công nợ (US-WMS-10)

## 1. Context and Goal
Kế toán lập hóa đơn bán hàng cho Đại lý dựa trên đơn giao thành công (Delivered) và bảng giá có hiệu lực, cộng dồn công nợ Đại lý và tự động khóa tín dụng (`CREDIT_HOLD`) nếu vượt hạn mức.

Spec 004 creates the billing notification when OTP confirmation moves the Delivery Order to `DELIVERED`. This feature consumes that notification to create the invoice, resolve the accounting period, update dealer debt, and close the operational Delivery Order lifecycle by moving it to `COMPLETED`.

## 2. Actors
* **Kế toán viên (Maker)**: Lập hóa đơn bán hàng từ đơn Delivered.
* **Planner**: Bị chặn tạo đơn nếu Đại lý bị khóa tín dụng.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always maintain a running `current_balance` for each dealer that is updated with every invoice transaction.
  * The system SHALL always perform a credit check before allowing new delivery order creation.
* **Event-driven:**
  * WHEN a delivery order status changes to `DELIVERED`, the system SHALL notify Kế toán viên to create an invoice.
  * WHEN a Kế toán viên creates an invoice, the system SHALL:
    * Calculate total from DO items × prices valid at shipment date.
    * Set payment terms (`payment_term_days` from dealer profile).
    * Resolve `accounting_period_id` from the invoice document date according to open accounting period rules.
    * Increase dealer's `current_balance` by invoice `total_amount`.
    * IF `current_balance > credit_limit`: auto-set dealer status to `CREDIT_HOLD`; if `current_balance = credit_limit`, keep the dealer eligible for new transaction creation subject to other rules.
    * Update the source Delivery Order from `DELIVERED` to `COMPLETED`.
    * Mark the related billing notification with `invoice_status = INVOICED` and `status = ARCHIVED`.
* **State-driven:**
  * WHILE dealer status is `CREDIT_HOLD`, the system SHALL block creation of new delivery orders for that dealer.
  * WHILE a Delivery Order is not `DELIVERED`, the system SHALL reject invoice creation from that Delivery Order.
  * WHILE a Delivery Order is already `COMPLETED` or already linked to an invoice, the system SHALL reject duplicate invoice creation with HTTP 409.

## 4. API Endpoints
* `POST /api/v1/invoices` - Lập hóa đơn bán hàng từ đơn Delivered.
* `GET /api/v1/invoices/{id}` - Xem chi tiết hóa đơn.

## 5. Acceptance Criteria
* **Scenario: Lock credit limit upon invoicing**
  * Given a dealer with `credit_limit = 500M` and `current_balance = 0`
  * When an invoice of `600M` is created and posted
  * Then the dealer's status SHALL be changed to `CREDIT_HOLD` and their `current_balance` set to `600M`.

* **Scenario: Complete delivered Delivery Order after invoice creation**
  * Given a Delivery Order is `DELIVERED` and has an active billing notification with `invoice_status = NOT_INVOICED`
  * When Kế toán viên creates an invoice for that Delivery Order
  * Then the system SHALL create the invoice, set the billing notification to `invoice_status = INVOICED` and `status = ARCHIVED`, and update the Delivery Order status to `COMPLETED`.
