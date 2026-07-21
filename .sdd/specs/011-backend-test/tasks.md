# Tasks: Hoàn thiện Hệ thống Test Backend

**Input**: Design documents from `.sdd/specs/011-backend-test-sonarqube/`
**Prerequisites**: `spec.md`, `plan.md`

---

## Phase 1: Setup & Design Alignment

- [x] T001 Đọc kỹ tệp đặc tả [.sdd/specs/011-backend-test-sonarqube/spec.md](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/011-backend-test-sonarqube/spec.md).
- [x] T002 Xác nhận trạng thái git (`git status`) để đảm bảo không ghi đè bất kỳ thay đổi chưa commit nào.
- [x] T003 Ghi nhận danh sách kiểm thử hiện tại đang hoạt động thông qua kết quả tại [surefire-reports](file:///d:/swp/Manager-warehouse-sdd/backend/target/surefire-reports).

---

## Phase 2: US-WMS-TEST-01: Cấu hình Hạ tầng Kiểm thử (Test Infrastructure)

- [x] T010 [P] Cấu hình plugin `maven-failsafe-plugin` và `jacoco-maven-plugin` trong [backend/pom.xml](file:///d:/swp/Manager-warehouse-sdd/backend/pom.xml).
- [x] T011 [P] Tạo tệp cấu hình H2 database cho môi trường test tại [backend/src/test/resources/application-test.yml](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/resources/application-test.yml).
- [x] T012 Chạy kiểm thử cục bộ `mvn clean verify` xác nhận Failsafe và JaCoCo XML/HTML reports được tạo thành công.
- [x] T013 Cập nhật tệp cấu hình GitHub Actions Workflow [.github/workflows/test.yml](file:///d:/swp/Manager-warehouse-sdd/.github/workflows/test.yml) để tự động hóa `mvn clean verify` và đẩy JaCoCo report lên SonarQube.

---

## Phase 3: US-WMS-TEST-02: Kiểm thử Core Services, Security & Master Data

- [x] T020 [P] Viết bài test bảo mật JWT và RBAC tại [backend/src/test/java/com/wms/security/SecurityConfigTest.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/security/SecurityConfigTest.java).
- [x] T021 [P] Viết Integration Test kiểm thử cách ly dữ liệu kho tại [backend/src/test/java/com/wms/security/WarehouseIsolationIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/security/WarehouseIsolationIT.java).
- [x] T022 [P] Viết Unit Test kiểm thử Global Exception Handler tại [backend/src/test/java/com/wms/exception/GlobalExceptionHandlerTest.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/exception/GlobalExceptionHandlerTest.java).
- [x] T023 [P] Viết Integration Test kiểm thử Jakarta Validation cho DTO tại [backend/src/test/java/com/wms/validation/RequestValidationIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/validation/RequestValidationIT.java).
- [x] T024 [P] Viết Unit Test cho Supplier Service tại [backend/src/test/java/com/wms/service/SupplierServiceTest.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/SupplierServiceTest.java) kiểm tra CRUD nhà cung cấp và ràng buộc dữ liệu.

---

## Phase 4: US-WMS-TEST-03: Kiểm thử Nghiệp vụ Kho bãi & Tài chính (WMS Business Operations)

- [x] T030 [P] Viết Unit Test cho `InventoryServiceImpl` tại [backend/src/test/java/com/wms/service/InventoryServiceTest.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/InventoryServiceTest.java).
- [x] T031 [P] Viết Integration Test kiểm thử optimistic locking (`@Version`) tại [backend/src/test/java/com/wms/service/InventoryOptimisticLockingIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/InventoryOptimisticLockingIT.java).
- [x] T032 [P] Integration Test cho luồng Inbound Receipt & Quarantine tại [backend/src/test/java/com/wms/service/InboundReceiptServiceIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/InboundReceiptServiceIT.java).
- [x] T033 [P] Integration Test cho luồng Outbound DO & Picking tại [backend/src/test/java/com/wms/service/DeliveryOrderServiceIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/DeliveryOrderServiceIT.java).
- [x] T034 [P] Integration Test cho luồng Điều chuyển liên kho E2E tại [backend/src/test/java/com/wms/service/TransferServiceIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/TransferServiceIT.java).
- [x] T035 [P] Integration Test cho luồng Kiểm kê và Adjustment tại [backend/src/test/java/com/wms/service/AdjustmentServiceIT.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/AdjustmentServiceIT.java).
- [x] T036 [P] Viết Unit Test cho Price List & COGS Management tại [backend/src/test/java/com/wms/service/PriceListServiceTest.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/PriceListServiceTest.java) kiểm tra tính toán giá vốn và thời hạn bảng giá.

---

## Phase 5: SonarQube Exclusions & Parameterized Tests Standard

- [x] T040 Thêm cấu hình `<sonar.coverage.exclusions>` vào [backend/pom.xml](file:///d:/swp/Manager-warehouse-sdd/backend/pom.xml) để loại bỏ các lớp Entity, DTO, Config, Mapper, Exception, Controller, Enums.
- [x] T041 Áp dụng JUnit 5 Parameterized Tests (`@ParameterizedTest`) cho các service/util có nhiều bộ kịch bản test.

---

## Phase 6: Verification & Automated CI Gates

- [x] T050 Chạy `mvn clean verify` từ root `backend/` để đảm bảo 100% test suite passed. AI chủ động chạy lệnh và tổng hợp báo cáo thay đổi cho người dùng nếu phát hiện lỗi cần fix.
- [x] T051 Xác nhận JaCoCo HTML report đạt line coverage >= 80% áp dụng cho New Code.
