# Feature: Nhan vien kho Lay hang & QC Outbound (US-WMS-07B)

## 1. Context and Goal

Nhan vien kho lay hang thuc te theo ke hoach cua Thu kho, kiem tra chat luong tung san pham va nhap so luong da lay, so luong dat QC, so luong khong dat QC. Hang dat QC duoc giu tai khu outbound staging trong cung kho de cho Thu kho phe duyet chat luong va Truong kho phe duyet xuat kho. Hang khong dat QC phai chuyen sang quarantine, tao quarantine record, tao inventory adjustment record cho phan fail, tru khoi ton kho hop le va khong duoc tinh vao available inventory.

## 2. Actors

* **Nhan vien kho**: Lay hang tu bin/zone theo ke hoach, kiem tra chat luong san pham, nhap so luong dat/khong dat QC.
* **Thu kho**: Phe duyet ket qua chat luong outbound va chon hang thay the khi co QC fail.
* **Truong kho**: Phe duyet hoac tu choi xuat kho sau khi QC completed.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * The system SHALL create `DELIVERY_ORDER_PICK_START`, `DELIVERY_ORDER_PICK_COMPLETE`, `OUTBOUND_QC_FAIL_QUARANTINE`, `DELIVERY_ORDER_QC_APPROVE`, `DELIVERY_ORDER_WAREHOUSE_APPROVE`, and `DELIVERY_ORDER_WAREHOUSE_REJECT` audit log entries for outbound picking, QC, quarantine, and warehouse approval decisions.
  * Failed QC quantity SHALL be moved to quarantine, recorded by an inventory adjustment, deducted from valid regular inventory, and excluded from available inventory.
  * QC-passed quantity SHALL remain in outbound staging until warehouse manager approval or rejection.
* **Event-driven:**
  * WHEN warehouse staff starts physical picking for a Delivery Order in `WAITING_PICKING`, the system SHALL move the Delivery Order to `PICKING`.
  * WHEN warehouse staff saves picked and QC quantities, the system SHALL:
    * Store picked quantity, QC pass quantity, and QC fail quantity for each item.
    * Move picked goods from the planned bin to outbound staging.
    * Move QC fail quantity from outbound staging to quarantine, create a quarantine record, and create an inventory adjustment record.
    * Deduct QC fail quantity from valid regular inventory.
    * Move the Delivery Order to `QC_PENDING_APPROVAL`.
  * WHEN QC pass quantity is lower than requested quantity, the system SHALL block storekeeper quality approval until replacement goods have been planned, picked, and QC-passed.
  * WHEN all requested quantities have QC-passed goods in outbound staging, the storekeeper SHALL be able to approve quality and move the Delivery Order to `QC_COMPLETED`.
  * WHEN warehouse manager approves a `QC_COMPLETED` Delivery Order, the system SHALL move the Delivery Order to `WAREHOUSE_APPROVED`.
  * WHEN warehouse manager rejects a `QC_COMPLETED` Delivery Order, the system SHALL:
    * Store the rejection reason.
    * Move QC-passed goods from outbound staging back to their original bin/location.
    * Release the order's reserved quantity.
    * Keep QC-failed goods in quarantine.
    * Move the Delivery Order to `REJECTED` and end the outbound flow.

## 4. API Endpoints

* `PUT /api/v1/delivery-orders/{id}/pick-start` - Nhan vien kho bat dau lay hang thuc te.
* `PUT /api/v1/delivery-orders/{id}/pick-qc-result` - Nhan vien kho luu so luong da lay, dat QC va khong dat QC.
* `PUT /api/v1/delivery-orders/{id}/quality-approval` - Thu kho phe duyet chat luong outbound sau khi du so luong dat QC.
* `PUT /api/v1/delivery-orders/{id}/warehouse-approval` - Truong kho phe duyet xuat kho sau QC completed.
* `PUT /api/v1/delivery-orders/{id}/warehouse-reject` - Truong kho tu choi xuat kho sau QC completed.

## 5. Acceptance Criteria

* **Scenario: Warehouse staff records QC result**
  * Given a delivery order is in `WAITING_PICKING` with a saved picking plan
  * When warehouse staff picks goods and records picked, QC pass, and QC fail quantities
  * Then the system SHALL move picked goods to outbound staging, move failed goods to quarantine, create the inventory adjustment record, create QC/quarantine audit logs, deduct failed goods from valid regular inventory, and move the order to `QC_PENDING_APPROVAL`.

* **Scenario: Replacement required before quality approval**
  * Given requested quantity is 10 and warehouse staff records 8 QC pass and 2 QC fail
  * When the storekeeper tries to approve quality before replacement goods pass QC
  * Then the system SHALL reject the action with `QC_REPLACEMENT_REQUIRED`.

* **Scenario: Storekeeper approves quality**
  * Given all requested quantities have QC-passed goods in outbound staging
  * When the storekeeper approves quality
  * Then the system SHALL move the delivery order to `QC_COMPLETED`.

* **Scenario: Warehouse manager approves outbound**
  * Given a delivery order is in `QC_COMPLETED`
  * When the warehouse manager approves outbound release
  * Then the system SHALL move the delivery order to `WAREHOUSE_APPROVED`.

* **Scenario: Warehouse manager rejects outbound**
  * Given a delivery order is in `QC_COMPLETED`
  * When the warehouse manager rejects outbound release with a reason
  * Then the system SHALL move QC-passed goods back to original bins, keep failed goods in quarantine, release reservations, store the reason, and move the order to `REJECTED`.
