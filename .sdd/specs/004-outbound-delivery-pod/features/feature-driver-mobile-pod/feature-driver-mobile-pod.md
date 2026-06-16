# Feature: Tai xe Xac nhan Giao hang & POD (US-WMS-09)

## 1. Context and Goal

Tai xe su dung mobile view de xem chuyen xe, giao hang va ghi nhan ket qua giao hang. Neu dai ly nhan hang, tai xe phai upload 2 anh: anh hang da dua xuong xe/chuyen vao dai ly va anh phieu xuat kho co chu ky xac nhan cua dai ly. Sau khi upload du bang chung, he thong gui OTP toi email dai ly; dai ly doc OTP cho tai xe nhap vao he thong. Khi OTP hop le, he thong xac nhan giao hang thanh cong, tru kho ao In-Transit, tu dong tao invoice/cong no va chuyen Delivery Order sang `COMPLETED`. Neu dai ly tu choi nhan hang, Delivery Order chuyen sang `RETURNED`; hang van nam trong kho ao In-Transit cho toi khi luong hoan hang rieng tiep nhan.

## 2. Actors

* **Tai xe**: Xem chuyen xe duoc gan, upload POD images, yeu cau OTP, nhap OTP hoac bao cao dai ly tu choi/giao that bai.

## 3. Functional Requirements (EARS)

* **Event-driven:**
  * WHEN goods are dispatched for a Delivery Order delivery attempt, the system SHALL:
    * Create a new `deliveries` record for the current physical attempt.
    * Assign the next `attempt_number` for that Delivery Order.
    * Preserve all previous failed, returned, or completed attempts without overwriting POD, OTP, or failure data.
  * WHEN a driver uploads POD evidence, the system SHALL:
    * Require one goods handover image showing goods unloaded/transferred into the dealer site.
    * Require one signed delivery document image showing dealer receipt confirmation.
    * Store uploaded images in file storage and save their URLs only on the current attempt's `deliveries` record.
    * Reject images larger than 5MB or non-image file types.
  * WHEN a driver requests delivery OTP after both POD images are uploaded, the system SHALL:
    * Send a one-time OTP to the dealer email configured on the dealer profile.
    * Create a `delivery_otp_attempts` record linked to the current `deliveries` record.
    * Store only the OTP hash/verifier, recipient email, expiry time, and attempt metadata.
    * Never store raw OTP on `deliveries` or `delivery_otp_attempts`.
  * WHEN a driver confirms delivery with the OTP read by the dealer, the system SHALL:
    * Validate that both POD images exist and the OTP is valid and not expired.
    * Mark the active `delivery_otp_attempts` record as consumed after successful verification.
    * Update the current attempt's `deliveries` record with status `DELIVERED`, POD URLs, OTP verification timestamp, and delivery timestamp.
    * Decrease virtual In-Transit inventory for the delivered quantities.
    * Automatically create invoice and receivable for the Delivery Order.
    * Move the Delivery Order directly to `COMPLETED`.
  * WHEN a driver reports dealer refusal or delivery failure, the system SHALL:
    * Update the current attempt's `deliveries` record with status `FAILED` and save the `failure_reason`.
    * Update Delivery Order status to `RETURNED`.
    * Keep goods tracked in virtual In-Transit inventory until the separate return flow receives and classifies the returned goods.
  * WHEN the separate return flow confirms returned goods back into warehouse custody, the outbound flow SHALL allow the Delivery Order to move from `RETURNED` to `DELIVERY_FAILED`.

## 4. API Endpoints

* `GET /api/v1/trips/{id}` - Driver mobile view for an assigned trip.
* `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/pod-evidence` - Upload goods handover image and signed delivery document image for one order in the trip.
* `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp` - Send OTP confirmation email to the dealer after POD evidence exists.
* `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/confirm-delivery` - Confirm successful delivery using dealer OTP.
* `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/fail-delivery` - Record dealer refusal or delivery failure with reason.

## 5. Acceptance Criteria

* **Scenario: Delivery success with POD and OTP**
  * Given a Delivery Order in `IN_TRANSIT`
  * And the current delivery attempt exists for this Delivery Order and trip
  * When Driver uploads the goods handover image and signed delivery document image
  * And Driver requests an OTP sent to the dealer email
  * And Driver submits the valid OTP read by the dealer
  * Then the system SHALL save POD evidence on the current attempt, mark OTP as verified, decrease virtual In-Transit inventory, auto-create invoice/receivable, and move the Delivery Order to `COMPLETED`.

* **Scenario: Missing POD evidence blocks delivery confirmation**
  * Given a Delivery Order in `IN_TRANSIT`
  * When Driver submits delivery confirmation without both required POD images
  * Then the system SHALL reject the request with `MISSING_POD`.

* **Scenario: Invalid or expired OTP blocks delivery confirmation**
  * Given a Delivery Order in `IN_TRANSIT` with both POD images uploaded
  * When Driver submits an invalid or expired OTP
  * Then the system SHALL reject the request with `DELIVERY_OTP_INVALID` or `DELIVERY_OTP_EXPIRED`.

* **Scenario: Dealer refuses delivery**
  * Given a Delivery Order in `IN_TRANSIT`
  * When Driver records dealer refusal with a reason
  * Then the system SHALL close the current attempt as `FAILED`, move the Delivery Order to `RETURNED`, and keep goods in virtual In-Transit for the separate return flow.
