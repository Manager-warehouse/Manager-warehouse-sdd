# Feature: Thủ kho Nguồn Soạn & Xuất hàng Điều chuyển (US-WMS-12)

## 1. Context and Goal
Thủ kho tại kho nguồn chịu trách nhiệm soạn hàng theo phiếu điều chuyển đã được phê duyệt, bốc xếp lên xe tải nội bộ và ghi nhận số lượng gửi đi. Dispatcher phải lập một chuyến xe nội bộ riêng cho phiếu điều chuyển, và Tài xế xác nhận đã nhận hàng, xe rời kho để hệ thống chuyển tồn kho sang trạng thái In-Transit ảo.

## 2. Actors
* **Thủ kho (Kho nguồn)**: Soạn hàng, xác nhận xuất hàng lên xe tải.
* **Trưởng kho nguồn**: Duyệt phiếu điều chuyển đi.
* **Dispatcher**: Lập chuyến xe nội bộ riêng cho phiếu điều chuyển, gán xe và tài xế khả dụng.
* **Tài xế**: Xác nhận nhận hàng và xe rời kho nguồn.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always route all inter-warehouse transfers through a virtual In-Transit warehouse for tracking.
  * The system SHALL always enforce `source_warehouse_id ≠ destination_warehouse_id`.
  * The system SHALL always require exactly one dedicated internal trip (`trip_type = 'TRANSFER'`) before a transfer can depart.
* **Event-driven:**
  * WHEN a Trưởng kho nguồn approves a transfer (status → `APPROVED`), the system SHALL:
    * Verify: `available_qty = total_qty - reserved_qty ≥ planned_qty` at the source warehouse.
    * If sufficient, increase source inventories: `reserved_qty += planned_qty`.
  * WHEN a Dispatcher assigns transport for an approved transfer, the system SHALL create or link exactly one `TRANSFER` trip with vehicle, driver, and planned date.
  * WHEN a Thủ kho nguồn confirms shipment preparation, the system SHALL record `sent_qty` and keep the transfer in `APPROVED` until driver departure confirmation.
  * WHEN a Tài xế confirms goods received and vehicle departure (status → `IN_TRANSIT`), the system SHALL:
    * Decrease source warehouse inventories: `total_qty -= sent_qty`.
    * Decrease source warehouse reserved quantity: `reserved_qty -= planned_qty`.
    * Increase In-Transit virtual warehouse inventories: `total_qty += sent_qty`.
  * WHEN a transfer is cancelled, the system SHALL:
    * IF the status was `APPROVED`, decrease source warehouse reserved quantity: `reserved_qty -= planned_qty`.
    * IF the status is `IN_TRANSIT`, reject the cancellation.

## 4. API Endpoints
* `PUT /api/v1/transfers/{id}/approve` - Duyệt điều chuyển và giữ chỗ hàng (Trưởng kho nguồn).
* `POST /api/v1/transfers/{id}/trip` - Lập chuyến xe nội bộ riêng cho phiếu điều chuyển (Dispatcher).
* `PUT /api/v1/transfers/{id}/ship` - Thủ kho nguồn ghi nhận số lượng gửi đi và bốc xếp lên xe.
* `PUT /api/v1/transfers/{id}/depart` - Tài xế xác nhận đã nhận hàng, xe rời kho, giải phóng giữ chỗ và chuyển sang In-Transit.
* `PUT /api/v1/transfers/{id}/cancel` - Hủy phiếu điều chuyển, giải phóng giữ chỗ (Planner / Trưởng kho).

## 5. Acceptance Criteria
* **Scenario: Transfer approval reserves stock**
  * Given source warehouse HP has 50 units of product X with `reserved_qty = 0` (available = 50)
  * When Planner creates a transfer of 30 units and Trưởng kho HP approves it
  * Then source inventory HP SHALL show `total_qty = 50`, `reserved_qty = 30`, and `available_qty = 20`.

* **Scenario: Transfer departure releases reservation and moves to In-Transit**
  * Given source warehouse HP has 50 units of product X with `reserved_qty = 30` (from an approved transfer of 30 units)
  * And Dispatcher has assigned a dedicated transfer trip with vehicle and driver
  * And Thủ kho HP records shipment of 30 units
  * When Tài xế confirms goods received and vehicle departure
  * Then:
    * Source inventory HP SHALL show `total_qty = 20` and `reserved_qty = 0`.
    * Virtual In-Transit inventory SHALL show `total_qty = 30`.

* **Scenario: Reject cancellation after departure**
  * Given a transfer is already in `IN_TRANSIT` status
  * When Planner or Trưởng kho attempts to cancel it
  * Then the system SHALL reject the cancellation.
