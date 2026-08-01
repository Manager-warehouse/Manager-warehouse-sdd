# Tính năng: Kho nguồn soạn, QC và xuất hàng điều chuyển (US-WMS-12)

## 1. Bối cảnh và mục tiêu

Công nhân kho nguồn lấy hàng, bốc xếp lên xe nội bộ và báo cáo số lượng thực xếp theo phiếu đã duyệt. Thủ kho nguồn QC xuất sau khi có báo cáo xếp, yêu cầu công nhân xử lý lại nếu QC thất bại, và chỉ cho bàn giao tài xế khi QC đạt. Dispatcher lập chuyến xe `TTR-*` riêng cho phiếu điều chuyển. Tài xế được gán xác nhận nhận hàng và rời kho để hệ thống chuyển tồn sang `IN_TRANSIT`.

Luồng này chỉ áp dụng cho mã `TRF-*` trong màn **Điều chuyển nội bộ**, không liên quan luồng `RN-*` nhập nhà cung cấp. Sprint 1 không dùng Barcode/QR; các bước báo xếp, QC xuất và bàn giao dùng số lượng nhập/xác nhận và ảnh bằng chứng. UI không nhập link ảnh thủ công; người dùng chọn file ảnh hoặc chụp trực tiếp trước khi nút xác nhận được bật.

## 2. Tác nhân

- **Công nhân/Nhân viên kho nguồn**: Lấy hàng, bốc xếp lên xe và báo số lượng xếp theo từng dòng phiếu.
- **Thủ kho nguồn**: QC xuất, yêu cầu xử lý lại khi QC fail, xác nhận hàng đủ điều kiện bàn giao.
- **Trưởng kho nguồn**: Duyệt hoặc từ chối phiếu điều chuyển.
- **Dispatcher kho nguồn**: Lập chuyến xe, gán xe và tài xế khả dụng trong scope kho nguồn.
- **Tài xế**: Xác nhận nhận hàng và xe rời kho nguồn.

## 3. Yêu cầu chức năng

- Mọi transfer phải đi qua kho ảo `IN_TRANSIT`.
- `source_warehouse_id` phải khác `destination_warehouse_id`.
- Transfer phải có đúng một trip nội bộ `trip_type = TRANSFER` trước khi depart.
- Mọi thao tác kho nguồn phải kiểm role và warehouse scope.
- Dispatcher chỉ lập chuyến cho transfer có kho nguồn thuộc scope của mình.
- Chỉ chọn được tài xế có scope kho chứa kho nguồn.
- Trưởng kho nguồn chỉ duyệt transfer `NEW` thuộc kho nguồn của mình hoặc có quyền manager override.
- Trước khi duyệt, hệ thống kiểm `available_qty = total_qty - reserved_qty >= planned_qty` trên tồn FIFO hợp lệ, active, không quarantine.
- Nếu tồn không đủ, không được tạo partial reservation/allocation, không đổi status và không ghi approval audit.
- Khi duyệt, hệ thống tăng `reserved_qty` nguồn và ghi audit `TRANSFER_APPROVE`.
- Khi từ chối, bắt buộc reason, set status `REJECTED`, không đổi tồn và ghi audit `TRANSFER_REJECT`.
- Dispatcher gán/cập nhật trip chỉ khi transfer `APPROVED`, thời gian hợp lệ, không quá khứ, không vượt hạn cần hàng.
- Xe/tài xế được chọn phải khả dụng, không trùng lịch và đúng scope kho nguồn.
- Có thể đổi xe/tài xế/lịch trước departure; sau departure trip assignment bị lock.
- Công nhân nguồn báo `loaded_qty` cho mọi item trước outbound QC.
- `loaded_qty` phải là số nguyên không âm; trước khi QC pass, mọi dòng phải có `loaded_qty = planned_qty`.
- Nếu QC fail, hệ thống set rework marker; công nhân phải dỡ/đổi/sửa/báo lại trước khi QC được pass.
- Outbound QC bắt buộc có ảnh bằng chứng, không yêu cầu Barcode/QR.
- Khi QC thiếu hoặc fail, hệ thống chặn ship, load handover và depart.
- Ship xác nhận `sent_qty` từ `loaded_qty`, bắt buộc `sent_qty = planned_qty` cho mọi item.
- Load handover bắt buộc có ảnh bàn giao hàng đã xếp cho tài xế.
- Depart chỉ được thực hiện bởi tài xế được gán, sau khi đã có trip, ship, QC pass và load handover.
- Depart trừ tồn nguồn, giảm reservation nguồn, cộng tồn `IN_TRANSIT`, set transfer `IN_TRANSIT` và ghi audit `TRANSFER_DEPART`.
- Nếu muốn hủy sau ship nhưng trước depart, phải `/unship` trước để xóa `sent_qty`.
- Transfer `NEW` do Planner hủy; transfer `APPROVED` chưa ship do Trưởng kho nguồn/manager hủy và release reservation.
- Không được hủy khi đã `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, `REJECTED` hoặc `CANCELLED`.

## 4. API endpoint

- `POST /api/v1/inter-warehouse-transfers/{id}/approve` - Trưởng kho nguồn duyệt và giữ hàng.
- `POST /api/v1/inter-warehouse-transfers/{id}/reject` - Trưởng kho nguồn từ chối phiếu `NEW` với lý do.
- `POST /api/v1/inter-warehouse-transfers/{id}/trip` - Dispatcher gán chuyến xe nội bộ.
- `POST /api/v1/inter-warehouse-transfers/{id}/source-load-report` - Công nhân nguồn báo số lượng thực xếp trước QC.
- `POST /api/v1/inter-warehouse-transfers/{id}/outbound-qc` - Thủ kho nguồn QC xuất sau khi công nhân báo xếp.
- `POST /api/v1/inter-warehouse-transfers/{id}/ship` - Thủ kho nguồn chốt số lượng gửi sau QC đạt.
- `POST /api/v1/inter-warehouse-transfers/{id}/load-handover` - Ghi nhận bàn giao hàng đã xếp cho tài xế.
- `POST /api/v1/inter-warehouse-transfers/{id}/unship` - Gỡ hàng trước khi tài xế rời kho, xóa `sent_qty`.
- `POST /api/v1/inter-warehouse-transfers/{id}/depart` - Tài xế xác nhận rời kho và chuyển tồn sang `IN_TRANSIT`.
- `POST /api/v1/inter-warehouse-transfers/{id}/cancel` - Hủy phiếu theo quyền và trạng thái.

## 5. Validation và xử lý lỗi

- `INSUFFICIENT_TRANSFER_STOCK` / `INSUFFICIENT_AVAILABLE_STOCK` (HTTP 422): Tồn nguồn không đủ.
- `TRANSFER_ALREADY_APPROVED` (HTTP 409): Duyệt lặp.
- `REJECTION_REASON_REQUIRED` (HTTP 400): Từ chối thiếu lý do.
- `TRANSFER_TRIP_REQUIRED` (HTTP 400): Depart trước khi có đúng một trip `TRANSFER`.
- `TRANSFER_TRIP_NOT_AVAILABLE` (HTTP 409): Xe/tài xế không khả dụng hoặc trùng lịch.
- `TRIP_DATE_MUST_NOT_BE_PAST` (HTTP 400): Lịch chuyến ở quá khứ.
- `TRIP_DEADLINE_EXPIRED` (HTTP 409): Hạn cần hàng đã qua trước dispatch/depart.
- `TRIP_END_BEFORE_START` (HTTP 400): Giờ kết thúc trước giờ bắt đầu.
- `TRIP_CAPACITY_EXCEEDED` (HTTP 422): Vượt tải trọng/thể tích xe.
- `WAREHOUSE_SCOPE_REQUIRED` (HTTP 403): Actor ngoài scope kho nguồn.
- `SOURCE_LOAD_REPORT_REQUIRED` (HTTP 409): Chưa có báo cáo xếp hàng.
- `SOURCE_LOAD_QTY_INVALID` (HTTP 400): `loaded_qty` âm hoặc không nguyên.
- `SOURCE_LOAD_REWORK_REQUIRED` (HTTP 409): QC trước fail, công nhân phải xử lý/báo lại.
- `OUTBOUND_QC_REQUIRED` (HTTP 409): Ship/depart trước QC pass.
- `TRANSFER_PHOTO_REQUIRED` (HTTP 400): Thiếu ảnh QC hoặc handover.
- `ASSIGNED_DRIVER_REQUIRED` (HTTP 409): Depart không phải tài xế được gán.
- `TRANSFER_SHIP_NOT_ALLOWED` (HTTP 409): Không được ship theo trạng thái/quyền hoặc đã ship.
- `SENT_QTY_MISMATCH` (HTTP 400): `sentQty` khác `plannedQty`.
- `TRANSFER_UNSHIP_NOT_ALLOWED` (HTTP 409): Không được unship theo trạng thái/quyền hoặc đã depart.
- `TRANSFER_DEPART_NOT_ALLOWED` (HTTP 409): Thiếu ship/trip/QC/handover hoặc sai tài xế.
- `TRANSFER_CANCEL_NOT_ALLOWED` (HTTP 409): Không được hủy theo trạng thái/quyền hoặc đã ship chưa unship.
- `TRANSFER_TRIP_LOCKED` (HTTP 409): Đổi xe/tài xế/lịch sau departure.

## 6. Audit trail

- `TRANSFER_APPROVE`: Duyệt phiếu, reserve tồn nguồn và chuyển `APPROVED`.
- `TRANSFER_REJECT`: Từ chối phiếu `NEW` kèm lý do.
- `TRANSFER_TRIP_ASSIGN`: Gán trip điều chuyển.
- `TRANSFER_TRIP_REASSIGN`: Đổi xe/tài xế/lịch trước departure.
- `TRANSFER_SOURCE_LOAD_REPORT`: Công nhân báo số lượng xếp.
- `TRANSFER_SOURCE_LOAD_REWORK`: Công nhân xử lý/báo lại sau QC fail.
- `TRANSFER_OUTBOUND_QC`: Thủ kho ghi QC xuất và ảnh.
- `TRANSFER_SHIP`: Thủ kho chốt `sent_qty`.
- `TRANSFER_LOAD_HANDOVER`: Bàn giao hàng cho tài xế kèm ảnh.
- `TRANSFER_UNSHIP`: Gỡ hàng trước departure.
- `TRANSFER_DEPART`: Tài xế rời kho, tồn chuyển nguồn -> `IN_TRANSIT`.
- `TRANSFER_CANCEL`: Hủy phiếu và release reservation nếu có.

## 7. Tiêu chí chấp nhận

- **Duyệt transfer giữ hàng**: HP có 50, reserve 0; duyệt transfer 30 thì `reserved_qty = 30`, available còn 20.
- **Không đủ tồn không reserve một phần**: Nếu chỉ có 1 đơn vị FIFO hợp lệ mà duyệt 2, hệ thống trả `INSUFFICIENT_AVAILABLE_STOCK` và không đổi status/reservation/audit.
- **Từ chối bắt buộc reason**: Phiếu `NEW` bị từ chối phải lưu reason, status `REJECTED`, audit `TRANSFER_REJECT`, tồn không đổi.
- **Dispatcher chỉ gán tài xế đúng scope nguồn**: Transfer HP -> HN không hiển thị tài xế chỉ thuộc HN cho Dispatcher HP.
- **Chặn dispatch quá hạn**: Nếu deadline đã qua trước khi depart, hệ thống trả `TRIP_DEADLINE_EXPIRED`.
- **Planner hủy `NEW`**: Status thành `CANCELLED`, audit `TRANSFER_CANCEL`, tồn không đổi.
- **Trưởng kho hủy `APPROVED` chưa ship**: Release reservation và audit `TRANSFER_CANCEL`.
- **Công nhân báo xếp trước QC**: Sau `loaded_qty = planned_qty`, thủ kho được QC xuất.
- **QC fail trả về công nhân**: Hệ thống chặn handover/depart và chỉ hiện action rework/re-report.
- **Chặn mismatch số lượng gửi**: `loaded_qty` 29 hoặc 31 cho kế hoạch 30 bị reject `SENT_QTY_MISMATCH`.
- **Unship trước cancel**: Phiếu đã ship nhưng chưa depart phải `/unship` trước khi hủy.
- **Depart release reservation và vào `IN_TRANSIT`**: Sau depart, nguồn giảm `total_qty/reserved_qty`, `IN_TRANSIT` tăng và status `IN_TRANSIT`.
- **Cho reassign trước departure, chặn sau departure**: Trước depart có thể đổi xe/tài xế/lịch; sau depart trả `TRANSFER_TRIP_LOCKED`.
- **Chặn hủy sau departure**: Transfer `IN_TRANSIT` không được hủy trực tiếp.
