# Feature: Truong kho de xuat dieu chuyen tu ton kho kho khac va CEO duyet (US-WMS-11A)

## 1. Context and Goal

Khi kho dang thieu hang, Truong kho can xem ton kho kha dung cua cac kho khac de biet kho nao con hang co the dieu chuyen. Vi day la quyet dinh dieu phoi giua cac kho vat ly, Truong kho khong duoc tu tao lenh xuat hang truc tiep. Truong kho tao yeu cau dieu chuyen, gui CEO phe duyet, sau do he thong gui mau de nghi da duyet den Planner cua kho nguon de Planner tao phieu `TRF-*` theo luong dieu chuyen noi bo hien co.

Luong nay la tien xu ly cua transfer, khong thay the buoc Planner tao `TRF`, Truong kho nguon phe duyet ton, Dispatcher gan xe, Thu kho xuat hang, va kho dich nhan hang.

## 2. Actors

* **Truong kho kho yeu cau**: Xem ton kho kha dung cua kho khac, tao va gui yeu cau dieu chuyen cho CEO.
* **CEO**: Xem nhu cau, ton kho tham chieu, ly do thieu hang, sau do phe duyet hoac tu choi.
* **Planner kho nguon / Planner trung tam**: Nhan mau de nghi da duyet va tao `TRF-*` tu yeu cau da duoc CEO phe duyet.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * The system SHALL allow a WAREHOUSE_MANAGER to view read-only available stock of other active warehouses for products they search or select.
  * The system SHALL calculate available stock as `totalQty - reservedQty` and SHALL NOT include quarantine inventory as available stock.
  * The system SHALL NOT allow the viewing warehouse manager to change inventory, reserve stock, or create shipment documents in another warehouse from the stock lookup screen.
  * The system SHALL require warehouse-scope authorization: a warehouse manager may create requests only for their own assigned requesting warehouse.
  * The system SHALL store manager-initiated transfer requests separately from executable `TRF-*` transfers until CEO approval and Planner conversion.
  * The system SHALL require CEO approval before a manager-initiated transfer request can be converted into a `TRF-*`.
  * The system SHALL notify or assign the approved request template to the Planner responsible for the source warehouse after CEO approval.
  * The system SHALL create audit log entries for request creation, submission, CEO approval/rejection, and Planner conversion.

* **Event-driven:**
  * WHEN a warehouse manager searches cross-warehouse stock, the system SHALL return product, warehouse, total quantity, reserved quantity, available quantity, and quarantine-excluded availability.
  * WHEN HP warehouse manager sees that HCM has available stock for an item HP lacks, the manager MAY create a transfer request with HP as requesting/destination warehouse and HCM as proposed source warehouse.
  * WHEN a manager creates a transfer request, the system SHALL require source warehouse, requesting warehouse, needed-by date, business reason, and at least one item line.
  * WHEN a manager adds an item line, the system SHALL require product, requested quantity, observed source available quantity, and observed requesting warehouse available quantity.
  * WHEN requested quantity is greater than current source available quantity at submit time, the system SHALL reject the request.
  * WHEN the manager submits the request, the system SHALL set status to `SUBMITTED` and route it to CEO review.
  * WHEN CEO approves a submitted request, the system SHALL set status to `CEO_APPROVED`, record approval metadata, and send/generate the approved request template for the source Planner.
  * WHEN CEO rejects a submitted request, the system SHALL require `rejectionReason`, set status to `CEO_REJECTED`, and keep the request immutable for audit.
  * WHEN Planner creates a `TRF-*` from a CEO-approved request, the system SHALL copy source warehouse, destination warehouse, item lines, needed-by/planned date, and traceability reference into the transfer create form.
  * WHEN Planner successfully creates the `TRF-*`, the system SHALL link the transfer to the request and set request status to `CONVERTED`.

* **State-driven:**
  * WHILE a transfer request is `DRAFT`, the requesting warehouse manager MAY edit or cancel it.
  * WHILE a transfer request is `SUBMITTED`, only CEO may approve or reject it; the requesting warehouse manager SHALL NOT edit item lines.
  * WHILE a transfer request is `CEO_APPROVED`, Planner MAY convert it to one `TRF-*`; CEO approval SHALL NOT reserve inventory.
  * WHILE a transfer request is `CEO_REJECTED`, `CONVERTED`, or `CANCELLED`, the system SHALL reject further edits, approval, or conversion.

## 4. API Endpoints

* `GET /api/v1/warehouse-stock/cross-warehouse` - Warehouse manager searches read-only stock availability in other warehouses.
* `POST /api/v1/transfer-requests` - Warehouse manager creates a draft transfer request from cross-warehouse stock visibility.
* `PUT /api/v1/transfer-requests/{id}` - Warehouse manager updates a `DRAFT` request.
* `POST /api/v1/transfer-requests/{id}/submit` - Warehouse manager submits request to CEO.
* `POST /api/v1/transfer-requests/{id}/approve` - CEO approves request and sends/generates the approved request template for source Planner.
* `POST /api/v1/transfer-requests/{id}/reject` - CEO rejects request with required reason.
* `POST /api/v1/transfer-requests/{id}/convert` - Planner creates a `TRF-*` from an approved request.

### Create Request Payload

```json
{
  "requestingWarehouseId": 1,
  "sourceWarehouseId": 3,
  "neededByDate": "2026-06-28",
  "businessReason": "Kho HP thieu hang de giao dai ly trong tuan, kho HCM dang con ton kha dung.",
  "items": [
    {
      "productId": 101,
      "requestedQty": 50,
      "observedSourceAvailableQty": 120,
      "observedRequestingAvailableQty": 5,
      "shortageReason": "Du kien khong du giao hang neu khong dieu chuyen."
    }
  ]
}
```

### Response Payload

```json
{
  "id": 501,
  "requestNumber": "TRQ-20260624-0001",
  "status": "DRAFT",
  "requestingWarehouseId": 1,
  "sourceWarehouseId": 3,
  "neededByDate": "2026-06-28",
  "businessReason": "Kho HP thieu hang de giao dai ly trong tuan, kho HCM dang con ton kha dung.",
  "items": [
    {
      "id": 9001,
      "productId": 101,
      "requestedQty": 50,
      "observedSourceAvailableQty": 120,
      "observedRequestingAvailableQty": 5,
      "shortageReason": "Du kien khong du giao hang neu khong dieu chuyen."
    }
  ],
  "createdAt": "2026-06-24T09:00:00Z"
}
```

## 5. Validation and Error Handling

* `CROSS_WAREHOUSE_STOCK_VIEW_FORBIDDEN` (HTTP 403): actor cannot view cross-warehouse stock.
* `SAME_WAREHOUSE` (HTTP 422): requesting warehouse and source warehouse are the same.
* `WAREHOUSE_INACTIVE` (HTTP 422): requesting or source warehouse is inactive.
* `TRANSFER_REQUEST_ITEMS_REQUIRED` (HTTP 400): request has no item lines.
* `INVALID_TRANSFER_QTY` (HTTP 400): `requestedQty <= 0`.
* `PRODUCT_INACTIVE` (HTTP 422): product is inactive or unavailable for transfer.
* `TRANSFER_REQUEST_REASON_REQUIRED` (HTTP 400): business reason or required item shortage reason is blank.
* `TRANSFER_REQUEST_QTY_EXCEEDS_SOURCE_AVAILABLE` (HTTP 422): requested quantity exceeds current source available quantity.
* `TRANSFER_REQUEST_APPROVAL_NOT_ALLOWED` (HTTP 409): CEO approval/rejection attempted outside `SUBMITTED` status.
* `CEO_REJECTION_REASON_REQUIRED` (HTTP 400): CEO rejects without reason.
* `TRANSFER_REQUEST_NOT_APPROVED` (HTTP 409): Planner attempts conversion before CEO approval.
* `TRANSFER_REQUEST_ALREADY_CONVERTED` (HTTP 409): request already linked to a `TRF-*`.

## 6. Acceptance Criteria

* **Scenario: Warehouse manager requests stock from another warehouse**
  * Given HP warehouse manager sees HP has only 5 available pans and HCM has 120 available pans
  * When the manager creates a request for HCM to send 50 pans to HP with a business reason
  * Then the system SHALL create a `DRAFT` transfer request with HP as requesting/destination warehouse, HCM as source warehouse, observed stock quantities, and a `TRANSFER_REQUEST_CREATE` audit log.

* **Scenario: Submit request to CEO**
  * Given a transfer request is in `DRAFT` status with valid item lines
  * When the requesting warehouse manager submits it
  * Then the system SHALL set status to `SUBMITTED`, record `submittedAt`, route it to CEO review, and create a `TRANSFER_REQUEST_SUBMIT` audit log.

* **Scenario: CEO approves and Planner receives template**
  * Given a transfer request is `SUBMITTED`
  * When CEO approves it
  * Then the system SHALL set status to `CEO_APPROVED`, record CEO approval metadata, create a `TRANSFER_REQUEST_CEO_APPROVE` audit log, and send/generate an approved request template for the Planner of the source warehouse.

* **Scenario: Planner converts approved request to TRF**
  * Given CEO approved a request for HCM to send 50 pans to HP
  * When the source Planner converts the request
  * Then the transfer create form SHALL be prefilled with source HCM, destination HP, requested item lines, and traceability reference
  * And after creation the system SHALL create one `TRF-*`, link it to the request, mark the request `CONVERTED`, and create a `TRANSFER_REQUEST_CONVERT` audit log.

* **Scenario: Block conversion before CEO approval**
  * Given a transfer request is still `DRAFT` or `SUBMITTED`
  * When Planner attempts to create a `TRF-*` from it
  * Then the system SHALL reject the action with `TRANSFER_REQUEST_NOT_APPROVED`.

* **Scenario: CEO rejects with reason**
  * Given a transfer request is `SUBMITTED`
  * When CEO rejects it with a reason
  * Then the system SHALL set status to `CEO_REJECTED`, store the reason, create a `TRANSFER_REQUEST_CEO_REJECT` audit log, and prevent future conversion.
