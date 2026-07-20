# Feature: Trưởng kho Duyệt Tiêu hủy Hàng lỗi từ Quarantine (US-WMS-04 - Disposal Sub-flow)

## 1. Context and Goal

Hàng hóa hư hỏng hoặc fail QC tích tụ trong khu cách ly (Quarantine Zone) được Trưởng kho đề xuất xuất tiêu hủy, lập Biên bản hư hỏng (Damage Report) và duyệt xuất trực tiếp — không phân cấp theo giá trị. Domain hiện tại không quản lý hàng hết hạn sử dụng.

RTV (trả hàng nhà cung cấp) cho hàng lỗi nguồn gốc nhập kho (`origin = RECEIPT_QC_FAIL`) được xử lý hoàn toàn tại Spec 003 (`QuarantineRtvService`); Spec 009 KHÔNG lặp lại luồng RTV.

Hàng có nguồn từ điều chuyển nội bộ đã được đưa vào Quarantine sẽ được xử lý bằng luồng tiêu hủy hiện có của Spec 009 và không được trả nhà cung cấp (RTV).

## 2. Actors

- **Trưởng kho**: Đề xuất và phê duyệt tiêu hủy, không phân cấp theo giá trị.

## 3. Functional Requirements (EARS)

- **Event-driven:**
  - WHEN a Disposal document is created for quarantine goods, the system SHALL:
    - Create an `adjustments` record with type = `'DISPOSAL'` and create a `damage_reports` record.
    - Send for Trưởng kho approval.
    - Once approved, decrease quarantine inventories: `total_qty -= quantity`.
    - Record an audit log entry with destruction reason, cost value, and approving authority.
  - WHEN quarantine goods originate from an internal transfer, the system SHALL allow only the existing `DISPOSAL` flow and SHALL NOT allow `RETURN_TO_VENDOR` or create a supplier Debit Note.
  - WHEN a disposal document is created, updated, or approved, the system SHALL verify the acting user is assigned to the disposal's `warehouse_id`, in addition to the role check.
- **State-driven:**
  - WHILE adjustments for disposal is pending approval, the system SHALL NOT deduct quarantine inventories.

## 4. API Endpoints

- `POST /api/v1/disposal` - Tạo biên bản đề xuất hủy hàng lỗi (WAREHOUSE_MANAGER).
- `PUT /api/v1/disposal/{id}/approve` - Phê duyệt tiêu hủy (Trưởng kho).

## 5. Acceptance Criteria

- **Scenario: Approve and execute disposal**
  - Given 10 units of damaged/QC-failed product X (value 8M) are in quarantine
  - When Trưởng kho submits and approves a disposal request
  - Then the system SHALL write a `DISPOSAL` adjustment of -10 units, create a `damage_reports` entry, decrease quarantine inventory by 10, and write an audit log.
- **Scenario: Dispose quarantined goods from an internal transfer**
  - Given physically damaged goods originating from an internal transfer are already in Quarantine
  - When Trưởng kho processes those goods
  - Then the system SHALL use the existing `DISPOSAL` flow and SHALL NOT offer `RETURN_TO_VENDOR`.
