# Feature: Trưởng kho Phê duyệt Điều chỉnh Chênh lệch Kiểm kê (US-WMS-13)

## 1. Context and Goal
Hệ thống gửi yêu cầu phê duyệt chênh lệch kiểm kê (Maker-Checker) cho Trưởng kho. Khi được phê duyệt, hệ thống tự động cập nhật tồn kho về số đếm thực tế, ghi log audit trail và giải phóng vị trí ô kệ.

## 2. Actors
* **Trưởng kho kiêm Trưởng QC**: Duyệt chênh lệch kiểm kê và phê duyệt điều chỉnh tồn kho.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a stocktake is submitted for approval, the system SHALL route the approval to the Trưởng kho.
  * WHEN a stocktake is approved, the system SHALL:
    * Update inventories to match actual counted quantities.
    * Record an `adjustments` entry with type = `'STOCK_TAKE'`.
    * Update status to `APPROVED`.
    * Release the lock on warehouse locations.
* **State-driven:**
  * WHILE inventories optimistic lock version conflict occurs, the system SHALL abort and retry the update.

## 4. API Endpoints
* `PUT /api/v1/stocktakes/{id}/approve` - Phê duyệt điều chỉnh (Trưởng kho).
* `PUT /api/v1/stocktakes/{id}/reject` - Từ chối điều chỉnh kiểm kê.

## 5. Acceptance Criteria

**Scenario: Stocktake approval by Trưởng kho**
* Given a stocktake variance value is -120M
* When Thủ kho submits the stocktake
* And Trưởng kho approves the stocktake
* Then the system SHALL update inventories to match actual counted quantities, record an `adjustments` entry, set status to `APPROVED`, and release the location lock.
