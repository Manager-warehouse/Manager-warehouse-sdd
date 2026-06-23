# Research: Warehouse Staff Picking & QC Outbound

## Decision: Treat pick/QC submission as one complete snapshot of the active planned allocation set

**Rationale**: The feature spec explicitly forbids partial submission. Requiring one request to cover every currently `PLANNED` allocation without an existing QC record keeps the state transition deterministic, avoids half-finished outbound movement, and makes `WAITING_PICKING -> QC_PENDING_APPROVAL` atomic.

**Alternatives considered**: Saving per-row or partial progress was rejected because it would require a new in-between status, complicate replacement re-entry, and increase reconciliation risk across staging and quarantine rows.

## Decision: Persist allocation-level outbound QC records as the authoritative duplicate guard

**Rationale**: `outbound_qc_records` keyed by allocation provides a direct way to detect whether a planned allocation already completed a pick/QC cycle. This aligns with the spec requirement that duplicate submissions are blocked unless retried safely through idempotency.

**Alternatives considered**: Deriving duplicate state only from `picked_qty` or item summaries was rejected because summary fields lose request identity and cannot distinguish safe retries from true resubmissions.

## Decision: Support idempotent retry with request key plus payload hash

**Rationale**: Network retries should return the previous successful result without replaying inventory movement. An `idempotencyKey` paired with a normalized request hash allows exact-retry acceptance while rejecting key reuse with a different payload.

**Alternatives considered**: No idempotency support was rejected because the feature spec requires safe retry. Accepting the same key without validating payload identity was rejected because it can hide conflicting submissions.

## Decision: Move QC-passed goods from source inventory to outbound staging by inventory transfer semantics, not issue semantics

**Rationale**: At this step the goods are not yet sold or departed. The source regular inventory must decrease `total_qty` and `reserved_qty`, while a staging inventory row for the same product and batch increases `total_qty` and `reserved_qty`, preserving reservation for the Delivery Order until warehouse approval and dispatch.

**Alternatives considered**: Directly decreasing stock as if issued was rejected because outbound fulfillment is not final until later `IN_TRANSIT` logic.

## Decision: Move QC-failed goods into quarantine while also creating quarantine and inventory adjustment records

**Rationale**: The constitution requires failed QC goods to enter quarantine and stay out of available inventory. Creating a quarantine record plus `QC_FAIL_OUTBOUND` adjustment preserves traceability between the Delivery Order, allocation, source row, and quarantined quantity.

**Alternatives considered**: Leaving fail quantity on the source allocation with a flag was rejected because it would keep damaged goods mixed with regular stock and weaken auditability.

## Decision: Let replacement cycles submit only newly active allocations without replaying previously passed staging allocations

**Rationale**: Once a prior allocation already passed QC and is sitting in outbound staging, forcing warehouse staff to resubmit it on replacement cycles would duplicate QC evidence and risk double movement. The active cycle should therefore include only replacement allocations or allocations still lacking QC records.

**Alternatives considered**: Requiring all allocations on every cycle was rejected because it conflicts with the feature requirements and idempotency rules.

## Decision: Keep quality approval and warehouse approval/reject inside the Delivery Order aggregate

**Rationale**: Pick/QC result, Storekeeper quality approval, and Warehouse Manager approval/reject are sequential state transitions on the same outbound order. Keeping them in one service boundary preserves consistent warehouse scope checks, audit format, and reject-return logic.

**Alternatives considered**: Splitting approvals into separate services or modules was rejected because the repo already centers outbound state changes in `DeliveryOrderServiceImpl`.
