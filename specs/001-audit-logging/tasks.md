# Tasks: System Audit Logging

**Input**: `specs/001-audit-logging/plan.md`, `research.md`, `data-model.md`, `contracts/audit-log-api.md`

**Status**: Regenerated to align with Admin-only access, existing `UserManagement` audit tab, page-based pagination, and detail view.

## Phase 1: Backend Foundation

- [X] T001 Update `backend/src/main/java/com/wms/enums/AuditAction.java` to match DB enum values: `LOGIN`, `LOGOUT`, `CREATE`, `UPDATE`, `STATUS_CHANGE`, `APPROVE`, `REJECT`, `CANCEL`, `SOFT_DELETE`, `ASSIGN`, `UNASSIGN`.
- [X] T002 Verify `backend/src/main/java/com/wms/entity/AuditLog.java` maps `description`, `warehouse`, `oldValue`, `newValue`, `timestamp`, and `ipAddress` to the current `audit_logs` schema.
- [X] T003 Create/adjust DTOs: `AuditLogListItemResponse`, `AuditLogDetailResponse`, `AuditLogPageResponse`.
- [X] T004 Create/adjust `AuditLogRepository` for read queries sorted by `timestamp DESC`, optional time filter, optional warehouse filter, and detail lookup by ID.

## Phase 2: Backend Service and API

- [X] T005 Implement `AuditLogService` as a Spring singleton `@Service`, using constructor injection and no static global state.
- [X] T006 Implement `AuditLogService.log(...)` to resolve actor from `SecurityContext`, omit sensitive fields, store field-level diffs, and save append-only rows.
- [X] T007 Implement `AuditLogService.getAuditLogs(page, pageSize, from, to, warehouseId)` with 30-row page size and 50-page unfiltered browsing limit.
- [X] T008 Implement `AuditLogService.getAuditLogById(id)` for detail modal/drawer.
- [X] T009 Implement `AuditLogController` endpoints: `GET /api/v1/audit-logs` and `GET /api/v1/audit-logs/{id}`.
- [X] T010 Enforce `ADMIN`-only access; CEO and all other roles must receive `FORBIDDEN_AUDIT_ACCESS`.

## Phase 3: Frontend Existing Tab

- [X] T011 Keep audit log inside `frontend/src/pages/Admin/UserManagement.jsx`; do not create a standalone route.
- [X] T012 Update `frontend/src/services/admin.service.js` so `getAuditLogs` accepts `{ page, pageSize, from, to, warehouseId }`.
- [X] T013 Add `getAuditLogById(id)` to `admin.service.js`.
- [X] T014 Add page controls to the existing audit table: previous/next, current page, disabled next when page 50 is reached without filters.
- [X] T015 Add optional time range and warehouse filters to the audit tab.
- [X] T016 Add detail modal/drawer that renders metadata and changed field before/after values.
- [X] T017 Keep action badge display generic so DB enum values such as `CREATE`, `UPDATE`, `SOFT_DELETE`, and `LOGIN` render without relying on mock strings like `USER_CREATED`.

## Phase 4: Tests

- [X] T018 Unit test `AuditLogService.log(...)` for actor resolution, sensitive-field omission, changed-field persistence, and null warehouse support.
- [X] T019 Unit test `AuditLogService.getAuditLogs(...)` for default page size, newest-first order, filter behavior, and page 51 rejection without filters.
- [X] T020 Integration test `GET /api/v1/audit-logs` for Admin success and CEO/non-admin forbidden.
- [X] T021 Integration test `GET /api/v1/audit-logs/{id}` for Admin success, forbidden access, and `AUDIT_LOG_NOT_FOUND`.
- [ ] T022 Frontend test or manual verification for pagination, filters, and detail view in the existing User Management Audit Trail tab.

## Phase 5: Documentation and Verification

- [X] T023 Update Swagger/OpenAPI for both audit log endpoints.
- [ ] T024 Run backend tests with Maven.
- [X] T025 Run frontend tests/build if frontend files are changed.
- [X] T026 Verify no update/delete API path exists for audit logs.
