# Plan: Feature 05 Putaway And Inventory

## Scope

Implement putaway into regular bin and the regular inventory increase.

## Implementation Notes

- Validate receipt status and idempotency.
- Validate allocation quantity equals `approved_qty`.
- Validate location is regular, active, same warehouse.
- Validate capacity.
- Increase regular inventory with optimistic locking.
- Set `PUTAWAY_COMPLETED` and `putaway_completed_at`.
- Emit `RECEIPT_PUTAWAY_COMPLETE` and `INVENTORY_UPDATE`.

## Verification

- Tests for full and partial putaway.
- Tests for invalid location, capacity exceeded, quantity mismatch, duplicate putaway.
- Verify outbound available stock includes only putaway quantity.
