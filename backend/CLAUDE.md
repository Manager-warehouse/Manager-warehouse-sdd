# Backend CLAUDE.md - WMS Backend Guide

## Purpose

This file is the backend-specific operating guide for agents and developers working in `backend/`.
It complements the root `CLAUDE.md`, `AGENTS.md`, `.specify/memory/constitution.md`, Speckit feature specs, and test docs.

The backend implements the Warehouse Management System for 3 physical warehouses: Hai Phong, Ha Noi, and Ho Chi Minh. The current Sprint 1 focus is core warehouse operations: inventory, receipt, outbound delivery, transfer, stocktake, master data, security, audit, and related accounting records.

## Canonical Sources

Read these before risky backend changes:

1. `../AGENTS.md` - project rules, forbidden patterns, sprint context, GitNexus policy.
2. `../CLAUDE.md` - full-system architecture, workflow, ADRs, lessons learned.
3. `../.specify/memory/constitution.md` - canonical technical and domain rules.
4. Relevant `.sdd/specs/<feature>/spec.md`, `plan.md`, `tasks.md`, `contracts/openapi.yaml`.
5. `../test/unit.md` and `../test/integrantion.md` for test expectations.
6. `src/main/resources/db/migration/README.md` before touching Flyway migrations.

If these files conflict, `.specify/memory/constitution.md` wins unless the user explicitly amends it.

## Immutable Stack

- Java 21.
- Spring Boot 3.4.5.
- Maven.
- PostgreSQL 18 target, Supabase PostgreSQL may be used for local shared testing.
- Spring Data JPA / Hibernate only for application DB access.
- Flyway for migrations.
- JWT auth and bcrypt with cost factor 12 or higher.
- Springdoc OpenAPI / Swagger.
- JUnit 5, Mockito, Spring Security Test.
- Lombok is allowed, but JPA entities with lazy relationships must not use `@Data`.

Do not replace these choices without an approved spec or explicit user request.

## Backend Package Map

Main package: `com.wms`

```text
src/main/java/com/wms/
├── WmsApplication.java
├── aop/              # cross-cutting behavior when present
├── config/           # security, JWT filter, Flyway, mail, JPA config
├── controller/       # REST controllers, validation boundary, Swagger annotations
├── dto/
│   ├── auth/         # auth payloads
│   ├── request/      # write/read request DTOs with Jakarta Validation
│   └── response/     # API response DTOs
├── entity/           # JPA entities and relationships
├── enums/            # domain constants and state machines
├── exception/        # typed exceptions and global exception mapping
├── mapper/           # DTO/entity mapping helpers
├── repository/       # Spring Data JPA repositories
├── service/          # business services, transactions, invariants
├── service/impl/     # implementations where interface style is used
└── util/             # focused utilities, no hidden business mutations
```

Tests:

```text
src/test/java/com/wms/
├── controller/       # @WebMvcTest or API-focused tests with mocked services
├── service/          # unit tests for business logic
└── util/             # utility tests
```

## Architecture Rules

The backend must follow:

```text
Controller -> Service -> Repository -> Entity
```

Controller responsibilities:
- Define REST endpoints under `/api/v1/...`.
- Use `@Valid` request DTOs for all write endpoints.
- Return correct HTTP status codes.
- Keep Swagger/OpenAPI annotations aligned with `.sdd/.../contracts/openapi.yaml`.
- Get the authenticated actor through `CurrentUserService` or Spring Security context.
- Do not contain business rules, state transitions, inventory math, or audit payload logic.

Service responsibilities:
- Own business rules, authorization, transactions, state transitions, inventory mutations, and audit logs.
- Use `@Transactional` for multi-entity mutations.
- Enforce role plus warehouse assignment for warehouse-scoped actions.
- Enforce optimistic locking/version checks for concurrent warehouse mutations.
- Never create inventory, accounting, audit, or status side effects outside the approved flow.

Repository responsibilities:
- Extend Spring Data JPA repositories.
- Use derived queries or JPQL where needed.
- Do not use raw SQL in application code.
- Locking queries must be explicit and tested when used.

Entity responsibilities:
- Map database tables and relationships.
- Keep business operations in services, not entity methods.
- Prefer `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` where useful.
- Do not use Lombok `@Data` on JPA entities with relationships.

## API Conventions

- Resource path prefix: `/api/v1`.
- Resource names use kebab-case, for example `/api/v1/warehouse-locations`.
- Write endpoints require request DTOs and Jakarta Validation.
- Return DTOs, not entities.
- Endpoint changes require:
  - Controller Swagger annotations.
  - OpenAPI contract update in the relevant `.sdd/specs/.../contracts/openapi.yaml`.
  - Controller/API tests for happy path and error paths.

Common status mapping:

| Case | HTTP |
| --- | --- |
| Validation error | 400 |
| Missing/invalid auth | 401 |
| Role or warehouse forbidden | 403 |
| Missing resource | 404 |
| Duplicate, stale version, already confirmed/decided | 409 |
| Business invariant violation | 422 |

`GlobalExceptionHandler` is the central mapping point. Prefer typed exceptions for stable business errors.

## Security And Authorization

- Backend is stateless: JWT filter before username/password filter.
- Public endpoints are limited to auth and Swagger/OpenAPI endpoints.
- Password hashing uses `BCryptPasswordEncoder(12)`.
- Warehouse actions must check both:
  - Required role.
  - User assignment to the target warehouse.
- Do not hardcode warehouse IDs, privileged usernames, role shortcuts, or approval states.
- Do not print, copy, or expose secrets from local config. Never commit `.env`, API keys, passwords, JWT secrets, Supabase credentials, or SMTP credentials.

## WMS Domain Invariants

These are not optional validations. They are system invariants.

Inventory:
- `inventories.total_qty >= 0`.
- `inventories.reserved_qty >= 0`.
- `total_qty - reserved_qty >= 0`.
- Inventory changes must go through receipt, delivery/issue, transfer, adjustment, or stocktake flows.
- Use optimistic locking through `@Version` and explicit expected-version checks when exposed by API.
- FIFO by received date is the default outbound batch rule.
- Do not add negative inventory paths.

Batch:
- Household goods are not tracked per unit.
- No serial tracking in Sprint 1.
- No expiry-date tracking in Sprint 1.
- No grade/A-B-C quality tier for sellable stock.
- Batch identity is based on product plus source receipt/document and received date where inbound requires it.

QC and quarantine:
- Inbound goods must pass QC before regular available stock can increase.
- Approval unlocks putaway; inventory increases only after putaway into a regular Bin.
- QC-failed goods go to quarantine and are excluded from available stock.
- Quarantine stock leaves quarantine only through approved RTV or disposal flows.
- Feature 003 handles RTV with "Tra NCC"; disposal belongs to Spec 009.

Transfer:
- Inter-warehouse transfer must pass through an In-Transit location.
- Sent/received discrepancies require adjustment and audit records.

Soft delete:
- Master data uses `is_active = false`.
- Transaction history uses status cancellation where applicable.
- Do not physically delete business history.

## Current Backend Flows

Implemented package examples:

- Auth: `AuthController`, `AuthService`, `JwtAuthFilter`, `JwtUtil`, `UserDetailsServiceImpl`.
- RBAC and users: `UserController`, `UserService`, `UserWarehouseAssignmentRepository`.
- Master data: product, warehouse, location, supplier, dealer, vehicle, driver controllers/services.
- Audit: `AuditLogService`, `AuditLogController`, `AuditLogUtil`, `PartnerAuditUtil`.
- Inbound receipt approval: `ReceiptApprovalController`, `ReceiptApprovalService`.
- Quarantine RTV: `QuarantineRtvController`, `QuarantineRtvService`.
- Outbound delivery: `DeliveryOrderController`, `DeliveryOrderService`.
- System config: `SystemConfigController`, `SystemConfigService`.

When adding or changing a flow, keep the existing package style unless the current feature spec explicitly calls for a different boundary.

## Audit Logging

Every business mutation must create an audit log entry.

Audit entries must include, when relevant:
- Actor and actor role.
- Action.
- Entity type.
- Entity id and code.
- Warehouse id.
- Timestamp.
- Before and after state.

Examples of warehouse actions:
- `RECEIPT_APPROVE`.
- `RECEIPT_REJECT`.
- `RECEIPT_RETURN_CONFIRM`.
- `RECEIPT_PUTAWAY_COMPLETE`.
- `QUARANTINE_RTV_CREATE`.
- `QUARANTINE_RTV_CONFIRM`.
- `INVENTORY_UPDATE`.

Audit logs are append-only. Do not update or delete audit rows.

## Flyway And Database Rules

- Flyway migrations live in `src/main/resources/db/migration/`.
- Do not delete, squash, or renumber migrations during normal feature work.
- Once a migration has been applied in a shared/durable database, it is immutable.
- Add a new migration for new schema changes.
- Never delete business data, `/data`, or `/uploads`.
- Read `src/main/resources/db/migration/README.md` before migration work.

Known migration caution:
- Early development history has duplicate versions for `V3`, `V4`, and `V5`.
- Supabase/shared DB runs must be treated carefully.
- A local run on 2026-06-13 connected successfully to Supabase, but Flyway reported the connected schema at version 18 while the local latest available migration was 13, with `outOfOrder` active. Coordinate before applying more schema changes to that database.

## Supabase And Runtime Notes

Default local command from `backend/`:

```bash
mvn spring-boot:run
```

Use Java 21 explicitly on this machine:

```bash
JAVA_HOME=/Users/haison/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/haison/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn spring-boot:run
```

If port `8080` is occupied, use:

```bash
JAVA_HOME=/Users/haison/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/haison/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

OpenAPI verification:

```bash
curl -I http://127.0.0.1:8081/v3/api-docs
```

Do not paste datasource credentials, passwords, tokens, or mail settings into chat or docs.

## Testing Rules

Minimum backend definition of done:
- `mvn compile` passes.
- Service tests cover changed business logic with happy path and error paths.
- API/controller tests cover endpoint happy path and error paths.
- Warehouse mutations verify audit logging.
- Inventory flows verify no negative inventory, reserved quantity invariants, and version conflicts when touched.
- Swagger/OpenAPI is updated when endpoints change.
- No production `System.out`.
- No completed-code `TODO`.

Common commands from `backend/`:

```bash
mvn compile
mvn test
mvn test -Dtest='*Receipt*Test,*Quarantine*Test,*Inbound*Test'
mvn test -Dtest='AuthServiceTest'
```

Controller/API tests should include:
- 200/201 happy path.
- 400 validation failures.
- 401 missing/invalid JWT where applicable.
- 403 role or warehouse scope violation.
- 404 missing resource.
- 409 stale version or duplicate action.
- 422 business invariant violation.

Test naming style:

```text
methodName_condition_expectedResult
```

Use builders or focused helper methods for test data. Keep tests specific to the rule being verified.

## GitNexus Workflow

The root project requires GitNexus impact analysis before editing code symbols.

Before modifying Java classes/methods:

```bash
npx gitnexus status
npx gitnexus analyze
npx gitnexus impact <SymbolName> --repo Manager-warehouse-sdd
```

If impact analysis reports HIGH or CRITICAL risk, warn the user before editing.

Before commit:

```bash
npx gitnexus detect-changes --repo Manager-warehouse-sdd
```

For documentation-only edits, symbol impact is normally not applicable, but still keep the change scoped and run `git diff --check`.

## Coding Conventions

- Prefer constructor injection.
- Keep functions around 40 lines when practical.
- Keep files around 300 lines when practical.
- Use SLF4J logging, not `System.out`.
- Comments explain why a rule exists, not what the next line does.
- Avoid TODO comments in completed task code.
- Do not introduce raw SQL for service logic.
- Do not bypass DTO validation on write endpoints.
- Do not return JPA entities directly from controllers.
- Do not add broad abstractions unless they remove real duplication or match existing patterns.
- Use domain enums/constants for states and actions.

## Backend Change Checklist

Before coding:
- Read the active spec/plan/tasks and relevant feature files.
- Check GitNexus status/impact for symbols you will edit.
- Identify affected domain invariants: inventory, batch, QC, transfer, reservation, audit, accounting.
- Identify required authorization: role plus warehouse scope.
- Identify migration impact.

During coding:
- Keep controller thin.
- Put state transitions and invariants in services.
- Use repositories for DB access.
- Add or update DTO validation.
- Add audit logs for mutations.
- Keep OpenAPI annotations/contracts aligned.

Before marking done:
- Run focused tests for the changed area.
- Run `mvn compile`.
- Run `git diff --check`.
- Scan for forbidden patterns when warehouse domain code is touched:

```bash
rg -n "System\\.out|TODO|has_serial|has_expiry|FEFO|BatchGrade|serialNumber" src/main/java src/test/java ../.sdd/specs
```

- Run `npx gitnexus detect-changes --repo Manager-warehouse-sdd` before commit.
- Report any full-suite failures that are outside the change scope instead of hiding them.

## Spec 003 Backend Notes

For inbound receipt approval and quarantine handling:

- Manager approval of `QC_COMPLETED` receipt creates/resolves batch and sets `APPROVED`.
- Approval does not increase regular inventory.
- Storekeeper putaway of `APPROVED` receipt into a regular Bin increases regular inventory.
- Putaway must reject quarantine locations and capacity overflow.
- Manager rejection of `QC_COMPLETED` receipt sets `RETURN_TO_SUPPLIER_PENDING`, stores reason, and creates no inventory, batch, RTV, or Debit Note.
- Storekeeper confirms supplier handover to move `RETURN_TO_SUPPLIER_PENDING` to `RETURNED_TO_SUPPLIER`.
- Manager creates RTV only for `QC_FAILED` receipt with quarantine inventory.
- RTV create generates pending `RETURN_TO_VENDOR` adjustment and system Debit Note, without deducting quarantine inventory.
- Storekeeper confirms full RTV quantity; partial quantity returns HTTP 422.
- RTV confirmation deducts quarantine inventory and keeps source receipt status `QC_FAILED`.

Relevant classes:
- `controller/ReceiptApprovalController.java`
- `service/ReceiptApprovalService.java`
- `controller/QuarantineRtvController.java`
- `service/QuarantineRtvService.java`
- `repository/ReceiptRepository.java`
- `repository/ReceiptItemRepository.java`
- `repository/AdjustmentRepository.java`
- `repository/DebitNoteRepository.java`
- `repository/InventoryRepository.java`
- `exception/GlobalExceptionHandler.java`

Relevant tests:
- `src/test/java/com/wms/service/ReceiptServiceApprovalTest.java`
- `src/test/java/com/wms/service/ReceiptServiceReturnToSupplierTest.java`
- `src/test/java/com/wms/service/ReceiptPutawayServiceTest.java`
- `src/test/java/com/wms/service/ReceiptRtvCreateServiceTest.java`
- `src/test/java/com/wms/service/ReceiptRtvConfirmServiceTest.java`
- `src/test/java/com/wms/controller/ReceiptControllerApprovalIT.java`
- `src/test/java/com/wms/controller/ReceiptQuarantineControllerIT.java`
