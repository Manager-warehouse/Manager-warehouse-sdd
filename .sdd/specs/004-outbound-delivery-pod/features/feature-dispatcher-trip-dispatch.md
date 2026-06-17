# Feature: Điều phối viên Lập Chuyến xe & Vận chuyển Nội bộ (US-WMS-08)

## 1. Context and Goal
Dispatcher gom nhiều đơn hàng và gán xe, tài xế nội bộ để lập Chuyến xe (Trip Log). Khi xe xuất phát rời kho, hệ thống tự động trừ tồn kho vật lý và chuyển trạng thái hàng sang In-Transit.

Sprint 1 delivery trips are single-warehouse trips: one trip can group many Delivery Orders, but all selected Delivery Orders MUST belong to the same `warehouse_id`. Partial pick and partial ship are out of scope; a Delivery Order can depart only when every line is fully picked, outbound-QC passed, warehouse-approved, and ready to ship.

## 2. Actors
* **Dispatcher (Người điều phối)**: Lập chuyến xe, gán xe và tài xế, sắp xếp Stop Order.
* **Tài xế**: Xác nhận nhận hàng và xuất phát rời kho.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always release reserved inventory when a delivery order transitions to `IN_TRANSIT`.
  * The system SHALL create `TRIP_CREATE` and `TRIP_DEPART` audit log entries for trip assignment and departure, including vehicle, driver, stop order, and inventory delta.
  * The system SHALL store outbound delivery trips with `trip_type = DELIVERY`.
  * Delivery Trip status lifecycle SHALL be `PLANNED -> IN_TRANSIT -> COMPLETED`, with `CANCELLED` allowed only from `PLANNED`.
  * The system SHALL NOT allow partial pick or partial ship in Sprint 1. Before departure, each Delivery Order item SHALL have `issued_qty = requested_qty = reserved_qty`.
* **Event-driven:**
  * WHEN a Dispatcher creates a trip, the system SHALL:
    * Validate that all selected DOs are in `READY_TO_SHIP` status.
    * Validate that all selected DOs belong to the same warehouse.
    * Validate that selected DOs are not assigned to another `PLANNED` or `IN_TRANSIT` trip.
    * Validate that the selected vehicle and driver are active, available, not expired/invalid, and not assigned to any incomplete trip (`PLANNED` or `IN_TRANSIT`).
    * Validate total weight and volume of DO items against vehicle maximum weight/volume capacity.
    * Set Trip status to `PLANNED` and keep DO status unchanged at `READY_TO_SHIP`.
  * WHEN a Dispatcher changes trip contents before departure, the system SHALL:
    * Allow adding/removing/reordering DOs only while Trip status is `PLANNED`.
    * Re-run same-warehouse, readiness, duplicate-assignment, and vehicle capacity validations.
    * Create an audit entry for the trip assignment change.
  * WHEN a Tài xế confirms departure (goods loaded, vehicle leaves warehouse), the system SHALL:
    * Verify the authenticated Driver owns the assigned `driver_id` for the trip.
    * Verify every DO item is fully issued (`issued_qty = requested_qty = reserved_qty`).
    * Update inventories using optimistic locking: `total_qty -= issued_qty` and `reserved_qty -= issued_qty`.
    * Update DO status to `IN_TRANSIT` and Trip status to `IN_TRANSIT`.
    * Set the assigned vehicle and driver to `ON_TRIP`.
  * WHEN all DOs in a delivery trip have a delivery result (`DELIVERED` or `RETURNED`), the system SHALL:
    * Update Trip status to `COMPLETED`.
    * Restore the assigned vehicle and driver to `AVAILABLE`.
    * Create a `TRIP_COMPLETE` audit entry.
    * Treat Trip `COMPLETED` as logistics completion; it SHALL NOT wait for invoice creation or DO `COMPLETED`.
  * WHEN a Dispatcher cancels a trip, the system SHALL:
    * Allow cancellation only while Trip status is `PLANNED`.
    * Unassign its DOs from the trip and keep those DOs in `READY_TO_SHIP`.
    * Restore the assigned vehicle and driver to `AVAILABLE` if they were reserved for the planned trip.
    * Create a cancellation audit entry.
* **State-driven:**
  * WHILE Trip status is `PLANNED`, Dispatcher MAY add, remove, or reorder DOs.
  * WHILE Trip status is `IN_TRANSIT`, Dispatcher SHALL NOT add, remove, reorder, or cancel DOs.
  * WHILE a vehicle or driver is assigned to any `PLANNED` or `IN_TRANSIT` trip, the system SHALL reject assigning them to another trip.
  * WHILE a Delivery Order is assigned to a `PLANNED` or `IN_TRANSIT` trip, the system SHALL reject assigning it to another trip.

## 4. API Endpoints
* `POST /api/v1/trips` - Tạo chuyến xe mới (giao diện Dispatcher).
* `PUT /api/v1/trips/{id}` - Cập nhật DOs, xe, tài xế, ngày giao dự kiến hoặc stop order khi trip còn `PLANNED`.
* `PUT /api/v1/trips/{id}/delivery-orders/{doId}/remove` - Gỡ một DO khỏi trip khi trip còn `PLANNED` mà không xóa dữ liệu nghiệp vụ.
* `PUT /api/v1/trips/{id}/depart` - Xác nhận xuất phát chuyến xe (Tài xế).
* `PUT /api/v1/trips/{id}/cancel` - Hủy trip khi trip còn `PLANNED`.

## 5. Acceptance Criteria
* **Scenario: Stock deduction at departure**
  * Given a trip loaded with fully picked DO items, where the DO items currently hold `issued_qty = requested_qty = reserved_qty = 50` and inventory `total_qty = 100`
  * When the driver clicks "Xác nhận nhận hàng & Xuất phát"
  * Then the system SHALL change DO status to `IN_TRANSIT`, change Trip status to `IN_TRANSIT`, decrease `total_qty` by 50, decrease `reserved_qty` by 50, and set vehicle/driver to `ON_TRIP`.

* **Scenario: Block cross-warehouse trip**
  * Given DO-HP belongs to warehouse HP and DO-HN belongs to warehouse HN
  * When Dispatcher tries to create one trip with both Delivery Orders
  * Then the system SHALL reject the trip with HTTP 422.

* **Scenario: Block partial shipment**
  * Given a Delivery Order line has `requested_qty = 50`, `reserved_qty = 50`, and `issued_qty = 45`
  * When the driver attempts to depart
  * Then the system SHALL reject departure and keep inventory and DO status unchanged.

* **Scenario: Reassign before departure**
  * Given Trip A is `PLANNED` and contains DO-001
  * When Dispatcher removes DO-001 from Trip A or moves it to another valid `PLANNED` trip
  * Then the system SHALL allow the change, re-run validations, and write an audit entry.

* **Scenario: Block reassignment after departure**
  * Given Trip A is `IN_TRANSIT`
  * When Dispatcher tries to remove or reorder a Delivery Order in Trip A
  * Then the system SHALL reject the change with HTTP 409.

* **Scenario: Prevent vehicle and driver double booking**
  * Given a vehicle or driver is already assigned to any `PLANNED` or `IN_TRANSIT` trip
  * When Dispatcher creates another trip with that vehicle or driver
  * Then the system SHALL reject the assignment.

* **Scenario: Complete trip after all delivery results are recorded**
  * Given a trip is `IN_TRANSIT` and all DOs in the trip are now `DELIVERED` or `RETURNED`
  * When the system processes the final delivery result
  * Then the system SHALL change Trip status to `COMPLETED`, restore vehicle/driver to `AVAILABLE`, and SHALL NOT wait for invoice creation.
