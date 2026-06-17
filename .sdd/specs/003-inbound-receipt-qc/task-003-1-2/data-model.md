# Data Model: Inbound Receipt Approval & Quarantine Handling

## Receipt

**Table**: `receipts`

**Purpose**: Header for inbound purchase/return receipt and lifecycle status.

**Relevant fields**:
- `id`
- `receipt_number`
- `type`
- `warehouse_id`
- `supplier_id`
- `status`
- `approved_by`
- `approved_at`
- `rejection_reason`
- `document_date`
- `accounting_period_id`
- `version`

**Status transitions**:
- `PENDING_RECEIPT -> DRAFT`
- `DRAFT -> QC_COMPLETED`
- `DRAFT -> QC_FAILED`
- `QC_COMPLETED -> APPROVED`
- `QC_COMPLETED -> RETURN_TO_SUPPLIER_PENDING`
- `RETURN_TO_SUPPLIER_PENDING -> RETURNED_TO_SUPPLIER`
- `QC_FAILED -> QC_FAILED` after RTV confirmation; RTV completion is represented by `adjustments.approved_at`.

**Validation rules**:
- Approve/reject only when status is `QC_COMPLETED`.
- Return-to-supplier confirmation only when status is `RETURN_TO_SUPPLIER_PENDING`.
- Duplicate approve/reject after `APPROVED`, `RETURN_TO_SUPPLIER_PENDING`, or `RETURNED_TO_SUPPLIER` returns HTTP 409.
- Stale `version` returns HTTP 409.
- Manager and Storekeeper actions require warehouse assignment.

## ReceiptItem

**Table**: `receipt_items`

**Purpose**: Product lines, actual quantities, QC sample data, batch/location assignment.

**Relevant fields**:
- `id`
- `receipt_id`
- `product_id`
- `batch_id`
- `location_id`
- `expected_qty`
- `actual_qty`
- `sample_qty`
- `sample_passed_qty`
- `sample_failed_qty`
- `qc_result`
- `qc_failure_reason`
- `unit_cost`

**Validation rules**:
- `sample_passed_qty + sample_failed_qty = sample_qty`.
- Sprint 1 QC result is only `PENDING`, `PASSED`, `FAILED`; no partial result.
- `batch_id` is assigned on approval, not on QC submit.
- `location_id` is assigned on putaway, not on approval.
- No serial, expiry, or grade validation.

## Batch

**Table**: `batches`

**Purpose**: Trace receipt lot by product/source/date for FIFO.

**Relevant fields**:
- `id`
- `batch_number`
- `product_id`
- `warehouse_id`
- `received_date`
- `quantity`
- `created_at`

**Validation rules**:
- Resolve/create during approval using product plus source receipt/date.
- Do not split by grade.
- Do not require expiry.

## Inventory

**Table**: `inventories`

**Purpose**: Quantity by warehouse, product, batch, location.

**Relevant fields**:
- `warehouse_id`
- `product_id`
- `batch_id`
- `location_id`
- `total_qty`
- `reserved_qty`
- `version`

**Validation rules**:
- `total_qty >= 0`, `reserved_qty >= 0`, `total_qty - reserved_qty >= 0`.
- Putaway increases regular inventory only at non-quarantine Bin.
- RTV confirmation decreases quarantine inventory by full quarantined quantity.
- All updates use optimistic locking.

## WarehouseLocation

**Table**: `warehouse_locations`

**Purpose**: Bin/zone location and quarantine flag.

**Relevant fields**:
- `id`
- `warehouse_id`
- `type`
- `capacity_m3`
- `capacity_kg`
- `current_volume_m3`
- `current_weight_kg`
- `is_quarantine`

**Validation rules**:
- Putaway target must be regular Bin (`is_quarantine = false`).
- QC failed/quarantine inventory must be in quarantine location.
- Capacity checks happen before putaway.

## Adjustment

**Table**: `adjustments`

**Purpose**: RTV document for quarantine stock movement.

**Relevant fields**:
- `id`
- `adjustment_number`
- `warehouse_id`
- `product_id`
- `batch_id`
- `location_id`
- `quantity_adjustment`
- `type = RETURN_TO_VENDOR`
- `reference_id = receipt.id`
- `reference_type = RECEIPT`
- `reason`
- `approved_by`
- `approved_at`
- `document_date`
- `accounting_period_id`
- `created_by`
- `created_at`

**Validation rules**:
- One pending or confirmed RTV per receipt.
- `approved_at == null` means pending physical return.
- Confirming sets `approved_by` and `approved_at`.
- Confirming deducts full quarantine quantity; partial confirmation is rejected.

## DebitNote

**Table**: `debit_notes`

**Purpose**: Supplier claim generated from RTV request.

**Relevant fields**:
- `id`
- `debit_note_number`
- `supplier_id`
- `receipt_id`
- `failed_qty`
- `amount`
- `reason`
- `created_by`
- `document_date`
- `accounting_period_id`
- `created_at`

**Validation rules**:
- Created only for `QC_FAILED` RTV creation.
- Not created for manager reject of `QC_COMPLETED`.
- Duplicate Debit Note for same receipt/RTV is rejected with duplicate RTV rule.

## AuditLog

**Table**: `audit_logs`

**Purpose**: Append-only trail for warehouse/accounting mutations.

**Required actions**:
- `RECEIPT_APPROVE`
- `RECEIPT_REJECT`
- `RECEIPT_RETURN_CONFIRM`
- `RECEIPT_PUTAWAY_COMPLETE`
- `QUARANTINE_RTV_CREATE`
- `QUARANTINE_RTV_CONFIRM`
- `INVENTORY_UPDATE`

**Payload rules**:
- Include actor, actor role, action, entity type, entity id/code, warehouse, timestamp.
- Include before/after status or quantity values when changed.
