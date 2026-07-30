# Feature 05: Putaway And Inventory

## Context

Putaway is the only point where accepted inbound goods increase regular available inventory.

## Actors

| Actor | Responsibility |
|-------|----------------|
| STOREKEEPER | Putaway approved goods into regular bin |

## User Story

STOREKEEPER puts approved goods into regular bin after WH_MANAGER decision.

## Acceptance Criteria

1. Given `APPROVED`, putaway increases inventory by `actual_qty`.
2. Given `PARTIALLY_APPROVED`, putaway increases inventory by `approved_qty` only.
3. Duplicate putaway returns conflict and does not double-count inventory.
4. Given the same product and supplier are received in two different receipt events, putaway preserves two distinct batch codes in inventory.

## Functional Requirements

- **F05-FR-001**: Putaway SHALL be allowed only from `APPROVED` or `PARTIALLY_APPROVED`.
- **F05-FR-002**: Putaway allocation quantity SHALL equal `approved_qty`.
- **F05-FR-003**: Putaway target SHALL be active regular bin in the receipt warehouse.
- **F05-FR-004**: Putaway SHALL validate bin capacity before inventory mutation.
- **F05-FR-005**: Successful putaway SHALL increase regular inventory and set `PUTAWAY_COMPLETED`.
- **F05-FR-006**: Duplicate putaway SHALL NOT increase inventory again.
- **F05-FR-007**: Successful putaway SHALL persist inventory by product, warehouse, batch code, and bin/location so later outbound, transfer, stocktake, and audit flows can trace the original receiving batch.

## Errors

| Error | Resolution |
|-------|------------|
| PUTAWAY_QTY_MISMATCH | Block putaway; require allocation equal approved quantity |
| PUTAWAY_LOCATION_INVALID | Block putaway; require valid regular bin |
| BIN_CAPACITY_EXCEEDED | Block putaway; choose another bin or supported split |
| PUTAWAY_ALREADY_COMPLETED | Return existing result; do not double-count inventory |

## Out Of Scope

- Automated putaway optimization.
- Outbound picking.
