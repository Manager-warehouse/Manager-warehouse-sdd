# Plan: Feature 03 QC Classification

## Scope

Implement QC submit and confirm rules for all-passed and failed-quantity cases.

## Implementation Notes

- Validate sample and quality quantity sums.
- Require failure reason for failed quantity.
- Move all-passed receipt to `QC_COMPLETED`.
- Move any-failed receipt to `QC_FAILED`.
- Create/update Quarantine readiness only for failed quantity.
- Keep passed quantity outside regular available stock.
- Emit `RECEIPT_QC_SUBMIT` and `RECEIPT_QC_CONFIRM`.

## Verification

- Tests for all passed.
- Tests for failed quantity to Quarantine only.
- Tests for all QC mismatch errors.
- Verify no regular inventory impact.
