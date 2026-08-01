# Feature 04: Manager Decision

## Context

WH_MANAGER decides whether Storekeeper-approved count/QC results can proceed to putaway, partially proceed, or the whole receipt should be rejected.

## Actors

| Actor | Responsibility |
|-------|----------------|
| STOREKEEPER | Provides the approved count/QC result for manager decision |
| WH_MANAGER | Approve full receipt, approve passed quantity, reject whole receipt, and confirm supplier handover after whole-receipt rejection |

## User Story

WH_MANAGER reviews only Storekeeper-approved QC results and makes an official receipt decision with audit, warehouse scope, and optimistic locking.

## Acceptance Criteria

1. `QC_COMPLETED -> APPROVED`, `approved_qty = actual_qty`, regular inventory unchanged.
2. `QC_FAILED -> PARTIALLY_APPROVED`, `approved_qty = quality_passed_qty`, failed quantity becomes finalized Quarantine stock.
3. `QC_COMPLETED/QC_FAILED -> RETURN_TO_SUPPLIER_PENDING` only when WH_MANAGER rejects with reason.
4. Given receipt `PENDING_STOREKEEPER_REVIEW` or `RECOUNT_REQUIRED`, when WH_MANAGER tries to approve/reject, then the request is rejected because Storekeeper review is not approved.

## Functional Requirements

- **F04-FR-001**: Decision SHALL require `WH_MANAGER` role plus warehouse scope.
- **F04-FR-002**: WHEN `WH_MANAGER` approves `QC_COMPLETED`, the system SHALL set `APPROVED`.
- **F04-FR-003**: WHEN `WH_MANAGER` approves passed quantity from `QC_FAILED`, the system SHALL set `PARTIALLY_APPROVED` and finalize failed quantity into Quarantine stock.
- **F04-FR-004**: WHEN `WH_MANAGER` rejects whole receipt, the system SHALL require reason, set `RETURN_TO_SUPPLIER_PENDING`, and finalize any failed quantity into Quarantine stock.
- **F04-FR-005**: `WH_MANAGER` approval SHALL resolve batch lineage but SHALL NOT increase regular inventory.
- **F04-FR-006**: `WH_MANAGER` handover confirmation SHALL move `RETURN_TO_SUPPLIER_PENDING -> RETURNED_TO_SUPPLIER`; STOREKEEPER SHALL NOT see or execute the supplier-return confirmation action.
- **F04-FR-007**: `WH_MANAGER` decision SHALL be allowed only after STOREKEEPER review approval has moved the receipt to `QC_COMPLETED` or `QC_FAILED`.

## Errors

| Error | Resolution |
|-------|------------|
| FORBIDDEN_RECEIPT_WAREHOUSE | Block decision; log denied authorization |
| REJECTION_REASON_REQUIRED | Keep current status; require reason |
| NO_PASSED_QUANTITY_TO_APPROVE | Require whole rejection or quarantine handling |
| APPROVED_QTY_EXCEEDS_PASSED_QTY | Block approval; approve passed quantity only |
| UNIT_COST_REQUIRED | Block approval/AP notification until unit cost is completed |
| STOREKEEPER_REVIEW_PENDING | Block manager decision until Storekeeper approves count/QC |

## Out Of Scope

- Putaway inventory mutation.
- RTV/disposal execution.
