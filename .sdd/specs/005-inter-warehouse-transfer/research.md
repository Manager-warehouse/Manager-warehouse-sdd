# Research: 005 Inter-Warehouse Transfer

## Decision: Use `sent_qty` as the loaded/unshipped marker

**Rationale**: The business status remains `APPROVED` before driver departure. `sent_qty == null` means goods are not loaded; `sent_qty != null` means source storekeeper has loaded goods and cancellation must be blocked until `/unship` clears sent quantities. This avoids adding another workflow status.

**Alternatives considered**: Add `LOADED` status. Rejected because the user explicitly accepted using `sent_qty` and the spec keeps status `APPROVED` until driver departure.

## Decision: Implement transfer endpoints under `/api/v1/transfers`

**Rationale**: Existing project convention uses REST resources under `/api/v1`. Transfer operations are state transitions on one aggregate, so sub-actions like `/approve`, `/ship`, `/depart`, `/receive-check` are appropriate.

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

## Decision: Tests are mandatory for this feature

**Rationale**: Constitution and AGENTS require service coverage, endpoint integration tests, inventory invariant tests, and audit logging verification for warehouse operations.

**Alternatives considered**: Generate implementation tasks first and tests later. Rejected because transfer touches inventory, reservation, authorization, and audit.

## Decision: Model manager-initiated replenishment as TransferRequest before TRF

**Rationale**: A warehouse manager can identify shortage by viewing read-only stock in other warehouses, but that request still needs CEO approval before warehouse execution. Keeping this as `transfer_requests` avoids overloading executable `transfers` statuses and preserves the existing `TRF` flow where inventory is reserved only after source manager approval.

**Alternatives considered**: Let the warehouse manager create a `TRF` directly. Rejected because it bypasses CEO approval and Planner responsibility. Auto-create `TRF` immediately after CEO approval was also rejected because the source Planner still needs to receive the approved template and create the operational document with traceability.
