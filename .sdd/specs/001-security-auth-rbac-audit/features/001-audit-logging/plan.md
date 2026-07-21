# Implementation Plan: System Audit Logging

**Branch**: `fix/audit_log` | **Date**: 2026-07-22 | **Spec**: [feature-system-audit-logging.md](./feature-system-audit-logging.md)

**Input**: Feature specification from `.sdd/specs/001-security-auth-rbac-audit/features/001-audit-logging/feature-system-audit-logging.md`

## Summary

Finalize Sprint 1 system audit logging so all authenticated mutation and authentication state-changing actions create immutable audit records, while read-only views/searches/exports do not create audit entries. The implementation tightens the existing audit module around canonical `AuditAction.java` values, nullable entity references for non-entity events, fixed 30-row pagination, unfiltered browsing limited to the newest 1,500 rows, System Admin-only query access, and date/warehouse filtering.

## Technical Context

**Language/Version**: Java 21 / Spring Boot 3.4.5; React 18 + JavaScript for audit log UI behavior if touched

**Primary Dependencies**: Spring Web, Spring Data JPA, Hibernate, Spring Security, Lombok, Springdoc OpenAPI, Jackson JSON support, React, Tailwind CSS

**Storage**: PostgreSQL 18 via Flyway migrations and Spring Data JPA

**Testing**: JUnit 5 + Mockito for service behavior; Spring MVC/API integration tests for endpoints; Jest + React Testing Library if frontend audit log pagination/filter UI is touched

**Target Platform**: Full-stack WMS web application and REST API

**Project Type**: Backend + frontend web application

**Performance Goals**: Audit log list queries for the newest 1,500 entries or time/warehouse-filtered ranges should return within p95 <= 2s under Sprint 1 scale; audit writes must run inside the business transaction without changing inventory/business state semantics.

**Constraints**: No raw SQL in application code; audit logs are append-only; `actor_id`, `actor_role`, `action`, `timestamp`, and description are required; `entity_type` and `entity_id` are nullable only for actions without a natural affected entity row; sensitive values are omitted while field names remain visible; read-only view/search/filter/export actions are not audited.

**Scale/Scope**: 3 physical warehouses, 1000+ products, 50+ dealers, 1000+ transactions/month, newest 1,500 audit rows browsable without filters.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Layered architecture preserved: Controller -> Service -> Repository -> Entity.
- [x] Write endpoints use request DTOs with Jakarta Validation. This feature adds no write endpoint; it validates query params for read endpoints and relies on existing write DTOs for audited mutations.
- [x] Service methods own business rules, transactions, authorization, and audit logging.
- [x] All DB access goes through Spring Data JPA/Hibernate; no raw SQL in application code.
- [x] Inventory invariants preserved if touched: this feature does not mutate inventory; existing warehouse mutation services keep their invariants and call audit logging.
- [x] QC/quarantine/transfer/accounting state rules listed when touched: no new state transition is introduced; audit entries mirror those flows.
- [x] Audit action, entity type, before/after payload, and warehouse scope identified.
- [x] OpenAPI/Swagger impact identified for every new or changed endpoint.
- [x] Flyway migration impact identified; add a new migration to relax `audit_logs.entity_type` and `audit_logs.entity_id` nullability if the current runnable schema still has NOT NULL constraints.
- [x] Unit and integration test strategy covers happy path and error paths.

## Domain Impact

**Actors/Roles**: System Admin can query audit logs. All authenticated users can be audit actors when performing audited mutations or login/logout. CEO is audit actor only and cannot query audit logs.

**State Changes**: No WMS business state transition is introduced. Audit rows are inserted for successful authenticated mutation/authentication state-changing actions.

**Inventory Impact**: None. Inventory-affecting services continue to enforce stock/QC/transfer invariants and append audit entries as a side effect of successful transactions.

**Audit Actions**: `AuditAction.java` is canonical. Do not add `VIEW_REPORT` work for read-only reports in Sprint 1. Each audit entry stores a single aggregated changed-field payload for the affected entity. `old_value`/`new_value` are empty for non-mutation actions such as `LOGIN`/`LOGOUT`.

**Security/Authorization**: JWT authentication required. `GET /api/v1/admin/audit-logs` and `GET /api/v1/admin/audit-logs/{id}` require `ADMIN`; non-admin users receive `FORBIDDEN_AUDIT_ACCESS`.

**Accounting Impact**: None directly. Accounting/finance mutations that already use `AuditAction.java` remain audited; read-only finance reports and exports are not audited.

## Data Model / Migration Impact

- Entities/tables touched: `audit_logs`, `users`, `warehouses` references only.
- New/changed columns or constraints:
  - `audit_logs.entity_type` nullable for non-entity actions.
  - `audit_logs.entity_id` nullable for non-entity actions.
  - `audit_logs.action` check constraint must remain aligned with backend `AuditAction.java`.
  - Existing immutability trigger remains unchanged.
- Flyway plan: add a new migration after the latest runnable version to drop NOT NULL constraints on `entity_type` and `entity_id`; update action check only if current DB constraint is missing enum values present in `AuditAction.java`.
- Backfill/seed data: none. Existing rows keep their entity values.

## API / Contract Impact

- Endpoints added/changed:
  - `GET /api/v1/admin/audit-logs`
  - `GET /api/v1/admin/audit-logs/{id}`
- Request DTOs: none; query params only.
  - `page`: 1-based, defaults to 1.
  - `pageSize`: ignored/clamped to fixed 30.
  - `from`, `to`: ISO date/datetime; if both present, `to - from` must be at least 1 hour and an exact 1-hour increment.
  - `warehouse_id`: optional warehouse filter. Keep backward compatibility with existing `warehouseId` only if needed by current UI.
- Response DTOs:
  - `AuditLogPageResponse`: `data`, `page`, `pageSize`, `totalItems`, `totalPages`, `hasNext`, `hasPrevious`, `requiresFilterForOlder`.
  - `AuditLogListItemResponse`: nullable `entityType`, nullable `entityId`, nullable warehouse display fields.
  - `AuditLogDetailResponse`: nullable `entityType`, nullable `entityId`, parsed `oldValue`, parsed `newValue`, `ipAddress`.
- Error codes/statuses:
  - 400 `INVALID_DATE_RANGE`
  - 400 `QUERY_RANGE_TOO_LARGE`
  - 400 `VALIDATION_ERROR`
  - 403 `FORBIDDEN_AUDIT_ACCESS`
  - 404 `AUDIT_LOG_NOT_FOUND`
- OpenAPI path/schema updates: document nullable entity refs, fixed page size, `warehouse_id`, date range validation, and non-admin access denial.

## Test Strategy

- Service unit tests:
  - Creates audit entry with canonical action, actor snapshot, warehouse, and changed-field payload.
  - Allows null `entityType`/`entityId` for non-entity actions.
  - Keeps sensitive field names and omits before/after values.
  - Returns empty old/new values for no-field-change events.
  - Rejects page > 50 without date filter.
  - Accepts page > 50 with valid time filter.
  - Rejects invalid date order and non-1-hour-increment ranges.
- Repository/query tests:
  - Timestamp descending order.
  - Warehouse filter.
  - Time range filter.
- Controller/API integration tests:
  - Admin list/detail happy paths.
  - CEO/non-admin receives `FORBIDDEN_AUDIT_ACCESS`.
  - Missing row returns `AUDIT_LOG_NOT_FOUND`.
  - Query validation error paths.
- Frontend tests:
  - Audit UI requests backend page changes instead of slicing loaded data locally if the UI is touched.
  - Filter UI sends only date/time and warehouse filters.
- Regression tests for invariants:
  - Audit log immutability trigger remains active.
  - Read-only list/report/dashboard/export calls do not create audit entries.
  - Existing warehouse mutation services still create audit entries.

## Project Structure

### Documentation

```text
.sdd/specs/001-security-auth-rbac-audit/features/001-audit-logging/
├── feature-system-audit-logging.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── integration-guide.md
└── contracts/
    └── audit-logs.openapi.yaml
```

### Source Code

```text
backend/src/main/java/com/wms/
├── controller/AuditLogController.java
├── dto/response/AuditLogDetailResponse.java
├── dto/response/AuditLogListItemResponse.java
├── dto/response/AuditLogPageResponse.java
├── entity/AuditLog.java
├── enums/AuditAction.java
├── repository/AuditLogRepository.java
├── service/AuditLogService.java
└── util/AuditLogUtil.java

backend/src/main/resources/db/migration/
└── V[latest+1]__audit_log_nullable_entity_refs.sql

backend/src/test/java/com/wms/
├── controller/AuditLogControllerTest.java
├── service/AuditLogServiceTest.java
└── util/AuditLogUtilTest.java
```

**Structure Decision**: Extend the existing audit module in place; do not introduce a second audit abstraction or a read-view audit subsystem.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |

## Post-Design Constitution Check

- [x] Layered architecture preserved.
- [x] No raw SQL in application code; schema changes isolated to Flyway.
- [x] Audit immutability retained by database trigger and no update/delete service API.
- [x] Authorization, OpenAPI, migration, and test impacts are explicit.
- [x] No unresolved clarification remains for Sprint 1 planning.
