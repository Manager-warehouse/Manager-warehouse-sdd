# Research: System Audit Logging

## Decision: Extend existing audit module

**Decision**: Use the current `AuditLogController`, `AuditLogService`, `AuditLogRepository`, `AuditLog` entity, DTOs, and `AuditAction.java` enum.

**Rationale**: The codebase already has the layered audit infrastructure, tests, OpenAPI annotations, pagination response DTO, and immutable database trigger. Extending this module keeps the implementation small and consistent with existing services that already call `AuditLogService`.

**Alternatives considered**: Introduce a new event/audit abstraction. Rejected for Sprint 1 because the existing module already covers synchronous transactional audit logging and adding a parallel system would increase migration and adoption risk.

## Decision: Canonical action list comes from `AuditAction.java`

**Decision**: Treat backend `AuditAction.java` as the canonical list of auditable actions and keep the DB check constraint aligned with it.

**Rationale**: Domain services already compile against `AuditAction`; using it as the source of truth avoids conflicts between specs, migrations, and service code.

**Alternatives considered**: Maintain a shorter generic-only action list in the spec. Rejected because many implemented WMS flows already use domain-specific audit actions such as receipt, delivery, transfer, stocktake, and quarantine actions.

## Decision: Read-only views and exports are not audited in Sprint 1

**Decision**: Do not create audit rows for read-only view, search, filter, dashboard, report, or export actions.

**Rationale**: The project needs audit trail for business mutations and auth state changes, not high-volume read telemetry. Skipping read-only audit avoids log growth and simplifies acceptance tests.

**Alternatives considered**: Audit sensitive finance/report/dashboard views with `VIEW_REPORT`. Rejected by product decision for Sprint 1.

## Decision: Nullable entity references for non-entity actions

**Decision**: Allow `audit_logs.entity_type` and `audit_logs.entity_id` to be null when an audit action has no natural affected entity row.

**Rationale**: Login/logout and similar non-entity events do not always map cleanly to a business entity. Nullable references prevent fake entity IDs while preserving actor/action/timestamp evidence.

**Alternatives considered**: Use actor user id as `entity_id` for all non-entity actions. Rejected because it conflates actor and affected entity semantics.

## Decision: One aggregated changed-field payload per affected entity

**Decision**: Each audit row summarizes all changed fields for the affected entity in `old_value` and `new_value`.

**Rationale**: The existing service method accepts one entity type/id plus old/new maps. Aggregating fields keeps audit rows readable and avoids row explosion.

**Alternatives considered**: One audit row per changed field. Rejected because it increases log volume and makes business transaction reconstruction harder.

## Decision: Sensitive field names remain, values are omitted

**Decision**: When sensitive fields change, keep the field name in the changed-field payload and omit before/after values.

**Rationale**: This preserves evidence that a password/token/secret-related change happened without leaking sensitive data.

**Alternatives considered**: Omit sensitive fields entirely. Rejected because it hides the fact that the sensitive field changed. Masking values was also rejected because even masked secrets can create unnecessary handling risk.

## Decision: Fixed pagination and bounded unfiltered browsing

**Decision**: Return 30 rows per page, allow at most 50 unfiltered pages, and require a time filter for older records. Valid time ranges must be at least 1 hour and expand in exact 1-hour increments.

**Rationale**: The newest 1,500 rows are enough for normal admin browsing and preserve predictable query size. Time filters keep historical queries bounded and index-friendly.

**Alternatives considered**: Allow arbitrary page size or unrestricted unfiltered browsing. Rejected for performance and operational predictability.
