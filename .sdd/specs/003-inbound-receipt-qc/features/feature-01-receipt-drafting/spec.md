# Feature 01: Receipt Drafting

## Context

PLANNER creates a purchase receipt from a supplier so WH_MANAGER can approve it for receiving before warehouse staff count, QC, and audit inbound goods.

## Actors

| Actor | Responsibility |
|-------|----------------|
| PLANNER | Create receipt header and item expectations |

## User Story

PLANNER creates a `PURCHASE` receipt with supplier, warehouse, receipt document date, item list, expected quantity, and unit cost when available. The system generates the receipt number and sets the receipt to `PENDING_MANAGER_APPROVAL`; PLANNER does not enter source PO/source document, contact person, or source channel fields in Spec 003.

## Acceptance Criteria

1. Given valid supplier, warehouse, and product items, when PLANNER creates a receipt, then the receipt is `PENDING_MANAGER_APPROVAL`.
2. Given a valid create request, when PLANNER creates a receipt, then the system returns a generated receipt number in format `PO-{YYYYMMDD}-{SEQ}`.
3. Given a created receipt, then no regular inventory, quarantine inventory, or batch is created.
4. Given WH_MANAGER rejects a newly created receipt before receiving, then the receipt becomes `REVISION_REQUIRED` and can be revised/resubmitted only by PLANNER.

## Functional Requirements

- **F01-FR-001**: WHEN PLANNER creates a purchase receipt, the system SHALL set status to `PENDING_MANAGER_APPROVAL`.
- **F01-FR-002**: WHERE receipt type is created in Spec 003, the system SHALL allow `PURCHASE` only.
- **F01-FR-003**: WHEN PLANNER creates a purchase receipt, the system SHALL generate `receipt_number` as `PO-{YYYYMMDD}-{SEQ}` using the receipt document date and a daily sequence.
- **F01-FR-004**: WHEN receipt is created, the system SHALL write `RECEIPT_CREATE` audit.
- **F01-FR-005**: The create receipt request SHALL NOT require or expose user-entered `sourceOrderCode` / source PO, `contactPerson`, or `sourceChannel` in Spec 003.
- **F01-FR-006**: WHEN WH_MANAGER rejects a newly created receipt before receiving, the system SHALL set status to `REVISION_REQUIRED` and require a reason.
- **F01-FR-007**: WHEN PLANNER resubmits a `REVISION_REQUIRED` receipt after correction, the system SHALL return it to `PENDING_MANAGER_APPROVAL`.

## Errors

| Error | Resolution |
|-------|------------|
| RECEIPT_NUMBER_CONFLICT | Retry sequence generation; do not expose duplicate receipt number |
| FORBIDDEN_RECEIPT_WAREHOUSE | Block creation; log denied authorization |
| INVALID_RECEIPT_COUNT | Reject invalid expected quantity before receipt is created |
| PRE_RECEIVE_REJECTION_REASON_REQUIRED | Keep current status; require WH_MANAGER rejection reason |

## Out Of Scope

- Dealer return receipt creation.
- External supplier API integration.
