# Quickstart: 005 Inter-Warehouse Transfer

## Prerequisites

- Backend runs with Java 21 and Spring Boot 3.4.5.
- Frontend runs with React 18.
- A test user exists for each role: Planner, requesting warehouse manager, CEO, source warehouse manager, Dispatcher, source storekeeper, assigned driver, destination worker, destination storekeeper, destination manager.
- Warehouses exist for Hải Phòng, Hà Nội, Hồ Chí Minh, one In-Transit warehouse, and at least one active quarantine location for each destination warehouse.
- `RN-*` supplier inbound receipts are available in `/inbound/receipts`.
- `TRF-*` internal transfer documents are available in `/transfers` and stay in that module through the full receive flow.

## Backend Validation Flow

1. Search cross-warehouse stock as requesting warehouse manager:
   - `GET /api/v1/warehouse-stock/cross-warehouse`
   - expect read-only available quantities for other active warehouses.
   - quarantine inventory must not be counted as available.

2. Create and submit a manager transfer request:
   - `POST /api/v1/transfer-requests`
   - include requesting warehouse, source warehouse, needed-by date, business reason, and item lines.
   - `POST /api/v1/transfer-requests/{id}/submit`
   - expect request number format `TRQ-YYYYMMDD-####` and status `SUBMITTED`.

3. Approve as CEO and convert as Planner:
   - `POST /api/v1/transfer-requests/{id}/ceo-approve`
   - expect status `CEO_APPROVED` and approved template/notification for source Planner.
   - `POST /api/v1/transfer-requests/{id}/convert-to-transfer`
   - expect one linked `TRF-*` and request status `CONVERTED`.

4. Create a transfer as Planner:
   - `POST /api/v1/transfers`
   - include `externalInstructionCode`, source warehouse, destination warehouse, document date, planned date, and at least one item.
   - expect transfer number format `TRF-YYYYMMDD-####` and status `NEW`.

5. Edit the transfer while `NEW`:
   - `GET /api/v1/transfers/{id}`
   - `PUT /api/v1/transfers/{id}`
   - omit an old item to remove it.
   - expect updated item list and audit log.

6. Approve as source warehouse manager:
   - `POST /api/v1/transfers/{id}/approve`
   - expect source reserved quantity increased and status `APPROVED`.

7. Assign trip as Dispatcher:
   - `POST /api/v1/transfers/{id}/trip`
   - use an available vehicle and a driver whose warehouse scope includes the transfer source warehouse.
   - expect one `TRANSFER` trip linked to the transfer and trip number format `TTR-YYYYMMDD-####`.

8. Ship as source storekeeper:
   - `POST /api/v1/transfers/{id}/ship`
   - the shipment step records exact approved quantity for every line.
   - sending less or more must return `SENT_QTY_MISMATCH`.

9. Cancel after ship:
   - `POST /api/v1/transfers/{id}/cancel`
   - expect blocked until `/unship`.
   - `POST /api/v1/transfers/{id}/unship`
   - then cancel as source manager.

10. Depart as assigned driver:
   - `POST /api/v1/transfers/{id}/depart`
   - expect source total/reserved decreased, In-Transit total increased, status `IN_TRANSIT`.

11. Receive count as destination worker:
   - `PUT /api/v1/transfers/{id}/receive-count`
   - shortage/over-count or reported issue requires item-level `issueReason`.

12. Receive check as destination storekeeper:
   - `PUT /api/v1/transfers/{id}/receive-check`
   - QC totals must equal `confirmedReceivedQty`.
   - `checkerNote` is required only when confirmed quantity differs from worker-entered quantity.

13. Final receive as destination manager:
    - `POST /api/v1/transfers/{id}/final-receive`
    - shortage requires `discrepancyReason` and creates `TRANSFER_DISCREPANCY`.
    - QC failed quantity moves to quarantine.

## Frontend Validation Flow

1. Requesting warehouse manager searches other warehouses' available stock and starts a transfer request from the shortage context.
2. CEO opens submitted requests and approves or rejects with reason.
3. Source Planner sees the approved request template and converts it to a `TRF`.
4. Planner opens the shared transfer workspace at `/transfers` and creates or reviews the manual `TRF` transfer.
5. Planner edits a `NEW` transfer and sees existing items loaded, not a blank form.
6. Source manager sees approval/rejection actions only for source-scoped transfers.
7. Dispatcher sees trip assignment actions only for approved transfers whose source warehouse is in dispatcher scope.
8. Dispatcher can choose only vehicles and drivers valid for the source warehouse scope.
9. Source storekeeper ships exact approved quantity and sees mismatch validation.
10. Driver sees only the assigned transfer trip in the driver trip screen and can depart only that trip.
11. Destination worker records initial count inside the transfer module, not inside the supplier inbound receipt list.
12. Destination storekeeper checks count/QC and selects destination location for passed stock.
13. Destination manager final-confirms completion/discrepancy in the same transfer module.

## Required Checks Before Coding Is Done

- `mvn test` or targeted backend tests pass.
- Frontend tests/build pass.
- OpenAPI/Swagger exposes all transfer endpoints.
- OpenAPI/Swagger exposes transfer-request endpoints and cross-warehouse stock lookup.
- Audit log records every transfer mutation.
- Audit log records transfer-request create/submit/CEO approval/rejection/conversion.
- No inventory invariant can become negative.
