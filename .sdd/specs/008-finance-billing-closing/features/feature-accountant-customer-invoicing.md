# Feature: Ke toan Theo doi Hoa don Tu dong & Ghi nhan Cong no (US-WMS-10)

## 1. Context and Goal
Sau khi Delivery Order giao thanh cong bang POD + OTP, delivery attempt duoc danh dau `DELIVERED`, he thong tu dong tao invoice/cong no va chuyen Delivery Order sang `COMPLETED`. Ke toan vien khong lap invoice thu cong tu Delivery Order; thay vao do, Ke toan vien theo doi invoice/receivable da duoc tao tu dong theo pham vi kho duoc phan quyen, doi soat so tien, va tiep tuc quy trinh thu tien.

## 2. Actors
* **Ke toan vien (Maker)**: Xem invoice/counter receivable da tu dong tao tu DO `COMPLETED`, doi soat thong tin va theo doi thu tien.
* **Planner**: Bi chan tao don neu Dai ly bi khoa tin dung.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always maintain a running `current_balance` for each dealer that is updated when an invoice is automatically created from a completed Delivery Order.
  * The system SHALL always perform a credit check before allowing new delivery order creation.
  * The system SHALL expose automatically created invoices and receivables through an accounting worklist scoped by accountant role and warehouse assignment.
* **Event-driven:**
  * WHEN POD + OTP confirmation succeeds for a Delivery Order, the outbound flow SHALL automatically create the invoice and receivable, increase the dealer's `current_balance`, and move the Delivery Order to `COMPLETED`.
  * WHEN an automatic invoice is created, the system SHALL:
    * Calculate total from DO items x prices valid at shipment date.
    * Set payment terms (`payment_term_days` from dealer profile).
    * Increase dealer's `current_balance` by invoice `total_amount`.
    * IF `current_balance > credit_limit`: auto-set dealer status to `CREDIT_HOLD`; if `current_balance = credit_limit`, keep the dealer eligible for new transaction creation subject to other rules.
* **State-driven:**
  * WHILE dealer status is `CREDIT_HOLD`, the system SHALL block creation of new delivery orders for that dealer.

## 4. API Endpoints
* `GET /api/v1/accounting/invoices?warehouseId={warehouseId}` - Danh sach invoices/receivables da tu dong tao tu Delivery Orders `COMPLETED`, theo pham vi kho cua Ke toan vien.
* `GET /api/v1/invoices/{id}` - Xem chi tiet hoa don.

## 5. Acceptance Criteria
* **Scenario: Lock credit limit upon automatic invoicing**
  * Given a dealer with `credit_limit = 500M` and `current_balance = 0`
  * When an automatic invoice of `600M` is created from a completed Delivery Order
  * Then the dealer's status SHALL be changed to `CREDIT_HOLD` and their `current_balance` set to `600M`.

* **Scenario: Prevent missing receivables**
  * Given Delivery Orders are `COMPLETED` and have automatically created invoices
  * When a Ke toan vien opens the invoice/receivable worklist for an assigned warehouse
  * Then the system SHALL show all matching invoices and exclude invoices outside the accountant's warehouse assignment.

* **Scenario: View automatically created invoice**
  * Given a Delivery Order is `COMPLETED`
  * And an invoice was automatically created from that Delivery Order
  * When a Ke toan vien opens the invoice detail
  * Then the system SHALL show the invoice, related Delivery Order, dealer, total amount, payment terms, and receivable status.
