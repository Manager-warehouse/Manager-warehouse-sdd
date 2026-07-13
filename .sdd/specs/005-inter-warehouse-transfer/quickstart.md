# Quickstart: 005 Inter-Warehouse Transfer

## Prerequisites

- Backend runs with Java 21 and Spring Boot 3.4.5.
- Frontend runs with React 18.
- A test user exists for each role: Planner, requesting warehouse manager, CEO, source warehouse manager, Dispatcher, source storekeeper, assigned driver, destination worker, destination storekeeper, destination manager.
- Warehouses exist for Hải Phòng, Hà Nội, Hồ Chí Minh, one In-Transit warehouse, and at least one active quarantine location for each destination warehouse.
- `RN-*` supplier inbound receipts are available in `/inbound/receipts`.
- `TRF-*` internal transfer documents are available in `/inter-warehouse-transfers` and stay in that module through the full receive flow.

## Backend Validation Flow

1. Search cross-warehouse stock as requesting warehouse manager:
   - `GET /api/v1/transfer-requests/stock-lookup`
   - expect read-only available quantities for other active warehouses.
   - quarantine inventory must not be counted as available.

2. Create and submit a manager transfer request:
   - `POST /api/v1/transfer-requests`
   - include requesting warehouse, source warehouse, needed-by date, business reason, and item lines.
   - `POST /api/v1/transfer-requests/{id}/submit`
   - expect request number format `TRQ-YYYYMMDD-####` and status `SUBMITTED`.

3. Approve as CEO and convert as Planner:
   - `POST /api/v1/transfer-requests/{id}/approve`
   - expect status `CEO_APPROVED` and approved template/notification for source Planner.
   - `POST /api/v1/transfer-requests/{id}/convert`
   - expect one linked `TRF-*` and request status `CONVERTED`.

4. Create a transfer as Planner:
   - `POST /api/v1/inter-warehouse-transfers`
   - include `externalInstructionCode`, source warehouse, destination warehouse, document date, planned date, and at least one item.
   - expect transfer number format `TRF-YYYYMMDD-####` and status `NEW`.

5. Edit the transfer while `NEW`:
   - `GET /api/v1/inter-warehouse-transfers/{id}`
   - `PUT /api/v1/inter-warehouse-transfers/{id}`
   - omit an old item to remove it.
   - expect updated item list and audit log.

6. Approve as source warehouse manager:
   - `POST /api/v1/inter-warehouse-transfers/{id}/approve`
   - expect source reserved quantity increased and status `APPROVED`.

7. Assign trip as Dispatcher:
   - `POST /api/v1/inter-warehouse-transfers/{id}/trip`
   - use an available vehicle and a driver whose warehouse scope includes the transfer source warehouse.
   - expect one `TRANSFER` trip linked to the transfer and trip number format `TTR-YYYYMMDD-####`.

8. Outbound QC, ship, and handover as source storekeeper:
   - `POST /api/v1/inter-warehouse-transfers/{id}/outbound-qc`
   - `POST /api/v1/inter-warehouse-transfers/{id}/ship`
   - `POST /api/v1/inter-warehouse-transfers/{id}/load-handover`
   - outbound QC and load/handover require photo references; no Barcode/QR scan is required.
   - the shipment step records exact approved quantity for every line.
   - sending less or more must return `SENT_QTY_MISMATCH`.

9. Cancel after ship:
   - `POST /api/v1/inter-warehouse-transfers/{id}/cancel`
   - expect blocked until `/unship`.
   - `POST /api/v1/inter-warehouse-transfers/{id}/unship`
   - then cancel as source manager.

10. Depart as assigned driver:
    - `POST /api/v1/inter-warehouse-transfers/{id}/depart`
   - expect source total/reserved decreased, In-Transit total increased, status `IN_TRANSIT`.

11. Arrive and handover as assigned driver/receiving warehouse:
   - `POST /api/v1/inter-warehouse-transfers/{id}/arrive`
   - `POST /api/v1/inter-warehouse-transfers/{id}/arrival-handover`
   - expect receive-count to remain blocked before both events are recorded.

12. Receive count as destination worker:
   - `PUT /api/v1/inter-warehouse-transfers/{id}/receive-count`
   - shortage/over-count or reported issue requires item-level `issueReason`.

13. Receive check as destination storekeeper:
   - `PUT /api/v1/inter-warehouse-transfers/{id}/receive-check`
   - QC totals must equal `confirmedReceivedQty`.
   - destination bin capacity must be enough for QC-passed quantity.
   - `checkerNote` is required only when confirmed quantity differs from worker-entered quantity.

14. Final receive as destination manager:
    - `POST /api/v1/inter-warehouse-transfers/{id}/final-receive`
    - shortage requires `discrepancyReason` and creates `TRANSFER_DISCREPANCY`.
    - QC failed physical quantity moves to quarantine with `INTERNAL_TRANSFER` origin and is handed to spec 009 disposal.
    - shortage quantity does not create quarantine stock.
    - intact wrong SKU uses Return to Source rather than disposal.
    - transfer-origin quarantine stock cannot use supplier RTV.

15. Verify shortage valuation with 30 sent and 28 received:
    - destination inventory receives and calculates value for 28 units only.
    - `TRANSFER_DISCREPANCY` records 2 missing units as quantity only; those units carry no destination receipt amount.
    - no invoice, revenue, receivable, payable, supplier Debit Note, or automatic driver charge is created.

16. Report and approve an intact wrong-SKU return:
    - destination Storekeeper submits `POST /api/v1/inter-warehouse-transfers/{id}/request-return` with transfer item, expected SKU, actual SKU, affected quantity, reason, and photo refs when available.
    - destination Warehouse Manager approves through `POST /api/v1/inter-warehouse-transfers/{id}/approve-return`.
    - expect `isReturned = true`; the same transfer/trip/vehicle/driver and In-Transit stock remain active.
    - assigned driver confirms `POST /api/v1/inter-warehouse-transfers/{id}/return-depart` and `POST /api/v1/inter-warehouse-transfers/{id}/return-arrive`.
    - source Staff performs receive-count, source Storekeeper performs receive-check/QC, and source Warehouse Manager performs final-receive.
    - expect terminal `COMPLETED` with UI label “Đã hoàn về kho nguồn”.

## Frontend Validation Flow

1. Requesting warehouse manager searches other warehouses' available stock and starts a transfer request from the shortage context.
2. CEO opens submitted requests and approves or rejects with reason.
3. Source Planner sees the approved request template and converts it to a `TRF`.
4. Planner opens the shared transfer workspace at `/inter-warehouse-transfers` and creates or reviews the manual `TRF` transfer.
5. Planner edits a `NEW` transfer and sees existing items loaded, not a blank form.
6. Source manager sees approval/rejection actions only for source-scoped transfers.
7. Dispatcher sees trip assignment actions only for approved transfers whose source warehouse is in dispatcher scope.
8. Dispatcher can choose only vehicles and drivers valid for the source warehouse scope.
9. Source storekeeper ships exact approved quantity and sees mismatch validation.
10. Driver sees only the assigned transfer trip in the driver trip screen and can depart only that trip.
11. Destination worker records initial count inside the transfer module, not inside the supplier inbound receipt list.
12. Destination storekeeper checks count/QC and selects destination location for passed stock.
13. Destination manager final-confirms completion/discrepancy in the same transfer module.
14. Quarantine Workspace displays transfer origin and offers disposal only for damaged internal-transfer stock.
15. Destination Storekeeper sees “Báo gửi nhầm SKU”; destination Manager sees approve/reject; neither action is shown outside destination warehouse scope.
16. After approval, the driver sees the return instruction and source-side roles see the same three-step receiving workflow.

## Required Checks Before Coding Is Done

- `mvn test` or targeted backend tests pass.
- Backend controller/integration tests cover every transfer and transfer-request endpoint with happy path, validation failure, invalid state, authorization/scope failure, and stale-version conflict where applicable.
- PostgreSQL/Flyway tests run against the real migration set and prove the latest additive migration works from a clean database.
- Frontend tests/build pass.
- Frontend tests cover every primary action button in the transfer workspace: visible/enabled role-state, hidden/disabled role-state, successful click, failed API response, and post-success refresh.
- At least one frontend-to-backend smoke path passes from `TRQ` creation through final receive, including outbound QC photo refs, load handover photo refs, arrival/handover, receive-check, final receive, inventory movement, and audit assertion.
- OpenAPI/Swagger exposes all transfer endpoints.
- OpenAPI/Swagger exposes transfer-request endpoints and cross-warehouse stock lookup.
- Audit log records every transfer mutation.
- Audit log records transfer-request create/submit/CEO approval/rejection/conversion.
- No inventory invariant can become negative.
- Transfer shortages never become quarantine/disposal quantities.
- Transfer-origin quarantine stock retains transfer-item traceability and cannot create RTV or supplier Debit Note.
- Destination inventory quantity and value include only physically received and accepted goods.
- Wrong-SKU return requires Storekeeper report, destination Manager approval, assigned-driver return, and source three-step receiving.
- Real PostgreSQL/Flyway migration tests pass for the latest additive migration.
- Requirement-to-test mapping exists for every P0 item and exception branch in `tasks.md`.
