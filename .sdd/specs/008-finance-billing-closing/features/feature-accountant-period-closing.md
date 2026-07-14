# Feature: Kế toán trưởng Chốt sổ Kế toán & Khóa Kỳ (US-WMS-17)

## 1. Context and Goal
Kế toán trưởng (`ACCOUNTANT_MANAGER`) thực hiện chốt sổ kế toán định kỳ hàng tháng để khóa cứng toàn bộ dữ liệu nghiệp vụ lịch sử (nhập, xuất, điều chuyển, điều chỉnh, hóa đơn, thanh toán). Sau khi kỳ kế toán đã khóa, không ai có thể thêm, sửa, hoặc xóa dữ liệu thuộc kỳ đó. Mọi sai sót phát sinh trong kỳ đã khóa chỉ được điều chỉnh bằng các chứng từ điều chỉnh ngược (Correction Voucher) trong kỳ kế toán hiện hành đang mở.

## 2. Actors
* **Kế toán trưởng (`ACCOUNTANT_MANAGER`)**: Người duy nhất có thẩm quyền thực hiện chốt sổ và khóa kỳ kế toán.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * **WHEN** Kế toán trưởng yêu cầu khóa kỳ kế toán, hệ thống **SHALL** tự động kiểm tra tính toàn vẹn của dữ liệu trong kỳ đó:
    * Kiểm tra xem còn chứng từ dở dang nào có ngày chứng từ `document_date` nằm trong kỳ kế toán đó không:
      * Đơn nhập kho (`receipts` có trạng thái khác `'APPROVED'` và `'REJECTED'`).
      * Đơn xuất kho (`delivery_orders` có trạng thái khác `'COMPLETED'`, `'CANCELLED'` và `'RETURNED'`).
      * Đơn điều chuyển (`transfers` có trạng thái khác `'COMPLETED'`, `'COMPLETED_WITH_DISCREPANCY'` và `'CANCELLED'`).
      * Phiếu kiểm kê (`stock_takes` có trạng thái khác `'APPROVED'` và `'CANCELLED'`).
      * Phiếu điều chỉnh (`adjustments` có trạng thái khác `'APPROVED'`).
      * Hóa đơn (`invoices` ở trạng thái khác `'PAID'` - tùy cấu hình, hoặc chỉ chặn nếu có hóa đơn dở dang chưa tạo).
    * **IF** phát hiện còn chứng từ dở dang: Hệ thống **SHALL** từ chối khóa kỳ và trả về danh sách chi tiết các chứng từ chưa hoàn tất.
    * **IF** kiểm tra hợp lệ: Cập nhật trạng thái kỳ kế toán sang `status = 'CLOSED'`, ghi nhận người khóa `closed_by` và thời gian khóa `closed_at`.
  * **WHEN** ngày hiện tại đạt hoặc vượt quá ngày khóa sổ hàng tháng `MONTHLY_CLOSING_DAY` (lấy từ cấu hình bảng `system_configs`, mặc định là ngày `25`), hệ thống **SHALL** gửi thông báo nhắc nhở Kế toán trưởng tiến hành rà soát để đóng kỳ kế toán.
* **State-driven:**
  * **WHILE** một kỳ kế toán có trạng thái `status = 'CLOSED'`, hệ thống **SHALL** chặn tất cả các yêu cầu tạo mới (CREATE), chỉnh sửa (UPDATE) hoặc xóa (DELETE) đối với mọi chứng từ (bao gồm `receipts`, `delivery_orders`, `transfers`, `stock_takes`, `adjustments`, `invoices`, `payment_receipts`) có ngày chứng từ `document_date` thuộc phạm vi từ `start_date` đến `end_date` của kỳ đã đóng đó (trả về lỗi `PERIOD_CLOSED` với HTTP 422).

## 4. API Endpoints

### 4.1 Lấy danh sách kỳ kế toán
* **Protocol & Path**: `GET /api/v1/accounting-periods`
* **Response 200 OK**:
  ```json
  [
    {
      "id": 1,
      "periodName": "2026-05",
      "startDate": "2026-05-01",
      "endDate": "2026-05-31",
      "status": "CLOSED",
      "closedBy": 6,
      "closedAt": "2026-06-01T17:00:00Z",
      "notes": "Chốt sổ tháng 5 thành công"
    },
    {
      "id": 2,
      "periodName": "2026-06",
      "startDate": "2026-06-01",
      "endDate": "2026-06-30",
      "status": "OPEN",
      "closedBy": null,
      "closedAt": null,
      "notes": null
    }
  ]
  ```

### 4.2 Khóa kỳ kế toán
* **Protocol & Path**: `PUT /api/v1/accounting-periods/{id}/close`
* **Request Body**:
  ```json
  {
    "notes": "Chốt sổ kỳ kế toán tháng 6"
  }
  ```
* **Response 200 OK**:
  ```json
  {
    "id": 2,
    "periodName": "2026-06",
    "startDate": "2026-06-01",
    "endDate": "2026-06-30",
    "status": "CLOSED",
    "closedBy": 6,
    "closedAt": "2026-06-17T01:15:00Z",
    "notes": "Chốt sổ kỳ kế toán tháng 6"
  }
  ```
* **Error Response 422 Unprocessable Entity** (Nếu còn chứng từ dở dang):
  ```json
  {
    "code": "PENDING_DOCUMENTS_EXIST",
    "message": "Không thể đóng kỳ kế toán vì còn chứng từ dở dang chưa xử lý xong.",
    "details": [
      {
        "type": "DELIVERY_ORDER",
        "documentNumber": "DO-20260612-001",
        "status": "NEW"
      }
    ]
  }
  ```

## 5. Acceptance Criteria

* **Scenario: Chặn sửa đổi dữ liệu trong kỳ đã chốt sổ**
  * **Given**: Kỳ kế toán `2026-05` (từ `2026-05-01` đến `2026-05-31`) có trạng thái `status = 'CLOSED'`.
  * **When**: Kế toán viên cố gắng tạo mới hoặc cập nhật một đơn xuất kho có ngày chứng từ `document_date = '2026-05-15'`.
  * **Then**: Hệ thống từ chối yêu cầu, trả về mã lỗi `PERIOD_CLOSED` (HTTP 422) và không ghi nhận bất kỳ thay đổi nào trong cơ sở dữ liệu.
