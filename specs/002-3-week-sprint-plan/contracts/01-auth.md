# Auth API — /api/v1/auth

## POST /api/v1/auth/login
**Request**: `{ "username": "string", "password": "string" }`
**Response 200**: `{ "access_token": "string", "refresh_token": "string", "expires_in": 900 }`
**Response 401**: `{ "error": "Invalid credentials" }`

## POST /api/v1/auth/refresh
**Request**: `{ "refresh_token": "string" }`
**Response 200**: `{ "access_token": "string", "expires_in": 900 }`
**Response 401**: `{ "error": "Invalid or expired refresh token" }`

## POST /api/v1/auth/logout
**Headers**: Authorization: Bearer {token}
**Response 200**: `{ "message": "Logged out" }`

## GET /api/v1/auth/me
**Headers**: Authorization: Bearer {token}
**Response 200**: `{ "id", "username", "full_name", "roles": [...], "warehouses": [...] }`
