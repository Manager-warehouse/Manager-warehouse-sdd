# Quickstart: Inbound Receipt Approval & Quarantine Handling

## Prerequisites

- PostgreSQL is available and Flyway migrations apply successfully.
- Backend compiles with `mvn compile` from `backend/`.
- For Supabase-backed local runs, use the project Spring datasource configuration and run the backend with Java 21. If port `8080` is occupied, run on `8081` with `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`.
- Test users exist for:
  - Trưởng kho assigned to the receipt warehouse.
  - Trưởng kho not assigned to the receipt warehouse.
  - Storekeeper assigned to the receipt warehouse.
- Test data includes:
  - A `QC_COMPLETED` receipt with receipt items and actual quantities.
  - A `QC_FAILED` receipt with quarantine inventory matching receipt actual quantity.
  - Regular Bin and Quarantine Bin locations.

## Scenario 1: Approve Receipt Without Increasing Inventory

1. Load a `QC_COMPLETED` receipt and record current regular inventory for its product/location.
2. Call `PUT /api/v1/receipts/{id}/approve` as assigned Trưởng kho with the current receipt version:

   ```json
   {
     "expectedVersion": 0,
     "note": "QC passed, approve for putaway"
   }
   ```

3. Expect HTTP 200 and receipt status `APPROVED`.
4. Verify `receipt_items.batch_id` is set.
5. Verify regular inventory did not increase.
6. Verify audit action `RECEIPT_APPROVE`.

## Scenario 2: Complete Putaway After Approval

1. Use the `APPROVED` receipt from Scenario 1.
2. Call `PUT /api/v1/receipts/{id}/complete` as Storekeeper with target regular Bin id per receipt item:

   ```json
   {
     "expectedVersion": 1,
     "items": [
       {
         "receiptItemId": 1001,
         "targetLocationId": 501
       }
     ],
     "note": "Putaway completed"
   }
   ```

3. Expect HTTP 200.
4. Verify `receipt_items.location_id` is set.
5. Verify regular `inventories.total_qty` increased by actual quantity.
6. Verify the target location is a regular Bin and has enough remaining volume/weight capacity for `actual_qty`.
7. Verify audit actions `RECEIPT_PUTAWAY_COMPLETE` and `INVENTORY_UPDATE`.

## Scenario 3: Reject Receipt And Confirm Supplier Handover

1. Load a `QC_COMPLETED` receipt.
2. Call `PUT /api/v1/receipts/{id}/reject` as assigned Trưởng kho:

   ```json
   {
     "expectedVersion": 0,
     "rejectionReason": "Supplier delivered damaged packaging"
   }
   ```

3. Expect status `RETURN_TO_SUPPLIER_PENDING`.
4. Verify no batch, inventory, RTV, or Debit Note was created.
5. Call `PUT /api/v1/receipts/{id}/return-to-supplier/confirm` as Storekeeper:

   ```json
   {
     "expectedVersion": 1,
     "handoverReference": "SUP-TRUCK-001",
     "note": "Supplier vehicle collected rejected goods"
   }
   ```

6. Expect status `RETURNED_TO_SUPPLIER`.
7. Verify inventory remains unchanged.
8. Verify audit actions `RECEIPT_REJECT` and `RECEIPT_RETURN_CONFIRM`.

## Scenario 4: Create RTV For QC-Failed Quarantine Receipt

1. Load a `QC_FAILED` receipt with full quarantine inventory.
2. Call `POST /api/v1/receipts/{id}/rtv` as assigned Trưởng kho with reason and current version:

   ```json
   {
     "expectedVersion": 0,
     "reason": "QC failed inbound sample inspection",
     "documentDate": "2026-06-13"
   }
   ```

3. Expect HTTP 201.
4. Verify a pending `RETURN_TO_VENDOR` adjustment exists.
5. Verify a linked Debit Note exists.
6. Verify quarantine inventory is unchanged.
7. Verify audit action `QUARANTINE_RTV_CREATE`.

## Scenario 5: Confirm Full RTV

1. Use the receipt from Scenario 4.
2. Call `PUT /api/v1/receipts/{id}/rtv/confirm` as Storekeeper with `returnedQty` equal to full quarantined quantity:

   ```json
   {
     "expectedVersion": 1,
     "returnedQty": 20,
     "handoverReference": "RTV-TRUCK-001",
     "note": "Full quarantine quantity returned"
   }
   ```

3. Expect HTTP 200.
4. Verify adjustment has `approved_by` and `approved_at`.
5. Verify quarantine inventory decreased by full quantity and does not go negative.
6. Verify receipt remains `QC_FAILED`.
7. Verify audit actions `QUARANTINE_RTV_CONFIRM` and `INVENTORY_UPDATE`.

## Scenario 6: Block Partial RTV

1. Load a `QC_FAILED` receipt with 20 units in quarantine.
2. Call `PUT /api/v1/receipts/{id}/rtv/confirm` with `returnedQty = 18`.
3. Expect HTTP 422 `RTV_QUANTITY_MISMATCH`.
4. Verify adjustment is still pending.
5. Verify quarantine inventory remains 20.

## Scenario 7: Authorization And Concurrency

1. Attempt approve/reject/RTV using a manager assigned to another warehouse.
2. Expect HTTP 403.
3. Submit approve/reject with stale `expectedVersion`.
4. Expect HTTP 409.
5. Repeat approve/reject/RTV create after a final decision or existing RTV.
6. Expect HTTP 409 and no duplicate audit/inventory/accounting records.

## Verification Commands

```bash
cd backend
mvn compile
mvn test -Dtest='*Receipt*Test,*Inbound*Test,*Quarantine*Test'
```

To run the backend against Supabase locally:

```bash
cd backend
JAVA_HOME=/Users/haison/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/haison/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Then verify OpenAPI is reachable:

```bash
curl -I http://127.0.0.1:8081/v3/api-docs
```

Expected result: HTTP 200 and the app log shows `WMSHikariCP - Start completed` against the Supabase PostgreSQL database.

Observed Supabase migration note from the 2026-06-13 local run: Flyway validated successfully, but the connected schema was at version 18 while the local migration set's latest available version was 13, with `outOfOrder` active. Treat that as a shared-database coordination warning before applying further migration changes.

If frontend changes are included:

```bash
cd frontend
npm test -- --runInBand
npm run build
```
