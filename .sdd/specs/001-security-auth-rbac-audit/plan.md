# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `.sdd/specs/[###-feature-name]/spec.md`

## Summary

[Primary business outcome + affected WMS flow/state transition.]

## Technical Context

**Language/Version**: Java 21 / Spring Boot 3.4.5; React 18 + JavaScript when UI is touched

**Primary Dependencies**: Spring Web, Spring Data JPA, Hibernate, Spring Security, Lombok, Springdoc OpenAPI, React, Tailwind CSS

**Storage**: PostgreSQL 18 via Flyway migrations and Spring Data JPA

**Testing**: JUnit 5 + Mockito for backend; Spring integration tests for APIs; Jest + React Testing Library for frontend business UI

**Target Platform**: Full-stack WMS web application and REST API

**Project Type**: Backend + frontend web application

**Performance Goals**: [Use spec NFRs, e.g. <= 500ms search, <= 2s inventory mutation]

**Constraints**: Must preserve WMS invariants: no negative inventory, QC gates, audit logs, role + warehouse authorization, no raw SQL in application code

**Scale/Scope**: 3 physical warehouses, In-Transit warehouse, 1000+ products, 50+ dealers, 1000+ transactions/month

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [ ] Layered architecture preserved: Controller -> Service -> Repository -> Entity.
- [ ] Write endpoints use request DTOs with Jakarta Validation.
- [ ] Service methods own business rules, transactions, authorization, and audit logging.
- [ ] All DB access goes through Spring Data JPA/Hibernate; no raw SQL in application code.
- [ ] Inventory invariants preserved if touched: `total_qty >= 0`, `reserved_qty >= 0`, `available = total_qty - reserved_qty >= 0`, `@Version`.
- [ ] QC/quarantine/transfer/accounting state rules listed when touched.
- [ ] Audit action, entity type, before/after payload, and warehouse scope identified.
- [ ] OpenAPI/Swagger impact identified for every new or changed endpoint.
- [ ] Flyway migration impact identified; no duplicate migration version in runnable history.
- [ ] Unit and integration test strategy covers happy path and error paths.

## Domain Impact

**Actors/Roles**: [Roles and warehouse scope]

**State Changes**: [Entity statuses before -> after]

**Inventory Impact**: [None or exact total/reserved/quarantine/In-Transit mutation]

**Audit Actions**: [Action names and payload]

**Security/Authorization**: [JWT role + warehouse checks]

**Accounting Impact**: [None or invoice/payment/period/debt effect]

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
