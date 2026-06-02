# Feature: Nhân viên QC Kiểm tra Đóng gói Outbound (US-WMS-07)

## 1. Context and Goal
Nhân viên kho (phụ trách QC) kiểm QC đóng gói (kiểm tra SKU, số lượng, quy cách đóng gói chống sốc) sau khi Thủ kho soạn xong, đảm bảo hàng hóa rời kho chính xác và an toàn.

## 2. Actors
* **Nhân viên kho (QC)**: Kiểm QC Outbound và xác nhận chất lượng đóng gói.
* **Thủ kho**: Nhận kết quả QC để chuyển trạng thái sẵn sàng xuất.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Nhân viên kho confirms QC Outbound passed, the system SHALL allow the Thủ kho to set status to `READY_TO_SHIP`.

## 4. API Endpoints
* `PUT /api/v1/delivery-orders/{id}/qc-outbound` - Xác nhận đạt QC Outbound (WAREHOUSE_STAFF).
* `PUT /api/v1/delivery-orders/{id}/ship` - Xác nhận sẵn sàng xuất (STORE_KEEPER).

## 5. Acceptance Criteria
* **Scenario: Successful QC confirmation**
  * Given a delivery order in `PICKING` status with all items picked
  * When Nhân viên kho verifies correct SKU and packaging, and confirms QC passed
  * Then Thủ kho SHALL be allowed to mark the DO as `READY_TO_SHIP`.
