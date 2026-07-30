# Plan: Feature 02 Receipt Counting

## Scope

Implement complete physical count submit/correction with variance preservation and QC reset.

## Implementation Notes

- Validate complete count payload atomically.
- Preserve expected-vs-actual variance.
- Store over-received quantity outside inventory.
- Clear QC fields on correction after QC.
- Use `expectedVersion`.
- Emit `RECEIPT_RECEIVE` or `RECEIPT_CORRECTION`.

## Verification

- Unit tests for complete count, incomplete count, invalid quantity, wrong item, over-received quantity.
- Unit test for correction after QC.
- Integration test for stale version.
