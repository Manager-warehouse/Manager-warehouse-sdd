# Plan: Feature 06 Quarantine RTV

## Scope

Implement return-to-vendor handling for failed quarantine quantity.

## Implementation Notes

- Track unresolved failed quarantine quantity.
- Create pending `RETURN_TO_VENDOR` adjustment.
- Create Debit Note for supplier.
- Do not reduce Quarantine on RTV creation.
- Reduce Quarantine only on physical confirmation.
- Block duplicate RTV and duplicate confirmation.
- Emit `QUARANTINE_RTV_CREATE`, `QUARANTINE_RTV_CONFIRM`, and `INVENTORY_UPDATE`.

## Verification

- Tests for RTV create, duplicate RTV, no quarantine quantity.
- Tests for exact confirmation, quantity mismatch, duplicate confirmation.
- Verify Debit Note is generated once.
