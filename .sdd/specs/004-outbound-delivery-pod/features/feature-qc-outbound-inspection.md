# Feature: Thủ kho Kiểm tra Đóng gói Outbound (US-WMS-07)

## 1. Context and Goal
Thủ kho kiêm QC kiểm QC đóng gói (kiểm tra SKU, số lượng, quy cách đóng gói chống sốc) sau khi soạn xong, đảm bảo hàng hóa rời kho chính xác và an toàn.

## 2. Actors
* **Thủ kho kiêm QC**: Kiểm QC Outbound, xác nhận chất lượng đóng gói và chuyển trạng thái sẵn sàng xuất.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Thủ kho confirms QC Outbound passed, the system SHALL allow setting status to `READY_TO_SHIP`.
  * WHEN QC passes, the system SHALL require Trưởng kho approval before the delivery order can move to `READY_TO_SHIP`.
  * WHEN Trưởng kho rejects the outbound approval, the system SHALL keep the delivery order in `PICKING` and record the rejection reason.

## 4. API Endpoints
* `PUT /api/v1/delivery-orders/{id}/qc-outbound` - Xác nhận đạt QC Outbound (STORE_KEEPER).
* `PUT /api/v1/delivery-orders/{id}/ship` - Xác nhận sẵn sàng xuất (STORE_KEEPER).
* `PUT /api/v1/delivery-orders/{id}/warehouse-approval` - Trưởng kho phê duyệt xuất kho sau QC.
* `PUT /api/v1/delivery-orders/{id}/warehouse-reject` - Trưởng kho từ chối xuất kho sau QC.

## 5. Acceptance Criteria
* **Scenario: Successful QC confirmation**
  * Given a delivery order in `PICKING` status with all items picked
  * When Thủ kho verifies correct SKU and packaging, and confirms QC passed
  * Then the system SHALL require warehouse approval before the DO can be marked as `READY_TO_SHIP`.

* **Scenario: Warehouse approval granted**
  * Given a delivery order has passed QC
  * When Trưởng kho approves the outbound request
  * Then the system SHALL change the DO status to `READY_TO_SHIP`.

* **Scenario: Warehouse approval rejected**
  * Given a delivery order has passed QC
  * When Trưởng kho rejects the outbound request
  * Then the system SHALL keep the DO status in `PICKING` and store the rejection reason.
