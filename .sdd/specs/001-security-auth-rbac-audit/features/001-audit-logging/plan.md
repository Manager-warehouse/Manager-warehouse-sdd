# Implementation Plan: System Audit Logging

**Branch**: `001-audit-logging` | **Date**: 2026-06-03 | **Spec**: [.sdd/specs/001-security-auth-rbac-audit/features/feature-system-audit-logging.md](../../.sdd/specs/001-security-auth-rbac-audit/features/feature-system-audit-logging.md)

**Input**: Feature specification from `.sdd/specs/001-security-auth-rbac-audit/features/feature-system-audit-logging.md`

---

## Summary

Implement immutable system audit logging for user-driven changes across WMS. The backend records field-level diffs through a Spring singleton `AuditLogService` and exposes read-only audit APIs. The frontend keeps the existing Audit Trail tab inside `UserManagement`, adds backend pagination design, optional time/warehouse filters, and a detail view for before/after field changes.

Only `System Admin` (`ADMIN`) can view audit logs. `CEO` and all other roles are forbidden.

## Technical Context

**Language/Version**: Java 21 + Spring Boot 3.4.5 (Maven), React 18 + JavaScript

**Primary Dependencies**: Spring Data JPA, Spring Web, Spring Security JWT, Jackson JSON, Jakarta Validation, Tailwind CSS 3.x

**Storage**: PostgreSQL 18, Flyway table `audit_logs`

**Testing**: JUnit 5 + Mockito, Spring Boot Test, Jest for frontend behavior where added

**Target Platform**: Full-stack web application

**Project Type**: Backend REST API + existing React admin UI tab

**Performance Goals**: Audit log query p95 <= 2s for last 90 days; default page size 30 logs

**Constraints**: Append-only audit log, no UPDATE/DELETE after creation, no sensitive fields in log, no production `console.log`/`System.out`, max 300 lines/file and 40 lines/function where feasible

**Scale/Scope**: WMS operations across 3 warehouses; default unfiltered browsing window is 50 pages (1,500 newest logs)

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Layered Architecture | PASS | Controller -> Service -> Repository -> Entity |
| II. Inventory Integrity | PASS | Audit logging records inventory operations but does not mutate inventory directly |
| III. FEFO/FIFO | N/A | No batch selection logic in audit query feature |
| IV. QC Gate | PASS | QC state changes are logged; QC gate is not bypassed |
| V. In-Transit Tracking | PASS | Transfer state changes are logged; transfer flow remains unchanged |
| VI. Auth & RBAC | PASS | Only ADMIN may view audit logs; warehouse filter is optional for search |
| VII. Test Coverage | PLANNED | Service and controller tests required for log creation, immutability assumptions, RBAC, pagination, and filters |
| Tech Stack | PASS | Uses required Spring Boot, JPA, PostgreSQL, React, Tailwind stack |
| Code Quality | PLANNED | Singleton is Spring `@Service`, not static/global mutable state |

**Gate Result**: PASS. No constitution violation.

## Project Structure

### Documentation

```text
specs/001-audit-logging/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── audit-log-api.md
└── tasks.md
```

### Backend Design

```text
backend/src/main/java/com/wms/
├── entity/
│   └── AuditLog.java
├── enums/
│   └── AuditAction.java
├── dto/
│   ├── AuditLogResponse.java
│   ├── AuditLogDetailResponse.java
│   └── AuditLogPageResponse.java
├── repository/
│   └── AuditLogRepository.java
├── service/
│   └── AuditLogService.java
└── controller/
    └── AuditLogController.java
```

**Backend Decision**: `AuditLogService` is a Spring singleton `@Service`. Business services call it after successful user-driven changes. It is not a static singleton and it does not bypass layered architecture.

### Frontend Design

```text
frontend/src/pages/Admin/UserManagement.jsx
├── existing users tab
└── existing auditLogs tab
    ├── audit log table
    ├── page controls
    ├── optional time/warehouse filters
    └── detail modal/drawer for changed fields

frontend/src/services/admin.service.js
└── getAuditLogs(params), getAuditLogById(id)
```

**Frontend Decision**: Keep Audit Trail as a tab inside `UserManagement`. Do not create a standalone route. Reuse the existing `Table`, `Badge`, `Input`, `Button`, and `Modal` components.

## Complexity Tracking

No constitution violations.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| - | - | - |

## Phase 0 Output

See [research.md](research.md).

## Phase 1 Output

See [data-model.md](data-model.md), [contracts/audit-log-api.md](contracts/audit-log-api.md), and [quickstart.md](quickstart.md).
