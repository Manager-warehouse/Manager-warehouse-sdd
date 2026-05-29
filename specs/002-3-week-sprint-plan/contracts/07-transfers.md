# Transfer Order API — /api/v1/transfers

## GET /api/v1/transfers
**Query**: ?sourceWarehouseId=X&destWarehouseId=Y&status=IN_TRANSIT&page=0&size=20
**Response 200**: Paginated transfer list

## POST /api/v1/transfers
**Request**: `{ "source_warehouse_id", "dest_warehouse_id", "items": [{ "product_id", "batch_id", "quantity_sent" }], "notes" }`
**Response 201**: Transfer (status=DRAFT)

## GET /api/v1/transfers/{id}
**Response 200**: Transfer with items

## PUT /api/v1/transfers/{id}/submit
**Response 200**: Status -> PENDING_APPROVAL

## PUT /api/v1/transfers/{id}/approve
**Request**: `{ "approved_by" }`
**Response 200**: Status -> APPROVED, inventory source reduced, in-transit increased
**Note**: Requires WAREHOUSE_MANAGER

## PUT /api/v1/transfers/{id}/ship
**Response 200**: Status -> IN_TRANSIT, physical goods shipped

## PUT /api/v1/transfers/{id}/receive
**Request**: `{ "items": [{ "transfer_item_id", "quantity_received" }] }`
**Response 200**: Status -> COMPLETED (or PARTIALLY_RECEIVED), inventory dest increased, in-transit reduced, adjustment if mismatch
