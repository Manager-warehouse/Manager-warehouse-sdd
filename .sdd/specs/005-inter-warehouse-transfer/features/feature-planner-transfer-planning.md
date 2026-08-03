# Tính năng: Planner nhập phiếu điều chuyển nội bộ (US-WMS-11)

## 1. Bối cảnh và mục tiêu

Planner nhận lệnh điều chuyển từ Công ty mẹ, bộ phận điều phối trung tâm hoặc từ một `TRQ` đã được Quản lý kho nguồn duyệt và giữ hàng. Planner nhập hoặc chốt phiếu `TRF-*` trong màn **Điều chuyển nội bộ** tại `/inter-warehouse-transfers` để dispatcher lập chuyến và kho nguồn thực thi.

Công ty mẹ không phải user trong hệ thống ở Sprint 1. Hệ thống không tự gợi ý điều chuyển theo tồn kho và không cho kho tự ý xuất hàng nếu chưa có lệnh điều phối hợp lệ hoặc `TRQ` đã duyệt.

Phiếu điều chuyển có thể có nhiều dòng hàng, ví dụ 50 chảo và 30 nồi từ kho Hải Phòng sang kho Hà Nội trong cùng một chứng từ.

## 2. Tác nhân

- **Planner**: Tạo, sửa, hủy phiếu `TRF` khi phiếu còn `NEW`; không gán xe và không phê duyệt tồn.

## 3. Yêu cầu chức năng

- Hệ thống không được tự sinh đề xuất điều chuyển trong Sprint 1.
- Hệ thống chỉ tạo transfer từ input rõ ràng của Planner dựa trên lệnh ngoài hoặc `TRQ` đã duyệt.
- Mỗi transfer bắt buộc có `externalInstructionCode` để truy vết về lệnh gốc.
- Không được tạo hai transfer đang hoạt động có cùng `externalInstructionCode`, kho nguồn, kho đích và `documentDate`.
- Transfer đã `REJECTED` hoặc `CANCELLED` không chặn tạo lại chứng từ sửa cho cùng lệnh ngoài.
- Planner phải có quyền trước khi tạo/sửa/hủy phiếu.
- Một transfer phải có ít nhất một item line.
- Transfer mới tạo có status `NEW`.
- Khi tạo transfer, hệ thống ghi audit `TRANSFER_CREATE`.
- Sprint 1 không bắt buộc upload file lệnh điều chuyển ngoài.
- Khi Planner mở màn sửa, hệ thống phải load header và item hiện có để sửa trên dữ liệu cũ.
- Planner chỉ được sửa/hủy khi transfer còn `NEW`.
- Khi sửa transfer `NEW`, payload item hiện tại là danh sách cuối cùng; item bị bỏ khỏi payload sẽ bị xóa khỏi phiếu.
- Nếu transfer đã `APPROVED`, `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY` hoặc `CANCELLED`, hệ thống phải chặn sửa.
- Nếu Planner hủy transfer `NEW`, hệ thống đặt status `CANCELLED`, không đổi tồn kho và ghi audit `TRANSFER_CANCEL`.

## 4. API endpoint

- `POST /api/v1/inter-warehouse-transfers` - Planner tạo phiếu điều chuyển.
- `GET /api/v1/inter-warehouse-transfers/{id}` - Tải chi tiết phiếu để xem/sửa.
- `PUT /api/v1/inter-warehouse-transfers/{id}` - Planner lưu header và danh sách item khi phiếu còn `NEW`.
- `POST /api/v1/inter-warehouse-transfers/{id}/cancel` - Hủy phiếu; Planner chỉ được hủy `NEW`.

### Payload request

```json
{
  "sourceWarehouseId": 1,
  "destinationWarehouseId": 2,
  "plannedDate": "2026-06-20",
  "documentDate": "2026-06-13",
  "externalInstructionCode": "HQ-TRF-20260613-001",
  "notes": "Lệnh điều chuyển từ công ty mẹ",
  "items": [
    {
      "productId": 101,
      "plannedQty": 50
    },
    {
      "productId": 102,
      "plannedQty": 30
    }
  ]
}
```

### Payload response

```json
{
  "id": 10,
  "transferNumber": "TRF-20260613-0001",
  "status": "NEW",
  "sourceWarehouseId": 1,
  "destinationWarehouseId": 2,
  "plannedDate": "2026-06-20",
  "documentDate": "2026-06-13",
  "externalInstructionCode": "HQ-TRF-20260613-001",
  "notes": "Lệnh điều chuyển từ công ty mẹ",
  "items": [
    {
      "id": 1001,
      "productId": 101,
      "plannedQty": 50
    },
    {
      "id": 1002,
      "productId": 102,
      "plannedQty": 30
    }
  ],
  "createdAt": "2026-06-13T10:00:00Z"
}
```

## 5. Validation và xử lý lỗi

- `SAME_WAREHOUSE` (HTTP 422): Kho nguồn trùng kho đích.
- `TRANSFER_ITEMS_REQUIRED` (HTTP 400): Không có dòng hàng.
- `INVALID_TRANSFER_QTY` (HTTP 400): `plannedQty <= 0`.
- `PRODUCT_INACTIVE` (HTTP 422): Sản phẩm inactive hoặc không được giao dịch.
- `WAREHOUSE_INACTIVE` (HTTP 422): Kho nguồn hoặc kho đích inactive.
- `EXTERNAL_INSTRUCTION_CODE_REQUIRED` (HTTP 400): Thiếu `externalInstructionCode`.
- `DUPLICATE_EXTERNAL_INSTRUCTION` (HTTP 409): Trùng lệnh ngoài trên phiếu đang hoạt động.
- `ACCOUNTING_PERIOD_CLOSED` (HTTP 409): `documentDate` thuộc kỳ kế toán đã đóng.
- `DOCUMENT_DATE_MUST_NOT_BE_PAST` (HTTP 400): Ngày chứng từ ở quá khứ.
- `PLANNED_DATE_MUST_NOT_BE_PAST` (HTTP 400): Ngày dự kiến ở quá khứ.
- `PLANNED_DATE_BEFORE_DOCUMENT_DATE` (HTTP 400): Ngày dự kiến trước ngày chứng từ.
- `DUPLICATE_TRANSFER_ITEM` (HTTP 400): Một sản phẩm xuất hiện nhiều lần trong phiếu.
- `TRANSFER_QTY_MUST_BE_WHOLE_NUMBER` (HTTP 400): Số lượng điều chuyển là số lẻ/thập phân.
- `TRANSFER_UPDATE_NOT_ALLOWED` (HTTP 409): Phiếu không còn `NEW`.
- `TRANSFER_CANCEL_NOT_ALLOWED` (HTTP 409): Planner hủy phiếu sau khi không còn `NEW`.

## 6. Tiêu chí chấp nhận

- **Tạo transfer nhiều dòng từ lệnh ngoài**: Khi Planner gửi form có lệnh ngoài và hai dòng hàng, hệ thống tạo một transfer `NEW`, có hai item và audit `TRANSFER_CREATE`.
- **Chặn thiếu mã lệnh ngoài**: Nếu thiếu `externalInstructionCode`, hệ thống trả `EXTERNAL_INSTRUCTION_CODE_REQUIRED`.
- **Chặn trùng lệnh ngoài đang hoạt động**: Nếu đã có phiếu active cùng lệnh ngoài, nguồn, đích và ngày chứng từ, hệ thống trả `DUPLICATE_EXTERNAL_INSTRUCTION`.
- **Chặn nguồn trùng đích**: Nếu source và destination giống nhau, hệ thống trả `SAME_WAREHOUSE`.
- **Chặn ngày quá khứ**: Nếu `documentDate` hoặc `plannedDate` trước ngày nghiệp vụ backend, hệ thống reject và giữ phiếu không đổi.
- **Chặn SKU trùng**: Nếu cùng product xuất hiện nhiều lần trong form, hệ thống trả `DUPLICATE_TRANSFER_ITEM`.
- **Chặn item rỗng**: Nếu không có item, hệ thống trả `TRANSFER_ITEMS_REQUIRED`.
- **Sửa phiếu khi `NEW`**: Planner sửa quantity hoặc bỏ dòng hàng, hệ thống lưu danh sách mới, giữ status `NEW` và ghi audit.
- **Hủy phiếu khi `NEW`**: Hệ thống đặt `CANCELLED`, ghi `TRANSFER_CANCEL` và không đổi tồn kho.
- **Chặn sửa sau duyệt/từ chối**: Phiếu đã `APPROVED` hoặc `REJECTED` không được sửa item.
- **Tạo lại thay vì submit lại phiếu bị từ chối**: Phiếu bị từ chối giữ nguyên để audit; Planner tạo phiếu mới nếu vẫn cần thực thi lệnh.
- **Dashboard nhẹ cho Planner**: Workspace tối thiểu hiển thị mã transfer, tuyến, trạng thái và số dòng hàng.
