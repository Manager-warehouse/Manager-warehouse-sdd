# Quickstart: Warehouse Staff Receipt Counting

## Preconditions

1. A Planner has created a purchase receipt in `PENDING_RECEIPT`.
2. The receipt has one or more `receipt_items`.
3. The authenticated user has role `WAREHOUSE_STAFF` and is assigned to the receipt warehouse.
4. The receipt status is not `APPROVED` or `REJECTED`.

## Submit Complete Counts

```bash
curl -X PUT "http://localhost:8080/api/v1/receipts/1001/receive" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      { "receipt_item_id": 501, "counted_qty": 98 },
      { "receipt_item_id": 502, "counted_qty": 120 }
    ]
  }'
```

Expected result:

- Receipt status becomes `DRAFT`.
- Item `501` stores `actual_qty = 98`, `over_received_qty = 0` if expected quantity is at least 98.
- Item `502` caps `actual_qty` at `expected_qty` and stores any excess in `over_received_qty`.
- No inventory, batch, quarantine, or location records are created or updated.
- A receive audit log is created.

## Correct Counts Before Manager Decision

```bash
curl -X PUT "http://localhost:8080/api/v1/receipts/1001/receive" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      { "receipt_item_id": 501, "counted_qty": 100 },
      { "receipt_item_id": 502, "counted_qty": 118 }
    ]
  }'
```

Expected result:

- If receipt was `DRAFT`, `QC_COMPLETED`, or `QC_FAILED`, receipt returns/remains `DRAFT`.
- Prior QC sample/result data is cleared when present.
- A receive audit log captures before/after count values.

## Error Checks

- Omit any receipt item: expect `422 RECEIPT_COUNT_INCOMPLETE`.
- Send `counted_qty = 0`, negative, fractional, duplicate item id, or item id from another receipt: expect `422 INVALID_RECEIPT_COUNT`.
- Submit for `APPROVED` or `REJECTED` receipt: expect `409 RECEIPT_ALREADY_FINALIZED`.
- Submit as a user not assigned to the receipt warehouse: expect `403`.

## Verification Commands

```bash
cd backend
mvn test
mvn compile
```
