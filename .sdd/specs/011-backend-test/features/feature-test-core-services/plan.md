# Implementation Plan: Kiểm thử Core Services & Phân quyền (Core Services & Auth Test Suite)

**Branch**: `feat/backend-test-sonarqube` | **Date**: 2026-07-01 | **Spec**: [spec.md](./spec.md)

## Summary

Viết các bộ test tự động kiểm thử cơ chế xác thực JWT, phân quyền vai trò (RBAC) trên các API, cách ly dữ liệu giữa các kho (Warehouse Isolation), xử lý ngoại lệ tập trung (GlobalExceptionHandler) và validation dữ liệu đầu vào.

## Technical Context

- **Language/Version**: Java 21 / Spring Boot 3.4.5
- **Primary Dependencies**: Spring Boot Starter Test, Spring Security Test, Mockito
- **Testing Tools**: JUnit 5, MockMvc, `@WithMockUser`
- **Constraints**: Phải kiểm tra đầy đủ cả trường hợp thành công (happy path) và thất bại (forbidden, bad request, unauthorized).

## Constitution Check
*GATE: Passed*

- [x] Layered architecture preserved: N/A.
- [x] Write endpoints validation checked via MockMvc test.
- [x] Security roles & Warehouse isolation rules checked in controller integration tests.
- [x] Unit and integration test strategy covers happy path and error paths.

## Domain Impact

- **Actors/Roles**: Tất cả các Actors bị ảnh hưởng bởi phân quyền; bổ sung QA Engineer kiểm thử phân quyền E2E.
- **State Changes**: N/A.
- **Inventory Impact**: Không.
- **Audit Actions**: Đăng nhập và các thao tác bảo mật tạo Audit Logs.
- **Security/Authorization**: Kiểm thử các endpoint với annotation `@PreAuthorize` đảm bảo phân quyền chính xác.

## Data Model / Migration Impact

- Entities/tables touched: Không có.
- Flyway plan: Không.

## API / Contract Impact

- Đảm bảo các API trả về đúng HTTP Status codes:
  - 401 khi không truyền JWT / JWT hết hạn.
  - 403 khi sai Role / không được gán quyền Kho.
  - 400 khi truyền DTO sai validation.

## Test Strategy

- **Security & RBAC Integration Tests**:
  - Tạo `SecurityConfigTest.java` / `AuthenticationControllerIT.java` chạy trên môi trường MockMvc.
  - Test đăng nhập thành công nhận JWT.
  - Test truy cập API được bảo vệ với JWT hợp lệ vs không hợp lệ.
  - Test phân quyền Kho: gán user vào kho A, cố gắng truy cập dữ liệu kho B.
- **Exception & Validation Tests**:
  - `GlobalExceptionHandlerTest.java`: Mock một Service ném ra Exception cụ thể, verify MockMvc bắt được và format JSON trả về chính xác.
  - `DTOValidationTest.java`: Gọi API với request body trống hoặc chứa giá trị không hợp lệ, verify validation lỗi.

## Project Structure

```text
.sdd/specs/011-backend-test-sonarqube/features/feature-test-core-services/
├── spec.md
└── plan.md

backend/src/test/java/com/wms/
├── security/
│   ├── SecurityConfigTest.java
│   └── JwtAuthenticationFilterIT.java
├── exception/
│   └── GlobalExceptionHandlerTest.java
└── validation/
    └── RequestValidationIT.java
```

**Structure Decision**: Tổ chức các package kiểm thử tương ứng dưới `src/test/java/com/wms/` đảm bảo rõ ràng, tách biệt logic nghiệp vụ.
