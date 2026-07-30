# Feature 02: Receipt Counting

## Context

WH_STAFF or STOREKEEPER records actual received quantity before QC only after WH_MANAGER approves the created receipt for receiving. Counting does not create inventory.

## Actors

| Actor | Responsibility |
|-------|----------------|
| WH_STAFF | Submit and correct physical counts |
| STOREKEEPER | Submit and correct physical counts |

## User Story

WH_STAFF or STOREKEEPER submits complete physical counts for every receipt item and can correct counts before WH_MANAGER final decision.

## Acceptance Criteria

1. Given receipt `PENDING_RECEIPT`, when WH_STAFF or STOREKEEPER submits complete valid counts, then receipt becomes `DRAFT`.
2. Given `DRAFT`, `QC_COMPLETED`, or non-finalized `QC_FAILED`, when WH_STAFF or STOREKEEPER corrects counts, then previous QC fields and Quarantine readiness are cleared and receipt returns to `DRAFT`.
3. Given receipt `PENDING_MANAGER_APPROVAL` or `REVISION_REQUIRED`, when WH_STAFF or STOREKEEPER submits counts, then the request is rejected without saving because WH_MANAGER has not approved receiving.
4. Given invalid, duplicate, missing, or wrong receipt item count, then the request is rejected without partial save.

## Functional Requirements

- **F02-FR-001**: WHEN count is submitted, the system SHALL require exactly one positive integer count per receipt item.
- **F02-FR-002**: WHEN `counted_qty <= expected_qty`, the system SHALL set `actual_qty = counted_qty` and `over_received_qty = 0`.
- **F02-FR-003**: WHEN `counted_qty > expected_qty`, the system SHALL set `actual_qty = expected_qty` and store excess in `over_received_qty`.
- **F02-FR-004**: WHEN count correction occurs after QC but before WH_MANAGER finalization, the system SHALL clear QC data, clear non-finalized Quarantine readiness, and return receipt to `DRAFT`.
- **F02-FR-005**: WHERE count is saved, the system SHALL not create inventory, quarantine, or batch.
- **F02-FR-006**: WHERE receipt status is `PENDING_MANAGER_APPROVAL` or `REVISION_REQUIRED`, the system SHALL reject count submission with a business-rule error and leave all count fields unchanged.

## Errors

| Error | Resolution |
|-------|------------|
| INVALID_RECEIPT_COUNT | Reject full payload; keep previous counts unchanged |
| RECEIPT_COUNT_INCOMPLETE | Reject full payload; show missing item IDs |
| OVER_RECEIVED_PENDING_DECISION | Keep excess outside inventory; require separate approved correction/new receipt |
| RECEIPT_PENDING_MANAGER_APPROVAL | Block counting until WH_MANAGER approves the receipt for receiving |
| INVENTORY_VERSION_CONFLICT | Reload latest receipt before retry |

## Out Of Scope

- QC result entry.
- Putaway or inventory update.
