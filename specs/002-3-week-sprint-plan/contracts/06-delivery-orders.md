# Delivery Order API — /api/v1/delivery-orders

## GET /api/v1/delivery-orders
**Query**: ?warehouseId=X&status=PICKING&dealerId=Y&page=0&size=20
**Response 200**: Paginated DO list

## POST /api/v1/delivery-orders
**Request**: `{ "warehouse_id", "dealer_id", "items": [{ "product_id", "quantity", "price" }], "notes" }`
**Response 201**: DO with auto credit check (CREATED or CREDIT_HOLD)

## GET /api/v1/delivery-orders/{id}
**Response 200**: DO with items, credit check status

## PUT /api/v1/delivery-orders/{id}/pick
**Request**: `{ "items": [{ "delivery_order_item_id", "batch_id", "picked_qty" }] }`
**Response 200**: Reserved qty consumed, picking status updated

## PUT /api/v1/delivery-orders/{id}/qc-outbound
**Request**: `{ "qc_passed": true/false, "notes" }`
**Response 200**: Status -> READY_TO_SHIP or QC_FAILED

## PUT /api/v1/delivery-orders/{id}/ship
**Request**: `{ "trip_id" }`
**Response 200**: Status -> IN_TRANSIT

## PUT /api/v1/delivery-orders/{id}/deliver
**Request**: `{ "pod_confirmed": true/false, "pod_notes", "pod_images" }`
**Response 200**: Status -> DELIVERED or RETURNED
**Note**: On DELIVERED -> trigger invoice creation

## PUT /api/v1/delivery-orders/{id}/cancel
**Request**: `{ "reason" }`
**Response 200**: Status -> CANCELLED, reserved qty released
