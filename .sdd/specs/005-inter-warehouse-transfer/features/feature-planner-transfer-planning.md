# Feature: Planner Nhập Lệnh Điều chuyển kho từ Công ty mẹ (US-WMS-11)

## 1. Context and Goal
Planner nhận chỉ đạo điều chuyển hàng từ Công ty mẹ hoặc bộ phận điều phối trung tâm, sau đó nhập phiếu điều chuyển vào hệ thống WMS để kho nguồn kiểm tra, phê duyệt và thực thi. Công ty mẹ không phải user trong hệ thống ở Sprint 1; mọi lệnh điều chuyển từ Công ty mẹ được nhập trung gian qua Planner. Sprint 1 không có nghiệp vụ kho tự quyết định điều chuyển và không có gợi ý điều chuyển tự động dựa trên tồn kho.

Phiếu điều chuyển có thể gồm nhiều dòng hàng vì lệnh điều chuyển thực tế có thể yêu cầu gửi nhiều SKU trong cùng một chuyến chứng từ, ví dụ 50 cái chảo và 30 nồi từ kho Hải Phòng sang kho Hà Nội. Planner không chọn batch ở bước tạo phiếu; batch được xác định theo FIFO trong bước phê duyệt/soạn/xuất của kho nguồn.

## 2. Actors
* **Planner (Người lập kế hoạch)**: Nhập phiếu điều chuyển theo lệnh từ Công ty mẹ hoặc bộ phận điều phối trung tâm.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL NOT generate transfer suggestions or automatically decide source/destination/quantity for inter-warehouse transfers in Sprint 1.
  * The system SHALL create transfer records only from explicit Planner input based on an external transfer instruction.
  * The system SHALL support multiple transfer item lines in one transfer.
  * The system SHALL store each created transfer with status `NEW`.
  * The system SHALL create a `TRANSFER_CREATE` audit log entry when a Planner creates a transfer.
  * The system SHALL NOT require upload/attachment for the external transfer instruction in Sprint 1.
* **Event-driven:**
  * WHEN a Planner creates a transfer manually, the system SHALL require source warehouse, destination warehouse, planned date, document date, and at least one item line.
  * WHEN a Planner adds an item line, the system SHALL require product and planned quantity.
  * WHEN a Planner submits a transfer where source warehouse equals destination warehouse, the system SHALL reject the request.
  * WHEN a Planner submits a transfer with inactive product, inactive warehouse, zero quantity, or negative quantity, the system SHALL reject the request.
  * WHEN a transfer is created, the system SHALL NOT require Planner to select `batch_id`.
  * WHEN a Planner updates a transfer in `NEW` status, the system SHALL allow editing header fields and adding/updating/removing item lines.
  * WHEN a Planner attempts to update a transfer after it is `APPROVED`, `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, or `CANCELLED`, the system SHALL reject the update.
* **State-driven:**
  * WHILE a transfer is in `NEW` status, Trưởng kho nguồn is responsible for checking available inventory and approving or rejecting by the shipment feature flow.
  * WHILE a transfer is in `NEW` status, Planner MAY edit or remove transfer item lines.
  * WHILE a transfer is in any status after `NEW`, Planner SHALL NOT edit transfer header fields or item lines.
  * WHILE a transfer is `REJECTED`, Planner SHALL NOT revise or resubmit the same transfer; Planner MUST create a new transfer if the external instruction still needs to be executed.

## 4. API Endpoints
* `POST /api/v1/transfers` - Planner tạo phiếu điều chuyển nhiều dòng hàng theo lệnh từ Công ty mẹ.
* `PUT /api/v1/transfers/{id}` - Planner sửa thông tin phiếu hoặc thêm/sửa/xóa dòng hàng khi phiếu còn `NEW`.

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
  "transferNumber": "ITN-20260613-001",
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
* `ACCOUNTING_PERIOD_CLOSED` (HTTP 409): `documentDate` falls in a closed accounting period.
* `TRANSFER_UPDATE_NOT_ALLOWED` (HTTP 409): transfer is no longer in `NEW` status.

## 6. Acceptance Criteria
* **Scenario: Create multi-item transfer from external instruction**
  * Given Planner receives an external instruction to move 50 pans and 30 pots from warehouse HP to warehouse HN
  * When Planner submits the transfer form with two item lines
  * Then the system SHALL create one `NEW` transfer with two transfer items, source warehouse HP, destination warehouse HN, planned date, document date, and a `TRANSFER_CREATE` audit log entry.

* **Scenario: Planner does not select batch**
  * Given Planner creates a transfer for product A with planned quantity 50
  * When the transfer is saved
  * Then the system SHALL create the transfer item without requiring `batch_id`, and batch allocation SHALL be handled later by the approval/shipment flow using FIFO.

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
  * When Planner changes the quantity of one line and removes the other line
  * Then the system SHALL save the updated transfer, keep status `NEW`, and create an audit entry for the change.

* **Scenario: Block edit after approval or rejection**
  * Given a transfer is already `APPROVED` or `REJECTED`
  * When Planner tries to change item quantity or remove an item line
  * Then the system SHALL reject the request with `TRANSFER_UPDATE_NOT_ALLOWED`.

* **Scenario: Recreate instead of resubmitting rejected transfer**
  * Given a transfer was rejected by Trưởng kho nguồn with a rejection reason
  * When Planner needs to continue the external transfer instruction after correction
  * Then Planner SHALL create a new transfer, and the rejected transfer SHALL remain unchanged for audit traceability.
