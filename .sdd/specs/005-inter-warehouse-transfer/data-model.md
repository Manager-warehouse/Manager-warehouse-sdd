# Mô hình dữ liệu: 005 Điều chuyển nội bộ giữa kho

## Transfer

**Bảng**: `transfers`

Các trường cần thêm/kiểm tra:
- `id`
- `transfer_number`
- `source_warehouse_id`
- `destination_warehouse_id`
- `status`: `NEW`, `APPROVED`, `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, `CANCELLED`, `QUARANTINED`
- `is_returned`
- `return_reason_code`, `return_reason`
- `outbound_qc_checked_by`, `outbound_qc_checked_at`, `outbound_qc_result`
- `source_loaded_reported_by`, `source_loaded_reported_at`
- `source_load_rework_required`, `source_load_rework_reason`
- `load_handover_by`, `load_handover_at`
- `outbound_qc_photo_ref`
- `load_handover_photo_ref`
- `driver_departed_at`, `driver_arrived_at`, `arrival_handover_at`, `arrival_handover_photo_ref`
- `return_departed_at`, `return_arrived_at`
- `created_by`
- `external_instruction_code`
- `approved_by`, `approved_at`
- `rejected_by`, `rejected_at`, `rejection_reason`
- `confirmed_by`, `confirmed_at`
- `planned_date`
- `actual_received_date`
- `discrepancy_reason`
- `trip_id`
- `document_date`
- `accounting_period_id`
- `notes`
- `transfer_request_id` (nullable, liên kết tới yêu cầu quản lý đã được duyệt)
- `created_at`, `updated_at`
- `version`

Validation:
- Kho nguồn và kho đích phải khác nhau.
- Bắt buộc có mã lệnh ngoài.
- Phiếu đang hoạt động phải unique theo mã lệnh ngoài, kho nguồn, kho đích và ngày chứng từ.
- Planner chỉ được sửa phiếu `NEW`.
- Planner chỉ được hủy phiếu `NEW`.
- Quản lý kho nguồn hoặc quản lý có quyền chỉ được hủy phiếu `APPROVED` khi chưa chốt gửi hàng.
- Không được hủy sau khi phiếu đã `IN_TRANSIT`.
- Nếu tạo từ transfer request, request liên kết phải là `APPROVED` và chưa từng convert.
- GET/list/detail không được mutate status hoặc persist overdue transition.
- Receive-count bị chặn cho đến khi đã ghi nhận tài xế đến và bàn giao tại kho nhận.
- Nhận hàng quay đầu bị chặn cho đến khi có return departure và source arrival/handover.
- Outbound QC tại nguồn bị chặn cho đến khi có số lượng loaded do công nhân báo cáo.
- Load handover và departure tại nguồn bị chặn khi `source_load_rework_required = true`.

## TransferRequest

**Bảng**: `transfer_requests`

Các trường cần thêm/kiểm tra:
- `id`
- `request_number`
- `requesting_warehouse_id` (kho đang cần hàng; sau này là kho đích của transfer)
- `source_warehouse_id` (kho dự kiến gửi hàng)
- `status`: `DRAFT`, `SUBMITTED`, `APPROVED`, `REJECTED`, `CONVERTED`, `CANCELLED`
- `requested_by`
- `submitted_at`
- `approved_by`, `approved_at`
- `rejected_by`, `rejected_at`, `rejection_reason`
- `needed_by_date`
- `business_reason`
- `planner_assignee_id`
- `converted_transfer_id`
- `created_at`, `updated_at`
- `version`

Validation:
- Kho yêu cầu và kho nguồn phải khác nhau.
- Kho yêu cầu phải nằm trong phạm vi kho được phân công của quản lý kho tạo yêu cầu.
- Tra cứu tồn liên kho chỉ read-only và phải loại hàng quarantine khỏi số lượng khả dụng.
- Bắt buộc có lý do nghiệp vụ trước khi submit.
- Quản lý kho yêu cầu chỉ được sửa request `DRAFT` trong phạm vi kho đích được phân công.
- Quản lý kho yêu cầu chỉ được soft-cancel request `DRAFT`; hủy đặt `status = CANCELLED` và không được xóa vật lý request hoặc items.
- CEO chỉ được approve/reject request `SUBMITTED`.
- CEO reject bắt buộc có `rejection_reason`.
- CEO approval không reserve inventory.
- Chỉ request `APPROVED` mới được convert thành `TRF`.
- Một request chỉ được convert tối đa thành một transfer đang hoạt động.
- Cập nhật đồng thời và duplicate conversion race phải fail bằng version/unique-constraint conflict.

## TransferRequestItem

**Bảng**: `transfer_request_items`

Các trường cần thêm/kiểm tra:
- `id`
- `transfer_request_id`
- `product_id`
- `requested_qty`
- `observed_source_available_qty`
- `observed_requesting_available_qty`
- `shortage_reason`

Validation:
- `requested_qty > 0`.
- `requested_qty` không được vượt quá tồn khả dụng hiện tại của kho nguồn tại thời điểm submit/approval.
- Tồn khả dụng nguồn là `total_qty - reserved_qty`, loại trừ hàng quarantine.
- Bắt buộc có lý do thiếu hàng theo item khi lý do nghiệp vụ không giải thích thiếu hụt ở cấp sản phẩm.

## TransferItem

**Bảng**: `transfer_items`

Các trường cần thêm/kiểm tra:
- `id`
- `transfer_id`
- `product_id`
- `source_location_id`
- `destination_location_id`
- `planned_qty`
- `sent_qty`
- `received_qty`
- `variance_qty`
- `qc_passed_qty`
- `qc_failed_qty`
- `qc_result`
- `qc_failure_reason`
- `receive_issue_reason`
- `receive_checked_by`
- `receive_checked_at`
- `receive_checker_note`
- `batch_id` nullable trên planned item; FIFO allocation rows lưu batch thật sau approval
- `loaded_qty`
- `loaded_reported_by`
- `loaded_reported_at`
- `outbound_qc_result`
- `outbound_qc_note`

Validation:
- `planned_qty > 0`.
- Khi shipping, `sent_qty = planned_qty`.
- `loaded_qty` do công nhân nguồn nhập trước outbound QC và phải bằng `planned_qty` trước khi outbound QC được pass.
- Nếu outbound QC fail, công nhân nguồn phải dỡ/đổi/sửa/báo lại số lượng loaded trước load handover hoặc departure.
- `sent_qty` chỉ được xác nhận từ `loaded_qty` sau khi outbound QC pass.
- `sent_qty == null` nghĩa là chưa loaded; `sent_qty != null` nghĩa là đã loaded nhưng chưa chắc đã rời kho.
- Receive count được công nhân kho đích sửa cho đến khi `receive_checked_at` được set.
- Bắt buộc có `receive_issue_reason` theo item nếu số lượng nhận ban đầu nhỏ/lớn hơn số gửi hoặc công nhân báo vấn đề.
- `confirmedReceivedQty` trở thành `received_qty` hiệu lực sau khi receive-check được duyệt.
- `qc_passed_qty + qc_failed_qty = confirmedReceivedQty`.
- `receive_checker_note` là optional khi số thủ kho kiểm bằng số công nhân nhập, và bắt buộc khi khác nhau.
- `qc_failed_qty` có mặt vật lý sẽ tạo hoặc cập nhật quarantine source record với `origin_type = INTERNAL_TRANSFER` và `origin_id = transfer_item.id`.
- `variance_qty` âm thể hiện thiếu hàng và không được tạo quarantine source record.
- Sai SKU không còn tự tạo flow return-to-source; nếu có damage/QC failure riêng thì xử lý theo quarantine/disposal tương ứng.
- Vị trí kho đích cho số lượng QC pass phải còn đủ sức chứa bin.

## WrongSkuReport

**Bảng**: `transfer_wrong_sku_reports` hoặc tên bảng thực tế tương đương

Các trường cần thêm/kiểm tra:
- `id`
- `transfer_id`
- `transfer_item_id`
- `expected_product_id`
- `actual_product_id`
- `quantity`
- `reason`
- `photo_refs`
- `status`: `REPORTED`, `APPROVED`, `REJECTED`, `RETURN_DEPARTED`, `RETURN_ARRIVED`, `CLOSED`
- `reported_by`, `reported_at`
- `decided_by`, `decided_at`, `decision_reason`

Validation:
- Sản phẩm expected và actual phải khác nhau.
- Quantity phải dương và không được vượt quá số lượng `IN_TRANSIT` bị ảnh hưởng.
- Bắt buộc có reason.
- Photo references là optional cho wrong-SKU trong Sprint 1 nhưng phải được giữ nếu người dùng cung cấp.
- Quyết định của quản lý yêu cầu scope quản lý kho đích.

## Quyết định trả hàng điều chuyển

Các trường trên `transfers`:
- `is_returned`
- `return_reason_code`: `TRIP_OVERDUE`
- `return_reason`

Rules:
- Khi transfer quá deadline trong lúc `IN_TRANSIT`, hệ thống set `is_returned = true`.
- Cùng trip, vehicle, driver và inventory `IN_TRANSIT` vẫn active cho chặng quay đầu.
- Sau khi quay về kho nguồn, kho nguồn phải count/check/QC/final receive trước khi nhập lại tồn.
- Tài xế được gán phải ghi return departure và source arrival/handover trước khi kho nguồn bắt đầu nhận.
- Nhân viên kho nguồn ghi return count, thủ kho nguồn check/QC, quản lý kho nguồn final-confirm.
- Xác nhận cuối tại nguồn hoàn tất transfer nhưng vẫn giữ `is_returned = true` để báo cáo.

## Trip

**Bảng**: `trips`

Cách dùng cho điều chuyển:
- `trip_type = TRANSFER`
- Mỗi transfer có đúng một trip.
- Xe và tài xế được chọn phải khả dụng và không trùng lịch chuyến khác.
- Chỉ tài xế được gán mới được xác nhận departure điều chuyển.
- Trip điều chuyển được gán phải hiển thị được trên màn mobile dùng chung của tài xế với `tripType = TRANSFER`, label `Dieu chuyen noi bo`, tuyến nguồn/đích, số dòng transfer, xe, giờ dự kiến, tổng trọng lượng và status.
- `total_weight` và `total_volume` cho trip điều chuyển được tính từ số lượng item transfer và thuộc tính sản phẩm/đóng gói.
- Trip assignment bị reject nếu trọng lượng hoặc thể tích tính toán vượt capacity xe.
- Vehicle/driver/trip chỉ được reassign trước departure.
- Khi completion, release vehicle/driver phải kiểm tài nguyên không còn active trip assignment khác.

## Inventory

Thao tác điều chuyển:
- Approval: tăng `reserved_qty` tại nguồn theo `planned_qty`.
- Approval/reservation: chỉ reserve stock FIFO hợp lệ từ vị trí active, không quarantine, đúng scope nguồn.
- Cancel phiếu approved chưa ship: giảm `reserved_qty` tại nguồn.
- Depart: giảm `total_qty` nguồn, giảm `reserved_qty` nguồn, tăng `total_qty` ở `IN_TRANSIT`.
- Final receive: giảm `total_qty` ở `IN_TRANSIT`; tăng regular inventory kho đích cho số lượng QC pass; tăng quarantine inventory kho đích cho số lượng QC fail.
- Final source return receive: giảm `total_qty` ở `IN_TRANSIT`; trả số lượng QC pass về regular inventory kho nguồn; chuyển số lượng vật lý QC fail vào Quarantine kho nguồn.
- Với shortage, số lượng và giá trị kho đích chỉ tính theo `received_qty` đã nhận vật lý và được chấp nhận.
- Số lượng thiếu giữ dưới dạng `variance_qty`/`TRANSFER_DISCREPANCY` chỉ theo số lượng và bị loại khỏi giá trị nhập kho đích cũng như billing totals.

## DiscrepancyIncident

**Bảng**: `discrepancy_incidents` hoặc bảng thực tế tương đương

Dùng cho điều chuyển:
- Thiếu hàng: incident loại `SHORTAGE` với transfer, transfer item, product, expected quantity, actual quantity, missing quantity và reason.
- Nhận thừa vật lý: incident loại `OVER_RECEIPT`; phần thừa không được nhập regular inventory cho đến khi có resolution được duyệt.
- Incident phải giữ status điều tra, người tạo, thời điểm tạo và resolution note.
- Incident phải liên kết được với audit log và adjustment nếu có.

## Adjustment

Dùng cho thiếu hàng điều chuyển:
- Loại adjustment: `TRANSFER_DISCREPANCY`.
- Quantity adjustment âm cho số lượng thiếu.
- Không gắn giá trị tiền vào adjustment thiếu hàng trong flow nhận điều chuyển.
- Adjustment phải tham chiếu transfer, transfer item, product, warehouse và reason.
- Adjustment không được tạo quarantine inventory cho hàng không tồn tại vật lý.

## QuarantineRecord

Dùng cho hàng QC fail vật lý:
- `origin_type = INTERNAL_TRANSFER`.
- `origin_id = transfer_item.id` hoặc link tương đương tới transfer item.
- Warehouse là nơi hàng được quarantine thực tế: kho đích trong luồng thường, kho nguồn trong luồng quay đầu.
- Quantity là số QC fail vật lý.
- Hàng quarantine không được tính vào available inventory.
- Disposal/return/scrap tiếp theo thuộc Spec 009, không thuộc Spec 005.

## AuditLog

Bắt buộc audit cho:
- Tạo/sửa/hủy `TRQ`.
- Submit/approve/reject/convert `TRQ`.
- Tạo/sửa/hủy `TRF`.
- Duyệt/từ chối `TRF`.
- Reservation/release reservation.
- Gán/reassign trip.
- Source load report và source load rework.
- Outbound QC.
- Ship/unship.
- Load handover.
- Depart và chuyển tồn sang `IN_TRANSIT`.
- Driver arrive và arrival handover.
- Receive count.
- Receive check.
- Submit putaway/final receive.
- Discrepancy incident và adjustment.
- Quarantine reject.
- Wrong-SKU report, approve/reject return.
- Return depart, return arrive, return handover và final source receive.

Audit payload phải đủ để dựng lại before/after header, item quantities, allocations, trip/resource state, QC result, photo refs, inventory movements, discrepancy ids và actor/timestamp.
