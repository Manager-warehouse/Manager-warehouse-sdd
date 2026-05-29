# Product & Batch API — /api/v1/products, /api/v1/batches

## GET /api/v1/products
**Query**: ?page=0&size=20&category=X&search=keyword
**Response 200**: Paginated product list

## POST /api/v1/products
**Request**: `{ "sku", "name", "category", "unit", "has_serial": false, "has_expiry": false }`
**Response 201**: Product object

## GET /api/v1/products/{id}
**Response 200**: Product with active batches

## PUT /api/v1/products/{id}
**Request**: Partial product update
**Response 200**: Updated product

## GET /api/v1/batches
**Query**: ?productId=X&warehouseId=Y&grade=A&expired=false
**Response 200**: List of batches

## POST /api/v1/batches
**Request**: `{ "product_id", "batch_code", "grade", "expiry_date"?, "received_date" }`
**Response 201**: Batch object
**Validation**: Grade must be A/B/C only

## GET /api/v1/batches/{id}/inventory
**Response 200**: Inventory records for this batch across warehouses
