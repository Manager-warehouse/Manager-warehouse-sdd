# Kế hoạch triển khai: 005 Điều chuyển nội bộ giữa kho

**Branch**: `feat/son-005` | **Ngày**: 2026-07-22 | **Spec**: [spec.md](./spec.md)

**Đầu vào**: Đặc tả tính năng từ `.sdd/specs/005-inter-warehouse-transfer/spec.md`

## Tóm tắt

Triển khai và siết chặt luồng điều chuyển nội bộ Sprint 1 thành workflow riêng `TRF`/`TTR`, tách khỏi phiếu nhập nhà cung cấp `RN`. Luồng mục tiêu là: `TRQ draft -> submit -> source manager approve and reserve -> Planner convert once -> Dispatcher capacity/overlap plan -> source worker pick/load/report loaded quantities -> source storekeeper photo-confirmed outbound QC -> QC failed returns to worker rework/re-report -> QC passed -> photo-confirmed load/handover -> driver depart -> IN_TRANSIT -> driver arrive -> destination storekeeper handover confirm without required photo -> blind count -> storekeeper QC/bin-capacity check without editing count -> manager final confirmation`. Planner cũng có thể tạo `TRF` thủ công từ lệnh điều phối ngoài. Hàng hỏng vật lý trong điều chuyển được bàn giao sang Spec 009 để xử lý tiêu hủy với traceability đến transfer item. Thiếu hàng tạo incident/discrepancy và adjustment chỉ theo số lượng; nhận thừa vẫn được cất đủ theo count vật lý của công nhân và đồng thời giữ discrepancy incident cho phần vượt số gửi để quản lý/CEO xử lý sau. Sai SKU nhưng hàng còn nguyên vẹn phải có chi tiết từng dòng expected/actual SKU, số lượng ảnh hưởng, photo ref tùy chọn, quyết định của quản lý kho đích, mốc xe quay đầu/rời về/đến nguồn và nhận lại ở kho nguồn. Triển khai phải giữ invariant tồn kho, RBAC theo kho, audit bất biến ở cấp dòng, xử lý quarantine, không tồn âm, chống ghi đè cạnh tranh, tương thích PostgreSQL/Flyway và không thêm Barcode/QR trong phạm vi Sprint 1.

Màn danh sách chuyến cho tài xế `TTR-*` dùng chung với Spec 004. Tóm tắt chuyến điều chuyển phải có `tripType = TRANSFER`, `tripTypeLabel = Dieu chuyen noi bo`, tuyến kho nguồn/kho đích, số dòng hàng, xe, lịch chạy, trạng thái và trọng lượng để bộ lọc `Noi bo` chỉ hiển thị việc điều chuyển nội bộ mà không lộ action POD/OTP của giao đại lý.

## Bối cảnh kỹ thuật

**Ngôn ngữ/Phiên bản**: Java 21, JavaScript với React 18

**Phụ thuộc chính**: Spring Boot 3.4.5, Spring Data JPA/Hibernate, Jakarta Validation, Spring Security JWT, OpenAPI/Swagger, React 18, Tailwind CSS 3.x, axios

**Lưu trữ**: PostgreSQL 18 với Flyway migrations; không dùng raw SQL trong application code ngoài migration

**Kiểm thử**: JUnit 5 + Mockito cho service, Spring MVC/integration tests cho endpoint, Jest cho frontend

**Nền tảng mục tiêu**: Full-stack web app với REST API dưới `/api/v1`

**Loại dự án**: Web application gồm Spring Boot backend và React frontend

**Mục tiêu hiệu năng**: Các mutation tạo/duyệt/rời kho/nhận điều chuyển hoàn tất trong 2 giây với dữ liệu Sprint 1 thông thường; truy vấn `IN_TRANSIT` đủ realtime cho màn vận hành.

**Ràng buộc**: Không tồn âm; kiểm optimistic locking/version trên inventory, transfer, transfer request và trip/resource updates; mọi mutation điều chuyển phải audit before/after cấp dòng; bắt buộc role + warehouse scope; xem tồn liên kho chỉ read-only; Quản lý kho nguồn duyệt request và giữ hàng ngay, CEO chỉ xem/giám sát; reservation kho nguồn chỉ lấy hàng FIFO hợp lệ từ vị trí active, không quarantine; công nhân nguồn phải báo số lượng xếp trước outbound QC; outbound QC fail phải chặn handover/departure và trả về công nhân xử lý lại/báo lại trước khi QC pass; không thêm serial/expiry/grade từng đơn vị; hàng quarantine từ điều chuyển nội bộ không dùng supplier RTV; thiếu hàng không được biến thành tồn quarantine; điều chuyển nội bộ không tạo hóa đơn/khoản phải thu; wrong-SKU return tại kho đích đã được gỡ khỏi runtime Sprint 1; Flyway migration đã áp dụng là bất biến, mọi sửa schema dùng migration cộng thêm tiếp theo.

**Ràng buộc danh sách tài xế dùng chung**: Danh sách chuyến điều chuyển chỉ được đọc và không được mutate transfer status, trip status, inventory, resource assignment hoặc audit logs. Danh sách dùng chung có thể lọc cục bộ bằng `tripType`; khi mở trip `TRANSFER` vẫn phải dùng action của Spec 005, không dùng action giao đại lý của Spec 004.

**Quy mô/Phạm vi**: Ba kho vật lý, một kho ảo `IN_TRANSIT`, mỗi kho đích có một quarantine location, phiếu nhiều item, mỗi phiếu có một chuyến riêng.

## Kiểm tra Constitution

*GATE: Phải đạt trước khi triển khai.*

| Nguyên tắc | Trạng thái | Cách đáp ứng thiết kế |
|-----------|--------|-----------------|
| Layered Architecture | PASS | Backend tách Controller -> Service -> Repository -> Entity/DTO/Mapper; frontend tách service/pages/components. |
| Inventory Integrity | PASS | Transfer service sở hữu reservation FIFO hợp lệ, trừ nguồn, cộng/trừ `IN_TRANSIT`, cộng kho đích/quarantine, tạo discrepancy incident/adjustment và audit before/after cấp dòng. |
| Inventory Selection Principle | PASS | Reservation dùng FIFO allocation và loại quarantine, inactive, locked hoặc vị trí nguồn không khả dụng. Planned transfer item có thể `batch_id` nullable vì allocation rows giữ batch thật sau approval. |
| QC Gate & Quarantine | PASS | Bắt công nhân nguồn báo số lượng xếp trước outbound QC; outbound QC bắt buộc trước handover/departure; QC fail trả về công nhân rework/re-report trước khi QC pass. Receive-check bắt tổng QC, lý do lỗi, kiểm quarantine và sức chứa bin kho đích. Final receive đưa hàng QC fail vật lý vào quarantine với origin điều chuyển nội bộ; Spec 009 xử lý disposal và chặn RTV. |
| In-Transit Tracking | PASS | Depart chuyển tồn nguồn sang `IN_TRANSIT`; phải có driver arrival/handover trước khi nhận; final receive chỉ clear `IN_TRANSIT` sau xác nhận kho đích hoặc xác nhận nhận lại nguồn khi return. |
| Auth & RBAC | PASS | Task bao gồm kiểm role và scope kho nguồn/kho đích cho mọi mutation; quản lý kho xem tồn liên kho read-only và chỉ tạo request trong kho được phân công. |
| Test Coverage | PASS | Task bao gồm unit, controller, PostgreSQL/Flyway integration và frontend workflow tests, map tới từng requirement P0 và nhánh ngoại lệ. |

## Cấu trúc dự án

### Tài liệu

```text
.sdd/specs/005-inter-warehouse-transfer/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
├── tasks.md
└── features/
    ├── feature-planner-transfer-planning.md
    ├── feature-warehouse-manager-transfer-request.md
    ├── feature-storekeeper-transfer-ship.md
    └── feature-storekeeper-transfer-receive.md
```

### Mã nguồn

```text
backend/src/main/java/com/wms/
├── controller/InterWarehouseTransferController.java
├── controller/TransferRequestController.java
├── dto/request/InterWarehouseTransfer*.java
├── dto/request/TransferRequest*.java
├── dto/response/InterWarehouseTransfer*.java
├── dto/response/TransferRequest*.java
├── entity/InterWarehouseTransfer.java
├── entity/InterWarehouseTransferItem.java
├── entity/InterWarehouseTransferAllocation.java
├── entity/TransferItem.java
├── entity/TransferRequest.java
├── entity/TransferRequestItem.java
├── enums/InterWarehouseTransferStatus.java
├── enums/TransferRequestStatus.java
├── enums/AuditAction.java
├── repository/InterWarehouseTransferRepository.java
├── repository/InterWarehouseTransferItemRepository.java
├── repository/InterWarehouseTransferAllocationRepository.java
├── repository/TransferRequestRepository.java
├── mapper/InterWarehouseTransferMapper.java
├── service/transfer/InterWarehouseTransferService.java
├── service/TransferRequestService.java
├── service/transfer/impl/InterWarehouseTransferServiceImpl.java
├── service/transfer/impl/InterWarehouseTransferPlanningService.java
├── service/transfer/impl/InterWarehouseTransferApprovalService.java
├── service/transfer/impl/InterWarehouseTransferShippingService.java
├── service/transfer/impl/InterWarehouseTransferReceivingService.java
├── service/transfer/impl/InterWarehouseTransferHelper.java
└── service/transfer/impl/TransferRequestServiceImpl.java

backend/src/test/java/com/wms/
├── controller/InterWarehouseTransferControllerTest.java
├── service/InterWarehouseTransferServiceImplTest.java
├── service/InterWarehouseTransferFlowE2ETest.java
└── db/InterWarehouseTransferMigrationIntegrationTest.java

frontend/src/
├── services/inter-warehouse-transfer.service.js
├── pages/InterWarehouseTransfer/
│   ├── InterWarehouseTransferWorkspace.jsx
│   ├── InterWarehouseTransferActionPanel.jsx
│   ├── TransferRequestWorkspace.jsx
│   └── InterWarehouseTransferStatusBadge.jsx
├── utils/interWarehouseTransferStatus.js
└── routes/AppRoutes.jsx
```

**Quyết định cấu trúc**: Giữ code điều chuyển trong các controller/service/entity `InterWarehouseTransfer*` hiện có và module frontend `frontend/src/pages/InterWarehouseTransfer`. Frontend hiện dùng một workspace chung với action panel theo role và trạng thái, thay vì tách thành nhiều trang planner/ship/receive. Nhận hàng điều chuyển nội bộ vẫn nằm trong module transfer này và không nhập chung vào màn nhập nhà cung cấp `RN`.

## Phụ lục kế hoạch sửa chữa

Lượt triển khai tiếp theo PHẢI ưu tiên các blocker này trước khi polish tính năng:

1. Thêm Flyway migration cộng thêm sau migration mới nhất đã deploy để đồng bộ constraint status điều chuyển, cho planned item `batch_id` nullable, thêm version columns, chi tiết wrong-SKU report, timestamp arrival/handover, outbound QC fields, trọng lượng/thể tích tính toán của trip và dữ liệu discrepancy incident/hold. Không sửa `V1`-`V5`.
2. Siết reservation để approval chỉ lock/reserve inventory FIFO hợp lệ từ vị trí active, không quarantine.
3. Thêm optimistic locking và xử lý stale-write trên transfer, transfer request, trip/resource và inventory mutations.
4. Thêm source worker load/report, vòng outbound QC rework, load/handover, driver arrival/handover, return departure/arrival và receive gating.
5. Thêm tính capacity chuyến và chỉ cho đổi driver/vehicle/trip trước departure.
6. Thêm kiểm sức chứa bin kho đích trước khi ghi tồn QC-passed.
7. Mở rộng lưu wrong-SKU report đến cấp dòng: expected/actual SKU, quantity, reason và photo refs tùy chọn.
8. Giới hạn overdue return-to-source cho trip thật sự quá hạn và bắt buộc reason; hỗ trợ photo refs nếu có.
9. Cập nhật OpenAPI dùng đúng `/api/v1/inter-warehouse-transfers`, `/approve` và `/final-receive` như code triển khai.
10. Thay task tracking cũ bằng danh sách task sạch có map requirement-to-test.
11. Xem testing là deploy gate: mọi nút frontend chính và endpoint backend phải có coverage happy, unhappy, authorization/scope, invalid-state và stale/concurrency khi áp dụng, cộng ít nhất một smoke path frontend-to-backend.

## Theo dõi độ phức tạp

Không có vi phạm constitution dự kiến. Tính năng lớn và nên triển khai thành các lát nhỏ, test được, vì chạm vào tồn kho, tài nguyên chuyến, an toàn migration, audit và workflow frontend theo role/scope. Một lát chưa được xem là deploy-ready cho đến khi hành vi backend, action frontend, audit side effect và hành vi database được test cùng nhau ở nơi cần thiết.
