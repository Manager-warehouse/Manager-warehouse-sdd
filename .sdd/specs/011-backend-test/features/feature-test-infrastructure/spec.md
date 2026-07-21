# Feature Specification: Cấu hình Hạ tầng Kiểm thử (Test Infrastructure)

**Feature Branch**: `feat/backend-test-sonarqube`
**Created**: 2026-07-01
**Status**: Draft

---

## 1. Context and Goal

Để đo lường chất lượng code và đảm bảo các bài test tự động được chạy trong quy trình CI/CD, dự án cần cấu hình đồng bộ bộ ba plugin Maven: Surefire (Unit Test), Failsafe (Integration Test), và JaCoCo (Coverage). Kết quả sau đó được gửi tới SonarQube Server thông qua GitHub Actions runner.

**Goal:** Triển khai cấu hình plugin Maven, thiết lập database in-memory H2 cho môi trường kiểm thử, và cấu hình pipeline CI/CD tự động xuất báo cáo JaCoCo XML để SonarQube phân tích.

---

## 2. Actors

| Actor | Vai trò | Trách nhiệm |
|-------|---------|-------------|
| Developer | Maker | Cấu hình Maven POM, thiết lập file properties kiểm thử, kiểm tra build cục bộ |
| QA Engineer | QA Controller | Phối hợp cấu hình môi trường Staging/QA, theo dõi báo cáo kiểm thử tự động, và kiểm thử liên thông hệ thống |
| CI/CD Runner | System | Tự động chạy `mvn verify`, xuất XML report, và kích hoạt sonar-maven-plugin gửi báo cáo |

---

## 3. User Scenarios & Testing

### User Story 1 - Phân tách Unit Test và Integration Test trong Maven (Priority: P1)
**Why this priority:** Tránh xung đột hoặc kéo dài thời gian build không đáng có. Unit Test chạy trong pha `test`, Integration Test (truy cập DB) chạy trong pha `verify`.
**Independent Test:** Chạy `mvn test` để xem chỉ chạy Unit Test, chạy `mvn verify` để xem chạy cả hai.
**Acceptance Scenarios:**
1. **Given** Cấu hình surefire và failsafe plugins, **When** Chạy `mvn test`, **Then** Hệ thống chỉ chạy các tệp `*Test.java` và không chạy `*IT.java`.
2. **Given** Cấu hình surefire và failsafe plugins, **When** Chạy `mvn verify`, **Then** Hệ thống chạy cả `*Test.java` (surefire) và `*IT.java` (failsafe).

### User Story 2 - Đo lường Coverage bằng JaCoCo và đồng bộ SonarQube (Priority: P1)
**Why this priority:** Giám sát chất lượng mã nguồn liên tục, đưa ra cảnh báo sớm nếu coverage giảm dưới ngưỡng yêu cầu.
**Independent Test:** Kiểm tra sự hiện diện của `target/site/jacoco/jacoco.xml`.
**Acceptance Scenarios:**
1. **Given** jacoco-maven-plugin đã được cấu hình trong `pom.xml`, **When** Thực thi `mvn clean verify`, **Then** Sinh ra báo cáo `jacoco.xml` và `index.html` trong thư mục target.
2. **Given** Báo cáo `jacoco.xml` tồn tại, **When** Chạy `mvn sonar:sonar`, **Then** Hệ thống gửi chính xác chỉ số coverage lên SonarQube.

---

### Edge Cases
- **Bypass test execution:** Developer sử dụng `-DskipTests` hoặc `-Dmaven.test.skip=true`. Hệ thống CI/CD phải từ chối biên dịch hoặc cảnh báo nghiêm ngặt.
- **Port/Database collision:** Nhiều luồng integration test chạy song song ghi đè database của nhau. Phải sử dụng cơ chế in-memory H2 với random hoặc unique database name mỗi lần chạy.

---

## 4. Functional Requirements (EARS)

- **FR-001**: WHEN chạy lệnh `mvn clean verify`, hệ thống SHALL thực thi tuần tự Surefire Plugin trước và Failsafe Plugin sau.
- **FR-002**: WHILE Integration Test chạy, hệ thống SHALL sử dụng H2 Database in-memory biệt lập với database production.
- **FR-003**: IF báo cáo JaCoCo XML được tạo ra, hệ thống SHALL đảm bảo tệp báo cáo nằm tại `target/site/jacoco/jacoco.xml`.
- **FR-004**: WHERE dự án sử dụng GitHub Actions, pipeline SHALL tự động thực thi `mvn clean verify` và chạy `sonar-maven-plugin` nếu có token SonarQube được cung cấp.

---

## 5. Non-functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-001 | JaCoCo execution overhead | ≤ 5% tổng thời gian chạy test |
| NFR-002 | Coverage report size | ≤ 10MB |
| NFR-003 | Compatibility | Tương thích Java 21, Spring Boot 3.4.5 và SonarQube 10.x |
| NFR-004 | QA Environment isolation | Môi trường Staging/QA phải độc lập hoàn toàn với Production |

---

## 6. Data Model & Configuration

### plugins in `pom.xml`
- `maven-surefire-plugin` (version 3.5.x)
- `maven-failsafe-plugin` (version 3.5.x)
- `jacoco-maven-plugin` (version 0.8.12)

### application-test.yml
- datasource: `jdbc:h2:mem:wmstestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`
- JPA dialect: `org.hibernate.dialect.H2Dialect`
- ddl-auto: `create-drop`

---

## 7. Execution Spec (Maven Lifecycles)

| Command | Phase | Purpose | Expected Deliverable |
|---------|-------|---------|----------------------|
| `mvn test` | test | Chạy Unit Test | Báo cáo Surefire tại `target/surefire-reports` |
| `mvn verify` | verify | Chạy Unit + Integration Test + JaCoCo | Báo cáo JaCoCo tại `target/site/jacoco` |

---

## 8. Error Handling

| Error | HTTP/Action | Condition |
|-------|-------------|-----------|
| H2 Dialect mismatch | Build failure | Cấu hình sai JPA dialect cho H2 |
| Sonar connection timeout | Pipeline warning/error | Không kết nối được tới SonarQube Server |

---

## 9. Success Criteria

- **SC-001**: `mvn clean verify` chạy thành công.
- **SC-002**: Sinh báo cáo JaCoCo XML tại `target/site/jacoco/jacoco.xml`.
- **SC-003**: Tích hợp GitHub Actions chạy thành công bước chạy test.
