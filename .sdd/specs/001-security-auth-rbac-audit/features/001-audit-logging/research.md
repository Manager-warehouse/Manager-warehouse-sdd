# Research: System Audit Logging

**Feature**: System Audit Logging
**Date**: 2026-06-03
**Status**: Complete

---

## R1: Audit Log Scope

**Decision**: Audit every user-driven action that changes data or state in the system.

**Rationale**: The audit log is a system-wide trail for operational accountability. It includes authentication state changes such as login/logout and domain changes across RBAC, configuration, master data, warehouse operations, finance, returns, scrap, and disposal.

**Alternatives considered**:
- Warehouse-only audit: rejected because user requested audit for all changes in the system.
- Include read/export events: rejected because user explicitly said view and export actions do not need audit entries.

## R2: Changed Field Storage

**Decision**: Store field-level diffs only in `old_value` and `new_value`.

**Rationale**: Diff-only JSONB is smaller and easier for the frontend detail view to render as a before/after table.

**Alternatives considered**:
- Full object snapshots: rejected because unchanged fields add noise and storage cost.
- Single `changes` object: rejected because the existing schema already has `old_value` and `new_value`.

## R3: Sensitive Data Handling

**Decision**: Sensitive fields are omitted from audit logs.

**Rationale**: Audit logs are widely useful for investigation and must never become a secondary store for credentials or secrets.

**Sensitive examples**: `password`, `passwordHash`, `password_hash`, `accessToken`, `refreshToken`, `jwt`, `secret`, `apiKey`, `credentials`.

## R4: Actor Model

**Decision**: Every audit log entry references the authenticated user as `actor_id`.

**Rationale**: The user confirmed the system does not perform autonomous business actions. No background/system actor is needed for this feature.

**Alternatives considered**:
- Nullable actor for system jobs: rejected as unnecessary for current WMS behavior.

## R5: View Access

**Decision**: Only System Admin (`ADMIN`) can view audit logs. CEO cannot view audit logs.

**Rationale**: User explicitly clarified that CEO should not see logs. This also matches the current frontend protected route for `UserManagement`, which allows only `ROLES.ADMIN`.

**Alternatives considered**:
- CEO and Admin full access: rejected by user clarification.
- Warehouse Manager scoped access: rejected because audit log viewing is Admin-only.

## R6: Frontend Placement

**Decision**: Keep audit log UI as the existing Audit Trail tab inside `UserManagement`.

**Rationale**: The frontend already has a table-based audit tab in `frontend/src/pages/Admin/UserManagement.jsx`; user requested not to redesign it as a separate page.

**Alternatives considered**:
- New `/admin/audit-logs` route: rejected because user wants to follow existing frontend structure.

## R7: Pagination Strategy

**Decision**: Use page-based pagination: 30 logs per page, newest first, up to 50 unfiltered pages.

**Rationale**: User specified 30 logs per page, newest first, and a default 50-page browsing window. Page-based controls fit the existing table UI better than cursor-only infinite loading.

**Rule**: Requests beyond page 50 require at least one narrowing filter, such as time range or warehouse.

**Alternatives considered**:
- Cursor pagination: rejected for this feature because the frontend requirement is page navigation with a default page window.

## R8: Audit Detail View

**Decision**: Add a detail modal/drawer from the audit table row.

**Rationale**: The table should stay scan-friendly while the detail view shows field-level before/after values without overcrowding columns.

**UI content**: timestamp, actor, role, action, entity, warehouse, description, IP address, and changed fields rendered as before/after rows.

## R9: Singleton Logging Object

**Decision**: Implement logging through a Spring singleton `AuditLogService`.

**Rationale**: Spring `@Service` beans are singleton by default and comply with the required Controller -> Service -> Repository -> Entity architecture. This avoids static global state while giving all business services one consistent logging entry point.

**Alternatives considered**:
- Static singleton/global logger: rejected because it is harder to test and bypasses dependency injection.
- Hibernate Envers: rejected because the system needs custom action categories, sensitive-field omission, and frontend-friendly diff-only data.
- AOP-only logging: deferred because service-level calls provide clearer control over domain timing and before/after values in Sprint 1.

## R10: Immutability

**Decision**: Audit logs are append-only and immutable at database and application levels.

**Rationale**: The feature requires that no one, including System Admin, can edit or delete existing log entries.

**Implementation direction**: No update/delete endpoints, repository methods only for insert/read, and DB trigger preventing `UPDATE`/`DELETE` on `audit_logs`.
