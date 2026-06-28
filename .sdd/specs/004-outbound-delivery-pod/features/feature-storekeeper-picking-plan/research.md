# Research: Storekeeper Picking Plan

## Decision: Use dedicated allocation rows for concrete picking

**Rationale**: A Delivery Order item may be planned from multiple batch/bin/zone rows, so concrete selection must live in `delivery_order_item_allocations` rather than overloading `delivery_order_items`. The item row can still keep summary quantities and optional single-source summary fields, while allocations remain the authoritative source for plan, picked, and replacement tracking.

**Alternatives considered**: Storing one batch/location directly on `delivery_order_items` was rejected because it cannot represent multi-bin picking or track per-allocation picked/QC history safely.

## Decision: Save picking plan by diffing old vs new allocations

**Rationale**: Revising a plan in `WAITING_PICKING` is simpler and safer when the request carries the full revised allocation set. The service can compare current allocations with requested allocations, release removed or reduced reservations, reserve added or increased rows, and preserve unchanged rows without ambiguity.

**Alternatives considered**: Patch-style add/remove operations were rejected because they complicate full-quantity validation and make return-to-bin requirements harder to enforce consistently.

## Decision: Transfer reservation from warehouse/product level to concrete inventory rows in the same transaction

**Rationale**: Planner creation already reserves quantity in `warehouse_product_reservations`. Initial picking-plan save must reduce that aggregate reservation while increasing `inventories.reserved_qty` for each selected allocation so the same stock cannot be promised twice.

**Alternatives considered**: Keeping both planner-level and concrete reservations in parallel was rejected because it double-counts stock and breaks availability rules.

## Decision: Rank candidate inventory with FIFO over valid regular stock only

**Rationale**: The current domain is household goods without expiry management. Candidate inventory should therefore be ordered by oldest received stock first while excluding quarantine, outbound staging, In-Transit, inactive locations, and any row whose `total_qty - reserved_qty <= 0`.

**Alternatives considered**: FEFO ordering was rejected because expiry is out of scope for this domain and would introduce a rule that conflicts with the constitution and feature spec.

## Decision: Require return-to-bin only for changed picked allocations

**Rationale**: Once warehouse staff has recorded picked/QC results for an allocation, removing or reducing that allocation would otherwise orphan staged or picked stock. The revised plan request should therefore include `returnToBinRecords` only for picked allocations whose reserved/picked commitment is being reduced, while unchanged picked allocations remain untouched.

**Alternatives considered**: Requiring returns for every picked allocation on any edit was rejected because it adds unnecessary churn and contradicts the feature acceptance criteria.

## Decision: Model replacement planning as new replacement allocations plus explicit replacement history

**Rationale**: QC fail recovery must preserve traceability between failed source stock and newly selected replacement stock. A dedicated replacement history table tied to the affected `do_item_id` and inventory rows records the actor, reason, quantity, and before/after source linkage while allowing the new allocation to re-enter the normal `WAITING_PICKING` flow.

**Alternatives considered**: Overwriting the original failed allocation in place was rejected because it erases QC fail provenance and weakens auditability.

## Decision: Keep service ownership in the Delivery Order aggregate

**Rationale**: Picking plan, QC, cancellation, and approval all mutate one outbound lifecycle. Extending `DeliveryOrderService` or a closely related outbound application service keeps status transitions, warehouse scope checks, and audit logging consistent with the current codebase.

**Alternatives considered**: A standalone picking micro-module was rejected because the repo is a single Spring Boot application and the outbound workflow already centers on `DeliveryOrderServiceImpl`.
