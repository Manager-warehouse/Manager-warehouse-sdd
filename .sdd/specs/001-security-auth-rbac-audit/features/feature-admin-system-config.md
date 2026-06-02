# Feature: System Admin Cấu hình Tham số & Định mức Phê duyệt động (US-WMS-01)

## 1. Context and Goal
Hệ thống WMS cần vận hành linh hoạt với các tham số cấu hình động như hạn mức công nợ mặc định, tồn kho tối thiểu, kỳ hạn thanh toán, và các định mức phê duyệt điều chỉnh kho (Maker-Checker). System Admin quản lý các cấu hình này thông qua một giao diện cấu hình tập trung.

## 2. Actors
* **System Admin**: Người thiết lập và cấu hình các tham số hệ thống.
* **CEO**: Người duyệt các thay đổi vượt định mức phê duyệt của Trưởng kho.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a System Admin updates a system configuration parameter, the system SHALL validate the format and check the actor's permissions.
  * WHEN a system configuration parameter is successfully updated, the system SHALL record the previous value, new value, and actor identity in the audit log.

## 4. API Endpoints
* `GET /api/v1/admin/system-config` - Xem danh sách tham số cấu hình.
* `PUT /api/v1/admin/system-config` - Cập nhật tham số cấu hình.

## 5. Acceptance Criteria

**Scenario: System Config Change**
* Given a user with `SYSTEM_ADMIN` role
* When they update a system configuration parameter (e.g. default credit limit to 800M)
* Then the system SHALL update the parameter successfully and record the old and new values in the audit log.
