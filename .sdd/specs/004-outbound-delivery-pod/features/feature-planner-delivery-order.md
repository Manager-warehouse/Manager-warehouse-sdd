# Feature: Planner Lập Đơn xuất hàng & Tự động Kiểm tra Công nợ (US-WMS-06)

## 1. Context and Goal
Planner tiếp nhận yêu cầu xuất hàng từ Công ty mẹ, hệ thống bắt buộc tự động kiểm tra công nợ Đại lý (Credit Check) và tồn kho khả dụng để cho phép tạo Đơn xuất hàng (Delivery Order) và tự động giữ chỗ (Reserve) tồn kho.

## 2. Actors
* **Planner (Người nhận đơn)**: Nhận yêu cầu và lập Đơn xuất hàng.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always perform an automatic credit check BEFORE creating a delivery order. IF the dealer's status is CREDIT_HOLD, the system SHALL block order creation with a clear reason.
  * The system SHALL also block order creation when `current_balance + order_value > credit_limit` or when the dealer has any overdue invoice older than 30 days.
  * The system SHALL always reserve inventory (`inventories.reserved_qty += quantity`) upon successful delivery order creation.
  * The system SHALL always release reserved inventory when a delivery order is CANCELLED.
  * The system SHALL create `DELIVERY_ORDER_CREATE` and `DELIVERY_ORDER_CANCEL` audit log entries for successful DO creation and cancellation, including credit-check outcome and reserved inventory delta.
* **Event-driven:**
  * WHEN a Planner creates a delivery order, the system SHALL:
    * Validate: `available_qty = total_qty - reserved_qty ≥ requested_qty`.
    * IF insufficient stock, the system SHALL warn and suggest alternative warehouses.
    * Apply FIFO to select batch and warehouse location for each item.
    * Update inventories: `reserved_qty += requested_qty`.
* **State-driven:**
  * WHILE dealer status is `CREDIT_HOLD`, the system SHALL block creation of new delivery orders for that dealer.

## 4. API Endpoints
* `POST /api/v1/delivery-orders` - Tạo Đơn xuất hàng mới (hệ thống tự chạy credit check và reserve stock).
* `PUT /api/v1/delivery-orders/{id}/cancel` - Hủy Đơn xuất hàng, tự động giải phóng lượng hàng đã reserve.

## 5. Acceptance Criteria

**Scenario 1: Block order due to Credit Limit breach**
* Given a dealer with `current_balance = 480M` and `credit_limit = 500M`
* When Planner attempts to create a DO worth `30M`
* Then the system SHALL block DO creation and display a Credit Check error.

**Scenario 1b: Block order due to overdue debt**
* Given a dealer has an invoice overdue by more than 30 days
* When Planner attempts to create a DO
* Then the system SHALL block DO creation and show the overdue reason.

**Scenario 2: Suggest alternative warehouse on stock shortage**
* Given product X has `total_qty = 100` and `reserved_qty = 30` in warehouse HP (available = 70)
* When Planner attempts to create a DO for `80` units in warehouse HP
* Then the system SHALL warn about insufficient stock and suggest warehouse HN which has enough available stock.
