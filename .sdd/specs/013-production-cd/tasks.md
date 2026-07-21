# Tasks: Production Continuous Delivery

**Input**: Design documents from `specs/001-production-cd/`

**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: Deployment failure, backup failure, health/smoke failure, rollback and secret-redaction paths are required because this feature controls production safety.

**Organization**: Tasks are grouped by user story and each story has an independent verification checkpoint.

## Phase 1: Setup & Design Alignment

**Purpose**: Establish safe repository and release boundaries.

- [x] T001 Review `.specify/memory/constitution.md`, `AGENTS.md`, and `specs/001-production-cd/spec.md`; record any conflict in `specs/001-production-cd/plan.md`
- [x] T002 Run GitNexus impact analysis for every existing script/config symbol or execution flow before editing `.github/workflows/deploy.yml`, `compose.prod.yaml`, and deployment scripts
- [x] T003 [P] Validate the current production Compose model using `compose.prod.yaml` with non-secret placeholder environment values
- [x] T004 [P] Inventory existing GitHub workflow permissions, secrets, variables, environment gates, and artifact retention in `.github/workflows/deploy.yml` and `DEPLOY_GUIDE.md`
- [x] T005 [P] Inventory frontend lint/test capability in `frontend/package.json` and existing tests under `frontend/src/`

---

## Phase 2: Foundational Release Infrastructure

**Purpose**: Create shared contracts and testable script foundations that block all stories.

- [x] T006 Create strict shared deployment helpers for required inputs, redacted logging, atomic files, and exit codes in `scripts/deploy/lib.sh`
- [ ] T007 [P] Add shell static-analysis configuration and test fixture conventions for `scripts/deploy/` in `.shellcheckrc` and `scripts/deploy/tests/README.md`
- [x] T008 Create release manifest validation and state persistence in `scripts/deploy/release-manifest.sh` following `specs/001-production-cd/contracts/release-manifest.schema.json`
- [x] T009 Update `compose.prod.yaml` to require immutable `BACKEND_IMAGE` and `FRONTEND_IMAGE` references instead of production build contexts
- [x] T010 Add least-privilege package/registry permissions, concurrency, and reusable environment variables to `.github/workflows/deploy.yml`

**Checkpoint**: Manifest helpers validate inputs, Compose resolves only pinned images, and no production deployment occurs yet.

---

## Phase 3: User Story 1 - Release A Verified Version (Priority: P1)

**Goal**: Build once, publish immutable images, require approval, and deploy exactly the approved digests.

**Independent Test**: Publish a candidate from a known source SHA, approve it, deploy to a rehearsal environment, and compare running digests with the manifest; a failed gate or unapproved candidate must not deploy.

### Tests for User Story 1

- [ ] T011 [P] [US1] Add manifest validation tests for valid SHA/digests, mutable tags, unknown fields, and invalid states in `scripts/deploy/tests/release-manifest-test.sh`
- [ ] T012 [P] [US1] Add Compose contract checks for missing and digest-pinned image references in `scripts/deploy/tests/compose-contract-test.sh`
- [x] T013 [P] [US1] Add a real frontend unit test harness and `test` script in `frontend/package.json` and `frontend/src/utils/format.test.js`

### Implementation for User Story 1

- [x] T014 [US1] Extend `.github/workflows/deploy.yml` to run backend tests, frontend lint/test/build, Compose validation, and preserve reports before image publication
- [ ] T015 [US1] Build and publish backend/frontend images once with source-SHA tags, capture digests, and emit a validated candidate manifest in `.github/workflows/deploy.yml`
- [ ] T016 [US1] Add HIGH/CRITICAL image scanning and SBOM/report artifacts before production approval in `.github/workflows/deploy.yml`
- [x] T017 [US1] Implement exact-digest pull, production-drift rejection, deploy serialization, and running-digest verification in `scripts/deploy/deploy-release.sh`
- [x] T018 [US1] Route both main-triggered and manual releases through the same GitHub production environment approval and candidate-manifest gates in `.github/workflows/deploy.yml`

**Checkpoint**: US1 deploys only verified immutable digests and rejects failed, mutable, concurrent, drifted, or unapproved releases.

---

## Phase 4: User Story 2 - Protect Production Data (Priority: P1)

**Goal**: Block production mutation unless database backup and migration-safety gates pass.

**Independent Test**: Exercise missing destination, low disk, failed dump, empty output and checksum mismatch fixtures; each must block deployment before Compose changes production.

### Tests for User Story 2

- [ ] T019 [P] [US2] Add backup gate tests for missing directory, insufficient space, dump failure, empty output, checksum mismatch, metadata, and retention in `scripts/deploy/tests/backup-database-test.sh`
- [ ] T020 [P] [US2] Add migration-policy checks for changed/removed applied migration files and forward-compatible release evidence in `scripts/deploy/tests/migration-policy-test.sh`

### Implementation for User Story 2

- [x] T021 [US2] Implement PostgreSQL custom-format backup, checksum, metadata, retention and atomic completion in `scripts/deploy/backup-database.sh`
- [x] T022 [US2] Add pre-deploy disk-capacity, backup-integrity and migration-policy gates before application mutation in `scripts/deploy/deploy-release.sh`
- [x] T023 [US2] Preserve backup metadata and reference its identifier in the release manifest from `.github/workflows/deploy.yml`
- [x] T024 [US2] Document forward-only expand/contract migration review and approved restore boundaries in `DEPLOY_GUIDE.md`

**Checkpoint**: US2 blocks deploy before mutation whenever backup or migration evidence is unsafe.

---

## Phase 5: User Story 3 - Detect Failure And Roll Back Application (Priority: P1)

**Goal**: Detect unhealthy deployments within 3 minutes and restore the previous compatible application within 10 minutes without database downgrade.

**Independent Test**: Deploy an intentionally unhealthy rehearsal image and show diagnostics, previous-digest restoration, repeated health/smoke success, and unchanged Flyway history within target times.

### Tests for User Story 3

- [ ] T025 [P] [US3] Add health and public smoke-test fixtures for success, timeout, HTTP failure, and secret-safe diagnostics in `scripts/deploy/tests/smoke-test-test.sh`
- [ ] T026 [P] [US3] Add rollback tests for missing previous manifest, incompatible schema marker, Compose failure, successful restoration, and incident exit code in `scripts/deploy/tests/rollback-release-test.sh`

### Implementation for User Story 3

- [x] T027 [US3] Implement bounded internal container health and public HTTPS/API smoke checks in `scripts/deploy/smoke-test.sh`
- [x] T028 [US3] Implement previous-manifest application rollback, exact-digest verification, no-migration-downgrade guard, and incident terminal state in `scripts/deploy/rollback-release.sh`
- [ ] T029 [US3] Integrate diagnostics, smoke checks, automatic compatible application rollback, and final status propagation in `scripts/deploy/deploy-release.sh`
- [ ] T030 [US3] Enforce the 3-minute detection and 10-minute rollback bounds in `.github/workflows/deploy.yml` and deployment script timeouts

**Checkpoint**: US3 meets time targets in rehearsal and never downgrades or restores the database automatically.

---

## Phase 6: User Story 4 - Trace And Operate Releases (Priority: P2)

**Goal**: Make every release reconstructable while keeping secrets out of evidence.

**Independent Test**: Reconstruct a completed release from source SHA, image digests, approval, backup, gates, timestamps and final status in under 10 minutes; secret scanning returns no findings.

### Tests for User Story 4

- [ ] T031 [P] [US4] Add release-evidence completeness and append-only history tests in `scripts/deploy/tests/release-evidence-test.sh`
- [ ] T032 [P] [US4] Add seeded secret-redaction tests covering passwords, tokens, SSH material and environment values in `scripts/deploy/tests/redaction-test.sh`

### Implementation for User Story 4

- [x] T033 [US4] Persist current, previous and timestamped append-only release manifests with requester/approver and gate timestamps in `scripts/deploy/deploy-release.sh`
- [ ] T034 [US4] Upload bounded non-secret test, scan and release evidence with explicit retention in `.github/workflows/deploy.yml`
- [x] T035 [US4] Expand production setup, release, rollback, incident and evidence reconstruction procedures in `DEPLOY_GUIDE.md`

**Checkpoint**: US4 provides complete evidence without exposing credentials or business data.

---

## Phase 7: Polish & Cross-Cutting Verification

- [ ] T036 [P] Run ShellCheck and all script fixtures under `scripts/deploy/tests/`; record commands in `specs/001-production-cd/quickstart.md`
- [x] T037 [P] Run backend tests and frontend lint/test/build; verify reports and coverage expectations from `.github/workflows/deploy.yml`
- [x] T038 [P] Validate `.github/workflows/deploy.yml`, `compose.prod.yaml`, Dockerfiles, and release manifest schema without reading `.env`
- [ ] T039 Rehearse healthy deployment and intentionally unhealthy rollback on a non-production environment using `specs/001-production-cd/quickstart.md`
- [ ] T040 Review logs/artifacts for secrets, ensure no TODO/unsafe debug output remains, and update `specs/001-production-cd/checklists/cd-readiness.md`
- [ ] T041 Run `gitnexus_detect_changes()` and review affected execution flows before any commit
- [x] T042 Review branch/commit/PR compliance and document GitHub environment/branch-protection settings in `DEPLOY_GUIDE.md`

## Dependencies

- Phase 1 blocks all edits and establishes impact/scope.
- Phase 2 blocks all user stories.
- US1 provides immutable images and manifests required by US2–US4.
- US2 must complete before US3 production rehearsal because rollback safety depends on migration and backup evidence.
- US3 and US4 can proceed in parallel after US1/US2 foundations, but final rehearsal requires both.
- Phase 7 requires all selected user stories complete.

## Parallel Execution Examples

- Setup: T003, T004, and T005 inspect independent surfaces.
- US1: T011, T012, and T013 can be implemented in parallel before T014–T018.
- US2: T019 and T020 cover independent backup and migration policies.
- US3: T025 and T026 cover independent smoke and rollback behaviors.
- US4: T031 and T032 cover evidence completeness and redaction independently.
- Verification: T036, T037, and T038 can run in parallel after implementation.

## Implementation Strategy

1. Deliver US1 as the artifact-identity MVP, but do not enable production deployment until US2 and US3 gates exist.
2. Add US2 data protection and complete a backup/restore drill outside production.
3. Add US3 rollback and pass the intentionally unhealthy rehearsal.
4. Add US4 evidence hardening and operational documentation.
5. Enable first production release only after the formal checklist and cross-artifact analysis have no CRITICAL/HIGH findings.
