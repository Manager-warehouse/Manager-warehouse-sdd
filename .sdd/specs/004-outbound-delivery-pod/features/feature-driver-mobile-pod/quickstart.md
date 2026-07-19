# Quickstart: Driver Mobile POD

## Goal

Implement driver mobile trip view, POD evidence upload, OTP request and confirmation, failed delivery handling, admin OTP reset, and trip completion while preserving trip assignment scope, OTP security, inventory integrity, and audit requirements.

The driver mobile trip list is also the shared list for assigned internal transfer trips. It must use neutral `Chuyen xe` wording, label each row by `tripType`, and let the driver filter `Tat ca`, `Noi bo`, or `Dai ly` before opening the appropriate detail workflow.

## Suggested implementation order

1. Add or extend repositories and DTOs for current delivery-attempt lookup, OTP row lifecycle, multipart POD upload, and admin reset.
2. Add repository helpers to load trip detail for the assigned driver, resolve the current attempt, fetch dealer email, and gather virtual `IN_TRANSIT` inventory rows for the confirmed Delivery Order.
3. Add controller endpoints for:
   - `GET /api/v1/trips/{id}`
   - `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/pod-evidence`
   - `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp`
   - `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/confirm-delivery`
   - `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/fail-delivery`
   - `PUT /api/v1/trips/{tripId}/complete`
   - `POST /api/v1/admin/delivery-orders/{doId}/delivery-otp/reset`
4. Implement service methods that:
   - validate driver assignment or admin authority
   - validate the current attempt and Delivery Order status
   - validate and store POD images
   - generate, resend, verify, lock, and reset OTP on a single row per current attempt
   - confirm successful delivery in one transaction with `IN_TRANSIT` inventory decrement, invoice creation, receivable creation, and Delivery Order completion
   - record failed delivery without changing inventory
   - complete the trip only after all assigned Delivery Orders are terminal
5. Update driver-facing trip response mapping so the mobile UI can show current trip, Delivery Orders, attempts, POD state, and delivery outcome.
6. Update driver-facing list response mapping and frontend normalization so assigned `DELIVERY` and `TRANSFER` trips expose the same card fields plus type-specific summaries.

## Driver list walkthrough

### 0. List and filter assigned trips

```http
GET /api/v1/trips/driver
Authorization: Bearer <driver-jwt>
```

Expected result:

- Return only trips assigned to the authenticated driver profile.
- Include `tripType = DELIVERY` for dealer delivery trips and `tripType = TRANSFER` for internal transfer trips.
- Include `tripTypeLabel` for Vietnamese card badges: `Giao dai ly` or `Dieu chuyen noi bo`.
- Delivery rows include `deliveryStopCount`.
- Transfer rows include `sourceWarehouseCode`, `destinationWarehouseCode`, and `transferLineCount`.
- The frontend title is `Chuyen xe cua toi`.
- The `Tat ca` filter shows every returned row.
- The `Noi bo` filter shows only `TRANSFER` rows and uses route wording.
- The `Dai ly` filter shows only `DELIVERY` rows and uses dealer stop count wording.
- Filter switching is local/read-only and must not create audit records.

## API walkthrough

### 1. Upload POD evidence

Request:

```http
POST /api/v1/trips/9001/delivery-orders/101/pod-evidence
Authorization: Bearer <jwt>
Content-Type: multipart/form-data
```

Form fields:

- `goodsImage`: required image file, max 5MB
- `signDocumentImage`: required image file, max 5MB
- `notes`: optional string

Expected result:

- Validate the authenticated driver is assigned to trip 9001.
- Validate Delivery Order 101 belongs to the trip and is `IN_TRANSIT`.
- Validate the current non-terminal attempt exists.
- Reject non-image files or files above 5MB.
- Store file URLs on the current attempt only.
- Write `UPLOAD_POD` audit.

### 2. Request OTP after POD exists

```http
POST /api/v1/trips/9001/delivery-orders/101/delivery-otp
```

```json
{
  "resend": false
}
```

Expected result:

- Validate both POD image URLs already exist.
- Validate dealer email exists.
- Generate a 6-digit OTP and send it to dealer email.
- Store only hash/verifier, expiry, status, attempt count, and recipient email on one OTP row for the current attempt.
- Reject resend while OTP is still active with `OTP_STILL_ACTIVE`.
- Allow resend after expiry by updating the same OTP row.
- Write `REQUEST_OTP` audit.

### 3. Confirm delivery with OTP

```http
PUT /api/v1/trips/9001/delivery-orders/101/confirm-delivery
```

```json
{
  "otp": "123456",
  "notes": "Dealer confirmed receipt"
}
```

Expected result:

- Validate both POD images exist and an OTP row exists.
- Validate OTP is exactly 6 digits and not expired.
- Increment `attempt_count` on wrong OTP.
- Lock after 3 wrong submissions and reject further requests until admin reset.
- On success, mark OTP `VERIFIED` and consumed.
- Update current attempt to `DELIVERED`.
- Decrease virtual `IN_TRANSIT` inventory only for Delivery Order 101.
- Auto-create invoice and receivable for Delivery Order 101 only.
- Move Delivery Order 101 to `COMPLETED`.
- Write `CONFIRM_DELIVERY` audit.

### 4. Record failed delivery

```http
PUT /api/v1/trips/9001/delivery-orders/101/fail-delivery
```

```json
{
  "failureReason": "Dealer refused delivery due to damaged outer carton",
  "notes": "Will return goods to warehouse"
}
```

Expected result:

- Validate the authenticated driver is assigned to the trip.
- Validate the current attempt exists and Delivery Order is `IN_TRANSIT`.
- Require `failureReason`.
- Mark current attempt `FAILED`.
- Move Delivery Order to `RETURNED`.
- Keep goods in virtual `IN_TRANSIT`.
- Write `FAIL_DELIVERY` audit.

### 5. Admin resets locked OTP

```http
POST /api/v1/admin/delivery-orders/101/delivery-otp/reset
```

```json
{
  "resetReason": "Dealer requested a new OTP after repeated misread",
  "notes": "Support-assisted reset"
}
```

Expected result:

- Validate the OTP row belongs to the latest current attempt for Delivery Order 101.
- Require `resetReason`.
- Mark current OTP row `EXPIRED`, reset `attempt_count`, clear `consumed_at`, and keep old hash only for trace.
- Allow the driver to request a new OTP on the same row.
- Write `RESET_DELIVERY_OTP` audit.

## Required tests

- Service test: driver cannot access a trip or Delivery Order outside the assigned driver profile.
- Service/controller test: assigned trip list returns mixed `DELIVERY` and `TRANSFER` summaries with `tripType` labels and hides other drivers' trips.
- Service test: POD upload rejects missing, oversized, or non-image files.
- Service test: OTP request is blocked before both POD images exist.
- Service test: OTP resend is blocked while active and updates the same row after expiry.
- Service test: wrong OTP increments `attempt_count` and locks after 3 attempts.
- Service test: admin reset clears the lock and allows new OTP generation on the same row.
- Service test: successful confirmation updates attempt, consumes OTP, decrements `IN_TRANSIT` inventory, creates invoice/receivable, and moves Delivery Order to `COMPLETED` in one transaction.
- Service test: failed delivery moves Delivery Order to `RETURNED` without changing inventory.
- Service test: trip completion only works when every assigned Delivery Order is `COMPLETED` or `RETURNED`.
- Controller integration test: POD upload, OTP request, confirm-delivery, fail-delivery, trip-complete, and admin-reset endpoints return expected happy-path and business-error responses.
- Frontend test: driver list filters `Tat ca`, `Noi bo`, and `Dai ly` render the expected card subset and type-specific wording.

## Definition of done reminders

- Never store raw OTP in application persistence.
- Keep all POD and OTP endpoints documented in OpenAPI.
- Do not change inventory for failed delivery; only successful confirmation decrements virtual `IN_TRANSIT`.
- Ensure every driver and admin action writes audit logs with before/after context.
- Keep driver list filters read-only: no inventory, delivery attempt, transfer, trip status, resource, or audit mutation occurs when filtering.
