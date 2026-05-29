# Audit & Reporting API — /api/v1/audit-logs, /api/v1/reports

## GET /api/v1/audit-logs
**Query**: ?entityType=INVENTORY&entityId=123&actorId=5&action=RECEIPT_APPROVED&fromDate=ISO&toDate=ISO&page=0&size=20
**Response 200**: Paginated audit log entries

## GET /api/v1/reports/inventory-summary
**Query**: ?warehouseId=X&date=ISO
**Response 200**: Summary by warehouse (total qty, total value, item count)

## GET /api/v1/reports/inventory-valuation
**Query**: ?warehouseId=X&date=ISO
**Response 200**: Inventory valuation report (cost basis)

## GET /api/v1/reports/daily-transactions
**Query**: ?warehouseId=X&date=ISO
**Response 200**: Daily transaction summary (receipts, deliveries, transfers)

## GET /api/v1/reports/dealer-aging
**Response 200**: Dealer aging summary
