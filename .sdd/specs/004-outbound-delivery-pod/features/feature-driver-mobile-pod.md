# Feature: Tài xế Xác nhận Giao hàng bằng OTP (US-WMS-09)

## 1. Context and Goal
Tài xế đăng nhập bằng tài khoản riêng trên giao diện web responsive bằng smartphone để xem chuyến xe, chỉ dẫn giao hàng và thực hiện xác nhận giao hàng bằng OTP do hệ thống gửi cho đại lý/người nhận. Nếu giao thất bại, hàng được chuyển vào Quarantine.

## 2. Actors
* **Tài xế**: Nhận chuyến, xem lộ trình, gửi/nhập OTP xác nhận giao hàng hoặc báo cáo giao thất bại.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL require JWT authentication for all Driver Mobile & OTP endpoints, following [001-security-auth-rbac-audit](../../001-security-auth-rbac-audit/spec.md).
  * The system SHALL authorize a Driver to view and update only trips assigned to their own `driver_id`.
  * The system SHALL create `OTP_REQUEST`, `OTP_CONFIRM`, or `DELIVERY_FAIL` audit log entries for every delivery confirmation or failure report.
* **Event-driven:**
  * WHEN a Tài xế requests delivery confirmation, the system SHALL:
    * Generate an OTP, store a hash, set an expiry, and send the OTP to the dealer/receiver contact.
  * WHEN a Tài xế confirms delivery using a valid OTP, the system SHALL:
    * Create a `deliveries` record with status `DELIVERED`, saving the OTP verification timestamp and recipient contact metadata.
    * Update DO status to `DELIVERED`.
    * Notify the Kế toán viên to create an invoice.
  * WHEN a Tài xế reports delivery failure, the system SHALL:
    * Create a `deliveries` record with status `RETURNED` and save the `failure_reason`.
    * Update DO status to `RETURNED`.
    * Automatically create a quarantine receipt (`receipts.type = 'RETURN'`) to put returned goods into Quarantine.

## 4. API Endpoints
* `GET /api/v1/trips/{id}` - Xem thông tin chuyến xe được gán (Driver mobile view).
* `POST /api/v1/trips/{id}/delivery-confirmation/otp/request` - Gửi OTP cho đại lý/người nhận.
* `POST /api/v1/trips/{id}/delivery-confirmation/otp/verify` - Xác nhận giao thành công bằng OTP.
* `PUT /api/v1/trips/{id}/fail-delivery` - Xác nhận giao thất bại (gửi kèm lý do lỗi).

## 5. Acceptance Criteria
* **Scenario: Driver can only access assigned trip**
  * Given a Driver is authenticated with a valid JWT
  * When they request a trip assigned to a different driver_id
  * Then the system SHALL reject the request with `403 FORBIDDEN`.

* **Scenario: Delivery success with OTP**
  * Given a DO in status `IN_TRANSIT`
  * When Driver requests OTP, receives it from the dealer/receiver, and submits a valid OTP
  * Then the system SHALL verify the OTP, change status to `DELIVERED`, and trigger a billing notification.
