# Feature: Quét hóa đơn chuyển khoản bằng OCR (US-WMS-18)

## 1. Context and Goal
Để giảm thiểu công sức nhập tay và tăng tính chính xác khi ghi nhận thanh toán từ đại lý, hệ thống hỗ trợ Kế toán viên (`ACCOUNTANT`) upload ảnh chụp/ảnh màn hình hóa đơn chuyển khoản ngân hàng (Bank Transfer Receipt). Hệ thống sử dụng dịch vụ nhận diện ký tự quang học (OCR) để tự động trích xuất các thông tin quan trọng của giao dịch (như Số tiền, Ngày thanh toán, Nội dung chuyển khoản, Số tài khoản/Tên đại lý) và tự động điền (autofill) vào biểu mẫu tạo Phiếu thu (`payment_receipts`).

## 2. Actors
* **Kế toán viên (`ACCOUNTANT`)**: Người thực hiện tải ảnh hóa đơn lên, kiểm tra thông tin trích xuất và xác nhận tạo phiếu thu.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * **WHEN** Kế toán viên thực hiện upload một ảnh hóa đơn chuyển khoản lên hệ thống, hệ thống **SHALL**:
    * Gửi tệp tin ảnh đến dịch vụ OCR (`OcrService`).
    * Nhận diện và trích xuất các thông tin chính từ ảnh:
      * **Số tiền (`amount`)**: Trích xuất số tiền chuyển khoản từ hóa đơn.
      * **Ngày thanh toán (`paymentDate`)**: Trích xuất ngày giao dịch (định dạng `YYYY-MM-DD`). Nếu không nhận diện được, mặc định là ngày hiện tại.
      * **Mã giao dịch / Nội dung (`notes`)**: Trích xuất nội dung chuyển khoản hoặc mã giao dịch ngân hàng để lưu vào ghi chú phiếu thu.
      * **Thông tin đại lý**: Cố gắng nhận diện tên đại lý hoặc số tài khoản gửi trong nội dung hóa đơn để ánh xạ với đại lý tương ứng trong hệ thống (`dealerId`).
    * Trả về kết quả trích xuất dưới dạng JSON để giao diện người dùng tự động điền vào các trường tương ứng của form tạo phiếu thu.
    * Đảm bảo tính an toàn dữ liệu: Không lưu vĩnh viễn tệp tin ảnh hóa đơn lên máy chủ nếu không có yêu cầu lưu trữ chứng từ đính kèm (hoặc chỉ lưu tạm thời phục vụ xử lý OCR).
  * **WHEN** Kế toán viên chỉnh sửa thông tin trích xuất trên form, hệ thống **SHALL** cho phép lưu phiếu thu với thông tin đã chỉnh sửa thủ công để khắc phục sai sót của OCR.

## 4. API Endpoints

### 4.1 Upload và phân tích hóa đơn chuyển khoản qua OCR
* **Protocol & Path**: `POST /api/v1/payment-receipts/ocr`
* **Request Header**: `Content-Type: multipart/form-data`
* **Request Body**:
  * `file`: File (Định dạng ảnh JPG, PNG, tối đa 5MB)
* **Response 200 OK**:
  ```json
  {
    "amount": 25000000.00,
    "paymentDate": "2026-06-17",
    "dealerId": 3,
    "notes": "CK TIEN HANG - DAI LY MINH TRI - GIAO DICH 983274298",
    "confidenceScore": 0.92
  }
  ```
* **Response 400 Bad Request**: Trả về khi file không đúng định dạng hoặc dung lượng vượt quá giới hạn.
* **Response 422 Unprocessable Entity**: Trả về khi dịch vụ OCR không thể đọc hoặc phân tích được thông tin từ ảnh.

## 5. Acceptance Criteria

* **Scenario 1: Upload hóa đơn chuyển khoản hợp lệ và nhận diện thành công**
  * **Given**: Kế toán viên đã đăng nhập và đang mở form "Ghi nhận thanh toán". Có một ảnh hóa đơn chuyển khoản của Đại lý Minh Trí (ID: 3) với số tiền 25,000,000 VNĐ ngày 17/06/2026.
  * **When**: Kế toán viên upload ảnh hóa đơn lên endpoint OCR.
  * **Then**: Hệ thống phản hồi thành công (HTTP 200), trích xuất chính xác `amount = 25000000.00`, `paymentDate = "2026-06-17"`, map được `dealerId = 3` (dựa trên tên đại lý trong nội dung) và điền các giá trị này vào form.

* **Scenario 2: Upload file không phải là ảnh hoặc dung lượng quá lớn**
  * **Given**: Kế toán viên chọn một file tài liệu PDF hoặc file ảnh dung lượng 10MB.
  * **When**: Kế toán viên thực hiện gửi file lên hệ thống.
  * **Then**: Hệ thống chặn lại và trả về mã lỗi HTTP 400 cùng thông báo lỗi tương ứng.

* **Scenario 3: Ảnh mờ hoặc không nhận diện được thông tin thiết yếu**
  * **Given**: Kế toán viên upload một ảnh phong cảnh hoặc ảnh hóa đơn bị mờ không thể đọc được chữ.
  * **When**: Hệ thống gửi ảnh qua dịch vụ OCR.
  * **Then**: Hệ thống trả về mã lỗi HTTP 422 hoặc trả về kết quả với các giá trị trống (`null`) và `confidenceScore` thấp, đồng thời hiển thị thông báo yêu cầu người dùng nhập tay thông tin.
