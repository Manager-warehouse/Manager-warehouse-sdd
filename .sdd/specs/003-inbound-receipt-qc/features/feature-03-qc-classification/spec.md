# Feature 03: QC Classification And Storekeeper Review

## Context

WH_STAFF enters inbound QC classification on the same "Nhan hang & QC dau vao" screen used for physical receipt counting. STOREKEEPER reviews the submitted count/QC result before it can reach WH_MANAGER. QC classifies passed and failed quantities only; it never rejects the whole receipt and never creates regular inventory, batch, putaway, supplier invoice, Debit Note, RTV, or finalized Quarantine stock.

## Actors

| Actor | Responsibility |
|-------|----------------|
| WH_STAFF | Enter QC observations and save QC classification with receiving quantities |
| STOREKEEPER | Approve the submitted count/QC result or reject it with recount reason |
| WH_MANAGER | View Storekeeper-approved QC outcome and decide approval/rejection in the downstream manager decision step |

## User Story

WH_STAFF records whether all goods passed QC or whether any quantity failed. STOREKEEPER checks the submitted count/QC and either sends it back for recount or approves it for WH_MANAGER decision.

## Acceptance Criteria

1. Given receipt `PENDING_RECEIPT`, when WH_STAFF saves valid receive-and-QC data, then receipt becomes `PENDING_STOREKEEPER_REVIEW`.
2. Given receipt `PENDING_STOREKEEPER_REVIEW`, when STOREKEEPER rejects with a reason, then receipt becomes `RECOUNT_REQUIRED` and no manager decision is available.
3. Given receipt `PENDING_STOREKEEPER_REVIEW` and all actual quantity passed, when STOREKEEPER approves review, then receipt becomes `QC_COMPLETED`.
4. Given receipt `PENDING_STOREKEEPER_REVIEW` and any failed quantity, when STOREKEEPER approves review, then receipt becomes `QC_FAILED` and only failed quantity is staged as Quarantine readiness.
5. Given mismatched QC quantity or failed quantity without reason, when receive-and-QC is saved, then receipt status and item quantities remain unchanged.

## Functional Requirements

- **F03-FR-001**: The unified Staff screen SHALL display columns: Ma hang, Ten hang, SL du kien, SL thuc nhan, QC dat, QC loi, Ly do loi, Ket qua.
- **F03-FR-002**: WHEN `actual_qty` is entered, the UI SHALL default `quality_passed_qty = actual_qty` and `quality_failed_qty = 0`.
- **F03-FR-003**: WHEN `quality_failed_qty` is edited, the UI SHALL recalculate `quality_passed_qty = actual_qty - quality_failed_qty`.
- **F03-FR-004**: The UI SHALL show a clear warning and prevent save when `quality_passed_qty + quality_failed_qty != actual_qty`.
- **F03-FR-005**: WHEN receive-and-QC is saved, `quality_passed_qty + quality_failed_qty` SHALL equal `actual_qty`.
- **F03-FR-006**: WHEN `quality_failed_qty > 0`, the system SHALL require `qc_failure_reason`.
- **F03-FR-007**: IF STOREKEEPER approves and all failed quantities are zero, the system SHALL move receipt to `QC_COMPLETED`.
- **F03-FR-008**: IF STOREKEEPER approves and any failed quantity exists, the system SHALL move receipt to `QC_FAILED` and stage failed quantity as Quarantine readiness.
- **F03-FR-009**: QC save and Storekeeper review SHALL NOT create regular inventory, batch, putaway, supplier invoice, Debit Note, RTV, supplier-return status, finalized Quarantine stock, or warehouse-location occupancy.
- **F03-FR-010**: The receipt list SHALL display `PENDING_STOREKEEPER_REVIEW` as "Cho thu kho duyet", `RECOUNT_REQUIRED` as "Can dem lai", `QC_COMPLETED` as "Da QC", and `QC_FAILED` as "QC co hang loi".
- **F03-FR-011**: STOREKEEPER recount request SHALL require a reason, store it as `recount_reason` for WH_STAFF visibility, and write `RECEIPT_STOREKEEPER_RECOUNT_REQUEST`.
- **F03-FR-012**: STOREKEEPER approval SHALL write `RECEIPT_STOREKEEPER_REVIEW_APPROVE` with actor, role, warehouse, before/after status, and reviewed item quantities.

## Errors

| Error | Resolution |
|-------|------------|
| QC_QUANTITY_MISMATCH | Keep receipt unchanged; require passed/failed quantity correction |
| QC_FAILED_REASON_REQUIRED | Keep receipt unchanged; require failure reason |
| QC_RESULT_INCONSISTENT | Keep receipt unchanged; recalculate QC result |
| RECEIPT_STATUS_NOT_RECEIVABLE | Keep receipt unchanged when status is not Staff receivable |
| STOREKEEPER_REVIEW_REASON_REQUIRED | Keep receipt in `PENDING_STOREKEEPER_REVIEW`; require recount reason |

## Out Of Scope

- WH_MANAGER approval/rejection.
- Quality-grade resale classification.
- Whole receipt rejection.
- Finalized Quarantine stock movement.
