# Quickstart: System Audit Logging

## Prerequisites

- Backend is running with PostgreSQL migrations applied.
- At least one active `ADMIN` user exists.
- At least one non-admin user exists, such as `CEO`.
- Test data contains audit rows across at least two warehouses and multiple timestamps.

## Manual Verification

### 1. Mutation creates audit log

1. Log in as a valid user.
2. Perform a write action that already calls `AuditLogService`, such as updating a product, warehouse, user, receipt, transfer, or stocktake.
3. Log in as `ADMIN`.
4. Call `GET /api/v1/admin/audit-logs?page=1`.
5. Verify the newest audit row includes actor, actor role, action, timestamp, description, changed fields, and warehouse when applicable.

### 2. Non-admin cannot query audit logs

1. Log in as `CEO` or another non-admin role.
2. Call `GET /api/v1/admin/audit-logs`.
3. Expect HTTP 403 with `FORBIDDEN_AUDIT_ACCESS`.

### 3. Default pagination

1. Log in as `ADMIN`.
2. Call `GET /api/v1/admin/audit-logs` without filters.
3. Verify `page = 1`, `pageSize = 30`, newest-first ordering, and page metadata.
4. Call `GET /api/v1/admin/audit-logs?page=2`.
5. Verify the backend returns page 2 data rather than the frontend slicing page 1 locally.

### 4. Unfiltered browse limit

1. Log in as `ADMIN`.
2. Call `GET /api/v1/admin/audit-logs?page=51`.
3. Expect HTTP 400 with `QUERY_RANGE_TOO_LARGE`.

### 5. Date and warehouse filters

1. Log in as `ADMIN`.
2. Call `GET /api/v1/admin/audit-logs?from=2026-07-22T08:00:00+07:00&to=2026-07-22T09:00:00+07:00`.
3. Verify matching rows are returned newest first.
4. Call `GET /api/v1/admin/audit-logs?warehouse_id=1`.
5. Verify all returned rows belong to warehouse 1 or have the expected nullable warehouse behavior defined by the implementation.

### 6. Invalid date range

1. Call `GET /api/v1/admin/audit-logs?from=2026-07-22T09:00:00+07:00&to=2026-07-22T08:00:00+07:00`.
2. Expect HTTP 400 with `INVALID_DATE_RANGE`.
3. Call a range shorter than 1 hour.
4. Expect HTTP 400 with `INVALID_DATE_RANGE`.

### 7. Nullable entity references

1. Create or seed a non-entity audit event such as login/logout.
2. Verify `entityType` and `entityId` may be null in list and detail responses.
3. Verify `oldValue` and `newValue` are empty.

### 8. Read-only views are not audited

1. Record current audit row count.
2. Open ordinary list/detail screens, dashboards, reports, and export a read-only report if available.
3. Verify no new audit rows are created for those read-only actions.

## Automated Checks

```powershell
mvn -f backend/pom.xml test -Dtest=AuditLogServiceTest,AuditLogControllerTest,AuditLogUtilTest
mvn -f backend/pom.xml compile
```

Run frontend Jest tests only if the audit log UI pagination or filter controls are changed.
