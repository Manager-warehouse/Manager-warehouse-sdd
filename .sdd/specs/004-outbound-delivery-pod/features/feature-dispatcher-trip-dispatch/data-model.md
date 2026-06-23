# Data Model: Dispatcher Trip Dispatch

## Trip

**Purpose**: Aggregate root for one outbound delivery route from a single warehouse.

**Fields used/updated**

- `id`
- `trip_number`
- `warehouse_id`
- `vehicle_id`
- `driver_id`
- `dispatcher_id`
- `planned_start_at` (Thời gian dự kiến bắt đầu)
- `planned_end_at` (Thời gian dự kiến kết thúc)
- `trip_type`
- `status`
- `total_weight_kg`
- `total_volume_m3`
- `cancel_reason`
- `departed_at`
- `completed_at`
- `notes`
- `created_at`
- `updated_at`

**Validation rules**

- `trip_type = DELIVERY` for Sprint 1 outbound dispatch.
- Create and update are allowed only while the trip is `PLANNED`.
- Cancel is allowed only while the trip is `PLANNED`.
- Depart is allowed only while the trip is `PLANNED`.
- Complete is allowed only while the trip is `IN_TRANSIT`.
- Dispatcher must be assigned to `warehouse_id`.

**State transitions**

- `PLANNED -> PLANNED`: valid update.
- `PLANNED -> CANCELLED`: dispatcher cancels trip.
- `PLANNED -> IN_TRANSIT`: assigned driver confirms departure.
- `IN_TRANSIT -> COMPLETED`: assigned driver confirms vehicle return after all Delivery Orders are terminal.

## TripDeliveryOrder

**Purpose**: Links one Delivery Order to one trip with a route stop position.

**Fields**

- `id`
- `trip_id`
- `do_id`
- `stop_order`

**Validation rules**

- Each trip must have at least one row.
- `stop_order` values must be unique within a trip.
- Every linked Delivery Order must belong to the same warehouse as the trip.
- A Delivery Order cannot belong to another active trip in `PLANNED` or `IN_TRANSIT`.

## DeliveryOrder

**Purpose**: Outbound order dispatched by the trip workflow.

**Fields used/updated**

- `id`
- `warehouse_id`
- `status`
- `updated_at`

**Validation rules**

- Trip create/update only accepts Delivery Orders in `WAREHOUSE_APPROVED`.
- Trip departure requires every assigned Delivery Order to remain `WAREHOUSE_APPROVED`.
- Trip departure moves assigned Delivery Orders to `IN_TRANSIT`.
- Trip completion requires every assigned Delivery Order to be `COMPLETED` or `RETURNED`.

## DeliveryOrderItem

**Purpose**: Item-level outbound quantity that must be issued at departure.

**Fields used/updated**

- `id`
- `delivery_order_id`
- `product_id`
- `requested_qty`
- `qc_pass_qty`
- `issued_qty`

**Validation rules**

- Before departure, `qc_pass_qty` for the Delivery Order must fully cover requested quantity.
- At departure, `issued_qty` is set to the validated QC-passed quantity for each item.
- Sprint 1 assumes full shipment, so `issued_qty = requested_qty` for valid departure.

## Inventory

**Purpose**: Holds staged outbound stock and virtual `IN_TRANSIT` stock moved during departure.

**Fields used**

- `id`
- `warehouse_id`
- `product_id`
- `batch_id`
- `location_id`
- `total_qty`
- `reserved_qty`
- `version`

**Validation rules**

- Departure decreases outbound staging `total_qty` and `reserved_qty`.
- Departure increases virtual `IN_TRANSIT` `total_qty` with `reserved_qty = 0`.
- All inventory updates must keep rows non-negative and pass optimistic locking.
- Trip completion does not move returned goods back to regular inventory.

## Delivery

**Purpose**: Current delivery attempt created per Delivery Order at departure.

**Fields used/updated**

- `id`
- `delivery_number`
- `do_id`
- `trip_id`
- `vehicle_id`
- `driver_id`
- `attempt_number`
- `status`
- `dispatched_at`
- `created_at`
- `updated_at`

**Validation rules**

- One new current attempt is created for each dispatched Delivery Order at departure.
- `attempt_number` increments from the highest existing attempt for that Delivery Order.
- New attempts start in `IN_TRANSIT`.
- Planned trip updates or cancellation do not create or delete delivery attempts.

## Vehicle

**Purpose**: Physical outbound resource assigned to the trip.

**Fields used/updated**

- `id`
- `warehouse_id`
- `status`
- `max_weight_kg`
- `max_volume_m3`
- `is_active`

**Validation rules**

- Vehicle must belong to the trip warehouse.
- Vehicle must be active and `AVAILABLE` for create/update.
- Vehicle must not be under maintenance.
- Departure marks vehicle `ON_TRIP`.
- Cancellation releases the vehicle from active assignment while keeping the historical `vehicle_id` on the cancelled trip.
- Completion marks vehicle `AVAILABLE`.

## Driver

**Purpose**: Human resource assigned to operate the trip and confirm departure/return.

**Fields used/updated**

- `id`
- `user_id`
- `warehouse_id`
- `status`
- `is_active`

**Validation rules**

- Driver must belong to the trip warehouse.
- Driver must be active and `AVAILABLE` for create/update.
- Only the assigned driver may depart or complete the trip.
- Departure marks driver `ON_TRIP`.
- Cancellation releases the driver from active assignment while keeping the historical `driver_id` on the cancelled trip.
- Completion marks driver `AVAILABLE`.

## Trip Capacity View

**Purpose**: Derived validation model used during trip create/update.

**Inputs**

- Sum of assigned Delivery Order item weights.
- Sum of assigned Delivery Order item volumes.
- Vehicle `max_weight_kg`.
- Optional vehicle `max_volume_m3`.

**Validation rules**

- Total weight must never exceed `max_weight_kg`.
- Total volume is validated only when `max_volume_m3` is configured.

## AuditLog

**Purpose**: Immutable trace for trip workflow and outbound dispatch mutations.

**Events**

- `TRIP_CREATE`: trip header, assigned vehicle/driver, stop order list, totals, before/after state.
- `TRIP_UPDATE`: revised vehicle/driver/date/notes/Delivery Order list and capacity totals.
- `TRIP_CANCEL`: cancellation reason and release of active assignment.
- `TRIP_DEPART`: staged-to-`IN_TRANSIT` stock movement, Delivery Order status changes, vehicle/driver status changes.
- `DELIVERY_ATTEMPT_CREATE`: one event or nested detail per created departure attempt.
- `COMPLETE_TRIP`: trip completion plus vehicle/driver release after all Delivery Orders are terminal.
