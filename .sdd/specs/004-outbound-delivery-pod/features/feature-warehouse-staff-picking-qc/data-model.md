# Data Model: Warehouse Staff Picking & QC Outbound

## DeliveryOrder

**Purpose**: Outbound aggregate root whose lifecycle advances from active picking to QC review, quality approval, warehouse approval, or warehouse rejection.

**Fields used/updated**

- `id`
- `warehouse_id`
- `status`
- `rejection_reason`
- `updated_at`

**Validation rules**

- Pick/QC submission is allowed only from `WAITING_PICKING`.
- Quality approval is allowed only from `QC_PENDING_APPROVAL`.
- Warehouse approval and reject are allowed only from `QC_COMPLETED`.
- Actor must be assigned to `warehouse_id`.

**State transitions**

- `WAITING_PICKING -> QC_PENDING_APPROVAL`: full active pick/QC submission succeeds.
- `QC_PENDING_APPROVAL -> WAITING_PICKING`: replacement plan saved by Storekeeper in the paired feature.
- `QC_PENDING_APPROVAL -> QC_COMPLETED`: Storekeeper quality approval succeeds after all requested quantity has QC-passed goods available.
- `QC_COMPLETED -> WAREHOUSE_APPROVED`: Warehouse Manager approves outbound release.
- `QC_COMPLETED -> REJECTED`: Warehouse Manager rejects and returned QC-passed goods are restored to original rows.

## DeliveryOrderItem

**Purpose**: Requested product line whose cumulative picked, passed, failed, and staged state must remain consistent across one or more QC cycles.

**Fields used/updated**

- `id`
- `delivery_order_id`
- `product_id`
- `requested_qty`
- `planned_qty`
- `picked_qty`
- `qc_pass_qty`
- `qc_fail_qty`
- `reserved_qty`

**Validation rules**

- For the active submission, total `pickedQty` across the submitted rows for each item must equal the item's currently active planned quantity.
- Cumulative `qc_pass_qty` must not exceed `requested_qty`.
- Replacement cycles only add new QC records for allocations that still have no outbound QC row.

## DeliveryOrderItemAllocation

**Purpose**: Concrete stock assignment prepared by Storekeeper and consumed by warehouse-staff picking/QC.

**Fields used/updated**

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

**Validation rules**

- Every pick/QC row must reference an existing allocation in the Delivery Order warehouse.
- Request `batchId`, `locationId`, and `zoneId` must match the allocation and original inventory row.
- The active cycle may include only allocations without an existing QC record, except for a safe idempotent replay of the same completed request.

## OutboundQcRecord

**Purpose**: Immutable proof of one successful pick/QC result row for a concrete allocation in a given active cycle.

**Fields**

- `id`
- `do_item_id`
- `allocation_id`
- `batch_id`
- `location_id`
- `zone_id`
- `staging_location_id`
- `quarantine_location_id`
- `picked_qty`
- `qc_pass_qty`
- `qc_fail_qty`
- `qc_fail_reason`
- `quarantine_record_id`
- `idempotency_key`
- `request_hash`
- `notes`
- `created_at`

**Validation rules**

- One successful QC row per allocation per active cycle.
- `picked_qty >= 0`, `qc_pass_qty >= 0`, `qc_fail_qty >= 0`.
- `picked_qty = qc_pass_qty + qc_fail_qty`.
- `qc_fail_reason` is required when `qc_fail_qty > 0`.
- A repeated request with the same `idempotency_key` and the same payload hash returns the previous success without replaying movement.

## Inventory

**Purpose**: Holds the concrete source, outbound staging, and quarantine rows mutated by the QC result flow.

**Fields used/updated**

- `id`
- `warehouse_id`
- `product_id`
- `batch_id`
- `location_id`
- `total_qty`
- `reserved_qty`
- `version`
- `updated_at`

**Validation rules**

- Source regular row decreases `total_qty` and `reserved_qty` for both pass and fail quantities.
- Outbound staging row increases `total_qty` and `reserved_qty` for `qc_pass_qty`.
- Quarantine row increases `total_qty` and keeps `reserved_qty = 0` for `qc_fail_qty`.
- Reject return from outbound staging restores source `total_qty` and available regular stock while releasing staging reservation.
- Every update must pass optimistic locking and preserve non-negative quantities.

## QuarantineRecord

**Purpose**: Trace a failed outbound QC quantity into quarantine handling.

**Fields used/created**

- `id`
- `warehouse_id`
- `product_id`
- `batch_id`
- `quantity`
- `reason`
- references to Delivery Order and allocation context

**Validation rules**

- Created for every QC-failed quantity greater than zero.
- Must link back to the affected Delivery Order and concrete allocation context for audit and later disposition.

## InventoryAdjustment

**Purpose**: Financial and stock-control evidence that regular available inventory was reduced because of outbound QC failure.

**Fields used/created**

- `id`
- `type = QC_FAIL_OUTBOUND`
- `warehouse_id`
- `product_id`
- `batch_id`
- `quantity`
- references to Delivery Order, allocation, QC record, and quarantine record
- `created_at`

**Validation rules**

- One adjustment per failed outbound QC movement group or per row, depending on the repo pattern chosen.
- Quantity is recorded as a negative adjustment against the regular source stock context.

## DeliveryOrderQualityApproval

**Purpose**: Logical mutation event that advances a QC-reviewed order into `QC_COMPLETED`.

**Fields used**

- Delivery Order `status`
- optional `notes`
- audit metadata

**Validation rules**

- Allowed only when every requested quantity has matching QC-passed goods available in outbound staging after any replacement cycle.
- Must reject with `QC_REPLACEMENT_REQUIRED` if failed quantity remains unresolved.

## DeliveryOrderWarehouseRejectReturn

**Purpose**: Structured input proving all QC-passed staging quantity was returned to original source rows when Warehouse Manager rejects outbound release.

**Fields**

- `do_item_id`
- `allocation_id`
- `batch_id`
- `returned_qty`
- `source_location_id`
- `original_location_id`
- `original_zone_id`
- `reason`

**Validation rules**

- Required when the Delivery Order still holds QC-passed goods in outbound staging.
- Total returned quantity per allocation must equal the current staged passed quantity for that allocation.
- Full request total must equal all QC-passed staging quantity on the Delivery Order.

## AuditLog

**Purpose**: Immutable before/after trace for outbound picking, QC, quarantine, approval, reject, and staging-return actions.

**Events**

- `DELIVERY_ORDER_PICK_COMPLETE`: actor, idempotency context, per-allocation picked/pass/fail rows, and status move to `QC_PENDING_APPROVAL`.
- `OUTBOUND_QC_FAIL_QUARANTINE`: actor, failed quantity, quarantine target, adjustment reference, and before/after inventory state.
- `DELIVERY_ORDER_QC_APPROVE`: actor, notes, before/after Delivery Order state, and staged pass summary.
- `DELIVERY_ORDER_WAREHOUSE_APPROVE`: actor, notes, before/after Delivery Order state.
- `DELIVERY_ORDER_WAREHOUSE_REJECT`: actor, reason, before/after Delivery Order state, and returned staging summary.
- `PICKED_GOODS_RETURN_TO_BIN`: actor, returned QC-passed goods from staging to original row during warehouse rejection.
