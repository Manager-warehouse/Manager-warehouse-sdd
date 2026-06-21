# Feature: Thu kho Nguon Soan & Xuat hang Dieu chuyen (US-WMS-12)

## 1. Context and Goal
Thu kho tai kho nguon chiu trach nhiem soan hang theo phieu dieu chuyen da duoc phe duyet, boc xep len xe tai noi bo va ghi nhan so luong gui di. Dispatcher kho nguon phai lap mot chuyen xe noi bo rieng cho phieu dieu chuyen, va Tai xe duoc gan vao chuyen do xac nhan da nhan hang, xe roi kho de he thong chuyen ton kho sang trang thai `IN_TRANSIT`.

Luot ship nay thuoc man **Dieu chuyen noi bo** va chi ap dung cho ma `TRF-*`, khong lien quan den luong `RN-*` cua nhap NCC.
Trang thai dieu phoi cua tai xe la du lieu van hanh/danh muc. Tai xe khong tu doi trang thai san sang/ban/khong kha dung trong luong ship transfer; Dispatcher/admin quan ly lich ranh, con he thong co the tu dong chuyen sang `ON_TRIP` khi tai xe xac nhan roi kho.

## 2. Actors
- **Thu kho (Kho nguon)**: Soan hang, xac nhan xuat hang len xe tai.
- **Truong kho nguon**: Duyet hoac tu choi phieu dieu chuyen.
- **Dispatcher (Kho nguon)**: Lap chuyen xe noi bo rieng cho phieu dieu chuyen, gan xe va tai xe kha dung trong pham vi kho nguon.
- **Tai xe**: Xac nhan nhan hang va xe roi kho nguon.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * The system SHALL always route all inter-warehouse transfers through a virtual In-Transit warehouse for tracking.
  * The system SHALL always enforce `source_warehouse_id != destination_warehouse_id`.
  * The system SHALL always require exactly one dedicated internal trip (`trip_type = 'TRANSFER'`) before a transfer can depart.
  * The system SHALL enforce role plus warehouse scope for source warehouse operations.
  * The system SHALL keep Dispatcher planning limited to transfers whose source warehouse belongs to the Dispatcher scope.
  * The system SHALL allow driver selection only from drivers whose assigned warehouse scope includes the transfer source warehouse.

* **Event-driven:**
  * WHEN a Truong kho nguon approves a transfer (status -> `APPROVED`), the system SHALL:
    * Allow approval only when the actor is assigned to the transfer source warehouse or has an authorized manager override role.
    * Verify `available_qty = total_qty - reserved_qty >= planned_qty` at the source warehouse.
    * Increase source inventory reservation by the approved quantity.
    * Create a `TRANSFER_APPROVE` audit log entry.

  * WHEN a Truong kho nguon rejects a transfer, the system SHALL:
    * Allow rejection only while the transfer is `NEW`.
    * Require a non-empty rejection reason.
    * Set transfer status to `REJECTED`.
    * Not reserve or change inventory totals.
    * Create a `TRANSFER_REJECT` audit log entry.

  * WHEN a Dispatcher assigns transport for an approved transfer, the system SHALL:
    * Create or link exactly one `TRANSFER` trip with vehicle, driver, and planned date.
    * Require Dispatcher role; Planner SHALL NOT assign or create trips.
    * Require the transfer source warehouse to be in dispatcher scope.
    * Verify the selected vehicle and driver are available and not already assigned to an overlapping trip.
    * Reject drivers that are active but are not assigned to the transfer source warehouse scope.
    * Create a `TRANSFER_TRIP_ASSIGN` audit log entry.
    * Allow changing vehicle, driver, or planned date only before departure; after departure the trip assignment becomes immutable in Sprint 1.

  * WHEN a Thu kho nguon confirms shipment preparation, the system SHALL:
    * Allow the action only while the transfer is `APPROVED`.
    * Allow the action only when the actor is assigned to the transfer source warehouse.
    * Require `sent_qty = planned_qty` for every item.
    * Create a `TRANSFER_SHIP` audit log entry.

  * WHEN a user needs to cancel after shipment preparation but before driver departure, the system SHALL require an unship/unload action first:
    * Allow unship/unload only while the transfer is still `APPROVED` and `sent_qty` has been recorded.
    * Allow unship/unload only for Thu kho nguon assigned to the transfer source warehouse or an authorized manager.
    * Clear the recorded `sent_qty`/loaded marker and keep the transfer in `APPROVED`.
    * Create a `TRANSFER_UNSHIP` audit log entry.

  * WHEN the assigned Tai xe confirms goods received and vehicle departure (status -> `IN_TRANSIT`), the system SHALL:
    * Require the transfer to be `APPROVED`.
    * Require exactly one assigned trip where `trip_type = 'TRANSFER'`.
    * Require `sent_qty` to have been recorded for every item.
    * Require the actor to be the driver assigned to the transfer trip.
    * Decrease source warehouse inventories: `total_qty -= sent_qty`.
    * Decrease source warehouse reserved quantity: `reserved_qty -= planned_qty`.
    * Increase In-Transit virtual warehouse inventories: `total_qty += sent_qty`.
    * Set status to `IN_TRANSIT`.
    * Create a `TRANSFER_DEPART` audit log entry.
    * Treat the driver's dispatch status as system-driven for the trip lifecycle rather than a user-editable status inside the transfer screen.

  * WHEN a transfer is cancelled, the system SHALL:
    * IF the status is `NEW`, allow only Planner to cancel and do not change inventory quantities.
    * IF the status is `APPROVED` and `sent_qty` has not been recorded, allow only Truong kho nguon assigned to the source warehouse or an authorized manager to cancel, decrease source warehouse reserved quantity by the planned quantity, keep inventory totals unchanged, and create a `TRANSFER_CANCEL` audit log entry.
    * IF the status is `APPROVED` and `sent_qty` has already been recorded, reject cancellation until the transfer is unshipped/unloaded first.
    * IF the status is `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, or `CANCELLED`, reject the cancellation.

## 4. API Endpoints
- `POST /api/v1/transfers/{id}/approve` - Duyet dieu chuyen va giu cho hang (Truong kho nguon).
- `POST /api/v1/transfers/{id}/reject` - Tu choi phieu dieu chuyen khi con `NEW`, bat buoc nhap ly do (Truong kho nguon).
- `POST /api/v1/transfers/{id}/trip` - Lap chuyen xe noi bo rieng cho phieu dieu chuyen (Dispatcher kho nguon).
- `POST /api/v1/transfers/{id}/ship` - Thu kho nguon ghi nhan so luong gui di va boc xep len xe.
- `POST /api/v1/transfers/{id}/unship` - Ha hang tu xe xuong kho truoc khi tai xe xac nhan roi kho; xoa so luong da ghi len xe va giu phieu o `APPROVED`.
- `POST /api/v1/transfers/{id}/depart` - Tai xe duoc gan xac nhan da nhan hang, xe roi kho, giai phong giu cho va chuyen sang In-Transit.
- `POST /api/v1/transfers/{id}/cancel` - Huy phieu dieu chuyen (`NEW`: Planner; unshipped `APPROVED`: Truong kho nguon/manager, giai phong giu cho).

## 5. Validation and Error Handling
- `INSUFFICIENT_TRANSFER_STOCK` (HTTP 422): source warehouse lacks available quantity at approval.
- `TRANSFER_ALREADY_APPROVED` (HTTP 409): duplicate approval attempt.
- `REJECTION_REASON_REQUIRED` (HTTP 400): Truong kho nguon rejects transfer without reason.
- `TRANSFER_TRIP_REQUIRED` (HTTP 400): departure attempted before assigning exactly one `TRANSFER` trip.
- `TRANSFER_TRIP_NOT_AVAILABLE` (HTTP 409): selected vehicle or driver is unavailable or already assigned to an overlapping trip.
- `WAREHOUSE_SCOPE_REQUIRED` (HTTP 403): Dispatcher/manager/storekeeper acts outside source warehouse scope.
- `ASSIGNED_DRIVER_REQUIRED` (HTTP 409): departure actor is not the driver assigned to the transfer trip.
- `TRANSFER_SHIP_NOT_ALLOWED` (HTTP 409): transfer is not `APPROVED`, actor is not in source warehouse scope, or shipment has already been recorded.
- `SENT_QTY_MISMATCH` (HTTP 400): any `sentQty` differs from the approved `plannedQty`.
- `TRANSFER_UNSHIP_NOT_ALLOWED` (HTTP 409): transfer is not `APPROVED`, shipment has not been recorded, actor is not authorized, or driver departure already occurred.
- `TRANSFER_DEPART_NOT_ALLOWED` (HTTP 409): transfer is not `APPROVED`, shipment is missing, trip is missing, or actor is not the assigned driver.
- `TRANSFER_CANCEL_NOT_ALLOWED` (HTTP 409): actor/status is not allowed to cancel, or shipment has already been recorded and must be unshipped first.
- `TRANSFER_TRIP_LOCKED` (HTTP 409): an attempt is made to change driver, vehicle, or trip date after departure.

## 6. Audit Trail
- `TRANSFER_APPROVE`: Truong kho nguon approves transfer, reserves source inventory, and changes status to `APPROVED`.
- `TRANSFER_REJECT`: Truong kho nguon rejects a `NEW` transfer with a required reason and changes status to `REJECTED`.
- `TRANSFER_TRIP_ASSIGN`: Dispatcher assigns one dedicated transfer trip with source-scoped vehicle and driver.
- `TRANSFER_SHIP`: Thu kho nguon records exact approved quantities as loaded onto vehicle.
- `TRANSFER_UNSHIP`: Thu kho nguon/authorized manager unloads goods before departure and clears recorded sent quantities.
- `TRANSFER_DEPART`: Assigned driver confirms goods received and vehicle departure; system moves inventory from source to In-Transit and changes status to `IN_TRANSIT`.
- `TRANSFER_CANCEL`: Planner cancels `NEW`, or Truong kho nguon/authorized manager cancels unshipped `APPROVED` and releases reserved quantity.

## 7. Acceptance Criteria
- **Scenario: Transfer approval reserves stock**
  - Given source warehouse HP has 50 units of product X with `reserved_qty = 0` (available = 50)
  - When Planner creates a transfer of 30 units and Truong kho HP approves it
  - Then source inventory HP SHALL show `total_qty = 50`, `reserved_qty = 30`, and `available_qty = 20`.

- **Scenario: Transfer rejection requires reason**
  - Given a transfer is in `NEW` status
  - When Truong kho HP rejects it with reason "Khong du ton kha dung tai kho nguon"
  - Then the system SHALL set transfer status to `REJECTED`, store the rejection reason, create a `TRANSFER_REJECT` audit log entry, and keep inventory unchanged.

- **Scenario: Dispatcher only assigns source-scoped driver**
  - Given transfer `TRF-*` moves from HP to HN
  - And one active driver belongs only to HN
  - When Dispatcher kho HP opens the trip assignment form
  - Then that HN-only driver SHALL NOT appear in the selectable driver list.

- **Scenario: Planner cancels NEW transfer**
  - Given a transfer is in `NEW` status and no inventory has been reserved
  - When Planner cancels the transfer
  - Then the system SHALL set transfer status to `CANCELLED`, create a `TRANSFER_CANCEL` audit log entry, and keep inventory unchanged.

- **Scenario: Truong kho cancels APPROVED transfer**
  - Given a transfer is in `APPROVED` status, shipment has not been recorded, and source inventory has `reserved_qty = 30` for that transfer
  - When Truong kho nguon cancels the transfer
  - Then the system SHALL set transfer status to `CANCELLED`, release the reserved quantity, and create a `TRANSFER_CANCEL` audit log entry.

- **Scenario: Reject shipment quantity mismatch**
  - Given a transfer is in `APPROVED` status for 30 units
  - When Thu kho HP records shipment of 29 or 31 units
  - Then the system SHALL reject the request with `SENT_QTY_MISMATCH`.

- **Scenario: Block cancel after shipment preparation**
  - Given a transfer is in `APPROVED` status
  - And Thu kho HP has recorded shipment of 30 units
  - When Truong kho nguon attempts to cancel the transfer
  - Then the system SHALL reject the cancellation with `TRANSFER_CANCEL_NOT_ALLOWED` because the goods must be unshipped first.

- **Scenario: Unship before cancel**
  - Given a transfer is in `APPROVED` status
  - And Thu kho HP has recorded shipment of 30 units
  - When Thu kho HP unships/unloads the goods
  - Then the system SHALL clear the recorded `sent_qty`, keep status `APPROVED`, and create a `TRANSFER_UNSHIP` audit log entry.
  - When Truong kho nguon cancels the transfer
  - Then the system SHALL release the reserved quantity and set status to `CANCELLED`.

- **Scenario: Transfer departure releases reservation and moves to In-Transit**
  - Given source warehouse HP has 50 units of product X with `reserved_qty = 30`
  - And Dispatcher has assigned a dedicated transfer trip with vehicle and source-scoped driver
  - And Thu kho HP records shipment of 30 units
  - When the assigned Tai xe confirms goods received and vehicle departure
  - Then:
    - Source inventory HP SHALL show `total_qty = 20` and `reserved_qty = 0`.
    - Virtual In-Transit inventory SHALL show `total_qty = 30`.

- **Scenario: Allow reassignment before departure**
  - Given a transfer is `APPROVED`
  - And Dispatcher has assigned an initial driver and vehicle
  - And the driver has not yet departed
  - When Dispatcher changes the assigned driver to another valid source-scoped available driver
  - Then the system SHALL allow the reassignment and audit the trip assignment change.

- **Scenario: Block reassignment after departure**
  - Given a transfer is already `IN_TRANSIT`
  - When Dispatcher attempts to change the assigned driver or trip date
  - Then the system SHALL reject the request with `TRANSFER_TRIP_LOCKED`.

- **Scenario: Reject cancellation after departure**
  - Given a transfer is already in `IN_TRANSIT` status
  - When any actor attempts to cancel it
  - Then the system SHALL reject the cancellation.
