---
description: "WMS task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `.sdd/specs/[###-feature-name]/`

**Prerequisites**: `spec.md`, `plan.md`, and, when relevant, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: Tests are REQUIRED for service/business logic and API endpoints touched by the feature.

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
- Feature docs: `.sdd/specs/[###-feature-name]/`

## Phase 1: Setup & Design Alignment

**Purpose**: Confirm the feature is implementable without breaking WMS invariants.

- [ ] T001 Read `.specify/memory/constitution.md`, `AGENTS.md`, and the feature `spec.md`.
- [ ] T002 Verify `plan.md` lists affected entities, state transitions, audit actions, authorization rules, and OpenAPI impact.
- [ ] T003 [P] Verify existing entities/repositories/services related to the feature.
- [ ] T004 [P] Verify Flyway migration state; if cleanup/squash is needed, create a separate migration-cleanup task and do not delete applied migrations.

---

## Phase 2: Foundational Backend

**Purpose**: Shared backend pieces that block user stories.

- [ ] T005 Create/update JPA entities in `backend/src/main/java/com/wms/entity/` without using Lombok `@Data` on lazy relationships.
- [ ] T006 Create/update enums in `backend/src/main/java/com/wms/enums/`.
- [ ] T007 Create/update repositories in `backend/src/main/java/com/wms/repository/`.
- [ ] T008 Create/update request DTOs with Jakarta Validation in `backend/src/main/java/com/wms/dto/request/`.
- [ ] T009 Create/update response DTOs in `backend/src/main/java/com/wms/dto/response/`.
- [ ] T010 Create/update mapper code in `backend/src/main/java/com/wms/mapper/`.
- [ ] T011 Add Flyway migration only if schema changes are required.
- [ ] T012 Update centralized error handling if new business errors are introduced.

**Checkpoint**: Backend foundation compiles and preserves constitution gates.

---

## Phase 3: User Story 1 - [Title] (Priority: P1)

**Goal**: [Brief description]

**Independent Test**: [How to verify]

### Tests for User Story 1

- [ ] T013 [P] [US1] Add service unit tests for happy path and business-rule failures in `backend/src/test/java/com/wms/service/`.
- [ ] T014 [P] [US1] Add controller/API integration tests for happy path and error paths in `backend/src/test/java/com/wms/controller/`.
- [ ] T015 [P] [US1] Add repository/query tests if custom query behavior or locking is used.

### Implementation for User Story 1

- [ ] T016 [US1] Implement service method with `@Transactional`, authorization, validation, state transition, and audit logging.
- [ ] T017 [US1] Implement REST endpoint in `backend/src/main/java/com/wms/controller/`.
- [ ] T018 [US1] Update OpenAPI/Swagger annotations and contract docs.
- [ ] T019 [US1] Implement frontend service/page/component changes if UI is in scope.
- [ ] T020 [US1] Add frontend tests for business UI logic if UI is in scope.

**Checkpoint**: US1 works independently and all US1 tests pass.

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description]

**Independent Test**: [How to verify]

### Tests for User Story 2

- [ ] T021 [P] [US2] Add service unit tests for happy path and business-rule failures.
- [ ] T022 [P] [US2] Add controller/API integration tests for happy path and error paths.

### Implementation for User Story 2

- [ ] T023 [US2] Implement service logic with audit and authorization.
- [ ] T024 [US2] Implement endpoint and DTO mapping.
- [ ] T025 [US2] Update OpenAPI/Swagger annotations and contract docs.
- [ ] T026 [US2] Implement frontend changes if UI is in scope.

**Checkpoint**: US1 and US2 work independently.

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description]

**Independent Test**: [How to verify]

### Tests for User Story 3

- [ ] T027 [P] [US3] Add service unit tests for happy path and business-rule failures.
- [ ] T028 [P] [US3] Add controller/API integration tests for happy path and error paths.

### Implementation for User Story 3

- [ ] T029 [US3] Implement service logic with audit and authorization.
- [ ] T030 [US3] Implement endpoint and DTO mapping.
- [ ] T031 [US3] Update OpenAPI/Swagger annotations and contract docs.
- [ ] T032 [US3] Implement frontend changes if UI is in scope.

---

## Phase N: Cross-Cutting Verification

- [ ] T900 Run `mvn clean compile` from `backend/`.
- [ ] T901 Run backend tests relevant to the changed feature.
- [ ] T902 Run frontend build/tests relevant to the changed feature if frontend changed.
- [ ] T903 Verify no `System.out`, production `console.log`, hardcoded secrets, or TODO comments remain.
- [ ] T904 Verify all warehouse mutations create audit logs.
- [ ] T905 Verify OpenAPI/Swagger reflects endpoint changes.
- [ ] T906 Verify inventory/QC/transfer/accounting invariants touched by this feature have tests.

## Dependency Rules

- Foundational backend tasks block user story implementation.
- Entities/enums/repositories come before services.
- Services come before controllers.
- DTO validation and error mapping must be complete before controller tests pass.
- Audit logging and authorization are part of the story implementation, not polish.
- Do not mark a story complete until its independent tests pass.
