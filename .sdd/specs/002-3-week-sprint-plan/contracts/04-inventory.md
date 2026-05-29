# Inventory API — /api/v1/inventory

## GET /api/v1/inventory
**Query**: ?warehouseId=X&productId=Y&batchId=Z&binLocationId=W&available=true&page=0&size=20
**Response 200**: Paginated inventory (quantity, reserved_qty, available_qty)

## GET /api/v1/inventory/{id}
**Response 200**: Inventory with batch, product, bin details

## GET /api/v1/inventory/stock-card
**Query**: ?productId=X&warehouseId=Y&fromDate=ISO&toDate=ISO
**Response 200**: Stock card entries (in/out/balance) for audit

## GET /api/v1/inventory/low-stock
**Query**: ?threshold=10&warehouseId=X
**Response 200**: Products below stock threshold

## POST /api/v1/inventory/adjust
**Request**: `{ "inventory_id", "new_quantity", "reason", "approved_by" }`
**Response 200**: Updated inventory
**Note**: Only thru adjustment flow, never direct UPDATE
