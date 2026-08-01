# Tính năng: Trưởng kho đề xuất điều chuyển và CEO duyệt (US-WMS-11A)

## 1. Bối cảnh và mục tiêu

Khi kho thiếu hàng, Trưởng kho cần xem tồn khả dụng ở kho khác để đề xuất điều chuyển. Vì đây là quyết định điều phối liên kho, Trưởng kho không được tự tạo lệnh xuất hàng trực tiếp. Trưởng kho tạo `TRQ`, gửi CEO duyệt, sau đó Planner kho nguồn dùng yêu cầu đã duyệt để tạo phiếu `TRF-*`.

Luồng này là bước tiền xử lý của transfer. Nó không thay thế các bước Planner tạo `TRF`, Trưởng kho nguồn duyệt tồn, Dispatcher gán xe, Thủ kho xuất hàng và kho đích nhận hàng.

## 2. Tác nhân

- **Trưởng kho kho yêu cầu**: Xem tồn khả dụng ở kho khác, tạo/sửa/soft-cancel `TRQ DRAFT`, gửi CEO duyệt.
- **CEO**: Xem nhu cầu, tồn tham chiếu, lý do thiếu hàng rồi duyệt hoặc từ chối.
- **Planner kho nguồn / Planner trung tâm**: Nhận mẫu yêu cầu đã duyệt và tạo `TRF-*`.

## 3. Yêu cầu chức năng

- Hệ thống cho phép `WAREHOUSE_MANAGER` xem read-only tồn khả dụng ở các kho active khác.
- Tồn khả dụng = `totalQty - reservedQty`, không tính hàng quarantine.
- Màn tra cứu tồn không được thay đổi tồn, reserve hàng hoặc tạo chứng từ xuất ở kho khác.
- Trưởng kho chỉ được tạo request cho kho yêu cầu nằm trong scope kho được phân công.
- `TRQ` được lưu riêng với `TRF` cho đến khi CEO duyệt và Planner convert.
- `TRQ` phải được CEO duyệt trước khi convert thành `TRF-*`.
- Sau CEO approval, hệ thống thông báo hoặc gán template đã duyệt cho Planner phụ trách kho nguồn.
- Hệ thống ghi audit cho tạo, submit, CEO approve/reject và Planner convert.
- Khi tạo request, bắt buộc có kho nguồn, kho yêu cầu, ngày cần hàng, lý do nghiệp vụ và ít nhất một dòng hàng.
- Mỗi dòng hàng bắt buộc có product, requested quantity, observed source available quantity và observed requesting available quantity.
- `neededByDate` không được ở quá khứ.
- Một product chỉ được xuất hiện một lần trong request.
- Số lượng request phải là số nguyên dương.
- Khi submit/approve/convert, số lượng request không được vượt tồn khả dụng hiện tại của kho nguồn.
- Khi sửa `DRAFT`, hệ thống load header và item cũ vào form, lưu qua `PUT /api/v1/transfer-requests/{id}` và ghi audit.
- Khi xóa `DRAFT`, hệ thống soft-cancel sang `CANCELLED`; không xóa vật lý request hoặc item history.
- CEO chỉ approve/reject request `SUBMITTED`; reject bắt buộc có `rejectionReason`.
- Planner chỉ convert request `APPROVED`; sau khi tạo `TRF`, request được link tới transfer và đổi status `CONVERTED`.
- Request `REJECTED`, `CONVERTED`, `CANCELLED` không được sửa, duyệt hoặc convert tiếp.

## 4. API endpoint

- `GET /api/v1/warehouse-stock/cross-warehouse` - Trưởng kho xem tồn khả dụng read-only ở kho khác.
- `POST /api/v1/transfer-requests` - Tạo `TRQ DRAFT`.
- `PUT /api/v1/transfer-requests/{id}` - Sửa `TRQ DRAFT`.
- `POST /api/v1/transfer-requests/{id}/cancel` - Soft-cancel `TRQ DRAFT` từ action `Xoa`.
- `POST /api/v1/transfer-requests/{id}/submit` - Gửi CEO duyệt.
- `POST /api/v1/transfer-requests/{id}/approve` - CEO duyệt request.
- `POST /api/v1/transfer-requests/{id}/reject` - CEO từ chối request với lý do.
- `POST /api/v1/transfer-requests/{id}/convert` - Planner tạo `TRF-*` từ request đã duyệt.

### Payload tạo request

```json
{
  "requestingWarehouseId": 1,
  "sourceWarehouseId": 3,
  "neededByDate": "2026-06-28",
  "businessReason": "Kho HP thiếu hàng để giao đại lý trong tuần, kho HCM còn tồn khả dụng.",
  "items": [
    {
      "productId": 101,
      "requestedQty": 50,
      "observedSourceAvailableQty": 120,
      "observedRequestingAvailableQty": 5,
      "shortageReason": "Dự kiến không đủ giao hàng nếu không điều chuyển."
    }
  ]
}
```

### Payload response

```json
{
  "id": 501,
  "requestNumber": "TRQ-20260624-0001",
  "status": "DRAFT",
  "requestingWarehouseId": 1,
  "sourceWarehouseId": 3,
  "neededByDate": "2026-06-28",
  "businessReason": "Kho HP thiếu hàng để giao đại lý trong tuần, kho HCM còn tồn khả dụng.",
  "items": [
    {
      "id": 9001,
      "productId": 101,
      "requestedQty": 50,
      "observedSourceAvailableQty": 120,
      "observedRequestingAvailableQty": 5,
      "shortageReason": "Dự kiến không đủ giao hàng nếu không điều chuyển."
    }
  ],
  "createdAt": "2026-06-24T09:00:00Z"
}
```

## 5. Validation và xử lý lỗi

- `CROSS_WAREHOUSE_STOCK_VIEW_FORBIDDEN` (HTTP 403): Actor không được xem tồn liên kho.
- `SAME_WAREHOUSE` (HTTP 422): Kho yêu cầu trùng kho nguồn.
- `WAREHOUSE_INACTIVE` (HTTP 422): Kho yêu cầu hoặc kho nguồn inactive.
- `TRANSFER_REQUEST_ITEMS_REQUIRED` (HTTP 400): Request không có item.
- `INVALID_TRANSFER_QTY` (HTTP 400): `requestedQty <= 0`.
- `TRANSFER_QTY_MUST_BE_WHOLE_NUMBER` (HTTP 400): Số lượng request là số lẻ/thập phân.
- `NEEDED_BY_DATE_MUST_NOT_BE_PAST` (HTTP 400): Ngày cần hàng ở quá khứ.
- `DUPLICATE_TRANSFER_REQUEST_ITEM` (HTTP 400): Một product xuất hiện nhiều lần.
- `PRODUCT_INACTIVE` (HTTP 422): Product inactive hoặc không được điều chuyển.
- `TRANSFER_REQUEST_REASON_REQUIRED` (HTTP 400): Thiếu lý do nghiệp vụ hoặc lý do thiếu hàng bắt buộc.
- `TRANSFER_REQUEST_QTY_EXCEEDS_SOURCE_AVAILABLE` (HTTP 422): Số lượng request vượt tồn khả dụng nguồn.
- `TRANSFER_REQUEST_APPROVAL_NOT_ALLOWED` (HTTP 409): CEO approve/reject ngoài status `SUBMITTED`.
- `ONLY_DRAFT_CAN_BE_UPDATED` (HTTP 409): Sửa request không còn `DRAFT`.
- `ONLY_DRAFT_CAN_BE_CANCELLED` (HTTP 409): Hủy request không còn `DRAFT`.
- `CEO_REJECTION_REASON_REQUIRED` (HTTP 400): CEO reject thiếu lý do.
- `TRANSFER_REQUEST_NOT_APPROVED` (HTTP 409): Planner convert trước khi CEO duyệt.
- `TRANSFER_REQUEST_ALREADY_CONVERTED` (HTTP 409): Request đã link tới `TRF-*`.

## 6. Tiêu chí chấp nhận

- **Trưởng kho yêu cầu hàng từ kho khác**: HP chỉ còn 5 chảo, HCM còn 120; Trưởng kho HP tạo request 50 chảo từ HCM, hệ thống tạo `DRAFT` và audit `TRANSFER_REQUEST_CREATE`.
- **Chặn ngày cần hàng quá khứ**: `neededByDate` trước ngày nghiệp vụ backend bị reject với `NEEDED_BY_DATE_MUST_NOT_BE_PAST`.
- **Chặn request vượt tồn nguồn**: Nếu HN chỉ còn 49 khả dụng mà request 50, hệ thống reject và không submit CEO.
- **Submit CEO**: Request `DRAFT` hợp lệ chuyển `SUBMITTED`, ghi `submittedAt`, route tới CEO và audit `TRANSFER_REQUEST_SUBMIT`.
- **Sửa `DRAFT`**: Bấm `Sua`, sửa header/item, lưu lại, giữ `DRAFT` và ghi audit update.
- **Xóa `DRAFT`**: Bấm `Xoa`, confirm, hệ thống set `CANCELLED`, giữ lịch sử và chặn submit/edit/convert.
- **CEO duyệt**: Request `SUBMITTED` chuyển `APPROVED`, ghi metadata, audit `TRANSFER_REQUEST_CEO_APPROVE` và gửi template cho Planner nguồn.
- **Planner convert**: Planner tạo một `TRF-*`, link với request, mark request `CONVERTED` và audit `TRANSFER_REQUEST_CONVERT`.
- **Chặn convert trước CEO approval**: Request `DRAFT` hoặc `SUBMITTED` không được convert.
- **CEO từ chối**: CEO reject với reason, request thành `REJECTED`, lưu reason, audit và không được convert.
