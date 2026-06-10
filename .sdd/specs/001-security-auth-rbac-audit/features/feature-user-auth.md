# Feature: Xác thực Người dùng (Authentication)

## 1. Context and Goal
Đảm bảo tất cả người dùng trong hệ thống phải được xác thực danh tính an toàn thông qua JWT. Hệ thống bảo vệ tất cả các API nghiệp vụ và chỉ cho phép người dùng đăng nhập hợp lệ truy cập.

## 2. Actors
* **Mọi người dùng**: Nhân viên thực hiện đăng nhập và sử dụng hệ thống.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always require authentication via JWT (access + refresh token) for all endpoints except `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/forgot-password`, and `POST /api/v1/auth/verify-otp`.
  * The system SHALL always hash user passwords using bcrypt with a cost factor ≥ 12.
  * The system SHALL always issue an access token with TTL of **15 minutes** and a refresh token with TTL of **7 days**.
* **State-driven:**
  * WHILE a user account is inactive (`is_active = false`), the system SHALL reject all authentication attempts with HTTP 401.
* **Event-driven:**
  * WHEN a user calls `POST /api/v1/auth/logout`, the system SHALL invalidate the provided refresh token so it cannot be reused.
  * WHEN a refresh token is used to obtain a new access token, the system SHALL verify the token exists in the server-side store and has not been invalidated.
  * WHEN a user requests a password reset, the system SHALL generate a 6-digit OTP, store it hashed server-side with a TTL of **10 minutes**, and send it to the user's registered email via Gmail SMTP.
  * WHEN an OTP has been used successfully to reset a password, the system SHALL immediately invalidate that OTP so it cannot be reused.
  * WHEN a user changes their password, the system SHALL verify the current password before applying the new one.

## 4. Token & OTP Storage

Refresh tokens and OTP codes are persisted server-side on the `users` table. This enables revocation on logout and single-use OTP validation without an external store.

| Column | Type | Notes |
|---|---|---|
| `refresh_token_hash` | VARCHAR(255) | SHA-256 hash of the raw refresh token. NULL = no active session. |
| `refresh_token_expires_at` | TIMESTAMPTZ | Expiry timestamp of the current refresh token. |
| `otp_hash` | VARCHAR(255) | SHA-256 hash of the 6-digit OTP. NULL = no pending reset. |
| `otp_expires_at` | TIMESTAMPTZ | Expiry timestamp of the OTP (TTL 10 minutes). |

> **Note:** These four columns must be added to the `users` table defined in `spec.md`.

## 5. API Endpoints

### POST /api/v1/auth/login
Đăng nhập bằng email/password, nhận JWT tokens.

**Request body:**
```json
{
  "email": "string (required)",
  "password": "string (required)"
}
```

**Response 200 OK:**
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": 1,
    "fullName": "string",
    "email": "string",
    "role": "STOREKEEPER",
    "assignedWarehouses": [
      { "id": 1, "name": "Hà Nội" }
    ]
  }
}
```

**Errors:** `401 INVALID_CREDENTIALS`, `401 ACCOUNT_INACTIVE`, `400 VALIDATION_ERROR`

---

### POST /api/v1/auth/refresh
Làm mới access token bằng refresh token còn hiệu lực.

**Request body:**
```json
{
  "refreshToken": "string (required)"
}
```

**Response 200 OK:**
```json
{
  "accessToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Errors:** `401 TOKEN_EXPIRED`, `401 TOKEN_INVALID`

---

### POST /api/v1/auth/logout
Đăng xuất, vô hiệu hóa refresh token hiện tại. Yêu cầu access token hợp lệ trong header.

**Request header:** `Authorization: Bearer <accessToken>`

**Response:** `204 No Content`

**Errors:** `401 TOKEN_INVALID`

---

### GET /api/v1/auth/me
Xem thông tin người dùng hiện tại. Yêu cầu access token hợp lệ trong header.

**Request header:** `Authorization: Bearer <accessToken>`

**Response 200 OK:**
```json
{
  "id": 1,
  "code": "string",
  "fullName": "string",
  "email": "string",
  "phone": "string",
  "role": "STOREKEEPER",
  "jobTitle": "string",
  "assignedWarehouses": [
    { "id": 1, "name": "Hà Nội" }
  ]
}
```

**Errors:** `401 TOKEN_EXPIRED`, `401 TOKEN_INVALID`

---

### POST /api/v1/auth/forgot-password
Gửi OTP 6 chữ số về email đã đăng ký. Không yêu cầu đăng nhập.

**Request body:**
```json
{
  "email": "string (required)"
}
```

**Response 200 OK:**
```json
{
  "message": "OTP đã được gửi đến email của bạn."
}
```

> Luôn trả về 200 dù email không tồn tại trong hệ thống (tránh email enumeration attack).

**Errors:** `400 VALIDATION_ERROR`

---

### POST /api/v1/auth/verify-otp
Xác minh OTP và đặt mật khẩu mới. Không yêu cầu đăng nhập.

**Request body:**
```json
{
  "email": "string (required)",
  "otp": "string (required, 6 digits)",
  "newPassword": "string (required)"
}
```

**Response 200 OK:**
```json
{
  "message": "Mật khẩu đã được đặt lại thành công."
}
```

**Errors:** `400 OTP_INVALID`, `400 OTP_EXPIRED`, `400 VALIDATION_ERROR`

---

### PUT /api/v1/auth/change-password
User tự đổi mật khẩu khi đang đăng nhập. Yêu cầu access token hợp lệ trong header.

**Request header:** `Authorization: Bearer <accessToken>`

**Request body:**
```json
{
  "currentPassword": "string (required)",
  "newPassword": "string (required)"
}
```

**Response:** `204 No Content`

**Errors:** `401 INVALID_CREDENTIALS` (sai mật khẩu hiện tại), `400 VALIDATION_ERROR`

---

## 6. Acceptance Criteria

**Scenario 1: Đăng nhập thành công**
* Given một user có tài khoản active và mật khẩu đúng
* When họ gọi `POST /api/v1/auth/login` với đúng email/password
* Then hệ thống trả về HTTP 200 kèm accessToken (hết hạn sau 15 phút), refreshToken (hết hạn sau 7 ngày), và thông tin user gồm role và danh sách kho được gán.

**Scenario 2: Sai mật khẩu**
* Given một user có tài khoản active
* When họ gọi `POST /api/v1/auth/login` với mật khẩu sai
* Then hệ thống trả về HTTP 401 với error code `INVALID_CREDENTIALS`.

**Scenario 3: Tài khoản bị vô hiệu hóa**
* Given một user có `is_active = false`
* When họ gọi `POST /api/v1/auth/login` với bất kỳ mật khẩu nào
* Then hệ thống trả về HTTP 401 với error code `ACCOUNT_INACTIVE`.

**Scenario 4: Làm mới access token thành công**
* Given một refresh token còn hiệu lực được lưu server-side
* When user gọi `POST /api/v1/auth/refresh` với token đó
* Then hệ thống trả về HTTP 200 với accessToken mới (TTL 15 phút).

**Scenario 5: Refresh token hết hạn**
* Given một refresh token đã quá 7 ngày
* When user gọi `POST /api/v1/auth/refresh`
* Then hệ thống trả về HTTP 401 với error code `TOKEN_EXPIRED`.

**Scenario 6: Đăng xuất và vô hiệu hóa token**
* Given một user đang đăng nhập với refresh token hợp lệ
* When họ gọi `POST /api/v1/auth/logout` với access token trong header
* Then hệ thống trả về HTTP 204 và xóa `refresh_token_hash` khỏi DB.
* And khi gọi lại `POST /api/v1/auth/refresh` với refresh token cũ → HTTP 401 `TOKEN_INVALID`.

**Scenario 7: Gọi API nghiệp vụ không có token**
* Given một request tới bất kỳ endpoint nào ngoài `/auth/login` và `/auth/refresh`
* When request không có header `Authorization`
* Then hệ thống trả về HTTP 401.

**Scenario 8: Xem thông tin người dùng hiện tại**
* Given một user đang đăng nhập với access token hợp lệ
* When họ gọi `GET /api/v1/auth/me`
* Then hệ thống trả về HTTP 200 với thông tin user đầy đủ gồm role và danh sách kho được gán.

**Scenario 9: Quên mật khẩu — gửi OTP thành công**
* Given một user có tài khoản active với email đã đăng ký
* When họ gọi `POST /api/v1/auth/forgot-password` với email đó
* Then hệ thống gửi email chứa OTP 6 chữ số (hết hạn sau 10 phút) và trả về HTTP 200.

**Scenario 10: Quên mật khẩu — email không tồn tại**
* Given một email không có trong hệ thống
* When gọi `POST /api/v1/auth/forgot-password`
* Then hệ thống vẫn trả về HTTP 200 (không lộ thông tin tài khoản).

**Scenario 11: Đặt lại mật khẩu với OTP hợp lệ**
* Given một OTP còn hiệu lực đã được gửi về email
* When user gọi `POST /api/v1/auth/verify-otp` với đúng email, OTP và mật khẩu mới
* Then hệ thống cập nhật mật khẩu, vô hiệu hóa OTP, và trả về HTTP 200.

**Scenario 12: Đặt lại mật khẩu với OTP hết hạn**
* Given một OTP đã quá 10 phút
* When user gọi `POST /api/v1/auth/verify-otp`
* Then hệ thống trả về HTTP 400 với error code `OTP_EXPIRED`.

**Scenario 13: Đặt lại mật khẩu với OTP sai**
* Given một OTP không khớp
* When user gọi `POST /api/v1/auth/verify-otp`
* Then hệ thống trả về HTTP 400 với error code `OTP_INVALID`.

**Scenario 14: Đổi mật khẩu thành công**
* Given một user đang đăng nhập với access token hợp lệ
* When họ gọi `PUT /api/v1/auth/change-password` với đúng mật khẩu hiện tại và mật khẩu mới
* Then hệ thống cập nhật mật khẩu và trả về HTTP 204.

**Scenario 15: Đổi mật khẩu sai mật khẩu hiện tại**
* Given một user đang đăng nhập
* When họ gọi `PUT /api/v1/auth/change-password` với mật khẩu hiện tại sai
* Then hệ thống trả về HTTP 401 với error code `INVALID_CREDENTIALS`.
