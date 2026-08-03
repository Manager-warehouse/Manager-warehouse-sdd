# Research: Planner Delivery Order

## Decision: Backend owns credit and stock validation

**Rationale**: Credit limit, overdue invoices, dealer status, warehouse scope, and availability are business invariants. They must run in the backend service transaction before any Delivery Order or reservation is persisted.

**Alternatives considered**: Frontend pre-check only was rejected because it can be bypassed and cannot safely handle concurrent reservations.

## Decision: Planner reserves at warehouse/product level only

**Rationale**: The spec separates Planner creation from Storekeeper picking. Planner should reserve requested product quantity in `warehouse_product_reservations`, while concrete batch/bin/location/zone reservation is assigned later by picking-plan. This avoids premature bin selection and keeps multi-bin picking flexible.

**Alternatives considered**: Reserving concrete `inventories.reserved_qty` during create was rejected because final picking locations are not selected by Planner.

## Decision: Availability excludes quarantine and non-quality stock

**Rationale**: Outbound stock must come from quality-valid regular inventory. Availability is calculated as `sum(inventories.total_qty - inventories.reserved_qty)` for eligible regular inventory in the selected warehouse/product minus existing `warehouse_product_reservations.reserved_qty`.

**Alternatives considered**: Using aggregate product balance without zone/quality filtering was rejected because it could include quarantine or failed-QC stock.

## Decision: Optimistic locking protects reservation updates

**Rationale**: Multiple planners may create orders for the same warehouse/product. `warehouse_product_reservations` and any affected cancellation reservation rows must use version checks so one request cannot overwrite another.

**Alternatives considered**: Database-level pessimistic locks were not selected for Sprint 1 because optimistic locking matches project conventions and keeps transactions shorter.

## Decision: Create-time credit check uses backend-calculated order value

**Rationale**: Credit check needs an `order_value` before the Delivery Order exists. The backend should calculate this from the approved selling price effective at create time or from the pricing service result, not trust a client-supplied total. The final invoice still uses `delivery_order_items.unit_price` captured later when Storekeeper prepares the picking plan, per outbound spec.

**Alternatives considered**: Trusting client-provided unit prices was rejected because credit control must be server-authoritative.

## Decision: Cancellation authority is state-based

**Rationale**: Planner may cancel only while the Delivery Order is still `NEW`, before Storekeeper saves the first picking plan. At this point the order has only planner-level reservations, so cancellation releases `warehouse_product_reservations`, marks the DO `CANCELLED`, and writes audit. After Storekeeper planning starts, downstream warehouse workflows own the order and Planner cancellation is blocked. Warehouse Manager cancellation before outbound release approval remains available for later pre-approval states.

**Alternatives considered**: Allowing Planner cancellation after `WAITING_PICKING` was rejected because concrete inventory allocation and warehouse work may already exist. Restricting all cancellation to Warehouse Manager was rejected because Planner needs a correction window immediately after creating a mistaken `NEW` order.

## Decision: Insufficient stock rejection does not suggest other warehouses

**Rationale**: When selected warehouse stock is insufficient, the API rejects creation with a clear insufficient-stock reason only. It does not return candidate warehouses with enough availability because Planner warehouse scope and business intent are explicit, and this feature must not redirect planning decisions to another warehouse.

**Alternatives considered**: Returning read-only cross-warehouse hints was rejected because it can encourage creating the order against a different warehouse outside this feature's selected-warehouse flow.

## Decision: Fleet ceiling counts every active vehicle assigned to the selected warehouse

**Rationale**: This validation answers whether one Delivery Order can ever be fulfilled in one coordinated delivery wave with the warehouse's current fleet. Therefore it sums `vehicles.max_weight_kg` for all active vehicles assigned to that warehouse, including vehicles currently ready, busy, on trip, or under maintenance. Operational status affects when Dispatcher can plan the delivery, not the warehouse's maximum fleet ceiling.

**Alternatives considered**: Counting only currently ready vehicles was rejected because temporary unavailability should produce a wait/replan outcome in Dispatcher flow, not force Planner to split a Delivery Order. Counting inactive or another warehouse's vehicles was rejected because those vehicles are not part of the selected warehouse fleet.

## Decision: Product weight is mandatory for fleet-capacity validation

**Rationale**: Delivery Order total weight is calculated as `sum(requested_qty * products.weight_kg)`. Treating a missing or non-positive product weight as zero could allow an unsafe oversized order, so create/update is rejected with `PRODUCT_WEIGHT_MISSING` before persistence or reservation mutation.

**Alternatives considered**: Defaulting missing weight to zero or estimating from product category was rejected because both understate payload and are not auditable master-data values.

## Decision: Capacity guard runs before persistence and reservation mutation

**Rationale**: A rejected create/update must leave Delivery Order data, planner-level reservations, concrete inventory, and success audit history unchanged. The service performs role/scope and request/master-data validation, calculates order and fleet weight, and rejects before applying reservation deltas or saving the order.

**Alternatives considered**: Creating then cancelling the order was rejected because it creates unnecessary transaction history and reservation churn for a request that was never valid.
