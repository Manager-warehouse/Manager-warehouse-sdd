# [PROJECT_NAME] Constitution

<!--
  Use `.specify/memory/constitution.md` as the canonical project constitution.
  This template exists only for re-initialization. Keep it aligned with the WMS
  constitution shape when the canonical file is regenerated.
-->

**Version**: [CONSTITUTION_VERSION]
**Ratified**: [RATIFICATION_DATE]
**Last Amended**: [LAST_AMENDED_DATE]
**Status**: Active

## 1. Scope

[PROJECT_SCOPE]

## 2. Immutable Tech Stack

- Backend: Spring Boot 3.4.5 + Java 21 + Maven.
- Frontend: React 18 + JavaScript + Vite.
- Database: PostgreSQL 18.
- ORM: Spring Data JPA / Hibernate; no raw SQL in application code.
- Auth: JWT + bcrypt with cost factor >= 12.
- API Docs: OpenAPI / Swagger.
- DB Migration: Flyway.
- Testing: JUnit 5 + Mockito; Jest + React Testing Library.
- Styling: Tailwind CSS 3.x.

## 3. Architecture Principles

[LAYERED_ARCHITECTURE_AND_ERROR_HANDLING_RULES]

## 4. Domain Invariants

[INVENTORY_BATCH_QC_TRANSFER_ACCOUNTING_RULES]

## 5. Audit, Security, And Data Integrity

[AUDIT_SECURITY_SOFT_DELETE_RULES]

## 6. Code Quality And JPA Conventions

[CODE_QUALITY_AND_JPA_RULES]

## 7. Migration Lifecycle

[FLYWAY_LIFECYCLE_RULES]

## 8. Speckit Artifact Gates

[SPEC_PLAN_TASK_GATE_RULES]

## 9. Testing And Definition Of Done

[TESTING_AND_DOD_RULES]

## 10. Governance

[GOVERNANCE_RULES]
