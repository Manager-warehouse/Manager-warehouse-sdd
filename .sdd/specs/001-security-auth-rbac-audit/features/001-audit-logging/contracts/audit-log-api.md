# API Contract: System Audit Logging

**Base URL**: `/api/v1`
**Date**: 2026-06-03

---

## GET /api/v1/audit-logs

Returns audit logs for the Audit Trail tab in `UserManagement`.

### Authorization

Allowed role: `ADMIN`

Forbidden roles: `CEO`, `WAREHOUSE_MANAGER`, `STOREKEEPER`, `WAREHOUSE_STAFF`, `ACCOUNTANT`, `ACCOUNTANT_MANAGER`, `PLANNER`, `DISPATCHER`, `DRIVER`, `REPORT_VIEWER`

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | Integer | No | 1 | 1-based page number |
| pageSize | Integer | No | 30 | Fixed default page size. Max 30 for this UI |
| from | ISO date/datetime | No | null | Start timestamp/date inclusive |
| to | ISO date/datetime | No | null | End timestamp/date inclusive |
| warehouseId | Long | No | null | Optional warehouse filter |

### Rules

- Results are always sorted by `timestamp DESC`.
- Without `from`, `to`, or `warehouseId`, the API allows page 1 through page 50.
- If `page > 50` and no filter is provided, return `QUERY_RANGE_TOO_LARGE`.
- If `from` is after `to`, return `INVALID_DATE_RANGE`.
- Read/list/export actions are not audit events and do not create audit rows.

### Response: 200 OK

```json
{
  "data": [
    {
      "id": 1234,
      "timestamp": "2026-06-03T10:30:00+07:00",
      "actorId": 5,
      "actorName": "Nguyễn Văn Admin",
      "actorRole": "ADMIN",
      "action": "UPDATE",
      "entityType": "User",
      "entityId": 101,
      "description": "UPDATE User 101",
      "warehouseId": 2,
      "warehouseCode": "HP-01"
    }
  ],
  "page": 1,
  "pageSize": 30,
  "hasNext": true,
  "hasPrevious": false,
  "requiresFilterForOlder": false
}
```

### Response: 400 INVALID_DATE_RANGE

```json
{
  "error": "INVALID_DATE_RANGE",
  "message": "from must be before or equal to to",
  "status": 400
}
```

### Response: 400 QUERY_RANGE_TOO_LARGE

```json
{
  "error": "QUERY_RANGE_TOO_LARGE",
  "message": "Use time or warehouse filters to search older audit logs",
  "status": 400
}
```

### Response: 403 FORBIDDEN_AUDIT_ACCESS

```json
{
  "error": "FORBIDDEN_AUDIT_ACCESS",
  "message": "Only System Admin can access audit logs",
  "status": 403
}
```

---

## GET /api/v1/audit-logs/{id}

Returns detail for a single audit log row.

### Authorization

Allowed role: `ADMIN`

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | Long | Yes | Audit log ID |

### Response: 200 OK

```json
{
  "id": 1234,
  "timestamp": "2026-06-03T10:30:00+07:00",
  "actorId": 5,
  "actorName": "Nguyễn Văn Admin",
  "actorRole": "ADMIN",
  "action": "UPDATE",
  "entityType": "User",
  "entityId": 101,
  "description": "UPDATE User 101",
  "warehouseId": 2,
  "warehouseCode": "HP-01",
  "oldValue": {
    "fullName": "Nguyễn Văn A"
  },
  "newValue": {
    "fullName": "Nguyễn Văn B"
  },
  "ipAddress": "192.168.1.100"
}
```

### Response: 404 AUDIT_LOG_NOT_FOUND

```json
{
  "error": "AUDIT_LOG_NOT_FOUND",
  "message": "Audit log entry does not exist",
  "status": 404
}
```

---

## Internal Contract: AuditLogService

`AuditLogService` is a Spring singleton `@Service`. Domain services call it after successful user-driven state changes.

### Method Shape

```java
void log(AuditAction action,
         String entityType,
         Long entityId,
         String description,
         Long warehouseId,
         Map<String, Object> oldValue,
         Map<String, Object> newValue);
```

### Behavior

- Resolve actor and role from `SecurityContext`.
- Store only changed fields in `oldValue` and `newValue`.
- Omit sensitive fields from both maps.
- Save exactly one append-only `AuditLog` row per logged action.
- Do not expose update/delete operations.
