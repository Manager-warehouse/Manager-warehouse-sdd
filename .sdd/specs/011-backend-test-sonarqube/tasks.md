# Tasks: Hoàn thiện Hệ thống Test Backend

**Input**: Design documents from `.sdd/specs/011-backend-test-sonarqube/`

**Prerequisites**: `spec.md`, `plan.md`, `features/*/spec.md`, `features/*/plan.md`

**Tests**: Yêu cầu kiểm thử tự động (Unit & Integration Test) cho toàn bộ hạ tầng cấu hình mới, core security, exception handling, DTO validation, và logic nghiệp vụ WMS.

---

## Phase 1: Setup & Design Alignment

**Purpose**: Đảm bảo môi trường thiết lập đúng quy chuẩn spec-kit và không xung đột mã nguồn hiện có.

- [ ] T001 Đọc kỹ các tệp đặc tả [.sdd/specs/011-backend-test-sonarqube/spec.md](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/011-backend-test-sonarqube/spec.md) và hiến pháp dự án `.specify/memory/constitution.md`.
- [ ] T002 Xác nhận trạng thái git (`git status`) để đảm bảo không ghi đè bất kỳ thay đổi chưa commit nào của người dùng.
- [ ] T003 Ghi nhận danh sách kiểm thử hiện tại đang hoạt động thông qua kết quả tại [surefire-reports](file:///d:/swp/Manager-warehouse-sdd/backend/target/surefire-reports).

---

## Phase 2: US-WMS-TEST-01: Cấu hình Hạ tầng Kiểm thử (Test Infrastructure)

**Goal**: Cấu hình Maven (Surefire, Failsafe, JaCoCo), thiết lập H2 Test Database và tích hợp GitHub Actions CI.

**Independent Test**: Chạy `mvn clean verify` sinh đầy đủ báo cáo Surefire, Failsafe và JaCoCo XML/HTML.

### Tasks
- [x] T010 [P] Cấu hình thêm plugin `maven-failsafe-plugin` và `jacoco-maven-plugin` vào [backend/pom.xml](file:///d:/swp/Manager-warehouse-sdd/backend/pom.xml), đồng thời tinh chỉnh `maven-surefire-plugin` chạy đúng phân vùng Unit Test.
- [x] T011 Tạo tệp cấu hình H2 database cho môi trường test tại [backend/src/test/resources/application-test.yml](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/resources/application-test.yml).
- [x] T012 Chạy thử nghiệm verify cục bộ `mvn clean verify` từ root `backend/` để kiểm tra:
  - Failsafe chạy thành công các tệp kết thúc bằng `*IT.java` (nếu có).
  - JaCoCo tạo đúng tệp `backend/target/jacoco.exec`.
  - JaCoCo xuất báo cáo XML/HTML tại `backend/target/site/jacoco/`.
- [x] T013 Cập nhật tệp cấu hình GitHub Actions Workflow (`.github/workflows/` nếu có) để tự động hóa lệnh `mvn clean verify` và gửi báo cáo JaCoCo lên SonarQube Server.

**Checkpoint**: Hạ tầng biên dịch, kiểm thử tự động và đo lường coverage hoạt động trơn tru.

---

## Phase 3: US-WMS-TEST-02: Kiểm thử Core Services & Phân quyền (Core Services & Auth)

**Goal**: Triển khai các Unit & Integration Test cho Security JWT, RBAC, cách ly kho, Exception Handling và Validation.

**Independent Test**: Chạy `mvn verify` và xác nhận tất cả các bài test bảo mật và xử lý lỗi được thực thi thành công.

### Tasks
- [x] T020 [P] Viết các bài test bảo mật cho JWT Provider và cơ chế phân quyền (RBAC) tại [backend/src/test/java/com/wms/security/SecurityConfigTest.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/security/SecurityConfigTest.java).
- [x] T021 [P] Viết Integration Test kiểm thử cách ly dữ liệu kho (Warehouse Isolation) tại [backend/src/test/java/com/wms/security/WarehouseIsolationIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/security/WarehouseIsolationIT.java).
- [x] T022 [P] Viết Unit Test kiểm thử cơ chế Global Exception Handler bắt và định dạng JSON lỗi tại [backend/src/test/java/com/wms/exception/GlobalExceptionHandlerTest.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/exception/GlobalExceptionHandlerTest.java).
- [x] T023 [P] Viết Integration Test kiểm thử bắt lỗi Jakarta Validation cho DTO đầu vào tại [backend/src/test/java/com/wms/validation/RequestValidationIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/validation/RequestValidationIT.java).

**Checkpoint**: Toàn bộ luồng bảo mật, phân quyền, xử lý lỗi và validation được kiểm thử thành công, nâng tỷ lệ coverage của các thành phần lõi lên trên 90%.

---

## Phase 4: US-WMS-TEST-03: Kiểm thử Nghiệp vụ Kho bãi (WMS Business Operations)

**Goal**: Triển khai Unit & Integration Test cho các Service nghiệp vụ Inbound, Outbound, Transfer, Stocktake/Adjustment, đảm bảo các Invariants kho được kiểm soát tốt.

**Independent Test**: Chạy `mvn verify` và xác nhận toàn bộ các kịch bản nghiệp vụ vượt qua các điều kiện biên và chặn lỗi thành công.

### Tasks
- [x] T030 [P] Viết Unit Test cho `InventoryServiceImpl` kiểm thử việc giữ chỗ (reserving), nguyên tắc FIFO và tránh tồn kho âm tại [backend/src/test/java/com/wms/service/InventoryServiceTest.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/InventoryServiceTest.java).
- [x] T031 [P] Viết Integration Test kiểm thử tranh chấp đồng thời ghi đè dữ liệu (optimistic locking `@Version`) tại [backend/src/test/java/com/wms/service/InventoryOptimisticLockingIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/InventoryOptimisticLockingIT.java).
- [x] T032 [P] Bổ sung Integration Test cho luồng Inbound Receipt, kiểm tra QC gate và đưa hàng vào khu vực Quarantine nếu lỗi tại [backend/src/test/java/com/wms/service/InboundReceiptServiceIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/InboundReceiptServiceIT.java).
- [x] T033 [P] Bổ sung Integration Test cho luồng Outbound DO, Picking và bàn giao xe tại [backend/src/test/java/com/wms/service/DeliveryOrderServiceIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/DeliveryOrderServiceIT.java).
- [x] T034 [P] Bổ sung Integration Test cho luồng Điều chuyển liên kho qua In-Transit và nhận hàng tại kho đích tại [backend/src/test/java/com/wms/service/TransferServiceIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/TransferServiceIT.java).
- [x] T035 [P] Bổ sung Integration Test cho luồng Kiểm kê và điều chỉnh tồn kho (Adjustment) sau khi duyệt chênh lệch tại [backend/src/test/java/com/wms/service/AdjustmentServiceIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/AdjustmentServiceIT.java).

**Checkpoint**: Các Invariant cốt lõi của WMS đã được bảo vệ hoàn toàn bằng kiểm thử tự động E2E / Integration.

---

## Phase 5: Cross-Cutting Verification

- [x] T900 Chạy lệnh `mvn clean verify` từ root `backend/` để biên dịch và chạy lại toàn bộ bộ test (để người dùng chạy thủ công, AI không tự ý chạy).
- [x] T901 Xác nhận JaCoCo HTML report được tạo thành công và độ bao phủ (line coverage) của các Service nghiệp vụ đạt tối thiểu 80%.
- [x] T902 Đảm bảo không để lại comment `TODO` trong code test đã hoàn thành.
- [x] T903 Xác nhận không có credentials, token hay thông tin nhạy cảm nào bị lưu hoặc commit trong code test.
- [x] T904 Đảm bảo các Integration Test không ghi đè dữ liệu trên database Production thực tế.

---

## Phase 6: Cấu hình SonarQube Exclusions & Áp dụng Parameterized Tests

**Goal**: Loại bỏ mã nguồn boilerplate khỏi thống kê độ bao phủ và chuẩn hóa việc viết test cho các bộ dữ liệu đa dạng.

- [x] T040 Thêm cấu hình `<sonar.coverage.exclusions>` vào [backend/pom.xml](file:///d:/swp/Manager-warehouse-sdd/backend/pom.xml) để loại bỏ các lớp Entity, DTO, Config, Mapper, Exception, Controller, Enums và `WmsApplication`.
- [x] T041 Rà soát và chuyển đổi các test class nghiệp vụ có nhiều bộ dữ liệu test (như validation, formatters) sang sử dụng JUnit 5 Parameterized Tests (`@ParameterizedTest` với `@CsvSource` hoặc `@MethodSource`).

---

## Dependency Rules

1. Các task thuộc Phase 2 (Cấu hình Hạ tầng Surefire, Failsafe, JaCoCo, H2) là bắt buộc phải hoàn thành trước tiên để có môi trường chạy test và đo lường.
2. Các bài test Unit/Integration của Service phải đi trước hoặc đi song song với các bài test Controller.
3. Không đánh dấu một User Story hoàn thành cho đến khi toàn bộ bài test của câu chuyện đó chạy thành công độc lập.
4. Không tự ý chạy lệnh test tự động từ phía AI (để người dùng chạy thủ công).
5. Không được tự ý sửa code nguồn/test khi phát hiện test thất bại (chỉ báo cáo lỗi chi tiết cho người dùng).
6. Các task thuộc Phase 6 cần thực thi sau khi hệ thống test baseline đã ổn định và trước khi chạy phân tích SonarQube chính thức cho toàn bộ dự án.
