# Feature: Thủ kho & Kế toán Xử lý Hàng hoàn trả từ Đại lý (US-WMS-24)

## 1. Context and Goal

Đại lý thực hiện hoàn trả hàng (lỗi hoặc thừa). Thủ kho (STOREKEEPER) lập phiếu nhận hàng hoàn (`receipts` type = `'RETURN'`) từ Delivery Order (DO) gốc và thực hiện kiểm đếm thực tế kèm kiểm QC (Split QC). Trưởng kho phê duyệt phiếu nhận, sau đó Thủ kho putaway hàng Đạt vào kho thường và hàng Lỗi vào Quarantine Zone. Cuối cùng, Kế toán lập Credit Note dựa trên phiếu hoàn trả để giảm dư nợ của Đại lý.

## 2. Actors

* **Thủ kho kiêm QC (STOREKEEPER)**: Lập phiếu nhận hàng hoàn, thực hiện kiểm QC mẫu (Split QC) và cất hàng vào vị trí (Putaway).
* **Trưởng kho (WAREHOUSE_MANAGER)**: Phê duyệt phiếu nhận hàng hoàn (`APPROVED`).
* **Kế toán viên (ACCOUNTANT)**: Lập Credit Note để cấn trừ giảm công nợ cho Đại lý.
* **Nhân viên kho (WAREHOUSE_STAFF)**: Hỗ trợ bốc xếp, di chuyển hàng hoàn theo chỉ dẫn của Thủ kho.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  - The system SHALL always route returned goods through QC before admitting them back into available or quarantine inventories.
  - The system SHALL require `expectedVersion` (optimistic locking) on all write requests (QC, approve, complete putaway, credit-note).

* **Event-driven:**
  - WHEN a Storekeeper creates a Return Receipt from a dealer, the system SHALL:
    - Create a `receipts` record with `type = 'RETURN'` and status `DRAFT`.
    - Require fields: `dealer_id`, `warehouse_id`, `contact_person`, and `delivery_order_id` (original DO).
  - WHEN Storekeeper performs count and split QC:
    - The system SHALL validate that for each product, the actual returned quantity (`actual_qty`) is less than or equal to the original sold quantity (`issued_qty`) in the referenced DO. If it exceeds, reject with HTTP 422 `RETURN_EXCEEDS_ORIGINAL_SALE`.
    - Require `qc_passed_qty + qc_failed_qty == actual_qty`.
    - Record `qc_passed_qty`, `qc_failed_qty`, `qc_result`, and `qc_failure_reason` on `receipt_items`.
    - Set the receipt status to `QC_COMPLETED`.
  - WHEN Warehouse Manager approves the return receipt:
    - The system SHALL update status to `APPROVED`.
    - Resolve or create a batch for the return (using `received_date` equal to the return's `document_date` for FIFO).
  - WHEN Storekeeper completes putaway for the approved return receipt, the system SHALL:
    - Putaway passed units (`qc_passed_qty`) to the specified regular bin (`is_quarantine = false`) and increase available inventory.
    - Putaway failed units (`qc_failed_qty`) to the specified quarantine bin (`is_quarantine = true`) and increase quarantine inventory.
    - Set the receipt status to `APPROVED` (or appropriate status representing putaway completion).
  - WHEN Accountant creates a Credit Note for returned goods, the system SHALL:
    - Verify the return receipt status is `APPROVED` (or completed putaway).
    - Calculate the credit note amount as: `Sum of (actual_qty * original_do_item.unit_price)`.
    - Create a `credit_notes` record referencing the receipt.
    - Decrease dealer's `current_balance` by the Credit Note amount: `current_balance = current_balance - amount`.

* **State-driven:**
  - WHILE a return receipt is not `APPROVED`, the system SHALL reject putaway operations and Credit Note creation attempts.
  - WHILE an accounting period for the return's document date is closed (`accounting_periods.status = 'CLOSED'`), the system SHALL reject return creation and Credit Note generation with HTTP 422 `PERIOD_CLOSED`.

## 4. API Endpoints

### POST `/api/v1/returns`
* **Role**: `STOREKEEPER`
* **Description**: Lập nháp phiếu nhận hàng hoàn từ DO gốc.
* **Request Body**:
```json
{
  "warehouseId": 1,
  "dealerId": 2,
  "deliveryOrderId": 10,
  "contactPerson": "Nguyễn Văn A",
  "documentDate": "2026-06-17",
  "notes": "Đại lý trả hàng do thừa",
  "items": [
    {
      "productId": 5,
      "expectedQty": 10.00
    }
  ]
}
```
* **Response (201 Created)**:
```json
{
  "id": 101,
  "receiptNumber": "RET-20260617-A01B2C",
  "status": "DRAFT",
  "version": 0
}
```

### PUT `/api/v1/returns/{id}/qc`
* **Role**: `STOREKEEPER`
* **Description**: Nhập kết quả kiểm đếm thực tế và phân loại QC hàng hoàn (Split QC).
* **Request Body**:
```json
{
  "expectedVersion": 0,
  "items": [
    {
      "receiptItemId": 201,
      "actualQty": 10.00,
      "qcPassedQty": 8.00,
      "qcFailedQty": 2.00,
      "qcFailureReason": "2 cái trầy xước nặng"
    }
  ]
}
```
* **Response (200 OK)**:
```json
{
  "id": 101,
  "status": "QC_COMPLETED",
  "version": 1
}
```

### PUT `/api/v1/returns/{id}/approve`
* **Role**: `WAREHOUSE_MANAGER`
* **Description**: Trưởng kho phê duyệt phiếu nhận hàng hoàn đã hoàn thành QC.
* **Request Body**:
```json
{
  "expectedVersion": 1
}
```
* **Response (200 OK)**:
```json
{
  "id": 101,
  "status": "APPROVED",
  "version": 2
}
```

### PUT `/api/v1/returns/{id}/complete`
* **Role**: `STOREKEEPER`
* **Description**: Xác nhận putaway hàng Đạt vào vị trí thường và hàng Lỗi vào vị trí Quarantine.
* **Request Body**:
```json
{
  "expectedVersion": 2,
  "putawayItems": [
    {
      "receiptItemId": 201,
      "passedLocationId": 50,  // regular bin
      "failedLocationId": 60   // quarantine bin
    }
  ]
}
```
* **Response (200 OK)**:
```json
{
  "id": 101,
  "message": "Putaway completed. Regular and quarantine inventories updated.",
  "version": 3
}
```

### POST `/api/v1/returns/{id}/credit-note`
* **Role**: `ACCOUNTANT`
* **Description**: Lập Credit Note ghi giảm công nợ cho Đại lý dựa trên phiếu trả hàng hoàn đã duyệt/putaway.
* **Request Body**:
```json
{
  "reason": "Hoàn tiền hàng trả RET-20260617-A01B2C",
  "documentDate": "2026-06-17"
}
```
* **Response (201 Created)**:
```json
{
  "creditNoteId": 5,
  "creditNoteNumber": "CN-20260617-0012A",
  "amount": 25000000.00,  // 10 cái x đơn giá gốc 2.5M
  "dealerId": 2,
  "message": "Credit Note generated. Dealer balance reduced by 25,000,000.00 VND."
}
```

## 5. Acceptance Criteria

**Scenario: Enforce return quantity limits against original DO**
* Given a DO has product X with `issued_qty = 10`
* When Storekeeper creates return and enters `actualQty = 12`
* Then the system SHALL reject the QC submission with HTTP 422 `RETURN_EXCEEDS_ORIGINAL_SALE`.

**Scenario: Split QC on returned goods**
* Given a return receipt with 10 expected units
* When Storekeeper enters `actualQty = 10`, `qcPassedQty = 8`, and `qcFailedQty = 2`
* Then the system SHALL accept the QC, set status to `QC_COMPLETED`.

**Scenario: Putaway splits stock correctly**
* Given an approved return receipt with `qcPassedQty = 8` and `qcFailedQty = 2`
* When Storekeeper confirms putaway to regular bin 50 and quarantine bin 60
* Then the system SHALL increase regular inventory at location 50 by 8
* And increase quarantine inventory at location 60 by 2.

**Scenario: Accountant creates Credit Note and cấn trừ công nợ**
* Given a dealer has `current_balance = 100M` (outstanding debt)
* And an approved return receipt is completed with total value of 25M
* When Accountant requests to create a Credit Note referencing the return receipt
* Then the system SHALL create the `credit_notes` record with amount 25M
* And reduce the dealer's `current_balance` to 75M.
