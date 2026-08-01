# Feature 02: Receipt Counting Within Receive & QC

## Context

WH_STAFF records actual received quantity on the unified "Nhan hang & QC dau vao" screen only after WH_MANAGER approves the created receipt for receiving. Counting remains a distinct business concept from QC classification, Storekeeper review, manager approval, and putaway. Staff entry does not create inventory, batch, putaway, supplier invoice, Debit Note, RTV, or finalized Quarantine stock.

## Actors

| Actor | Responsibility |
|-------|----------------|
| WH_STAFF | Submit and correct physical counts together with inbound QC entry |
| STOREKEEPER | Review submitted count/QC in Feature 03; cannot act as the Staff count-entry actor |

## User Story

WH_STAFF submits complete physical counts for every receipt item on "Nhan hang & QC dau vao" and resubmits corrected counts when STOREKEEPER requests recount.

## Acceptance Criteria

1. Given receipt `PENDING_RECEIPT`, when WH_STAFF saves complete valid receive-and-QC data, then receipt becomes `PENDING_STOREKEEPER_REVIEW`.
2. Given receipt `RECOUNT_REQUIRED`, when WH_STAFF saves corrected receive-and-QC data, then previous pending review data is replaced and receipt becomes `PENDING_STOREKEEPER_REVIEW`.
3. Given receipt `PENDING_MANAGER_APPROVAL` or `REVISION_REQUIRED`, when WH_STAFF submits receive-and-QC data, then the request is rejected without saving because WH_MANAGER has not approved receiving or correction is still required.
4. Given invalid, duplicate, missing, or wrong receipt item count, then the request is rejected without partial save.

## Functional Requirements

- **F02-FR-001**: WHEN receive-and-QC data is submitted, the system SHALL require exactly one non-negative integer `actual_qty` per receipt item.
- **F02-FR-002**: WHEN `actual_qty <= expected_qty`, the system SHALL store `actual_qty` as submitted and set `over_received_qty = 0`.
- **F02-FR-003**: WHEN `actual_qty > expected_qty`, the system SHALL store `actual_qty` as submitted and store `over_received_qty = actual_qty - expected_qty`; the excess remains outside all inventory and downstream documents.
- **F02-FR-004**: WHEN WH_STAFF resubmits after `RECOUNT_REQUIRED`, the system SHALL clear old pending QC data and non-finalized Quarantine readiness before saving the new count/QC data.
- **F02-FR-005**: WHERE count is saved, the system SHALL not create regular inventory, finalized Quarantine inventory, batch, putaway, supplier invoice, Debit Note, or RTV.
- **F02-FR-006**: WHERE receipt status is `PENDING_MANAGER_APPROVAL`, `REVISION_REQUIRED`, `PENDING_STOREKEEPER_REVIEW`, `QC_COMPLETED`, `QC_FAILED`, or any manager-finalized state, the system SHALL reject Staff receive-and-QC submission unless the status is explicitly `RECOUNT_REQUIRED`.
- **F02-FR-007**: The receive-and-QC mutation SHALL require `expectedVersion` and reject stale versions with HTTP `409`.
- **F02-FR-008**: The receive-and-QC mutation SHALL write audit with Staff actor, role, warehouse, before/after receipt status, and before/after item quantities.

## Errors

| Error | Resolution |
|-------|------------|
| INVALID_RECEIPT_COUNT | Reject full payload; keep previous counts unchanged |
| RECEIPT_COUNT_INCOMPLETE | Reject full payload; show missing item IDs |
| OVER_RECEIVED_PENDING_DECISION | Keep excess outside inventory; require separate approved correction/new receipt |
| RECEIPT_PENDING_MANAGER_APPROVAL | Block counting until WH_MANAGER approves the receipt for receiving |
| RECEIPT_REVISION_REQUIRED | Block receive-and-QC until PLANNER resubmits and WH_MANAGER approves |
| RECEIPT_PENDING_STOREKEEPER_REVIEW | Block Staff overwrite until STOREKEEPER approves or requests recount |
| INVENTORY_VERSION_CONFLICT | Reload latest receipt before retry |

## Out Of Scope

- Storekeeper review decision.
- Putaway or inventory update.
- Manager approval/rejection and whole receipt rejection.
