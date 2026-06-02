# API Contract: Audit Logging

**Base URL**: `/api/v1`
**Date**: 2026-06-03

---

## GET /api/v1/audit-logs

Tra cứu nhật ký hoạt động hệ thống với cursor-based pagination.

### Authorization

Roles: `ADMIN`, `CEO`, `WAREHOUSE_MANAGER`, `ACCOUNTANT`

**RBAC Rules:**
- `ADMIN`, `CEO`: Full access — xem toàn bộ log.
- `WAREHOUSE_MANAGER`: Auto-filter `warehouse_id` theo kho được phân công.
- `ACCOUNTANT`: Full access — xem toàn bộ log.

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| cursor | Long | No | null | ID of last record from previous page. Null = first page |
| size | Integer | No | 30 | Number of records per page. Min: 1, Max: 100 |
| actorId | Long | No | null | Filter by actor user ID |
| entityType | String | No | null | Filter by entity type (e.g., RECEIPT, TRANSFER) |
| action | String | No | null | Filter by action (e.g., CREATE, APPROVE) |
| warehouseId | Long | No | null | Filter by warehouse ID |
| startDate | LocalDate (yyyy-MM-dd) | No | today - 7 days | Start date inclusive (00:00:00) |
| endDate | LocalDate (yyyy-MM-dd) | No | today | End date inclusive (23:59:59.999999) |

### Response: 200 OK

```json
{
  "data": [
    {
      "id": 1234,
      "actorId": 5,
      "actorName": "Nguyễn Văn A",
      "actorRole": "STOREKEEPER",
      "action": "APPROVE",
      "entityType": "RECEIPT",
      "entityId": 101,
      "description": "APPROVE RECEIPT PN-2026-001",
      "warehouseId": 2,
      "oldValue": {"status": "PENDING_APPROVAL"},
      "newValue": {"status": "APPROVED"},
      "timestamp": "2026-06-02T14:30:00+07:00",
      "ipAddress": "192.168.1.100"
    }
  ],
  "nextCursor": 1233,
  "hasNext": true
}
```

### Response: 403 Forbidden

```json
{
  "error": "FORBIDDEN_ROLE",
  "message": "You do not have permission to access audit logs",
  "status": 403
}
```

### Cursor-based Pagination Logic

1. Client sends first request without `cursor`.
2. Backend returns `data` sorted by `timestamp DESC` (effectively `id DESC`), `nextCursor` = last entry's `id`, `hasNext` = true/false.
3. Client sends next request with `cursor = nextCursor`.
4. Backend queries: `WHERE id < :cursor ORDER BY id DESC LIMIT :size`.
5. Repeat until `hasNext = false`.

### Date Range Backend Processing

```java
// Default: last 7 days if no dates provided
LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(7);
LocalDate end = (endDate != null) ? endDate : LocalDate.now();

// Convert to timestamps for query
OffsetDateTime startTimestamp = start.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
OffsetDateTime endTimestamp = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toOffsetDateTime();
```

---

## Internal Contract: AuditLogService.log()

This is the internal service method called by AOP or service layer to create audit entries.

### Method Signature

```java
void log(AuditAction action,
         String entityType,
         Long entityId,
         String entityCode,
         Long warehouseId,
         Map<String, Object> oldValue,
         Map<String, Object> newValue);
```

### Behavior

- Resolves `actor` and `actorRole` from SecurityContext (current authenticated user).
- Auto-generates `description`: `"{action} {entityType} {entityCode}"`.
- Resolves `ipAddress` from `HttpServletRequest`.
- Filters out sensitive fields from `oldValue` / `newValue` before persisting.
- Persists `AuditLog` entity.

### Sensitive Fields Blacklist

```java
private static final Set<String> SENSITIVE_FIELDS = Set.of(
    "passwordHash", "password_hash", "password", "refreshToken", "accessToken"
);
```
