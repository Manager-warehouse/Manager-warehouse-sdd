# Quickstart: Planner Receipt Drafting

## Preconditions

- Backend runs with PostgreSQL and Flyway migrations applied.
- Auth is enabled and the caller has Planner permissions for the target warehouse.
- Supplier, warehouse, and products in the request are active.

## API Flow

1. Authenticate as a Planner.
2. Submit `POST /api/v1/receipts` with supplier, contact person, warehouse, PO/source reference, source channel, and expected items.
3. Verify the response has status `PENDING_RECEIPT` and a generated receipt number.
4. Verify no inventory rows, batches, QC fields, or quarantine locations are created/updated by this request.
5. Verify an audit entry exists for the receipt creation.

## Example Request

```json
{
  "supplier_id": 12,
  "contact_person": "Nguyen Van A",
  "warehouse_id": 1,
  "source_reference": "PO-2026-00045",
  "source_channel": "ZALO",
  "items": [
    {
      "product_id": 101,
      "expected_qty": 500
    }
  ],
  "notes": "Supplier notified by Zalo"
}
```

## Expected Response

```json
{
  "id": 9001,
  "receipt_number": "RN-20260613-0001",
  "type": "PURCHASE",
  "status": "PENDING_RECEIPT",
  "supplier_id": 12,
  "warehouse_id": 1,
  "source_reference": "PO-2026-00045",
  "source_channel": "ZALO",
  "items": [
    {
      "product_id": 101,
      "expected_qty": 500
    }
  ]
}
```

## Required Tests

- Creates a purchase receipt with generated number and `PENDING_RECEIPT`.
- Rejects missing supplier, contact person, warehouse, source reference, source channel, or empty items.
- Rejects `RETURN` receipt creation through this flow.
- Rejects source channel outside `ZALO` and `EMAIL`.
- Rejects inactive supplier, warehouse, or product.
- Rejects expected quantity `0`, negative, or fractional.
- Rejects duplicate source reference for same supplier and warehouse.
- Writes audit log and does not mutate inventory.

## Verification Commands

```powershell
cd backend
mvn test
mvn compile
```
