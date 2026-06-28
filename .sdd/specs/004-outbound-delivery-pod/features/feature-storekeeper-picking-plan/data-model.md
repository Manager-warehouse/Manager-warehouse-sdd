# Data Model: Storekeeper Picking Plan

## DeliveryOrder

**Purpose**: Outbound aggregate root whose lifecycle is advanced by storekeeper planning and replacement actions.

**Fields used/updated**

- `id`
- `warehouse_id`
- `status`
- `updated_at`

**Validation rules**

- Initial picking plan save is allowed only from `NEW`.
- Picking-plan revision is allowed only while `WAITING_PICKING`.
- Replacement planning is allowed only while `QC_PENDING_APPROVAL`.
- Actor must be assigned to `warehouse_id`.

**State transitions**

- `NEW -> WAITING_PICKING`: initial plan saved successfully.
- `WAITING_PICKING -> WAITING_PICKING`: revised plan saved successfully.
- `QC_PENDING_APPROVAL -> WAITING_PICKING`: replacement plan saved for QC-failed quantity.

## DeliveryOrderItem

**Purpose**: Requested product line that must be fully covered by concrete allocations.

**Fields used/updated**

- `id`
- `delivery_order_id`
- `product_id`
- `requested_qty`
- `reserved_qty`
- `planned_qty`
- `picked_qty`
- `qc_pass_qty`
- `qc_fail_qty`
- `unit_price`
- optional summary `batch_id`, `location_id`, `zone_id`

**Validation rules**

- Sum of active allocation `planned_qty` for each item must equal `requested_qty`.
- `planned_qty` mirrors the sum of active allocations.
- `reserved_qty` remains aligned with the quantity still committed for outbound fulfillment.
- `unit_price` snapshot should be populated when the initial picking plan is saved if not already captured by earlier flow.

## DeliveryOrderItemAllocation

**Purpose**: Concrete picking allocation for a Delivery Order item against one inventory row.

**Fields**

- `id`
- `do_item_id`
- `inventory_id`
- `batch_id`
- `location_id`
- `zone_id`
- `planned_qty`
- `picked_qty`
- `is_replacement`
- `replaced_allocation_id`
- `created_by`
- `created_at`
- `updated_at`

**Validation rules**

- `planned_qty > 0`.
- Referenced inventory row must belong to the Delivery Order warehouse and product.
- Referenced inventory row must be valid regular quality-passed stock with `available > 0`.
- For initial and revised plans, per-item sum of `planned_qty` must equal item `requested_qty`.
- If an allocation has recorded picked/QC rows, reduction or removal requires matching return-to-bin records first.
- Replacement allocations set `is_replacement = true` and may reference the failed original allocation through `replaced_allocation_id`.

## DeliveryOrderItemReturnToBinRecord

**Purpose**: Audit-grade record proving picked goods were returned to the original source before a changed plan is applied.

**Fields**

- `id`
- `do_item_id`
- `allocation_id`
- `product_id`
- `batch_id`
- `original_location_id`
- `original_zone_id`
- `source_location_id`
- `returned_qty`
- `reason`
- `created_by`
- `created_at`

**Validation rules**

- Required only for picked allocations that are removed or reduced.
- `returned_qty > 0`.
- `returned_qty` cannot exceed the picked quantity being backed out from the original allocation.
- Return destination must match the original allocation batch/location/zone.
- Inventory movement back to the original row must be recorded before revised allocations are persisted.

## DeliveryOrderItemReplacement

**Purpose**: Trace failed QC quantity to replacement stock selected by Storekeeper.

**Fields**

- `id`
- `do_item_id`
- `failed_inventory_id`
- `replacement_inventory_id`
- `failed_batch_id`
- `failed_location_id`
- `replacement_batch_id`
- `replacement_location_id`
- `quantity`
- `reason`
- `created_by`
- `created_at`

**Validation rules**

- `quantity > 0`.
- Quantity cannot exceed unresolved QC-failed quantity for the item.
- Replacement source must be valid regular stock in the same warehouse.
- Replacement save also creates or updates the corresponding replacement allocation rows.

## WarehouseProductReservation

**Purpose**: Planner-level reservation bucket that must be converted to concrete reservations on initial picking-plan save.

**Fields used**

- `warehouse_id`
- `product_id`
- `reserved_qty`
- `version`

**Validation rules**

- On initial plan save, decrement by the amount assigned to concrete allocations for that product.
- On later plan revisions, only concrete inventory reservations change unless the feature explicitly restores planner-level quantity.
- `reserved_qty >= 0` after every mutation.
- Every update must pass optimistic locking.

## Inventory

**Purpose**: Concrete stock source for FIFO ranking, reservation, return-to-bin, and replacement planning.

**Fields used**

- `id`
- `warehouse_id`
- `product_id`
- `batch_id`
- `location_id`
- `zone_id`
- `received_date` or equivalent FIFO sort field
- `total_qty`
- `reserved_qty`
- `version`
- quality/location status fields

**Validation rules**

- Candidate list includes only rows in the Delivery Order warehouse with valid regular quality-passed stock.
- Exclude quarantine, outbound staging, In-Transit, inactive locations, and rows where `total_qty - reserved_qty <= 0`.
- Initial plan save increments `reserved_qty` on selected rows while decrementing planner reservation.
- Revised plan adjusts only the changed rows by delta.
- Return-to-bin moves quantity back to the original row before new reservation is applied.
- Replacement planning reserves new rows without reusing failed or quarantined stock.

## OutboundQcRecord

**Purpose**: Determines whether an allocation has already been picked/QC processed and whether return-to-bin is required before changing it.

**Fields used**

- `allocation_id`
- `picked_qty`
- `qc_pass_qty`
- `qc_fail_qty`
- `staging_location_id`
- `quarantine_location_id`

**Validation rules**

- Existing QC record on a changed allocation triggers return-to-bin validation for the reduced quantity.
- Unchanged picked allocations remain valid without a new return record.

## AuditLog

**Purpose**: Immutable trace for warehouse mutations driven by picking-plan workflows.

**Events**

- `PICKING_PLAN_SAVE`: actor, delivery order, before/after allocations, reservation deltas, status transition.
- `PICKED_GOODS_RETURN_TO_BIN`: actor, allocation, returned quantity, source state/location, original batch/bin/zone, before/after inventory state.
- `PICKING_REPLACEMENT_SAVE`: actor, failed source, replacement source, quantity, reason, and status transition back to `WAITING_PICKING`.
