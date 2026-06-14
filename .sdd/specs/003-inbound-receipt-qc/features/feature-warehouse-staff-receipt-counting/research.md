# Research: Warehouse Staff Receipt Counting

## Decision: Use integer quantity semantics end-to-end

**Decision**: `counted_qty`, `expected_qty`, `actual_qty`, `over_received_qty`, and QC sample quantity fields use positive integer semantics in request DTOs, entities, schema, OpenAPI, and tests.

**Rationale**: The feature owner confirmed all quantities are integers. This removes ambiguity between `Integer` and `BigDecimal` and prevents fractional physical counts from entering receipt/QC flows.

**Alternatives considered**: Keep current `BigDecimal` entity fields for actual and sample quantities while validating whole numbers at the API boundary. Rejected because it leaves inconsistent storage and can allow fractional writes from future internal paths.

## Decision: All receipt items are required in one receive-count request

**Decision**: `PUT /api/v1/receipts/{id}/receive` requires exactly one request item for every receipt item in the target receipt.

**Rationale**: The feature requires Warehouse Staff to count all product lines and enter the counted quantity into the receive check document. Atomic submission avoids half-counted receipts and simplifies downstream QC readiness.

**Alternatives considered**: Keep `complete_receiving` or allow partial saves. Rejected because the clarified spec removed partial receiving for this feature.

## Decision: Count correction is gated by receipt status only

**Decision**: The service allows count correction while receipt status is `DRAFT`, `QC_COMPLETED`, or `QC_FAILED`, and rejects correction when status is `APPROVED` or `REJECTED`.

**Rationale**: The feature owner clarified that Warehouse Manager approval is represented by receipt status. Using only `receipts.status` avoids coupling this slice to quarantine/RTV implementation details.

**Alternatives considered**: Check quarantine intake or RTV records. Rejected because it adds cross-feature coupling and contradicts the clarified rule.

## Decision: Correcting counts invalidates prior QC data

**Decision**: When a receipt with QC data is corrected, clear QC sample/result fields and return receipt status to `DRAFT`.

**Rationale**: QC results were based on the previous counted quantities. Clearing stale QC data protects the QC gate and forces the QC inbound feature to run again.

**Alternatives considered**: Preserve QC data and mark only a warning. Rejected because it allows old QC results to remain attached to changed physical quantities.

## Decision: No inventory, batch, quarantine, or location writes

**Decision**: This feature updates only receipt header/item counting fields and audit logs.

**Rationale**: Parent inbound spec states inventory remains unchanged until later approval or quarantine handling. Batch creation, inventory increase, quarantine intake, and putaway belong to later features.

**Alternatives considered**: Create holding/quarantine inventory for over-received quantity during counting. Rejected because it violates inbound inventory timing rules.

## Decision: Audit action for receive counting

**Decision**: Create an audit log equivalent to `RECEIPT_RECEIVE`. If the current enum remains generic, use `AuditAction.UPDATE` with entity type `RECEIPT` and before/after state containing receipt status plus item count fields.

**Rationale**: Inbound mutations require audit trail. Current code uses generic audit actions, so generic action plus detailed state is the least disruptive path unless domain-specific audit actions are introduced separately.

**Alternatives considered**: Add `RECEIPT_RECEIVE` to `AuditAction`. Acceptable only if audit action enums are being standardized around domain-specific constants in the same implementation slice.

## Documentation And Code Conflicts Reviewed

1. Speckit `setup-plan.ps1` expects `spec.md`, but this repo feature uses `feature-warehouse-staff-receipt-counting.md`; this plan treats the feature markdown as the input spec.
2. Existing `ReceiptItem` Java fields for `actualQty`, `overReceivedQty`, and sample quantities are `BigDecimal`; implementation should migrate them to `Integer` or enforce integer schema alignment before this feature is done.
3. Existing `ReceiptService` is a concrete `@Service` class rather than an interface plus impl; the plan follows the current local pattern.
4. Serial capture is intentionally out of scope for this counting feature per feature owner clarification.
