# Common API Conventions

## Base URL: `/api/v1`

## Authentication
- All endpoints except /auth/login and /auth/refresh require:
  `Authorization: Bearer {access_token}`

## Pagination
- Page parameter: 0-based
- Default: page=0, size=20
- Max page size: 100
- Response format: `{ "content": [...], "totalElements": N, "totalPages": N, "number": N, "size": N }`

## Error Response Format
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2026-05-29T10:00:00Z",
  "path": "/api/v1/receipts",
  "details": {
    "field": "warehouse_id",
    "reason": "must not be null"
  }
}
```

## Standard HTTP Status Codes
| Code | Usage |
|------|-------|
| 200 | Success (GET, PUT) |
| 201 | Created (POST) |
| 204 | No Content (DELETE) |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (invalid/missing token) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Not Found |
| 409 | Conflict (version mismatch, duplicate) |
| 422 | Unprocessable Entity (business rule violation) |
| 500 | Internal Server Error |

## Common Headers
- Request: `Content-Type: application/json`, `Authorization: Bearer {token}`
- Response: `X-Total-Count`, `X-Page-Number`, `X-Page-Size`

## Audit Log Convention
Moi endpoint modify data phai tao audit log:
- actor: current user (from JWT)
- action: "{ENTITY}_{ACTION}" (e.g., "RECEIPT_APPROVED")
- before_state: JSON truoc khi thay doi
- after_state: JSON sau khi thay doi
