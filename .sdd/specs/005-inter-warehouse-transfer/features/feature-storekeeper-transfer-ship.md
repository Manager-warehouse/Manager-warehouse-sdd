# Feature: Thủ kho Nguồn Soạn & Xuất hàng Điều chuyển (US-WMS-12)

## 1. Context and Goal
Thủ kho tại kho nguồn chịu trách nhiệm soạn hàng theo phiếu điều chuyển đã được phê duyệt, bốc xếp lên xe tải nội bộ và xác nhận xuất hàng để chuyển tồn kho sang trạng thái In-Transit ảo.

## 2. Actors
* **Thủ kho (Kho nguồn)**: Soạn hàng, xác nhận xuất hàng lên xe tải.
* **Trưởng kho nguồn**: Duyệt phiếu điều chuyển đi.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always route all inter-warehouse transfers through a virtual In-Transit warehouse for tracking.
  * The system SHALL always enforce `source_warehouse_id ≠ destination_warehouse_id`.
* **Event-driven:**
  * WHEN a Trưởng kho nguồn approves a transfer, the system SHALL verify available inventory at the source before approval.
  * WHEN a Thủ kho nguồn confirms shipment (status → `IN_TRANSIT`), the system SHALL:
    * Decrease source warehouse inventories: `total_qty -= sent_qty`.
    * Increase In-Transit virtual warehouse inventories: `total_qty += sent_qty`.

## 4. API Endpoints
* `PUT /api/v1/transfers/{id}/approve` - Duyệt điều chuyển (Trưởng kho nguồn).
* `PUT /api/v1/transfers/{id}/ship` - Xác nhận xuất hàng lên xe (Thủ kho nguồn).

## 5. Acceptance Criteria
* **Scenario: Transfer stock to virtual In-Transit**
  * Given source warehouse HP has 50 units of product X
  * When Planner creates a transfer of 30 units and Thủ kho HP confirms shipment
  * Then source inventory HP SHALL show `total_qty = 20` and virtual In-Transit inventory SHALL show `total_qty = 30`.
