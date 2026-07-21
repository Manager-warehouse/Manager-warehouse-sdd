# Implementation Plan: Hoàn thiện Hệ thống Kiểm thử & Tổ chức QA/QC

**Branch**: `feat/backend-test-sonarqube` | **Date**: 2026-07-04 | **Spec**: [spec.md](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/011-backend-test-sonarqube/spec.md)

**Input**: Feature specification from `.sdd/specs/011-backend-test-sonarqube/spec.md`

## Summary

Dự án WMS cần hoàn thiện bộ kiểm thử tự động (QC - Quality Control) tích hợp JaCoCo và SonarQube, đồng thời thiết lập quy trình vận hành và kiểm soát chất lượng QA (Quality Assurance) thông qua môi trường Staging, Defect Log, và cổng QA Sign-off nhằm bảo đảm độ tin cậy của mã nguồn trước khi release.

## Technical Context

**Language/Version**: Java 21 / Spring Boot 3.4.5; React 18 + JavaScript

**Primary Dependencies**: Spring Boot Starter Test, Spring Security Test, Mockito, MockMvc, jacoco-maven-plugin, maven-failsafe-plugin, maven-surefire-plugin

**Storage**: PostgreSQL 18 (Production/Dev), H2 Database (In-Memory PostgreSQL mode for Integration Tests)

**Testing**: 
- JUnit 5 + Mockito cho Backend Unit Tests.
- Spring Integration Tests cho APIs với H2 và MockMvc.
- SonarQube Scanner + JaCoCo cho đo lường code quality & coverage.

**Target Platform**: Full-stack WMS web application and REST API

**Performance Goals**: Chạy toàn bộ test suite cục bộ ≤ 5 phút; thời gian thực thi của mỗi MockMvc test ≤ 2 giây.

**Constraints**:
- Không làm ảnh hưởng đến dữ liệu database Production (`application-test.yml` bắt buộc dùng H2 Database cô lập).
- Không tự ý bỏ qua test (`maven.test.skip=true` bị cấm trên CI).
- Bảo toàn logic nghiệp vụ WMS: lock version (`@Version`), giữ chỗ (`reserved_qty`), tránh âm kho, tuân thủ FIFO.

## Constitution Check

*GATE: Passed*

- [x] Layered architecture preserved: N/A (code test không ảnh hưởng đến cấu trúc layered của code chính).
- [x] Write endpoints use request DTOs with Jakarta Validation: Kiểm thử đầy đủ việc validate các DTO đầu vào của API viết bằng Integration Test.
- [x] Service methods own business rules, transactions, authorization, and audit logging: Các bài test nghiệp vụ đảm bảo phủ đủ các check role, warehouse scope và lưu audit log.
- [x] All DB access goes through Spring Data JPA/Hibernate; no raw SQL in application code: Tuân thủ trong toàn bộ code test và code chính.
- [x] Inventory invariants preserved if touched: Các test service kiểm tra nghiêm ngặt `total_qty >= 0`, `reserved_qty >= 0`, và `available = total_qty - reserved_qty >= 0`.
- [x] QC/quarantine/transfer/accounting state rules listed when touched: Kiểm thử luồng Inbound QC chuyển hàng lỗi sang quarantine zone, luồng Inter-warehouse Transfer qua In-Transit.
- [x] Audit action, entity type, before/after payload, and warehouse scope identified: Các bài test nghiệp vụ verify bản ghi Audit Log được tạo tự động với đầy đủ metadata.
- [x] OpenAPI/Swagger impact identified for every new or changed endpoint: Đảm bảo không thay đổi API contract hiện tại.
- [x] Flyway migration impact identified: Không có migration mới trong tính năng test này.
- [x] Unit and integration test strategy covers happy path and error paths: Cấu hình Surefire chạy Unit Tests, Failsafe chạy Integration Tests bao gồm các case lỗi (400, 401, 403, 404, 409).

## Domain Impact

**Actors/Roles**:
- **Developer (Maker)**: Viết mã nguồn kiểm thử (QC), thực thi test cục bộ.
- **QA Engineer (QA Controller)**: Tổ chức kiểm thử độc lập, quản lý danh sách bug (Defect Log), chạy regression test trên môi trường QA/Staging, và phê duyệt cổng QA Sign-off trước khi release.
- **Tech Lead / QA (Checker)**: Đánh giá chỉ số coverage, chất lượng code test và phê duyệt PR.

**State Changes**: N/A.

**Inventory Impact**: N/A.

**Audit Actions**: Các thao tác kiểm thử đảm bảo tạo Audit Log đầy đủ cho các nghiệp vụ kho bãi (Inbound, Outbound, Transfer, Adjustment).

**Security/Authorization**: Kiểm thử phân quyền RBAC và cách ly dữ liệu kho (Warehouse Isolation). Cấm truy cập dữ liệu giữa các kho khác nhau (403 Forbidden).

**Accounting Impact**: N/A.

## Data Model / Migration Impact

- Entities/tables touched: Không (code test không thay đổi DB schema).
- New/changed columns or constraints: Không.
- Flyway plan: Không có migration.

## API / Contract Impact

- Endpoints added/changed: Không.
- Request DTOs: Không thay đổi contract, chỉ bổ sung integration tests kiểm tra validation lỗi (400 Bad Request) khi gửi DTO không hợp lệ.
- Response DTOs: Đảm bảo GlobalExceptionHandler format JSON lỗi đồng nhất.

## QA/QC Organization Plan

### 1. Phân vùng QC (Quality Control - Kiểm soát chất lượng tự động)
Hệ thống QC tự động hóa được tích hợp vào CI/CD (GitHub Actions):
* **Unit Tests & Integration Tests**: 
  * Chạy `mvn clean test` cho Unit test (Surefire).
  * Chạy `mvn clean verify` cho Unit + Integration test (Surefire + Failsafe).
* **Đo lường độ bao phủ**: JaCoCo ghi nhận kết quả và xuất báo cáo `jacoco.xml` phục vụ phân tích SonarQube.
* **Ngưỡng chất lượng (SonarQube Quality Gate)**:
  * Line Coverage ≥ 80% đối với **New Code** trong PR.
  * Cấu hình **loại trừ độ bao phủ (Coverage Exclusions)** đối với các lớp: Entity, DTO, Config, Mapper, Exception, Controller, Enums và `WmsApplication.java` để chỉ số coverage phản ánh đúng 100% chất lượng nghiệp vụ thực tế của Spec 001 - 010.
  * 0 Security Vulnerabilities & Critical Bugs.
  * Code Smell và Duplication ở mức chấp nhận (theo cấu hình dự án).

### 2. Quy trình QA (Quality Assurance - Bảo đảm quy trình chất lượng)
Quy trình QA vận hành song song để kiểm soát phát hành sản phẩm:
* **Môi trường QA/Staging**:
  * Thiết lập một môi trường Staging/QA độc lập có cấu hình tiệm cận Production để QA Engineer thực hiện kiểm thử E2E thủ công hoặc bằng script tự động.
* **Defect Management (Quản lý lỗi)**:
  * Mọi lỗi nghiệp vụ hoặc kỹ thuật phát hiện trên QA/Staging phải được ghi nhận vào Defect Log (hoặc Jira/GitHub Issues).
  * Lỗi phải được phân loại mức độ nghiêm trọng (Blocker, Critical, Major, Minor).
  * Chỉ khi 100% lỗi Blocker/Critical/Major được vá và nghiệm thu thành công, phiên bản mới được phép chuyển sang bước tiếp theo.
* **Cổng phê duyệt (QA Sign-off Gate)**:
  * Trước khi release phiên bản lên Production, QA Engineer bắt buộc phải kiểm tra và xác nhận chất lượng (QA Sign-off) bằng văn bản/báo cáo nghiệm thu.

## Test Strategy

- **Service unit tests**: Viết tại `InventoryServiceTest.java` để bảo vệ các bất biến của WMS (FIFO, Reserved, âm kho).
- **Controller/API integration tests**: 
  * `SecurityConfigTest.java` và `WarehouseIsolationIT.java` kiểm thử JWT, RBAC và bảo vệ phân vùng kho.
  * `RequestValidationIT.java` kiểm tra Jakarta Validation.
  * `InboundReceiptServiceIT.java`, `DeliveryOrderServiceIT.java`, `TransferServiceIT.java`, `AdjustmentServiceIT.java` kiểm thử tích hợp nghiệp vụ kết nối H2 DB.
- **Regression tests**: Chạy toàn bộ test suite cục bộ thông qua `mvn clean verify` trước khi tạo Pull Request để đảm bảo không phát sinh lỗi hồi quy.
- **Test Code Quality (JUnit 5 Parameterized Tests)**: Sử dụng `@ParameterizedTest` kết hợp với `@ValueSource`, `@CsvSource`, hoặc `@MethodSource` khi kiểm thử các chức năng nhận nhiều bộ dữ liệu (edge cases, validation, formatters) để loại bỏ code lặp và code gán cứng (hardcode).

## Project Structure

### Documentation

```text
.sdd/specs/011-backend-test-sonarqube/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

### Source Code

```text
backend/src/main/java/com/wms/
├── aop/
├── controller/
├── dto/
├── entity/
├── repository/
├── service/
└── exception/

backend/src/test/java/com/wms/
├── security/
│   ├── SecurityConfigTest.java
│   └── WarehouseIsolationIT.java
├── exception/
│   └── GlobalExceptionHandlerTest.java
├── validation/
│   └── RequestValidationIT.java
├── service/
│   ├── InventoryServiceTest.java
│   ├── InventoryOptimisticLockingIT.java
│   ├── InboundReceiptServiceIT.java
│   ├── DeliveryOrderServiceIT.java
│   ├── TransferServiceIT.java
│   └── AdjustmentServiceIT.java
└── resources/
    └── application-test.yml
```

**Structure Decision**: 
- Phân tách rõ ràng các thư mục Unit/Integration Test trong `backend/src/test/java/com/wms/`.
- Cấu hình hạ tầng trong `pom.xml` và `application-test.yml`.
- Tách bạch vai trò QA kiểm soát quy trình và QC kiểm soát chất lượng mã nguồn tự động.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [None] | [N/A] | [N/A] |
