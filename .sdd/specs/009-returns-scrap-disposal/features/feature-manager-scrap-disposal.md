# Feature: Trưởng kho & CEO Duyệt Tiêu hủy Hàng lỗi từ Quarantine (US-WMS-04 - Disposal Sub-flow)

## 1. Context and Goal

Hàng hóa hư hỏng hoặc lỗi QC được cách ly trong Quarantine Zone (đã được xác nhận nhập vào Quarantine inventory tại Spec 003) cần được xử lý tiêu hủy khi không thể sử dụng hay trả lại NCC. Trưởng kho (WAREHOUSE_MANAGER) sẽ lập đề xuất tiêu hủy, lập Biên bản hư hỏng (Damage Report) và duyệt xuất theo hạn mức thẩm quyền. Các đề xuất vượt hạn mức sẽ do CEO phê duyệt.

## 2. Actors

* **Trưởng kho (WAREHOUSE_MANAGER)**: Maker (Tạo đề xuất tiêu hủy hàng lỗi từ Quarantine) và Checker (Phê duyệt các đề xuất tiêu hủy có giá trị ≤ 100,000,000 VND).
* **CEO (CEO)**: Checker (Phê duyệt các đề xuất tiêu hủy có giá trị > 100,000,000 VND).
* **Nhân viên kho (WAREHOUSE_STAFF)**: Thực hiện tiêu hủy vật lý sau khi đề xuất được duyệt.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  - The system SHALL record `QUARANTINE_DISPOSAL_CREATE` and `QUARANTINE_DISPOSAL_APPROVE` audit log entries.
  - The system SHALL reject disposal approval if the quantity to deduct exceeds the available quantity in the Quarantine inventory.
  - The system SHALL require `expectedVersion` (optimistic locking) on the approval endpoint to prevent concurrent updates.

* **Event-driven:**
  - WHEN a Warehouse Manager creates a disposal proposal, the system SHALL:
    - Create a `damage_reports` record storing warehouse, product, batch, quantity, cause, and image URL.
    - Create an `adjustments` record with:
      - `type = 'DISPOSAL'`
      - `quantity_adjustment = -quantity` (negative value)
      - `reference_id = damage_report.id`
      - `reference_type = 'DAMAGE_REPORT'`
      - `approved_by = NULL` and `approved_at = NULL`
    - Keep quarantine inventory unchanged (do not deduct inventory yet).
  - WHEN an authorized actor (Warehouse Manager for values ≤ 100M VND, or CEO for values > 100M VND) approves the disposal, the system SHALL:
    - Set `approved_by` and `approved_at` on the `adjustments` record.
    - Deduct the quantity from the quarantine inventory (`inventories.total_qty = total_qty - quantity`).
    - Create an `INVENTORY_UPDATE` audit log.

* **State-driven:**
  - WHILE a disposal proposal is pending approval, the system SHALL NOT decrease quarantine inventories.
  - WHILE the total value of the disposal (calculated as `quantity * cost_price` from the quarantine inventory record) is greater than 100,000,000 VND, the system SHALL reject approval attempts by WAREHOUSE_MANAGER with HTTP 403.

## 4. API Endpoints

### POST `/api/v1/disposal`
* **Role**: `WAREHOUSE_MANAGER`
* **Description**: Tạo biên bản hư hỏng (Damage Report) và lập đề xuất tiêu hủy (Adjustment).
* **Request Body**:
```json
{
  "warehouseId": 1,
  "productId": 2,
  "batchId": 3,
  "locationId": 4,
  "quantity": 10.00,
  "cause": "Hàng bị bể vỡ do va đập trong kho",
  "imageUrl": "http://cloud-storage.com/disposal-images/img_123.jpg",
  "documentDate": "2026-06-17"
}
```
* **Response (201 Created)**:
```json
{
  "adjustmentId": 15,
  "adjustmentNumber": "ADJ-20260617-X9A21B",
  "damageReportId": 5,
  "damageReportNumber": "DR-20260617-H892A",
  "totalValueEstimate": 8000000.00,
  "confirmed": false,
  "message": "Disposal proposal created successfully. Awaiting approval."
}
```

### PUT `/api/v1/disposal/{id}/approve`
* **Role**: `WAREHOUSE_MANAGER` (for value ≤ 100M), `CEO` (for value > 100M)
* **Description**: Phê duyệt tiêu hủy và giảm trừ tồn kho Quarantine.
* **Request Body**:
```json
{
  "expectedVersion": 0
}
```
* **Response (200 OK)**:
```json
{
  "adjustmentId": 15,
  "adjustmentNumber": "ADJ-20260617-X9A21B",
  "confirmed": true,
  "approvedBy": 3,
  "approvedAt": "2026-06-17T08:15:30Z",
  "deductedQty": 10.00,
  "message": "Disposal approved. Quarantine stock deducted."
}
```

## 5. Acceptance Criteria

**Scenario: Propose disposal for quarantine goods**
* Given 10 units of product X (value 8M) are in quarantine inventory at location A (`is_quarantine = true`)
* When Warehouse Manager submits a disposal proposal with quantity = 10, cause = "Broken"
* Then the system SHALL create a `damage_reports` entry and a pending `adjustments` record with type `'DISPOSAL'` and quantity = `-10.00`
* And the quarantine inventory SHALL remain 10 units.

**Scenario: Warehouse Manager approves a disposal under 100M VND limit**
* Given a pending `DISPOSAL` adjustment for 10 units (value 8M, which is ≤ 100M VND)
* When Warehouse Manager approves the disposal
* Then the system SHALL approve the adjustment, deduct 10 units from quarantine inventory (new total_qty = 0), and write `QUARANTINE_DISPOSAL_APPROVE` audit log.

**Scenario: Block Warehouse Manager from approving disposal over 100M VND limit**
* Given a pending `DISPOSAL` adjustment for 150 units (value 120M, which is > 100M VND)
* When Warehouse Manager attempts to approve the disposal
* Then the system SHALL reject the request with HTTP 403 Forbidden.

**Scenario: CEO approves a disposal over 100M VND limit**
* Given a pending `DISPOSAL` adjustment for 150 units (value 120M, which is > 100M VND)
* When CEO approves the disposal
* Then the system SHALL approve the adjustment and deduct 150 units from quarantine inventory.

**Scenario: Block disposal when quantity exceeds quarantine stock**
* Given 10 units of product X are in quarantine inventory
* When Warehouse Manager proposes a disposal with quantity = 12
* Then the system SHALL reject the request with HTTP 422 Unprocessable Entity.
