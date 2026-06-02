# Feature: Quản lý & CEO Phê duyệt Điều chỉnh Chênh lệch Kiểm kê (US-WMS-13)

## 1. Context and Goal
Hệ thống định tuyến phê duyệt chênh lệch kiểm kê (Maker-Checker) dựa trên tổng trị giá lệch. Khi được phê duyệt, hệ thống tự động cập nhật tồn kho về số đếm thực tế, ghi log audit trail và giải phóng vị trí ô kệ.

## 2. Actors
* **Trưởng kho kiêm Trưởng QC**: Duyệt chênh lệch kiểm kê trị giá 5M - 100M VND.
* **CEO**: Duyệt chênh lệch kiểm kê trị giá > 100M VND hoặc do lỗi nhân viên.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a stocktake is submitted for approval, the system SHALL route the approval according to threshold rules (<5M: auto-approve, 5M-100M: Trưởng kho, >100M: CEO).
  * WHEN a stocktake is approved, the system SHALL:
    * Update inventories to match actual counted quantities.
    * Record an `adjustments` entry with type = `'STOCK_TAKE'`.
    * Update status to `APPROVED`.
    * Release the lock on warehouse locations.
* **State-driven:**
  * WHILE inventories optimistic lock version conflict occurs, the system SHALL abort and retry the update.

## 4. API Endpoints
* `PUT /api/v1/stocktakes/{id}/approve` - Phê duyệt điều chỉnh (Trưởng kho hoặc CEO).
* `PUT /api/v1/stocktakes/{id}/reject` - Từ chối điều chỉnh kiểm kê.

## 5. Acceptance Criteria

**Scenario: Stocktake approval routing**
* Given a stocktake variance value is -120M
* When Thủ kho submits the stocktake
* Then the system SHALL route approval request to the CEO.
