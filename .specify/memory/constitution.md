# WMS Phuc Anh Constitution

<!--
  Sync Impact Report
  Version change: 1.0.0 -> 1.1.0
  Modified principles:
  - Consolidated canonical constitution from .sdd/constitution.md into this Speckit source.
  - Clarified inventory fields, audit schema, migration lifecycle, and JPA/Lombok rules.
  Added sections:
  - Canonical Sources
  - Migration Lifecycle
  - Speckit Artifact Gates
  Removed sections:
  - Generic placeholder-style constraints
  Templates requiring updates:
  - .specify/templates/plan-template.md: pending in same cleanup
  - .specify/templates/spec-template.md: pending in same cleanup
  - .specify/templates/tasks-template.md: pending in same cleanup
  Follow-up TODOs: none
-->

**Version**: 1.1.0
**Ratified**: 2026-05-29
**Last Amended**: 2026-06-11
**Status**: Active

This constitution is the canonical rule source for Speckit agents and human
review. Project docs under `.sdd/constraints/` may expand these rules, but they
must not contradict this file.

## 1. Scope

The Warehouse Management System (WMS) manages inventory, receipt, outbound
delivery, transfer, stocktake, internal accounting, dealer debt, and internal
fleet operations for the Phuc Anh warehouses in Hai Phong, Ha Noi, and Ho Chi
Minh.

In scope:
- Warehouse operations: inbound, outbound, transfer, stocktake, adjustment.
- Internal accounting: COGS, pricing, invoices, payments, credit status.
- Internal fleet dispatch only; no third-party logistics in Sprint 1.
- Dealer and supplier master data.

Out of scope unless explicitly specified:
- Manufacturing, HR/HRM, B2B/B2C portals, external integrations, barcode/QR
  scanner automation.

## 2. Immutable Tech Stack

- Backend: Spring Boot 3.4.5 + Java 21 + Maven.
- Frontend: React 18 + JavaScript + Vite.
- Database: PostgreSQL 18.
- ORM: Spring Data JPA / Hibernate. Application code MUST NOT use raw SQL.
- Auth: JWT + bcrypt with cost factor >= 12.
- API docs: OpenAPI / Swagger.
- DB migration: Flyway.
- Backend tests: JUnit 5 + Mockito.
- Frontend tests: Jest + React Testing Library.
- Styling: Tailwind CSS 3.x.

Supporting libraries such as Lombok, MapStruct, Jackson, Zustand, React Hook
Form, Axios, and Lucide Icons are allowed when they do not replace the stack.

## 3. Architecture Principles

Backend code MUST follow layered architecture:
Controller -> Service -> Repository -> Entity.

- Controllers handle HTTP, validation, response shape, status codes, and DTOs.
  They MUST NOT contain business logic.
- Services contain business rules, transactions, authorization checks, and audit
  logging.
- Repositories are Spring Data JPA interfaces.
- Entities map database tables and relationships only.

All write endpoints MUST use request DTO validation with Jakarta Validation.
Errors MUST be handled by centralized exception handling. Business rule
violations use HTTP 422, validation failures use HTTP 400, and optimistic lock
conflicts use HTTP 409.

## 4. Inventory And Batch Invariants

- Inventory fields are `inventories.total_qty`, `inventories.reserved_qty`, and
  `inventories.version`.
- The following MUST always hold in database constraints and service logic:
  `total_qty >= 0`, `reserved_qty >= 0`, and
  `total_qty - reserved_qty >= 0`.
- Inventory updates MUST go through receipt, outbound delivery, transfer,
  adjustment, or stocktake flows. Services MUST NOT directly patch inventory
  quantities outside those flows.
- Every inventory update MUST use optimistic locking with `@Version`.
- FIFO by received date is the default and only batch selection rule for the
  current household-goods domain.
- Products do not track per-unit serial numbers in Sprint 1.
- Products and batches do not track expiry dates in Sprint 1.
- Batches do not use A/B/C grade classification; QC-failed goods move to
  quarantine for return-to-vendor or disposal handling instead of being
  reclassified into another sellable grade.
- Putaway MUST validate bin capacity before moving stock into a bin.

## 5. QC, Quarantine, And Transfer Invariants

- Inbound goods MUST pass QC before they can increase regular available stock.
- Outbound goods MUST pass outbound QC before shipment.
- QC-failed stock MUST be moved into a quarantine location
  (`warehouse_locations.is_quarantine = true`) and excluded from available stock.
- Quarantine stock may leave quarantine only through approved return-to-vendor
  or disposal flows.
- Internal transfers MUST pass through the In-Transit warehouse/location until
  the destination warehouse confirms receipt.
- Transfer discrepancies between sent and received quantities MUST create
  adjustment and audit records.

## 6. Audit, Security, And Data Integrity

- Every business mutation MUST create an audit log entry.
- Audit entries MUST include actor, actor role, action, entity type, entity id,
  timestamp, warehouse when relevant, and before/after values when relevant.
- Audit logs are append-only. Code MUST NOT update or delete audit rows.
- Secrets, passwords, API keys, JWT secrets, and credentials MUST NOT be stored
  in source control or committed `.env` files.
- JWT authorization MUST check both role and warehouse scope where warehouse
  data is involved.
- Master data is soft-deleted with `is_active = false`.
- Transaction data is cancelled with `status = CANCELLED`; business history MUST
  NOT be physically deleted.
- Code MUST NOT hardcode warehouse IDs, role assumptions, or approval state
  transitions without explicit domain constants or lookup rules.

## 7. Code Quality And JPA Conventions

- Prefer constructor injection.
- Java classes use PascalCase; React components use PascalCase; hooks/utilities
  use camelCase; API resources use kebab-case; database tables and columns use
  snake_case.
- Production code MUST NOT use `System.out` or `console.log`.
- Completed code MUST NOT leave TODO comments.
- Keep functions near 40 lines and files near 300 lines when practical.
- Comments explain why, not what.
- JPA entities SHOULD use Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`,
  `@AllArgsConstructor`, and `@Builder` when useful.
- JPA entities with lazy relationships MUST NOT use Lombok `@Data`.

## 8. Migration Lifecycle

- Before a database has been shared, deployed, or used as a durable environment,
  migration squashing and renumbering may be done in a dedicated cleanup task.
- After a migration has been applied in a shared or deployed environment, it is
  immutable. New schema changes MUST be added in a new Flyway migration.
- Duplicate Flyway versions are not allowed in runnable migration history.
- Migration cleanup MUST NOT delete business data, `/data`, or `/uploads`.

## 9. Speckit Artifact Gates

Every feature spec, plan, and task list MUST make the following visible when
relevant:
- The affected WMS domain flow and state transitions.
- Entity/table changes and Flyway migration impact.
- Request/response DTO validation.
- Authorization rule: role plus warehouse scope when applicable.
- Audit log action and before/after state.
- Inventory, batch, QC, reservation, transfer, or accounting invariant touched.
- Unit tests for services/business rules and integration tests for endpoints.
- OpenAPI/Swagger update for every new or changed endpoint.

## 10. Testing And Definition Of Done

A task is not done until:
- Service/business logic tests are written and passing with >= 80% coverage for
  the changed service surface.
- API endpoint integration tests cover happy and error paths.
- `maven compile` and frontend lint/build commands relevant to the change pass.
- Swagger/OpenAPI is updated for endpoint changes.
- Error cases use correct HTTP status codes.
- Warehouse mutations create audit logs.
- FIFO, negative inventory, reserved quantity, and version conflict paths are
  tested when touched.

## 11. Git And Review

- Branches use `feat/*`, `fix/*`, `spec/*`, or `chore/*`.
- Commits use `[type]([scope]): [description]`.
- Direct commits to `main` or `production` are forbidden.
- Pull requests require at least one approval and should stay under 400 changed
  lines. Larger work should be split.

## 12. Governance

This constitution overrides conflicting project guidance. If another file
disagrees, update that file or amend this constitution explicitly.

Amendments require:
1. A pull request or explicit user request describing the reason and impact.
2. Review of dependent Speckit templates and project guidance files.
3. Semantic versioning:
   - MAJOR for incompatible principle removals or redefinitions.
   - MINOR for new principles or materially expanded rules.
   - PATCH for wording and non-semantic clarifications.
