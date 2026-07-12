# WMS QA/QC & Test System Tracking (B1 - B18)

Bản theo dõi tiến độ triển khai hệ thống kiểm thử tự động (QC) và quy trình vận hành chất lượng (QA) cho dự án Warehouse Management System (WMS).

---

## Danh sách Task chi tiết (B1 - B18)

- [x] **B1: Xác định phạm vi test: Backend, Frontend, API, database và Docker.**
  * *Trạng thái:* Đã hoàn thành.
  * *Chi tiết:* Đã định hình rõ ràng trong tài liệu đặc tả [spec.md](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/011-backend-test-sonarqube/spec.md).

- [x] **B2: Rà soát test hiện có: Backend (53 files, 535 tests JUnit/Mockito), Frontend (2 files test chưa chạy).**
  * *Trạng thái:* Đã hoàn thành.
  * *Chi tiết:* Rà soát và phân loại: 536 backend Unit Tests, và 2 file test frontend nháp (`config.test.js`, `rbac.test.js`).

- [x] **B3: Chạy baseline backend: `mvn clean test` ghi nhận pass/fail.**
  * *Trạng thái:* Đã hoàn thành.
  * *Chi tiết:* Chạy thành công 536 Unit Tests độc lập qua Surefire Plugin (100% Success).

- [x] **B4: Cấu hình Integration Test: Surefire (`*Test`), Failsafe (`*IT`), chạy `mvn clean verify`.**
  * *Trạng thái:* Đã hoàn thành.
  * *Chi tiết:* Đã cấu hình Surefire & Failsafe trong [backend/pom.xml](file:///d:/swp/Manager-warehouse-sdd/backend/pom.xml). Chạy `mvn clean verify` quét và chạy thành công 21 Integration Tests (`*IT.java`) kết nối H2 DB.

- [x] **B5: Thêm JaCoCo để đo coverage backend.**
  * *Trạng thái:* Đã hoàn thành.
  * *Chi tiết:* Đã tích hợp `jacoco-maven-plugin`. Báo cáo XML và HTML được xuất tự động tại thư mục `backend/target/site/jacoco/`.

- [x] **B6: Cấu hình Vitest + React Testing Library cho frontend.**
  * *Trạng thái:* Đã hoàn thành.
  * *Chi tiết:* Đã cài đặt vitest, jsdom, react testing library, v.v. và cấu hình thành công trong vite.config.js cùng tests/setup.js.

- [x] **B7: Sửa frontend test để gọi code production thật, không sao chép logic.**
  * *Trạng thái:* Đã hoàn thành.
  * *Chi tiết:* Đã sửa đổi rbac.test.js và config.test.js để import và chạy trực tiếp trên các module thật từ mã nguồn production (auth.store.js, SystemConfig.jsx).

- [x] **B8: Chạy frontend: `npm run lint`, `npm run test`, `npm run test:coverage`, `npm run build`.**
  * *Trạng thái:* Đã hoàn thành.
  * *Chi tiết:* Đã chạy thành công 100% test cases cục bộ, kiểm tra lint, đo lường coverage và biên dịch thành công production build.

- [x] **B9: Bổ sung test cho phần coverage thấp và nghiệp vụ quan trọng.**
  * *Trạng thái:* Đã hoàn thành.
  * *Chi tiết:*
    * **Backend (Đã hoàn thành):** Đã bổ sung 21 Integration tests bao gồm các nghiệp vụ cốt lõi (Inventory, Inbound QC, Outbound DO, Transfer, Adjustment, Security, Global Exception, DTO Validation).
    * **Frontend (Đã hoàn thành):** Đã bổ sung bộ unit test cho format utility (`format.js`) và UI state store (`ui.store.js`) đạt 100% coverage cho các tệp này.

- [x] **B10: Cài/chạy SonarQube server hoặc dùng SonarQube Cloud.**
  * *Trạng thái:* Đã hoàn thành.
  * *Chi tiết:* Đã thiết lập và khởi chạy SonarQube cục bộ để thực hiện quét cả mã nguồn Backend và Frontend.

- [x] **B11: Gửi JaCoCo/LCOV lên SonarQube để kiểm tra bug, security, duplication và Quality Gate.**
  * *Trạng thái:* Đã hoàn thành.
  * *Chi tiết:* Đã cấu hình đẩy báo cáo JaCoCo XML (cho backend) và LCOV (cho frontend) lên SonarQube trong CI/CD. Đang cấu hình thêm loại trừ (coverage exclusions) để chỉ đo lường chính xác các lớp nghiệp vụ thuộc Spec 001 - 010.

- [x] **B12: Hoàn thiện GitHub Actions cho cả frontend/backend.**
  * *Trạng thái:* Đã hoàn thành.
  * *Chi tiết:* Tệp workflow test.yml đã được cập nhật bắt buộc chạy lint, test:coverage, và build đối với frontend trên CI (không còn dùng `--if-present` để bypass).

- [ ] **B13: Push feature branch, tạo Pull Request và để CI/SonarQube kiểm tra.**
  * *Trạng thái:* Chưa thực hiện.
  * *Chi tiết:* Đang phát triển trên branch `feat/backend-test-sonarqube`, chưa tạo PR hoàn chỉnh lên `main` hoặc `develop`.

- [ ] **B14: Build một Docker image có tag cố định sau khi CI đạt.**
  * *Trạng thái:* Chưa thực hiện.
  * *Chi tiết:* Dự án chưa có Dockerfile cho backend/frontend và chưa cấu hình build image tự động trong CI.

- [ ] **B15: Chạy image trên môi trường test với database/secret riêng.**
  * *Trạng thái:* Chưa thực hiện.
  * *Chi tiết:* Cần cấu hình docker-compose và môi trường chạy test cô lập (Staging environment).

- [ ] **B16: Smoke test và E2E các luồng login, nhập/xuất/chuyển/kiểm kê/trả hàng.**
  * *Trạng thái:* Chưa thực hiện.
  * *Chi tiết:* Chưa tích hợp framework Smoke test / E2E (như Cypress/Playwright).

- [/] **B17: Hoàn thiện QA: test plan, acceptance criteria, test case ID, traceability matrix, bug report và release checklist.**
  * *Trạng thái:* Mới làm một phần.
  * *Chi tiết:* Đã cập nhật thiết lập vai trò QA Engineer, quy định quy trình QA Staging, Defect Log và Release QA Sign-off vào các đặc tả. Tuy nhiên, chưa lập chi tiết Test Case ID hay Traceability Matrix.

- [ ] **B18: Chỉ merge/deploy khi test, coverage, Quality Gate và Docker smoke test đều đạt.**
  * *Trạng thái:* Chưa thực hiện.
  * *Chi tiết:* Cần kích hoạt các quy tắc kiểm soát nhánh (branch protection rules) trên Git sau khi hạ tầng QA/QC được cấu hình đầy đủ.
