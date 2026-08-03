# Tính năng: Trưởng kho đề xuất điều chuyển và trưởng kho nguồn duyệt giữ hàng (US-WMS-11A)

## 1. Bối cảnh và mục tiêu

Khi kho thiếu hàng, Trưởng kho cần xem tồn khả dụng ở kho khác để đề xuất điều chuyển. Vì đây là quyết định điều phối liên kho có ảnh hưởng trực tiếp đến tồn kho nguồn, Trưởng kho kho yêu cầu không được tự tạo lệnh xuất hàng trực tiếp. Trưởng kho tạo `TRQ`, gửi trưởng kho nguồn duyệt; khi trưởng kho nguồn duyệt, hệ thống giữ hàng luôn, sau đó Planner dùng yêu cầu đã duyệt/đã giữ hàng để tạo phiếu `TRF-*`.

Luồng này là bước tiền xử lý của transfer. Nó không thay thế các bước Planner tạo `TRF`, Dispatcher gán xe, Thủ kho xuất hàng và kho đích nhận hàng. CEO chỉ xem/giám sát yêu cầu điều chuyển, không duyệt nghiệp vụ `TRQ`.

## 2. Tác nhân

- **Trưởng kho kho yêu cầu**: Xem tồn khả dụng ở kho khác, tạo/sửa/soft-cancel `TRQ DRAFT`, gửi trưởng kho nguồn duyệt.
- **Trưởng kho nguồn**: Xem nhu cầu, tồn tham chiếu, lý do thiếu hàng rồi duyệt hoặc từ chối; khi duyệt phải giữ hàng nguồn theo FIFO.
- **CEO**: Xem/giám sát yêu cầu điều chuyển và báo cáo liên quan, không approve/reject `TRQ`.
- **Planner kho nguồn / Planner trung tâm**: Nhận mẫu yêu cầu đã duyệt/đã giữ hàng và tạo `TRF-*`.

## 3. Yêu cầu chức năng

- Hệ thống cho phép `WAREHOUSE_MANAGER` xem read-only tồn khả dụng ở các kho active khác.
- Tồn khả dụng = `totalQty - reservedQty`, không tính hàng quarantine.
- Màn tra cứu tồn không được thay đổi tồn, reserve hàng hoặc tạo chứng từ xuất ở kho khác.
- Trưởng kho chỉ được tạo request cho kho yêu cầu nằm trong scope kho được phân công.
- `TRQ` được lưu riêng với `TRF` cho đến khi trưởng kho nguồn duyệt/giữ hàng và Planner convert.
- `TRQ` phải được trưởng kho nguồn duyệt và giữ hàng trước khi convert thành `TRF-*`.
- Sau source-manager approval, hệ thống thông báo hoặc gán template đã duyệt cho Planner phụ trách kho nguồn.
- Hệ thống ghi audit cho tạo, submit, source-manager approve/reject/reserve và Planner convert.
- Khi tạo request, bắt buộc có kho nguồn, kho yêu cầu, ngày cần hàng, lý do nghiệp vụ và ít nhất một dòng hàng.
- Mỗi dòng hàng bắt buộc có product, requested quantity, observed source available quantity và observed requesting available quantity.
- `neededByDate` không được ở quá khứ.
- Một product chỉ được xuất hiện một lần trong request.
- Số lượng request phải là số nguyên dương.
- Khi tạo hoặc sửa `TRQ DRAFT`, hệ thống phải tính tổng tải theo `requestedQty * product.weightKg/product.volumeM3`; nếu không có xe active nào trong đội xe chở được toàn bộ yêu cầu thì chặn ngay từ đầu.
- Khi submit/approve/convert, số lượng request không được vượt tồn khả dụng hiện tại của kho nguồn.
- Khi trưởng kho nguồn approve, hệ thống giữ hàng FIFO ngay; nếu không đủ toàn bộ số lượng thì fail và không tạo partial reservation.
- Khi sửa `DRAFT`, hệ thống load header và item cũ vào form, lưu qua `PUT /api/v1/transfer-requests/{id}` và ghi audit.
- Khi xóa `DRAFT`, hệ thống soft-cancel sang `CANCELLED`; không xóa vật lý request hoặc item history.
- Trưởng kho nguồn chỉ approve/reject request `SUBMITTED`; reject bắt buộc có `rejectionReason`.
- Planner chỉ convert request `APPROVED`; sau khi tạo `TRF`, request được link tới transfer và đổi status `CONVERTED`.
- Request `REJECTED`, `CONVERTED`, `CANCELLED` không được sửa, duyệt hoặc convert tiếp.

## 4. API endpoint

- `GET /api/v1/warehouse-stock/cross-warehouse` - Trưởng kho xem tồn khả dụng read-only ở kho khác.
- `POST /api/v1/transfer-requests` - Tạo `TRQ DRAFT`.
- `PUT /api/v1/transfer-requests/{id}` - Sửa `TRQ DRAFT`.
- `POST /api/v1/transfer-requests/{id}/cancel` - Soft-cancel `TRQ DRAFT` từ action `Xoa`.
- `POST /api/v1/transfer-requests/{id}/submit` - Gửi trưởng kho nguồn duyệt.
- `POST /api/v1/transfer-requests/{id}/approve` - Trưởng kho nguồn duyệt request và giữ hàng.
- `POST /api/v1/transfer-requests/{id}/reject` - Trưởng kho nguồn từ chối request với lý do.
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
- `TRANSFER_REQUEST_TOO_LARGE_FOR_FLEET` (HTTP 422): Đơn điều chuyển quá lớn, không xe active nào trong đội xe chở được.
- `TRANSFER_REQUEST_APPROVAL_NOT_ALLOWED` (HTTP 409): Trưởng kho nguồn approve/reject ngoài status `SUBMITTED`.
- `ONLY_DRAFT_CAN_BE_UPDATED` (HTTP 409): Sửa request không còn `DRAFT`.
- `ONLY_DRAFT_CAN_BE_CANCELLED` (HTTP 409): Hủy request không còn `DRAFT`.
- `SOURCE_MANAGER_REJECTION_REASON_REQUIRED` (HTTP 400): Trưởng kho nguồn reject thiếu lý do.
- `INSUFFICIENT_AVAILABLE_STOCK` (HTTP 422): Không đủ tồn khả dụng để giữ hàng khi approve.
- `TRANSFER_REQUEST_NOT_APPROVED` (HTTP 409): Planner convert trước khi trưởng kho nguồn duyệt/giữ hàng.
- `TRANSFER_REQUEST_ALREADY_CONVERTED` (HTTP 409): Request đã link tới `TRF-*`.

## 6. Tiêu chí chấp nhận

- **Trưởng kho yêu cầu hàng từ kho khác**: HP chỉ còn 5 chảo, HCM còn 120; Trưởng kho HP tạo request 50 chảo từ HCM, hệ thống tạo `DRAFT` và audit `TRANSFER_REQUEST_CREATE`.
- **Chặn ngày cần hàng quá khứ**: `neededByDate` trước ngày nghiệp vụ backend bị reject với `NEEDED_BY_DATE_MUST_NOT_BE_PAST`.
- **Chặn request vượt tồn nguồn**: Nếu HN chỉ còn 49 khả dụng mà request 50, hệ thống reject và không gửi duyệt.
- **Chặn request vượt đội xe**: Nếu tổng tải yêu cầu lớn hơn mọi xe active, hệ thống reject ngay khi tạo/sửa `DRAFT`, không tạo `TRQ`.
- **Submit trưởng kho nguồn**: Request `DRAFT` hợp lệ chuyển `SUBMITTED`, ghi `submittedAt`, route tới trưởng kho nguồn và audit `TRANSFER_REQUEST_SUBMIT`.
- **Sửa `DRAFT`**: Bấm `Sua`, sửa header/item, lưu lại, giữ `DRAFT` và ghi audit update.
- **Xóa `DRAFT`**: Bấm `Xoa`, confirm, hệ thống set `CANCELLED`, giữ lịch sử và chặn submit/edit/convert.
- **Trưởng kho nguồn duyệt**: Request `SUBMITTED` chuyển `APPROVED`, ghi metadata, giữ hàng FIFO nguồn, audit `TRANSFER_REQUEST_SOURCE_APPROVE` và gửi template cho Planner nguồn.
- **Planner convert**: Planner tạo một `TRF-*`, link với request, mark request `CONVERTED` và audit `TRANSFER_REQUEST_CONVERT`.
- **Chặn convert trước source approval**: Request `DRAFT` hoặc `SUBMITTED` không được convert.
- **Trưởng kho nguồn từ chối**: Trưởng kho nguồn reject với reason, request thành `REJECTED`, lưu reason, audit và không được convert.
