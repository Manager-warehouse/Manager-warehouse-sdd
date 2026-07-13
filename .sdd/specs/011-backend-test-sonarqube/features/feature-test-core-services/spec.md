# Feature Specification: Kiểm thử Core Services & Phân quyền (Core Services & Auth Test Suite)

**Feature Branch**: `feat/backend-test-sonarqube`
**Created**: 2026-07-01
**Status**: Draft

---

## 1. Context and Goal

Bộ phận xác thực (JWT, BCrypt) và phân quyền (RBAC, cách ly kho) là lõi bảo mật của hệ thống WMS. Bất kỳ lỗi logic nào tại đây cũng có thể dẫn đến rò rỉ dữ liệu hoặc thất thoát hàng hóa. Đồng thời, cơ chế xử lý lỗi tập trung (Global Exception Handler) và Validation cũng cần được bảo vệ bằng các ca kiểm thử tự động.

**Goal:** Triển khai các Unit Test và Integration Test bao phủ toàn diện các kịch bản phân quyền, đăng nhập, JWT validation, xử lý lỗi hệ thống và kiểm tra ràng buộc dữ liệu đầu vào.

---

## 2. Actors

| Actor | Vai trò | Trách nhiệm |
|-------|---------|-------------|
| Developer | Maker | Viết mã nguồn kiểm thử cho security, controller advice và request validation |
| QA Engineer | QA Controller | Thực hiện kiểm thử phân quyền E2E độc lập, quản lý danh sách bug (defect log) liên quan đến bảo mật và validation |
| QA / Security Auditor | Checker | Xem xét báo cáo test phân quyền, kiểm tra các kịch bản thâm nhập (penetration tests) giả lập |

---

## 3. User Scenarios & Testing

### User Story 1 - Kiểm thử Phân quyền và Cách ly Kho (Priority: P1)
**Why this priority:** Ngăn chặn người dùng thuộc kho A can thiệp dữ liệu kho B, hoặc người dùng có vai trò thấp thực hiện hành động của Quản lý/Admin.
**Independent Test:** Tạo mock user với các vai trò khác nhau và gửi request tới API được bảo vệ.
**Acceptance Scenarios:**
1. **Given** Người dùng có role `STOREKEEPER` thuộc kho Hà Phòng, **When** Gửi yêu cầu xem danh sách bin của kho Hà Nội, **Then** Hệ thống trả về lỗi 403 Forbidden.
2. **Given** Người dùng có role `ADMIN`, **When** Gửi yêu cầu cấu hình hệ thống, **Then** Hệ thống cho phép và thực thi thành công.

### User Story 2 - Xử lý lỗi và Validation dữ liệu đầu vào (Priority: P1)
**Why this priority:** Đảm bảo hệ thống trả về thông báo lỗi thân thiện, chuẩn định dạng và mã HTTP chính xác thay vì dump stack trace.
**Independent Test:** Gửi dữ liệu không hợp lệ (null, empty, âm) tới các controller.
**Acceptance Scenarios:**
1. **Given** Request DTO có trường `total_qty` âm, **When** Gọi API tạo inventory, **Then** Hệ thống bắt lỗi Validation và trả về mã lỗi 400 Bad Request cùng chi tiết trường vi phạm.
2. **Given** API ném ra lỗi `EntityNotFoundException`, **When** Global Exception Handler bắt được, **Then** Trả về mã lỗi 404 Not Found kèm cấu trúc JSON lỗi chuẩn của dự án.

---

## 4. Functional Requirements (EARS)

- **FR-001**: WHEN người dùng gửi request không có JWT hoặc JWT hết hạn, hệ thống SHALL từ chối dịch vụ và trả về mã lỗi 401 Unauthorized.
- **FR-002**: WHILE thực hiện các thao tác ghi nhận kho bãi, hệ thống SHALL bắt buộc kiểm tra phân quyền kho của người dùng (Warehouse Isolation).
- **FR-003**: IF trường dữ liệu vi phạm Jakarta Validation ràng buộc, hệ thống SHALL ném ra `MethodArgumentNotValidException` và được xử lý bởi GlobalExceptionHandler.
- **FR-004**: WHERE các bài kiểm thử Controller bảo mật được chạy, hệ thống SHALL sử dụng `@WebMvcTest` kết hợp `@MockBean` hoặc `@SpringBootTest` với `MockMvc` và `@WithMockUser`.

---

## 5. Non-functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-001 | Security Test Coverage | Line coverage tối thiểu 90% cho các lớp liên quan đến Security / Filter / Auth |
| NFR-002 | Performance impact | Thời gian chạy các bài test MockMvc không vượt quá 2 giây/bài test |
| NFR-003 | Defect zero-tolerance | 100% các lỗ hổng bảo mật (security leaks) hoặc lỗi validation nghiêm trọng phải được giải quyết triệt để trước khi release |

---

## 6. Target Components to Test

- `com.wms.config.SecurityConfig` / Custom Authentication Filters.
- `com.wms.service.impl.UserServiceImpl` / Token Provider.
- `com.wms.exception.GlobalExceptionHandler`.
- Các DTO có sử dụng annotation validation như `@NotNull`, `@Min`, `@NotBlank`.

---

## 7. Error Handling

| Error | HTTP Status | Target Response |
|-------|-------------|-----------------|
| Access Denied | 403 Forbidden | JSON trả về chứa mã lỗi `FORBIDDEN_ROLE` hoặc `FORBIDDEN_WAREHOUSE` |
| Invalid DTO | 400 Bad Request | JSON chứa danh sách chi tiết các field vi phạm |

---

## 8. Success Criteria

- **SC-001**: Tất cả các test case cho luồng Authentication (đăng nhập thành công, đăng nhập thất bại) chạy thành công.
- **SC-002**: Các bài test kiểm tra phân quyền RBAC (ADMIN, STOREKEEPER, ACCOUNTANT...) bao phủ cả luồng thành công và luồng bị cấm.
- **SC-003**: Global Exception Handler bắt và format lỗi chính xác theo yêu cầu.
