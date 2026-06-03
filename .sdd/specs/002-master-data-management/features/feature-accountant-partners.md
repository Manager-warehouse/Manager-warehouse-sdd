# Feature: Kế toán trưởng Quản lý Đối tác & Hạn mức Tín dụng (US-WMS-22)

## 1. Context and Goal

Quản lý danh mục Đại lý và Nhà cung cấp, bao gồm hạn mức tín dụng (Credit Limit) và kỳ hạn thanh toán (Net 30/60) làm nền tảng cho luồng bán hàng và mua hàng. Kế toán trưởng quản lý chặt chẽ hạn mức tín dụng của từng đại lý để tránh rủi ro nợ xấu.

## 2. Actors

- **Kế toán viên**: Tạo mới, cập nhật và quản lý hồ sơ Nhà cung cấp.
- **Kế toán trưởng**: Thiết lập, thay đổi hạn mức tín dụng (Credit Limit) của từng Đại lý.

## 3. Functional Requirements (EARS)

- **Event-driven:**
  - WHEN Kế toán trưởng updates a dealer's credit_limit, the system SHALL record this action with values before and after in audit logs.
- **State-driven:**
  - WHILE a dealer is `is_active = false`, the system SHALL prevent creating new delivery orders referencing that dealer.

## 4. API Endpoints

- `GET /api/v1/dealers` - Danh sách Đại lý.
- `POST /api/v1/dealers` - Tạo mới Đại lý.
- `PUT /api/v1/dealers/{id}/credit-limit` - Cập nhật hạn mức tín dụng (Credit Limit).
- `GET /api/v1/suppliers` - Danh sách Nhà cung cấp.
- `POST /api/v1/suppliers` - Tạo mới Nhà cung cấp.

## 5. Acceptance Criteria

- **Scenario: Change credit limit**
  - Given a dealer with `credit_limit = 500M` and `current_balance = 0`
  - When `ACCOUNTANT_MANAGER` sets credit_limit to `800M`
  - Then the system SHALL update successfully and record the change in the audit log.
