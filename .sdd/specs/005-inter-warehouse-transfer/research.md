# Research: 005 Inter-Warehouse Transfer

## Decision: Use `sent_qty` as the loaded/unshipped marker

**Rationale**: The business status remains `APPROVED` before driver departure. `sent_qty == null` means goods are not loaded; `sent_qty != null` means source storekeeper has loaded goods and cancellation must be blocked until `/unship` clears sent quantities. This avoids adding another workflow status.

**Alternatives considered**: Add `LOADED` status. Rejected because the user explicitly accepted using `sent_qty` and the spec keeps status `APPROVED` until driver departure.

## Decision: Implement transfer endpoints under `/api/v1/inter-warehouse-transfers`

**Rationale**: Existing implementation exposes the transfer aggregate through `InterWarehouseTransferController` at `/api/v1/inter-warehouse-transfers`. Transfer operations are state transitions on one aggregate, so sub-actions like `/approve`, `/ship`, `/depart`, `/receive-check`, and `/final-receive` are appropriate.

**Alternatives considered**: Separate resources like `/api/v1/transfer-shipments`. Rejected because it would split one transactional aggregate across unrelated controllers.

## Decision: Keep source lot allocation out of spec 005

**Rationale**: User clarified source lot allocation is not part of this transfer module. The feature will operate on product, warehouse, location, and aggregate inventory quantities only.

**Alternatives considered**: Transfer item source-lot allocation. Rejected by current business clarification.

## Decision: Enforce duplicate external instruction on active transfers only

**Rationale**: `externalInstructionCode + sourceWarehouse + destinationWarehouse + documentDate` prevents duplicate active work while still allowing corrected re-entry after `REJECTED` or `CANCELLED`.

**Alternatives considered**: Globally unique `externalInstructionCode`. Rejected because external company codes may be reused across corrected/cancelled documents.

## Decision: Route QC failed transfer stock to destination quarantine location

**Rationale**: Constitution requires QC-failed goods to enter quarantine and be excluded from available inventory. Storekeeper chooses destination location only for QC-passed quantity; system finds active destination quarantine location.

**Alternatives considered**: Ask storekeeper to choose quarantine location manually. Rejected to reduce user error and match clarified rule.

## Decision: Map transfer quarantine outcomes to spec 009 by physical condition

**Rationale**: Internal-transfer damage has no supplier return relationship. Physically damaged transfer goods remain in the warehouse where they are quarantined and are disposed under spec 009. A shortage is not physical stock and therefore creates only a transfer discrepancy adjustment. An intact wrong SKU can safely use Return to Source and is not a disposal candidate.

**Alternatives considered**: Allow RTV for all quarantine goods. Rejected because internal transfers have no supplier claim. Return all damaged transfer goods to the source warehouse. Rejected because it only moves the internal handling responsibility and adds unnecessary risk. Dispose every transfer exception. Rejected because shortages are not physical goods and intact wrong-SKU shipments remain recoverable.

## Decision: Calculate destination quantity and value from actual receipt only

**Rationale**: If 30 units are sent and 28 physically arrive, the destination imports and calculates value for 28 units only. The missing 2 units remain a quantity-only `TRANSFER_DISCREPANCY` and are excluded from the destination receipt amount and all commercial billing.

**Alternatives considered**: Include all 30 units in destination value. Rejected because two units were not physically received. Calculate a monetary loss inside the transfer receiving flow. Rejected by the business decision to keep missing units as quantity-only discrepancy. Automatically charge the driver for two units. Rejected because responsibility requires a separate investigation and approval process.

## Decision: Wrong-SKU return requires destination report and manager approval

**Rationale**: The destination Storekeeper is the operational checker who identifies the wrong SKU, while the destination Warehouse Manager is accountable for authorizing the vehicle to turn back. Keeping the same transfer, trip, driver, and In-Transit stock avoids false receipt and duplicate shipment records. Source receiving then repeats count, check/QC, and final confirmation before stock re-enters the source warehouse.

**Alternatives considered**: Let Storekeeper turn the vehicle back immediately. Rejected because it bypasses manager control and audit. Put intact wrong SKU into Quarantine. Rejected because the goods are not damaged. Create a new transfer for the return leg. Rejected for Sprint 1 because it duplicates documents and breaks traceability to the original trip.

## Decision: Tests are mandatory for this feature

**Rationale**: Constitution and AGENTS require service coverage, endpoint integration tests, inventory invariant tests, and audit logging verification for warehouse operations.

**Alternatives considered**: Generate implementation tasks first and tests later. Rejected because transfer touches inventory, reservation, authorization, and audit.

## Decision: Model manager-initiated replenishment as TransferRequest before TRF

**Rationale**: A warehouse manager can identify shortage by viewing read-only stock in other warehouses, but that request still needs CEO approval before warehouse execution. Keeping this as `transfer_requests` avoids overloading executable `transfers` statuses and preserves the existing `TRF` flow where inventory is reserved only after source manager approval.

**Alternatives considered**: Let the warehouse manager create a `TRF` directly. Rejected because it bypasses CEO approval and Planner responsibility. Auto-create `TRF` immediately after CEO approval was also rejected because the source Planner still needs to receive the approved template and create the operational document with traceability.
