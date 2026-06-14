# Feature: Kế toán Lập Hóa đơn Bán hàng & Ghi nhận Công nợ (US-WMS-10)

## 1. Context and Goal
Kế toán lập hóa đơn bán hàng cho Đại lý dựa trên Delivery Orders đã giao thành công (`DELIVERED`) và chưa có invoice. Để không bỏ sót đơn, Kế toán thao tác từ invoice candidates worklist theo phạm vi kho được phân quyền. Khi hóa đơn được tạo thành công, hệ thống cộng dồn công nợ Đại lý và chuyển Delivery Order sang `COMPLETED`.

## 2. Actors
* **Kế toán viên (Maker)**: Xem invoice candidates, lập hóa đơn bán hàng từ đơn `DELIVERED`.
* **Planner**: Bị chặn tạo đơn nếu Đại lý bị khóa tín dụng.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always maintain a running `current_balance` for each dealer that is updated with every invoice transaction.
  * The system SHALL always perform a credit check before allowing new delivery order creation.
  * The system SHALL always expose delivered Delivery Orders without invoices through an invoice candidates worklist scoped by accountant role and warehouse assignment.
* **Event-driven:**
  * WHEN a delivery order status changes to `DELIVERED`, the system SHALL notify Kế toán viên to create an invoice.
  * WHEN a Kế toán viên creates an invoice, the system SHALL:
    * Calculate total from DO items x prices valid at shipment date.
    * Set payment terms (`payment_term_days` from dealer profile).
    * Increase dealer's `current_balance` by invoice `total_amount`.
    * Update the Delivery Order status to `COMPLETED`.
    * IF `current_balance > credit_limit`: auto-set dealer status to `CREDIT_HOLD`; if `current_balance = credit_limit`, keep the dealer eligible for new transaction creation subject to other rules.
* **State-driven:**
  * WHILE dealer status is `CREDIT_HOLD`, the system SHALL block creation of new delivery orders for that dealer.

## 4. API Endpoints
* `GET /api/v1/accounting/invoice-candidates?warehouseId={warehouseId}` - Danh sách Delivery Orders đã `DELIVERED` nhưng chưa có invoice, theo phạm vi kho của Kế toán viên.
* `POST /api/v1/accounting/invoice-candidates/{doId}/invoice` - Lập hóa đơn bán hàng từ một Đơn Delivered trong invoice candidates worklist.
* `GET /api/v1/invoices/{id}` - Xem chi tiết hóa đơn.

## 5. Acceptance Criteria
* **Scenario: Lock credit limit upon invoicing**
  * Given a dealer with `credit_limit = 500M` and `current_balance = 0`
  * When an invoice of `600M` is created and posted
  * Then the dealer's status SHALL be changed to `CREDIT_HOLD` and their `current_balance` set to `600M`.

* **Scenario: Prevent missing delivered orders**
  * Given Delivery Orders are `DELIVERED` and do not yet have invoices
  * When a Kế toán viên opens the invoice candidates worklist for an assigned warehouse
  * Then the system SHALL show all matching orders and exclude orders outside the accountant's warehouse assignment.

* **Scenario: Complete DO after invoice creation**
  * Given a Delivery Order is `DELIVERED` and has no invoice
  * When a Kế toán viên creates an invoice from the invoice candidates worklist
  * Then the system SHALL create the invoice and move the Delivery Order to `COMPLETED`.
