# Tasks: System Audit Logging

**Input**: Design documents from `.sdd/specs/001-security-auth-rbac-audit/features/001-audit-logging/`

**Prerequisites**: `feature-system-audit-logging.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/audit-logs.openapi.yaml`, `quickstart.md`

**Tests**: Tests are REQUIRED for audit service rules, API endpoints, and frontend audit UI behavior touched by this feature.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it touches different files and has no dependency on the paired task.
- **[Story]**: User story label such as US1, US2, US3.
- Include exact paths.
- Mention audit, validation, authorization, OpenAPI, and tests when relevant.

## Path Conventions

- Backend code: `backend/src/main/java/com/wms/`
- Backend tests: `backend/src/test/java/com/wms/`
- Flyway migrations: `backend/src/main/resources/db/migration/`
- Frontend code: `frontend/src/`
- Feature docs: `.sdd/specs/001-security-auth-rbac-audit/features/001-audit-logging/`

## Phase 1: Setup & Design Alignment

**Purpose**: Confirm the task slice matches the clarified Sprint 1 audit logging scope.

- [X] T001 Read `.specify/memory/constitution.md`, `AGENTS.md`, and `.sdd/specs/001-security-auth-rbac-audit/features/001-audit-logging/feature-system-audit-logging.md`
- [X] T002 Read `.sdd/specs/001-security-auth-rbac-audit/features/001-audit-logging/plan.md`, `.sdd/specs/001-security-auth-rbac-audit/features/001-audit-logging/research.md`, and `.sdd/specs/001-security-auth-rbac-audit/features/001-audit-logging/data-model.md`
- [X] T003 [P] Review existing audit backend files `backend/src/main/java/com/wms/entity/AuditLog.java`, `backend/src/main/java/com/wms/service/AuditLogService.java`, `backend/src/main/java/com/wms/controller/AuditLogController.java`, and `backend/src/main/java/com/wms/util/AuditLogUtil.java`
- [X] T004 [P] Review existing audit tests in `backend/src/test/java/com/wms/service/AuditLogServiceTest.java`, `backend/src/test/java/com/wms/controller/AuditLogControllerTest.java`, and `backend/src/test/java/com/wms/util/AuditLogUtilTest.java`
- [X] T005 [P] Verify latest Flyway migration version under `backend/src/main/resources/db/migration/` and reserve `V18__audit_log_nullable_entity_refs.sql` if no newer migration exists
- [X] T006 [P] Review frontend audit files `frontend/src/pages/Admin/AuditLogs.jsx` and `frontend/src/services/admin.service.js`

---

## Phase 2: Foundational Backend

**Purpose**: Shared schema and audit utility behavior that blocks all user stories.

- [X] T007 Add Flyway migration `backend/src/main/resources/db/migration/V18__audit_log_nullable_entity_refs.sql` to drop NOT NULL from `audit_logs.entity_type` and `audit_logs.entity_id` while preserving audit immutability trigger
- [X] T008 Update the audit action check constraint in `backend/src/main/resources/db/migration/V18__audit_log_nullable_entity_refs.sql` only if current migration history lacks any value present in `backend/src/main/java/com/wms/enums/AuditAction.java`
- [X] T009 Update JPA nullability for `entityType` and `entityId` in `backend/src/main/java/com/wms/entity/AuditLog.java`
- [X] T010 Update `AuditLogUtil.filterSensitiveFields` in `backend/src/main/java/com/wms/util/AuditLogUtil.java` to keep sensitive field names while omitting before/after values
- [X] T011 Update `AuditLogUtil.generateDescription` in `backend/src/main/java/com/wms/util/AuditLogUtil.java` to produce a valid description when `entityType` or entity code is null
- [X] T012 Update JSON helpers in `backend/src/main/java/com/wms/util/AuditLogUtil.java` so empty changed-field payloads serialize consistently for no-field-change actions
- [X] T013 Update OpenAPI contract `.sdd/specs/001-security-auth-rbac-audit/features/001-audit-logging/contracts/audit-logs.openapi.yaml` if task decisions change parameter names, nullable fields, or error behavior during implementation

**Checkpoint**: Audit schema and helper behavior support nullable entity references, canonical actions, and sensitive field-name retention.

---

## Phase 3: User Story 1 - Record Audited Mutations and Auth State Changes (Priority: P1)

**Goal**: Successful audited mutations and login/logout-like auth state changes create immutable audit rows with actor, role, action, timestamp, optional entity reference, warehouse context, and sanitized changed fields.

**Independent Test**: Call `AuditLogService.log` for an entity mutation and a non-entity event; verify saved rows have required actor/action/timestamp data, nullable entity references where applicable, empty old/new payloads where no fields changed, and sensitive field names without sensitive values.

### Tests for User Story 1

- [X] T014 [P] [US1] Add service unit test for nullable `entityType` and `entityId` non-entity audit events in `backend/src/test/java/com/wms/service/AuditLogServiceTest.java`
- [X] T015 [P] [US1] Add service unit test for empty old/new changed-field payloads on no-field-change actions in `backend/src/test/java/com/wms/service/AuditLogServiceTest.java`
- [X] T016 [P] [US1] Add utility unit tests proving sensitive field names are retained and sensitive before/after values are omitted in `backend/src/test/java/com/wms/util/AuditLogUtilTest.java`
- [X] T017 [P] [US1] Add service unit test proving `AuditAction.java` values used by existing warehouse/accounting services can be persisted by `AuditLogService` in `backend/src/test/java/com/wms/service/AuditLogServiceTest.java`

### Implementation for User Story 1

- [X] T018 [US1] Update `AuditLogService.saveAuditLog` in `backend/src/main/java/com/wms/service/AuditLogService.java` to accept nullable entity reference fields without throwing or generating invalid descriptions
- [X] T019 [US1] Update `AuditLogService.log` overloads in `backend/src/main/java/com/wms/service/AuditLogService.java` to normalize null old/new maps to the agreed empty changed-field representation
- [X] T020 [US1] Confirm existing auth and mutation services use canonical values from `backend/src/main/java/com/wms/enums/AuditAction.java` without adding read-only `VIEW_REPORT` calls
- [X] T021 [US1] Ensure `AuditLogDetailResponse.from` and `AuditLogListItemResponse.from` in `backend/src/main/java/com/wms/dto/response/` return nullable entity references without display-side exceptions

**Checkpoint**: US1 works independently and all US1 tests pass.

---

## Phase 4: User Story 2 - System Admin Queries Audit Logs (Priority: P1)

**Goal**: System Admin can query audit logs newest-first with fixed 30-row pages, page 1 default, page 50 unfiltered limit, date/time and warehouse filters only, and detail access by id.

**Independent Test**: As ADMIN, call list and detail endpoints with default, page 2, page 51 without filter, valid 1-hour time filter, invalid time ranges, and warehouse filter; verify response metadata and error codes.

### Tests for User Story 2

- [X] T022 [P] [US2] Add service unit tests for fixed page size 30 and unfiltered page 51 rejection in `backend/src/test/java/com/wms/service/AuditLogServiceTest.java`
- [X] T023 [P] [US2] Add service unit tests for valid 1-hour time ranges and invalid shorter/non-increment ranges in `backend/src/test/java/com/wms/service/AuditLogServiceTest.java`
- [X] T024 [P] [US2] Add service unit tests for warehouse filter and timestamp descending sort in `backend/src/test/java/com/wms/service/AuditLogServiceTest.java`
- [X] T025 [P] [US2] Add controller tests for ADMIN list/detail happy paths in `backend/src/test/java/com/wms/controller/AuditLogControllerTest.java`
- [X] T026 [P] [US2] Add controller tests for CEO/non-admin `FORBIDDEN_AUDIT_ACCESS`, missing id `AUDIT_LOG_NOT_FOUND`, invalid date `INVALID_DATE_RANGE`, and page limit `QUERY_RANGE_TOO_LARGE` in `backend/src/test/java/com/wms/controller/AuditLogControllerTest.java`

### Implementation for User Story 2

- [X] T027 [US2] Update `AuditLogService.resolvePageSize` in `backend/src/main/java/com/wms/service/AuditLogService.java` to always return the fixed page size of 30
- [X] T028 [US2] Update date range validation in `backend/src/main/java/com/wms/service/AuditLogService.java` to require `from <= to`, minimum 1-hour duration when both bounds exist, and exact 1-hour increments
- [X] T029 [US2] Update `AuditLogService.getAuditLogs` in `backend/src/main/java/com/wms/service/AuditLogService.java` so page > 50 is rejected only when no time filter is supplied
- [X] T030 [US2] Update `AuditLogController.getAuditLogs` in `backend/src/main/java/com/wms/controller/AuditLogController.java` to accept the contract parameter `warehouse_id` and preserve `warehouseId` backward compatibility only if current frontend needs it
- [X] T031 [US2] Update Swagger annotations in `backend/src/main/java/com/wms/controller/AuditLogController.java` to document fixed page size, `warehouse_id`, nullable entity refs, and audit error codes
- [X] T032 [US2] Verify centralized error responses map `FORBIDDEN_AUDIT_ACCESS`, `AUDIT_LOG_NOT_FOUND`, `INVALID_DATE_RANGE`, and `QUERY_RANGE_TOO_LARGE` consistently in `backend/src/main/java/com/wms/exception/`

**Checkpoint**: US2 works independently and all US2 tests pass.

---

## Phase 5: User Story 3 - Audit Log UI Uses Server Pagination and Approved Filters (Priority: P2)

**Goal**: The Admin audit log screen displays backend pages directly, uses only warehouse and date/time filters, handles nullable entity references, and does not expose actor/action/entity local filters as Sprint 1 query features.

**Independent Test**: In the frontend, change audit pages and filters; verify the UI sends backend requests with `page`, fixed `pageSize = 30`, `from`, `to`, and `warehouse_id`, displays nullable entity values cleanly, and does not locally filter by actor/action/entity.

### Tests for User Story 3

- [ ] T033 [P] [US3] Add frontend test for server-side page navigation in `frontend/src/pages/Admin/AuditLogs.test.jsx`
- [X] T034 [P] [US3] Add frontend test proving only date/time and warehouse filters are sent by `frontend/src/services/admin.service.js`
- [ ] T035 [P] [US3] Add frontend test for nullable `entityType` and `entityId` display in `frontend/src/pages/Admin/AuditLogs.test.jsx`

### Implementation for User Story 3

- [X] T036 [US3] Update `frontend/src/services/admin.service.js` to send `warehouse_id` instead of unsupported query filters and keep mock pagination fixed at 30 rows
- [X] T037 [US3] Remove local actor/action/entity filtering state and controls from `frontend/src/pages/Admin/AuditLogs.jsx`
- [X] T038 [US3] Update date inputs in `frontend/src/pages/Admin/AuditLogs.jsx` to send a valid time range or show client-side validation aligned with the 1-hour backend rule
- [X] T039 [US3] Update table/card/detail rendering in `frontend/src/pages/Admin/AuditLogs.jsx` to show `-` when `entityType` or `entityId` is null
- [X] T040 [US3] Update pagination controls in `frontend/src/pages/Admin/AuditLogs.jsx` to keep page size fixed at 30 and request every page from the backend

**Checkpoint**: US3 works independently after US2 API behavior is available.

---

## Phase 6: Cross-Cutting Verification

- [X] T041 Update implementation notes in `.sdd/specs/001-security-auth-rbac-audit/features/001-audit-logging/integration-guide.md` to reflect nullable entity refs, sensitive field-name retention, and no read-only view audit
- [X] T042 Verify `backend/src/main/resources/db/migration/V18__audit_log_nullable_entity_refs.sql` does not delete or rewrite applied migration files
- [X] T043 Run `mvn -f backend/pom.xml test -Dtest=AuditLogServiceTest,AuditLogControllerTest,AuditLogUtilTest`
- [X] T044 Run `mvn -f backend/pom.xml compile`
- [X] T045 Run frontend Jest tests for audit UI if `frontend/src/pages/Admin/AuditLogs.jsx` or `frontend/src/services/admin.service.js` changed
- [X] T046 Verify no `System.out`, production `console.log`, hardcoded secrets, raw SQL in application code, or TODO comments remain in touched files under `backend/src/main/java/com/wms/` and `frontend/src/`
- [X] T047 Verify OpenAPI/Swagger annotations and `.sdd/specs/001-security-auth-rbac-audit/features/001-audit-logging/contracts/audit-logs.openapi.yaml` match implemented API parameters and nullable response fields
- [X] T048 Verify read-only list/detail/report/dashboard/export flows do not call `AuditLogService` for view-only actions
- [X] T049 Verify existing warehouse mutation flows still create audit logs and preserve inventory/QC/transfer/accounting invariants when touched

## Dependency Rules

- Phase 1 must complete before schema/code changes.
- Phase 2 blocks US1 and US2 because nullable entity references and helper behavior affect both write and read paths.
- US1 and US2 can proceed in parallel after Phase 2 if different developers split service write behavior from query/API behavior.
- US3 depends on US2 API parameter and pagination behavior.
- Cross-cutting verification runs after all implemented story phases.

## Parallel Execution Examples

```text
# Phase 1 parallel review
T003, T004, T005, T006

# US1 tests can start together after Phase 2
T014, T015, T016, T017

# US2 tests can start together after Phase 2
T022, T023, T024, T025, T026

# US3 frontend tests can start together after US2 contract is stable
T033, T034, T035
```

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Deliver US1 and US2 backend behavior with tests.
3. Run backend audit tests and compile.

### Incremental Delivery

1. Add US3 frontend alignment after backend API behavior is stable.
2. Update docs/contracts after implementation confirms exact parameter compatibility.
3. Finish cross-cutting verification and only then mark the feature ready for implementation review.
