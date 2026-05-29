# StockTake API — /api/v1/stocktakes

## GET /api/v1/stocktakes
**Query**: ?warehouseId=X&status=IN_PROGRESS&page=0&size=20
**Response 200**: Paginated stocktake list

## POST /api/v1/stocktakes
**Request**: `{ "warehouse_id", "bin_location_ids": [...], "notes" }`
**Response 201**: StockTake with system quantities pre-filled (status=DRAFT)

## GET /api/v1/stocktakes/{id}
**Response 200**: StockTake with items + variance

## PUT /api/v1/stocktakes/{id}/start
**Response 200**: Status -> IN_PROGRESS, locks affected bins

## PUT /api/v1/stocktakes/{id}/count
**Request**: `{ "items": [{ "stocktake_item_id", "actual_qty", "notes" }] }`
**Response 200**: Variance calculated (actual - system)

## PUT /api/v1/stocktakes/{id}/complete
**Response 200**: Status -> COMPLETED, variance total computed

## PUT /api/v1/stocktakes/{id}/approve
**Request**: `{ "approved_by" }`
**Response 200**: Status -> APPROVED (route to CEO if variance > 100M VND)

## PUT /api/v1/stocktakes/{id}/adjust
**Response 200**: Status -> ADJUSTED, inventory quantities updated, audit log created
