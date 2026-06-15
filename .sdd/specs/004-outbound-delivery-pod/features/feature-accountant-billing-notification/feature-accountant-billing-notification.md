# Feature: Kế toán Tiếp nhận Thông báo Lập Hóa đơn (US-WMS-10)

## 1. Context and Goal

Ngay khi đơn hàng được tài xế giao thành công và có chữ ký POD, hệ thống tự động bắn cảnh báo đến bộ phận kế toán để lập hóa đơn bán hàng, ghi nhận công nợ đúng kỳ.

## 2. Actors

- **Kế toán viên / Kế toán trưởng**: Tiếp nhận thông báo lập hóa đơn để xử lý nhanh chóng.

## 3. Functional Requirements (EARS)

- **Ubiquitous:**
  - The system SHALL create a `BILLING_NOTIFICATION_CREATE` audit log entry or immutable notification event whenever a delivered DO becomes ready for invoicing after OTP confirmation.
  - The system SHALL expose a pending-invoice worklist so accountants can find all delivered Delivery Orders that do not yet have an invoice.
  - The pending-invoice worklist SHALL enforce both Accountant role and warehouse assignment scope.
- **Event-driven:**
  - WHEN a delivery order transitions to `DELIVERED` after POD evidence and dealer OTP verification, the system SHALL send a notification to accountants to prompt invoice creation.
  - WHEN an Accountant creates an invoice from a delivered Delivery Order, the system SHALL move the Delivery Order to `COMPLETED`.

## 4. API Endpoints

- `GET /api/v1/accounting/invoice-candidates?warehouseId={warehouseId}` - Danh sách Delivery Orders đã `DELIVERED` nhưng chưa có invoice, theo phạm vi kho được phân quyền.
- `POST /api/v1/accounting/invoice-candidates/{doId}/invoice` - Kế toán tạo hóa đơn từ một Delivery Order đã `DELIVERED`; sau khi thành công, DO chuyển sang `COMPLETED`.

## 5. Acceptance Criteria

- **Scenario: Notify accountant on delivery**
  - Given a delivery order transitions to `DELIVERED`
  - When the system processes the state change
  - Then the system SHALL trigger an in-app notification to the Accountant role.

- **Scenario: Accountant finds all delivered orders awaiting invoice**
  - Given multiple Delivery Orders are in `DELIVERED` status and do not yet have invoices
  - When an Accountant opens the pending-invoice worklist for an assigned warehouse
  - Then the system SHALL show all matching Delivery Orders and exclude orders outside the Accountant's warehouse scope.

- **Scenario: Create invoice from delivered order**
  - Given a Delivery Order is `DELIVERED` and has no invoice
  - When an Accountant creates an invoice from that order
  - Then the system SHALL create the invoice and update the Delivery Order status to `COMPLETED`.
