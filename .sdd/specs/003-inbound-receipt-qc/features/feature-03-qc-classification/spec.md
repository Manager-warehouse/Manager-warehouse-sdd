# Feature 03: QC Classification

## Context

WH_STAFF, STOREKEEPER, or WH_MANAGER confirms inbound QC. QC classifies passed and failed quantities only; it never rejects the whole receipt.

## Actors

| Actor | Responsibility |
|-------|----------------|
| WH_STAFF | Enter QC observations and confirm QC classification |
| STOREKEEPER | Enter QC observations and confirm QC classification |
| WH_MANAGER | Enter QC observations and confirm QC classification |

## User Story

WH_STAFF, STOREKEEPER, or WH_MANAGER confirms whether all goods passed QC or whether any quantity failed and must enter Quarantine readiness.

## Acceptance Criteria

1. Given receipt `DRAFT` and all actual quantity passed, when QC is confirmed, then receipt becomes `QC_COMPLETED`.
2. Given receipt `DRAFT` and any failed quantity, when QC is confirmed, then receipt becomes `QC_FAILED` and only failed quantity is staged as Quarantine readiness.
3. Given mismatched QC quantity, when QC is confirmed, then receipt remains `DRAFT`.

## Functional Requirements

- **F03-FR-001**: WHEN QC is submitted, `sample_passed_qty + sample_failed_qty` SHALL equal `sample_qty`.
- **F03-FR-002**: WHEN QC is confirmed, `quality_passed_qty + quality_failed_qty` SHALL equal `actual_qty`.
- **F03-FR-003**: IF all failed quantities are zero, the system SHALL move receipt to `QC_COMPLETED`.
- **F03-FR-004**: IF any failed quantity exists, the system SHALL move receipt to `QC_FAILED` and stage failed quantity as Quarantine readiness.
- **F03-FR-005**: QC confirmation SHALL NOT create regular inventory or supplier-return status.

## Errors

| Error | Resolution |
|-------|------------|
| QC_SAMPLE_MISMATCH | Keep receipt in `DRAFT`; require corrected sample values |
| QC_QUANTITY_MISMATCH | Keep receipt in `DRAFT`; require passed/failed quantity correction |
| QC_FAILED_REASON_REQUIRED | Keep receipt in `DRAFT`; require failure reason |
| QC_RESULT_INCONSISTENT | Keep receipt in `DRAFT`; recalculate QC result |

## Out Of Scope

- WH_MANAGER approval/rejection.
- Quality-grade resale classification.
