# Feature: Trưởng kho Duyệt Tiêu hủy Hàng lỗi từ Quarantine (US-WMS-04 - Disposal Sub-flow)

## 1. Context and Goal
Hàng hóa hư hỏng hoặc fail QC tích tụ trong khu cách ly (Quarantine Zone) được Trưởng kho đề xuất xuất tiêu hủy, lập Biên bản hư hỏng (Damage Report) và duyệt xuất theo hạn mức thẩm quyền. Domain hiện tại không quản lý hàng hết hạn sử dụng.

## 2. Actors
* **Trưởng kho**: Đề xuất và duyệt tiêu hủy dưới 100M VND.
* **CEO**: Duyệt tiêu hủy trên 100M VND.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Disposal document is created for quarantine goods, the system SHALL:
    * Create an `adjustments` record with type = `'DISPOSAL'` and create a `damage_reports` record.
    * Send for Trưởng kho approval.
    * Once approved, decrease quarantine inventories: `total_qty -= quantity`.
    * Record an audit log entry with destruction reason, cost value, and approving authority.
  * WHEN Trưởng kho selects Return to Vendor (RTV) for quarantine goods:
    * Create a `debit_notes` record for the supplier.
    * Create adjustments record with type = `'RETURN_TO_VENDOR'`.
    * Decrease quarantine inventories: `total_qty -= quantity`.
* **State-driven:**
  * WHILE adjustments for disposal or return to vendor is pending approval, the system SHALL NOT deduct quarantine inventories.

## 4. API Endpoints
* `POST /api/v1/disposal` - Tạo biên bản đề xuất hủy hàng lỗi (WAREHOUSE_MANAGER).
* `PUT /api/v1/disposal/{id}/approve` - Phê duyệt tiêu hủy (Trưởng kho).

## 5. Acceptance Criteria
* **Scenario: Approve and execute disposal**
  * Given 10 units of expired product X (value 8M) are in quarantine
  * When Trưởng kho submits and approves a disposal request
  * Then the system SHALL write a `DISPOSAL` adjustment of -10 units, create a `damage_reports` entry, decrease quarantine inventory by 10, and write an audit log.
