# Feature: Planner Lap Don xuat hang & Tu dong Kiem tra Cong no (US-WMS-06)

## 1. Context and Goal

Planner tiep nhan yeu cau xuat hang tu Cong ty me cho kho ma Planner duoc gan. Truoc khi tao Delivery Order, he thong bat buoc kiem tra cong no dai ly va ton kho kha dung tai kho xuat. Neu cong no khong hop le, Planner khong co quyen tren kho, hoac ton kho khong du, he thong khong tao phieu va tra loi ro ly do cho Planner. Neu tao thanh cong, he thong reserve tong so luong hang can xuat tai kho va tao Delivery Order o trang thai `NEW` de Thu kho lap danh sach lay hang theo vi tri cu the trong kho.

## 2. Actors

* **Planner**: Lap Delivery Order tu yeu cau xuat hang cho kho duoc gan va nhan thong bao loi neu credit/stock/warehouse scope khong dat dieu kien.
* **Warehouse Manager**: La actor duy nhat duoc huy Delivery Order truoc khi da phe duyet xuat kho.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * The system SHALL always perform an automatic credit check BEFORE creating a Delivery Order.
  * The system SHALL allow order creation when `current_balance + order_value <= credit_limit`, including the case where the resulting balance equals the credit limit.
  * IF the dealer status is `CREDIT_HOLD`, the system SHALL block order creation with a clear reason.
  * The system SHALL block order creation when `current_balance + order_value > credit_limit`.
  * The system SHALL block order creation when the dealer has any overdue invoice older than 30 days.
  * The system SHALL block order creation when the Planner is not assigned to the selected warehouse.
  * The system SHALL reserve requested product quantity at the selected warehouse on Delivery Order items upon successful Delivery Order creation.
  * The system SHALL NOT increase `inventories.reserved_qty` or assign final batch, bin, or zone on Delivery Order creation; the Storekeeper SHALL create the picking list with concrete batch/bin/zone and quantity per location in the picking-plan feature.
  * The system SHALL maintain one `warehouse_product_reservations` aggregate row per warehouse/product to track open Planner-level reservations before Storekeeper assigns concrete batch/bin/zone.
  * The system SHALL calculate warehouse-level availability as `available_qty = sum(inventories.total_qty - inventories.reserved_qty) - warehouse_product_reservations.reserved_qty` for the same warehouse and product.
  * The system SHALL update `warehouse_product_reservations.reserved_qty` with optimistic locking in the same transaction as Delivery Order creation and cancellation.
  * The system SHALL release Delivery Order item reservation when a Warehouse Manager cancels a Delivery Order before warehouse approval.
  * The system SHALL block cancellation when the Delivery Order is already `WAREHOUSE_APPROVED` or later.
  * The system SHALL block cancellation attempts by any actor other than Warehouse Manager.
  * The system SHALL create `DELIVERY_ORDER_CREATE` and `DELIVERY_ORDER_CANCEL` audit log entries for successful creation and cancellation, including credit-check outcome and Delivery Order item reservation delta.
* **Event-driven:**
  * WHEN a Planner creates a Delivery Order, the system SHALL:
    * Validate `available_qty >= requested_qty` using warehouse inventory minus `warehouse_product_reservations.reserved_qty`.
    * IF insufficient stock, block creation and suggest alternative warehouses with enough available stock.
    * For the current household-goods domain, apply FIFO by ranking older received inventory before newer received inventory.
    * NOT require expiry date or FEFO selection for current household goods such as pots, pans, and plastic products.
    * Increase `delivery_order_items.reserved_qty` by requested quantity.
    * Increase `warehouse_product_reservations.reserved_qty` by requested quantity for each warehouse/product pair.
    * Create the Delivery Order in `NEW` status.
* **State-driven:**
  * WHILE dealer status is `CREDIT_HOLD`, the system SHALL block new Delivery Orders for that dealer.
  * WHILE a Delivery Order status is `WAREHOUSE_APPROVED` or later, the system SHALL block cancellation through this feature.

## 4. API Endpoints

* `POST /api/v1/delivery-orders` - Create a new Delivery Order after automatic credit check and stock reservation.
* `PUT /api/v1/delivery-orders/{id}/cancel` - Warehouse Manager cancels a Delivery Order before warehouse approval and releases Delivery Order item reservation.

## 5. Acceptance Criteria

**Scenario 1: Block order due to credit limit breach**
* Given a dealer with `current_balance = 480M` and `credit_limit = 500M`
* When Planner attempts to create a Delivery Order worth `30M`
* Then the system SHALL block creation and display a credit check error.

**Scenario 1b: Block order due to overdue debt**
* Given a dealer has an invoice overdue by more than 30 days
* When Planner attempts to create a Delivery Order
* Then the system SHALL block creation and show the overdue reason.

**Scenario 1c: Allow order exactly at credit limit**
* Given a dealer with `current_balance = 480M` and `credit_limit = 500M`
* When Planner attempts to create a Delivery Order worth `20M`
* Then the system SHALL allow creation because `current_balance + order_value = credit_limit`.

**Scenario 2: Suggest alternative warehouse on stock shortage**
* Given product X has `total_qty = 100` and `reserved_qty = 30` in warehouse HP
* When Planner attempts to create a Delivery Order for `80` units in warehouse HP
* Then the system SHALL block creation and suggest another warehouse that has enough available stock.

**Scenario 2b: Block Planner outside assigned warehouse**
* Given a Planner assigned only to warehouse HP
* When Planner attempts to create a Delivery Order for warehouse HN
* Then the system SHALL block creation with `WAREHOUSE_SCOPE_FORBIDDEN`.

**Scenario 3: Successful Delivery Order starts at NEW**
* Given dealer credit is valid and requested stock is available
* When Planner creates a Delivery Order successfully
* Then the system SHALL reserve requested product quantity on Delivery Order items and `warehouse_product_reservations`, create `DELIVERY_ORDER_CREATE` audit log, and create the Delivery Order in `NEW` status without changing `inventories.reserved_qty` or assigning final batch/bin/zone.

**Scenario 4: Warehouse Manager cancels before approval**
* Given a Delivery Order is not yet `WAREHOUSE_APPROVED`
* When Warehouse Manager cancels the Delivery Order with a reason
* Then the system SHALL release Delivery Order item reservation, create `DELIVERY_ORDER_CANCEL` audit log, and move the Delivery Order to `CANCELLED`.

**Scenario 5: Block cancellation after warehouse approval**
* Given a Delivery Order is already `WAREHOUSE_APPROVED`
* When Warehouse Manager attempts to cancel the Delivery Order
* Then the system SHALL block cancellation because warehouse-approved Delivery Orders cannot be cancelled by this feature.

**Scenario 6: Block non-manager cancellation**
* Given a Delivery Order is not yet `WAREHOUSE_APPROVED`
* When Planner or Storekeeper attempts to cancel the Delivery Order
* Then the system SHALL block cancellation because only Warehouse Manager can cancel Delivery Orders.
