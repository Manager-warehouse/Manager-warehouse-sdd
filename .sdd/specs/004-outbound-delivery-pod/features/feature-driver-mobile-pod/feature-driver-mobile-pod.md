# Feature: Tài xế Xác nhận Giao hàng & POD (US-WMS-09)

## 1. Context and Goal

Tài xế sử dụng mobile view để xem các chuyến xe được gán, giao từng Delivery Order trong chuyến và ghi nhận kết quả giao hàng. Khi Đại lý nhận hàng, tài xế phải upload 2 ảnh: ảnh hàng đã bàn giao vào điểm nhận và ảnh phiếu xuất kho/biên nhận có chữ ký xác nhận của Đại lý. Sau khi có đủ bằng chứng POD, hệ thống gửi OTP 6 số ngẫu nhiên tới email Đại lý; Đại lý đọc OTP cho tài xế nhập vào hệ thống.

Khi OTP hợp lệ và còn hạn, hệ thống xác nhận giao hàng thành công cho đúng Delivery Order đó, trừ hàng của Delivery Order đó khỏi kho ảo `IN_TRANSIT`, tự động tạo invoice/công nợ và chuyển Delivery Order sang `COMPLETED`. Các Delivery Order khác trong cùng trip không bị thay đổi bởi thao tác xác nhận này.

Nếu Đại lý từ chối nhận hàng hoặc giao hàng thất bại, tài xế bấm nút chuyển Delivery Order sang `RETURNED`; hàng vẫn nằm trong kho ảo `IN_TRANSIT` cho tới khi luồng hoàn hàng riêng tiếp nhận và xử lý. Sau khi mọi Delivery Order trong trip đã `COMPLETED` hoặc `RETURNED`, tài xế có nút xác nhận xe đã quay về kho để chuyển trip sang `COMPLETED`.

## 2. Actors

* **Tài xế**: Xem trip được gán cho driver profile của mình, upload `goodsImage`/`signDocumentImage`, yêu cầu OTP, nhập OTP, báo Đại lý từ chối/giao thất bại, và xác nhận xe đã quay về kho.
* **Admin**: Reset OTP đã bị khóa sau 3 lần nhập sai để tài xế có thể yêu cầu mã mới.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * The system SHALL allow Driver users to view and mutate only trips assigned to their own driver profile.
  * The system SHALL reject any driver action when the authenticated user is not linked to the trip `driver_id`.
  * The system SHALL treat the current delivery attempt as the latest `deliveries` record for the given `trip_id`, `do_id`, and authenticated `driver_id` that is not terminal.
  * The system SHALL NOT use `OUT_FOR_DELIVERY` in Sprint 1 delivery attempt status transitions.
  * The system SHALL require full Delivery Order delivery; partial delivery confirmation is not supported in Sprint 1.
  * The system SHALL create audit records for every user action in this flow: POD upload, OTP request/resend, OTP confirmation, delivery failure/return, and trip completion.

* **Event-driven:**
  * WHEN goods are dispatched for a Delivery Order delivery attempt, the system SHALL:
    * Create a new `deliveries` record for the current physical attempt at trip departure.
    * Assign the next `attempt_number` for that Delivery Order.
    * Initialize the current attempt with status `IN_TRANSIT`.
    * Preserve all previous failed, returned, or completed attempts without overwriting POD, OTP, or failure data.
  * WHEN a driver uploads POD evidence, the system SHALL:
    * Validate the authenticated driver is assigned to the trip.
    * Validate the Delivery Order belongs to the trip and is in `IN_TRANSIT`.
    * Validate the current delivery attempt exists and is the latest non-terminal attempt.
    * Accept multipart fields `goodsImage` and `signDocumentImage`.
    * Require `goodsImage` to show goods handed over/unloaded at the dealer site.
    * Require `signDocumentImage` to show the signed delivery document or receipt confirmation.
    * Reject each image when it is not an image file type or is larger than 5MB.
    * Store uploaded images in file storage and save their URLs only on the current attempt's `deliveries` record.
    * Create `UPLOAD_POD` audit log.
  * WHEN a driver requests delivery OTP after both POD images are uploaded, the system SHALL:
    * Validate the authenticated driver is assigned to the trip.
    * Validate the current delivery attempt has both POD image URLs.
    * Generate a random 6-digit numeric OTP.
    * Send the OTP to the dealer email configured on the dealer profile.
    * Store only the OTP hash/verifier, recipient email, `created_at`, `expires_at`, `attempt_count`, and status in `delivery_otp_attempts`.
    * Set OTP validity to 5 minutes from creation time.
    * Maintain exactly one `delivery_otp_attempts` row per current delivery attempt; the first OTP request inserts the row, and later resend after expiry updates that same row.
    * Reject resend while the current OTP is still active and not expired with `OTP_STILL_ACTIVE`; the backend SHALL NOT overwrite the active OTP.
    * If a previous OTP row exists for the current delivery attempt and the driver requests resend after expiry, update that same row by overwriting the OTP hash, reset `created_at`, reset `expires_at`, reset `attempt_count`, clear `consumed_at`, and set status back to `ACTIVE`.
    * Never store raw OTP on `deliveries` or `delivery_otp_attempts`.
    * Create `REQUEST_OTP` audit log.
  * WHEN a driver confirms delivery with the OTP read by the dealer, the system SHALL:
    * Validate the authenticated driver is assigned to the trip.
    * Validate the current delivery attempt exists and is still `IN_TRANSIT`.
    * Validate both POD images exist.
    * Validate an OTP record exists for the current delivery attempt.
    * Validate the submitted OTP is exactly 6 digits.
    * Validate the submission time is not later than `delivery_otp_attempts.expires_at`.
    * Reject expired OTP with `DELIVERY_OTP_EXPIRED`.
    * Reject incorrect or missing OTP with `DELIVERY_OTP_INVALID`.
    * Increment `attempt_count` for each incorrect OTP submission.
    * Reject confirmation with `OTP_MAX_ATTEMPTS_EXCEEDED` after 3 incorrect OTP submissions; after this lock, Driver cannot request a new OTP and Admin must reset the OTP row before the system can generate a new code.
    * Mark the OTP record as successful by setting status `VERIFIED` and `consumed_at`; the OTP SHALL no longer be usable after successful verification.
    * Update the current attempt's `deliveries` record to `DELIVERED`, with POD URLs, OTP verification timestamp, and delivery timestamp.
    * Confirm the whole Delivery Order only; partial delivery quantities SHALL be rejected.
    * Decrease virtual In-Transit inventory only for this Delivery Order's delivered quantities by item/product/batch.
    * Automatically create invoice and receivable for this Delivery Order only.
    * Move this Delivery Order directly to `COMPLETED`.
    * Run inventory movement, attempt update, OTP update, invoice/receivable creation, and Delivery Order status update in one transaction with optimistic version checks.
    * Create `CONFIRM_DELIVERY` audit log for the user confirmation action.
  * WHEN a driver reports dealer refusal or delivery failure, the system SHALL:
    * Validate the authenticated driver is assigned to the trip.
    * Validate the Delivery Order belongs to the trip and is in `IN_TRANSIT`.
    * Require `failureReason`.
    * Update the current attempt's `deliveries` record with status `FAILED` and save `failureReason`.
    * Update Delivery Order status to `RETURNED`.
    * NOT change inventory quantity; goods remain tracked in virtual `IN_TRANSIT` until the separate return flow receives and classifies them.
    * Create `FAIL_DELIVERY` audit log.
  * WHEN the assigned driver confirms the vehicle has returned to the source warehouse, the system SHALL:
    * Validate the authenticated driver is assigned to the trip.
    * Validate the trip is `IN_TRANSIT`.
    * Validate every Delivery Order in the trip is `COMPLETED` or `RETURNED`.
    * Move the trip to `COMPLETED`.
    * Mark vehicle and driver as `AVAILABLE`.
    * Keep any returned goods in virtual `IN_TRANSIT` inventory for the separate return flow.
    * Create `COMPLETE_TRIP` audit log.
  * WHEN the separate return flow confirms returned goods back into warehouse custody, the outbound flow SHALL allow the Delivery Order to move from `RETURNED` to `DELIVERY_FAILED`.
  * WHEN Admin resets a locked delivery OTP, the system SHALL:
    * Validate the OTP row belongs to the latest current delivery attempt of the Delivery Order.
    * Require `resetReason`.
    * Set OTP status to `EXPIRED`, reset `attempt_count` to 0, clear `consumed_at`, and keep the old OTP hash for audit trace only; the old OTP SHALL NOT be valid after reset.
    * Allow Driver to request a new OTP, which updates the same OTP row with a new hash and expiry time.
    * Create `RESET_DELIVERY_OTP` audit log with before/after state.

## 4. API Endpoints

* `GET /api/v1/trips/{id}` - Driver mobile view for an assigned trip.
* `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/pod-evidence` - Upload POD images for one order in the trip.
* `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp` - Generate/resend OTP to dealer email after POD evidence exists.
* `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/confirm-delivery` - Confirm full Delivery Order delivery using dealer OTP.
* `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/fail-delivery` - Record dealer refusal or delivery failure.
* `PUT /api/v1/trips/{tripId}/complete` - Assigned driver confirms the vehicle has returned to the source warehouse.
* `POST /api/v1/admin/delivery-orders/{doId}/delivery-otp/reset` - Admin resets a locked OTP for the current delivery attempt.

### POD evidence request payload

`POST /api/v1/trips/{tripId}/delivery-orders/{doId}/pod-evidence` SHALL accept `multipart/form-data`:

* `goodsImage` - Required image file, max 5MB.
* `signDocumentImage` - Required image file, max 5MB.
* `notes` - Optional driver notes.

### Delivery OTP request payload

`POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp` SHALL accept:

* `resend` - Optional boolean. If true and the previous OTP is expired, backend SHALL update the current OTP row for this delivery attempt. If the previous OTP is still active, backend SHALL reject with `OTP_STILL_ACTIVE`.

Backend SHALL generate the 6-digit OTP; client SHALL NOT submit an OTP value in this request.

### Confirm delivery request payload

`PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/confirm-delivery` SHALL accept:

* `otp` - Required 6-digit numeric OTP read by the dealer.
* `notes` - Optional driver notes.

### Fail delivery request payload

`PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/fail-delivery` SHALL accept:

* `failureReason` - Required reason for dealer refusal or delivery failure.
* `notes` - Optional driver notes.

### Complete trip request payload

`PUT /api/v1/trips/{tripId}/complete` SHALL accept:

* `returnedAt` - Optional vehicle return timestamp from the client; backend SHALL store server timestamp as authoritative audit time.
* `notes` - Optional driver return notes.

### Admin reset OTP request payload

`POST /api/v1/admin/delivery-orders/{doId}/delivery-otp/reset` SHALL accept:

* `resetReason` - Required reason for resetting a locked OTP.
* `notes` - Optional admin notes.

## 5. Error Codes

| Error | HTTP | Condition |
| ----- | ---- | --------- |
| `DRIVER_NOT_ASSIGNED_TO_TRIP` | 403 | Authenticated driver is not assigned to the trip. |
| `DELIVERY_ATTEMPT_NOT_FOUND` | 404 | Current delivery attempt does not exist for trip/DO/driver. |
| `DELIVERY_ATTEMPT_NOT_CURRENT` | 409 | Request targets an old or terminal delivery attempt. |
| `DELIVERY_ALREADY_FINALIZED` | 409 | Delivery attempt is already `DELIVERED`, `FAILED`, or `RETURNED`. |
| `POD_FILE_INVALID` | 400 | POD file is missing, not an image, or larger than 5MB. |
| `MISSING_POD` | 400 | Confirmation or OTP request is attempted before both POD images exist. |
| `DEALER_EMAIL_MISSING` | 422 | Dealer profile has no email for OTP delivery. |
| `OTP_NOT_REQUESTED` | 400 | Delivery confirmation is attempted before OTP is requested. |
| `DELIVERY_OTP_INVALID` | 400 | OTP is incorrect or not issued for this delivery attempt. |
| `DELIVERY_OTP_EXPIRED` | 400 | OTP is expired. |
| `OTP_STILL_ACTIVE` | 409 | Driver requested resend while the current OTP is still valid. |
| `OTP_MAX_ATTEMPTS_EXCEEDED` | 423 | OTP has been entered incorrectly 3 times and requires Admin reset. |
| `OTP_RESET_REQUIRED` | 423 | OTP is locked and must be reset by Admin before a new code can be generated. |
| `PARTIAL_DELIVERY_NOT_ALLOWED` | 422 | Request attempts to deliver less than the full Delivery Order. |
| `IN_TRANSIT_STOCK_NOT_FOUND` | 422 | Required In-Transit inventory rows are missing or insufficient for this DO. |
| `INVOICE_ALREADY_EXISTS` | 409 | Invoice already exists for the Delivery Order. |
| `TRIP_NOT_READY_TO_COMPLETE` | 422 | Trip cannot complete because the vehicle return is not confirmed or assigned orders are not all `COMPLETED` or `RETURNED`. |
| `INVENTORY_VERSION_CONFLICT` | 409 | Concurrent inventory update conflict. |

## 6. Acceptance Criteria

* **Scenario: Delivery success with POD and OTP**
  * Given a Delivery Order in `IN_TRANSIT`
  * And the latest current delivery attempt exists for this Delivery Order and trip
  * And the authenticated driver is assigned to the trip
  * When Driver uploads `goodsImage` and `signDocumentImage`
  * And Driver requests an OTP sent to the dealer email
  * And Driver submits the valid 6-digit OTP within 5 minutes
  * Then the system SHALL save POD evidence on the current attempt, mark OTP as verified and consumed, decrease virtual In-Transit inventory for this Delivery Order only, auto-create invoice/receivable, mark attempt `DELIVERED`, and move the Delivery Order to `COMPLETED`.

* **Scenario: Missing POD evidence blocks OTP and delivery confirmation**
  * Given a Delivery Order in `IN_TRANSIT`
  * When Driver requests OTP or submits delivery confirmation without both required POD images
  * Then the system SHALL reject the request with `MISSING_POD`.

* **Scenario: Expired OTP requires resend**
  * Given a 6-digit OTP was generated more than 5 minutes ago
  * When Driver submits that OTP
  * Then the system SHALL reject the request with `DELIVERY_OTP_EXPIRED`.
  * When Driver requests resend after expiry
  * Then the system SHALL update the current OTP row with a new OTP hash and new expiry time.

* **Scenario: Resend is blocked while OTP is still active**
  * Given a 6-digit OTP was generated less than 5 minutes ago and has not been consumed
  * When Driver requests resend
  * Then the system SHALL reject the request with `OTP_STILL_ACTIVE` and keep the current OTP row unchanged.

* **Scenario: OTP locks after 3 incorrect submissions**
  * Given a Delivery Order in `IN_TRANSIT` with an active OTP
  * When Driver submits incorrect OTP 3 times
  * Then the system SHALL reject further confirmation with `OTP_MAX_ATTEMPTS_EXCEEDED` and require Admin reset before a new OTP can be generated.

* **Scenario: Admin resets locked OTP**
  * Given a Delivery Order in `IN_TRANSIT` with OTP locked after 3 incorrect submissions
  * When Admin submits `resetReason`
  * Then the system SHALL mark the current OTP row `EXPIRED`, reset `attempt_count` to 0, create `RESET_DELIVERY_OTP` audit log, and allow Driver to request a new OTP on the same row.

* **Scenario: Invalid OTP blocks delivery confirmation**
  * Given a Delivery Order in `IN_TRANSIT` with both POD images uploaded
  * When Driver submits an invalid OTP
  * Then the system SHALL reject the request with `DELIVERY_OTP_INVALID`.

* **Scenario: Dealer refuses delivery**
  * Given a Delivery Order in `IN_TRANSIT`
  * And the authenticated driver is assigned to the trip
  * When Driver records dealer refusal with `failureReason`
  * Then the system SHALL close the current attempt as `FAILED`, move the Delivery Order to `RETURNED`, and keep goods in virtual In-Transit for the separate return flow.

* **Scenario: Driver completes trip after vehicle returns**
  * Given a trip is `IN_TRANSIT`
  * And every Delivery Order in the trip is `COMPLETED` or `RETURNED`
  * When the assigned driver confirms the vehicle has returned to the source warehouse
  * Then the system SHALL mark the trip `COMPLETED`, mark vehicle and driver `AVAILABLE`, and create `COMPLETE_TRIP` audit log.
