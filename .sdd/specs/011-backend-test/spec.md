# Feature Specification: Hoàn thiện Hệ thống Test Backend

**Spec ID**: 011-backend-test-sonarqube
**Created**: 2026-07-01
**Status**: Draft
**Features**: US-WMS-TEST-01, US-WMS-TEST-02, US-WMS-TEST-03

---

## 1. Context and Goal

Hệ thống WMS sử dụng Spring Boot 3.4.5, Java 21, Maven làm nền tảng Backend. Để đảm bảo chất lượng code và tính chính xác của các nghiệp vụ kho bãi (nhập, xuất, điều chuyển, kiểm kê...), hệ thống cần có một bộ test suite hoàn chỉnh bao gồm cả Unit Test và Integration Test.

**Goal:** Đánh giá, chuẩn hóa cấu hình chạy test hiện có, kích hoạt tích hợp kiểm thử tích hợp (Failsafe Plugin), cấu hình đo lường độ bao phủ mã nguồn (JaCoCo Coverage), và đẩy dữ liệu phân tích chất lượng lên SonarQube thông qua CI/CD Pipeline.

### Features List
* [US-WMS-TEST-01: Cấu hình Hạ tầng Kiểm thử (Test Infrastructure)](./features/feature-test-infrastructure/spec.md)
* [US-WMS-TEST-02: Kiểm thử Core Services & Phân quyền (Core Services & Auth Test Suite)](./features/feature-test-core-services/spec.md)
* [US-WMS-TEST-03: Kiểm thử Nghiệp vụ Kho bãi (WMS Business Test Suite)](./features/feature-test-wms-operations/spec.md)

## Clarifications

### Session 2026-07-04
- Q: Cơ cấu hoạt động QA (Quality Assurance) cần được bổ sung vào đặc tả hệ thống kiểm thử này theo hướng nào? → A: Quy trình QA toàn diện & Cổng phê duyệt (QA Governance): Bổ sung quy trình kiểm thử QA, môi trường Staging/QA, quy trình Log/Track bug, kịch bản Regression test và cổng QA Sign-off trước khi Release.

### Session 2026-07-12
- Q: Làm thế nào để xử lý chỉ số đo lường độ bao phủ (coverage) 80% của SonarQube cho dự án hiện tại? → A: Chỉ áp dụng Quality Gate 80% cho New Code (mã nguồn viết mới hoặc sửa đổi) trong Pull Request, đồng thời thiết lập loại trừ (exclusions) cho các tệp UI thuần, config, DTO.
- Q: Chiến lược viết test cho các hàm/kịch bản có nhiều bộ dữ liệu đầu vào là gì để tránh hardcode? → A: Bắt buộc áp dụng Parameterized Tests (sử dụng JUnit 5 Parameterized Tests cho Backend và Vitest parameterized tests cho Frontend).

### Session 2026-07-22
- Q: Quy định tự động chạy test và tự động sửa code/test khi phát hiện bug của AI được điều chỉnh như thế nào? → A: AI được phép tự động chạy lệnh test và tự động đề xuất/sửa mã nguồn/test case để xanh hóa test, nhưng bắt buộc phải tổng hợp và báo cáo rõ các thay đổi (diff/summary) cho người dùng biết.
- Q: Quy trình kiểm thử và nghiệm thu QA được chuẩn hóa như thế nào để tránh cồng kềnh thủ tục? → A: Tối ưu hóa quy trình QA: Giản lược các thủ tục ký duyệt thủ công (QA Sign-off) và môi trường Staging cồng kềnh, chuyển hoàn toàn sang tự động hóa kiểm thử trên CI/CD Pipeline (chặn PR nếu test fail hoặc Quality Gate < 80% trên New Code).
- Q: Anh muốn xử lý các tệp test thừa/không đúng mục đích spec như thế nào? → A: Xóa 2 tệp nháp thừa (AuthServiceLoginTest.java, BcryptHashPrinter.java), chuyển SecurityConfigTest.java về đúng gói com.wms.security và loại bỏ thư mục com.wms.test.

---

## 2. Actors

| Actor | Vai trò | Trách nhiệm |
|-------|---------|-------------|
| Developer | Maker | Viết mã nguồn, triển khai các bộ test unit & integration, chạy kiểm thử cục bộ |
| Tech Lead / Reviewer | Checker | Đánh giá độ bao phủ kiểm thử (coverage), chất lượng kiểm thử và phê duyệt PR |
| CI/CD Runner (GitHub Actions) | System | Tự động biên dịch, chạy toàn bộ bộ test, tạo báo cáo coverage, thực thi Quality Gate và chặn merge nếu test fail |

---

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Test Infrastructure](./features/feature-test-infrastructure/spec.md#3-functional-requirements-ears)
* [EARS - Core Services & Auth Test Suite](./features/feature-test-core-services/spec.md#3-functional-requirements-ears)
* [EARS - WMS Business Test Suite](./features/feature-test-wms-operations/spec.md#3-functional-requirements-ears)

---

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-001 | Test execution time limit (local) | Chạy toàn bộ test suite ≤ 5 phút |
| NFR-002 | Code Coverage Target | Line coverage tối thiểu 80% áp dụng cho **New Code** trong PR. Loại trừ các DTO, Configuration, Entity và boilerplate code qua exclusions (`sonar.coverage.exclusions`). |
| NFR-003 | Security compliance | 0 token/credentials được commit trong code test |
| NFR-004 | Output compatibility | Định dạng báo cáo JaCoCo XML tương thích hoàn toàn với SonarQube Scanner |
| NFR-005 | Defect Prevention | Tự động phát hiện lỗi qua CI/CD Pipeline và ngăn chặn merge PR chứa test thất bại |
| NFR-006 | Release Gate (CI Quality Gate) | Mọi PR phát hành phải vượt qua toàn bộ test cases và SonarQube Quality Gate trên New Code (>= 80%) |
| NFR-007 | Test Code Quality | Sử dụng JUnit 5 Parameterized Tests cho các kịch bản test có nhiều bộ dữ liệu (edge cases) để loại bỏ trùng lặp code và hardcode. |

---

## 5. Data Model & Configuration
*Chi tiết các file cấu hình và cơ sở dữ liệu dùng cho việc test:*
- Cấu hình Maven Surefire, Failsafe và JaCoCo Plugins trong `pom.xml`.
- Cơ sở dữ liệu H2 Database dùng trong in-memory mode cho Integration Test.
- Chi tiết cấu hình tại: [Plan - Test Infrastructure](./features/feature-test-infrastructure/plan.md).

---

## 6. API & Execution Spec
*Chi tiết các pha chạy test trong vòng đời Maven:*
* [Execution Specs - Test Infrastructure](./features/feature-test-infrastructure/spec.md#4-execution-spec-maven-lifecycles)

---

## 7. Error Handling

| Error | HTTP/Action | Condition |
|-------|-------------|-----------|
| Test Failure | Exit code != 0, stop build | Có ít nhất 1 assert thất bại hoặc lỗi logic trong code test |
| Configuration Error | Build failure, exit code != 0 | Lỗi cấu hình pom.xml hoặc thiếu file properties |
| Database Connection Refused | Fail test ngay lập tức, không skip | H2 database test không khởi chạy được hoặc lỗi dialect |

---

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Test Infrastructure](./features/feature-test-infrastructure/spec.md#5-acceptance-criteria)
* [Acceptance - Core Services & Auth Test Suite](./features/feature-test-core-services/spec.md#5-acceptance-criteria)
* [Acceptance - WMS Business Test Suite](./features/feature-test-wms-operations/spec.md#5-acceptance-criteria)

---

## 9. Out of Scope

- Kiểm thử hiệu năng tải (Load Testing, Stress Testing).
- Tự động sửa mã nguồn production bị lỗi logic nghiệp vụ lớn mà không báo cáo (mọi thay đổi fix bug của AI phải được tổng hợp báo cáo cho người dùng).
- Cho phép AI tự động thực thi các lệnh kiểm thử (Maven/Vitest) và chủ động sửa lỗi code/test case khi test thất bại, nhưng bắt buộc phải báo cáo tổng hợp đầy đủ các sửa đổi cho người dùng.
