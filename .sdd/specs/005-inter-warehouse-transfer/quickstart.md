# Hướng dẫn nhanh: 005 Điều chuyển nội bộ giữa kho

## Điều kiện trước khi chạy

- Backend chạy với Java 21 và Spring Boot 3.4.5.
- Frontend chạy với React 18.
- Có user test cho từng vai trò: Planner, quản lý kho yêu cầu, quản lý kho nguồn, Dispatcher, công nhân nguồn, thủ kho nguồn, tài xế được gán, công nhân kho đích, thủ kho kho đích, quản lý kho đích. CEO chỉ cần cho màn xem/giám sát, không duyệt `TRQ`.
- Có kho Hải Phòng, Hà Nội, Hồ Chí Minh, một kho `IN_TRANSIT`, và ít nhất một vị trí quarantine đang hoạt động cho mỗi kho đích.
- Phiếu nhập nhà cung cấp `RN-*` nằm ở `/inbound/receipts`.
- Phiếu điều chuyển nội bộ `TRF-*` nằm ở `/inter-warehouse-transfers` và toàn bộ luồng nhận hàng phải ở trong module này.

## Luồng kiểm tra backend

1. Quản lý kho đang thiếu hàng tra cứu tồn liên kho:
   - `GET /api/v1/transfer-requests/stock-lookup`
   - Kỳ vọng trả về số lượng khả dụng read-only ở các kho active khác.
   - Tồn quarantine không được tính là khả dụng.

2. Tạo và gửi yêu cầu điều chuyển của quản lý kho:
   - `POST /api/v1/transfer-requests`
   - Gửi kho yêu cầu, kho nguồn, ngày cần hàng, lý do nghiệp vụ và các dòng hàng.
   - Khi status là `DRAFT`, kiểm `PUT /api/v1/transfer-requests/{id}` sửa được header/item.
   - Khi status là `DRAFT`, kiểm `POST /api/v1/transfer-requests/{id}/cancel` soft-cancel sang `CANCELLED`; không được xóa vật lý.
   - `POST /api/v1/transfer-requests/{id}/submit`
   - Kỳ vọng mã request dạng `TRQ-YYYYMMDD-####` và status `SUBMITTED`.

3. Trưởng kho nguồn duyệt, giữ hàng và Planner convert:
   - `POST /api/v1/transfer-requests/{id}/approve`
   - Kỳ vọng status `APPROVED`, `reserved_qty` nguồn tăng theo FIFO hợp lệ, không reserve một phần nếu thiếu hàng, và có template/thông báo cho Planner nguồn.
   - `POST /api/v1/transfer-requests/{id}/convert`
   - Kỳ vọng sinh đúng một `TRF-*` liên kết và request status `CONVERTED`.

4. Planner tạo phiếu điều chuyển:
   - `POST /api/v1/inter-warehouse-transfers`
   - Gửi `externalInstructionCode`, kho nguồn, kho đích, ngày chứng từ, ngày dự kiến và ít nhất một item.
   - Kỳ vọng mã transfer dạng `TRF-YYYYMMDD-####` và status `NEW`.

5. Sửa transfer khi còn `NEW`:
   - `GET /api/v1/inter-warehouse-transfers/{id}`
   - `PUT /api/v1/inter-warehouse-transfers/{id}`
   - Bỏ một item cũ khỏi payload để xóa item đó.
   - Kỳ vọng danh sách item cập nhật và có audit log.

6. Quản lý kho nguồn duyệt `TRF` thủ công nếu còn nhánh tạo phiếu trực tiếp:
   - `POST /api/v1/inter-warehouse-transfers/{id}/approve`
   - Kỳ vọng `reserved_qty` nguồn tăng và status thành `APPROVED` chỉ với phiếu chưa có reservation từ `TRQ`.
   - Với `TRF` sinh từ `TRQ`, không reserve lần hai; phiếu phải đủ điều kiện lập chuyến theo trạng thái sau convert.

7. Dispatcher gán chuyến:
   - `POST /api/v1/inter-warehouse-transfers/{id}/trip`
   - Dùng xe khả dụng và tài xế có scope kho chứa kho nguồn của transfer.
   - Kỳ vọng có một trip `TRANSFER` liên kết transfer và mã trip dạng `TTR-YYYYMMDD-####`.

8. Công nhân nguồn báo xếp, QC xuất, ship và bàn giao:
   - Công nhân nguồn gọi `POST /api/v1/inter-warehouse-transfers/{id}/source-load-report`.
   - Payload phải có mọi transfer item và `loadedQty` thực tế đã đặt lên xe.
   - `POST /api/v1/inter-warehouse-transfers/{id}/outbound-qc`
   - `POST /api/v1/inter-warehouse-transfers/{id}/ship`
   - `POST /api/v1/inter-warehouse-transfers/{id}/load-handover`
   - Outbound QC và load/handover bắt buộc có photo reference; không cần Barcode/QR scan.
   - Outbound QC phải bị chặn trước khi công nhân nguồn báo xếp.
   - UI phải giữ nút QC pass/fail disabled cho đến khi chọn/chụp ảnh.
   - Nếu outbound QC fail, UI chỉ hiện thao tác công nhân dỡ/xử lý lại/báo lại; load handover và driver departure vẫn bị chặn cho đến khi công nhân báo lại số lượng đúng và thủ kho QC pass.
   - Bước ship xác nhận đúng số lượng đã duyệt từ `loadedQty` cho mọi dòng.
   - Gửi thiếu hoặc thừa phải trả `SENT_QTY_MISMATCH`.

9. Hủy sau ship:
   - `POST /api/v1/inter-warehouse-transfers/{id}/cancel`
   - Kỳ vọng bị chặn cho đến khi `/unship`.
   - `POST /api/v1/inter-warehouse-transfers/{id}/unship`
   - Sau đó quản lý kho nguồn mới hủy được.

10. Tài xế được gán rời kho:
    - `POST /api/v1/inter-warehouse-transfers/{id}/depart`
    - Kỳ vọng `total/reserved` nguồn giảm, `IN_TRANSIT` tăng và status thành `IN_TRANSIT`.

11. Tài xế đến và kho nhận bàn giao:
   - `POST /api/v1/inter-warehouse-transfers/{id}/arrive`
   - `POST /api/v1/inter-warehouse-transfers/{id}/receiving-handover`
   - Thủ kho kho đích bấm xác nhận nhận bàn giao, không cần chọn/chụp ảnh.
   - Receive-count phải tiếp tục bị chặn trước khi cả hai mốc này được ghi nhận.

12. Công nhân kho đích đếm hàng:
   - `PUT /api/v1/inter-warehouse-transfers/{id}/receive-count`
   - Thiếu/thừa hoặc báo vấn đề phải có `issueReason` theo item.

13. Thủ kho kho đích kiểm nhận:
   - `PUT /api/v1/inter-warehouse-transfers/{id}/receive-check`
   - Tổng QC phải bằng `confirmedReceivedQty`.
   - Bin kho đích phải đủ sức chứa cho số lượng QC pass.
   - Chỉ bắt `checkerNote` khi số xác nhận khác số công nhân nhập.

14. Quản lý kho đích nhập kho cuối:
    - `POST /api/v1/inter-warehouse-transfers/{id}/final-receive`
    - Thiếu hàng bắt buộc có `discrepancyReason` và tạo `TRANSFER_DISCREPANCY`.
    - Số lượng vật lý QC fail chuyển vào quarantine với origin `INTERNAL_TRANSFER` và bàn giao cho Spec 009 xử lý disposal.
    - Số lượng thiếu không tạo quarantine stock.
    - Sai SKU không còn dùng Return to Source; kho nhận xử lý qua count/QC/chênh lệch hoặc quarantine theo trạng thái vật lý.
    - Quarantine stock có origin điều chuyển không được dùng supplier RTV.

15. Kiểm định giá trị shortage với 30 gửi và 28 nhận:
    - Kho đích chỉ nhận và tính giá trị cho 28 đơn vị.
    - `TRANSFER_DISCREPANCY` ghi 2 đơn vị thiếu chỉ theo số lượng; 2 đơn vị đó không có giá trị nhập kho đích.
    - Không tạo invoice, revenue, receivable, payable, supplier Debit Note hoặc tự động charge tài xế.

16. Quay đầu do quá hạn khi hàng đang vận chuyển:
    - Khi transfer quá deadline trong lúc `IN_TRANSIT`, hệ thống đánh dấu `isReturned = true` với lý do `TRANSFER_REQUIRED_DATE_EXPIRED`.
    - Kỳ vọng cùng transfer/trip/vehicle/driver và stock `IN_TRANSIT` vẫn active.
    - Tài xế được gán xác nhận `POST /api/v1/inter-warehouse-transfers/{id}/return-depart` và `POST /api/v1/inter-warehouse-transfers/{id}/return-arrive`.
    - Nhân viên kho nguồn receive-count, thủ kho nguồn receive-check/QC, quản lý kho nguồn final-receive.
    - Kỳ vọng terminal `COMPLETED` với nhãn UI “Đã hoàn về kho nguồn”.

## Luồng kiểm tra frontend

1. Quản lý kho yêu cầu tra cứu tồn khả dụng ở kho khác và bắt đầu transfer request từ bối cảnh thiếu hàng.
2. Khi request là `DRAFT`, kiểm card/detail hiển thị `Sua`, `Xoa`, `Gui truong kho nguon duyet`; `Sua` nạp request hiện tại vào form và `Xoa` soft-cancel sang `CANCELLED`.
3. Trưởng kho nguồn mở request đã submit và approve hoặc reject với reason; approve phải giữ hàng nguồn ngay.
4. Planner nguồn thấy template request đã duyệt/đã giữ hàng và convert thành `TRF`.
5. Planner mở workspace chung tại `/inter-warehouse-transfers` và tạo hoặc xem phiếu `TRF` thủ công.
6. Planner sửa transfer `NEW` và thấy item cũ được load sẵn, không phải form trắng.
7. Quản lý kho nguồn chỉ thấy action duyệt/từ chối cho transfer thuộc scope nguồn.
8. Dispatcher chỉ thấy action gán trip cho transfer approved có kho nguồn trong scope dispatcher.
9. Dispatcher chỉ chọn được xe và tài xế hợp lệ với scope kho nguồn.
10. Công nhân nguồn báo số lượng loaded theo item trước outbound QC.
11. Thủ kho nguồn không bấm được QC pass/fail nếu chưa chọn/chụp ảnh; QC pass mở ship/load handover, QC fail chỉ cho công nhân dỡ/xử lý lại/báo lại.
12. Thủ kho nguồn ship đúng số lượng duyệt từ `loadedQty` và thấy validation khi mismatch.
13. Tài xế chỉ thấy trip điều chuyển được gán trên màn chuyến của tài xế và chỉ depart được trip đó.
    - Danh sách tài xế dùng chung đặt tiêu đề `Chuyen xe cua toi`, không dùng wording chỉ dành cho giao hàng.
    - Chọn `Noi bo` hiển thị trip `TTR-*` và ẩn trip giao đại lý.
    - Card transfer hiển thị `Dieu chuyen noi bo` và tuyến kho nguồn -> kho đích thay vì `Diem giao`.
    - Detail transfer không expose POD, dealer OTP, dealer refusal, invoice hoặc confirm-delivery actions.
14. Nút xác nhận destination handover không yêu cầu ảnh; sau khi thủ kho kho đích xác nhận bàn giao, công nhân kho đích thấy ngay phần initial count trong module transfer, không vào danh sách phiếu nhập nhà cung cấp.
15. Return handover vẫn disabled cho đến khi chọn/chụp ảnh nếu hàng quay đầu về kho nguồn.
16. Thủ kho kho đích kiểm count/QC và chọn vị trí kho đích cho hàng pass.
17. Quản lý kho đích final-confirm completion/discrepancy trong cùng module transfer.
18. Quarantine Workspace hiển thị origin điều chuyển và chỉ cho disposal với hàng điều chuyển nội bộ bị hỏng.
19. Thủ kho kho đích thấy “Báo gửi nhầm SKU”; quản lý kho đích thấy approve/reject; không role nào thấy action nếu ngoài scope kho đích.
20. Sau approval, tài xế thấy lệnh quay đầu và các role kho nguồn thấy lại workflow nhận hàng 3 bước.

## Smoke test cho nhánh ngoại lệ

Chạy các case sau trước khi chấp nhận thay đổi transfer-flow:

1. Tạo/sửa `TRQ` với `neededByDate` quá khứ, dòng SKU trùng, số lượng lẻ, thiếu business reason, và số lượng yêu cầu vượt tồn nguồn. Kỳ vọng lỗi inline hoặc toast backend đã dịch; trạng thái `DRAFT` hiện có vẫn sửa được.
2. Tạo/sửa `TRF` thiếu external instruction, source/destination trùng, `documentDate` quá khứ, `plannedDate` quá khứ, `plannedDate < documentDate`, dòng SKU trùng, số lượng lẻ, product/warehouse inactive, và external instruction đang active bị trùng. Kỳ vọng mã lỗi backend ổn định kèm message tiếng Việt.
3. Duyệt `TRQ` khi tồn khả dụng nguồn thấp hơn requested quantity. Kỳ vọng lỗi `INSUFFICIENT_AVAILABLE_STOCK` hoặc tương đương; status vẫn `SUBMITTED`; không tạo partial reservation/allocation hoặc audit approval.
4. Gán trip với scope Dispatcher sai, tài xế không hợp lệ với kho nguồn, xe/tài xế unavailable, trùng lịch, time window sai, planned time quá khứ, deadline transfer hết hạn, và vượt capacity. Kỳ vọng reject trip mutation và không lock resource.
5. Submit source load thiếu item, trùng item, loaded quantity âm/lẻ hoặc quantity mismatch thiếu rework reason. Kỳ vọng lỗi source-load và chưa bật outbound QC.
6. Submit outbound QC hoặc handover khi chưa chọn/chụp ảnh bằng chứng. Kỳ vọng client disabled action và backend trả `TRANSFER_PHOTO_REQUIRED` nếu bypass.
7. Thử ship/depart trước load report, trước outbound QC pass, trước load handover, bằng sai tài xế hoặc sau khi trip assignment bị lock. Kỳ vọng validation đúng thứ tự và inventory không đổi.
8. Submit receive-count trước arrival/handover, có dòng item trùng, thiếu dòng item, số âm/lẻ, vượt sent quantity hoặc shortage thiếu reason. Kỳ vọng receive-count validation và chưa ghi tồn; handover kho đích không yêu cầu ảnh.
9. Submit receive-check với item trùng, checker quantity khác mà thiếu note, tổng QC sai, QC failure thiếu reason, QC failure mà không có quarantine bin active, chọn quarantine bin cho QC-passed goods, bin inactive/sai kho hoặc vượt capacity. Kỳ vọng message validation trực tiếp.
10. Submit final receive trước receive-check, thiếu cấu hình `IN_TRANSIT`, putaway item/location trùng, putaway quantity bằng 0/âm, putaway vượt QC-passed, hoặc putaway thiếu mà không có discrepancy reason. Kỳ vọng validation trước mọi mutation `IN_TRANSIT`/destination/quarantine inventory.
11. Submit return leg khi phiếu chưa `isReturned = true` hoặc chưa có mốc return trước đó. Kỳ vọng validation đúng thứ tự và không đổi tồn kho.
12. Kiểm frontend chỉ hiển thị tối đa một message rõ ràng cho mỗi action fail: inline field errors cho field biết trước, một toast đã dịch cho backend rejection, toast stack deduplicate và giới hạn để không chồng lấn, chỉ auto-refresh sau mutation thành công.

## Kiểm tra bắt buộc trước khi xem là xong code

- `mvn test` hoặc targeted backend tests pass.
- Backend controller/integration tests cover mọi transfer và transfer-request endpoint với happy path, validation failure, invalid state, authorization/scope failure và stale-version conflict khi áp dụng.
- PostgreSQL/Flyway tests chạy với migration set thật và chứng minh migration cộng thêm mới nhất chạy được từ database sạch.
- Frontend tests/build pass.
- Test source load report chứng minh outbound QC bị chặn trước khi công nhân báo `loadedQty`, và QC fail trả về worker rework trước khi QC được pass lại.
- Frontend tests cover mọi nút action chính trong transfer workspace: visible/enabled theo role-state, hidden/disabled theo role-state, click thành công, API fail response và refresh sau thành công.
- Ít nhất một smoke path frontend-to-backend pass từ tạo `TRQ` đến final receive, gồm trưởng kho nguồn duyệt giữ hàng ở `TRQ`, Planner convert `TRF`, outbound QC photo refs, load handover photo refs, arrival/handover không ảnh, receive-check, final receive, inventory movement và audit assertion.
- OpenAPI/Swagger expose mọi transfer endpoint.
- OpenAPI/Swagger expose transfer-request endpoint và cross-warehouse stock lookup.
- Audit log ghi mọi mutation transfer.
- Audit log ghi transfer-request create/submit/source-manager approval-reservation/rejection/conversion.
- Audit log ghi transfer-request update và soft-cancel `DRAFT`; delete action không được xóa vật lý lịch sử request.
- Frontend action yêu cầu ảnh disabled cho đến khi có ảnh chọn/chụp: outbound QC, load handover, return handover và driver POD upload. Destination arrival handover không yêu cầu ảnh.
- Không invariant tồn kho nào có thể âm.
- Shortage điều chuyển không bao giờ thành quantity quarantine/disposal.
- Quarantine stock có origin điều chuyển giữ traceability đến transfer item và không tạo RTV hoặc supplier Debit Note.
- Số lượng và giá trị tồn kho đích chỉ gồm hàng nhận vật lý và được chấp nhận.
- Quay đầu quá hạn yêu cầu assigned-driver return và source receiving ba bước.
- Real PostgreSQL/Flyway migration tests pass cho migration cộng thêm mới nhất.
- Có requirement-to-test mapping cho mọi P0 item và nhánh ngoại lệ trong `tasks.md`.
