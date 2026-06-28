# Quickstart: Storekeeper Picking Plan

## Goal

Implement concrete picking-plan save, picking-plan revision with conditional return-to-bin, and replacement planning for outbound Delivery Orders while preserving FIFO, RBAC, optimistic locking, and audit requirements.

## Suggested implementation order

1. Add Flyway migrations and JPA entities for `delivery_order_item_allocations`, `delivery_order_item_return_to_bin_records`, and `delivery_order_item_replacements`.
2. Extend repositories and inventory query helpers to fetch FIFO-ranked valid stock, current allocations, QC records by allocation, and versioned reservation rows.
3. Add request DTOs and controller endpoints for:
   - `PUT /api/v1/delivery-orders/{id}/picking-plan`
   - `PUT /api/v1/delivery-orders/{id}/replacement-plan`
4. Implement service methods that:
   - validate storekeeper role plus warehouse assignment
   - validate Delivery Order status
   - validate full planned quantity per item
   - diff existing vs requested allocations
   - enforce return-to-bin only for changed picked allocations
   - update `warehouse_product_reservations` and `inventories.reserved_qty` with optimistic locking
   - write audit logs for save, return, and replacement
5. Update response mapping so Delivery Order detail can expose current allocations and replacement metadata if needed by the UI.

## API walkthrough

### 1. Save initial picking plan

Request:

```http
PUT /api/v1/delivery-orders/101/picking-plan
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "allocations": [
    {
      "doItemId": 1001,
      "inventoryId": 501,
      "batchId": 71,
      "locationId": 801,
      "zoneId": 31,
      "plannedQty": 6
    },
    {
      "doItemId": 1001,
      "inventoryId": 502,
      "batchId": 72,
      "locationId": 802,
      "zoneId": 31,
      "plannedQty": 4
    }
  ]
}
```

Expected result:

- Validate assigned warehouse scope and `NEW` status.
- Validate item 1001 total `plannedQty = requestedQty`.
- Decrease matching `warehouse_product_reservations.reserved_qty`.
- Increase selected `inventories.reserved_qty`.
- Persist allocation rows and move DO to `WAITING_PICKING`.
- Write `PICKING_PLAN_SAVE` audit.

### 2. Revise plan after some allocations were already picked

Request must include only the return records for changed picked allocations:

```json
{
  "allocations": [
    {
      "doItemId": 1001,
      "inventoryId": 501,
      "batchId": 71,
      "locationId": 801,
      "zoneId": 31,
      "plannedQty": 3
    },
    {
      "doItemId": 1001,
      "inventoryId": 503,
      "batchId": 73,
      "locationId": 803,
      "zoneId": 32,
      "plannedQty": 7
    }
  ],
  "returnToBinRecords": [
    {
      "allocationId": 9001,
      "returnedQty": 3,
      "sourceLocationId": 880,
      "reason": "Rebalance after picked bin became blocked"
    }
  ]
}
```

Expected result:

- Reject with `PICKED_GOODS_RETURN_REQUIRED` if a changed picked allocation lacks a valid return record.
- For valid requests, move returned goods back to the original row, write `PICKED_GOODS_RETURN_TO_BIN` audit, then apply the revised allocation diff.

### 3. Save replacement plan after QC fail

```http
PUT /api/v1/delivery-orders/101/replacement-plan
```

```json
{
  "replacements": [
    {
      "doItemId": 1001,
      "failedInventoryId": 601,
      "failedBatchId": 71,
      "failedLocationId": 801,
      "replacementInventoryId": 504,
      "replacementBatchId": 74,
      "replacementLocationId": 804,
      "replacementZoneId": 32,
      "quantity": 2,
      "reason": "QC fail scratched cookware"
    }
  ]
}
```

Expected result:

- Validate DO is `QC_PENDING_APPROVAL`.
- Persist replacement history and replacement allocation.
- Reserve replacement inventory rows.
- Move DO back to `WAITING_PICKING`.
- Write `PICKING_REPLACEMENT_SAVE` audit.

## Required tests

- Service test: initial plan converts warehouse/product reservation into concrete inventory reservation.
- Service test: reject save when per-item planned total differs from requested quantity.
- Service test: reject storekeeper outside assigned warehouse.
- Service test: revise `WAITING_PICKING` plan releases removed concrete reservation and reserves added rows.
- Service test: changed picked allocation requires `returnToBinRecords`.
- Service test: unchanged picked allocation does not require return record.
- Service test: invalid return quantity or wrong original location is rejected.
- Service test: replacement plan only works in `QC_PENDING_APPROVAL` and only for unresolved QC-failed quantity.
- Service test: optimistic locking conflict on inventory or warehouse-product reservation returns a conflict error.
- Controller integration test: happy-path `picking-plan` request returns updated Delivery Order.
- Controller integration test: `replacement-plan` request returns DO moved back to `WAITING_PICKING`.

## Definition of done reminders

- Keep FIFO selection logic explicit and tested.
- Document both endpoints in OpenAPI.
- Ensure all warehouse mutations create audit logs with before/after context.
- Do not bypass validation or update inventory directly outside the planned outbound mutation flow.
