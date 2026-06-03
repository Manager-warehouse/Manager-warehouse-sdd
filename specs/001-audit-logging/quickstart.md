# Quickstart: System Audit Logging

**Feature**: System Audit Logging
**Date**: 2026-06-03

---

## What This Feature Does

Records user-driven changes in an immutable audit log and exposes the logs to System Admin through the existing Audit Trail tab in `UserManagement`.

CEO and all non-admin roles cannot view audit logs.

## Backend Flow

```text
Business Service
  -> AuditLogService (@Service singleton)
      -> filters sensitive fields
      -> stores field-level old/new diffs
      -> AuditLogRepository
          -> audit_logs table
```

`AuditLogService` is a normal Spring singleton bean. Do not implement it as a static global object.

## Frontend Flow

```text
UserManagement.jsx
  -> Audit Trail tab
      -> adminService.getAuditLogs({ page, pageSize, from, to, warehouseId })
      -> Table rows sorted newest first
      -> row click / detail button
          -> adminService.getAuditLogById(id)
          -> Modal/Drawer with before/after changed fields
```

Do not create a standalone audit route for this feature. Keep the UI inside the existing user management screen.

## Files to Create or Modify

### Backend

| File | Change |
|------|--------|
| `entity/AuditLog.java` | Map current `audit_logs` schema and immutable behavior assumptions |
| `enums/AuditAction.java` | Ensure enum matches DB action values |
| `dto/AuditLogListItemResponse.java` | List row DTO |
| `dto/AuditLogDetailResponse.java` | Detail DTO with old/new values |
| `dto/AuditLogPageResponse.java` | Page wrapper |
| `repository/AuditLogRepository.java` | Read queries and insert support |
| `service/AuditLogService.java` | Singleton logging entry point and query logic |
| `controller/AuditLogController.java` | `GET /api/v1/audit-logs`, `GET /api/v1/audit-logs/{id}` |

### Frontend

| File | Change |
|------|--------|
| `frontend/src/pages/Admin/UserManagement.jsx` | Keep audit tab; add pagination controls, optional filters, and detail modal/drawer |
| `frontend/src/services/admin.service.js` | Add query params to `getAuditLogs`; add `getAuditLogById` |
| `frontend/src/components/common/Table.jsx` | Reuse as-is unless pagination controls are intentionally generalized |

## Manual API Checks

```bash
cd backend
mvn test
```

```bash
curl -H "Authorization: Bearer <admin-token>" \
  "http://localhost:8080/api/v1/audit-logs?page=1&pageSize=30"
```

```bash
curl -H "Authorization: Bearer <admin-token>" \
  "http://localhost:8080/api/v1/audit-logs?from=2026-06-01&to=2026-06-03&warehouseId=1&page=1&pageSize=30"
```

```bash
curl -H "Authorization: Bearer <admin-token>" \
  "http://localhost:8080/api/v1/audit-logs/1234"
```

## Expected Behavior

- Page 1 returns 30 newest logs.
- Unfiltered page 51 returns `QUERY_RANGE_TOO_LARGE`.
- Filtered older queries are allowed.
- Non-admin users, including CEO, receive `FORBIDDEN_AUDIT_ACCESS`.
- Detail view shows before/after changed fields and omits sensitive fields.
- Audit log rows cannot be updated or deleted after creation.
