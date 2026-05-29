# Dispatch & Trip API — /api/v1/trips

## GET /api/v1/trips
**Query**: ?warehouseId=X&status=PLANNED&driverId=Y&page=0&size=20
**Response 200**: Paginated trip list

## POST /api/v1/trips
**Request**: `{ "warehouse_id", "driver_id", "vehicle_plate", "delivery_order_ids": [...], "stop_order": [...], "notes" }`
**Response 201**: Trip object

## GET /api/v1/trips/{id}
**Response 200**: Trip with delivery orders, driver info

## PUT /api/v1/trips/{id}/start-loading
**Response 200**: Status -> LOADING

## PUT /api/v1/trips/{id}/depart
**Response 200**: Status -> IN_TRANSIT, DOs updated

## PUT /api/v1/trips/{id}/confirm-delivery
**Request**: `{ "delivery_order_id", "status": "DELIVERED/RETURNED", "pod_notes", "pod_image" }`
**Response 200**: Stop confirmed

## PUT /api/v1/trips/{id}/complete
**Response 200**: Status -> COMPLETED, all DOs final
