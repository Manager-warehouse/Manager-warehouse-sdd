# Feature 06: Quarantine RTV

## Context

Failed goods in Quarantine can be returned to supplier through RTV. Disposal and other handling are owned by Spec 009.

## Actors

| Actor | Responsibility |
|-------|----------------|
| WH_MANAGER | Create RTV for failed quarantine quantity |
| STOREKEEPER | Confirm physical handover to supplier |
| ACCOUNTANT | Apply a `PENDING` Debit Note (from RTV or whole-receipt rejection, see [Feature 04](../feature-04-manager-decision/spec.md)) to reduce supplier payable |
| ACCT_MANAGER | Review returns workspace financial context |

## User Story

WH_MANAGER creates RTV for finalized failed quarantine quantity, STOREKEEPER confirms physical return before stock is deducted, and ACCOUNTANT applies the resulting Debit Note to the supplier's payable balance once the physical handover is confirmed.

## Acceptance Criteria

1. Creating RTV creates pending `RETURN_TO_VENDOR` adjustment and a `PENDING` Debit Note without reducing Quarantine stock.
2. Confirming RTV with exact unresolved failed quantity reduces Quarantine stock once.
3. Duplicate RTV or duplicate confirmation is rejected.
4. A `PENDING` Debit Note has no effect on `suppliers.current_balance`; applying it reduces `suppliers.current_balance` by `debit_notes.amount` exactly once and sets `status = APPLIED`.
5. Applying a Debit Note is rejected if its RTV has not yet been confirmed (goods have not physically left), or if it is already `APPLIED`.

## Functional Requirements

- **F06-FR-001**: RTV SHALL be allowed only when finalized unresolved failed quarantine quantity exists.
- **F06-FR-002**: RTV creation SHALL create pending `RETURN_TO_VENDOR` adjustment and a Debit Note with `status = PENDING`.
- **F06-FR-003**: RTV creation SHALL NOT reduce Quarantine inventory.
- **F06-FR-004**: RTV confirmation SHALL require returned quantity equals unresolved failed quarantine quantity.
- **F06-FR-005**: RTV confirmation SHALL reduce Quarantine inventory exactly once.
- **F06-FR-006**: Quarantine stock SHALL remain excluded from outbound available stock.
- **F06-FR-007**: A Debit Note SHALL NOT affect `suppliers.current_balance` while `status = PENDING`; only applying it changes the balance.
- **F06-FR-008**: Applying a Debit Note SHALL require its linked RTV to be confirmed (Quarantine inventory already reduced, or — for whole-receipt rejection — the receipt already `RETURNED_TO_SUPPLIER` per [Feature 04](../feature-04-manager-decision/spec.md)); applying against an unconfirmed physical return SHALL be rejected.
- **F06-FR-009**: WHEN `ACCOUNTANT` applies a `PENDING` Debit Note, the system SHALL, in one transaction: reduce `suppliers.current_balance` by `debit_notes.amount`, set `status = APPLIED`, record `applied_by` and `applied_at`, and write audit log `DEBIT_NOTE_APPLY` with the supplier balance before/after.
- **F06-FR-010**: Applying a Debit Note that is already `APPLIED` SHALL be rejected (idempotency guard); the system SHALL NOT reduce `suppliers.current_balance` twice for the same Debit Note.

## API Endpoints

### Apply Debit Note
- **Protocol & Path**: `POST /api/v1/debit-notes/{id}/apply`
- **Actor**: `ACCOUNTANT`, `ACCOUNTANT_MANAGER`
- **Response 200 OK**: Debit Note with `status = APPLIED`, `appliedBy`, `appliedAt`, plus supplier `balanceBefore`/`balanceAfter`.

### List Debit Notes
- **Protocol & Path**: `GET /api/v1/debit-notes`
- **Query Params**: `status` (`PENDING`/`APPLIED`, optional), `supplierId` (optional)
- **Actor**: `ACCOUNTANT`, `ACCOUNTANT_MANAGER` — this is the worklist an accountant uses to find Debit Notes still awaiting application, the same role `billing_notifications` plays for invoices.

## Errors

| Error | Resolution |
|-------|------------|
| NO_QUARANTINE_ITEMS | Block RTV; show quarantine balance |
| RTV_ALREADY_EXISTS | Return existing RTV document; do not create duplicate Debit Note |
| RTV_QUANTITY_MISMATCH | Block confirmation; require exact unresolved failed quantity |
| RTV_ALREADY_CONFIRMED | Return existing confirmation; do not reduce stock twice |
| QUARANTINE_STOCK_ALREADY_MOVED | Block action; show latest quarantine ledger |
| DEBIT_NOTE_NOT_FOUND | 404; Debit Note id does not exist |
| RTV_NOT_CONFIRMED | 422; block apply until linked physical return is confirmed |
| DEBIT_NOTE_ALREADY_APPLIED | 409; block duplicate apply, return existing applied record |

## Out Of Scope

- Disposal approval flow.
- Quality-grade resale handling.
