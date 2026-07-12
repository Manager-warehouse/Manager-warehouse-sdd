# Feature Specification: Hoàn thiện Hệ thống Test Backend

**Spec ID**: feat-backend-test-sonarqube
**Created**: 2026-07-01
**Status**: Draft
**Input**: User description: "Kiểm tra và hoàn thiện hệ thống test backend, tích hợp JaCoCo và SonarQube"

---

## 1. Context and Goal

Hệ thống WMS sử dụng Spring Boot 3.4.5, Java 21, Maven làm nền tảng Backend. Để đảm bảo chất lượng code và tính chính xác của các nghiệp vụ kho bãi (nhập, xuất, điều chuyển, kiểm kê...), hệ thống cần có một bộ test suite hoàn chỉnh bao gồm cả Unit Test và Integration Test.

**Goal:** Đánh giá, chuẩn hóa cấu hình chạy test hiện có, kích hoạt tích hợp kiểm thử tích hợp (Failsafe Plugin), cấu hình đo lường độ bao phủ mã nguồn (JaCoCo Coverage), và đẩy dữ liệu phân tích chất lượng lên SonarQube thông qua CI/CD Pipeline.

---

## 2. Actors

| Actor | Vai trò | Trách nhiệm |
|-------|---------|-------------|
| Developer | Maker | Viết mã nguồn, triển khai các bộ test unit & integration, chạy kiểm thử cục bộ |
| Tech Lead / QA | Checker | Đánh giá độ bao phủ kiểm thử (coverage), chất lượng kiểm thử và phê duyệt mã nguồn |
| CI/CD Runner (GitHub Actions) | System | Tự động biên dịch, chạy toàn bộ bộ test, tạo báo cáo coverage và đồng bộ với SonarQube |

---

## 3. User Scenarios & Testing

### User Story 1 - Tự động hóa kiểm thử tích hợp (IT) với Maven (Priority: P1)
**Why this priority:** Đảm bảo các lớp kiểm thử tích hợp (*IT.java) được chạy tự động trong vòng đời build mà không bị bỏ sót, giúp phát hiện lỗi tương tác database/service sớm.
**Independent Test:** Chạy `mvn verify` và xác nhận các test class có tên kết thúc bằng `IT` được thực thi bởi Failsafe Plugin.
**Acceptance Scenarios:**
1. **Given** Cấu hình Maven có cả Surefire và Failsafe, **When** Chạy lệnh `mvn clean verify`, **Then** Surefire chạy các Unit Test (*Test.java) và Failsafe chạy các Integration Test (*IT.java).
2. **Given** Có lỗi xảy ra trong Integration Test, **When** Thực thi build CI, **Then** Quá trình build phải dừng lại (fail build) và trả về exit code khác 0.

### User Story 2 - Đo lường độ bao phủ mã nguồn với JaCoCo (Priority: P1)
**Why this priority:** Xác định trực quan những vùng code chưa được kiểm thử bảo vệ, đặc biệt các dịch vụ nghiệp vụ quan trọng của WMS.
**Independent Test:** Kiểm tra thư mục `target/site/jacoco/` có chứa tệp `index.html` và `jacoco.xml` sau khi chạy verify.
**Acceptance Scenarios:**
1. **Given** Dự án đã tích hợp jacoco-maven-plugin, **When** Thực thi `mvn clean verify`, **Then** JaCoCo sinh báo cáo định dạng HTML và XML đầy đủ tại thư mục target.
2. **Given** Báo cáo JaCoCo XML đã được sinh ra, **When** SonarQube scanner chạy phân tích, **Then** SonarQube đọc chính xác và hiển thị tỷ lệ line coverage và branch coverage trên dashboard.

---

### Edge Cases
- **Bypass Coverage:** Developer tìm cách tắt test hoặc cấu hình bỏ qua Surefire/Failsafe (ví dụ: `maven.test.skip=true`). Hệ thống CI/CD cần chặn các tham số bypass này.
- **Production Database Collision:** Integration test vô tình kết nối và ghi đè dữ liệu trên database Production. Hệ thống bắt buộc phải sử dụng cấu hình database độc lập (H2 hoặc PostgreSQL chuyên biệt cho test).
- **Sensitive Data Leak:** Token bảo mật, password hoặc API keys bị ghi nhận trong log test hoặc đẩy lên git repository. Cần quét loại bỏ thông tin nhạy cảm.

---

## 4. Functional Requirements (EARS)

- **FR-001**: WHEN chạy lệnh `mvn clean verify`, hệ thống SHALL thực thi toàn bộ Unit Test và Integration Test tuần tự và độc lập.
- **FR-002**: WHILE quá trình kiểm thử diễn ra, hệ thống SHALL sử dụng database H2 hoặc PostgreSQL test riêng biệt và KHÔNG được kết nối tới database Production.
- **FR-003**: IF bất kỳ ca kiểm thử nào thất bại (fail hoặc error), hệ thống CI/CD SHALL dừng pipeline ngay lập tức và đánh dấu trạng thái build thất bại.
- **FR-004**: WHERE các kiểm thử tích hợp giao tiếp với database, hệ thống SHALL sử dụng cơ chế rollback hoặc dọn dẹp dữ liệu sau mỗi test method để đảm bảo tính độc lập.

---

## 5. Non-functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-001 | Test execution time limit (local) | Chạy toàn bộ test suite ≤ 5 phút |
| NFR-002 | Code Coverage Target | Line coverage tối thiểu 80% cho các class Service mới |
| NFR-003 | Security compliance | 0 token/credentials được commit trong code test |
| NFR-004 | Output compatibility | Định dạng báo cáo JaCoCo XML tương thích hoàn toàn với SonarQube Scanner |

---

## 6. Configuration & Architecture

### Maven Plugins Configuration (`pom.xml`)

Cần đảm bảo cấu hình các plugin sau trong `pom.xml`:
- **maven-surefire-plugin**: Chạy Unit Test (`**/*Test.java`).
- **maven-failsafe-plugin**: Chạy Integration Test (`**/*IT.java`).
- **jacoco-maven-plugin**: Đo lường coverage và sinh báo cáo XML/HTML trong pha `verify`.

### Test Database Policy

- Sử dụng cấu hình profile `test` thông qua `application-test.yml` hoặc cấu hình động để chỉ định datasource H2/PostgreSQL độc lập.
- Sử dụng `@ActiveProfiles("test")` trên các lớp Integration Test.

---

## 7. Execution Spec (Maven Lifecycles)

| Command | Phase | Purpose | Expected Deliverable |
|---------|-------|---------|----------------------|
| `mvn test` | test | Chạy các Unit Test | File report Surefire tại `target/surefire-reports` |
| `mvn verify` | verify | Chạy Unit Test + Integration Test + JaCoCo | Báo cáo JaCoCo tại `target/site/jacoco` |
| `mvn sonar:sonar` | verify | Gửi phân tích code lên SonarQube | Dashboard SonarQube cập nhật chỉ số coverage |

---

## 8. Error Handling

| Error | Handling Method | Condition |
|-------|-----------------|-----------|
| Test Failure | Exit code != 0, stop build | Có ít nhất 1 assert thất bại hoặc lỗi logic |
| Configuration Error | Build failure, exit code != 0 | Lỗi cú pháp pom.xml, sai phiên bản plugin hoặc thiếu file cấu hình DB test |
| Database Connection Refused | Fail test ngay lập tức, không skip | Database test không khả dụng |

---

## 9. Audit Trail & Reports

- **Surefire Reports**: Lưu trữ kết quả chạy Unit Test định dạng TXT/XML trong `target/surefire-reports`.
- **Failsafe Reports**: Lưu trữ kết quả chạy Integration Test định dạng TXT/XML trong `target/failsafe-reports`.
- **JaCoCo Reports**:
  - `target/site/jacoco/index.html` (Xem thủ công)
  - `target/site/jacoco/jacoco.xml` (SonarQube đọc)

---

## 10. Rules & Invariants

- **Bảo toàn Test hiện có**: Không xóa các test đã viết trừ khi chứng minh được test bị sai logic hoặc lỗi thời.
- **Không mock thực thể đang test**: Mock các dependency bên ngoài (Repository, external API) bằng `@Mock`/`Mockito.mock()`, tuyệt đối không mock chính class đang được kiểm thử.
- **Kiểm thử hành vi nghiệp vụ**: Không viết test rác chỉ để tăng chỉ số coverage. Test phải có kiểm tra assert hợp lệ xác thực luồng nghiệp vụ.
- **An toàn dữ liệu**: Tuyệt đối không commit token, password hoặc thông tin cấu hình nhạy cảm.

---

## 11. Success Criteria

- **SC-001**: Lệnh `mvn clean verify` chạy thành công, không có lỗi build hoặc cấu hình.
- **SC-002**: Toàn bộ các lớp có đuôi *IT.java được Failsafe quét qua và thực thi thành công.
- **SC-003**: Tạo thành công báo cáo JaCoCo HTML và XML tại đúng vị trí quy định.
- **SC-004**: Dashboard SonarQube hiển thị đầy đủ tỷ lệ bao phủ test sau khi chạy phân tích.
- **SC-005**: Không làm mất mát hoặc ảnh hưởng đến các thay đổi chưa commit của người dùng.

---

## 12. Assumptions

- Hệ thống CI/CD (GitHub Actions) có cấu hình sẵn runner hỗ trợ Java 21 và Maven.
- Đã có tài khoản hoặc cấu hình kết nối SonarQube Server/SonarCloud hoạt động tốt.

---

## 13. Out of Scope

- Kiểm thử hiệu năng tải (Load Testing, Stress Testing).
- Tự động sửa mã nguồn production bị lỗi logic nghiệp vụ để vượt qua test (chỉ báo cáo phát hiện bug nếu có).

---

## 14. Implementation Workflow & Reporting

### Quy trình thực hiện (Execution Workflow)
- **Bước 1**: Kiểm tra `git status` và không ghi đè các thay đổi hiện tại của người dùng.
- **Bước 2**: Liệt kê các bài test hiện có, xác định bài test nào thực sự được Maven chạy.
- **Bước 3**: Chạy baseline test trước khi thay đổi cấu hình, ghi nhận số lượng pass/fail/error/skipped.
- **Bước 4**: Cấu hình `jacoco-maven-plugin`, `maven-surefire-plugin` và `maven-failsafe-plugin` trong `pom.xml`.
- **Bước 5**: Chạy thử nghiệm và bổ sung các test cần thiết.
- **Bước 6**: Chạy lại `mvn clean verify`.
- **Bước 7**: Kiểm tra báo cáo từ Surefire, Failsafe và JaCoCo.
- **Bước 8**: Sửa các lỗi phát sinh do cấu hình hoặc kiểm thử (trong phạm vi).
- **Bước 9**: Tuyệt đối không tự ý sửa logic production để vượt qua test. Nếu phát hiện lỗi production, báo cáo bằng chứng lỗi rõ ràng trước khi can thiệp.
- **Bước 10**: Không tự động commit, push hay merge khi chưa có yêu cầu cụ thể từ người dùng.

### Kết quả cần báo cáo (Final Report Deliverables)
1. Tổng số lượng bài test đã chạy.
2. Chi tiết số lượng: Pass, Fail, Error, Skipped.
3. Danh sách cụ thể các lớp Unit Test và Integration Test đã được thực thi.
4. Tỷ lệ phần trăm Line Coverage và Branch Coverage đạt được.
5. Danh sách các Package/Class có tỷ lệ coverage thấp cần lưu ý.
6. Danh sách các bài test đã được bổ sung hoặc chỉnh sửa.
7. Danh sách bug nghiệp vụ trong Production phát hiện được trong quá trình viết test (nếu có).
8. Các file cấu hình Maven, GitHub Actions, SonarQube đã được sửa đổi.
9. Các vấn đề còn tồn đọng và đề xuất bước tiếp theo.


-------------------------------------------------------------------
Chi tiết các phân vùng Plan được tạo mới:
Cấu hình Hạ tầng Kiểm thử (Test Infrastructure)

Đặc tả (Spec): 

feature-test-infrastructure/spec.md
Kế hoạch (Plan): 

feature-test-infrastructure/plan.md
Nhiệm vụ chính: Cấu hình Maven plugins (Surefire, Failsafe, JaCoCo), tạo file cấu hình H2 test database (application-test.yml), tích hợp SonarQube & GitHub Actions pipeline.
Kiểm thử Core Services & Phân quyền (Core Services & Auth)

Đặc tả (Spec): 

feature-test-core-services/spec.md
Kế hoạch (Plan): 

feature-test-core-services/plan.md
Nhiệm vụ chính: Viết các unit & integration test cho bộ lọc bảo mật JWT, phân quyền theo vai trò (RBAC), cách ly dữ liệu kho (Warehouse Isolation), GlobalExceptionHandler và Validation.
Kiểm thử Nghiệp vụ Kho bãi (WMS Business Operations)

Đặc tả (Spec): 

feature-test-wms-operations/spec.md
Kế hoạch (Plan): 

feature-test-wms-operations/plan.md
Nhiệm vụ chính: Viết các bộ test cho Inbound, Outbound, Transfer và Stocktake/Adjustment. Kiểm soát chặt chẽ các bất biến tồn kho như FIFO, Lock version, không cho phép tồn kho âm.