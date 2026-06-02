# Implementation Plan: System Audit Logging (Backend)

**Branch**: `001-audit-logging` | **Date**: 2026-06-03 | **Spec**: [feature-system-audit-logging.md](file:///d:/Git/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-system-audit-logging.md)

**Input**: Feature specification from `.sdd/specs/001-security-auth-rbac-audit/features/feature-system-audit-logging.md`

---

## Summary

Implement the backend audit logging infrastructure for WMS Phúc Anh. The system will record all warehouse business operations (receipt, issue, transfer, adjustment, stocktake, delivery, batch, inventory, return, scrap/disposal, trip) into an immutable `audit_logs` table. A cursor-based paginated REST API (`GET /api/v1/audit-logs`) will enable Admin, CEO, Warehouse Manager, and Accountant to search and review audit history with date range, entity, action, actor, and warehouse filters.

## Technical Context

**Language/Version**: Java 21 + Spring Boot 3.4.5 (Maven)

**Primary Dependencies**: Spring Data JPA, Spring Web, Spring Security (JWT), Jackson (JSON), Jakarta Validation

**Storage**: PostgreSQL 18 — table `audit_logs` already defined in `V1__init_schema.sql`

**Testing**: JUnit 5 + Mockito (unit tests), Spring Boot Test (integration tests)

**Target Platform**: Web service (REST API)

**Project Type**: Full-stack web application (backend focus for this plan)

**Performance Goals**: Audit log query p95 ≤ 2s for last 90 days (NFR-002)

**Constraints**: Immutable entries (no UPDATE/DELETE), permanent retention, max 300 lines/file, max 40 lines/function

**Scale/Scope**: ~1000 transactions/month across 3 warehouses

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Layered Architecture | ✅ PASS | Controller → Service → Repository → Entity |
| II. Inventory Integrity | ✅ N/A | Audit logging does not modify inventory |
| III. FEFO/FIFO | ✅ N/A | Not applicable to audit feature |
| IV. QC Gate | ✅ N/A | Not applicable to audit feature |
| V. In-Transit Tracking | ✅ N/A | Not applicable to audit feature |
| VI. Auth & RBAC | ✅ PASS | JWT auth required. RBAC enforced: Admin/CEO = all, Manager = own warehouse, Accountant = all |
| VII. Test Coverage | ✅ PLANNED | Min 80% for AuditLogService. Unit + integration tests planned |
| Tech Stack | ✅ PASS | Spring Boot 3.4.5 + Java 21 + PostgreSQL + JPA |
| Code Quality | ✅ PLANNED | 40 lines/function, 300 lines/file, no TODO, no System.out |

**Gate Result**: ✅ ALL PASSED — No violations.

## Project Structure

### Documentation (this feature)

```text
specs/001-audit-logging/
├── plan.md              # This file
├── research.md          # Phase 0 output — all decisions documented
├── data-model.md        # Phase 1 output — entity, DTOs, indexes
├── quickstart.md        # Phase 1 output — architecture + how to run
├── contracts/
│   └── audit-log-api.md # Phase 1 output — REST API contract
└── tasks.md             # Phase 2 output (created by /speckit-tasks)
```

### Source Code (backend)

```text
backend/src/main/java/com/wms/
├── entity/
│   └── AuditLog.java              # MODIFY — add description, warehouse FK, NOT NULL constraints
├── enums/
│   ├── AuditAction.java           # EXISTS — no changes needed
│   └── AuditEntityType.java       # NEW — enum for loggable entity types
├── dto/
│   ├── AuditLogResponse.java      # NEW — response DTO with actorName
│   └── AuditLogPageResponse.java  # NEW — cursor-based page wrapper
├── repository/
│   └── AuditLogRepository.java    # NEW — JPA repo + custom cursor-based queries
├── service/
│   └── AuditLogService.java       # NEW — log creation + paginated query logic
├── controller/
│   └── AuditLogController.java    # NEW — GET /api/v1/audit-logs endpoint
└── util/
    └── AuditLogUtil.java          # NEW — sensitive field filter + diff builder

backend/src/test/java/com/wms/
├── service/
│   └── AuditLogServiceTest.java   # NEW — unit tests (>= 80% coverage)
├── controller/
│   └── AuditLogControllerTest.java # NEW — integration tests (happy + error paths)
└── repository/
    └── AuditLogRepositoryTest.java # NEW — query tests with cursor pagination
```

**Structure Decision**: Follows existing Spring Boot layered architecture. No new packages needed — all files go into existing package structure.

## Complexity Tracking

> No constitution violations — this section is empty.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| — | — | — |
