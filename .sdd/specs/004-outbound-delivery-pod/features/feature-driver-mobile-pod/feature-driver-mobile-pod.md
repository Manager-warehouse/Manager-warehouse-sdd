# Feature: Tài xế Xác nhận Giao hàng & POD (US-WMS-09)

## 1. Context and Goal

Driver mobile list is a shared operational entry point for both assigned outbound delivery trips (`trip_type = DELIVERY`, usually `TRIP-*`) and assigned internal transfer trips (`trip_type = TRANSFER`, usually `TTR-*`). The list screen SHALL be titled `Chuyen xe cua toi`, not `Chuyen giao hang cua toi`, so a driver can distinguish dealer delivery work from internal warehouse transfer work before opening a trip detail. The list SHALL expose three visible filters: `Tat ca`, `Noi bo`, and `Dai ly`.

Tài xế sử dụng mobile view để xem chuyến xe hiện tại được gán, giao lần lượt từng Delivery Order trong chuyến và ghi nhận kết quả giao hàng. Tại một thời điểm, mỗi tài xế và mỗi xe chỉ được gán cho tối đa một trip đang hoạt động; một trip có thể chứa nhiều Delivery Order. Khi Đại lý nhận hàng, tài xế phải upload 2 ảnh: ảnh hàng đã bàn giao vào điểm nhận và ảnh phiếu xuất kho/biên nhận có chữ ký xác nhận của Đại lý. Sau khi có đủ bằng chứng POD, hệ thống gửi OTP 6 số ngẫu nhiên tới email Đại lý; Đại lý đọc OTP cho tài xế nhập vào hệ thống.

Khi OTP hợp lệ và còn hạn, hệ thống xác nhận giao hàng thành công cho đúng Delivery Order đó, trừ hàng của Delivery Order đó khỏi kho ảo `IN_TRANSIT`, tự động tạo invoice/công nợ và chuyển Delivery Order sang `COMPLETED`. Các Delivery Order khác trong cùng trip không bị thay đổi bởi thao tác xác nhận này.

Nếu Đại lý từ chối nhận hàng hoặc giao hàng thất bại, tài xế bấm nút chuyển Delivery Order sang `RETURNED`; hàng vẫn nằm trong kho ảo `IN_TRANSIT` cho tới khi luồng hoàn hàng riêng tiếp nhận và xử lý. Sau khi mọi Delivery Order trong trip đã `COMPLETED` hoặc `RETURNED`, tài xế có nút xác nhận xe đã quay về kho để chuyển trip sang `COMPLETED`. Việc chuyển Delivery Order từ `RETURNED` sang `DELIVERY_FAILED` không thuộc thao tác của tài xế: nhân viên kho phải đếm số lượng và kiểm tra chất lượng hàng hoàn, Thủ kho duyệt kết quả và lập kế hoạch cất hàng, sau đó nhân viên kho xác nhận cất hàng thành công.

## Clarifications

### Session 2026-08-03

- Q: Ảnh POD được lưu ở dịch vụ object storage hay local storage trên VPS? -> A: Lưu file trong persistent local storage trên VPS; database chỉ lưu relative storage path và metadata, còn frontend xem ảnh qua backend có kiểm tra quyền.
- Q: Sau khi Driver giao hàng thành công, bằng chứng POD được xem lại ở đâu? -> A: Màn hình chi tiết Delivery Order ở trạng thái `COMPLETED` hoặc `CLOSED` phải hiển thị đúng hai ảnh POD đã được chấp nhận của lần giao thành công.
- Q: Với một Delivery Order chia trên nhiều xe, POD và OTP được quản lý theo leg hay theo Delivery Order? -> A: Dùng chung một cặp POD và một OTP cho toàn bộ Delivery Order.
- Q: Driver nào thao tác mobile cho Delivery Order chia nhiều xe? -> A: Chỉ lead driver thao tác toàn bộ split delivery trên mobile: xác nhận xuất phát, ghi nhận đã đến/bàn giao cho toàn bộ đoàn xe, báo giao thất bại nếu có, upload/thay POD, yêu cầu/resend OTP, nhập OTP xác nhận giao hàng, và xác nhận toàn bộ đoàn xe đã quay về kho. Các xe phụ không cần và không được thực hiện thao tác mobile riêng trong flow này.
- Q: Khi nào được bắt đầu bàn giao hàng của Delivery Order chia nhiều xe? -> A: Lead driver xác nhận khi toàn bộ xe phụ trách Delivery Order đã đến điểm giao; hệ thống không yêu cầu từng driver phụ xác nhận arrival/handover riêng.
- Q: Nếu một xe trong split delivery không thể giao hàng thì xử lý thế nào? -> A: Lead driver báo thất bại một lần cho toàn bộ Delivery Order; toàn bộ split plan chuyển sang hoàn hàng và dùng chung luồng hoàn hàng của Delivery Order.
- Q: Có được thay ảnh POD sau khi đã yêu cầu OTP không? -> A: Cho phép thay đồng thời cả cặp ảnh POD; OTP hiện tại bị vô hiệu hóa và phải yêu cầu mã mới.
- Q: Nếu gửi email OTP thất bại thì xử lý thế nào? -> A: Lưu trạng thái `SEND_FAILED` và cho phép gửi lại ngay trên cùng bản ghi OTP.
- Q: Khi OTP thành công có tự động giải phóng toàn bộ xe không? -> A: Không; lead driver xác nhận toàn bộ đoàn xe đã quay về kho bằng một thao tác hoàn tất split delivery, sau đó hệ thống giải phóng toàn bộ xe và driver thuộc split plan.

## 2. Actors

- **Tài xế**: Xem trip hiện tại được gán cho driver profile của mình và các Delivery Order thuộc trip đó, upload `goodsImage`/`signDocumentImage`, yêu cầu OTP, nhập OTP, báo Đại lý từ chối/giao thất bại, và xác nhận xe đã quay về kho.
- **Nhân viên kho**: Đếm số lượng, kiểm tra chất lượng hàng hoàn từ Delivery Order `RETURNED`, và xác nhận cất hàng hoàn theo kế hoạch của Thủ kho.
- **Thủ kho**: Duyệt kết quả số lượng/chất lượng hàng hoàn và lập kế hoạch cất hàng hoàn vào vị trí phù hợp trước khi Delivery Order được đóng là `DELIVERY_FAILED`.
- **Admin**: Reset OTP đã bị khóa sau 3 lần nhập sai để tài xế có thể yêu cầu mã mới.

## 3. Functional Requirements (EARS)

- **Ubiquitous:**
  - The Driver trip list SHALL show all trips assigned to the authenticated driver profile across supported trip types: `DELIVERY` and `TRANSFER`.
  - The Driver trip list SHALL distinguish trip type using both machine-readable `tripType` and Vietnamese labels: `Giao dai ly` for `DELIVERY`, `Dieu chuyen noi bo` for `TRANSFER`.
  - The Driver trip list SHALL provide exactly three primary filters: `Tat ca`, `Noi bo`, and `Dai ly`; `Tat ca` is the default.
  - The `Noi bo` filter SHALL show only `tripType = TRANSFER`; the `Dai ly` filter SHALL show only `tripType = DELIVERY`; filter changes SHALL NOT mutate trip, inventory, delivery attempt, transfer, audit, or resource state.
  - The Driver trip list SHALL use context-appropriate summary fields: delivery trips show dealer stop count, while transfer trips show source warehouse, destination warehouse, and transfer line count instead of dealer delivery points.
  - The Driver trip list SHALL hide POD, OTP, dealer refusal, and delivery-confirmation affordances for `TRANSFER` trips.
  - The Driver trip list SHALL route detail actions by `tripType`: `DELIVERY` opens the Delivery Order POD/OTP workflow, while `TRANSFER` opens the transfer departure/arrival/handover workflow owned by Spec 005.
  - The system SHALL allow Driver users to view and mutate only the current trip assigned to their own driver profile.
  - The system SHALL enforce that a Driver and a vehicle are each assigned to no more than one active trip in status `PLANNED` or `IN_TRANSIT` at a time.
  - The current trip MAY contain multiple Delivery Orders, which the Driver SHALL process individually without changing sibling Delivery Orders that have not yet been completed or returned.
  - The system SHALL reject any driver action when the authenticated user is not linked to the trip `driver_id`.
  - The system SHALL treat the current delivery attempt as the latest `deliveries` record for the given `trip_id`, `do_id`, and authenticated `driver_id` that is not terminal.
  - The system SHALL NOT use `OUT_FOR_DELIVERY` in Sprint 1 delivery attempt status transitions.
  - The system SHALL require full Delivery Order delivery; partial delivery confirmation is not supported in Sprint 1.
  - A split delivery SHALL use exactly one current delivery attempt, one shared POD evidence pair, and one active OTP lifecycle for the whole Delivery Order, not separate POD/OTP records per leg.
  - A split delivery SHALL be a lead-driver-only mobile workflow: only the split plan's lead driver SHALL confirm departure, confirm dealer arrival/handover for the whole split Delivery Order, upload or replace POD evidence, request/resend OTP, confirm delivery using OTP, report delivery failure, and complete the vehicle-return step for the whole split plan.
  - Non-lead split drivers SHALL NOT be required to confirm readiness, departure, dealer arrival, handover, POD, OTP, failure, or return in the mobile workflow.
  - The system SHALL create audit records for every user action in this flow: POD upload, OTP request/resend, OTP confirmation, delivery failure/return, and trip completion.

- **Event-driven:**
  - WHEN a driver opens the mobile trip list, the system SHALL:
    - Return only trips assigned to the authenticated driver profile.
    - Include `tripType`, `tripTypeLabel`, trip number, status, vehicle plate, planned start/end timestamps, total weight, and the type-specific summary needed by the card.
    - Include delivery stop count for `DELIVERY` trips.
    - Include source warehouse, destination warehouse, and transfer line count for `TRANSFER` trips.
    - Preserve cancelled and completed assigned trips in the list when they are in the normal historical result window, while visually separating their status from active work.
    - Return an empty list instead of an error when the driver has no assigned trips.
  - WHEN goods are dispatched for a Delivery Order delivery attempt, the system SHALL:
    - Create a new `deliveries` record for the current physical attempt at trip departure.
    - Assign the next `attempt_number` for that Delivery Order.
    - Initialize the current attempt with status `IN_TRANSIT`.
    - Preserve all previous failed, returned, or completed attempts without overwriting POD, OTP, or failure data.
  - WHEN a driver uploads POD evidence, the system SHALL:
    - Validate the authenticated driver is assigned to the trip.
    - Validate the Delivery Order belongs to the trip and is in `IN_TRANSIT`.
    - Validate the current delivery attempt exists and is the latest non-terminal attempt.
    - Accept multipart fields `goodsImage` and `signDocumentImage` in the same request; both fields SHALL be present and the backend SHALL NOT support single-image POD upload.
    - Require `goodsImage` to show goods handed over/unloaded at the dealer site.
    - Require `signDocumentImage` to show the signed delivery document or receipt confirmation.
    - Reject each image when it is larger than 5 MB or its binary content cannot be decoded as an allowed image format; validation SHALL inspect file signatures/content and SHALL NOT trust only the filename extension or client-supplied MIME type.
    - Accept camera output encoded as JPEG, PNG, or WebP; reject SVG, GIF, renamed non-image files, malformed images, and other unsupported formats.
    - Store images through the Spring Boot backend in a configurable persistent local-storage root on the VPS. Production storage SHALL be outside the application release directory and mounted or retained across application restart, redeployment, and container replacement.
    - Generate server-controlled relative paths in the form `deliveries/{deliveryId}/{evidenceType}-{uuid}.{ext}`; original client filenames SHALL NOT be used as filesystem paths.
    - Treat persistence as one logical operation: if either file write or database update fails, the backend SHALL leave no partial POD state, remove files created by that failed request where applicable, and allow retry with both images.
    - Save only each image's relative storage path and metadata on the current attempt's `deliveries` record; metadata SHALL include evidence type (`GOODS` or `SIGNED_DOCUMENT`), original filename, detected content type, size in bytes, and upload timestamp. The database SHALL NOT store an absolute VPS path, public URL, access URL, or image binary.
    - Allow every authenticated user who is authorized to view the relevant Delivery Order detail under existing role and warehouse-scope rules to view its POD evidence.
    - Serve POD image bytes only through authenticated Spring Boot endpoints after rechecking Delivery Order detail access; the local storage root SHALL NOT be exposed by a public static-resource mapping.
    - Normalize and validate every resolved path under the configured storage root and reject any path that escapes that root.
    - Reading POD evidence SHALL NOT create an audit log.
    - Create `UPLOAD_POD` audit log.
  - WHEN the lead driver replaces POD evidence before delivery confirmation, the system SHALL:
    - Require a new complete pair containing both `goodsImage` and `signDocumentImage`; partial replacement of one image SHALL be rejected.
    - Replace the shared POD pair for the current Delivery Order attempt as one logical operation.
    - Invalidate the current non-consumed OTP by setting its status to `EXPIRED`; any previously issued code SHALL no longer confirm delivery.
    - Require the lead driver to request a new OTP after the replacement succeeds.
    - Create a new `UPLOAD_POD` audit log containing the before/after POD metadata.
  - WHEN a driver requests delivery OTP after both POD images are uploaded, the system SHALL:
    - Validate the authenticated driver is assigned to the trip.
    - Validate the current delivery attempt has relative local-storage paths for both POD images.
    - Generate a random 6-digit numeric OTP.
    - Send the OTP to the dealer email configured on the dealer profile.
    - Store only the OTP hash/verifier, recipient email, `created_at`, `expires_at`, `attempt_count`, and status in `delivery_otp_attempts`.
    - Set OTP validity to 5 minutes from creation time.
    - Maintain exactly one `delivery_otp_attempts` row per current delivery attempt; the first OTP request inserts the row, and later resend after expiry updates that same row.
    - Reject resend while the current OTP is still active and not expired with `OTP_STILL_ACTIVE`; the backend SHALL NOT overwrite the active OTP.
    - If a previous OTP row exists for the current delivery attempt and the driver requests resend after expiry, update that same row by overwriting the OTP hash, reset `created_at`, reset `expires_at`, reset `attempt_count`, clear `consumed_at`, and set status back to `ACTIVE`.
    - If email delivery fails, keep the same OTP row, set status to `SEND_FAILED`, return `OTP_DELIVERY_FAILED`, and allow the lead driver to retry immediately without waiting for `expires_at`.
    - On retry from `SEND_FAILED`, reuse the same OTP row with a newly generated hash and timestamps; never create a second OTP row for the current delivery attempt.
    - Never store raw OTP on `deliveries` or `delivery_otp_attempts`.
    - Create `REQUEST_OTP` audit log.
  - WHEN drivers deliver one Delivery Order through a split delivery plan, the system SHALL:
    - Treat the lead driver as the only mobile actor for the whole split Delivery Order.
    - Allow only the lead driver to confirm departure for the split plan.
    - Allow only the lead driver to confirm that the whole split convoy has arrived at the dealer.
    - Allow only the lead driver to confirm that the whole Delivery Order has been handed over to the dealer.
    - Allow the lead driver to upload the shared POD pair and request/confirm the shared OTP after the lead has confirmed whole-convoy handover.
    - Reject non-lead driver attempts to confirm readiness, departure, arrival, handover, POD, OTP, failure, or return for the split plan with `SPLIT_LEAD_DRIVER_REQUIRED`.
  - WHEN a driver confirms delivery with the OTP read by the dealer, the system SHALL:
    - Validate the authenticated driver is assigned to the trip.
    - Validate the current delivery attempt exists and is still `IN_TRANSIT`.
    - Validate both POD images exist.
    - Validate an OTP record exists for the current delivery attempt.
    - Validate the submitted OTP is exactly 6 digits.
    - Validate the submission time is not later than `delivery_otp_attempts.expires_at`.
    - Reject expired OTP with `DELIVERY_OTP_EXPIRED`.
    - Reject incorrect or missing OTP with `DELIVERY_OTP_INVALID`.
    - Increment `attempt_count` for each incorrect OTP submission.
    - Reject the third incorrect OTP submission with `OTP_MAX_ATTEMPTS_EXCEEDED`, set only the OTP row status to `LOCKED`, and leave the delivery attempt, Delivery Order, trip, inventory, invoice, and receivable states unchanged; after this lock, Driver cannot request a new OTP and Admin must reset the OTP row before the system can generate a new code.
    - Mark the OTP record as successful by setting status `VERIFIED` and `consumed_at`; the OTP SHALL no longer be usable after successful verification.
    - Update the current attempt's `deliveries` record to `DELIVERED`, with POD relative storage paths and metadata, OTP verification timestamp, and delivery timestamp.
    - Confirm the whole Delivery Order only; partial delivery quantities SHALL be rejected.
    - Decrease virtual In-Transit inventory only for this Delivery Order's delivered quantities by item/product/batch.
    - Automatically create invoice and receivable for this Delivery Order only.
    - Move this Delivery Order directly to `COMPLETED`.
    - Run inventory movement, attempt update, OTP update, invoice/receivable creation, and Delivery Order status update in one transaction with optimistic version checks.
    - Create `CONFIRM_DELIVERY` audit log for the user confirmation action.
  - WHEN an authorized user opens the detail of a Delivery Order in `COMPLETED` or `CLOSED`, the system SHALL:
    - Resolve the successful `DELIVERED` attempt that completed the Delivery Order.
    - Show a `Bằng chứng giao hàng` section containing exactly the accepted `goodsImage` and `signDocumentImage` from that successful attempt.
    - Label the two images `Ảnh hàng đã giao` and `Ảnh phiếu giao hàng có chữ ký` so their purposes are unambiguous.
    - Show each image as a preview and allow the user to open a larger view without leaving the Delivery Order detail workflow.
    - Request both image resources from the authenticated backend when the section is opened and retry those requests when loading fails.
    - Keep the rest of the Delivery Order detail available if POD evidence cannot be loaded, show a Vietnamese error state inside the evidence section, and provide a retry action.
    - Treat the successful POD pair as read-only; viewing the Delivery Order detail SHALL NOT allow replacement or deletion after delivery confirmation.
  - WHEN a driver reports dealer refusal or delivery failure, the system SHALL:
    - Validate the authenticated driver is assigned to the trip.
    - Validate the Delivery Order belongs to the trip and is in `IN_TRANSIT`.
    - Require `failureReason`.
    - Update the current attempt's `deliveries` record with status `FAILED` and save `failureReason`.
    - Update Delivery Order status to `RETURNED`.
    - NOT change inventory quantity; goods remain tracked in virtual `IN_TRANSIT` until the separate return flow receives and classifies them.
    - Create `FAIL_DELIVERY` audit log.
    - For a split delivery plan, allow only the lead driver to report failure for the whole Delivery Order.
    - For a split delivery plan, set every active leg and the split plan to `RETURNED`, open only one returned-goods flow for the Delivery Order, and prevent POD or OTP confirmation after the failure is recorded.
  - WHEN the lead driver confirms a split delivery convoy has returned to the source warehouse, the system SHALL:
    - Validate the authenticated driver is the split plan's lead driver.
    - Validate the split Delivery Order is already `COMPLETED` or `RETURNED`.
    - Mark every active split leg trip `COMPLETED` and mark every split leg vehicle and driver `AVAILABLE`.
    - Mark the split delivery plan operationally complete after this single lead-driver return confirmation.
    - Never release any split vehicle or driver merely because the shared OTP was verified and the Delivery Order became `COMPLETED`.
    - Create audit logs covering the whole split-plan return completion.
  - WHEN the assigned driver confirms the vehicle has returned to the source warehouse, the system SHALL:
    - Validate the authenticated driver is assigned to the trip.
    - Validate the trip is `IN_TRANSIT`.
    - Validate every Delivery Order in the trip is `COMPLETED` or `RETURNED`.
    - Move the trip to `COMPLETED`.
    - Mark vehicle and driver as `AVAILABLE`.
    - Keep any returned goods in virtual `IN_TRANSIT` inventory for the separate return flow.
    - Create `COMPLETE_TRIP` audit log.
  - WHEN the separate return flow handles a `RETURNED` Delivery Order, the system SHALL:
    - Allow Storekeeper to confirm the returned goods have physically arrived back at the warehouse before warehouse staff can enter inspection results.
    - Keep the Delivery Order in `RETURNED` and move the returned-goods flow to `COUNT_QC_PENDING` after Storekeeper confirms goods arrival.
    - Allow warehouse staff to submit returned-goods inspection by Delivery Order item/product/batch with actual received quantity, quality-passed quantity, quality-failed quantity, and failure reason for failed quantity.
    - Validate that passed quantity plus failed quantity equals actual received quantity for each returned item/product/batch.
    - Keep the Delivery Order in `RETURNED` while staff count/QC, Storekeeper QC decision, putaway planning, or putaway confirmation is pending.
    - Allow Storekeeper to accept or reject the staff count/QC result; rejection SHALL require a rejection reason and SHALL return the flow to staff rework.
    - Allow warehouse staff to revise and resubmit count/QC after Storekeeper rejection until Storekeeper accepts the result.
    - Require Storekeeper acceptance before any returned-goods putaway plan can be created.
    - Allow Storekeeper to create a putaway plan that selects destination warehouse locations for passed goods and failed/quarantine goods according to quality result.
    - Allow warehouse staff to confirm putaway completion only against the Storekeeper-approved plan.
    - Move returned goods out of virtual `IN_TRANSIT` inventory into the planned destination locations only when staff confirms putaway success.
    - Move the Delivery Order from `RETURNED` to `DELIVERY_FAILED` only after Storekeeper goods-arrival confirmation, staff count/QC, Storekeeper QC acceptance, Storekeeper putaway planning, and staff putaway confirmation are all complete.
    - Create audit logs for returned-goods arrival confirmation, count/QC submission, QC acceptance/rejection, putaway planning, and putaway completion.
  - WHEN Admin resets a locked delivery OTP, the system SHALL:
    - Validate the OTP row belongs to the latest current delivery attempt of the Delivery Order.
    - Require `resetReason`.
    - Set OTP status to `EXPIRED`, reset `attempt_count` to 0, clear `consumed_at`, and keep the old OTP hash for audit trace only; the old OTP SHALL NOT be valid after reset.
    - Allow Driver to request a new OTP, which updates the same OTP row with a new hash and expiry time.
    - Create `RESET_DELIVERY_OTP` audit log with before/after state.

### Driver UX Requirements
- **Language and feedback:**
  - All Driver interface content SHALL be in Vietnamese.
  - Driver-facing errors SHALL explain the problem and next action in plain Vietnamese and SHALL NOT expose error codes, stack traces, or technical terminology.
  - Loading, disabled, success, and error states SHALL be visually distinct.
- **Flow and layout:**
  - The Driver list breadcrumb SHALL read `Van hanh / Chuyen xe` and the list title SHALL read `Chuyen xe cua toi`.
  - The list description SHALL avoid delivery-only wording and SHALL read as assigned vehicle trips for the current driver.
  - The list SHALL provide three adjacent filter controls labelled `Tat ca`, `Noi bo`, and `Dai ly`; the active filter SHALL be visually obvious and keyboard/touch accessible.
  - Every trip card SHALL show a visible trip-type badge near the trip number: `Giao dai ly` or `Dieu chuyen noi bo`.
  - A `DELIVERY` trip card SHALL show `Diem giao: N dai ly`; a `TRANSFER` trip card SHALL show `Tuyen: Kho nguon -> Kho dich` and MAY show `Dong hang: N`.
  - The primary delivery flow SHALL remain in this order after a `DELIVERY` trip is opened: view the currently assigned trip; view a Delivery Order in that trip; capture two POD images; request OTP; enter OTP; view the delivery result.
  - If the Driver has no active trip assigned, the interface SHALL show `Hiện không có chuyến xe được giao` and SHALL NOT show delivery actions.
  - The primary action SHALL remain sticky at the bottom and change by step to `Chụp ảnh POD`, `Yêu cầu OTP`, or `Xác nhận giao hàng`.
  - `Báo giao thất bại` SHALL remain available as a visually distinct secondary destructive action for an eligible `IN_TRANSIT` Delivery Order and SHALL open the required `failureReason` form without competing with the sticky primary success action.
  - On activation, a request-producing action SHALL become disabled immediately, show a clear loading state, and prevent duplicate requests until the request completes.
- **Camera and POD capture:**
  - The interface SHALL use browser camera capture through `getUserMedia()` and SHALL NOT use a generic file picker or offer selection from the photo library.
  - The interface SHALL request the rear-facing camera using `facingMode = environment` where supported and SHALL encode captured still images as an allowed image format.
  - If no camera is available or camera permission is denied, the interface SHALL block the POD step and show Vietnamese instructions for granting camera permission again.
  - The interface SHALL provide two separate capture slots labeled for `goodsImage` and `signDocumentImage`; selecting a slot SHALL open the camera for that evidence type only.
  - Each capture SHALL populate or replace only its corresponding in-session preview. Capturing an image SHALL NOT upload it to the backend immediately.
  - The interface SHALL allow the Driver to delete and recapture either preview before submission.
  - Both `goodsImage` and `signDocumentImage` SHALL be required; each file SHALL be a valid image no larger than 5 MB.
- **Network and upload:**
  - Offline operation and draft persistence SHALL NOT be supported.
  - During upload, the interface SHALL show a Vietnamese processing state and prevent progression to OTP.
  - The interface SHALL enable POD submission only after both previews exist and SHALL send both files together in one multipart request; it SHALL never call the POD endpoint with only one image.
  - The interface SHALL progress to OTP only after both images have uploaded successfully.
  - If the network is weak or upload fails, the interface SHALL retain both available previews for the current browser session, explain the failure in plain Vietnamese, and allow retry without recapturing successful or retained images.
- **OTP entry:**
  - The OTP form SHALL contain six single-digit fields, use the numeric mobile keyboard, move focus forward after entry, and support deletion with focus returning to the previous field.
  - The interface SHALL display the remaining OTP validity time and the number of incorrect attempts used or remaining, based on backend data.
  - The interface SHALL provide a resend action only when the OTP has expired and the backend permits resend.
  - During confirmation or resend, the relevant action SHALL disable immediately, show loading, and prevent duplicate requests.
- **Success result:**
  - After valid OTP confirmation, the interface SHALL show an explicit Vietnamese delivery-success message without automatic navigation.
  - The success view SHALL provide `Xem chuyến xe`, which returns the Driver to the current trip detail page.
- **Delivery Order detail POD review:**
  - A `COMPLETED` or `CLOSED` Delivery Order detail SHALL include a visible `Bằng chứng giao hàng` section for users already authorized to view that order.
  - The section SHALL use two stable preview slots labelled `Ảnh hàng đã giao` and `Ảnh phiếu giao hàng có chữ ký`; loading either image SHALL NOT resize or shift unrelated order-detail content.
  - Selecting a preview SHALL open a larger image viewer with a clear close action and SHALL preserve the user's position in the Delivery Order detail after closing.
  - Loading, unavailable, and retry states SHALL be handled within the evidence section without replacing the whole detail page.
  - The evidence section SHALL be read-only after successful confirmation and SHALL NOT expose upload, replace, or delete controls.

## 4. API Endpoints

- `GET /api/v1/trips/driver` - Driver mobile list of all assigned trips for the authenticated Driver, including both `DELIVERY` and `TRANSFER` trip summaries for filtering.
- `GET /api/v1/trips/driver/{id}` - Driver mobile view for the current trip assigned to the authenticated Driver. This route is driver-scoped and separate from dispatcher/manager trip detail.
- `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/pod-evidence` - Upload POD images for one order in the trip.
- `GET /api/v1/delivery-orders/{doId}/pod-evidence/{evidenceType}` - Stream one locally stored POD image (`GOODS` or `SIGNED_DOCUMENT`) to an authorized Delivery Order detail viewer.
- `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp` - Generate/resend OTP to dealer email after POD evidence exists.
- `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/confirm-delivery` - Confirm full Delivery Order delivery using dealer OTP.
- `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/fail-delivery` - Record dealer refusal or delivery failure.
- `PUT /api/v1/trips/{tripId}/complete` - Assigned driver confirms the vehicle has returned to the source warehouse.
- `PUT /api/v1/split-delivery-plans/{planId}/dealer-arrival` - Lead driver confirms the whole split convoy has arrived at the dealer.
- `PUT /api/v1/split-delivery-plans/{planId}/handover` - Lead driver confirms the whole split Delivery Order was handed over to the dealer.
- `PUT /api/v1/split-delivery-plans/{planId}/fail-delivery` - Lead driver reports failure; the whole Delivery Order and all legs enter return handling.
- `PUT /api/v1/split-delivery-plans/{planId}/complete` - Lead driver confirms the whole split convoy has returned; all split leg trips, drivers, and vehicles are released together.
- `GET /api/v1/delivery-orders/{doId}/returned-goods` - Warehouse-scoped viewer reads the current returned-goods flow state so frontend can resume the correct staff/storekeeper step.
- `PUT /api/v1/delivery-orders/{doId}/returned-goods/receive` - Storekeeper confirms the returned goods have physically arrived back at the warehouse and opens staff count/QC.
- `PUT /api/v1/delivery-orders/{doId}/returned-goods/count-qc` - Warehouse staff submit or resubmit actual, quality-passed, and quality-failed returned quantities with failure reasons for a `RETURNED` Delivery Order.
- `PUT /api/v1/delivery-orders/{doId}/returned-goods/approval` - Storekeeper accepts or rejects returned count/QC; rejection requires a reason and sends the flow back for staff rework.
- `PUT /api/v1/delivery-orders/{doId}/returned-goods/putaway-plan` - Storekeeper creates the destination-location putaway plan for accepted returned goods.
- `PUT /api/v1/delivery-orders/{doId}/returned-goods/putaway-complete` - Warehouse staff confirm returned goods were put away successfully and close the Delivery Order as `DELIVERY_FAILED`.
- `POST /api/v1/admin/delivery-orders/{doId}/delivery-otp/reset` - Admin resets a locked OTP for the current delivery attempt.

### POD evidence request payload

`POST /api/v1/trips/{tripId}/delivery-orders/{doId}/pod-evidence` SHALL accept `multipart/form-data`:

- `goodsImage` - Required image file, max 5MB.
- `signDocumentImage` - Required image file, max 5MB.
- `notes` - Optional driver notes.

Both image fields SHALL be submitted together. If either field is absent or invalid, the backend SHALL reject the whole request with `POD_FILE_INVALID` and SHALL NOT persist either image as accepted POD evidence. The backend SHALL verify actual binary image content and detected format rather than trusting only the extension or declared `Content-Type`.

### POD evidence local-storage metadata and image response

For each POD image, the current delivery attempt SHALL persist:

- `storagePath` - Backend-generated relative path under the configured persistent VPS storage root; the absolute storage root SHALL NOT be persisted.
- `evidenceType` - `GOODS` for `goodsImage` or `SIGNED_DOCUMENT` for `signDocumentImage`.
- `originalFilename` - Client-provided filename retained as metadata only and never used as the storage path without backend sanitization.
- `contentType` - Backend-detected content type.
- `sizeBytes` - Validated file size in bytes.
- `uploadedAt` - Server-generated ISO 8601 upload timestamp with timezone.

The configured POD storage root SHALL:

- Resolve to persistent local storage on the VPS, outside the application release directory; `/var/lib/wms/pod-evidence` is the recommended production location.
- Be readable and writable only by the operating-system account running the backend and authorized VPS administrators.
- Be included in operational backup and restore procedures.
- Never be exposed through `/uploads/**`, a public web-server alias, or another unauthenticated static route.

`GET /api/v1/delivery-orders/{doId}/pod-evidence/{evidenceType}` SHALL:

- Require authentication and apply the same authorization and warehouse-scope checks used to view the Delivery Order detail.
- Return evidence only from the successful `DELIVERED` attempt that completed a `COMPLETED` or `CLOSED` Delivery Order.
- Accept only `GOODS` or `SIGNED_DOCUMENT` as `evidenceType`.
- Resolve the selected relative path under the configured storage root after normalization and root-boundary validation.
- Stream the image bytes with the stored detected `Content-Type`, `Content-Length`, and an inline content disposition using a sanitized filename.
- Never return the absolute VPS filesystem path.
- Return `POD_EVIDENCE_NOT_FOUND` when the metadata or local file is missing and SHALL NOT reveal filesystem details.
- Not create an audit log for image viewing.

If the Delivery Order has no complete POD evidence, the endpoint SHALL return `POD_EVIDENCE_NOT_FOUND`. If the authenticated user cannot view the Delivery Order detail, the endpoint SHALL return the existing authorization failure without revealing whether POD objects exist.

### Delivery OTP request payload

`POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp` SHALL accept:

- `resend` - Optional boolean. If true and the previous OTP is expired, backend SHALL update the current OTP row for this delivery attempt. If the previous OTP is still active, backend SHALL reject with `OTP_STILL_ACTIVE`.

Backend SHALL generate the 6-digit OTP; client SHALL NOT submit an OTP value in this request.

### Delivery OTP response contract

`POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp` SHALL return a `DeliveryOtpResponse` containing:

- `deliveryId` - Current delivery attempt identifier.
- `recipientEmail` - Dealer email address to which the OTP was sent; the Driver UI SHOULD mask part of the address when displayed.
- `status` - Current OTP status: `PENDING`, `ACTIVE`, `SEND_FAILED`, `EXPIRED`, `LOCKED`, or `VERIFIED`.
- `expiresAt` - Server-generated ISO 8601 timestamp with timezone indicating when the OTP expires.
- `attemptCount` - Number of incorrect submissions for the current OTP, from `0` through `3`.
- `maxAttempts` - Maximum incorrect submissions allowed; SHALL be `3` in Sprint 1.
- `remainingAttempts` - `max(0, maxAttempts - attemptCount)` calculated by the backend.
- `canResend` - Server-authoritative resend permission at response time. It SHALL be `true` only when the current OTP is expired, the OTP is not locked or verified, and the current delivery attempt remains eligible for OTP issuance.

The response SHALL NOT contain the raw OTP or OTP hash. The Driver UI MAY use `expiresAt` to update the countdown locally, but backend time and backend resend validation SHALL remain authoritative. Reaching zero in the client countdown SHALL NOT bypass backend validation.

When `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/confirm-delivery` rejects an incorrect, expired, or locked OTP, the error response SHALL retain the existing error `code` and additionally include an `otp` object with `status`, `expiresAt`, `attemptCount`, `maxAttempts`, `remainingAttempts`, and `canResend`. Specifically:

- `DELIVERY_OTP_INVALID` SHALL return the incremented `attemptCount` and updated `remainingAttempts`.
- The third incorrect submission SHALL return `OTP_MAX_ATTEMPTS_EXCEEDED`, `status = LOCKED`, `attemptCount = 3`, `remainingAttempts = 0`, and `canResend = false`.
- `DELIVERY_OTP_EXPIRED` SHALL return `status = EXPIRED`; `canResend` SHALL reflect whether the backend currently permits resend.
- `OTP_RESET_REQUIRED` SHALL return `status = LOCKED`, `remainingAttempts = 0`, and `canResend = false`.

The Driver UI SHALL use this response metadata rather than maintaining its own authoritative attempt count. Driver-facing messages SHALL translate the error outcome into plain Vietnamese and SHALL NOT display the backend error `code`.

### Confirm delivery request payload

`PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/confirm-delivery` SHALL accept:

- `otp` - Required 6-digit numeric OTP read by the dealer.
- `notes` - Optional driver notes.

### Fail delivery request payload

`PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/fail-delivery` SHALL accept:

- `failureReason` - Required reason for dealer refusal or delivery failure.
- `notes` - Optional driver notes.

### Complete trip request payload

`PUT /api/v1/trips/{tripId}/complete` SHALL accept:

- `returnedAt` - Optional vehicle return timestamp from the client; backend SHALL store server timestamp as authoritative audit time.
- `notes` - Optional driver return notes.

### Admin reset OTP request payload

`POST /api/v1/admin/delivery-orders/{doId}/delivery-otp/reset` SHALL accept:

- `resetReason` - Required reason for resetting a locked OTP.
- `notes` - Optional admin notes.

## 5. Error Codes

| Error                          | HTTP | Condition                                                                                                                  |
| ------------------------------ | ---- | -------------------------------------------------------------------------------------------------------------------------- |
| `DRIVER_NOT_ASSIGNED_TO_TRIP`  | 403  | Authenticated driver is not assigned to the trip.                                                                          |
| `DELIVERY_ATTEMPT_NOT_FOUND`   | 404  | Current delivery attempt does not exist for trip/DO/driver.                                                                |
| `DELIVERY_ATTEMPT_NOT_CURRENT` | 409  | Request targets an old or terminal delivery attempt.                                                                       |
| `DELIVERY_ALREADY_FINALIZED`   | 409  | Delivery attempt is already `DELIVERED`, `FAILED`, or `RETURNED`.                                                          |
| `POD_FILE_INVALID`             | 400  | Either POD file is missing, larger than 5 MB, malformed, not an image, or uses an unsupported binary image format.         |
| `POD_EVIDENCE_NOT_FOUND`       | 404  | The successful attempt has no complete POD metadata pair or a referenced local file is missing/unreadable.                 |
| `POD_STORAGE_UNAVAILABLE`      | 503  | The configured persistent POD storage root is unavailable or not writable.                                                 |
| `MISSING_POD`                  | 400  | Confirmation or OTP request is attempted before both POD images exist.                                                     |
| `DEALER_EMAIL_MISSING`         | 422  | Dealer profile has no email for OTP delivery.                                                                              |
| `OTP_NOT_REQUESTED`            | 400  | Delivery confirmation is attempted before OTP is requested.                                                                |
| `DELIVERY_OTP_INVALID`         | 400  | OTP is incorrect or not issued for this delivery attempt.                                                                  |
| `DELIVERY_OTP_EXPIRED`         | 400  | OTP is expired.                                                                                                            |
| `OTP_STILL_ACTIVE`             | 409  | Driver requested resend while the current OTP is still valid.                                                              |
| `OTP_MAX_ATTEMPTS_EXCEEDED`    | 423  | OTP has been entered incorrectly 3 times and requires Admin reset.                                                         |
| `OTP_RESET_REQUIRED`           | 423  | OTP is locked and must be reset by Admin before a new code can be generated.                                               |
| `OTP_DELIVERY_FAILED`          | 502  | OTP email could not be sent; the OTP row is `SEND_FAILED` and may be retried immediately.                                  |
| `SPLIT_LEAD_DRIVER_REQUIRED`   | 403  | A non-lead split driver attempted a lead-only split delivery action.                                                       |
| `SPLIT_DELIVERY_INCOMPLETE`    | 422  | POD or OTP was attempted before the lead driver confirmed whole-convoy arrival and handover.                               |
| `SPLIT_LEG_DRIVER_MISMATCH`    | 403  | Driver attempted to mutate a split leg outside the lead-driver-only workflow.                                              |
| `PARTIAL_DELIVERY_NOT_ALLOWED` | 422  | Request attempts to deliver less than the full Delivery Order.                                                             |
| `IN_TRANSIT_STOCK_NOT_FOUND`   | 422  | Required In-Transit inventory rows are missing or insufficient for this DO.                                                |
| `INVOICE_ALREADY_EXISTS`       | 409  | Invoice already exists for the Delivery Order.                                                                             |
| `TRIP_NOT_READY_TO_COMPLETE`   | 422  | Trip cannot complete because the vehicle return is not confirmed or assigned orders are not all `COMPLETED` or `RETURNED`. |
| `INVENTORY_VERSION_CONFLICT`   | 409  | Concurrent inventory update conflict.                                                                                      |

## 6. Acceptance Criteria

- **Scenario: Driver filters assigned trip list by type**
  - Given a Driver is assigned to at least one `DELIVERY` trip and at least one `TRANSFER` trip
  - When the Driver opens the mobile trip list
  - Then the interface SHALL show the title `Chuyen xe cua toi`.
  - And it SHALL show filter controls `Tat ca`, `Noi bo`, and `Dai ly`.
  - And every trip card SHALL display a type badge: `Giao dai ly` or `Dieu chuyen noi bo`.
  - When the Driver selects `Noi bo`
  - Then only `TRANSFER` trips SHALL remain visible and their cards SHALL show route/source-destination information instead of dealer delivery point wording.
  - When the Driver selects `Dai ly`
  - Then only `DELIVERY` trips SHALL remain visible and their cards SHALL show dealer stop count.

- **Scenario: Driver sees only the current assigned trip**
  - Given a Driver is assigned to one active trip containing one or more Delivery Orders
  - When the Driver opens the mobile Driver view
  - Then the interface SHALL show that current trip and its Delivery Orders.
  - And the Driver SHALL NOT be able to view or mutate a trip assigned to another Driver.

- **Scenario: Driver and vehicle cannot have overlapping active trips**
  - Given a Driver or vehicle is already assigned to a trip in `PLANNED` or `IN_TRANSIT`
  - When Dispatcher attempts to assign that same Driver or vehicle to another active trip
  - Then the system SHALL reject the overlapping assignment.

- **Scenario: Driver has no current trip**
  - Given the authenticated Driver has no trip in `PLANNED` or `IN_TRANSIT`
  - When the Driver opens the mobile Driver view
  - Then the interface SHALL show `Hiện không có chuyến xe được giao`.
  - And delivery actions SHALL NOT be available.

- **Scenario: Delivery success with POD and OTP**
  - Given a Delivery Order in `IN_TRANSIT`
  - And the latest current delivery attempt exists for this Delivery Order and trip
  - And the authenticated driver is assigned to the trip
  - When Driver uploads `goodsImage` and `signDocumentImage`
  - And Driver requests an OTP sent to the dealer email
  - And Driver submits the valid 6-digit OTP within 5 minutes
  - Then the system SHALL save POD evidence on the current attempt, mark OTP as verified and consumed, decrease virtual In-Transit inventory for this Delivery Order only, auto-create invoice/receivable, mark attempt `DELIVERED`, and move the Delivery Order to `COMPLETED`.

- **Scenario: Missing POD evidence blocks OTP and delivery confirmation**
  - Given a Delivery Order in `IN_TRANSIT`
  - When Driver requests OTP or submits delivery confirmation without both required POD images
  - Then the system SHALL reject the request with `MISSING_POD`.

- **Scenario: POD endpoint rejects single-image and invalid-content uploads atomically**
  - Given the Driver submits only one required image, a renamed non-image file, malformed image bytes, an unsupported format, or an image larger than 5 MB
  - When the backend validates the multipart POD request
  - Then the system SHALL reject the whole request with `POD_FILE_INVALID`.
  - And it SHALL NOT accept either image, persist partial POD metadata, or leave an object created by the failed request.

- **Scenario: Expired OTP requires resend**
  - Given a 6-digit OTP was generated more than 5 minutes ago
  - When Driver submits that OTP
  - Then the system SHALL reject the request with `DELIVERY_OTP_EXPIRED`.
  - When Driver requests resend after expiry
  - Then the system SHALL update the current OTP row with a new OTP hash and new expiry time.

- **Scenario: Resend is blocked while OTP is still active**
  - Given a 6-digit OTP was generated less than 5 minutes ago and has not been consumed
  - When Driver requests resend
  - Then the system SHALL reject the request with `OTP_STILL_ACTIVE` and keep the current OTP row unchanged.

- **Scenario: OTP locks after 3 incorrect submissions**
  - Given a Delivery Order in `IN_TRANSIT` with an active OTP
  - When Driver submits incorrect OTP 3 times
  - Then the third submission SHALL be rejected with `OTP_MAX_ATTEMPTS_EXCEEDED` and OTP metadata containing `status = LOCKED`, `attemptCount = 3`, `remainingAttempts = 0`, and `canResend = false`.
  - And the delivery attempt SHALL remain `IN_TRANSIT`, the Delivery Order and trip statuses SHALL remain unchanged, and inventory, invoice, and receivable data SHALL remain unchanged.
  - And the system SHALL require Admin reset before a new OTP can be generated.

- **Scenario: Admin resets locked OTP**
  - Given a Delivery Order in `IN_TRANSIT` with OTP locked after 3 incorrect submissions
  - When Admin submits `resetReason`
  - Then the system SHALL mark the current OTP row `EXPIRED`, reset `attempt_count` to 0, create `RESET_DELIVERY_OTP` audit log, and allow Driver to request a new OTP on the same row.

- **Scenario: Invalid OTP blocks delivery confirmation**
  - Given a Delivery Order in `IN_TRANSIT` with both POD images uploaded
  - When Driver submits an invalid OTP
  - Then the system SHALL reject the request with `DELIVERY_OTP_INVALID`.
  - And the error response SHALL include the incremented `attemptCount`, `maxAttempts = 3`, updated `remainingAttempts`, current `expiresAt`, OTP `status`, and `canResend`.
  - And the interface SHALL display the remaining attempts in plain Vietnamese without displaying `DELIVERY_OTP_INVALID`.

- **Scenario: Dealer refuses delivery**
  - Given a Delivery Order in `IN_TRANSIT`
  - And the authenticated driver is assigned to the trip
  - When Driver records dealer refusal with `failureReason`
  - Then the system SHALL close the current attempt as `FAILED`, move the Delivery Order to `RETURNED`, and keep goods in virtual In-Transit for the separate return flow.

- **Scenario: Split delivery is operated only by the lead driver**
  - Given one Delivery Order is being delivered by two or more split legs
  - And the authenticated driver is the split plan's lead driver
  - When the lead driver confirms departure, whole-convoy dealer arrival, and whole-Delivery-Order handover
  - Then the lead driver MAY upload the one shared POD pair and request the one shared OTP.
  - And no non-lead driver SHALL need or be allowed to confirm readiness, arrival, handover, POD, OTP, failure, or return.

- **Scenario: Lead driver reports split delivery failure for the whole Delivery Order**
  - Given a split Delivery Order is `IN_TRANSIT`
  - When the lead driver reports delivery failure with a reason
  - Then the system SHALL mark the current delivery attempt `FAILED`, the Delivery Order and split plan `RETURNED`, and every active leg `RETURNED`.
  - And the system SHALL keep all quantities in virtual In-Transit for one Delivery Order-level returned-goods flow.
  - And no split leg SHALL continue handover, POD, or OTP confirmation.

- **Scenario: Replacing POD invalidates the current OTP**
  - Given the lead driver uploaded both POD images and an OTP is current
  - When the lead driver replaces both POD images in one request before delivery confirmation
  - Then the system SHALL replace the shared POD pair atomically and set the current OTP status to `EXPIRED`.
  - And the old OTP SHALL no longer confirm delivery.
  - And the lead driver SHALL request a new OTP before confirming delivery.

- **Scenario: Failed OTP email can be retried immediately**
  - Given the shared POD pair exists for the current Delivery Order attempt
  - When the backend cannot send the generated OTP email
  - Then the system SHALL keep the single OTP row with status `SEND_FAILED` and return `OTP_DELIVERY_FAILED`.
  - When the lead driver retries immediately
  - Then the system SHALL update that same row with a newly generated OTP and attempt email delivery again without waiting for the previous expiry time.

- **Scenario: Lead driver releases the whole split convoy**
  - Given a split Delivery Order has been completed by the shared OTP or moved to `RETURNED` after failure
  - When the lead driver confirms the split convoy has returned
  - Then the system SHALL complete every split leg trip and mark every split leg driver and vehicle `AVAILABLE`.
  - And non-lead drivers SHALL NOT need to confirm vehicle return separately.
  - And successful shared OTP confirmation SHALL NOT automatically release split drivers or vehicles before the lead confirms return.

- **Scenario: Driver completes trip after vehicle returns**
  - Given a trip is `IN_TRANSIT`
  - And every Delivery Order in the trip is `COMPLETED` or `RETURNED`
  - When the assigned driver confirms the vehicle has returned to the source warehouse
  - Then the system SHALL mark the trip `COMPLETED`, mark vehicle and driver `AVAILABLE`, and create `COMPLETE_TRIP` audit log.
  - And any returned Delivery Order SHALL remain `RETURNED` until the separate returned-goods warehouse flow is complete.

- **Scenario: Warehouse completes returned-goods flow after failed delivery**
  - Given a Delivery Order is `RETURNED` because dealer refused delivery or delivery failed
  - And the returned goods are still tracked in virtual In-Transit inventory
  - When Storekeeper confirms the goods have physically arrived back at the warehouse
  - And warehouse staff submit actual received quantity, quality-passed quantity, quality-failed quantity, and failure reason for every returned item/product/batch
  - And Storekeeper accepts the returned quantity and quality results
  - And Storekeeper creates a putaway plan with the destination warehouse location
  - And warehouse staff confirm the returned goods were put away successfully according to that plan
  - Then the system SHALL move the returned goods from virtual In-Transit inventory to the planned destination location.
  - And the system SHALL move the Delivery Order from `RETURNED` to `DELIVERY_FAILED`.
  - And the system SHALL create audit logs for arrival confirmation, count/QC submission, Storekeeper acceptance, putaway planning, and putaway completion.

- **Scenario: Storekeeper rejects returned-goods QC and staff reworks**
  - Given a Delivery Order is `RETURNED`
  - And Storekeeper has confirmed the goods arrived back at the warehouse
  - And warehouse staff have submitted returned count/QC results
  - When Storekeeper rejects the count/QC result with a rejection reason
  - Then the Delivery Order SHALL remain `RETURNED`
  - And the returned-goods flow SHALL move back to staff rework for count/QC resubmission
  - And warehouse staff SHALL be able to submit corrected actual, quality-passed, quality-failed, and failure-reason values
  - And Storekeeper SHALL be able to review the corrected result again until accepted.

- **Scenario: Sticky primary action follows the delivery step**
  - Given the Driver is completing a Delivery Order
  - When the Driver advances from POD capture to OTP request and then OTP confirmation
  - Then the sticky bottom action SHALL respectively show `Chụp ảnh POD`, `Yêu cầu OTP`, and `Xác nhận giao hàng`.
  - And each request-producing action SHALL disable immediately, show loading, and prevent a duplicate request after it is activated.

- **Scenario: Camera-only POD capture with previews**
  - Given the Driver is at the POD step with browser camera permission
  - When the Driver captures `goodsImage` and `signDocumentImage`
  - Then the interface SHALL use `getUserMedia()` with rear-camera preference and SHALL NOT expose a generic file picker or photo-library selection.
  - And the `goodsImage` and `signDocumentImage` slots SHALL capture and replace their own previews independently.
  - And capturing either image SHALL NOT call the backend upload endpoint.
  - And each captured image SHALL have a preview that can be replaced or deleted and recaptured before upload.
  - And the interface SHALL reject a missing file, non-image file, or image larger than 5 MB with a plain Vietnamese message.

- **Scenario: Camera unavailable or permission denied blocks POD**
  - Given the phone has no available camera or the Driver denies camera permission
  - When the Driver attempts to capture POD evidence
  - Then the interface SHALL block progression beyond the POD step.
  - And it SHALL show plain Vietnamese instructions for granting camera permission again without exposing an error code, stack trace, or technical terminology.

- **Scenario: Private VPS POD storage is persistent and not publicly exposed**
  - Given both valid POD images are ready for upload
  - When the Driver uploads the evidence
  - Then the Spring Boot backend SHALL store both images under the configured persistent local-storage root on the VPS.
  - And the database SHALL store only backend-generated relative paths and metadata.
  - And neither the absolute VPS path nor a public static URL SHALL be returned to the frontend.
  - And the files SHALL remain available after application restart and redeployment.

- **Scenario: Authorized Delivery Order viewer loads locally stored POD images**
  - Given both POD relative storage paths and metadata are stored for a Delivery Order
  - And the authenticated user is authorized to view that Delivery Order detail under existing role and warehouse-scope rules
  - When the frontend requests `GOODS` and `SIGNED_DOCUMENT` from `GET /api/v1/delivery-orders/{doId}/pod-evidence/{evidenceType}`
  - Then the backend SHALL stream the corresponding image bytes with the stored media metadata.
  - And it SHALL NOT expose the absolute path or create an audit log for viewing the images.

- **Scenario: Completed Delivery Order detail shows both uploaded POD images**
  - Given a Delivery Order is `COMPLETED` or `CLOSED`
  - And its successful `DELIVERED` attempt contains the accepted `goodsImage` and `signDocumentImage`
  - And the authenticated user is authorized to view that Delivery Order detail
  - When the user opens the Delivery Order detail
  - Then the interface SHALL show a `Bằng chứng giao hàng` section with exactly two previews labelled `Ảnh hàng đã giao` and `Ảnh phiếu giao hàng có chữ ký`.
  - And selecting either preview SHALL open a larger read-only view that can be closed without leaving the order detail.
  - And the interface SHALL NOT show controls to replace or delete either confirmed image.

- **Scenario: POD image loading failure does not block Delivery Order detail**
  - Given an authorized user opens a completed Delivery Order detail
  - When one or both POD images cannot be loaded from local storage
  - Then the rest of the Delivery Order detail SHALL remain usable.
  - And the `Bằng chứng giao hàng` section SHALL show a Vietnamese error state and a retry action.
  - And retry SHALL request both authenticated image endpoints again.

- **Scenario: Unauthorized user cannot read local POD files**
  - Given an authenticated user is not authorized to view the Delivery Order detail
  - When that user requests either POD image endpoint
  - Then the backend SHALL reject access without reading the file or revealing whether POD evidence exists.

- **Scenario: POD storage path cannot escape the configured root**
  - Given a stored path or request would resolve outside the configured POD storage root
  - When the backend resolves the requested evidence file
  - Then the backend SHALL reject the operation without reading or writing any file outside the configured root.

- **Scenario: Failed POD upload can be retried in the current session**
  - Given the Driver has captured both required POD images
  - When a weak network or upload failure prevents one or both uploads from completing
  - Then the interface SHALL retain the available image previews in the current browser session and SHALL NOT proceed to OTP.
  - And it SHALL show a plain Vietnamese failure reason and a retry action without requiring the Driver to recapture retained images.
  - When both uploads later succeed
  - Then the `Yêu cầu OTP` action SHALL become available.

- **Scenario: Driver can report delivery failure from the mobile order view**
  - Given a Delivery Order is eligible for failure reporting while `IN_TRANSIT`
  - When the Driver opens that Delivery Order
  - Then the interface SHALL show `Báo giao thất bại` as a visually distinct secondary destructive action.
  - When the Driver activates it
  - Then the interface SHALL require `failureReason` before enabling submission and SHALL preserve the existing failure/return business flow.

- **Scenario: OTP form supports mobile entry and backend-controlled resend**
  - Given an active 6-digit OTP exists for the current delivery attempt
  - When the Driver enters or deletes digits in the OTP form
  - Then the interface SHALL show six single-digit fields, open the numeric keyboard, advance focus after entry, and return focus to the previous field on backward deletion.
  - And it SHALL show the remaining validity time and the incorrect attempts used or remaining from backend response data.
  - And resend SHALL remain disabled until the client countdown reaches `expiresAt` and the latest backend state does not prohibit resend; the backend SHALL make the authoritative decision when resend is requested.
  - When the Driver confirms or resends
  - Then the relevant action SHALL disable immediately, show loading, and prevent duplicate requests.

- **Scenario: Successful confirmation remains on the result screen**
  - Given both POD images are uploaded and the Driver has entered a valid unexpired OTP
  - When delivery confirmation succeeds
  - Then the interface SHALL show a clear Vietnamese delivery-success message and SHALL NOT navigate automatically.
  - And it SHALL show `Xem chuyến xe`.
  - When the Driver selects `Xem chuyến xe`
  - Then the interface SHALL return to the detail page for the current trip.
