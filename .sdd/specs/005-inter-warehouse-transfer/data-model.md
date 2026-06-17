# Data Model: 005 Inter-Warehouse Transfer

## Transfer

**Table**: `transfers`

Fields to add/verify:
- `id`
- `transfer_number`
- `source_warehouse_id`
- `destination_warehouse_id`
- `status`: `NEW`, `APPROVED`, `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, `CANCELLED`
- `created_by`
- `external_instruction_code`
- `approved_by`, `approved_at`
- `rejected_by`, `rejected_at`, `rejection_reason`
- `confirmed_by`, `confirmed_at`
- `planned_date`
- `actual_received_date`
- `discrepancy_reason`
- `trip_id`
- `document_date`
- `accounting_period_id`
- `notes`
- `created_at`, `updated_at`

Validation:
- Source and destination warehouses must differ.
- External instruction code is required.
- Active transfers are unique by external instruction code, source warehouse, destination warehouse, and document date.
- Planner can update only `NEW`.
- Planner can cancel only `NEW`.
- Source manager/authorized manager can cancel only unshipped `APPROVED`.
- No cancellation after `IN_TRANSIT`.

## TransferItem

**Table**: `transfer_items`

Fields to add/verify:
- `id`
- `transfer_id`
- `product_id`
- `source_location_id`
- `destination_location_id`
- `planned_qty`
- `sent_qty`
- `received_qty`
- `variance_qty`
- `qc_passed_qty`
- `qc_failed_qty`
- `qc_result`
- `qc_failure_reason`
- `receive_issue_reason`
- `receive_checked_by`
- `receive_checked_at`
- `receive_checker_note`

Validation:
- `planned_qty > 0`.
- `sent_qty = planned_qty` when shipping.
- `sent_qty == null` means not loaded; `sent_qty != null` means loaded but not necessarily departed.
- Receive count can be edited by destination worker until `receive_checked_at` is set.
- `receive_issue_reason` is required per item if initial received quantity is less/greater than sent quantity or worker reports issue.
- `confirmedReceivedQty` becomes effective `received_qty` after receive-check approval.
- `qc_passed_qty + qc_failed_qty = confirmedReceivedQty`.
- `receive_checker_note` is optional when checked quantity equals worker-entered quantity and required when different.

## Trip

**Table**: `trips`

Transfer usage:
- `trip_type = TRANSFER`
- exactly one trip per transfer
- selected vehicle and driver must be available and not in overlapping trip
- assigned driver is the only actor allowed to confirm transfer departure

## Inventory

Transfer operations:
- Approval: increase source `reserved_qty` by `planned_qty`.
- Cancel unshipped approved: decrease source `reserved_qty`.
- Depart: decrease source `total_qty`, decrease source `reserved_qty`, increase In-Transit `total_qty`.
- Final receive: decrease In-Transit `total_qty`; increase destination regular inventory for QC-passed quantity; increase destination quarantine inventory for QC-failed quantity.
- No inventory update may create negative total, negative reserved, or negative available.
- Inventory updates must use optimistic locking/version checks.

## Adjustment

Create adjustment with type `TRANSFER_DISCREPANCY` when final received quantity is lower than sent quantity.

Fields required:
- transfer reference or entity linkage
- product
- warehouse
- variance quantity
- reason
- created by/system actor
- audit linkage

## AuditLog

Required transfer actions:
- `TRANSFER_CREATE`
- `TRANSFER_UPDATE`
- `TRANSFER_APPROVE`
- `TRANSFER_REJECT`
- `TRANSFER_TRIP_ASSIGN`
- `TRANSFER_SHIP`
- `TRANSFER_UNSHIP`
- `TRANSFER_DEPART`
- `TRANSFER_RECEIVE_COUNT`
- `TRANSFER_RECEIVE_CHECK`
- `TRANSFER_RECEIVE_CONFIRM`
- `TRANSFER_DISCREPANCY_CREATE`
- `TRANSFER_CANCEL`

Each audit entry must include actor, action, entity type, entity id/code, timestamp, before state, and after state where relevant.
