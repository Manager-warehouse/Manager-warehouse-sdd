# Feature: Planner Lap Don xuat hang & Tu dong Kiem tra Cong no (US-WMS-06)

## 1. Context and Goal

Planner tiep nhan yeu cau xuat hang tu Cong ty me. Truoc khi tao Delivery Order, he thong bat buoc kiem tra cong no dai ly va ton kho kha dung. Neu cong no khong hop le hoac ton kho khong du, he thong khong tao phieu va tra loi ro ly do cho Planner. Neu tao thanh cong, he thong reserve ton kho va tao Delivery Order o trang thai `NEW` de Thu kho lap ke hoach lay hang.

## 2. Actors

* **Planner**: Lap Delivery Order tu yeu cau xuat hang va nhan thong bao loi neu credit/stock khong dat dieu kien.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * The system SHALL always perform an automatic credit check BEFORE creating a Delivery Order.
  * IF the dealer status is `CREDIT_HOLD`, the system SHALL block order creation with a clear reason.
  * The system SHALL block order creation when `current_balance + order_value > credit_limit`.
  * The system SHALL block order creation when the dealer has any overdue invoice older than 30 days.
  * The system SHALL reserve inventory upon successful Delivery Order creation.
  * The system SHALL release reserved inventory when a Delivery Order is cancelled before outbound departure.
  * The system SHALL create `DELIVERY_ORDER_CREATE` and `DELIVERY_ORDER_CANCEL` audit log entries for successful creation and cancellation, including credit-check outcome and reserved inventory delta.
* **Event-driven:**
  * WHEN a Planner creates a Delivery Order, the system SHALL:
    * Validate `available_qty = total_qty - reserved_qty >= requested_qty`.
    * IF insufficient stock, block creation and suggest alternative warehouses with enough available stock.
    * Apply FIFO by default when selecting reservable inventory.
    * Apply FEFO only for products with expiry date or explicit FEFO configuration.
    * Increase `inventories.reserved_qty` by requested quantity.
    * Create the Delivery Order in `NEW` status.
* **State-driven:**
  * WHILE dealer status is `CREDIT_HOLD`, the system SHALL block new Delivery Orders for that dealer.

## 4. API Endpoints

* `POST /api/v1/delivery-orders` - Create a new Delivery Order after automatic credit check and stock reservation.
* `PUT /api/v1/delivery-orders/{id}/cancel` - Cancel a Delivery Order before outbound departure and release reserved inventory.

## 5. Acceptance Criteria

**Scenario 1: Block order due to credit limit breach**
* Given a dealer with `current_balance = 480M` and `credit_limit = 500M`
* When Planner attempts to create a Delivery Order worth `30M`
* Then the system SHALL block creation and display a credit check error.

**Scenario 1b: Block order due to overdue debt**
* Given a dealer has an invoice overdue by more than 30 days
* When Planner attempts to create a Delivery Order
* Then the system SHALL block creation and show the overdue reason.

**Scenario 2: Suggest alternative warehouse on stock shortage**
* Given product X has `total_qty = 100` and `reserved_qty = 30` in warehouse HP
* When Planner attempts to create a Delivery Order for `80` units in warehouse HP
* Then the system SHALL block creation and suggest another warehouse that has enough available stock.

**Scenario 3: Successful Delivery Order starts at NEW**
* Given dealer credit is valid and requested stock is available
* When Planner creates a Delivery Order successfully
* Then the system SHALL reserve requested inventory, create `DELIVERY_ORDER_CREATE` audit log, and create the Delivery Order in `NEW` status.
