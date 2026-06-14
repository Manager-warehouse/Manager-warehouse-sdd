# Feature: Tài xế Xác nhận Giao hàng & Chữ ký Điện tử POD (US-WMS-09)

## 1. Context and Goal
Tài xế sử dụng smartphone để xem chuyến xe, chỉ dẫn giao hàng và thực hiện ký nhận giao hàng POD bằng cách chụp ảnh hàng bàn giao, chụp ảnh chữ ký/biên nhận của Đại lý, và xác thực OTP gửi qua email Đại lý. Nếu giao thất bại, tài xế ghi nhận lý do và đơn chuyển sang `RETURNED`; xử lý hàng hoàn được thực hiện trong luồng hoàn hàng riêng.

## 2. Actors
* **Tài xế**: Nhận chuyến, xem lộ trình, upload POD images, nhập OTP Đại lý hoặc báo cáo giao thất bại.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Tài xế uploads POD evidence, the system SHALL:
    * Require a goods handover image and a signature/receipt image from the Đại lý.
    * Store uploaded images in file storage and save their URLs on the `deliveries` record.
    * Reject images larger than 5MB or non-image file types.
  * WHEN a Tài xế requests delivery OTP after POD evidence is uploaded, the system SHALL:
    * Send a one-time OTP to the Đại lý email configured on the dealer profile.
    * Save `otp_recipient_email` and `otp_sent_at` for the delivery attempt.
  * WHEN a Tài xế confirms delivery with the OTP read by the Đại lý, the system SHALL:
    * Validate that POD evidence exists and the OTP is valid and not expired.
    * Create or update a `deliveries` record with status `DELIVERED`, saving the POD images, OTP verification timestamp, and delivery timestamp.
    * Decrease virtual In-Transit warehouse inventories for the delivered quantities.
    * Update DO status to `DELIVERED`.
    * Notify the Kế toán viên to create an invoice.
  * WHEN a Tài xế reports delivery failure, the system SHALL:
    * Create a `deliveries` record with status `RETURNED` and save the `failure_reason`.
    * Update DO status to `RETURNED`.
    * Keep the goods tracked in virtual In-Transit inventory until the separate return flow receives and classifies the returned goods.

## 4. API Endpoints
* `GET /api/v1/trips/{id}` - Xem thông tin chuyến xe được gán (Driver mobile view).
* `POST /api/v1/trips/{id}/pod-evidence` - Upload ảnh hàng bàn giao và ảnh chữ ký/biên nhận của Đại lý (`multipart/form-data`, mỗi ảnh ≤ 5MB).
* `POST /api/v1/trips/{id}/delivery-otp` - Gửi OTP xác nhận giao hàng thành công qua email Đại lý sau khi đã có POD evidence.
* `PUT /api/v1/trips/{id}/confirm-delivery` - Xác nhận giao thành công bằng OTP Đại lý đọc cho Tài xế.
* `PUT /api/v1/trips/{id}/fail-delivery` - Xác nhận giao thất bại (gửi kèm lý do lỗi).

## 5. Acceptance Criteria
* **Scenario: Delivery success with POD and OTP**
  * Given a DO in status `IN_TRANSIT`
  * When Driver uploads the goods handover image and the dealer signature/receipt image
  * And Driver requests an OTP sent to the dealer email
  * And Driver submits the OTP read by the dealer
  * Then the system SHALL save the POD, mark OTP as verified, decrease virtual In-Transit inventory, change status to `DELIVERED`, and trigger a billing notification.

* **Scenario: Missing POD evidence blocks delivery confirmation**
  * Given a DO in status `IN_TRANSIT`
  * When Driver submits delivery confirmation without both required POD images
  * Then the system SHALL reject the request with `MISSING_POD`.

* **Scenario: Invalid or expired OTP blocks delivery confirmation**
  * Given a DO in status `IN_TRANSIT` with POD evidence uploaded
  * When Driver submits an invalid or expired OTP
  * Then the system SHALL reject the request with `DELIVERY_OTP_INVALID` or `DELIVERY_OTP_EXPIRED`.
