# Research: Driver Mobile POD

## Decision: Keep driver mobile workflow in a dedicated driver-delivery service

**Rationale**: The repository already contains `Delivery`, `DeliveryOtpAttempt`, and trip entities, but the driver actions are materially different from dispatcher trip planning and warehouse outbound preparation. A dedicated service isolates driver assignment checks, POD upload rules, OTP lifecycle, delivery confirmation, and failure handling without overloading trip-planning or delivery-order services.

**Alternatives considered**: Extending `TripServiceImpl` for all mobile actions was rejected because OTP, POD, invoice creation, and delivery-attempt mutation are a separate operational workflow from trip planning.

## Decision: Resolve the current delivery attempt as the latest non-terminal attempt for the trip, Delivery Order, and driver

**Rationale**: The feature spec explicitly defines the current attempt as the latest non-terminal `deliveries` row for the authenticated driver. Centralizing that lookup avoids accidentally mutating old failed or delivered attempts and keeps retry/resend behavior tied to the right physical delivery.

**Alternatives considered**: Addressing attempts directly by attempt ID from the client was rejected because it would let stale mobile state target old attempts.

## Decision: Maintain exactly one OTP row per current delivery attempt

**Rationale**: The spec requires a single `delivery_otp_attempts` row per current attempt, with resend after expiry updating the same row. This keeps audit history focused, simplifies lock/reset logic, and avoids parallel active OTP rows for the same delivery.

**Alternatives considered**: Creating a new OTP row on every resend was rejected because it complicates expiry, lock, and current-attempt validation.

## Decision: Store only OTP hash/verifier and never raw OTP outside the outbound mail payload

**Rationale**: The repo already follows hashed OTP handling for password reset, and the feature spec explicitly forbids storing raw OTP on `deliveries` or `delivery_otp_attempts`. Reusing the same security pattern aligns with the constitution and reduces leakage risk.

**Alternatives considered**: Persisting raw OTP temporarily for troubleshooting was rejected because it violates the spec and weakens security.

## Decision: Block OTP resend while the current OTP is still active

**Rationale**: The spec says active OTP must remain unchanged until expiry or admin reset. Enforcing `OTP_STILL_ACTIVE` prevents churn in dealer communication, keeps the mobile flow deterministic, and avoids invalidating an OTP the dealer may already be reading.

**Alternatives considered**: Allowing immediate resend with silent overwrite was rejected because it causes user confusion and conflicts with the contract.

## Decision: Lock OTP verification after 3 incorrect submissions and require admin reset

**Rationale**: The feature spec elevates repeated wrong OTP attempts into a locked state that blocks both confirmation and new OTP generation until an admin resets the row. Treating that lock as a persisted row status makes the rule explicit and auditable.

**Alternatives considered**: Allowing endless retries with rate limiting was rejected because it conflicts with the required `OTP_MAX_ATTEMPTS_EXCEEDED` and admin-reset workflow.

## Decision: Confirm successful delivery in one transaction with inventory, OTP, invoice, receivable, and Delivery Order status updates

**Rationale**: The spec requires the current attempt update, OTP consumption, `IN_TRANSIT` stock decrement, invoice/receivable creation, and Delivery Order completion to succeed or fail together. A single transaction preserves inventory integrity and prevents cases where a Delivery Order is marked delivered without financial or stock effects.

**Alternatives considered**: Asynchronous invoice creation after delivery confirmation was rejected because it would break the required atomic completion workflow.

## Decision: Keep failed or refused deliveries in virtual `IN_TRANSIT`

**Rationale**: Dealer refusal or failed delivery moves the Delivery Order to `RETURNED` but does not receive stock back into regular inventory. That handoff belongs to the separate return flow, so the POD feature should only close the current attempt and preserve the `IN_TRANSIT` stock position. The separate return flow requires Storekeeper to confirm the goods physically arrived back at the warehouse, warehouse staff to count actual returned quantity and split quality-passed versus quality-failed quantity with failure reasons, Storekeeper to accept or reject the QC result, Storekeeper to create a putaway plan after acceptance, and warehouse staff to confirm putaway before goods leave virtual `IN_TRANSIT` and the Delivery Order moves to `DELIVERY_FAILED`. Storekeeper rejection keeps the Delivery Order `RETURNED` and sends the result back for staff rework.

**Alternatives considered**: Automatically returning failed goods to warehouse inventory was rejected because it bypasses the separate returns and classification workflow.

## Decision: Close returned Delivery Orders only after returned-goods putaway

**Rationale**: Driver trip completion confirms that the vehicle and driver are operationally back, not that returned goods have been received by Storekeeper, counted, quality-checked, accepted, and stored. Keeping the Delivery Order in `RETURNED` until staff putaway confirmation preserves stock accuracy and makes Storekeeper accountable for returned-goods receipt, QC decision, and location planning.

**Alternatives considered**: Moving the Delivery Order to `DELIVERY_FAILED` when the driver confirms vehicle return was rejected because it would close the outbound order before warehouse custody, quantity, quality, and storage location are verified.
