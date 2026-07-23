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

## Decision: Cancellation is restricted to Warehouse Manager before `WAREHOUSE_APPROVED`

**Rationale**: The feature scope allows cancellation before outbound release approval only. Cancellation releases planner-level reservation, releases concrete reservation if picking already assigned it, marks the DO `CANCELLED`, and writes audit.

**Alternatives considered**: Allowing Planner cancellation was rejected because the spec assigns cancellation authority to Warehouse Manager.

## Decision: Insufficient stock rejection does not suggest other warehouses

**Rationale**: When selected warehouse stock is insufficient, the API rejects creation with a clear insufficient-stock reason only. It does not return candidate warehouses with enough availability because Planner warehouse scope and business intent are explicit, and this feature must not redirect planning decisions to another warehouse.

**Alternatives considered**: Returning read-only cross-warehouse hints was rejected because it can encourage creating the order against a different warehouse outside this feature's selected-warehouse flow.
