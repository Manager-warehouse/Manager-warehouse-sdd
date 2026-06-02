# Quickstart: Audit Logging Backend

**Feature**: System Audit Logging
**Date**: 2026-06-03

---

## What Does This Feature Do?

Automatically records all warehouse business operations (create receipt, approve transfer, adjust inventory, etc.) into an immutable audit log table. Provides a paginated API for Admin, CEO, Warehouse Manager, and Accountant to search and review activity history.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  Service Layer (e.g., ReceiptService, TransferService)  │
│  Calls AuditLogService.log() after business operations  │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│              AuditLogService (@Service)                  │
│  - Resolves actor from SecurityContext                  │
│  - Auto-generates description                           │
│  - Filters sensitive fields from diff                   │
│  - Persists AuditLog entity                             │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│          AuditLogRepository (@Repository)                │
│  - Cursor-based pagination queries                      │
│  - Date range + filter queries                          │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│              AuditLogController (@RestController)        │
│  GET /api/v1/audit-logs                                 │
│  - Query params: cursor, size, filters, date range      │
│  - RBAC: ADMIN, CEO, WAREHOUSE_MANAGER, ACCOUNTANT      │
└─────────────────────────────────────────────────────────┘
```

## Files to Create/Modify

### New Files

| File | Purpose |
|------|---------|
| `enums/AuditEntityType.java` | Enum for loggable entity types |
| `dto/AuditLogResponse.java` | Response DTO with actorName |
| `dto/AuditLogPageResponse.java` | Cursor-based page wrapper |
| `repository/AuditLogRepository.java` | JPA repository + custom cursor queries |
| `service/AuditLogService.java` | Log creation + query service |
| `controller/AuditLogController.java` | REST endpoint |

### Modified Files

| File | Change |
|------|--------|
| `entity/AuditLog.java` | Add `description`, `warehouse` FK, make `actor` NOT NULL, add `actorRole` NOT NULL |

## How to Test

```bash
# 1. Start backend
cd backend && mvn spring-boot:run

# 2. Call audit log API (requires JWT)
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/audit-logs?size=30&startDate=2026-05-27&endDate=2026-06-03"

# 3. Run tests
mvn test -pl backend -Dtest="AuditLog*"
```

## Key Design Decisions

1. **AOP not used initially**: AuditLogService.log() is called explicitly from service methods. This gives more control over what data to diff and when to create entries (e.g., Transfer → 2 entries).
2. **Cursor-based pagination**: Uses `id` (BIGSERIAL) as cursor for O(1) pagination performance.
3. **Diff-only JSONB**: Only changed fields are stored, reducing storage and improving readability.
4. **Immutable entries**: No UPDATE/DELETE API or repository methods exist for audit logs.
