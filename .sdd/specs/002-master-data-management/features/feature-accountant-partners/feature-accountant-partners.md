# Feature: Kế toán trưởng Quản lý Đối tác & Hạn mức Tín dụng (US-WMS-22)

## 1. Context and Goal

Quản lý danh mục Đại lý và Nhà cung cấp, bao gồm hạn mức tín dụng (Credit Limit) và kỳ hạn thanh toán (Net 30/60) làm nền tảng cho luồng bán hàng và mua hàng. Kế toán trưởng quản lý chặt chẽ hạn mức tín dụng của từng đại lý để tránh rủi ro nợ xấu.

## 2. Actors

- **Kế toán viên**: Tạo mới, cập nhật, quản lý hồ sơ, và soft-delete hồ sơ Đại lý/Nhà cung cấp.
- **Kế toán trưởng**: Thiết lập, thay đổi hạn mức tín dụng (Credit Limit), kỳ hạn thanh toán (`payment_term_days` = Net 30/Net 60), và quản lý trạng thái khóa tín dụng (`credit_status`) của từng Đại lý khi cần can thiệp thủ công.

## 3. Functional Requirements (EARS)

- **Ubiquitous:**
  - The system SHALL allow `ACCOUNTANT` to create, update, view, and soft-delete Dealer and Supplier master data.
  - The system SHALL allow only `ACCOUNTANT_MANAGER` to update a dealer's `credit_limit`, `payment_term_days`, and manually controlled `credit_status`.
  - Dealer `credit_limit` SHALL be greater than `0` and greater than the dealer's current `current_balance`.
  - Dealer `payment_term_days` SHALL allow only `30` or `60`, representing Net 30 and Net 60 payment terms.
  - Dealer `credit_status` SHALL default to `ACTIVE`; the system SHALL set it automatically based on credit rules, and `ACCOUNTANT_MANAGER` SHALL be able to set it manually when needed.
  - The system SHALL allow new Dealer transactions when `current_balance + transaction_amount <= credit_limit`.
  - The system SHALL automatically reject creation of new Dealer transactions when the new transaction would cause `current_balance + transaction_amount > credit_limit`, set the dealer's `credit_status` to `CREDIT_HOLD`, and notify the transaction creator with the credit-limit reason.
  - The system SHALL soft-delete Dealers and Suppliers by setting `is_active = false`; the system SHALL NOT physically delete Dealer or Supplier business records.
  - WHILE a Dealer has `is_active = false`, the system SHALL block all new outbound delivery-order and dealer transaction creation flows.
  - WHILE a Dealer has `is_active = false`, the system SHALL prevent all new activity for that dealer regardless of `credit_status`.
  - The system SHALL record an audit log entry for every successful create, update, credit-limit change, credit-status change, and soft-delete action on Dealer or Supplier master data.
  - Audit log entries SHALL follow the system audit-log rules and include `actor_id`, `actor_role`, `action`, `entity_type`, `entity_id`, `timestamp`, and field-level before/after changed values.
- **Event-driven:**
  - WHEN Kế toán trưởng updates a dealer's credit_limit, the system SHALL record this action with values before and after in audit logs.
  - WHEN Kế toán trưởng updates a dealer's `payment_term_days`, the system SHALL record this action with values before and after in audit logs.
  - WHEN a new Dealer transaction would exceed the dealer's `credit_limit`, the system SHALL reject only that transaction before creation, set the dealer's `credit_status` to `CREDIT_HOLD`, and notify the creator.
  - WHEN Kế toán trưởng manually sets a dealer's `credit_status` to `CREDIT_HOLD`, the system SHALL block creation of new Dealer transactions without blocking non-transaction profile management.
  - WHEN Kế toán viên soft-deletes a Supplier, the system SHALL keep historical supplier orders and receipts available for viewing.
- **State-driven:**
  - WHILE a dealer is `is_active = false`, the system SHALL prevent creating new delivery orders referencing that dealer.
  - WHILE a supplier is `is_active = false`, the system SHALL prevent profile use as an active supplier in internal supplier-management screens while keeping historical orders readable.

## 4. Supplier Business Rules

- Dealer profile management SHALL follow the current database schema and support dealer identity/contact fields used by delivery and finance operations: `code`, `name`, `phone`, `default_delivery_address`, `region`, and `is_active`.
- Dealer `code` SHALL be unique.
- Dealer creation by `ACCOUNTANT` SHALL initialize credit fields from system defaults and entity defaults: `payment_term_days` from `DEFAULT_PAYMENT_TERM_DAYS`, `credit_limit` from `DEFAULT_CREDIT_LIMIT`, `current_balance = 0`, and `credit_status = ACTIVE`.
- Normal Dealer profile create/update requests SHALL NOT expose `credit_limit`, `payment_term_days`, `current_balance`, or `credit_status` as accountant-editable profile fields; those credit-control fields are managed by `ACCOUNTANT_MANAGER` endpoints or system finance flows.
- Supplier profile management SHALL follow the current database schema and support supplier identity fields needed for procurement and inbound operations: `code`, `company_name`, `tax_code`, `phone`, `address`, `contact_person`, and `is_active`.
- Supplier `code` SHALL be unique.
- Supplier `tax_code` SHALL be optional and SHALL NOT require uniqueness because the current database schema does not define a unique constraint for it.
- Supplier management SHALL expose read-only access to orders/receipts already received from that supplier and their detail records.
- Supplier deactivation SHALL NOT alter historical purchase order, receipt, QC, debit-note, or audit records.
- Supplier reactivation SHALL restore the supplier profile to active status and SHALL NOT alter historical purchase order, receipt, QC, debit-note, or audit records.
- Changes to supplier identity, tax, contact, active status, or payment-related profile fields SHALL create audit logs with before/after values.

## 5. API Endpoints

- `GET /api/v1/dealers` - Danh sách Đại lý.
- `POST /api/v1/dealers` - Tạo mới Đại lý.
- `GET /api/v1/dealers/{id}` - Xem chi tiết Đại lý.
- `PUT /api/v1/dealers/{id}` - Cập nhật hồ sơ Đại lý.
- `PUT /api/v1/dealers/{id}/credit-limit` - Cập nhật hạn mức tín dụng (Credit Limit).
- `PUT /api/v1/dealers/{id}/payment-term` - Kế toán trưởng cập nhật kỳ hạn thanh toán của Đại lý (`30`/`60` ngày).
- `PUT /api/v1/dealers/{id}/credit-status` - Kế toán trưởng cập nhật trạng thái tín dụng (`ACTIVE`/`CREDIT_HOLD`).
- `DELETE /api/v1/dealers/{id}` - Soft-delete Đại lý bằng `is_active = false`; không xóa vật lý dữ liệu nghiệp vụ.
- `PUT /api/v1/dealers/{id}/reactivate` - Kích hoạt lại Đại lý bằng `is_active = true`.
- `GET /api/v1/suppliers` - Danh sách Nhà cung cấp.
- `POST /api/v1/suppliers` - Tạo mới Nhà cung cấp.
- `GET /api/v1/suppliers/{id}` - Xem chi tiết Nhà cung cấp.
- `PUT /api/v1/suppliers/{id}` - Cập nhật hồ sơ Nhà cung cấp.
- `DELETE /api/v1/suppliers/{id}` - Soft-delete Nhà cung cấp bằng `is_active = false`; không xóa vật lý dữ liệu nghiệp vụ.
- `PUT /api/v1/suppliers/{id}/reactivate` - Kích hoạt lại Nhà cung cấp bằng `is_active = true`.
- `GET /api/v1/suppliers/{id}/received-orders` - Xem danh sách đơn/phiếu đã nhận từ Nhà cung cấp.
- `GET /api/v1/suppliers/{id}/received-orders/{orderId}` - Xem chi tiết một đơn/phiếu đã nhận từ Nhà cung cấp.
- `GET /api/v1/delivery-orders` - Danh sách Delivery Order.
- `POST /api/v1/delivery-orders` - Tạo mới Delivery Order theo bảng `delivery_orders`.
- `GET /api/v1/delivery-orders/{id}` - Xem chi tiết Delivery Order.
- `PUT /api/v1/delivery-orders/{id}` - Cập nhật Delivery Order.
- `DELETE /api/v1/delivery-orders/{id}` - Hủy/soft-cancel Delivery Order bằng trạng thái `CANCELLED`.

## 6. Acceptance Criteria

- **Scenario: Change credit limit**
  - Given a dealer with `credit_limit = 500M` and `current_balance = 0`
  - When `ACCOUNTANT_MANAGER` sets credit_limit to `800M`
  - Then the system SHALL update successfully and record the change in the audit log.

- **Scenario: Reject invalid credit limit below current balance**
  - Given a dealer with `current_balance = 600M`
  - When `ACCOUNTANT_MANAGER` attempts to set `credit_limit` to `500M`
  - Then the system SHALL reject the request because credit limit must be greater than current balance.

- **Scenario: Reject non-positive credit limit**
  - Given a dealer exists
  - When `ACCOUNTANT_MANAGER` attempts to set `credit_limit` to `0`
  - Then the system SHALL reject the request because credit limit must be greater than `0`.

- **Scenario: Block transaction that exceeds credit limit**
  - Given a dealer has `current_balance = 480M`, `credit_limit = 500M`, and `is_active = true`
  - When a user attempts to create a new dealer transaction worth `30M`
  - Then the system SHALL reject only that transaction before creation, set the dealer's `credit_status` to `CREDIT_HOLD`, and notify the creator that the dealer would exceed the credit limit.

- **Scenario: Allow transaction exactly at credit limit**
  - Given a dealer has `current_balance = 480M`, `credit_limit = 500M`, and `is_active = true`
  - When a user attempts to create a new dealer transaction worth `20M`
  - Then the system SHALL allow the transaction because the resulting balance equals the credit limit.

- **Scenario: Configure dealer payment term**
  - Given a dealer has `payment_term_days = 30`
  - When `ACCOUNTANT_MANAGER` sets `payment_term_days` to `60`
  - Then the system SHALL update the dealer to Net 60 and record the before/after change in the audit log.

- **Scenario: Reject invalid payment term**
  - Given a dealer exists
  - When `ACCOUNTANT_MANAGER` attempts to set `payment_term_days` to `45`
  - Then the system SHALL reject the request because only Net 30 and Net 60 are allowed.

- **Scenario: Accountant manages dealer profile**
  - Given an authenticated `ACCOUNTANT`
  - When the accountant creates or updates a dealer profile with valid required fields
  - Then the system SHALL save the dealer profile and create an audit log entry with changed fields.

- **Scenario: Accountant manages supplier profile**
  - Given an authenticated `ACCOUNTANT`
  - When the accountant creates or updates a supplier profile with valid required fields
  - Then the system SHALL save the supplier profile and create an audit log entry with changed fields.

- **Scenario: Block delivery order for inactive dealer**
  - Given a dealer has `is_active = false`
  - When a Planner attempts to create a delivery order for that dealer
  - Then the system SHALL reject the delivery order creation because the dealer is inactive.

- **Scenario: View supplier received orders**
  - Given a supplier has historical received orders or receipts
  - When `ACCOUNTANT` views received orders for that supplier
  - Then the system SHALL return read-only order/receipt summaries and allow viewing details without creating new inbound documents.

- **Scenario: Soft-delete dealer**
  - Given an active dealer exists
  - When `ACCOUNTANT` deactivates the dealer
  - Then the system SHALL set `is_active = false`, keep historical records unchanged, block all new operational activity for that dealer account, and create an audit log entry.

- **Scenario: Delivery order for inactive dealer**
  - Given a dealer has `is_active = false`
  - When a user attempts to create a Delivery Order for that dealer
  - Then the system SHALL reject the Delivery Order creation because the dealer is inactive.

- **Scenario: Unauthorized credit-limit update**
  - Given an authenticated user without `ACCOUNTANT_MANAGER` role
  - When the user attempts to update a dealer's `credit_limit`
  - Then the system SHALL reject the request with an authorization error and leave the dealer unchanged.

- **Scenario: Duplicate supplier code**
  - Given a supplier already has code `SUP-001`
  - When `ACCOUNTANT` creates another supplier with code `SUP-001`
  - Then the system SHALL reject the request with a duplicate supplier-code validation error.
