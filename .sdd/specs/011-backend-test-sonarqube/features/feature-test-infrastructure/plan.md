# Implementation Plan: Cấu hình Hạ tầng Kiểm thử (Test Infrastructure)

**Branch**: `feat/backend-test-sonarqube` | **Date**: 2026-07-01 | **Spec**: [spec.md](./spec.md)

## Summary

Thiết lập cấu hình Maven (Surefire, Failsafe, JaCoCo), tạo cấu hình H2 test database cô lập để chạy Integration Test, và cập nhật GitHub Actions Workflow tích hợp SonarQube phân tích độ bao phủ code tự động.

## Technical Context

- **Language/Version**: Java 21 / Spring Boot 3.4.5 / Maven
- **Primary Dependencies**: Spring Boot Starter Test, H2 Database (test scope)
- **Plugins**: maven-surefire-plugin, maven-failsafe-plugin, jacoco-maven-plugin
- **CI/CD**: GitHub Actions + SonarQube Scanner
- **Constraints**: Không bypass test trong CI (`maven.test.skip` hoặc `|| true` bị cấm).

## Constitution Check
*GATE: Passed*

- [x] Layered architecture preserved: N/A (chỉ sửa cấu hình build).
- [x] Write endpoints use request DTOs: N/A.
- [x] Unit and integration test strategy covers happy path and error paths.
- [x] Database test isolated (H2).
- [x] No sensitive credentials stored.

## Domain Impact

- **Actors/Roles**: Developer, QA Engineer, CI/CD Runner.
- **State Changes**: N/A.
- **Inventory Impact**: Không có.
- **Audit Actions**: Không có.
- **Security/Authorization**: Không có.

## Data Model / Migration Impact

- Entities/tables touched: Không.
- Flyway plan: Không thay đổi DB migration. Flyway sẽ tự động chạy trên H2 khi chạy Integration Test.

## API / Contract Impact

- Endpoints added/changed: Không.

## Test Strategy

- Chạy thử nghiệm `mvn clean test` để kiểm tra Unit Test Surefire.
- Chạy thử nghiệm `mvn clean verify` để kiểm tra Failsafe quét các tệp `*IT.java` và JaCoCo sinh báo cáo `jacoco.xml` thành công.
- Kiểm tra báo cáo được tạo ra tại:
  - `backend/target/site/jacoco/jacoco.xml`
  - `backend/target/site/jacoco/index.html`

## Project Structure

```text
.sdd/specs/011-backend-test-sonarqube/features/feature-test-infrastructure/
├── spec.md
└── plan.md

backend/
├── pom.xml (Thêm cấu hình plugins)
└── src/test/resources/
    └── application-test.yml (Cấu hình H2 test database)
```

**Structure Decision**: Cập nhật trực tiếp `pom.xml` của module `backend` và thêm file cấu hình `application-test.yml` cục bộ của thư mục test resources.
