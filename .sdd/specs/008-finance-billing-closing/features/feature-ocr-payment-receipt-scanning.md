# Feature: Quét hóa đơn/ủy nhiệm chi chuyển khoản bằng OCR (US-WMS-18)

## 1. Context and Goal
Để giảm thiểu công sức nhập tay và tăng tính chính xác khi ghi nhận thanh toán, hệ thống hỗ trợ Kế toán viên (`ACCOUNTANT`) upload ảnh chụp/ảnh màn hình chứng từ chuyển khoản ngân hàng. Hệ thống sử dụng dịch vụ nhận diện ký tự quang học (OCR) để tự động trích xuất các thông tin quan trọng của giao dịch (như Số tiền, Ngày thanh toán, Nội dung chuyển khoản, Số tài khoản/Tên đối tác) và tự động điền (autofill) vào biểu mẫu tương ứng.

Tính năng này có **hai luồng song song, dùng chung một engine OCR** nhưng khác đối tượng đối chiếu và khác endpoint (spec.md Session 2026-07-23):
* **AR — Phiếu thu Đại lý** (mục 3–5 dưới đây): `POST /api/v1/payment-receipts/ocr`, đối chiếu với `dealers`.
* **AP — Phiếu chi Nhà cung cấp** (mục 4.2, `feature-accountant-supplier-invoicing.md` §4.5): `POST /api/v1/supplier-payments/ocr`, đối chiếu với `suppliers`.


## 2. Actors
* **Kế toán viên (`ACCOUNTANT`)**: Người thực hiện tải ảnh chứng từ lên, kiểm tra thông tin trích xuất và xác nhận tạo phiếu thu/phiếu chi.

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
    "payment_date": "2026-06-17",
    "dealer_id": 3,
    "notes": "CK TIEN HANG - DAI LY MINH TRI - GIAO DICH 983274298",
    "confidence_score": 0.92
  }
  ```
* **Response 400 Bad Request**: Trả về khi file không đúng định dạng hoặc dung lượng vượt quá giới hạn.
* **Response 422 Unprocessable Entity**: Trả về khi dịch vụ OCR không thể đọc hoặc phân tích được thông tin từ ảnh.

### 4.2 [AP] Upload và phân tích Ủy nhiệm chi thanh toán NCC qua OCR
* **Protocol & Path**: `POST /api/v1/supplier-payments/ocr`
* **Request Header**: `Content-Type: multipart/form-data`
* **Request Body**:
  * `file`: File (Định dạng ảnh JPG, PNG, tối đa 5MB)
* **Cơ chế**: Dùng chung engine trích xuất OCR với mục 4.1 (`OcrService.extractRawText()` — đọc ảnh thật bằng Tesseract, không phải regex trên tên file). Logic trích số tiền/ngày tháng dùng chung một bộ tiện ích (`OcrTextParser`) với luồng AR; điểm khác biệt duy nhất là đối chiếu tên/mã đối tác: mục 4.1 so khớp `dealers.name`/`dealers.code`, mục 4.2 so khớp `suppliers.company_name`/`suppliers.code` (chỉ xét NCC đang `is_active = true`).
* **Response 200 OK**:
  ```json
  {
    "amount": 20000000.00,
    "paymentDate": "2026-07-22",
    "supplierId": 5,
    "supplierInvoiceId": null,
    "notes": "UNC CHI TIEN HANG - NCC GIA DUNG PHUNG - GIAO DICH OCR",
    "confidenceScore": 0.95
  }
  ```
  > Response body dùng `camelCase` (không giống mục 4.1 dùng `snake_case`) — đây là sự bất đối xứng có thật giữa hai DTO hiện tại (`PaymentReceiptOcrResponse` có `@JsonProperty` snake_case, `SupplierPaymentOcrResponse` thì không), khớp đúng với cách `SupplierPaymentServiceImpl` (frontend) đang đọc field, không phải lỗi tài liệu.
  > `supplierInvoiceId` luôn trả `null` — OCR chỉ xác định được đối tác (`supplierId`), không xác định được hóa đơn mua hàng cụ thể nào đang được thanh toán; Kế toán viên/frontend tự chọn hóa đơn cần cấn trừ trong danh sách hóa đơn chưa thanh toán của NCC đó sau khi OCR điền `supplierId`.
  > `confidenceScore` là hằng số cố định theo kết quả so khớp (`0.95` nếu khớp tên/mã NCC, `0.60` nếu không xác định được NCC nào) — không phải điểm tin cậy do engine OCR tính toán, giống hệt cách mục 4.1 (AR) hoạt động.
* **Response 400 Bad Request**: File rỗng hoặc không hợp lệ.
* **Response 422 Unprocessable Entity**: Không nhận diện được số tiền hợp lệ từ ảnh, hoặc dịch vụ OCR chưa sẵn sàng trên máy chủ.

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
