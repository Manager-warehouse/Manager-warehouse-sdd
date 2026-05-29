# Admin & Users API — /api/v1/admin

## GET /api/v1/admin/users
**Query**: ?page=0&size=20&search=keyword
**Headers**: Authorization: Bearer {token} (requires SYSTEM_ADMIN)
**Response 200**: `{ "content": [...], "totalElements", "totalPages" }`

## POST /api/v1/admin/users
**Request**: `{ "username", "password", "full_name", "email", "role_ids": [...], "warehouse_ids": [...] }`
**Response 201**: User object

## PUT /api/v1/admin/users/{id}
**Request**: `{ "full_name", "email", "is_active", "role_ids": [...], "warehouse_ids": [...] }`
**Response 200**: Updated user

## GET /api/v1/admin/roles
**Response 200**: List of all roles

## GET /api/v1/admin/warehouses
**Response 200**: List of warehouses

## POST /api/v1/admin/warehouses
**Request**: `{ "code", "name", "address" }`
**Response 201**: Warehouse object

## GET /api/v1/admin/system-config
**Response 200**: System config parameters (approval thresholds, default payment terms, etc.)

## PUT /api/v1/admin/system-config
**Request**: `{ "key": "value", ... }`
**Response 200**: Updated config
