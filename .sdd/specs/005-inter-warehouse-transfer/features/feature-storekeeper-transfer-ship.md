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
  * WHEN a Trưởng kho nguồn approves a transfer (status → `APPROVED`), the system SHALL:
    * Verify: `available_qty = total_qty - reserved_qty ≥ planned_qty` at the source warehouse.
    * If sufficient, increase source inventories: `reserved_qty += planned_qty`.
  * WHEN a Thủ kho nguồn confirms shipment (status → `IN_TRANSIT`), the system SHALL:
    * Decrease source warehouse inventories: `total_qty -= sent_qty`.
    * Decrease source warehouse reserved quantity: `reserved_qty -= planned_qty`.
    * Increase In-Transit virtual warehouse inventories: `total_qty += sent_qty`.
  * WHEN a transfer is cancelled, the system SHALL:
    * IF the status was `APPROVED`, decrease source warehouse reserved quantity: `reserved_qty -= planned_qty`.

## 4. API Endpoints
* `PUT /api/v1/transfers/{id}/approve` - Duyệt điều chuyển và giữ chỗ hàng (Trưởng kho nguồn).
* `PUT /api/v1/transfers/{id}/ship` - Xác nhận xuất hàng lên xe, giải phóng giữ chỗ và chuyển sang In-Transit (Thủ kho nguồn).
* `PUT /api/v1/transfers/{id}/cancel` - Hủy phiếu điều chuyển, giải phóng giữ chỗ (Planner / Trưởng kho).

## 5. Acceptance Criteria
* **Scenario: Transfer approval reserves stock**
  * Given source warehouse HP has 50 units of product X with `reserved_qty = 0` (available = 50)
  * When Planner creates a transfer of 30 units and Trưởng kho HP approves it
  * Then source inventory HP SHALL show `total_qty = 50`, `reserved_qty = 30`, and `available_qty = 20`.

* **Scenario: Transfer shipment releases reservation and moves to In-Transit**
  * Given source warehouse HP has 50 units of product X with `reserved_qty = 30` (from an approved transfer of 30 units)
  * When Thủ kho HP confirms shipment of 30 units
  * Then:
    * Source inventory HP SHALL show `total_qty = 20` and `reserved_qty = 0`.
    * Virtual In-Transit inventory SHALL show `total_qty = 30`.
