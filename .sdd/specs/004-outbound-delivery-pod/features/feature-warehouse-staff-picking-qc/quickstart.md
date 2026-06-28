# Quickstart: Warehouse Staff Picking & QC Outbound

## Goal

Implement warehouse-staff pick/QC submission, QC-pass staging movement, QC-fail quarantine movement, safe duplicate handling, Storekeeper quality approval, and Warehouse Manager approval/reject while preserving inventory integrity, warehouse RBAC, idempotency, and audit requirements.

## Suggested implementation order

1. Add or extend persistence for `outbound_qc_records`, request-hash/idempotency support, quarantine references, and any missing reject-return support fields.
2. Extend repositories to load active Delivery Order detail with allocations, current QC rows, staging/quarantine locations, and versioned inventory rows.
3. Add request DTOs and controller endpoints for:
   - `PUT /api/v1/delivery-orders/{id}/pick-qc-result`
   - `PUT /api/v1/delivery-orders/{id}/quality-approval`
   - `PUT /api/v1/delivery-orders/{id}/warehouse-approval`
   - `PUT /api/v1/delivery-orders/{id}/warehouse-reject`
4. Implement service methods that:
   - validate warehouse-staff / Storekeeper / Warehouse Manager role plus warehouse assignment
   - require one full active pick/QC submission per cycle
   - block duplicate allocation-level QC rows unless request is a safe idempotent replay
   - move passed quantity to outbound staging and failed quantity to quarantine with supporting records
   - re-check staged availability before quality approval
   - return staged pass quantity to original rows on warehouse reject
5. Update response mapping and error handling so the Delivery Order detail exposes QC progress and downstream status changes if needed by the UI.

## API walkthrough

### 1. Save one full pick/QC submission

Request:

```http
PUT /api/v1/delivery-orders/101/pick-qc-result
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "idempotencyKey": "qc-101-cycle-1",
  "results": [
    {
      "doItemId": 1001,
      "allocationId": 9001,
      "batchId": 71,
      "locationId": 801,
      "zoneId": 31,
      "pickedQty": 6,
      "qcPassQty": 5,
      "qcFailQty": 1,
      "qcFailReason": "Surface scratch",
      "stagingLocationId": 880,
      "quarantineLocationId": 990,
      "notes": "Checked visually at source bin"
    }
  ]
}
```

Expected result:

- Validate assigned warehouse scope and `WAITING_PICKING` status.
- Validate the request includes every currently active allocation without a QC row.
- Validate `pickedQty = qcPassQty + qcFailQty`.
- Decrease source regular inventory `total_qty` and `reserved_qty`.
- Increase outbound staging inventory `total_qty` and `reserved_qty` for pass quantity.
- Increase quarantine inventory `total_qty` with `reserved_qty = 0` for fail quantity.
- Create `outbound_qc_records`, quarantine records, inventory adjustments, and audit logs.
- Move the Delivery Order to `QC_PENDING_APPROVAL`.

### 2. Retry the same request safely

Retry the exact same payload with the same `idempotencyKey`.

Expected result:

- Return the previous successful response.
- Do not create new QC records.
- Do not apply inventory movement again.

### 3. Approve quality after all required replacement cycles are done

```http
PUT /api/v1/delivery-orders/101/quality-approval
```

```json
{
  "notes": "All replacement goods passed outbound QC"
}
```

Expected result:

- Validate Delivery Order is `QC_PENDING_APPROVAL`.
- Validate all requested quantity is covered by QC-passed goods in outbound staging.
- Move the order to `QC_COMPLETED`.
- Write `DELIVERY_ORDER_QC_APPROVE` audit.

### 4. Reject outbound after QC completion

```http
PUT /api/v1/delivery-orders/101/warehouse-reject
```

```json
{
  "reason": "Seal issue found before loading",
  "returnToBinRecords": [
    {
      "doItemId": 1001,
      "allocationId": 9001,
      "batchId": 71,
      "returnedQty": 5,
      "sourceLocationId": 880,
      "originalLocationId": 801,
      "originalZoneId": 31,
      "reason": "Return staged goods after warehouse rejection"
    }
  ]
}
```

Expected result:

- Validate Delivery Order is `QC_COMPLETED`.
- Validate all QC-passed staging quantity is covered by the return rows.
- Move staged goods back to the original source rows.
- Release staging reservation and restore regular available stock.
- Keep QC-failed goods in quarantine.
- Move the order to `REJECTED`.
- Write `PICKED_GOODS_RETURN_TO_BIN` and `DELIVERY_ORDER_WAREHOUSE_REJECT` audits.

## Required tests

- Service test: reject pick/QC submission when `pickedQty != qcPassQty + qcFailQty`.
- Service test: reject partial submission when not every active allocation is included.
- Service test: move pass quantity to outbound staging and fail quantity to quarantine in one transaction.
- Service test: create quarantine and `QC_FAIL_OUTBOUND` adjustment records for fail quantity.
- Service test: block duplicate allocation submission without matching idempotency replay.
- Service test: return previous result for same `idempotencyKey` and identical payload.
- Service test: reject same `idempotencyKey` with different payload.
- Service test: replacement cycle accepts only new active allocations and does not require already-passed staging allocations again.
- Service test: block quality approval when unresolved fail quantity remains.
- Service test: warehouse reject returns staged pass quantity to original rows and keeps failed goods in quarantine.
- Controller integration test: happy-path `pick-qc-result` request returns `QC_PENDING_APPROVAL`.
- Controller integration test: quality approval and warehouse approval/reject endpoints return the expected Delivery Order status or business error.

## Definition of done reminders

- Do not introduce a `PICKING` status.
- Keep all inventory mutations version-safe and non-negative.
- Ensure quarantine inventory never becomes regular available stock.
- Keep staging goods reserved for the Delivery Order until later outbound departure flow.
- Document duplicate-handling and idempotency rules in OpenAPI.
