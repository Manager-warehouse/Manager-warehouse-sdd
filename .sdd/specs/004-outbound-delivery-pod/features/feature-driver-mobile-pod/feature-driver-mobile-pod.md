# Feature: Tài xế Xác nhận Giao hàng & POD (US-WMS-09)

## 1. Context and Goal

Tài xế sử dụng mobile view để xem chuyến xe hiện tại được gán, giao lần lượt từng Delivery Order trong chuyến và ghi nhận kết quả giao hàng. Tại một thời điểm, mỗi tài xế và mỗi xe chỉ được gán cho tối đa một trip đang hoạt động; một trip có thể chứa nhiều Delivery Order. Khi Đại lý nhận hàng, tài xế phải upload 2 ảnh: ảnh hàng đã bàn giao vào điểm nhận và ảnh phiếu xuất kho/biên nhận có chữ ký xác nhận của Đại lý. Sau khi có đủ bằng chứng POD, hệ thống gửi OTP 6 số ngẫu nhiên tới email Đại lý; Đại lý đọc OTP cho tài xế nhập vào hệ thống.

Khi OTP hợp lệ và còn hạn, hệ thống xác nhận giao hàng thành công cho đúng Delivery Order đó, trừ hàng của Delivery Order đó khỏi kho ảo `IN_TRANSIT`, tự động tạo invoice/công nợ và chuyển Delivery Order sang `COMPLETED`. Các Delivery Order khác trong cùng trip không bị thay đổi bởi thao tác xác nhận này.

Nếu Đại lý từ chối nhận hàng hoặc giao hàng thất bại, tài xế bấm nút chuyển Delivery Order sang `RETURNED`; hàng vẫn nằm trong kho ảo `IN_TRANSIT` cho tới khi luồng hoàn hàng riêng tiếp nhận và xử lý. Sau khi mọi Delivery Order trong trip đã `COMPLETED` hoặc `RETURNED`, tài xế có nút xác nhận xe đã quay về kho để chuyển trip sang `COMPLETED`.

## 2. Actors

* **Tài xế**: Xem trip hiện tại được gán cho driver profile của mình và các Delivery Order thuộc trip đó, upload `goodsImage`/`signDocumentImage`, yêu cầu OTP, nhập OTP, báo Đại lý từ chối/giao thất bại, và xác nhận xe đã quay về kho.
* **Admin**: Reset OTP đã bị khóa sau 3 lần nhập sai để tài xế có thể yêu cầu mã mới.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * The system SHALL allow Driver users to view and mutate only the current trip assigned to their own driver profile.
  * The system SHALL enforce that a Driver and a vehicle are each assigned to no more than one active trip in status `PLANNED` or `IN_TRANSIT` at a time.
  * The current trip MAY contain multiple Delivery Orders, which the Driver SHALL process individually without changing sibling Delivery Orders that have not yet been completed or returned.
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
    * Accept multipart fields `goodsImage` and `signDocumentImage` in the same request; both fields SHALL be present and the backend SHALL NOT support single-image POD upload.
    * Require `goodsImage` to show goods handed over/unloaded at the dealer site.
    * Require `signDocumentImage` to show the signed delivery document or receipt confirmation.
    * Reject each image when it is larger than 5 MB or its binary content cannot be decoded as an allowed image format; validation SHALL inspect file signatures/content and SHALL NOT trust only the filename extension or client-supplied MIME type.
    * Accept camera output encoded as JPEG, PNG, or WebP; reject SVG, GIF, renamed non-image files, malformed images, and other unsupported formats.
    * Upload images through the Spring Boot backend to a private Supabase Storage bucket, which SHOULD be named `pod-evidence`; the frontend SHALL NOT receive or contain the Supabase service key.
    * Treat persistence as one logical operation: if either object upload or database update fails, the backend SHALL leave no partial POD state, remove objects created by that failed request where applicable, and allow retry with both images.
    * Save only each image's object path/key and metadata on the current attempt's `deliveries` record; metadata SHALL include evidence type (`GOODS` or `SIGNED_DOCUMENT`), original filename, detected content type, size in bytes, and upload timestamp. The database SHALL NOT store a public URL, signed URL, or image binary.
    * Allow every authenticated user who is authorized to view the relevant Delivery Order detail under existing role and warehouse-scope rules to view its POD evidence.
    * Generate POD signed URLs only through the Spring Boot backend after rechecking Delivery Order detail access; each signed URL SHALL expire 15 minutes after generation and SHALL NOT be persisted in the database.
    * POD evidence SHALL NOT be publicly accessible, and generating or viewing a signed URL SHALL NOT create an audit log.
    * Create `UPLOAD_POD` audit log.
  * WHEN a driver requests delivery OTP after both POD images are uploaded, the system SHALL:
    * Validate the authenticated driver is assigned to the trip.
    * Validate the current delivery attempt has object paths/keys for both POD images.
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
    * Reject the third incorrect OTP submission with `OTP_MAX_ATTEMPTS_EXCEEDED`, set only the OTP row status to `LOCKED`, and leave the delivery attempt, Delivery Order, trip, inventory, invoice, and receivable states unchanged; after this lock, Driver cannot request a new OTP and Admin must reset the OTP row before the system can generate a new code.
    * Mark the OTP record as successful by setting status `VERIFIED` and `consumed_at`; the OTP SHALL no longer be usable after successful verification.
    * Update the current attempt's `deliveries` record to `DELIVERED`, with POD object paths/keys and metadata, OTP verification timestamp, and delivery timestamp.
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

### Driver Mobile Responsive UX Requirements

* **Supported environment:**
  * The Driver interface SHALL be mobile-first and support only Android phones and iPhones in portrait orientation.
  * A supported phone viewport SHALL have a portrait CSS viewport width from `320` through `480` px inclusive and a height greater than its width. A viewport width of `481` CSS px or greater SHALL be treated as tablet/unsupported for this feature; `480` px is the inclusive phone boundary and `481` px is the tablet boundary.
  * When the viewport is landscape (`width >= height`), the interface SHALL block trip and delivery content and actions and show a Vietnamese instruction to rotate the phone to portrait; returning to portrait SHALL restore the current step without losing in-session previews.
  * The reference design viewport SHALL be `390 × 844` CSS px and the interface SHALL be tested at `360 × 800`, `390 × 844`, and `414 × 896` CSS px.
  * The supported browsers SHALL be Chrome Android 120 or later and Safari iOS 16.4 or later.
  * At every supported viewport, the interface SHALL have no horizontal scrolling and no clipped, obscured, or unreachable content.
  * Visible Driver UI text SHALL use at least `14` CSS px; form inputs and OTP digits SHALL use at least `16` CSS px; primary action labels SHALL use at least `16` CSS px.
  * The interface SHALL NOT depend on hover and SHALL provide at least `44 × 44` CSS px touch targets for primary interactive controls.
* **Language and feedback:**
  * All Driver interface content SHALL be in Vietnamese.
  * Driver-facing errors SHALL explain the problem and next action in plain Vietnamese and SHALL NOT expose error codes, stack traces, or technical terminology.
  * Loading, disabled, success, and error states SHALL be visually distinct.
* **Flow and layout:**
  * The interface SHALL use a single-column layout optimized for one-handed operation.
  * The primary flow SHALL remain in this order: view the currently assigned trip; view a Delivery Order in that trip; capture two POD images; request OTP; enter OTP; view the delivery result.
  * If the Driver has no active trip assigned, the interface SHALL show `Hiện không có chuyến xe được giao` and SHALL NOT show delivery actions.
  * The primary action SHALL remain sticky at the bottom and change by step to `Chụp ảnh POD`, `Yêu cầu OTP`, or `Xác nhận giao hàng`.
  * `Báo giao thất bại` SHALL remain available as a visually distinct secondary destructive action for an eligible `IN_TRANSIT` Delivery Order and SHALL open the required `failureReason` form without competing with the sticky primary success action.
  * The sticky action area SHALL respect the iPhone safe area and SHALL NOT cover page content.
  * On activation, a request-producing action SHALL become disabled immediately, show a clear loading state, and prevent duplicate requests until the request completes.
* **Camera and POD capture:**
  * The interface SHALL use browser camera capture through `getUserMedia()` and SHALL NOT use a generic file picker or offer selection from the photo library.
  * The interface SHALL request the rear-facing camera using `facingMode = environment` where supported and SHALL encode captured still images as an allowed image format.
  * If no camera is available or camera permission is denied, the interface SHALL block the POD step and show Vietnamese instructions for granting camera permission again.
  * The interface SHALL provide two separate capture slots labeled for `goodsImage` and `signDocumentImage`; selecting a slot SHALL open the camera for that evidence type only.
  * Each capture SHALL populate or replace only its corresponding in-session preview. Capturing an image SHALL NOT upload it to the backend immediately.
  * The interface SHALL allow the Driver to delete and recapture either preview before submission.
  * Both `goodsImage` and `signDocumentImage` SHALL be required; each file SHALL be a valid image no larger than 5 MB.
* **Network and upload:**
  * Offline operation and draft persistence SHALL NOT be supported.
  * During upload, the interface SHALL show a Vietnamese processing state and prevent progression to OTP.
  * The interface SHALL enable POD submission only after both previews exist and SHALL send both files together in one multipart request; it SHALL never call the POD endpoint with only one image.
  * The interface SHALL progress to OTP only after both images have uploaded successfully.
  * If the network is weak or upload fails, the interface SHALL retain both available previews for the current browser session, explain the failure in plain Vietnamese, and allow retry without recapturing successful or retained images.
* **OTP entry:**
  * The OTP form SHALL contain six single-digit fields, use the numeric mobile keyboard, move focus forward after entry, and support deletion with focus returning to the previous field.
  * The interface SHALL display the remaining OTP validity time and the number of incorrect attempts used or remaining, based on backend data.
  * The interface SHALL provide a resend action only when the OTP has expired and the backend permits resend.
  * During confirmation or resend, the relevant action SHALL disable immediately, show loading, and prevent duplicate requests.
* **Success result:**
  * After valid OTP confirmation, the interface SHALL show an explicit Vietnamese delivery-success message without automatic navigation.
  * The success view SHALL provide `Xem chuyến xe`, which returns the Driver to the current trip detail page.

## 4. API Endpoints

* `GET /api/v1/trips/{id}` - Driver mobile view for the current trip assigned to the authenticated Driver.
* `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/pod-evidence` - Upload POD images for one order in the trip.
* `GET /api/v1/delivery-orders/{doId}/pod-evidence/signed-urls` - Generate fresh 15-minute signed URLs for authorized Delivery Order detail viewers.
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

Both image fields SHALL be submitted together. If either field is absent or invalid, the backend SHALL reject the whole request with `POD_FILE_INVALID` and SHALL NOT persist either image as accepted POD evidence. The backend SHALL verify actual binary image content and detected format rather than trusting only the extension or declared `Content-Type`.

### POD evidence metadata and signed URL response

For each POD image, the current delivery attempt SHALL persist:

* `objectKey` - Private Supabase Storage object path/key.
* `evidenceType` - `GOODS` for `goodsImage` or `SIGNED_DOCUMENT` for `signDocumentImage`.
* `originalFilename` - Client-provided filename retained as metadata only and never used as the storage path without backend sanitization.
* `contentType` - Backend-detected content type.
* `sizeBytes` - Validated file size in bytes.
* `uploadedAt` - Server-generated ISO 8601 upload timestamp with timezone.

`GET /api/v1/delivery-orders/{doId}/pod-evidence/signed-urls` SHALL:

* Require authentication and apply the same authorization and warehouse-scope checks used to view the Delivery Order detail.
* Resolve both private objects from the stored `objectKey` values.
* Generate new signed URLs with a 15-minute validity period on every successful request.
* Return `doId`, `deliveryId`, `expiresAt`, `goodsImage`, and `signDocumentImage`; each image object SHALL contain `signedUrl`, `evidenceType`, `originalFilename`, `contentType`, `sizeBytes`, and `uploadedAt`.
* Never return the Supabase service key or expose the bucket publicly.
* Never store the generated signed URLs in the database or create an audit log for URL generation or image viewing.

If the Delivery Order has no complete POD evidence, the endpoint SHALL return `POD_EVIDENCE_NOT_FOUND`. If the authenticated user cannot view the Delivery Order detail, the endpoint SHALL return the existing authorization failure without revealing whether POD objects exist.

### Delivery OTP request payload

`POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp` SHALL accept:

* `resend` - Optional boolean. If true and the previous OTP is expired, backend SHALL update the current OTP row for this delivery attempt. If the previous OTP is still active, backend SHALL reject with `OTP_STILL_ACTIVE`.

Backend SHALL generate the 6-digit OTP; client SHALL NOT submit an OTP value in this request.

### Delivery OTP response contract

`POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp` SHALL return a `DeliveryOtpResponse` containing:

* `deliveryId` - Current delivery attempt identifier.
* `recipientEmail` - Dealer email address to which the OTP was sent; the Driver UI SHOULD mask part of the address when displayed.
* `status` - Current OTP status: `ACTIVE`, `EXPIRED`, `LOCKED`, or `VERIFIED`.
* `expiresAt` - Server-generated ISO 8601 timestamp with timezone indicating when the OTP expires.
* `attemptCount` - Number of incorrect submissions for the current OTP, from `0` through `3`.
* `maxAttempts` - Maximum incorrect submissions allowed; SHALL be `3` in Sprint 1.
* `remainingAttempts` - `max(0, maxAttempts - attemptCount)` calculated by the backend.
* `canResend` - Server-authoritative resend permission at response time. It SHALL be `true` only when the current OTP is expired, the OTP is not locked or verified, and the current delivery attempt remains eligible for OTP issuance.

The response SHALL NOT contain the raw OTP or OTP hash. The Driver UI MAY use `expiresAt` to update the countdown locally, but backend time and backend resend validation SHALL remain authoritative. Reaching zero in the client countdown SHALL NOT bypass backend validation.

When `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/confirm-delivery` rejects an incorrect, expired, or locked OTP, the error response SHALL retain the existing error `code` and additionally include an `otp` object with `status`, `expiresAt`, `attemptCount`, `maxAttempts`, `remainingAttempts`, and `canResend`. Specifically:

* `DELIVERY_OTP_INVALID` SHALL return the incremented `attemptCount` and updated `remainingAttempts`.
* The third incorrect submission SHALL return `OTP_MAX_ATTEMPTS_EXCEEDED`, `status = LOCKED`, `attemptCount = 3`, `remainingAttempts = 0`, and `canResend = false`.
* `DELIVERY_OTP_EXPIRED` SHALL return `status = EXPIRED`; `canResend` SHALL reflect whether the backend currently permits resend.
* `OTP_RESET_REQUIRED` SHALL return `status = LOCKED`, `remainingAttempts = 0`, and `canResend = false`.

The Driver UI SHALL use this response metadata rather than maintaining its own authoritative attempt count. Driver-facing messages SHALL translate the error outcome into plain Vietnamese and SHALL NOT display the backend error `code`.

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
| `POD_FILE_INVALID` | 400 | Either POD file is missing, larger than 5 MB, malformed, not an image, or uses an unsupported binary image format. |
| `POD_EVIDENCE_NOT_FOUND` | 404 | The Delivery Order/current attempt has no complete pair of POD evidence objects. |
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

* **Scenario: Driver sees only the current assigned trip**
  * Given a Driver is assigned to one active trip containing one or more Delivery Orders
  * When the Driver opens the mobile Driver view
  * Then the interface SHALL show that current trip and its Delivery Orders.
  * And the Driver SHALL NOT be able to view or mutate a trip assigned to another Driver.

* **Scenario: Driver and vehicle cannot have overlapping active trips**
  * Given a Driver or vehicle is already assigned to a trip in `PLANNED` or `IN_TRANSIT`
  * When Dispatcher attempts to assign that same Driver or vehicle to another active trip
  * Then the system SHALL reject the overlapping assignment.

* **Scenario: Driver has no current trip**
  * Given the authenticated Driver has no trip in `PLANNED` or `IN_TRANSIT`
  * When the Driver opens the mobile Driver view
  * Then the interface SHALL show `Hiện không có chuyến xe được giao`.
  * And delivery actions SHALL NOT be available.

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

* **Scenario: POD endpoint rejects single-image and invalid-content uploads atomically**
  * Given the Driver submits only one required image, a renamed non-image file, malformed image bytes, an unsupported format, or an image larger than 5 MB
  * When the backend validates the multipart POD request
  * Then the system SHALL reject the whole request with `POD_FILE_INVALID`.
  * And it SHALL NOT accept either image, persist partial POD metadata, or leave an object created by the failed request.

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
  * Then the third submission SHALL be rejected with `OTP_MAX_ATTEMPTS_EXCEEDED` and OTP metadata containing `status = LOCKED`, `attemptCount = 3`, `remainingAttempts = 0`, and `canResend = false`.
  * And the delivery attempt SHALL remain `IN_TRANSIT`, the Delivery Order and trip statuses SHALL remain unchanged, and inventory, invoice, and receivable data SHALL remain unchanged.
  * And the system SHALL require Admin reset before a new OTP can be generated.

* **Scenario: Admin resets locked OTP**
  * Given a Delivery Order in `IN_TRANSIT` with OTP locked after 3 incorrect submissions
  * When Admin submits `resetReason`
  * Then the system SHALL mark the current OTP row `EXPIRED`, reset `attempt_count` to 0, create `RESET_DELIVERY_OTP` audit log, and allow Driver to request a new OTP on the same row.

* **Scenario: Invalid OTP blocks delivery confirmation**
  * Given a Delivery Order in `IN_TRANSIT` with both POD images uploaded
  * When Driver submits an invalid OTP
  * Then the system SHALL reject the request with `DELIVERY_OTP_INVALID`.
  * And the error response SHALL include the incremented `attemptCount`, `maxAttempts = 3`, updated `remainingAttempts`, current `expiresAt`, OTP `status`, and `canResend`.
  * And the interface SHALL display the remaining attempts in plain Vietnamese without displaying `DELIVERY_OTP_INVALID`.

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

* **Scenario: Driver flow is usable on every supported viewport**
  * Given the Driver opens the currently assigned trip in portrait on Chrome Android 120 or later or Safari iOS 16.4 or later
  * When the interface is tested at `360 × 800`, `390 × 844`, and `414 × 896` CSS px
  * Then each step SHALL use a readable single-column Vietnamese layout with no horizontal scroll, clipped content, or content hidden by the sticky action area.
  * And visible text SHALL be at least `14` CSS px, form inputs and OTP digits at least `16` CSS px, and primary action labels at least `16` CSS px.
  * And primary interactive controls SHALL have touch targets of at least `44 × 44` CSS px and SHALL NOT depend on hover.

* **Scenario: Unsupported tablet viewport is blocked**
  * Given the Driver opens the interface with a CSS viewport width of `481` px or greater
  * When the interface evaluates the viewport
  * Then it SHALL treat the device as tablet/unsupported and show a Vietnamese phone-only message instead of trip and delivery actions.
  * And a portrait viewport width of exactly `480` px SHALL remain within the phone boundary.

* **Scenario: Landscape orientation is blocked without losing current work**
  * Given the Driver is using a supported phone in portrait and has one or two in-session POD previews
  * When the viewport changes to landscape where `width >= height`
  * Then the interface SHALL block trip and delivery content and actions and instruct the Driver in Vietnamese to rotate the phone to portrait.
  * When the viewport returns to portrait
  * Then the interface SHALL restore the same delivery step and retained previews.

* **Scenario: Sticky primary action follows the delivery step**
  * Given the Driver is completing a Delivery Order on an iPhone or Android phone
  * When the Driver advances from POD capture to OTP request and then OTP confirmation
  * Then the sticky bottom action SHALL respectively show `Chụp ảnh POD`, `Yêu cầu OTP`, and `Xác nhận giao hàng`.
  * And the sticky area SHALL respect the iPhone safe area and SHALL NOT obscure content.
  * And each request-producing action SHALL disable immediately, show loading, and prevent a duplicate request after it is activated.

* **Scenario: Camera-only POD capture with previews**
  * Given the Driver is at the POD step with browser camera permission
  * When the Driver captures `goodsImage` and `signDocumentImage`
  * Then the interface SHALL use `getUserMedia()` with rear-camera preference and SHALL NOT expose a generic file picker or photo-library selection.
  * And the `goodsImage` and `signDocumentImage` slots SHALL capture and replace their own previews independently.
  * And capturing either image SHALL NOT call the backend upload endpoint.
  * And each captured image SHALL have a preview that can be replaced or deleted and recaptured before upload.
  * And the interface SHALL reject a missing file, non-image file, or image larger than 5 MB with a plain Vietnamese message.

* **Scenario: Camera unavailable or permission denied blocks POD**
  * Given the phone has no available camera or the Driver denies camera permission
  * When the Driver attempts to capture POD evidence
  * Then the interface SHALL block progression beyond the POD step.
  * And it SHALL show plain Vietnamese instructions for granting camera permission again without exposing an error code, stack trace, or technical terminology.

* **Scenario: Private POD storage does not expose credentials or public images**
  * Given both valid POD images are ready for upload
  * When the Driver uploads the evidence
  * Then the Spring Boot backend SHALL upload the images to a private Supabase Storage bucket, for which `pod-evidence` is the recommended name.
  * And the frontend SHALL NOT contain the Supabase service key.
  * And the database SHALL store only object paths/keys and metadata, while authorized viewing SHALL use time-limited signed URLs.

* **Scenario: Authorized Delivery Order viewer requests fresh POD signed URLs**
  * Given both POD object keys and metadata are stored for a Delivery Order
  * And the authenticated user is authorized to view that Delivery Order detail under existing role and warehouse-scope rules
  * When the frontend calls `GET /api/v1/delivery-orders/{doId}/pod-evidence/signed-urls`
  * Then the backend SHALL generate and return fresh signed URLs for both images with one `expiresAt` exactly 15 minutes after generation.
  * And it SHALL NOT persist either signed URL or create an audit log for generating or viewing the URLs.
  * When the URLs expire and the user views the evidence again
  * Then the frontend SHALL call the endpoint again instead of reusing the expired URLs.

* **Scenario: Unauthorized user cannot obtain POD signed URLs**
  * Given an authenticated user is not authorized to view the Delivery Order detail
  * When that user requests POD signed URLs
  * Then the backend SHALL reject access without generating URLs or revealing whether POD evidence exists.

* **Scenario: Failed POD upload can be retried in the current session**
  * Given the Driver has captured both required POD images
  * When a weak network or upload failure prevents one or both uploads from completing
  * Then the interface SHALL retain the available image previews in the current browser session and SHALL NOT proceed to OTP.
  * And it SHALL show a plain Vietnamese failure reason and a retry action without requiring the Driver to recapture retained images.
  * When both uploads later succeed
  * Then the `Yêu cầu OTP` action SHALL become available.

* **Scenario: Driver can report delivery failure from the mobile order view**
  * Given a Delivery Order is eligible for failure reporting while `IN_TRANSIT`
  * When the Driver opens that Delivery Order
  * Then the interface SHALL show `Báo giao thất bại` as a visually distinct secondary destructive action.
  * When the Driver activates it
  * Then the interface SHALL require `failureReason` before enabling submission and SHALL preserve the existing failure/return business flow.

* **Scenario: OTP form supports mobile entry and backend-controlled resend**
  * Given an active 6-digit OTP exists for the current delivery attempt
  * When the Driver enters or deletes digits in the OTP form
  * Then the interface SHALL show six single-digit fields, open the numeric keyboard, advance focus after entry, and return focus to the previous field on backward deletion.
  * And it SHALL show the remaining validity time and the incorrect attempts used or remaining from backend response data.
  * And resend SHALL remain disabled until the client countdown reaches `expiresAt` and the latest backend state does not prohibit resend; the backend SHALL make the authoritative decision when resend is requested.
  * When the Driver confirms or resends
  * Then the relevant action SHALL disable immediately, show loading, and prevent duplicate requests.

* **Scenario: Successful confirmation remains on the result screen**
  * Given both POD images are uploaded and the Driver has entered a valid unexpired OTP
  * When delivery confirmation succeeds
  * Then the interface SHALL show a clear Vietnamese delivery-success message and SHALL NOT navigate automatically.
  * And it SHALL show `Xem chuyến xe`.
  * When the Driver selects `Xem chuyến xe`
  * Then the interface SHALL return to the detail page for the current trip.
