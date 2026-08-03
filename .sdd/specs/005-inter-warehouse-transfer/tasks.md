# Task: 005 Sửa chữa luồng điều chuyển nội bộ giữa kho

**Đầu vào**: `.sdd/specs/005-inter-warehouse-transfer/spec.md`, `plan.md`, `data-model.md`, `contracts/openapi.yaml`, `quickstart.md`

**Cập nhật lần cuối**: 2026-07-22

**Mục đích**: Thay danh sách task cũ/trùng lặp bằng backlog sửa chữa có thể thực thi cho luồng điều chuyển nội bộ đúng production:
`TRQ draft -> submit -> source manager approve and reserve -> Planner convert once -> Dispatcher capacity/overlap plan -> công nhân nguồn pick/load/report loaded số lượng -> thủ kho nguồn QC xuất -> QC fail trả về công nhân để rework/re-report -> QC pass -> load/handover -> driver depart -> IN_TRANSIT -> driver arrive -> destination storekeeper handover confirm without photo -> blind count -> storekeeper count/QC/bin-capacity check -> manager final confirmation`.

**Quan trọng**: Không sửa, đổi tên hoặc xóa Flyway migration đã áp dụng. Mọi sửa schema phải dùng migration cộng thêm tiếp theo sau version mới nhất đã deploy.

## Bảng ánh xạ yêu cầu sang test

| Yêu cầu | Test tối thiểu bắt buộc |
|---|---|
| P0-C1 đồng bộ schema/code | PostgreSQL/Flyway migration test + create/reject/quarantine transfer integration test |
| P0-C2 FIFO eligible reservation | service test chứng minh đã loại vị trí quarantine/inactive/locked |
| P0-C3 QC xuất và bin capacity | service/controller test chứng minh chặn depart trước QC và chặn receive khi vượt capacity |
| P0-C4 optimistic locking/concurrency | test stale version cho transfer, request conversion, final receive và trip assignment |
| P0-C5 cấp dòng audit | test assert audit cho header, items, allocations, QC, trip và inventory movement |
| P0-C6 DB thật/frontend coverage | Testcontainers/Flyway test và frontend workflow test |
| Arrival/handover | receive-count bị chặn trước arrival/handover và được phép sau handover |
| Sai SKU detail | validation test cho expected SKU, actual SKU, dòng hàng, số lượng, lý do và photo refs tùy chọn |
| Trip capacity/reassignment/resource release | capacity exceed test, reassignment trước departure, lock sau departure, resource release guard |
| Overdue return-to-source | chỉ transfer `IN_TRANSIT` quá hạn mới được return, bắt buộc lý do, hỗ trợ photo refs khi có |
| Contract alignment | test/review OpenAPI path khớp với path controller |
| Nút action frontend | test hiển thị theo role/state, click thành công, API trả lỗi và refresh sau thành công cho mọi nút transfer chính |
| Transfer request edit/delete | backend service/controller test cho cập nhật `DRAFT` và soft-cancel; frontend test hiển thị nút và hành vi lưu/hủy modal |
| Photo-gated actions | UI test hoặc smoke thủ công chứng minh QC xuất, bàn giao xếp hàng, bàn giao khi quay đầu, và nút driver POD vẫn disabled cho đến khi chọn/chụp ảnh; bàn giao khi đến kho đích chỉ cần nút xác nhận |
| Source worker báo cáo xếp hàng trước QC xuất | service/controller/frontend test chứng minh QC bị chặn trước báo cáo xếp hàng, QC fail chặn handover/departure, công nhân báo lại thì QC được thử lại |
| Frontend-to-backend smoke | happy path full-stack từ `TRQ` đến final receive kèm assertion backend cho inventory, audit và trạng thái DB |
| Deploy gate | backend unit/controller/integration + DB thật migration + frontend test/build + backend compile phải pass toàn bộ |

## Giai đoạn 1: Đồng bộ tài liệu và contract

**Mục đích**: Làm cho tài liệu nguồn sự thật khớp luồng production mong muốn và tên controller hiện tại.

- [x] T001 Cập nhật `.sdd/specs/005-inter-warehouse-transfer/spec.md` với luồng chuẩn, invariant P0, arrival/handover, chặng return quá hạn, trip capacity và kỳ vọng requirement-to-test.
- [x] T002 Cập nhật `.sdd/specs/005-inter-warehouse-transfer/plan.md` trỏ tới đúng `InterWarehouseTransfer*` backend/frontend và thứ tự triển khai sửa chữa.
- [x] T003 Cập nhật `.sdd/specs/005-inter-warehouse-transfer/contracts/openapi.yaml` dùng `/api/v1/inter-warehouse-transfers`, `/approve`, `/final-receive`, chặng return quá hạn, và transfer-request `/approve|reject|convert`; endpoint request/approve/reject return do sai SKU đã được gỡ khỏi contract.
- [x] T004 [P] Cập nhật `.sdd/specs/005-inter-warehouse-transfer/data-model.md` với version fields, planned item batch nullable, QC xuất fields, timestamp arrival/handover, chặng return quá hạn, tổng capacity chuyến và entity discrepancy incident/hold.
- [x] T005 [P] Cập nhật `.sdd/specs/005-inter-warehouse-transfer/quickstart.md` với happy path đầy đủ, return path và checklist kiểm tra blocking-path.
- [x] T006 [P] Cập nhật feature docs trong `.sdd/specs/005-inter-warehouse-transfer/features/` để tài liệu shipment và receiving bao gồm QC xuất, bàn giao xếp hàng, driver arrival, bin capacity, xử lý sai SKU qua count/QC/discrepancy, và return leg quá hạn.

## Giai đoạn 2: Nền tảng database và concurrency

**Mục đích**: Sửa mismatch DB/runtime và rủi ro stale-write trước khi đổi hành vi nghiệp vụ.

- [x] T007 Tạo additive Flyway migration `backend/src/main/resources/db/migration/V6__inter_warehouse_transfer_hardening.sql` hoặc version khả dụng tiếp theo nếu `V6` đã tồn tại.
- [x] T008 Trong migration mới, thay transfer status check constraints để include `REJECTED` và `QUARANTINED` trên bảng điều chuyển nội bộ thực tế.
- [x] T009 Trong migration mới, cho planned transfer item `batch_id` nullable nhưng vẫn giữ traceability batch trên `inter_warehouse_transfer_allocations`.
- [x] T010 Trong migration mới, thêm version columns cho `inter_warehouse_transfers`, `inter_warehouse_transfer_items`, `transfer_requests`, và transfer trip/resource tables khi cần.
- [x] T011 Trong migration mới, thêm QC xuất photo refs, bàn giao xếp hàng photo refs, driver arrival, bàn giao khi đến, rời điểm nhận để quay đầu, và đến nguồn khi quay đầu vào schema transfer.
- [x] T012 Trong migration mới, thêm sai SKU report/report-item fields hoặc tables với expected product, actual product, số lượng, lý do, tùy chọn photo refs, status, reporter, và decision metadata.
- [x] T013 Trong migration mới, thêm calculated transfer trip weight/volume fields hoặc xác minh column trip hiện có tương thích.
- [x] T014 Trong migration mới, thêm discrepancy incident/hold data cần cho theo dõi thiếu hàng và nhận thừa vật lý.
- [x] T015 Thêm `@Version` và expose version DTO cho `backend/src/main/java/com/wms/entity/InterWarehouseTransfer.java`.
- [x] T016 Thêm `@Version` ở nơi cần cho `backend/src/main/java/com/wms/entity/InterWarehouseTransferItem.java`.
- [x] T017 Thêm `@Version` để `backend/src/main/java/com/wms/entity/TransferRequest.java`.
- [x] T018 Thêm xử lý stale-write và map 409 trong lớp xử lý exception dùng chung.
- [x] T019 Thêm PostgreSQL/Flyway migration integration test tại `backend/src/test/java/com/wms/db/InterWarehouseTransferMigrationIntegrationTest.java`.

## Giai đoạn 3: Reservation FIFO và toàn vẹn tồn kho

**Mục đích**: Đảm bảo duyệt tại nguồn chỉ reserve tồn hợp lệ để điều chuyển.

- [x] T020 Cập nhật `backend/src/main/java/com/wms/repository/InventoryRepository.java` query reservation để loại quarantine, inactive, locked, và wrong-warehouse locations.
- [x] T021 Cập nhật `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferApprovalService.java` để chỉ reserve allocation rows hợp lệ theo FIFO.
- [x] T022 Cập nhật `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferShippingService.java` để departure fail thay vì tự clamp reserved quantity không hợp lệ.
- [x] T023 Thêm service test tại `backend/src/test/java/com/wms/service/InterWarehouseTransferServiceImplTest.java` cho FIFO order, loại quarantine, loại inactive location và conflict reserved quantity.
- [x] T024 Thêm integration test chứng minh approval không reserve inventory mà tra cứu tồn liên kho đã phải ẩn.

## Giai đoạn 4: Duyệt request và convert một lần

**Mục đích**: Làm cho `TRQ` tin cậy khi approve/convert đồng thời.

- [x] T025 Cập nhật `backend/src/main/java/com/wms/dto/request/TransferRequestCreateRequest.java` và `TransferRequestUpdateRequest.java` để `neededByDate`, `businessReason`, observed quantities và lý do thiếu hàng khớp spec.
- [x] T026 Cập nhật `backend/src/main/java/com/wms/enums/TransferRequestStatus.java` dùng status đã tài liệu hóa hoặc thêm compatibility mapping nếu giữ legacy values.
- [x] T027 Cập nhật `backend/src/main/java/com/wms/service/transfer/impl/TransferRequestServiceImpl.java` để revalidate tồn khả dụng nguồn trước submit/source approval/conversion.
- [x] T028 Thêm guard unique một active transfer cho `transfer_request_id` trong migration mới và path convert repository/service.
- [x] T029 Thêm test stale conversion tại `backend/src/test/java/com/wms/service/TransferRequestServiceImplTest.java`.
- [x] T030 Thêm controller test tại `backend/src/test/java/com/wms/controller/TransferRequestControllerTest.java` cho path approve/reject/convert và duplicate conversion.

## Giai đoạn 5: Lập kế hoạch chuyến, capacity và vòng đời resource

**Mục đích**: Làm cho kế hoạch `TTR` khớp ràng buộc vận tải thực tế.

- [x] T031 Cập nhật `backend/src/main/java/com/wms/dto/request/InterWarehouseTransferTripAssignRequest.java` để mang planned start/end và version fields.
- [x] T032 Cập nhật `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferShippingService.java` để tính weight/volume chuyến từ số lượng item và metadata sản phẩm/đóng gói.
- [x] T033 Cập nhật trip assignment reject `TRIP_CAPACITY_EXCEEDED` khi tổng tính toán vượt capacity xe đã chọn.
- [x] T034 Cập nhật trip assignment cho phép đổi xe/tài xế/lịch trước departure và audit là `TRANSFER_TRIP_REASSIGN`.
- [x] T035 Cập nhật trip assignment reject reassignment sau departure với `TRANSFER_TRIP_LOCKED`.
- [x] T036 Cập nhật code terminal receive/quarantine để vehicle/driver chỉ release khi không còn assignment active khác.
- [x] T037 Thêm test cho trùng lịch, vượt capacity, reassign trước departure, lock sau departure và release resource có guard.

## Giai đoạn 6: QC xuất, bàn giao xếp hàng và rời kho

**Mục đích**: Ngăn hàng lỗi hoặc chưa xác minh rời kho nguồn.

- [x] T038 Thêm QC xuất request/response DTOs với photo refs bắt buộc và không yêu cầu Barcode/QR trong `backend/src/main/java/com/wms/dto/request/`.
- [x] T039 Thêm QC xuất fields để `backend/src/main/java/com/wms/dto/response/InterWarehouseTransferResponse.java`.
- [x] T040 Triển khai `recordOutboundQc` có xác nhận ảnh tại `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferShippingService.java`.
- [x] T041 Triển khai xác nhận ảnh source load/handover trước departure tại `InterWarehouseTransferShippingService.java`.
- [x] T042 Cập nhật `departTransfer` để bắt buộc QC xuất pass, đã ghi shipment, đã ghi bàn giao xếp hàng, đúng tài xế được gán và version hợp lệ.
- [x] T043 Thêm endpoint trong `backend/src/main/java/com/wms/controller/InterWarehouseTransferController.java` cho QC xuất và bàn giao xếp hàng.
- [x] T044 Thêm audit actions tại `backend/src/main/java/com/wms/enums/AuditAction.java` cho `TRANSFER_OUTBOUND_QC` và `TRANSFER_LOAD_HANDOVER`.
- [x] T045 Thêm test chặn ship/depart khi thiếu QC xuất, QC fail hoặc thiếu photo refs bắt buộc.

## Giai đoạn 7: Đến nơi, bàn giao, nhận hàng và sức chứa bin

**Mục đích**: Đảm bảo kho đích/kho nguồn chỉ nhận sau khi xe đến vật lý và ngăn bin bị vượt sức chứa.

- [x] T046 Thêm endpoint arrival và arrival-handover trong `backend/src/main/java/com/wms/controller/InterWarehouseTransferController.java`.
- [x] T047 Triển khai driver arrival và receiving-warehouse handover tại `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferShippingService.java`.
- [x] T048 Cập nhật `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferReceivingService.java` để chặn receive-count cho đến khi có arrival và handover.
- [x] T049 Cập nhật receive-check/final-receive để kiểm capacity bin kho đích cho số lượng QC pass trước khi ghi tồn.
- [x] T050 Thêm xử lý discrepancy incident/hold cho nhận thừa vật lý; phần thừa vẫn được cất theo count thực nhận và trace trách nhiệm bằng hold.
- [x] T051 Thêm audit actions cho `TRANSFER_ARRIVE` và `TRANSFER_ARRIVAL_HANDOVER`.
- [x] T052 Thêm test cho nhận trước arrival bị chặn, nhận sau handover được phép, vượt bin capacity và hold discrepancy nhận thừa.

## Giai đoạn 8: Quay đầu quá hạn

**Mục đích**: Giữ chặng quay đầu vật lý cho phiếu quá hạn và gỡ nhánh yêu cầu quay đầu thủ công do sai SKU tại kho đích.

- [x] T053 Gỡ request/approval return do sai SKU khỏi runtime Sprint 1; sai SKU tại kho đích tiếp tục qua count/QC/discrepancy/quarantine theo trạng thái vật lý.
- [x] T054 Cập nhật response/frontend để không hiển thị nhánh yêu cầu quay đầu do sai SKU.
- [x] T055 Cập nhật `InterWarehouseTransferReceivingService.java` để chỉ giữ nhận hàng và nhận hàng quay đầu quá hạn.
- [x] T056 Gỡ approve/reject return do sai SKU khỏi service/controller/API contract.
- [x] T057 Thêm rời điểm nhận để quay đầu và source đến nguồn khi quay đầu/handover service methods.
- [x] T058 Chặn source return receive-count cho đến khi đã ghi return depart và return arrive/handover.
- [x] T059 Cập nhật overdue `returnToSource` để require trip thật sự quá hạn, lý do không rỗng và photo refs tùy chọn khi có.
- [x] T060 Thêm endpoint cho rời điểm nhận để quay đầu và đến nguồn khi quay đầu/handover trong `InterWarehouseTransferController.java`.
- [x] T061 Thêm audit actions cho `TRANSFER_RETURN_DEPART`, `TRANSFER_RETURN_ARRIVE`, và `TRANSFER_RETURN_HANDOVER`.
- [x] T062 Thêm test cho chặn nhận return trước khi về nguồn, bắt buộc lý do overdue return và xác nhận UI không còn nhánh duyệt quay đầu do sai SKU.

## Giai đoạn 9: Audit và truy vết incident

**Mục đích**: Đảm bảo audit log dựng lại được đầy đủ mutation tồn kho và vận tải.

- [x] T063 Cập nhật `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferHelper.java` logic snapshot audit để bao gồm header, items, allocations, QC quantities, trip/resource state, return leg quá hạn, và inventory movement references.
- [x] T064 Đảm bảo `TRANSFER_DISCREPANCY_CREATE` audit bao gồm số lượng thiếu, product, warehouse, transfer item, adjustment id, và lý do.
- [x] T065 Đảm bảo quarantine rejection audit bao gồm kho đích, quarantine bin, affected item quantities, và transfer-origin references.
- [x] T066 Thêm audit test cho approve, QC xuất, depart, arrival/handover, receive-check, final-receive, overdue return, và quarantine reject.

## Giai đoạn 10: API contract và coverage controller backend

**Mục đích**: Giữ OpenAPI, controller và URL frontend service đồng bộ.

- [x] T067 Cập nhật Swagger/OpenAPI annotation trong `backend/src/main/java/com/wms/controller/InterWarehouseTransferController.java` cho mọi transfer endpoint.
- [x] T068 Cập nhật Swagger/OpenAPI annotation trong `backend/src/main/java/com/wms/controller/TransferRequestController.java` cho `/approve`, `/reject`, `/convert` và `/stock-lookup`.
- [x] T069 Thêm controller test cho `/api/v1/inter-warehouse-transfers/{id}/approve`, `/final-receive`, và xác nhận các endpoint request/approve/reject return do sai SKU không còn trong contract runtime.
- [x] T070 Thêm controller test cho endpoint QC xuất, bàn giao xếp hàng, arrival/handover, return depart và return arrive mới.
- [x] T071 Thêm contract smoke test hoặc bước review có tài liệu chứng minh `.sdd/.../contracts/openapi.yaml` tên path match path controller.

## Giai đoạn 11: Workflow frontend

**Mục đích**: Hiển thị control mới nhưng không cho người dùng chạy sai thứ tự bước.

- [x] T072 Cập nhật `frontend/src/services/inter-warehouse-transfer.service.js` với QC xuất, bàn giao xếp hàng, arrival/handover, rời điểm nhận để quay đầu, đến nguồn khi quay đầu, và expanded sai SKU APIs.
- [x] T073 Cập nhật `frontend/src/pages/InterWarehouseTransfer/InterWarehouseTransferActionPanel.jsx` để hiển thị QC xuất trước ship/depart.
- [x] T074 Cập nhật `InterWarehouseTransferActionPanel.jsx` để hiển thị bàn giao xếp hàng trước driver departure.
- [x] T075 Cập nhật `InterWarehouseTransferActionPanel.jsx` để hiển thị driver arrival và bàn giao khi đến trước receive-count.
- [x] T076 Cập nhật `InterWarehouseTransferActionPanel.jsx` để chặn/ẩn action nhận hàng cho đến khi hoàn tất arrival/handover.
- [x] T077 Cập nhật `InterWarehouseTransferActionPanel.jsx` form sai SKU để thu thập dòng hàng, expected SKU, actual SKU, affected số lượng, lý do, và tùy chọn photo refs.
- [x] T078 Cập nhật `InterWarehouseTransferActionPanel.jsx` để hiển thị trạng thái return depart và return arrive/handover cho return đã duyệt.
- [x] T079 Cập nhật `frontend/src/pages/InterWarehouseTransfer/TransferRequestWorkspace.jsx` để hiển thị `neededByDate`, lý do nghiệp vụ, tồn quan sát ở nguồn/kho yêu cầu và trạng thái convert một lần.
- [x] T080 Cập nhật `frontend/src/utils/interWarehouseTransferStatus.js` và `InterWarehouseTransferStatusBadge.jsx` cho nhãn status/action mới.
- [x] T081 Thêm frontend test cho service URL paths tại `frontend/src/services/inter-warehouse-transfer.service.test.js`.
- [x] T082 Thêm frontend workflow test cho thứ tự/khả năng hiển thị action tại `frontend/src/pages/InterWarehouseTransfer/`.

## Giai đoạn 12: Kiểm chứng end-to-end và quality gate

**Mục đích**: Chứng minh luồng đã siết hoạt động đúng trên các biên thực tế.

- [x] T083 Thêm Testcontainers PostgreSQL hoặc integration test DB thật tương đương cho Flyway + core transfer flow.
- [x] T084 Thêm happy-path integration test từ `TRQ` đến final receive với arrival/handover và kiểm bin capacity.
- [x] T085 Thêm manual `TRF` happy-path integration test từ planner tạo phiếu đến final receive.
- [x] T086 Thêm exception-path test cho thiếu hàng incident + adjustment, nhận thừa hold, QC fail để Quarantine, sai SKU qua discrepancy/quarantine, và overdue return.
- [x] T087 Chạy targeted backend test cho transfer services/controllers và migration test.
- [x] T088 Chạy `mvn compile` cho backend.
- [x] T089 Chạy frontend test/build cho module điều chuyển nội bộ.
- [x] T090 Cập nhật `.sdd/specs/005-inter-warehouse-transfer/quickstart.md` với command đã xác minh cuối cùng và rủi ro còn lại đã biết.
- [x] T091 Thêm backend endpoint coverage matrix test cho mọi `InterWarehouseTransferController` action in `backend/src/test/java/com/wms/controller/InterWarehouseTransferControllerTest.java`.
- [x] T092 Thêm backend endpoint coverage matrix test cho mọi `TransferRequestController` action in `backend/src/test/java/com/wms/controller/TransferRequestControllerTest.java`.
- [x] T093 Thêm service unhappy-path matrix test cho chuyển trạng thái sai, thiếu ảnh bắt buộc, thiếu lý do, warehouse scope sai, role sai, thiếu arrival/handover và stale version trong `backend/src/test/java/com/wms/service/InterWarehouseTransferServiceImplTest.java`.
- [x] T094 Thêm frontend action-nút coverage test cho mọi transfer workspace nút in `frontend/src/pages/InterWarehouseTransfer/InterWarehouseTransferActionPanel.test.jsx`.
- [x] T095 Thêm frontend transfer-request nút coverage test cho create, submit, approve, reject, convert, validation failure, API failure và refresh state in `frontend/src/pages/InterWarehouseTransfer/TransferRequestWorkspace.test.jsx`.
- [x] T096 Thêm smoke frontend-to-backend test cho `TRQ -> source manager approve/reserve -> planner convert -> trip -> QC xuất photo -> ship -> bàn giao xếp hàng photo -> depart -> arrive -> handover không ảnh -> receive-count -> receive-check -> final-receive`.
- [x] T097 Thêm smoke frontend-to-backend test cho overdue return bao gồm rời điểm nhận để quay đầu, source arrival/handover, và final receive tại nguồn; nhánh sai SKU return đã được loại khỏi smoke runtime.
- [x] T098 Thêm smoke frontend-to-backend test cho blocker deploy unhappy: invalid driver scope, overloaded trip, missing QC xuất photo, receive trước arrival, bin capacity exceeded, và stale version conflict.
- [x] T099 Thêm Tài liệu xác minh CI/deploy ghi rõ command bắt buộc cho backend test, DB migration test, frontend test, frontend build, backend compile, và full-stack smoke test.
- [x] T100 Chặn đánh dấu spec 005 deploy-ready cho đến khi mọi dòng requirement-to-test trong file này có tham chiếu test pass được ghi trong `.sdd/specs/005-inter-warehouse-transfer/quickstart.md`.
- [x] T101 Thêm backend `POST /api/v1/transfer-requests/{id}/cancel` endpoint soft-cancel và service method để UI `Xoa` không bao giờ xóa vật lý lịch sử request.
- [x] T102 Thêm backend service/controller test cho soft-cancel transfer request `DRAFT -> CANCELLED` và reject hủy non-DRAFT.
- [x] T103 Cập nhật `frontend/src/pages/InterWarehouseTransfer/TransferRequestWorkspace.jsx` để hiển thị `Sua`/`Xoa` trên card request DRAFT và detail modal, tái dùng form tạo để sửa, rồi refresh sau update/cancel.
- [x] T104 Thêm component frontend chọn/chụp ảnh dùng chung và dùng cho QC xuất, bàn giao khi đến, bàn giao khi quay đầu và bằng chứng POD tài xế.
- [x] T105 Chặn mọi nút action bắt buộc ảnh để chúng disabled cho đến khi đã chọn hoặc chụp ảnh.
- [x] T106 Cập nhật spec 005, feature docs, OpenAPI contract, và CLAUDE swimlane notes cho sửa/soft-delete request DRAFT và xác nhận bị gate bằng ảnh.
- [X] T107 Cập nhật danh sách chuyến tài xế dùng chung integration để `TTR-*` row expose `tripType = TRANSFER`, `tripTypeLabel = Dieu chuyen noi bo`, tuyến nguồn/đích, và số dòng transfer trong `frontend/src/pages/Outbound/DriverTrip.jsx`.
- [X] T108 Thêm frontend test coverage chứng minh bộ lọc `Noi bo` chỉ hiển thị `TTR-*` transfer trips và ẩn action POD/OTP đại lý trong `frontend/src/pages/Outbound/DriverTrip.test.jsx`.
- [X] T109 Thêm coverage backend hoặc service mapping chứng minh trip điều chuyển đã gán không hiển thị cho tài xế khác trong `backend/src/test/java/com/wms/service/InterWarehouseTransferServiceImplTest.java` hoặc `DriverDeliveryServiceImplTest.java`.
- [X] T110 Cập nhật `.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod/contracts/driver-pod.openapi.yaml` và backend Swagger annotations nếu thêm field tóm tắt transfer vào response chuyến tài xế dùng chung.

## Giai đoạn 13: Công nhân nguồn báo cáo xếp hàng trước QC xuất

**Mục đích**: Tách trách nhiệm xếp hàng vật lý khỏi QC xuất tại nguồn: công nhân báo số lượng loaded thực tế trước, và nếu QC fail thì quay lại công nhân xử lý/báo lại trước khi thủ kho QC lại.

- [x] T111 Cập nhật additive Flyway migration hoặc tạo migration tiếp theo cho các field báo cáo xếp hàng tại nguồn: transfer header `source_loaded_reported_by`, `source_loaded_reported_at`, `source_load_rework_required`, `source_load_rework_lý do`, và transfer item `loaded_qty`, `loaded_reported_by`, `loaded_reported_at` trong `backend/src/main/resources/db/migration/`.
- [x] T112 Thêm source báo cáo xếp hàng request/response DTO trong `backend/src/main/java/com/wms/dto/request/` và `backend/src/main/java/com/wms/dto/response/`, bắt buộc cấp item `transferItemId` và `loadedQty`.
- [x] T113 Cập nhật `backend/src/main/java/com/wms/entity/InterWarehouseTransfer.java` và `backend/src/main/java/com/wms/entity/InterWarehouseTransferItem.java` với field báo cáo xếp hàng tại nguồn và mapping an toàn version.
- [x] T114 Thêm `TRANSFER_SOURCE_LOAD_REPORT` và `TRANSFER_SOURCE_LOAD_REWORK` vào `backend/src/main/java/com/wms/enums/AuditAction.java`.
- [x] T115 Triển khai `recordSourceLoadReport` trong `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferShippingService.java`, scope cho công nhân/nhân viên nguồn, bắt buộc `APPROVED`, lưu loaded quantity cấp item và xóa marker rework sau khi báo cáo đã sửa.
- [x] T116 Cập nhật QC xuất logic trong `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferShippingService.java` để bắt buộc có loaded quantity của công nhân nguồn trước QC, reject QC pass khi loaded quantity khác planned quantity và set rework khi QC fail.
- [x] T117 Cập nhật guard shipment/load-handover/departure trong `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferShippingService.java` để chặn khi thiếu báo cáo xếp hàng tại nguồn hoặc còn yêu cầu rework.
- [x] T118 Thêm `POST /api/v1/inter-warehouse-transfers/{id}/source-load-report` để `backend/src/main/java/com/wms/controller/InterWarehouseTransferController.java` với validation và Swagger/OpenAPI annotations.
- [x] T119 Cập nhật Swagger/OpenAPI annotations sinh ra và test đồng bộ contract cho báo cáo xếp hàng tại nguồn, `SOURCE_LOAD_REPORT_REQUIRED`, và `SOURCE_LOAD_REWORK_REQUIRED`.
- [x] T120 Thêm backend service/controller test tại `backend/src/test/java/com/wms/service/InterWarehouseTransferServiceImplTest.java` và `backend/src/test/java/com/wms/controller/InterWarehouseTransferControllerTest.java` cho happy path báo cáo xếp hàng, chặn QC trước report, QC fail set rework, chặn handover/depart khi rework, re-report xóa rework và mismatch reject QC pass.
- [x] T121 Cập nhật `frontend/src/services/inter-warehouse-transfer.service.js` với API báo cáo xếp hàng tại nguồn.
- [x] T122 Cập nhật `frontend/src/pages/InterWarehouseTransfer/InterWarehouseTransferActionPanel.jsx` để hiển thị công nhân nguồn báo cáo xếp hàng trước QC xuất và sau QC fail chỉ hiển thị action dỡ/xử lý lại/báo lại trước khi QC retry.
- [x] T123 Cập nhật `frontend/src/utils/interWarehouseTransferStatus.js` và `frontend/src/pages/InterWarehouseTransfer/InterWarehouseTransferStatusBadge.jsx` với nhãn cho `Chờ công nhân xếp/báo số lượng`, `Chờ QC xuất`, và `Cần xử lý lại hàng xếp`.
- [x] T124 Thêm frontend workflow test tại `frontend/src/pages/InterWarehouseTransfer/InterWarehouseTransferActionPanel.test.jsx` chứng minh QC disabled trước báo cáo xếp hàng, QC fail ẩn handover/departure và hiển thị rework, re-report bật QC retry.
- [x] T125 Cập nhật `docs/overview/features-summary.md`, `docs/overview/user-stories.md`, `docs/overview/actors.md`, `README.md`, và `CLAUDE.md` với flow công nhân nguồn báo cáo xếp hàng trước QC xuất.

## Phụ thuộc

- Giai đoạn 2 chặn các giai đoạn hành vi backend vì schema và optimistic locking phải có trước.
- Giai đoạn 3 phải hoàn thành trước khi tin được test departure/final receive.
- Giai đoạn 5 và Giai đoạn 6 phải hoàn thành trước receiving gate Giai đoạn 7.
- Giai đoạn 8 phụ thuộc khái niệm arrival/handover của Giai đoạn 7.
- Giai đoạn 11 phụ thuộc hình dạng backend contract từ Giai đoạn 6-10.
- Giai đoạn 12 là acceptance gate cuối cùng.
- Giai đoạn 13 phải triển khai trước khi xem code khớp quyết định QC xuất ngày 2026-07-22; nó chạm hành vi Giai đoạn 6 và Giai đoạn 11 nên cần refresh test backend/frontend.
- Không task triển khai backend hoặc frontend nào được xem là xong nếu chưa thêm/cập nhật test happy-path và unhappy-path tương ứng trong cùng slice.

## Chiến lược triển khai

1. Đưa migration và DB integration test vào trước.
2. Siết invariant backend theo lát nhỏ: reservation, concurrency, trip, QC xuất, arrival/receiving, return leg, audit.
3. Cập nhật OpenAPI và controller test khi từng endpoint thành thật.
4. Cập nhật frontend chỉ sau khi DTO/API backend ổn định.
5. Chỉ xem task hoàn thành khi dòng requirement-to-test tương ứng có bằng chứng pass.
6. Trước deploy, chạy toàn bộ test gate: backend unit/controller/integration, PostgreSQL/Flyway migration test, frontend test, frontend build, backend compile và smoke flow full-stack.
