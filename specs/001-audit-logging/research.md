# Research: Audit Logging Backend

**Feature**: System Audit Logging
**Date**: 2026-06-03
**Status**: Complete — All unknowns resolved via user clarification

---

## R1: Audit Log Scope (Which entities to log?)

**Decision**: Log only warehouse business entities — documents/orders and inventory changes.
**Rationale**: User confirmed they only need traceability for warehouse operations, not master data or auth events.
**Alternatives considered**: Log all entities (rejected — too much noise), log only documents (rejected — need inventory quantity changes too).

**Entity types**: RECEIPT, ISSUE, TRANSFER, ADJUSTMENT, STOCKTAKE, DELIVERY_ORDER, BATCH, INVENTORY, RETURN, SCRAP_DISPOSAL, TRIP.

## R2: Diff-only JSONB vs Full Snapshot

**Decision**: Store only changed fields in `old_value` / `new_value` (diff-only approach).
**Rationale**: Reduces storage, clearer audit trail showing exactly what changed.
**Alternatives considered**: Full object snapshot before/after (rejected — bloats JSONB, harder to read), single `changes` column with `{field: {from, to}}` format (rejected — user prefers keeping 2 separate columns).

**Sensitive field exclusion**: `password_hash` and credential fields must be stripped before serialization.

## R3: Pagination Strategy

**Decision**: Cursor-based pagination using `id` as cursor, sorted by `timestamp DESC`.
**Rationale**: Better performance than offset-based for large datasets. ID-based cursor is monotonically increasing and correlates with timestamp order.
**Alternatives considered**: Offset-based (rejected — performance degrades with large offsets), timestamp-based cursor (rejected — potential duplicates at same millisecond).

**Default page size**: 30 records.

## R4: Date Range Filtering

**Decision**: Accept `startDate` and `endDate` as `LocalDate` from frontend. Backend converts `endDate` to `endDate.atTime(23, 59, 59)`. Default: last 7 days.
**Rationale**: Frontend typically works with dates, not timestamps. Backend handles time boundary.
**Alternatives considered**: Accept full ISO timestamp (rejected — adds frontend complexity for common use case).

## R5: actor_id Nullability

**Decision**: `actor_id` is NOT NULL. No system jobs exist.
**Rationale**: All actions in WMS Phúc Anh are user-initiated. No background/scheduled jobs modify business data.
**Alternatives considered**: Keep nullable for future system jobs (rejected by user — YAGNI).

## R6: Transfer Creates 2 Entries

**Decision**: A transfer operation creates 2 audit log entries — one for source warehouse (outbound) and one for destination warehouse (inbound).
**Rationale**: Enables warehouse-scoped filtering. Each warehouse manager sees relevant activity for their warehouse.
**Alternatives considered**: Single entry with both warehouse IDs (rejected — breaks single warehouse_id FK model), single entry with source warehouse only (rejected — destination warehouse manager loses visibility).

## R7: Description Auto-Generation

**Decision**: System auto-generates `description` field using format: `{ACTION} {ENTITY_TYPE} {ENTITY_CODE}`.
**Rationale**: Consistent, predictable format. No human input needed.
**Examples**: "CREATE RECEIPT PN-2026-001", "APPROVE TRANSFER TC-2026-003".

## R8: Access Control for Audit Log API

**Decision**: ADMIN and CEO see all logs. WAREHOUSE_MANAGER sees only logs for assigned warehouses. ACCOUNTANT sees all logs.
**Rationale**: Matches RBAC isolation principle. Warehouse managers are scoped to their warehouses. Accountants need cross-warehouse visibility for reconciliation.

## R9: Immutability & Retention

**Decision**: Audit logs are immutable (no UPDATE, no DELETE). Retained permanently — no archival, no purging.
**Rationale**: Regulatory and business requirement. Minimum 5-year retention but no mechanism to delete after that.
**Implementation**: No DELETE/UPDATE endpoints. Database-level could use REVOKE UPDATE, DELETE on audit_logs table in production.

## R10: AOP vs Manual Logging

**Decision**: Use Spring AOP (`@Aspect`) for cross-cutting audit log creation with an `@Auditable` annotation.
**Rationale**: Avoids scattering audit log calls throughout service methods. Clean separation of concerns. AOP directory already exists in project structure.
**Alternatives considered**: Manual service calls in each method (rejected — violates DRY, easy to forget), Hibernate Envers (rejected — overkill, limited flexibility for diff-only and description format), JPA Entity Listeners (rejected — limited access to HTTP context like IP address and actor).
