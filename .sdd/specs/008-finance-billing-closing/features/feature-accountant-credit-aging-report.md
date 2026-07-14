# Feature: Kế toán trưởng Xem Báo cáo Công nợ Phân kỳ (US-WMS-16)

## 1. Context and Goal
Kế toán trưởng (`ACCOUNTANT_MANAGER`) cần theo dõi và đánh giá rủi ro tín dụng của toàn bộ hệ thống đại lý. Tính năng báo cáo công nợ phân kỳ (Aging Report) thực hiện phân nhóm các khoản nợ của đại lý theo thời gian quá hạn thực tế so với hạn thanh toán trên hóa đơn. Báo cáo được phân chia thành các mốc: nợ trong hạn, quá hạn 1-30 ngày, quá hạn 31-60 ngày, quá hạn 61-90 ngày, và quá hạn >90 ngày.

## 2. Actors
* **Kế toán trưởng (`ACCOUNTANT_MANAGER`)**: Quyền xem và xuất dữ liệu báo cáo phân kỳ công nợ để ra quyết định điều chỉnh hạn mức tín dụng.
* **Kế toán viên (`ACCOUNTANT`)**: Quyền xem báo cáo để phục vụ việc đốc thúc thu hồi công nợ đại lý.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * Hệ thống luôn tự động tính toán thời gian quá hạn của mỗi hóa đơn `invoices` chưa thanh toán (`status IN ('UNPAID', 'PARTIALLY_PAID')`) dựa trên chênh lệch giữa ngày hiện tại và ngày đến hạn `due_date`.
* **Optional:**
  * **WHERE** đại lý có bất kỳ hóa đơn nào quá hạn quá mốc quy định (ví dụ >60 ngày), hệ thống **SHALL** gắn nhãn cảnh báo rủi ro cao (`High Risk`) đối với đại lý đó trên báo cáo công nợ.

## 4. API Endpoints

### 4.1 Xem báo cáo công nợ phân kỳ
* **Protocol & Path**: `GET /api/v1/credit/aging-report`
* **Query Params**:
  * `dealerId`: BIGINT (Optional - lọc theo đại lý cụ thể)
  * `region`: String (Optional - lọc theo vùng miền)
* **Response 200 OK**:
  ```json
  [
    {
      "dealerId": 3,
      "dealerCode": "DL-MINHTRI",
      "dealerName": "Đại lý Minh Trí",
      "creditLimit": 500000000.00,
      "currentBalance": 120000000.00,
      "creditStatus": "ACTIVE",
      "inTermAmount": 80000000.00,
      "overdue1to30": 30000000.00,
      "overdue31to60": 10000000.00,
      "overdue61to90": 0.00,
      "overdueOver90": 0.00,
      "riskLevel": "NORMAL"
    },
    {
      "dealerId": 1,
      "dealerCode": "DL-HOANGPHAT",
      "dealerName": "Đại lý Hoàng Phát",
      "creditLimit": 500000000.00,
      "currentBalance": 450000000.00,
      "creditStatus": "CREDIT_HOLD",
      "inTermAmount": 100000000.00,
      "overdue1to30": 150000000.00,
      "overdue31to60": 50000000.00,
      "overdue61to90": 100000000.00,
      "overdueOver90": 50000000.00,
      "riskLevel": "HIGH_RISK"
    }
  ]
  ```

## 5. Acceptance Criteria

* **Scenario: Lập báo cáo phân kỳ công nợ đại lý**
  * **Given**: Đại lý Minh Trí có 2 hóa đơn chưa thanh toán:
    * Hóa đơn INV1 trị giá `80,000,000` VNĐ, còn hạn thanh toán (thuộc nhóm trong hạn).
    * Hóa đơn INV2 trị giá `40,000,000` VNĐ, ngày đến hạn là cách đây 45 ngày (quá hạn 45 ngày, thuộc nhóm quá hạn 31-60 ngày).
  * **When**: Kế toán trưởng gửi yêu cầu lấy báo cáo công nợ phân kỳ.
  * **Then**: Hệ thống phân tích và trả về nhóm `inTermAmount = 80,000,000` VNĐ và `overdue31to60 = 40,000,000` VNĐ đối với đại lý Minh Trí, dư nợ `currentBalance` hiển thị chính xác là `120,000,000` VNĐ. Do có hóa đơn chưa quá hạn vượt mốc 60 ngày, trạng thái rủi ro `riskLevel` là `NORMAL`.
