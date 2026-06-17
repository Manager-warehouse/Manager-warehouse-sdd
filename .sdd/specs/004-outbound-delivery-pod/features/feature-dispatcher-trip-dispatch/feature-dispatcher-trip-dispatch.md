# Feature: Dispatcher Lập chuyến xe & Điều phối giao hàng nội bộ (US-WMS-08)

## 1. Context and Goal

Dispatcher gom các Delivery Order đã được Trưởng kho phê duyệt xuất kho (`WAREHOUSE_APPROVED`), gán xe nội bộ và tài xế nội bộ để lập chuyến giao hàng. Mỗi chuyến giao hàng outbound trong Sprint 1 chỉ xuất phát từ một kho nguồn; tất cả Delivery Order trong cùng chuyến phải thuộc cùng `warehouseId` với kho làm việc của Dispatcher, xe và tài xế được chọn cũng phải thuộc kho đó.

Khi tài xế được gán xác nhận đã nhận hàng và xe rời kho, hệ thống chuyển hàng đã đạt QC từ outbound staging sang kho ảo `IN_TRANSIT`, giải phóng reserved quantity ở outbound staging, tạo delivery attempt hiện tại cho từng Delivery Order và chuyển các Delivery Order sang `IN_TRANSIT`.

Trip được xem là hoàn tất vận hành khi xe quay trở lại kho và mọi Delivery Order trong chuyến đã có kết quả giao hàng cuối cùng (`COMPLETED` hoặc `RETURNED`). Nếu có Delivery Order `RETURNED`, hàng vẫn nằm trong kho ảo `IN_TRANSIT` cho tới khi luồng hoàn hàng riêng tiếp nhận và phân loại.

## 2. Actors

* **Dispatcher**: Lập chuyến xe trong kho được gán, chọn xe/tài xế thuộc kho đó, sắp xếp thứ tự giao hàng, sửa hoặc hủy trip khi trip chưa xuất phát.
* **Tài xế**: Xác nhận nhận hàng và xe rời kho cho trip được gán; sau khi hoàn tất tuyến giao hàng, xác nhận xe quay trở lại kho.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * The system SHALL allow Dispatcher users to create, update, cancel, and view delivery trips only for warehouses assigned to their user account.
  * The system SHALL only allow trip planning for Delivery Orders in `WAREHOUSE_APPROVED` status.
  * The system SHALL require all Delivery Orders in one outbound delivery trip to belong to the same warehouse.
  * The system SHALL require the selected vehicle and driver to belong to the same warehouse as the selected Delivery Orders and the Dispatcher user.
  * The system SHALL prevent a Delivery Order from being assigned to more than one active trip.
  * The system SHALL always validate total shipment weight against vehicle maximum weight before creating or updating a trip, and SHALL validate total shipment volume only when the selected vehicle has maximum volume configured.
  * The system SHALL release reserved inventory when a Delivery Order transitions to `IN_TRANSIT`.
  * The system SHALL create `TRIP_CREATE`, `TRIP_UPDATE`, `TRIP_CANCEL`, `TRIP_DEPART`, `COMPLETE_TRIP`, and `DELIVERY_ATTEMPT_CREATE` audit log entries with actor, warehouse, before state, and after state.

* **Event-driven:**
  * WHEN a Dispatcher creates a trip, the system SHALL:
    * Validate the Dispatcher is assigned to the trip warehouse.
    * Validate all selected Delivery Orders are in `WAREHOUSE_APPROVED`.
    * Validate all selected Delivery Orders belong to the same warehouse.
    * Validate none of the selected Delivery Orders is already assigned to an active trip.
    * Validate the selected vehicle belongs to the trip warehouse, is `AVAILABLE`, is not under maintenance, and is not assigned to another active trip.
    * Validate the selected driver belongs to the trip warehouse, is `AVAILABLE`, and is not assigned to another active trip.
    * Validate `stopOrder` values are unique within the trip.
    * Validate the trip has at least one Delivery Order.
    * Validate total weight does not exceed vehicle capacity, and validate total volume only when the selected vehicle has `maxVolumeM3` configured.
    * Store `tripType = DELIVERY`, selected warehouse, selected vehicle, driver, dispatcher, planned date, notes, and stop order.
    * Create the trip in `PLANNED` status and keep Delivery Orders in `WAREHOUSE_APPROVED` until departure.
  * WHEN a Dispatcher updates a trip in `PLANNED`, the system SHALL:
    * Allow changing `vehicleId`, `driverId`, `plannedDate`, `notes`, Delivery Order list, and Delivery Order stop order.
    * Allow adding or removing Delivery Orders only while the trip is still `PLANNED`.
    * Re-run the same warehouse, availability, active-trip, and capacity validations as trip creation against the final revised Delivery Order list.
    * Keep the trip in `PLANNED` and create `TRIP_UPDATE` audit log.
  * WHEN a Dispatcher cancels a trip in `PLANNED`, the system SHALL:
    * Store the cancellation reason.
    * Move the trip to `CANCELLED`.
    * Keep historical `vehicleId` and `driverId` on the cancelled trip, but release that vehicle and driver from active assignment so they become available for other trips.
    * Keep assigned Delivery Orders in `WAREHOUSE_APPROVED` so they can be assigned to another trip.
    * Create `TRIP_CANCEL` audit log.
  * WHEN the assigned driver confirms departure for a `PLANNED` trip, the system SHALL:
    * Validate the driver is the driver assigned to the trip.
    * Validate the trip is not cancelled and has not departed before.
    * Validate every Delivery Order in the trip is still `WAREHOUSE_APPROVED`.
    * Validate every Delivery Order has QC-passed staged quantity equal to requested quantity.
    * Set `issued_qty` to the validated QC-passed staged quantity for each Delivery Order item; for Sprint 1 full-shipment orders this SHALL equal `requested_qty`.
    * Move goods from outbound staging inventory to virtual `IN_TRANSIT` inventory by product/batch: decrease staging `total_qty` and `reserved_qty`, then increase In-Transit `total_qty` with `reserved_qty = 0`.
    * Create one current `deliveries` record per dispatched Delivery Order using the next `attempt_number`.
    * Set each new delivery attempt to `IN_TRANSIT`, with `trip_id`, `do_id`, `vehicle_id`, `driver_id`, `attempt_number`, and `dispatched_at`.
    * Update Delivery Order status to `IN_TRANSIT`, update Trip status to `IN_TRANSIT`, and mark vehicle/driver as `ON_TRIP`.
  * WHEN the assigned driver confirms the vehicle has returned to the source warehouse, the system SHALL:
    * Validate the trip is `IN_TRANSIT`.
    * Validate every assigned Delivery Order has reached `COMPLETED` or `RETURNED`.
    * Update Trip status to `COMPLETED`.
    * Mark vehicle and driver as `AVAILABLE`.
    * Keep any `RETURNED` goods in virtual `IN_TRANSIT` inventory until the separate return flow receives them.
    * Create `COMPLETE_TRIP` audit log.

## 4. API Endpoints

* `POST /api/v1/trips` - Dispatcher tạo chuyến xe mới từ các Delivery Order đã `WAREHOUSE_APPROVED`.
* `PUT /api/v1/trips/{id}` - Dispatcher sửa thông tin chuyến xe khi trip còn `PLANNED`.
* `PUT /api/v1/trips/{id}/cancel` - Dispatcher hủy chuyến xe khi trip còn `PLANNED`.
* `PUT /api/v1/trips/{id}/depart` - Tài xế được gán xác nhận đã nhận hàng và xe rời kho.
* `PUT /api/v1/trips/{id}/complete` - Tài xế được gán xác nhận xe đã quay trở lại kho sau khi mọi đơn trong chuyến có kết quả giao hàng.

### Create trip request payload

`POST /api/v1/trips` SHALL accept:

* `warehouseId` - Kho nguồn của chuyến giao hàng.
* `vehicleId` - Xe nội bộ được chọn.
* `driverId` - Tài xế nội bộ được gán.
* `plannedDate` - Ngày dự kiến giao hàng.
* `notes` - Ghi chú điều phối.
* `deliveryOrders[]` - Danh sách Delivery Order trong chuyến:
  * `doId` - Delivery Order đã được Trưởng kho duyệt xuất.
  * `stopOrder` - Thứ tự giao hàng trên tuyến.

Validation rules:

* Dispatcher SHALL be assigned to `warehouseId`.
* The system SHALL create outbound trips with `tripType = DELIVERY`.
* `deliveryOrders[]` SHALL contain at least one Delivery Order.
* All `deliveryOrders[].doId` SHALL belong to `warehouseId`.
* All Delivery Orders SHALL be in `WAREHOUSE_APPROVED`.
* No selected Delivery Order SHALL already belong to another active trip in `PLANNED` or `IN_TRANSIT`.
* `vehicleId` SHALL belong to `warehouseId`, be `AVAILABLE`, and not be assigned to another active trip.
* `driverId` SHALL belong to `warehouseId`, be `AVAILABLE`, and not be assigned to another active trip.
* `stopOrder` SHALL be unique within the request.
* Total shipment weight SHALL NOT exceed selected vehicle capacity.
* Total shipment volume SHALL NOT exceed selected vehicle capacity when `vehicle.maxVolumeM3` is configured; if `maxVolumeM3` is null, backend SHALL skip volume validation and validate weight only.

### Update trip request payload

`PUT /api/v1/trips/{id}` SHALL accept:

* `vehicleId` - Optional replacement vehicle.
* `driverId` - Optional replacement driver.
* `plannedDate` - Optional updated planned date.
* `notes` - Optional updated notes.
* `deliveryOrders[]` - Optional full revised Delivery Order list after editing; when provided, it replaces the existing trip Delivery Order list:
  * `doId`
  * `stopOrder`

Validation rules:

* Trip SHALL be in `PLANNED`.
* Dispatcher SHALL be assigned to the trip warehouse.
* The request MAY add Delivery Orders, remove Delivery Orders, or reorder existing Delivery Orders while the trip is still `PLANNED`.
* The final revised `deliveryOrders[]` SHALL contain at least one Delivery Order; to remove every Delivery Order, Dispatcher SHALL cancel the trip instead.
* Any added Delivery Order SHALL belong to the trip warehouse, be in `WAREHOUSE_APPROVED`, and not belong to another active trip.
* Any removed Delivery Order SHALL remain in `WAREHOUSE_APPROVED` and become eligible for assignment to another trip.
* Vehicle, driver, stop order, active-trip, same-warehouse, and capacity validations SHALL be re-run against the full revised Delivery Order list.
* Active-trip validation SHALL ignore the current trip being updated and reject only vehicle, driver, or Delivery Order assignments that belong to another active trip.
* Volume validation SHALL be skipped when the selected vehicle has no `maxVolumeM3`; weight validation always applies.
* The system SHALL reject updates after departure with `TRIP_NOT_EDITABLE`.

### Cancel trip request payload

`PUT /api/v1/trips/{id}/cancel` SHALL accept:

* `reason` - Cancellation reason, required.

Validation rules:

* Trip SHALL be in `PLANNED`.
* Dispatcher SHALL be assigned to the trip warehouse.
* The system SHALL reject cancellation after departure with `TRIP_NOT_EDITABLE`.
* Delivery Orders SHALL remain in `WAREHOUSE_APPROVED`.
* The cancelled trip SHALL keep historical `vehicleId` and `driverId`; the vehicle and driver SHALL no longer be treated as actively assigned to that cancelled trip.

### Depart trip request payload

`PUT /api/v1/trips/{id}/depart` SHALL accept:

* `confirmedAt` - Optional departure timestamp from the client; backend SHALL store server timestamp as authoritative audit time.
* `notes` - Optional driver departure notes.

Validation rules:

* Trip SHALL be in `PLANNED`.
* The authenticated driver SHALL match the trip `driverId`.
* Every Delivery Order in the trip SHALL still be in `WAREHOUSE_APPROVED`.
* Every Delivery Order SHALL have QC-passed staged quantity equal to requested quantity.
* Inventory movement SHALL run in one transaction with optimistic version checks.

### Complete trip request payload

`PUT /api/v1/trips/{id}/complete` SHALL accept:

* `returnedAt` - Optional vehicle return timestamp from the client; backend SHALL store server timestamp as authoritative audit time.
* `notes` - Optional driver return notes.

Validation rules:

* Trip SHALL be in `IN_TRANSIT`.
* The authenticated driver SHALL match the trip `driverId`.
* Every Delivery Order in the trip SHALL be `COMPLETED` or `RETURNED`.
* The system SHALL reject completion before all assigned Delivery Orders are `COMPLETED` or `RETURNED` with `TRIP_NOT_READY_TO_COMPLETE`.
* Trip completion SHALL NOT receive returned goods into regular inventory; returned goods remain in virtual `IN_TRANSIT` until the separate return flow handles them.

## 5. Delivery Attempt Initialization

The system SHALL create delivery attempts only at trip departure and initialize each attempt with status `IN_TRANSIT`.

Rationale:

* Before departure, the trip is only a plan and no physical delivery attempt has started.
* Driver mobile screens can query the current `IN_TRANSIT` attempt after departure.
* Cancellation or update of a `PLANNED` trip does not need to clean up unused delivery attempts.

## 6. Acceptance Criteria

* **Scenario: Create trip from approved orders**
  * Given multiple Delivery Orders are in `WAREHOUSE_APPROVED` and belong to the same warehouse
  * And the Dispatcher, vehicle, and driver belong to that warehouse
  * When Dispatcher assigns a valid vehicle, driver, planned date, and stop order
  * Then the system SHALL create the trip in `PLANNED` and keep the Delivery Orders ready for departure.

* **Scenario: Block cross-warehouse trip**
  * Given two Delivery Orders belong to different warehouses
  * When Dispatcher attempts to assign them to one trip
  * Then the system SHALL reject the request with `TRIP_DO_WAREHOUSE_MISMATCH`.

* **Scenario: Block unavailable vehicle or driver**
  * Given a vehicle or driver is not available or belongs to another warehouse
  * When Dispatcher attempts to create or update the trip with that vehicle or driver
  * Then the system SHALL reject the request with `VEHICLE_NOT_AVAILABLE` or `DRIVER_NOT_AVAILABLE`.

* **Scenario: Block overloaded vehicle**
  * Given total shipment weight or volume exceeds selected vehicle capacity
  * When Dispatcher attempts to create or update the trip
  * Then the system SHALL reject the request with `VEHICLE_OVERLOAD`.

* **Scenario: Block Delivery Order already assigned to another active trip**
  * Given a Delivery Order already belongs to a `PLANNED` or `IN_TRANSIT` trip
  * When Dispatcher attempts to assign it to another trip
  * Then the system SHALL reject the request with `DO_ALREADY_ASSIGNED_TO_TRIP`.

* **Scenario: Update planned trip**
  * Given a trip is in `PLANNED`
  * When Dispatcher changes driver, vehicle, planned date, notes, Delivery Order list, or stop order with valid data
  * Then the system SHALL update the trip and create a `TRIP_UPDATE` audit log.

* **Scenario: Add or remove Delivery Order before departure**
  * Given a trip is in `PLANNED`
  * When Dispatcher adds or removes Delivery Orders in the trip
  * Then the system SHALL re-run same-warehouse, active-trip, Delivery Order status, vehicle capacity, vehicle availability, and driver availability validations before saving the revised trip.

* **Scenario: Cancel planned trip**
  * Given a trip is in `PLANNED`
  * When Dispatcher cancels the trip with a reason
  * Then the system SHALL move the trip to `CANCELLED`, keep Delivery Orders in `WAREHOUSE_APPROVED`, keep historical vehicle/driver references while releasing active assignment, and create a `TRIP_CANCEL` audit log.

* **Scenario: Move staged goods to In-Transit at departure**
  * Given a trip loaded with 50 QC-passed units in outbound staging
  * When the assigned driver confirms goods received and vehicle departure
  * Then the system SHALL create delivery attempts in `IN_TRANSIT`, move Delivery Orders to `IN_TRANSIT`, decrease staging `total_qty` and `reserved_qty` by 50, and increase virtual In-Transit `total_qty` by 50 with `reserved_qty = 0`.

* **Scenario: Complete trip when vehicle returns to warehouse**
  * Given a trip is `IN_TRANSIT`
  * And every assigned Delivery Order is either `COMPLETED` or `RETURNED`
  * When the assigned driver confirms the vehicle has returned to the source warehouse
  * Then the system SHALL mark the trip as `COMPLETED`, mark vehicle/driver as `AVAILABLE`, and keep any returned goods in virtual In-Transit for the separate return flow.
