# Data Model: Planner Delivery Order

## DeliveryOrder

**Purpose**: Header for an outbound order requested by Planner.

**Fields used/created**

- `id`: primary key.
- `do_number`: generated unique code.
- `dealer_id`: dealer receiving the goods.
- `warehouse_id`: selected outbound warehouse.
- `type`: outbound Delivery Order type.
- `expected_delivery_date`: optional planned delivery date.
- `document_date`: business document date.
- `status`: starts as `NEW`; cancellation sets `CANCELLED`.
- `cancel_reason`: required when Warehouse Manager cancels.
- `created_by`: Planner user.
- `created_at`, `updated_at`.
- `notes`.

**Validation rules**

- Planner must be assigned to `warehouse_id`.
- Dealer must be active and not `CREDIT_HOLD`.
- `current_balance + order_value <= credit_limit`.
- Dealer must not have unpaid invoices overdue beyond the dealer's configured payment term days for one order.
- On create, status must be `NEW`.
- On cancel, status must be before `WAREHOUSE_APPROVED`.

**State transitions**

- `null -> NEW`: successful create.
- `NEW/WAITING_PICKING/QC_PENDING_APPROVAL/QC_COMPLETED -> CANCELLED`: Warehouse Manager cancellation before warehouse approval.
- `WAREHOUSE_APPROVED` and later states cannot be cancelled by this feature.

## DeliveryOrderItem

**Purpose**: Requested product lines for a Delivery Order.

**Fields used/created**

- `id`
- `delivery_order_id`
- `product_id`
- `requested_qty`
- `reserved_qty`: set to requested quantity for planner-level reservation tracking on the DO item.
- `issued_qty`: starts at `0`.
- `unit_price`: not trusted from client for credit control; invoice price is finalized by picking-plan when Storekeeper prepares concrete picking.

**Validation rules**

- `product_id` is required and active.
- `requested_qty > 0`.
- Duplicate product rows in one request should be normalized or rejected; implementation should prefer rejecting duplicates with a clear validation error unless the existing API convention already merges line quantities.

## WarehouseProductReservation

**Purpose**: Aggregate reservation for requested product quantity before concrete batch/bin/zone allocation.

**Fields**

- `warehouse_id`
- `product_id`
- `reserved_qty`
- `version`
- audit timestamps if present in schema.

**Validation rules**

- One logical row per warehouse/product.
- Increment by each Delivery Order item requested quantity on create.
- Decrement by remaining planner-level reservation on cancellation.
- Version check required on update.
- `reserved_qty >= 0` after every mutation.

## Inventory

**Purpose**: Source for availability calculation only in this feature.

**Fields used**

- `warehouse_id`
- `product_id`
- `total_qty`
- `reserved_qty`
- `batch_id`
- `location_id`
- `zone_id`
- `status/quality/location type` fields needed to exclude quarantine, failed-QC, in-transit, or invalid stock.
- `version`

**Validation rules**

- Planner feature must not assign final inventory rows.
- Availability includes only regular quality-valid inventory in the selected warehouse.
- Availability formula: `sum(total_qty - reserved_qty) - warehouse_product_reservations.reserved_qty`.

## Dealer

**Purpose**: Credit policy owner for Delivery Orders.

**Fields used**

- `id`
- `status`
- `current_balance`
- `credit_limit`
- `payment_term_days`: maximum allowed overdue days for any unpaid invoice before new Delivery Orders are blocked.

**Validation rules**

- `status != CREDIT_HOLD`.
- `current_balance + order_value <= credit_limit`.
- No unpaid invoice overdue beyond `payment_term_days`.

## AuditLog

**Purpose**: Immutable audit trail for create/cancel.

**Events**

- `DELIVERY_ORDER_CREATE`: includes actor, warehouse, dealer, credit-check result, item quantities, and reservation deltas.
- `DELIVERY_ORDER_CANCEL`: includes actor, before/after status, cancel reason, and released reservation deltas.
