# Feature: Kế toán Tiếp nhận Thông báo Lập Hóa đơn (US-WMS-10)

## 1. Context and Goal
Ngay khi đơn hàng được tài xế giao thành công và OTP được xác nhận, hệ thống tự động bắn cảnh báo đến bộ phận kế toán để lập hóa đơn bán hàng, ghi nhận công nợ đúng kỳ. Feature này chỉ tạo notification trong outbound flow; nghiệp vụ tạo invoice và cập nhật công nợ đầy đủ nằm tại [008 finance billing closing](../../008-finance-billing-closing/spec.md).

## 2. Actors
* **Kế toán viên / Kế toán trưởng**: Tiếp nhận thông báo lập hóa đơn để xử lý nhanh chóng.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL create a `BILLING_NOTIFICATION_CREATE` audit log entry or immutable notification event whenever a delivered DO becomes ready for invoicing after OTP confirmation.
* **Event-driven:**
  * WHEN a delivery order transitions to `DELIVERED`, the system SHALL send a notification to accountants to prompt invoice creation.

## 4. API Endpoints
* Đơn hàng đã ở trạng thái `DELIVERED` được lấy qua bộ lọc để kế toán lập hóa đơn.

## 5. Acceptance Criteria
* **Scenario: Notify accountant on delivery**
  * Given a delivery order transitions to `DELIVERED`
  * When the system processes the state change
  * Then the system SHALL trigger an in-app notification to the Accountant role.
