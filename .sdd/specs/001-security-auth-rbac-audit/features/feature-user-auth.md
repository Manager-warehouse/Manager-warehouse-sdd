# Feature: Xác thực Người dùng (Authentication)

## 1. Context and Goal
Đảm bảo tất cả người dùng trong hệ thống phải được xác thực danh tính an toàn thông qua JWT. Hệ thống bảo vệ tất cả các API nghiệp vụ và chỉ cho phép người dùng đăng nhập hợp lệ truy cập.

## 2. Actors
* **Mọi người dùng**: Nhân viên thực hiện đăng nhập và sử dụng hệ thống.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always require authentication via JWT (access + refresh token) for all endpoints except `/api/v1/auth/login` and `/api/v1/auth/refresh`.
  * The system SHALL always hash user passwords using bcrypt with a cost factor ≥ 12.
* **State-driven:**
  * WHILE a user account is inactive (`is_active = false`), the system SHALL reject all authentication attempts.

## 4. API Endpoints
* `POST /api/v1/auth/login` - Đăng nhập bằng email/password và nhận JWT tokens.
* `POST /api/v1/auth/refresh` - Làm mới access token bằng refresh token.
* `POST /api/v1/auth/logout` - Đăng xuất và vô hiệu hóa tokens.
* `GET /api/v1/auth/me` - Xem thông tin người dùng hiện tại đang đăng nhập.

## 5. Acceptance Criteria

**Scenario: Standard User Login**
* Given a user with an active account and correct password
* When they call `POST /api/v1/auth/login` with their credentials
* Then the system SHALL return HTTP 200 OK along with an access token, refresh token, and user details (role, assigned warehouses).
