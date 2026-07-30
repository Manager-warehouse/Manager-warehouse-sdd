# Plan: Feature 04 Manager Decision

## Scope

Implement WH_MANAGER approve/reject decisions and supplier handover confirmation for rejected receipts.

## Implementation Notes

- Add/verify `PARTIALLY_APPROVED` status.
- Implement full approval and partial approval branches.
- Set `approved_qty` from actual or passed quantity.
- Finalize failed quantity from Quarantine readiness into Quarantine stock when approving partial receipt or rejecting whole receipt.
- Resolve 5-part batch lineage.
- Require unit cost before approval/AP notification.
- Require reason for rejection.
- Preserve quarantine traceability when rejecting `QC_FAILED`.
- Emit `RECEIPT_APPROVE`, `RECEIPT_PARTIAL_APPROVE`, `RECEIPT_REJECT`, and `RECEIPT_RETURN_CONFIRM`.

## Verification

- Tests for full approval, partial approval, no passed quantity, missing unit cost.
- Tests for rejection with/without reason.
- Tests for warehouse scope and stale version.
