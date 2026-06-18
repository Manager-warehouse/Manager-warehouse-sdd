# Feature: Planner Nhập Lệnh Điều chuyển kho từ Công ty mẹ (US-WMS-11)

## 1. Context and Goal
Planner nhận chỉ đạo điều chuyển hàng từ Công ty mẹ hoặc bộ phận điều phối trung tâm, sau đó nhập phiếu điều chuyển vào hệ thống WMS để kho nguồn kiểm tra, phê duyệt và thực thi. Công ty mẹ không phải user trong hệ thống ở Sprint 1; mọi lệnh điều chuyển từ Công ty mẹ được nhập trung gian qua Planner. Sprint 1 không có nghiệp vụ kho tự quyết định điều chuyển và không có gợi ý điều chuyển tự động dựa trên tồn kho.

Planner thao tác trên man hinh dieu chuyen noi bo dung chung (`/transfers`). Luong nay tach rieng khoi man hinh phieu nhap `RN` tu nha cung cap.
Trong Sprint 1, man nay dong vai tro la workspace van hanh chinh cho transfer. Bao cao/dashboard chuyen sau cho transfer moi o muc co ban: list theo trang thai, route, dong hang, tai xe/chuyen neu da co. Cac KPI tong hop chuyen sau co the bo sung sau.

Phiếu điều chuyển có thể gồm nhiều dòng hàng vì lệnh điều chuyển thực tế có thể yêu cầu gửi nhiều SKU trong cùng một chuyến chứng từ, ví dụ 50 cái chảo và 30 nồi từ kho Hải Phòng sang kho Hà Nội.

## 2. Actors
* **Planner (Người lập kế hoạch)**: Nhập phiếu điều chuyển theo lệnh từ Công ty mẹ hoặc bộ phận điều phối trung tâm.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL NOT generate transfer suggestions or automatically decide source/destination/quantity for inter-warehouse transfers in Sprint 1.
  * The system SHALL create transfer records only from explicit Planner input based on an external transfer instruction.
  * The system SHALL require `externalInstructionCode` for every transfer so the WMS transfer can be traced back to the instruction from Công ty mẹ or the central coordination team.
  * The system SHALL reject duplicate active transfers with the same `externalInstructionCode`, source warehouse, destination warehouse, and `documentDate`; transfers in `REJECTED` or `CANCELLED` status SHALL NOT block creating a corrected transfer for the same external instruction.
  * The system SHALL enforce Planner authorization before transfer create/update/cancel; Planner can create and edit transfers but SHALL NOT assign trips.
  * The system SHALL support multiple transfer item lines in one transfer.
  * The system SHALL store each created transfer with status `NEW`.
  * The system SHALL create a `TRANSFER_CREATE` audit log entry when a Planner creates a transfer.
  * The system SHALL NOT require upload/attachment for the external transfer instruction in Sprint 1.
  * The system SHALL keep the Planner view focused on transfer-document operations; trip dispatching, departure, and destination receiving are handled by later role-specific stages.
* **Event-driven:**
  * WHEN a Planner creates a transfer manually, the system SHALL require source warehouse, destination warehouse, planned date, document date, external instruction code, and at least one item line.
  * WHEN a Planner adds an item line, the system SHALL require product and planned quantity.
  * WHEN a Planner submits a transfer where source warehouse equals destination warehouse, the system SHALL reject the request.
  * WHEN a Planner submits a transfer with inactive product, inactive warehouse, zero quantity, or negative quantity, the system SHALL reject the request.
  * WHEN a Planner opens a transfer edit screen, the system SHALL load the current transfer header and item list so the Planner edits the existing list instead of re-entering it from scratch.
  * WHEN a Planner updates a transfer in `NEW` status, the system SHALL allow editing header fields and adding/updating/removing item lines, then save the full current transfer state after editing.
  * WHEN a Planner updates a `NEW` transfer with a full item list, the system SHALL remove existing transfer item lines that are omitted from the payload.
  * WHEN a Planner attempts to update a transfer after it is `APPROVED`, `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, or `CANCELLED`, the system SHALL reject the update.
  * WHEN a Planner cancels a transfer in `NEW` status, the system SHALL set status to `CANCELLED`, keep inventory unchanged, and create a `TRANSFER_CANCEL` audit log entry.
* **State-driven:**
  * WHILE a transfer is in `NEW` status, Trưởng kho nguồn is responsible for checking available inventory and approving or rejecting by the shipment feature flow.
  * WHILE a transfer is in `NEW` status, Planner MAY edit header fields, add/update/remove transfer item lines, or cancel the transfer.
  * WHILE a transfer is in any status after `NEW`, Planner SHALL NOT edit transfer header fields or item lines.
  * WHILE a transfer is `REJECTED`, Planner SHALL NOT revise or resubmit the same transfer; Planner MUST create a new transfer if the external instruction still needs to be executed.

## 4. API Endpoints
* `POST /api/v1/transfers` - Planner tạo phiếu điều chuyển nhiều dòng hàng theo lệnh từ Công ty mẹ.
* `GET /api/v1/transfers/{id}` - Tải lại phiếu hiện tại để Planner sửa trên danh sách đã có.
* `PUT /api/v1/transfers/{id}` - Planner lưu lại header và danh sách item sau chỉnh sửa khi phiếu còn `NEW`; server xem payload là trạng thái hiện tại mong muốn của phiếu.
* `PUT /api/v1/transfers/{id}/cancel` - Endpoint hủy dùng chung; Planner chỉ được hủy phiếu `NEW`, còn phiếu `APPROVED` chỉ được hủy bởi Trưởng kho nguồn/manager theo shipment flow.

### Request Payload
```json
{
  "sourceWarehouseId": 1,
  "destinationWarehouseId": 2,
  "plannedDate": "2026-06-20",
  "documentDate": "2026-06-13",
  "externalInstructionCode": "HQ-TRF-20260613-001",
  "notes": "Lenh dieu chuyen tu cong ty me",
  "items": [
    {
      "productId": 101,
      "plannedQty": 50
    },
    {
      "productId": 102,
      "plannedQty": 30
    }
  ]
}
```

### Response Payload
```json
{
  "id": 10,
  "transferNumber": "TRF-20260613-0001",
  "status": "NEW",
  "sourceWarehouseId": 1,
  "destinationWarehouseId": 2,
  "plannedDate": "2026-06-20",
  "documentDate": "2026-06-13",
  "externalInstructionCode": "HQ-TRF-20260613-001",
  "notes": "Lenh dieu chuyen tu cong ty me",
  "items": [
    {
      "id": 1001,
      "productId": 101,
      "plannedQty": 50
    },
    {
      "id": 1002,
      "productId": 102,
      "plannedQty": 30
    }
  ],
  "createdAt": "2026-06-13T10:00:00Z"
}
```

## 5. Validation and Error Handling
* `SAME_WAREHOUSE` (HTTP 422): `sourceWarehouseId = destinationWarehouseId`.
* `TRANSFER_ITEMS_REQUIRED` (HTTP 400): no item lines provided.
* `INVALID_TRANSFER_QTY` (HTTP 400): `plannedQty <= 0`.
* `PRODUCT_INACTIVE` (HTTP 422): product is inactive or unavailable for transaction.
* `WAREHOUSE_INACTIVE` (HTTP 422): source or destination warehouse is inactive.
* `EXTERNAL_INSTRUCTION_CODE_REQUIRED` (HTTP 400): `externalInstructionCode` is blank or missing.
* `DUPLICATE_EXTERNAL_INSTRUCTION` (HTTP 409): another active transfer already uses the same `externalInstructionCode`, source warehouse, destination warehouse, and `documentDate`.
* `ACCOUNTING_PERIOD_CLOSED` (HTTP 409): `documentDate` falls in a closed accounting period.
* `TRANSFER_UPDATE_NOT_ALLOWED` (HTTP 409): transfer is no longer in `NEW` status.
* `TRANSFER_CANCEL_NOT_ALLOWED` (HTTP 409): Planner attempts to cancel a transfer after it is no longer `NEW`.

## 6. Acceptance Criteria
* **Scenario: Create multi-item transfer from external instruction**
  * Given Planner receives an external instruction to move 50 pans and 30 pots from warehouse HP to warehouse HN
  * When Planner submits the transfer form with external instruction code and two item lines
  * Then the system SHALL create one `NEW` transfer with two transfer items, source warehouse HP, destination warehouse HN, planned date, document date, external instruction code, and a `TRANSFER_CREATE` audit log entry.

* **Scenario: Reject missing external instruction code**
  * Given Planner enters source warehouse, destination warehouse, planned date, document date, and item lines
  * When Planner submits the transfer without `externalInstructionCode`
  * Then the system SHALL reject the request with `EXTERNAL_INSTRUCTION_CODE_REQUIRED`.

* **Scenario: Reject duplicate active external instruction**
  * Given an active transfer already exists for external instruction `HQ-TRF-20260613-001`, source warehouse HP, destination warehouse HN, and document date `2026-06-13`
  * When Planner creates another transfer with the same external instruction code, source warehouse, destination warehouse, and document date
  * Then the system SHALL reject the request with `DUPLICATE_EXTERNAL_INSTRUCTION`.

* **Scenario: Reject same source and destination warehouse**
  * Given Planner selects HP as both source and destination warehouse
  * When Planner submits the transfer
  * Then the system SHALL reject the request with `SAME_WAREHOUSE`.

* **Scenario: Reject empty item list**
  * Given Planner enters source warehouse, destination warehouse, and planned date
  * When Planner submits the transfer without item lines
  * Then the system SHALL reject the request with `TRANSFER_ITEMS_REQUIRED`.

* **Scenario: Edit transfer while NEW**
  * Given a transfer is in `NEW` status with two item lines
  * When Planner opens the edit screen, the system loads the current transfer header and item list
  * And Planner changes the quantity of one line and removes the other line
  * Then the system SHALL save the updated transfer, remove the omitted item line, keep status `NEW`, and create an audit entry for the change.

* **Scenario: Cancel transfer while NEW**
  * Given a transfer is in `NEW` status and no inventory has been reserved
  * When Planner cancels the transfer
  * Then the system SHALL set status to `CANCELLED`, create a `TRANSFER_CANCEL` audit entry, and keep inventory unchanged.

* **Scenario: Block edit after approval or rejection**
  * Given a transfer is already `APPROVED` or `REJECTED`
  * When Planner tries to change item quantity or remove an item line
  * Then the system SHALL reject the request with `TRANSFER_UPDATE_NOT_ALLOWED`.

* **Scenario: Recreate instead of resubmitting rejected transfer**
  * Given a transfer was rejected by Trưởng kho nguồn with a rejection reason
  * When Planner needs to continue the external transfer instruction after correction
  * Then Planner SHALL create a new transfer, and the rejected transfer SHALL remain unchanged for audit traceability.

* **Scenario: Planner sees lightweight transfer dashboard**
  * Given Planner opens the shared transfer workspace
  * When transfer records already exist
  * Then the screen SHALL at minimum show transfer code, route, status, and line count so Planner can identify which document to continue or review.
