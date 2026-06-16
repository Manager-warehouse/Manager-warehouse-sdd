# Feature: Thu kho Lap ke hoach lay hang (US-WMS-07A)

## 1. Context and Goal

Thu kho nhan Delivery Order moi va lap ke hoach lay hang truoc khi nhan vien kho thao tac thuc te. He thong hien thi danh sach ton kho theo batch/bin/zone, uu tien hang nhap kho som hon len dau danh sach theo FIFO; voi san pham co expiry/FEFO thi uu tien han dung gan nhat. Thu kho chon batch, bin, zone va so luong can lay cho tung dong hang, sau do phieu chuyen sang trang thai cho nhan vien kho lay hang.

## 2. Actors

* **Thu kho**: Chon batch/bin/zone tu danh sach FIFO/FEFO, luu ke hoach lay hang, dieu chinh ke hoach khi can thay the hang fail QC.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * The system SHALL show storekeepers a ranked inventory list by product, batch, bin, and zone for each Delivery Order item.
  * The ranked list SHALL place oldest received inventory first for FIFO products and nearest-expiry inventory first for FEFO-enabled products.
  * The system SHALL create `PICKING_PLAN_START` audit log entries whenever a storekeeper starts planning from a `NEW` Delivery Order.
  * The system SHALL create `PICKING_PLAN_SAVE` audit log entries whenever a storekeeper saves or changes the picking plan.
  * The system SHALL create `PICKING_REPLACEMENT_SAVE` audit log entries whenever failed QC goods require replacement goods from another batch/bin/zone.
* **Event-driven:**
  * WHEN a Delivery Order is created successfully, the system SHALL keep it in `NEW` status until a storekeeper saves a picking plan.
  * WHEN a storekeeper starts planning picking for a Delivery Order in `NEW`, the system SHALL move the Delivery Order to `PICKING_PLANNED`.
  * WHEN a storekeeper saves the initial picking plan, the system SHALL:
    * Store planned batch, bin, zone, and quantity for each item.
    * Keep the selected quantities reserved.
    * Move the Delivery Order to `WAITING_PICKING`.
  * WHEN QC fail quantity requires replacement, the system SHALL allow the storekeeper to select replacement goods from the same FIFO/FEFO-ranked inventory list.
  * WHEN the storekeeper saves replacement goods, the system SHALL:
    * Update the Delivery Order item plan with replacement batch/bin/zone details.
    * Store replacement history including failed source, replacement source, quantity, reason, and actor.
    * Require warehouse staff to pick and QC the replacement goods before quality approval can complete.

## 4. API Endpoints

* `PUT /api/v1/delivery-orders/{id}/picking-plan/start` - Thu kho bat dau lap ke hoach lay hang.
* `PUT /api/v1/delivery-orders/{id}/picking-plan` - Thu kho luu ke hoach lay hang ban dau.
* `PUT /api/v1/delivery-orders/{id}/replacement-plan` - Thu kho luu ke hoach lay hang thay the cho so luong fail QC.

## 5. Acceptance Criteria

* **Scenario: Start picking planning**
  * Given a delivery order in `NEW` status
  * When the storekeeper starts planning picking
  * Then the system SHALL move the order to `PICKING_PLANNED`.

* **Scenario: Save picking plan**
  * Given a delivery order in `PICKING_PLANNED` status
  * When the storekeeper selects batch, bin, zone, and planned quantities from the FIFO-ranked inventory list
  * Then the system SHALL save the picking plan, keep the quantities reserved, create a `PICKING_PLAN_SAVE` audit log, and move the order to `WAITING_PICKING`.

* **Scenario: Save replacement after QC fail**
  * Given a delivery order has QC fail quantity recorded and failed goods moved to quarantine
  * When the storekeeper selects replacement goods from another available bin
  * Then the system SHALL update the picking plan, store replacement history, create a `PICKING_REPLACEMENT_SAVE` audit log, and require the replacement goods to be picked and QC checked.
