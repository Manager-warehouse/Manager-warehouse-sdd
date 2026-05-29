# Receipt API — /api/v1/receipts

## GET /api/v1/receipts
**Query**: ?warehouseId=X&status=PENDING&page=0&size=20
**Response 200**: Paginated receipt list

## POST /api/v1/receipts
**Request**: `{ "warehouse_id", "items": [{ "product_id", "expected_qty" }], "notes" }`
**Response 201**: Receipt (status=PENDING), audit log created

## GET /api/v1/receipts/{id}
**Response 200**: Receipt with items + QC status

## PUT /api/v1/receipts/{id}/qc
**Request**: `{ "items": [{ "receipt_item_id", "qc_passed_qty", "qc_failed_qty", "qc_notes", "batch_id?" }] }`
**Response 200**: Updated QC status; failed items moved to QUARANTINE zone

## PUT /api/v1/receipts/{id}/approve
**Request**: `{ "approved_by" }`
**Response 200**: Receipt APPROVED, inventory updated for passed items
**Note**: Requires WAREHOUSE_MANAGER or higher

## PUT /api/v1/receipts/{id}/reject
**Request**: `{ "reason" }`
**Response 200**: Receipt REJECTED, no inventory impact

## PUT /api/v1/receipts/{id}/complete
**Request**: `{ "bin_mappings": [{ "receipt_item_id", "bin_location_id" }] }`
**Response 200**: Receipt COMPLETED, items putaway to bins
