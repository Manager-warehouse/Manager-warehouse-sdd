# Feature: Trưởng kho Xem Báo cáo Năng suất & Sản lượng Nhân viên Kho (US-WMS-25)

## 1. Context and Goal
Trưởng kho cần xem sản lượng bốc xếp/di chuyển hàng của Nhân viên kho, sản lượng soạn hàng và kiểm QC của Thủ kho, và giao hàng của Tài xế để làm căn cứ gửi bộ phận nhân sự (HRM) bên ngoài tính lương theo sản phẩm.

## 2. Actors
* **Trưởng kho**: Trích xuất báo cáo năng suất và xem chi tiết sản lượng của nhân viên kho.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Trưởng kho requests a productivity report, the system SHALL export an Excel file with:
    * Number of orders processed per warehouse employee (bốc xếp/di chuyển hàng).
    * QC throughput (units checked passed/failed) per Thủ kho.
    * Trips completed and deliveries made per driver.

## 4. API Endpoints
* `GET /api/v1/reports/productivity` - Xem và xuất báo cáo năng suất nhân viên kho (Excel).

## 5. Acceptance Criteria
* **Scenario: Export productivity report**
  * Given a list of transactions processed by warehouse staff in May
  * When Trưởng kho HN requests the productivity report for May
  * Then the system SHALL compile and export an Excel sheet showing correct counts of operations per employee.
