# Data Model: 005 Inter-Warehouse Transfer

## Transfer

**Table**: `transfers`

Fields to add/verify:
- `id`
- `transfer_number`
- `source_warehouse_id`
- `destination_warehouse_id`
- `status`: `NEW`, `APPROVED`, `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, `CANCELLED`
- `is_returned`
- `return_reason_code`, `return_reason`
- `outbound_qc_checked_by`, `outbound_qc_checked_at`, `outbound_qc_result`
- `load_handover_by`, `load_handover_at`
- `outbound_qc_photo_ref`
- `load_handover_photo_ref`
- `driver_departed_at`, `driver_arrived_at`, `arrival_handover_at`
- `return_departed_at`, `return_arrived_at`
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
- `transfer_request_id` (nullable link to approved manager request)
- `created_at`, `updated_at`
- `version`

Validation:
- Source and destination warehouses must differ.
- External instruction code is required.
- Active transfers are unique by external instruction code, source warehouse, destination warehouse, and document date.
- Planner can update only `NEW`.
- Planner can cancel only `NEW`.
- Source manager/authorized manager can cancel only unshipped `APPROVED`.
- No cancellation after `IN_TRANSIT`.
- If created from a transfer request, the linked request must be `APPROVED` and not already converted.
- GET/list/detail reads must not mutate status or persist overdue transitions.
- Receive-count is blocked until driver arrival and receiving-warehouse handover are recorded.
- Return receiving is blocked until return departure and source arrival/handover are recorded.

## TransferRequest

**Table**: `transfer_requests`

Fields to add/verify:
- `id`
- `request_number`
- `requesting_warehouse_id` (warehouse that needs stock; becomes transfer destination)
- `source_warehouse_id` (warehouse expected to send stock)
- `status`: `DRAFT`, `SUBMITTED`, `APPROVED`, `REJECTED`, `CONVERTED`, `CANCELLED`
- `requested_by`
- `submitted_at`
- `approved_by`, `approved_at`
- `rejected_by`, `rejected_at`, `rejection_reason`
- `needed_by_date`
- `business_reason`
- `planner_assignee_id`
- `converted_transfer_id`
- `created_at`, `updated_at`
- `version`

Validation:
- Requesting and source warehouses must differ.
- Requesting warehouse must be within the requesting warehouse manager's assigned warehouse scope.
- Cross-warehouse stock lookup is read-only and must exclude quarantine stock from available quantity.
- Business reason is required before submit.
- Requesting manager can edit only `DRAFT` requests within their assigned destination warehouse scope.
- Requesting manager can soft-cancel only `DRAFT` requests; cancellation sets `status = CANCELLED` and must not physically delete the request or its items.
- CEO can approve or reject only `SUBMITTED` requests.
- CEO rejection requires `rejection_reason`.
- CEO approval does not reserve inventory.
- Only `APPROVED` requests can be converted to `TRF`.
- A request can be converted to at most one active transfer.
- Concurrent updates and duplicate conversion races must fail with a version/unique-constraint conflict.

## TransferRequestItem

**Table**: `transfer_request_items`

Fields to add/verify:
- `id`
- `transfer_request_id`
- `product_id`
- `requested_qty`
- `observed_source_available_qty`
- `observed_requesting_available_qty`
- `shortage_reason`

Validation:
- `requested_qty > 0`.
- `requested_qty` must not exceed current source available quantity at submit/approval time.
- Source available quantity is `total_qty - reserved_qty`, excluding quarantine stock.
- Item shortage reason is required when business reason does not explain the shortage at product level.

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
- `batch_id` nullable on planned item; FIFO allocation rows store actual batch after approval
- `outbound_qc_result`
- `outbound_qc_note`

Validation:
- `planned_qty > 0`.
- `sent_qty = planned_qty` when shipping.
- `sent_qty == null` means not loaded; `sent_qty != null` means loaded but not necessarily departed.
- Receive count can be edited by destination worker until `receive_checked_at` is set.
- `receive_issue_reason` is required per item if initial received quantity is less/greater than sent quantity or worker reports issue.
- `confirmedReceivedQty` becomes effective `received_qty` after receive-check approval.
- `qc_passed_qty + qc_failed_qty = confirmedReceivedQty`.
- `receive_checker_note` is optional when checked quantity equals worker-entered quantity and required when different.
- A physically present `qc_failed_qty` creates or updates a quarantine source record with `origin_type = INTERNAL_TRANSFER` and `origin_id = transfer_item.id`.
- A negative `variance_qty` represents missing stock and must not create a quarantine source record.
- An intact wrong-SKU item remains linked to the transfer return-to-source flow and is not marked for disposal unless damage/QC failure is recorded separately.
- Destination location for QC-passed quantity must have enough remaining bin capacity.

## WrongSkuReport

**Table**: `transfer_wrong_sku_reports` or equivalent actual table name

Fields to add/verify:
- `id`
- `transfer_id`
- `transfer_item_id`
- `expected_product_id`
- `actual_product_id`
- `quantity`
- `reason`
- `photo_refs`
- `status`: `REPORTED`, `APPROVED`, `REJECTED`, `RETURN_DEPARTED`, `RETURN_ARRIVED`, `CLOSED`
- `reported_by`, `reported_at`
- `decided_by`, `decided_at`, `decision_reason`

Validation:
- Expected and actual products must be different.
- Quantity must be positive and must not exceed the affected in-transit quantity.
- Reason is required.
- Photo references are optional for wrong-SKU in Sprint 1 but must be preserved if supplied.
- A manager decision requires destination warehouse manager scope.

## Transfer Return Decision

Fields on `transfers`:
- `is_returned`
- `return_reason_code`: `TRIP_OVERDUE`, `WRONG_SKU`, or `OTHER_APPROVED_REASON`
- `return_reason`
- `return_requested_by`, `return_requested_at`
- `return_approved_by`, `return_approved_at`

Rules:
- Destination Storekeeper creates the `WRONG_SKU` report while the transfer remains `IN_TRANSIT`.
- Destination Warehouse Manager approves or rejects the wrong-SKU return within destination warehouse scope.
- Approval sets `is_returned = true`; the same trip, vehicle, driver, and In-Transit inventory remain active for the return leg.
- Assigned driver must record return departure and source arrival/handover before source receiving starts.
- Source Staff records return count, source Storekeeper checks/QC, and source Warehouse Manager final-confirms.
- Final source confirmation completes the transfer while retaining `is_returned = true` for reporting.

## Trip

**Table**: `trips`

Transfer usage:
- `trip_type = TRANSFER`
- exactly one trip per transfer
- selected vehicle and driver must be available and not in overlapping trip
- assigned driver is the only actor allowed to confirm transfer departure
- `total_weight` and `total_volume` for transfer trips are calculated from transfer item quantity and product/package attributes.
- Trip assignment is rejected if calculated weight or volume exceeds vehicle capacity.
- Vehicle/driver/trip can be reassigned only before departure.
- Vehicle/driver release at completion must verify the resource has no other active trip assignment.

## Inventory

Transfer operations:
- Approval: increase source `reserved_qty` by `planned_qty`.
- Approval/reservation: reserve only FIFO-eligible stock from active, non-quarantine, source-scoped locations.
- Cancel unshipped approved: decrease source `reserved_qty`.
- Depart: decrease source `total_qty`, decrease source `reserved_qty`, increase In-Transit `total_qty`.
- Final receive: decrease In-Transit `total_qty`; increase destination regular inventory for QC-passed quantity; increase destination quarantine inventory for QC-failed quantity.
- Final source return receive: decrease In-Transit `total_qty`; restore QC-passed quantity to source regular inventory; move QC-failed physical quantity to source Quarantine.
- For a shortage, destination inventory quantity and value are calculated only from physically received and accepted `received_qty`.
- Missing quantity is retained as a quantity-only `variance_qty`/`TRANSFER_DISCREPANCY` and is excluded from destination receipt value and billing totals.
- Internal transfer valuation never creates sales revenue, invoice, dealer receivable, supplier payable, or supplier Debit Note.
- Transfer quarantine inventory must be reconcilable to `quarantine_records.origin_type = INTERNAL_TRANSFER` before spec 009 disposal.
- No inventory update may create negative total, negative reserved, or negative available.
- Inventory updates must use optimistic locking/version checks.

## Discrepancy Incident / Hold

Create a discrepancy incident/hold when:
- received quantity is lower than sent quantity;
- physical over-receipt is observed and regular inventory posting is blocked;
- final manager confirmation records a material issue beyond normal QC failure.

Required data:
- transfer and transfer item references
- product and warehouse
- discrepancy type: `SHORTAGE`, `OVERAGE_HOLD`, `QC_FAILURE`, `WRONG_SKU`, `OTHER`
- quantity
- reason
- photo references when supplied; Barcode/QR scan references are not required for Sprint 1
- owner/status for follow-up
- audit linkage

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
- `TRANSFER_REQUEST_CREATE`
- `TRANSFER_REQUEST_UPDATE`
- `TRANSFER_REQUEST_SUBMIT`
- `TRANSFER_REQUEST_CEO_APPROVE`
- `TRANSFER_REQUEST_CEO_REJECT`
- `TRANSFER_REQUEST_CONVERT`
- `TRANSFER_REQUEST_CANCEL`
- `TRANSFER_CREATE`
- `TRANSFER_UPDATE`
- `TRANSFER_APPROVE`
- `TRANSFER_REJECT`
- `TRANSFER_TRIP_ASSIGN`
- `TRANSFER_TRIP_REASSIGN`
- `TRANSFER_OUTBOUND_QC`
- `TRANSFER_SHIP`
- `TRANSFER_LOAD_HANDOVER`
- `TRANSFER_UNSHIP`
- `TRANSFER_DEPART`
- `TRANSFER_ARRIVE`
- `TRANSFER_ARRIVAL_HANDOVER`
- `TRANSFER_RECEIVE_COUNT`
- `TRANSFER_RECEIVE_CHECK`
- `TRANSFER_RECEIVE_CONFIRM`
- `TRANSFER_DISCREPANCY_CREATE`
- `TRANSFER_RETURN_REQUEST`
- `TRANSFER_RETURN_APPROVE`
- `TRANSFER_RETURN_REJECT`
- `TRANSFER_RETURN_TO_SOURCE`
- `TRANSFER_RETURN_DEPART`
- `TRANSFER_RETURN_ARRIVE`
- `TRANSFER_RETURN_HANDOVER`
- `TRANSFER_CANCEL`

Each audit entry must include actor, action, entity type, entity id/code, timestamp, before state, and after state where relevant. Transfer audit snapshots must include header, items, allocations, QC quantities, wrong-SKU report lines, trip/resource state, and inventory movement references.
