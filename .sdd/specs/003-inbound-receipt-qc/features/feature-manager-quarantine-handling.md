# Feature: Trưởng kho Xử lý Hàng lỗi trong Quarantine Zone (US-WMS-04)

## 1. Context and Goal
Trưởng kho xem xét và quyết định phương án xử lý đối với hàng lỗi đang nằm trong khu vực cách ly (Quarantine Zone): Trả về nhà cung cấp (RTV) kèm đòi tiền (Debit Note) hoặc Xuất tiêu hủy theo định mức phê duyệt.

## 2. Actors
<<<<<<< HEAD
* **Trưởng kho kiêm Trưởng QC**: Đưa ra quyết định và duyệt xuất.
=======
* **Trưởng kho**: Đưa ra quyết định và duyệt xuất.
* **CEO**: Duyệt tiêu hủy hàng hỏng nếu tổng giá trị vượt thẩm quyền Trưởng kho (> 100 triệu VNĐ).
>>>>>>> 78bb76f (update spec master data and database, entity)
* **Kế toán viên**: Nhận thông báo trả hàng NCC, lập Debit Note tương ứng.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Trưởng kho selects "Trả NCC" (RTV), the system SHALL:
    * Create a `debit_notes` record for the supplier.
    * Create an `adjustments` record with type `'RETURN_TO_VENDOR'`.
    * Decrease quarantine inventories.
<<<<<<< HEAD
  * WHEN a Trưởng kho kiêm Trưởng QC selects "Tiêu hủy" (Disposal), the system SHALL:
    * Send for Trưởng kho approval.
=======
  * WHEN a Trưởng kho selects "Tiêu hủy" (Disposal), the system SHALL:
    * Apply approval threshold rules (Value < 5M: auto, 5M-100M: Trưởng kho, > 100M: CEO).
>>>>>>> 78bb76f (update spec master data and database, entity)
    * Create an `adjustments` record with type `'DISPOSAL'`.
    * Create a `damage_reports` record.
    * Decrease quarantine inventories (upon final approval).
* **State-driven:**
  * WHILE adjustments for disposal or return to vendor is pending approval, the system SHALL NOT deduct quarantine inventories.

## 4. API Endpoints
* `POST /api/v1/receipts/{id}/rtv` - Lập phiếu trả hàng NCC + sinh Debit Note.
* `POST /api/v1/receipts/{id}/dispose` - Lập phiếu xuất hủy hàng hỏng.
* `PUT /api/v1/disposal/{id}/approve` - Phê duyệt tiêu hủy (Trưởng kho).

## 5. Acceptance Criteria

**Scenario: Trưởng kho approves disposal**
* Given 20 units of product X (value 50M) in quarantine
* When Trưởng kho selects "Tiêu hủy" and approves
* Then the system SHALL approve the disposal and deduct quarantine stock.
