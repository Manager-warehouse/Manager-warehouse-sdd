# Research: Inbound Receipt Approval & Quarantine Handling

## Decision 1: Approval Unlocks Putaway Only

**Decision**: Manager approval moves `QC_COMPLETED -> APPROVED`, creates/resolves batches, and unlocks Storekeeper putaway. It does not increase `inventories.total_qty`.

**Rationale**: The business rule says physical inventory should only increase when goods are placed into a regular Bin after approval. This avoids available stock before goods are actually stored and capacity-checked.

**Alternatives considered**:
- Increase inventory at approval: rejected because it bypasses putaway capacity and location traceability.
- Increase inventory at QC completion: rejected because manager approval is a required checker gate.

## Decision 2: Rejected QC-Completed Receipts Have a Terminal Handover Step

**Decision**: Manager reject moves `QC_COMPLETED -> RETURN_TO_SUPPLIER_PENDING`. Storekeeper confirms physical supplier handover when the supplier vehicle arrives, moving `RETURN_TO_SUPPLIER_PENDING -> RETURNED_TO_SUPPLIER`.

**Rationale**: The user requires a visible handover milestone after rejection. Because rejected goods were never added to inventory, this confirmation is an audit/status transition only.

**Alternatives considered**:
- Leave rejected receipts permanently in `RETURN_TO_SUPPLIER_PENDING`: rejected because operations need to know the supplier has collected the goods.
- Use RTV for rejected QC-completed goods: rejected because RTV/Debit Note is reserved for `QC_FAILED` quarantine stock.

## Decision 3: RTV Must Return the Full Quarantine Quantity

**Decision**: Storekeeper RTV confirmation must match the full quarantined quantity for the receipt. Partial or over-confirmed quantities return HTTP 422 `RTV_QUANTITY_MISMATCH` and do not deduct quarantine inventory.

**Rationale**: User explicitly requires all failed goods to be returned, with no partial return in Sprint 1. This keeps quarantine handling simple and prevents stranded residual stock outside the disposal flow.

**Alternatives considered**:
- Allow partial RTV and leave residual quarantine quantity for disposal: rejected by user decision.
- Auto-adjust residual quantity: rejected because it would hide physical/accounting discrepancies.

## Decision 4: Debit Note Is System-Generated on RTV Create

**Decision**: `POST /api/v1/receipts/{id}/rtv` creates a pending `RETURN_TO_VENDOR` adjustment and a linked `debit_notes` record, but does not deduct inventory.

**Rationale**: The business wants accounting visibility as soon as RTV is documented, while physical stock remains in quarantine until handover is confirmed.

**Alternatives considered**:
- Accountant manually creates Debit Note: rejected because the current feature says the system creates it automatically.
- Create Debit Note only after physical return: rejected because documentation should exist before handover.

## Decision 5: Audit Log Stores Return Confirmation Metadata

**Decision**: Store rejected-goods handover actor/timestamp/note in `RECEIPT_RETURN_CONFIRM` audit log. Do not add receipt columns unless UI requires direct filtering by handover metadata.

**Rationale**: Audit log is already mandatory and append-only, and receipt status `RETURNED_TO_SUPPLIER` is enough for list filtering.

**Alternatives considered**:
- Add `returned_by`, `returned_at`, `return_note` columns: useful for reporting, but not required for Sprint 1 unless UI filters need them.

## Decision 6: No Serial, Expiry, Or Quality Tier In This Feature

**Decision**: This implementation must not require serial numbers, expiry dates, FEFO, or quality-grade classification. Batch resolution uses product plus source receipt/date.

**Rationale**: Current domain is household goods with large orders; per-unit serial and expiry tracking adds unnecessary operational burden.

**Alternatives considered**:
- Keep existing grade/expiry fields as active business rules: rejected by user decision and updated constitution/specs.
