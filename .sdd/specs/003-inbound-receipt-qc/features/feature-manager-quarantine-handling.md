# Feature: Trưởng kho Xử lý Hàng lỗi trong Quarantine Zone (US-WMS-04)

## 1. Context and Goal
Trưởng kho kiêm Trưởng QC xem xét và quyết định phương án xử lý đối với hàng lỗi đang nằm trong khu vực cách ly (Quarantine Zone): Trả về nhà cung cấp (RTV) kèm đòi tiền (Debit Note) hoặc Xuất tiêu hủy theo định mức phê duyệt.

## 2. Actors
* **Trưởng kho kiêm Trưởng QC**: Đưa ra quyết định và duyệt xuất.
* **CEO**: Duyệt tiêu hủy hàng hỏng nếu tổng giá trị vượt thẩm quyền Trưởng kho (> 100 triệu VNĐ).
* **Kế toán viên**: Nhận thông báo trả hàng NCC, lập Debit Note tương ứng.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Trưởng kho kiêm Trưởng QC selects "Trả NCC" (RTV), the system SHALL:
    * Create a `debit_notes` record for the supplier.
    * Create an `adjustments` record with type `'RETURN_TO_VENDOR'`.
    * Decrease quarantine inventories.
  * WHEN a Trưởng kho kiêm Trưởng QC selects "Tiêu hủy" (Disposal), the system SHALL:
    * Apply approval threshold rules (Value < 5M: auto, 5M-100M: Trưởng kho, > 100M: CEO).
    * Create an `adjustments` record with type `'DISPOSAL'`.
    * Create a `damage_reports` record.
    * Decrease quarantine inventories (upon final approval).
* **State-driven:**
  * WHILE adjustments for disposal or return to vendor is pending approval, the system SHALL NOT deduct quarantine inventories.

## 4. API Endpoints
* `POST /api/v1/receipts/{id}/rtv` - Lập phiếu trả hàng NCC + sinh Debit Note.
* `POST /api/v1/receipts/{id}/dispose` - Lập phiếu xuất hủy hàng hỏng.
* `PUT /api/v1/disposal/{id}/approve` - Phê duyệt tiêu hủy (Trưởng kho hoặc CEO).

## 5. Acceptance Criteria

**Scenario 1: Trưởng kho approval for low value disposal**
* Given 20 units of product X (value 50M) in quarantine
* When Trưởng kho selects "Tiêu hủy" and submits
* Then the system SHALL auto-approve the disposal (within 5-100M threshold) and deduct quarantine stock.

**Scenario 2: CEO approval required for high value disposal**
* Given 100 units of product Y (value 150M) in quarantine
* When Trưởng kho selects "Tiêu hủy" and submits
* Then the system SHALL route the approval request to the CEO and hold stock in quarantine until approved.
