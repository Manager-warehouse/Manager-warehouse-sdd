# Feature: Tài xế Xác nhận Giao hàng & Chữ ký Điện tử POD (US-WMS-09)

## 1. Context and Goal
Tài xế sử dụng smartphone để xem chuyến xe, chỉ dẫn giao hàng và thực hiện ký nhận giao hàng POD bằng cách chụp ảnh + ký tên trên màn hình. Nếu giao thất bại, hàng được chuyển vào Quarantine.

## 2. Actors
* **Tài xế**: Nhận chuyến, xem lộ trình, ký nhận POD hoặc báo cáo giao thất bại.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Tài xế confirms delivery (POD signed), the system SHALL:
    * Create a `deliveries` record with status `DELIVERED`, saving the customer signature, picture of goods, and timestamp.
    * Update DO status to `DELIVERED`.
    * Notify the Kế toán viên to create an invoice.
  * WHEN a Tài xế reports delivery failure, the system SHALL:
    * Create a `deliveries` record with status `RETURNED` and save the `failure_reason`.
    * Update DO status to `RETURNED`.
    * Automatically create a quarantine receipt (`receipts.type = 'RETURN'`) to put returned goods into Quarantine.

## 4. API Endpoints
* `GET /api/v1/trips/{id}` - Xem thông tin chuyến xe được gán (Driver mobile view).
* `PUT /api/v1/trips/{id}/confirm-delivery` - Xác nhận giao thành công (gửi kèm chữ ký và ảnh).
* `PUT /api/v1/trips/{id}/fail-delivery` - Xác nhận giao thất bại (gửi kèm lý do lỗi).

## 5. Acceptance Criteria
* **Scenario: Delivery success with POD**
  * Given a DO in status `IN_TRANSIT`
  * When Driver uploads signature and photo and submits delivery confirmation
  * Then the system SHALL save the POD, change status to `DELIVERED`, and trigger a billing notification.
