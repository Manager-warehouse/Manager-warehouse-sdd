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
  * The system SHALL enforce role plus warehouse scope for source warehouse operations.
* **Event-driven:**
  * WHEN a Trưởng kho nguồn approves a transfer (status → `APPROVED`), the system SHALL:
    * Allow approval only when the actor is assigned to the transfer source warehouse or has an authorized manager override role.
    * Verify: `available_qty = total_qty - reserved_qty ≥ planned_qty` at the source warehouse.
    * Allocate source batches for each transfer item using FIFO by batch received date because Planner does not select `batch_id` during transfer creation.
    * If sufficient, increase source inventories: `reserved_qty += planned_qty`.
    * Create a `TRANSFER_APPROVE` audit log entry.
  * WHEN a Trưởng kho nguồn rejects a transfer, the system SHALL:
    * Allow rejection only when the actor is assigned to the transfer source warehouse or has an authorized manager override role.
    * Allow rejection only while the transfer is `NEW`.
    * Require a non-empty rejection reason.
    * Set transfer status to `REJECTED`.
    * Not reserve or change inventory quantities.
    * Create a `TRANSFER_REJECT` audit log entry.
  * WHEN a Dispatcher assigns transport for an approved transfer, the system SHALL create or link exactly one `TRANSFER` trip with vehicle, driver, and planned date.
  * WHEN a Dispatcher assigns transport for a transfer, the system SHALL require Dispatcher role; Planner SHALL NOT assign or create trips.
  * WHEN a Dispatcher assigns transport, the system SHALL verify the vehicle and driver are available and not already assigned to an overlapping trip.
  * WHEN a Dispatcher assigns transport successfully, the system SHALL create a `TRANSFER_TRIP_ASSIGN` audit log entry.
  * WHEN a Thủ kho nguồn confirms shipment preparation, the system SHALL:
    * Allow the action only while the transfer is `APPROVED`.
    * Allow the action only when the actor is assigned to the transfer source warehouse.
    * Require `sent_qty = planned_qty` for every item; sending less or more than the approved quantity SHALL be rejected.
    * Create a `TRANSFER_SHIP` audit log entry.
  * WHEN a user needs to cancel after shipment preparation but before driver departure, the system SHALL require an unship/unload action first:
    * Allow unship/unload only while the transfer is still `APPROVED` and `sent_qty` has been recorded.
    * Allow unship/unload only for Thủ kho nguồn assigned to the transfer source warehouse or an authorized manager.
    * Clear the recorded `sent_qty`/loaded marker and keep the transfer in `APPROVED`.
    * Create a `TRANSFER_UNSHIP` audit log entry.
  * WHEN a Tài xế confirms goods received and vehicle departure (status → `IN_TRANSIT`), the system SHALL:
    * Require the transfer to be `APPROVED`.
    * Require exactly one assigned trip where `trip_type = 'TRANSFER'`.
    * Require `sent_qty` to have been recorded for every item.
    * Require the actor to be the driver assigned to the transfer trip.
    * Decrease source warehouse inventories: `total_qty -= sent_qty`.
    * Decrease source warehouse reserved quantity: `reserved_qty -= planned_qty`.
    * Increase In-Transit virtual warehouse inventories: `total_qty += sent_qty`.
    * Set status to `IN_TRANSIT`.
    * Create a `TRANSFER_DEPART` audit log entry.
  * WHEN a transfer is cancelled, the system SHALL:
    * IF the status is `NEW`, allow only Planner to cancel and do not change inventory quantities.
    * IF the status is `APPROVED` and `sent_qty` has not been recorded, allow only Trưởng kho nguồn assigned to the source warehouse or an authorized manager to cancel, decrease source warehouse reserved quantity: `reserved_qty -= planned_qty`, keep inventory totals unchanged, and create a `TRANSFER_CANCEL` audit log entry.
    * IF the status is `APPROVED` and `sent_qty` has already been recorded, reject cancellation until the transfer is unshipped/unloaded first.
    * IF the status is `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, or `CANCELLED`, reject the cancellation.

## 4. API Endpoints
* `PUT /api/v1/transfers/{id}/approve` - Duyệt điều chuyển và giữ chỗ hàng (Trưởng kho nguồn).
* `PUT /api/v1/transfers/{id}/reject` - Từ chối phiếu điều chuyển khi còn `NEW`, bắt buộc nhập lý do (Trưởng kho nguồn).
* `PUT /api/v1/transfers/{id}/reject` - Từ chối phiếu điều chuyển khi còn `NEW`, bắt buộc nhập lý do (Trưởng kho nguồn).
* `POST /api/v1/transfers/{id}/trip` - Lập chuyến xe nội bộ riêng cho phiếu điều chuyển (Dispatcher).
* `PUT /api/v1/transfers/{id}/ship` - Thủ kho nguồn ghi nhận số lượng gửi đi và bốc xếp lên xe.
* `PUT /api/v1/transfers/{id}/unship` - Hạ hàng từ xe xuống kho trước khi tài xế xác nhận rời kho; xóa số lượng đã ghi lên xe và giữ phiếu ở `APPROVED`.
* `PUT /api/v1/transfers/{id}/depart` - Tài xế xác nhận đã nhận hàng, xe rời kho, giải phóng giữ chỗ và chuyển sang In-Transit.
* `PUT /api/v1/transfers/{id}/cancel` - Hủy phiếu điều chuyển (`NEW`: Planner; unshipped `APPROVED`: Trưởng kho nguồn/manager, giải phóng giữ chỗ).

## 5. Validation and Error Handling
* `INSUFFICIENT_TRANSFER_STOCK` (HTTP 422): source warehouse lacks available quantity at approval.
* `TRANSFER_ALREADY_APPROVED` (HTTP 409): duplicate approval attempt.
* `REJECTION_REASON_REQUIRED` (HTTP 400): Trưởng kho nguồn rejects transfer without reason.
* `TRANSFER_TRIP_REQUIRED` (HTTP 400): departure attempted before assigning exactly one `TRANSFER` trip.
* `TRANSFER_TRIP_NOT_AVAILABLE` (HTTP 409): selected vehicle or driver is unavailable or already assigned to an overlapping trip.
* `TRANSFER_SHIP_NOT_ALLOWED` (HTTP 409): transfer is not `APPROVED`, actor is not in source warehouse scope, or shipment has already been recorded.
* `SENT_QTY_MISMATCH` (HTTP 400): any `sentQty` differs from the approved `plannedQty`.
* `TRANSFER_UNSHIP_NOT_ALLOWED` (HTTP 409): transfer is not `APPROVED`, shipment has not been recorded, actor is not authorized, or driver departure already occurred.
* `TRANSFER_DEPART_NOT_ALLOWED` (HTTP 409): transfer is not `APPROVED`, shipment is missing, trip is missing, or actor is not the assigned driver.
* `TRANSFER_CANCEL_NOT_ALLOWED` (HTTP 409): actor/status is not allowed to cancel, or shipment has already been recorded and must be unshipped first.

## 6. Audit Trail
* `TRANSFER_APPROVE`: Trưởng kho nguồn approves transfer, reserves source inventory, and changes status to `APPROVED`.
* `TRANSFER_REJECT`: Trưởng kho nguồn rejects a `NEW` transfer with a required reason and changes status to `REJECTED`.
* `TRANSFER_TRIP_ASSIGN`: Dispatcher assigns one dedicated transfer trip with available vehicle and assigned driver.
* `TRANSFER_SHIP`: Thủ kho nguồn records exact approved quantities as loaded onto vehicle.
* `TRANSFER_UNSHIP`: Thủ kho nguồn/authorized manager unloads goods before departure and clears recorded sent quantities.
* `TRANSFER_DEPART`: Assigned driver confirms goods received and vehicle departure; system moves inventory from source to In-Transit and changes status to `IN_TRANSIT`.
* `TRANSFER_CANCEL`: Planner cancels `NEW`, or Trưởng kho nguồn/authorized manager cancels unshipped `APPROVED` and releases reserved quantity.

## 7. Acceptance Criteria
* **Scenario: Transfer approval reserves stock**
  * Given source warehouse HP has 50 units of product X with `reserved_qty = 0` (available = 50)
  * When Planner creates a transfer of 30 units and Trưởng kho HP approves it
  * Then source inventory HP SHALL show `total_qty = 50`, `reserved_qty = 30`, and `available_qty = 20`.

* **Scenario: Transfer rejection requires reason**
  * Given a transfer is in `NEW` status
  * When Trưởng kho HP rejects it with reason "Không đủ tồn khả dụng tại kho nguồn"
  * Then the system SHALL set transfer status to `REJECTED`, store the rejection reason, create a `TRANSFER_REJECT` audit log entry, and keep inventory unchanged.

* **Scenario: Reject transfer without reason**
  * Given a transfer is in `NEW` status
  * When Trưởng kho HP rejects it without a reason
  * Then the system SHALL reject the request with `REJECTION_REASON_REQUIRED`.

* **Scenario: Planner cancels NEW transfer**
  * Given a transfer is in `NEW` status and no inventory has been reserved
  * When Planner cancels the transfer
  * Then the system SHALL set transfer status to `CANCELLED`, create a `TRANSFER_CANCEL` audit log entry, and keep inventory unchanged.

* **Scenario: Trưởng kho cancels APPROVED transfer**
  * Given a transfer is in `APPROVED` status, shipment has not been recorded, and source inventory has `reserved_qty = 30` for that transfer
  * When Trưởng kho nguồn cancels the transfer
  * Then the system SHALL set transfer status to `CANCELLED`, release the reserved quantity, and create a `TRANSFER_CANCEL` audit log entry.

* **Scenario: Block Planner cancel after approval**
  * Given a transfer is in `APPROVED` status
  * When Planner attempts to cancel it
  * Then the system SHALL reject the request with `TRANSFER_CANCEL_NOT_ALLOWED`.

* **Scenario: Reject shipment quantity mismatch**
  * Given a transfer is in `APPROVED` status for 30 units
  * When Thủ kho HP records shipment of 29 or 31 units
  * Then the system SHALL reject the request with `SENT_QTY_MISMATCH`.

* **Scenario: Block cancel after shipment preparation**
  * Given a transfer is in `APPROVED` status
  * And Thủ kho HP has recorded shipment of 30 units
  * When Trưởng kho nguồn attempts to cancel the transfer
  * Then the system SHALL reject the cancellation with `TRANSFER_CANCEL_NOT_ALLOWED` because the goods must be unshipped first.

* **Scenario: Unship before cancel**
  * Given a transfer is in `APPROVED` status
  * And Thủ kho HP has recorded shipment of 30 units
  * When Thủ kho HP unships/unloads the goods
  * Then the system SHALL clear the recorded `sent_qty`, keep status `APPROVED`, and create a `TRANSFER_UNSHIP` audit log entry.
  * When Trưởng kho nguồn cancels the transfer
  * Then the system SHALL release the reserved quantity and set status to `CANCELLED`.

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
  * When any actor attempts to cancel it
  * When any actor attempts to cancel it
  * Then the system SHALL reject the cancellation.
