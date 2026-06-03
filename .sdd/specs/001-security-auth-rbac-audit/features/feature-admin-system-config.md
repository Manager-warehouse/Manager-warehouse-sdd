# Feature: System Admin Cấu hình Tham số (US-WMS-01)

## 1. Context and Goal
Hệ thống WMS cần vận hành linh hoạt với các tham số cấu hình động bao gồm: Hạn mức công nợ mặc định (`DEFAULT_CREDIT_LIMIT`), Kỳ hạn thanh toán (`DEFAULT_PAYMENT_TERM_DAYS`), Số ngày quá hạn khóa tín dụng (`CREDIT_HOLD_OVERDUE_DAYS`), Ngưỡng mở khóa tín dụng (`CREDIT_UNLOCK_BUFFER_PCT`), Ngày khóa sổ kỳ kế toán (`MONTHLY_CLOSING_DAY`), và Ngưỡng cảnh báo tồn kho tối thiểu (`MIN_INVENTORY_WARNING_THRESHOLD`). System Admin quản lý các cấu hình này thông qua một giao diện tập trung.

## 2. Actors
* **System Admin**: Người thiết lập và cấu hình các tham số hệ thống.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a System Admin updates a system configuration parameter, the system SHALL validate the format and value based on the `config_key`:
    * `DEFAULT_CREDIT_LIMIT`: Phải là số dương (`> 0`).
    * `DEFAULT_PAYMENT_TERM_DAYS`: Phải là số nguyên dương (`> 0`).
    * `CREDIT_HOLD_OVERDUE_DAYS`: Phải là số nguyên dương (`> 0`).
    * `CREDIT_UNLOCK_BUFFER_PCT`: Phải là số thập phân, lớn hơn `0` và nhỏ hơn hoặc bằng `1`.
    * `MONTHLY_CLOSING_DAY`: Phải là số nguyên từ `1` đến `31`.
    * `MIN_INVENTORY_WARNING_THRESHOLD`: Phải là số nguyên lớn hơn hoặc bằng `0`.
  * WHEN a system configuration parameter is successfully updated, the system SHALL record the previous value, new value, and actor identity in the audit log.

## 4. API Endpoints
* `GET /api/v1/admin/system-config` - Xem danh sách toàn bộ tham số cấu hình.
* `PUT /api/v1/admin/system-config/{config_key}` - Cập nhật giá trị của một tham số cấu hình cụ thể (Payload chứa `config_value`).

## 5. Acceptance Criteria

**Scenario: System Config Change**
* Given a user with `ADMIN` role
* When they update a system configuration parameter (e.g. default credit limit to 800M)
* Then the system SHALL update the parameter successfully and record the old and new values in the audit log.
