# Quickstart: 005 Inter-Warehouse Transfer

## Prerequisites

- Backend runs with Java 21 and Spring Boot 3.4.5.
- Frontend runs with React 18.
- A test user exists for each role: Planner, source warehouse manager, Dispatcher, source storekeeper, assigned driver, destination worker, destination storekeeper, destination manager.
- Warehouses exist for Hải Phòng, Hà Nội, Hồ Chí Minh, one In-Transit warehouse, and at least one active quarantine location for each destination warehouse.
- `RN-*` supplier inbound receipts are available in `/inbound/receipts`.
- `TRF-*` internal transfer documents are available in `/transfers` and stay in that module through the full receive flow.

## Backend Validation Flow

1. Create a transfer as Planner:
   - `POST /api/v1/transfers`
   - include `externalInstructionCode`, source warehouse, destination warehouse, document date, planned date, and at least one item.
   - expect transfer number format `TRF-YYYYMMDD-####` and status `NEW`.

2. Edit the transfer while `NEW`:
   - `GET /api/v1/transfers/{id}`
   - `PUT /api/v1/transfers/{id}`
   - omit an old item to remove it.
   - expect updated item list and audit log.

3. Approve as source warehouse manager:
   - `POST /api/v1/transfers/{id}/approve`
   - expect source reserved quantity increased and status `APPROVED`.

4. Assign trip as Dispatcher:
   - `POST /api/v1/transfers/{id}/trip`
   - use an available vehicle and a driver whose warehouse scope includes the transfer source warehouse.
   - expect one `TRANSFER` trip linked to the transfer and trip number format `TTR-YYYYMMDD-####`.

5. Ship as source storekeeper:
   - `POST /api/v1/transfers/{id}/ship`
   - the shipment step records exact approved quantity for every line.
   - sending less or more must return `SENT_QTY_MISMATCH`.

6. Cancel after ship:
   - `POST /api/v1/transfers/{id}/cancel`
   - expect blocked until `/unship`.
   - `POST /api/v1/transfers/{id}/unship`
   - then cancel as source manager.

7. Depart as assigned driver:
   - `POST /api/v1/transfers/{id}/depart`
   - expect source total/reserved decreased, In-Transit total increased, status `IN_TRANSIT`.

8. Receive count as destination worker:
   - `PUT /api/v1/transfers/{id}/receive-count`
   - shortage/over-count or reported issue requires item-level `issueReason`.

9. Receive check as destination storekeeper:
   - `PUT /api/v1/transfers/{id}/receive-check`
   - QC totals must equal `confirmedReceivedQty`.
   - `checkerNote` is required only when confirmed quantity differs from worker-entered quantity.

10. Final receive as destination manager:
    - `POST /api/v1/transfers/{id}/final-receive`
    - shortage requires `discrepancyReason` and creates `TRANSFER_DISCREPANCY`.
    - QC failed quantity moves to quarantine.

## Frontend Validation Flow

1. Planner opens the shared transfer workspace at `/transfers` and creates a manual `TRF` transfer.
2. Planner edits a `NEW` transfer and sees existing items loaded, not a blank form.
3. Source manager sees approval/rejection actions only for source-scoped transfers.
4. Dispatcher sees trip assignment actions only for approved transfers whose source warehouse is in dispatcher scope.
5. Dispatcher can choose only vehicles and drivers valid for the source warehouse scope.
6. Source storekeeper ships exact approved quantity and sees mismatch validation.
7. Driver sees only the assigned transfer trip in the driver trip screen and can depart only that trip.
8. Destination worker records initial count inside the transfer module, not inside the supplier inbound receipt list.
9. Destination storekeeper checks count/QC and selects destination location for passed stock.
10. Destination manager final-confirms completion/discrepancy in the same transfer module.

## Required Checks Before Coding Is Done

- `mvn test` or targeted backend tests pass.
- Frontend tests/build pass.
- OpenAPI/Swagger exposes all transfer endpoints.
- Audit log records every transfer mutation.
- No inventory invariant can become negative.
