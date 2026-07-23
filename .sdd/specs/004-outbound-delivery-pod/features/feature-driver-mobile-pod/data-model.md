# Data Model: Driver Mobile POD

## Trip

**Purpose**: Operational route visible to the assigned driver.

**Fields used/updated**

- `id`
- `trip_number`
- `trip_type`
- `driver_id`
- `vehicle_id`
- `warehouse_id`
- `status`
- `planned_start_at`
- `planned_end_at`
- `total_weight_kg`
- `total_volume_m3`
- `completed_at`
- `notes`

**Validation rules**

- Driver may view and mutate only trips assigned to their own driver profile.
- Driver list summaries must expose `trip_type` as `tripType` so the UI can filter `Tat ca`, `Noi bo`, and `Dai ly`.
- `trip_type = DELIVERY` maps to `tripTypeLabel = Giao dai ly` and uses Delivery Order stop counts in the card summary.
- `trip_type = TRANSFER` maps to `tripTypeLabel = Dieu chuyen noi bo` and uses transfer source/destination route plus line count in the card summary.
- Read-only list filtering must not mutate trip status, inventory, transfer state, delivery attempts, or audit logs.
- Driver can complete a trip only while the trip is `IN_TRANSIT`.
- Trip completion requires every assigned Delivery Order to be `COMPLETED` or `RETURNED`.

## DriverTripSummaryResponse

**Purpose**: Read-only card model for the driver mobile list, combining outbound delivery and internal transfer trips without mixing their action rules.

**Fields returned**

- `tripId`
- `tripNumber`
- `tripType` (`DELIVERY` or `TRANSFER`)
- `tripTypeLabel` (`Giao dai ly` or `Dieu chuyen noi bo`)
- `status`
- `driverId`
- `vehicleId`
- `vehiclePlate`
- `plannedStartAt`
- `plannedEndAt`
- `totalWeightKg`
- `totalVolumeM3`
- `deliveryStopCount` for `DELIVERY`
- `sourceWarehouseCode`, `destinationWarehouseCode`, and `transferLineCount` for `TRANSFER`

**Validation rules**

- Response rows are generated only for trips assigned to the authenticated driver profile.
- `DELIVERY` rows must not include transfer-only route actions.
- `TRANSFER` rows must not include POD, OTP, dealer refusal, or confirm-delivery affordances.

## TripDeliveryOrder

**Purpose**: Links one Delivery Order to the driver's trip and provides delivery scope for mobile actions.

**Fields used**

- `trip_id`
- `do_id`
- `stop_order`

**Validation rules**

- The target Delivery Order must belong to the trip before POD, OTP, confirmation, or failure actions are allowed.

## Delivery

**Purpose**: Represents the current physical delivery attempt for one Delivery Order.

**Fields used/updated**

- `id`
- `do_id`
- `trip_id`
- `vehicle_id`
- `driver_id`
- `attempt_number`
- `status`
- `pod_image_url`
- `pod_signature_url`
- `pod_timestamp`
- `otp_verified_at`
- `failure_reason`
- `dispatched_at`
- `delivered_at`
- `created_at`
- `updated_at`

**Validation rules**

- The current attempt is the latest non-terminal attempt for `(trip_id, do_id, driver_id)`.
- POD upload only works while the Delivery Order is `IN_TRANSIT` and the current attempt exists.
- OTP request and confirmation require both POD image URLs on the current attempt.
- Successful confirmation updates the attempt to `DELIVERED`.
- Failure updates the current attempt to `FAILED`.

## DeliveryOtpAttempt

**Purpose**: Stores the single active or historical OTP lifecycle for the current delivery attempt.

**Fields used/updated**

- `id`
- `delivery_id`
- `otp_hash`
- `recipient_email`
- `expires_at`
- `consumed_at`
- `status`
- `attempt_count`
- `created_at`

**Validation rules**

- Exactly one row is maintained per current delivery attempt.
- OTP is backend-generated, exactly 6 digits, and only the hash/verifier is stored.
- Active OTP is valid for 5 minutes from `created_at`.
- Resend while active is rejected with `OTP_STILL_ACTIVE`.
- Resend after expiry updates the same row with new hash, new timestamps, reset `attempt_count`, cleared `consumed_at`, and `ACTIVE` status.
- Incorrect OTP increments `attempt_count`.
- After 3 wrong submissions, the row is locked and requires admin reset before a new OTP can be requested.
- Successful verification sets `status = VERIFIED` and `consumed_at`.

## DeliveryOrder

**Purpose**: Outbound order whose delivery outcome is confirmed by the driver.

**Fields used/updated**

- `id`
- `dealer_id`
- `status`
- `updated_at`

**Validation rules**

- Driver mobile actions only operate while the Delivery Order is `IN_TRANSIT`.
- Successful OTP confirmation moves the Delivery Order to `COMPLETED`.
- Failure or dealer refusal moves the Delivery Order to `RETURNED`.
- Outbound flow later allows `RETURNED -> DELIVERY_FAILED` only through the separate return flow after staff count/QC, Storekeeper approval, Storekeeper putaway planning, and staff putaway confirmation are complete.

## ReturnedDeliveryFlow

**Purpose**: Tracks returned goods for a failed/refused Delivery Order while the Delivery Order remains `RETURNED`.

**Fields used/updated**

- `delivery_order_id`
- `status` (`COUNT_QC_PENDING`, `COUNT_QC_SUBMITTED`, `APPROVED`, `PUTAWAY_PLANNED`, `PUTAWAY_COMPLETED`)
- `counted_by_staff_id`
- `approved_by_storekeeper_id`
- `putaway_planned_by_storekeeper_id`
- `putaway_completed_by_staff_id`
- `notes`
- `created_at`
- `updated_at`

**Validation rules**

- A returned flow can be opened only for a Delivery Order in `RETURNED`.
- Warehouse staff submit returned quantity count and quality result by item/product/batch.
- Storekeeper approval is required after staff count/QC and before putaway planning.
- Storekeeper selects the destination warehouse location in the putaway plan.
- Warehouse staff can confirm putaway only for a Storekeeper-approved putaway plan.
- The Delivery Order remains `RETURNED` until putaway is confirmed complete.
- Putaway completion moves returned goods from virtual `IN_TRANSIT` to the planned warehouse location and moves the Delivery Order to `DELIVERY_FAILED`.

## ReturnedDeliveryItemResult

**Purpose**: Captures staff count and quality inspection result for each returned Delivery Order item/product/batch.

**Fields used/updated**

- `returned_flow_id`
- `delivery_order_item_id`
- `product_id`
- `batch_id`
- `expected_qty`
- `counted_qty`
- `quality_result` (`PASSED`, `FAILED`)
- `quality_reason`
- `destination_location_id`

**Validation rules**

- `counted_qty` must be non-negative and must be reviewed by Storekeeper.
- Storekeeper cannot approve the return flow until every expected item/product/batch has a count and quality result.
- Putaway confirmation must match the Storekeeper-approved destination location and approved quantity.

## DeliveryOrderItem

**Purpose**: Provides delivered quantities that must be decremented from virtual `IN_TRANSIT` stock on successful confirmation.

**Fields used**

- `id`
- `delivery_order_id`
- `product_id`
- `batch_id` or batch mapping through allocation lineage
- `issued_qty`

**Validation rules**

- Sprint 1 confirms the whole Delivery Order only; partial delivery is rejected.
- Successful confirmation decrements only this Delivery Order's dispatched quantities from virtual `IN_TRANSIT`.

## Inventory

**Purpose**: Holds virtual `IN_TRANSIT` stock for dispatched but not yet finalized goods.

**Fields used/updated**

- `id`
- `warehouse_id`
- `product_id`
- `batch_id`
- `location_id`
- `total_qty`
- `reserved_qty`
- `version`

**Validation rules**

- Successful delivery decrements virtual `IN_TRANSIT` `total_qty` only for the confirmed Delivery Order's quantities.
- `reserved_qty` remains `0` for virtual `IN_TRANSIT` rows in this flow.
- Failed delivery does not change inventory.
- All successful confirmation updates must pass optimistic locking and keep rows non-negative.

## Dealer

**Purpose**: Supplies the recipient email for OTP delivery.

**Fields used**

- `id`
- `email`

**Validation rules**

- OTP request requires a non-empty dealer email on the Delivery Order's dealer profile.

## Invoice / Receivable

**Purpose**: Financial artifacts created automatically after successful delivery confirmation.

**Fields used/updated**

- Invoice identity and Delivery Order linkage
- Dealer linkage
- Total amount
- Receivable or downstream finance linkage

**Validation rules**

- Successful delivery creates invoice and receivable for the confirmed Delivery Order only.
- Duplicate invoice creation for the same Delivery Order is rejected with `INVOICE_ALREADY_EXISTS`.

## Vehicle

**Purpose**: Operational trip resource released when the trip is completed.

**Fields used/updated**

- `id`
- `status`

**Validation rules**

- Trip completion moves vehicle status from `ON_TRIP` to `AVAILABLE`.

## Driver

**Purpose**: Authenticated mobile actor and trip assignment anchor.

**Fields used/updated**

- `id`
- `user_id`
- `warehouse_id`
- `status`

**Validation rules**

- Driver actions require the authenticated user to map to the trip `driver_id`.
- Trip completion moves driver status from `ON_TRIP` to `AVAILABLE`.

## AuditLog

**Purpose**: Immutable trace for POD and mobile-delivery actions.

**Events**

- `UPLOAD_POD`: POD files accepted and saved on the current attempt.
- `REQUEST_OTP`: first OTP issue or valid resend after expiry.
- `CONFIRM_DELIVERY`: OTP verified, attempt delivered, `IN_TRANSIT` inventory decremented, invoice/receivable created, Delivery Order completed.
- `FAIL_DELIVERY`: current attempt failed and Delivery Order moved to `RETURNED`.
- `RESET_DELIVERY_OTP`: admin reset of a locked OTP row with before/after state.
- `COMPLETE_TRIP`: driver-confirmed vehicle return after all assigned Delivery Orders are terminal.
