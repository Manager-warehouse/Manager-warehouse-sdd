# Implementation Plan: System Admin, RBAC & Audit Alignment

**Branch**: `feat/accountant-supplier-invoicing` | **Date**: 2026-07-23 | **Spec**: [.sdd/specs/001-security-auth-rbac-audit/spec.md](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/spec.md)

**Input**: Feature specification from `.sdd/specs/001-security-auth-rbac-audit/spec.md` and role matrix from `.sdd/role.md`

## Summary

Lập kế hoạch thiết kế và đồng bộ phân quyền RBAC, vai trò hệ thống, và Nhật ký hoạt động Audit Log trên 3 kho vật lý. Đảm bảo 10 vai trò chuẩn (`ADMIN`, `CEO`, `WH_MANAGER`, `ACCT_MANAGER`, `PLANNER`, `DISPATCHER`, `STOREKEEPER`, `WH_STAFF`, `ACCOUNTANT`, `DRIVER`) tuân thủ đúng ma trận phân quyền màn hình và kiểm soát truy cập API theo phạm vi kho được gán.

## Technical Context

**Language/Version**: Java 21 / Spring Boot 3.4.5; React 18 + JavaScript

**Primary Dependencies**: Spring Web, Spring Data JPA, Hibernate, Spring Security, Lombok, Springdoc OpenAPI, React, Tailwind CSS

**Storage**: PostgreSQL 18 via Flyway migrations and Spring Data JPA

**Testing**: JUnit 5 + Mockito for backend; Spring integration tests for APIs; Jest + React Testing Library for frontend business UI

**Target Platform**: Full-stack WMS web application and REST API

**Project Type**: Backend + frontend web application

**Performance Goals**: Auth check overhead <= 10ms, Auth response p95 <= 500ms, Audit log query <= 2s

**Constraints**: Must preserve WMS invariants: no negative inventory, QC gates, audit logs, role + warehouse authorization, no raw SQL in application code

**Scale/Scope**: 3 physical warehouses, 10 canonical roles, 1000+ products, 50+ dealers, 1000+ transactions/month

## Constitution Check

*GATE: Passed Phase 0 research & Phase 1 design.*

- [x] Layered architecture preserved: Controller -> Service -> Repository -> Entity.
- [x] Write endpoints use request DTOs with Jakarta Validation.
- [x] Service methods own business rules, transactions, authorization, and audit logging.
- [x] All DB access goes through Spring Data JPA/Hibernate; no raw SQL in application code.
- [x] Inventory invariants preserved if touched: `total_qty >= 0`, `reserved_qty >= 0`, `available = total_qty - reserved_qty >= 0`, `@Version`.
- [x] QC/quarantine/transfer/accounting state rules listed when touched.
- [x] Audit action, entity type, before/after payload, and warehouse scope identified.
- [x] OpenAPI/Swagger impact identified for every new or changed endpoint.
- [x] Flyway migration impact identified; no duplicate migration version in runnable history.
- [x] Unit and integration test strategy covers happy path and error paths.

## Domain Impact

**Actors/Roles**: 10 canonical roles (`ADMIN`, `CEO`, `WH_MANAGER`, `ACCT_MANAGER`, `PLANNER`, `DISPATCHER`, `STOREKEEPER`, `WH_STAFF`, `ACCOUNTANT`, `DRIVER`)

**State Changes**: User active/inactive status, password updates, warehouse assignment mapping

**Inventory Impact**: Read-only cross-warehouse visibility for `WH_MANAGER` on available stock; physical mutations scoped to assigned warehouse

**Audit Actions**: `USER_CREATE`, `USER_UPDATE`, `USER_ROLE_CHANGE`, `WAREHOUSE_ASSIGN`, `SYSTEM_CONFIG_UPDATE`

**Security/Authorization**: Centralized `RoleHierarchy` for `ADMIN`; JWT role + warehouse scope validation for operations

**Accounting Impact**: Scoped access for `ACCOUNTANT` and `ACCT_MANAGER` (Maker-Checker pricing & credit notes)

## Data Model / Migration Impact

- Entities/tables touched: [list]
- New/changed columns or constraints: [list]
- Flyway plan: [new migration / no migration / pre-shared cleanup]
- Backfill/seed data: [none or list]

## API / Contract Impact

- Endpoints added/changed: [list]
- Request DTOs: [list validation rules]
- Response DTOs: [list]
- Error codes/statuses: [400/401/403/404/409/422/500 as applicable]
- OpenAPI path/schema updates: [list]

## Test Strategy

- Service unit tests: [business rules and edge cases]
- Repository/query tests: [if needed]
- Controller/API integration tests: [happy + error paths]
- Frontend tests: [if UI business logic touched]
- Regression tests for invariants: [inventory/QC/audit/auth/accounting]

## Project Structure

### Documentation

```text
.sdd/specs/[###-feature]/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

### Source Code

```text
backend/src/main/java/com/wms/
├── controller/
├── dto/request/
├── dto/response/
├── entity/
├── enums/
├── exception/
├── mapper/
├── repository/
├── service/
└── service/impl/

backend/src/main/resources/db/migration/

backend/src/test/java/com/wms/

frontend/src/
├── components/
├── hooks/
├── pages/
├── services/
├── stores/
└── utils/
```

**Structure Decision**: [Specific files for this feature]

## Complexity Tracking

> Fill only if a constitution gate is violated and justify why the simpler path is not viable.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [None] | [N/A] | [N/A] |
