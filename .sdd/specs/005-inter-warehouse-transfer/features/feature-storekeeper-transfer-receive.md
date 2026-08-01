# Tính năng: Kho đích nhận hàng và xử lý chênh lệch điều chuyển (US-WMS-12)

## 1. Bối cảnh và mục tiêu

Khi xe điều chuyển đến, công nhân kho đích ghi nhận số lượng thực nhận ban đầu. Thủ kho kho đích kiểm lại số lượng, chốt QC, chọn vị trí nhập cho hàng đạt. Trưởng kho đích xác nhận cuối cùng, xử lý chênh lệch nếu có và hoàn tất phiếu.

Luồng này xử lý trong màn **Điều chuyển nội bộ** cho mã `TRF-*`, không đi vào danh sách phiếu nhập `RN-*` từ nhà cung cấp. Sprint 1 giả định mỗi `TRF` có một lần ship và một lần final receive; không hỗ trợ split receive hoặc nhiều lần final receive độc lập. Mọi bước bàn giao có ảnh phải dùng chọn file hoặc chụp ảnh trực tiếp, không nhập link ảnh thủ công.

## 2. Tác nhân

- **Công nhân/Nhân viên kho đích**: Đếm hàng ban đầu khi xe đến.
- **Thủ kho kho đích**: Kiểm số lượng, QC nhận, chọn `destinationLocationId` cho hàng đạt.
- **Trưởng kho đích**: Duyệt nhập kho cuối và xác nhận discrepancy.
- **Tài xế được gán**: Ghi arrival, return departure và return arrival khi có quay đầu.
- **Kho nguồn**: Nhận lại hàng khi `is_returned = true`.

## 3. Yêu cầu chức năng

### 3.1. Arrival và handover

- Receive-count chỉ được thực hiện khi transfer đang `IN_TRANSIT`.
- Luồng thường bắt buộc có driver arrival và receiving handover trước khi đếm.
- Luồng quay đầu bắt buộc có return departure, return arrival và return handover trước khi kho nguồn đếm.
- Các nút xác nhận handover có ảnh phải disabled cho đến khi chọn/chụp ảnh.

### 3.2. Receive count

- Payload count phải có đúng một dòng cho mỗi transfer item.
- Không được trùng transfer item trong payload.
- `receivedQty` phải là số nguyên không âm.
- Nếu `receivedQty < sentQty`, `receivedQty > sentQty` hoặc công nhân báo vấn đề, bắt buộc có `issueReason`.
- Count được lưu như dữ liệu nháp của chu kỳ nhận; công nhân có thể sửa cho đến khi thủ kho duyệt receive check.
- Mỗi lần lưu count ghi audit `TRANSFER_RECEIVE_COUNT`.

### 3.3. Receive check và QC nhận

- Thủ kho chỉ check sau khi có receive count và trước khi check được duyệt.
- `confirmedReceivedQty`, `qcPassedQty`, `qcFailedQty` phải là số nguyên không âm.
- `qcPassedQty + qcFailedQty = confirmedReceivedQty`.
- Nếu `confirmedReceivedQty` khác `receivedQty` do công nhân nhập, bắt buộc có `checkerNote`.
- Nếu `qcFailedQty > 0`, bắt buộc có `qcFailureReason`.
- Nếu có hàng QC pass, bắt buộc chọn `destinationLocationId`.
- Vị trí nhập hàng pass phải thuộc kho nhận, active, đủ sức chứa và không phải quarantine bin.
- Nếu `qcFailedQty > 0`, kho nhận phải có ít nhất một quarantine bin active; lỗi này phải được phát hiện ở receive-check, không đợi final receive.
- Sau receive-check, công nhân không được sửa count.
- Receive-check ghi audit `TRANSFER_RECEIVE_CHECK`.

### 3.4. Final receive

- Trưởng kho chỉ final receive sau receive-check.
- Nếu `received_qty > sent_qty`, hệ thống không nhập thường phần thừa; phần thừa phải tạo discrepancy/hold.
- Nếu nhận đủ, hệ thống trừ `IN_TRANSIT`, cộng kho đích cho hàng QC pass, cộng quarantine cho hàng QC fail và set `COMPLETED`.
- Nếu thiếu hàng, bắt buộc `discrepancyReason`, trừ toàn bộ lượng sent khỏi `IN_TRANSIT`, cộng kho đích/quarantine theo lượng vật lý nhận được, tạo adjustment `TRANSFER_DISCREPANCY` và set `COMPLETED_WITH_DISCREPANCY`.
- Số lượng thiếu không có mặt vật lý nên không được tạo quarantine inventory hoặc disposal candidate.
- Final receive phải kiểm cấu hình kho ảo `IN_TRANSIT` và location active trước khi mutate tồn.
- Kế hoạch putaway không được trùng item/location, số lượng phải dương và không vượt `qcPassedQty`.
- Final receive ghi audit `TRANSFER_RECEIVE_CONFIRM`; nếu có shortage adjustment thì ghi thêm `TRANSFER_DISCREPANCY_CREATE`.

### 3.5. Quarantine reject

- Thủ kho hoặc Trưởng kho có thể từ chối toàn bộ khi transfer đang `IN_TRANSIT`.
- Bắt buộc có `rejectionReason`.
- Transfer chuyển `QUARANTINED`, set `rejectedBy`, `rejectedAt`, `rejectionReason`.
- Mọi item set `receivedQty = sentQty`, `qcPassedQty = 0`, `qcFailedQty = sentQty`.
- Tồn `IN_TRANSIT` chuyển vào quarantine bin của kho đích hoặc kho nguồn nếu `is_returned = true`.
- Quarantine record có origin `INTERNAL_TRANSFER` và trace tới transfer/transfer item để Spec 009 xử lý disposal.
- Chặn supplier RTV và supplier Debit Note với tồn này.
- Release vehicle/driver/trip và ghi audit `TRANSFER_QUARANTINE_REJECT`.

### 3.6. Wrong SKU và Return to Source

- Nếu phát hiện sai SKU nhưng hàng còn nguyên, thủ kho kho đích tạo report `WRONG_SKU`.
- Report bắt buộc có expected SKU, actual SKU, affected quantity và reason.
- Actual SKU phải tồn tại, khác expected SKU và quantity không vượt sent quantity của item.
- Khi report wrong-SKU, hàng vẫn ở `IN_TRANSIT`, không nhập kho thường và không vào quarantine.
- Quản lý kho đích duyệt hoặc từ chối return.
- Khi duyệt, transfer set `is_returned = true`, giữ nguyên trip/vehicle/driver/`IN_TRANSIT` và chỉ đạo tài xế quay về kho nguồn.
- Tài xế ghi `return-depart`, `return-arrive`; kho nguồn handover rồi lặp lại receive-count, receive-check/QC và final-receive.

## 4. API endpoint

- `POST /api/v1/inter-warehouse-transfers/{id}/arrive` - Tài xế ghi nhận đến kho nhận.
- `POST /api/v1/inter-warehouse-transfers/{id}/receiving-handover` - Kho nhận ghi bàn giao vật lý từ tài xế.
- `PUT /api/v1/inter-warehouse-transfers/{id}/receive-count` - Công nhân nhập/sửa số lượng nhận ban đầu.
- `PUT /api/v1/inter-warehouse-transfers/{id}/receive-check` - Thủ kho kiểm count, QC và chọn vị trí.
- `POST /api/v1/inter-warehouse-transfers/{id}/final-receive` - Trưởng kho duyệt nhập kho cuối.
- `POST /api/v1/inter-warehouse-transfers/{id}/quarantine-reject` - Từ chối toàn bộ và đưa vào quarantine.
- `POST /api/v1/inter-warehouse-transfers/{id}/request-return` - Báo wrong-SKU còn nguyên.
- `POST /api/v1/inter-warehouse-transfers/{id}/approve-return` - Duyệt cho xe quay đầu.
- `POST /api/v1/inter-warehouse-transfers/{id}/reject-return` - Từ chối quay đầu.
- `POST /api/v1/inter-warehouse-transfers/{id}/return-depart` - Tài xế bắt đầu chặng quay về nguồn.
- `POST /api/v1/inter-warehouse-transfers/{id}/return-arrive` - Tài xế về đến kho nguồn.

### Payload receive-count

```json
{
  "items": [
    {
      "transferItemId": 1001,
      "receivedQty": 28,
      "issueReason": "Thiếu 2 sản phẩm khi kiểm đếm ban đầu"
    }
  ]
}
```

### Payload receive-check

```json
{
  "items": [
    {
      "transferItemId": 1001,
      "confirmedReceivedQty": 28,
      "qcPassedQty": 26,
      "qcFailedQty": 2,
      "destinationLocationId": 201,
      "qcFailureReason": "2 sản phẩm bị móp vỏ hộp"
    }
  ],
  "checkerNote": "Đã đối chiếu số lượng với công nhân nhập ban đầu"
}
```

### Payload final-receive

```json
{
  "discrepancyReason": "Thiếu 2 sản phẩm so với số lượng đã gửi từ kho nguồn"
}
```

### Payload quarantine-reject

```json
{
  "rejectionReason": "Toàn bộ kiện hàng bị ướt sũng nước, không thể nhập kho"
}
```

## 5. Validation và xử lý lỗi

- `TRANSFER_RECEIVE_NOT_ALLOWED` (HTTP 409): Không được nhận ở trạng thái hiện tại.
- `TRANSFER_ARRIVAL_REQUIRED` (HTTP 409): Receive-count trước arrival/handover.
- `RETURN_ARRIVAL_REQUIRED` (HTTP 409): Nhận hàng quay đầu trước return arrival/handover.
- `RECEIVE_ISSUE_REASON_REQUIRED` (HTTP 400): Thiếu `issueReason` khi số lượng lệch hoặc có issue.
- `DUPLICATE_RECEIVE_COUNT_ITEM` (HTTP 400): Trùng item trong receive-count.
- `RECEIVE_QTY_MUST_BE_WHOLE_NUMBER` (HTTP 400): Số nhận không nguyên.
- `RECEIVED_QTY_EXCEEDS_SENT` (HTTP 422): Số nhận vượt số gửi.
- `RECEIVE_CHECK_REQUIRED` (HTTP 409): Final receive trước receive-check.
- `CHECKER_NOTE_REQUIRED` (HTTP 400): Thủ kho sửa số lượng nhưng thiếu note.
- `DUPLICATE_RECEIVE_CHECK_ITEM` (HTTP 400): Trùng item trong receive-check.
- `RECEIVE_CHECK_QTY_MUST_BE_WHOLE_NUMBER` (HTTP 400): Số QC/confirmed không nguyên.
- `QC_TOTAL_MISMATCH` (HTTP 400): Tổng QC pass + fail không bằng số confirmed.
- `QC_FAILURE_REASON_REQUIRED` (HTTP 400): Có QC fail nhưng thiếu reason.
- `DESTINATION_LOCATION_REQUIRED` (HTTP 400): Có QC pass nhưng thiếu vị trí nhập.
- `QC_PASSED_BIN_MUST_NOT_BE_QUARANTINE` (HTTP 400): Hàng QC pass chọn quarantine bin.
- `INVALID_DESTINATION_LOCATION` (HTTP 400): Vị trí sai kho hoặc inactive.
- `BIN_CAPACITY_EXCEEDED` (HTTP 422): Bin không đủ sức chứa.
- `QUARANTINE_LOCATION_NOT_CONFIGURED` (HTTP 422): Có QC fail nhưng kho không có quarantine bin active.
- `TRANSIT_WAREHOUSE_NOT_CONFIGURED` / `TRANSIT_LOCATION_NOT_CONFIGURED` (HTTP 500): Thiếu cấu hình `IN_TRANSIT`.
- `DUPLICATE_PUTAWAY_ITEM` / `DUPLICATE_PUTAWAY_LOCATION` (HTTP 400): Putaway trùng item/location.
- `PUTAWAY_QUANTITY_MUST_MATCH_QC_PASSED` (HTTP 422): Putaway vượt hoặc không khớp QC pass.
- `DISCREPANCY_REQUIRES_REASON` (HTTP 400): Thiếu reason cho shortage hoặc issue cuối.
- `RETURN_REQUEST_NOT_ALLOWED` (HTTP 409): Không được tạo wrong-SKU return ở trạng thái hiện tại.
- `WRONG_SKU_REASON_REQUIRED` (HTTP 400): Thiếu SKU/quantity/reason.
- `ACTUAL_WRONG_SKU_PRODUCT_NOT_FOUND` (HTTP 422): Actual SKU không tồn tại hoặc inactive.
- `WRONG_SKU_MUST_DIFFER_FROM_EXPECTED` (HTTP 400): Actual SKU trùng expected SKU.
- `AFFECTED_QTY_MUST_BE_POSITIVE` (HTTP 400): Quantity sai SKU không dương.
- `WRONG_SKU_QTY_EXCEEDS_SENT` (HTTP 422): Quantity sai SKU vượt số gửi.
- `RETURN_REQUEST_REQUIRED` (HTTP 409): Manager duyệt return khi chưa có report.
- `RETURN_APPROVAL_NOT_ALLOWED` (HTTP 403): Actor không phải quản lý kho đích hoặc ngoài scope.

## 6. Tiêu chí chấp nhận

- **Nhận thiếu hàng**: Gửi 30, nhận 28, có reason; hệ thống nhập 28 theo QC, clear 30 khỏi `IN_TRANSIT`, tạo discrepancy 2 và status `COMPLETED_WITH_DISCREPANCY`.
- **Chặn nhận thừa vào tồn thường**: Nếu nhận vượt số gửi, phần thừa không được nhập regular inventory và phải tạo hold/incident.
- **Chặn shortage thiếu reason**: Thiếu hàng nhưng không có `issueReason` hoặc `discrepancyReason` bị reject.
- **Chặn duplicate count/check item**: Payload trùng transfer item bị reject.
- **QC fail vào quarantine**: Hàng vật lý QC fail được đưa vào quarantine với origin `INTERNAL_TRANSFER`.
- **Thiếu hàng không vào quarantine**: Số thiếu không có mặt vật lý, không tạo quarantine/disposal.
- **Wrong-SKU nguyên vẹn quay đầu**: Thủ kho báo sai SKU, quản lý duyệt, tài xế quay về, kho nguồn nhận lại theo ba bước.
- **Chặn thủ kho tự duyệt return**: Thủ kho báo wrong-SKU không được tự approve.
- **Chặn sửa count sau receive-check**: Sau khi thủ kho duyệt check, công nhân không sửa count.
- **Thủ kho sửa count phải note**: Nếu confirmed khác worker count mà thiếu `checkerNote`, hệ thống reject.
- **Chặn QC fail thiếu reason/quarantine bin**: Có `qcFailedQty > 0` mà thiếu reason hoặc kho không có quarantine bin active thì reject.
- **Chặn hàng pass vào quarantine bin**: `destinationLocationId` là quarantine bin bị reject.
- **Chặn final trước receive-check**: Trưởng kho không final receive nếu thủ kho chưa check.
- **Không hỗ trợ split final receive**: Một `TRF` không được final receive nhiều chu kỳ độc lập trong Sprint 1.
