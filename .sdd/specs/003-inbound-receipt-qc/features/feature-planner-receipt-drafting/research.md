# Research: Planner Receipt Drafting

## Decision: Treat source channel as a dedicated enum

**Decision**: Add/use a receipt source channel enum with `ZALO` and `EMAIL` only at DTO validation and persistence boundaries.

**Rationale**: The feature spec requires only two allowed values. Using an enum prevents loose strings like `Zalo`, `email`, or other manual channels from entering workflow state.

**Alternatives considered**: Keep `Receipt.sourceChannel` as a string only. Rejected because it makes validation and OpenAPI weaker. A later migration can convert the entity field to enum if the current schema permits.

## Decision: Store expected quantity as a positive integer

**Decision**: API, service, entity, and database schema must use positive integer semantics for `receipt_items.expected_qty`.

**Rationale**: This resolves the spec conflict in favor of the feature-level EARS requirement and prevents fractional expected quantities from bypassing Java validation. `actual_qty`, `over_received_qty`, and QC sample quantities can remain decimal-compatible because later physical receiving/QC flows may need measured quantities.

**Alternatives considered**: Keep `expected_qty` as `DECIMAL(10,2)` and validate only in Java. Rejected because the database would still accept fractional values through other write paths.

## Decision: Duplicate source reference scope

**Decision**: Reject duplicate `source_reference` / `source_order_code` for the same supplier and warehouse across non-cancelled/non-rejected purchase receipts.

**Rationale**: The feature requires duplicate rejection by supplier and warehouse. Limiting the check to purchase receipts avoids blocking return flows, while excluding terminal cancelled-style records follows transaction status rules. Current status enum has no `CANCELLED`, so implementation should start with all existing purchase receipts except clearly rejected records if business approves that rule.

**Alternatives considered**: Global unique PO number across all warehouses. Rejected because the feature scopes uniqueness to supplier and warehouse.

## Decision: Generate receipt numbers with a locked document counter

**Decision**: Allocate receipt numbers from a `document_sequences` row locked with JPA pessimistic write, then format numbers as `RN-yyyyMMdd-000001`.

**Rationale**: Random generation plus existence checks can race under concurrent creates. A locked counter keeps generation deterministic and avoids raw SQL in application code.

**Alternatives considered**: PostgreSQL `nextval()` from application code. Rejected because the project forbids raw SQL in application code; JPA-managed locking keeps the rule intact.

## Decision: Create receipt only, no inventory mutation

**Decision**: `POST /api/v1/receipts` creates `Receipt` and `ReceiptItem` rows only, with status `PENDING_RECEIPT`.

**Rationale**: Inbound spec says inventory is unchanged until later QC/approval transitions. This preserves the QC gate and avoids premature stock availability.

**Alternatives considered**: Reserve inbound quantity or create holding inventory at drafting time. Rejected because it conflicts with inbound inventory timing rules.

## Decision: Audit action for creation

**Decision**: Create an audit log entry equivalent to `RECEIPT_CREATE` using the existing audit infrastructure; if enums remain generic, use `AuditAction.CREATE` with entity type `RECEIPT` and after-state containing receipt header and item summary.

**Rationale**: AGENTS and inbound spec require audit trail for inbound mutations. Current `AuditAction` enum does not contain domain-specific action names, so generic action plus entity/state is the least disruptive implementation path.

**Alternatives considered**: Add `RECEIPT_CREATE` to `AuditAction`. Acceptable only if the audit module is being standardized around domain-specific action constants in the same slice.

## Documentation Conflicts Reviewed

1. Parent spec links point to `features/feature-planner-receipt-drafting.md`, but the actual feature file is now in `features/feature-planner-receipt-drafting/feature-planner-receipt-drafting.md`.
2. Parent spec data model previously used `DECIMAL(10,2)` for `expected_qty`; this feature standardizes receipt expected quantities to `INTEGER`.
3. Root `CONSTITUTION.md` is referenced by AGENTS, but the repo currently stores the active constitution at `.specify/memory/constitution.md`.
4. `CLAUDE.md` contains visible merge conflict markers in an unrelated stocktake approval section; it does not block this receipt drafting plan but should be cleaned separately.
