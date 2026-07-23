# Feature Specification: Thiết lập & Hoàn thiện Hệ thống Test Giao diện (Frontend Testing System)

**Feature Branch**: `feat/frontend-testing`

**Created**: 2026-07-04

**Status**: Draft

**Input**: User description: "anh đang cần spec để test cho front end"

---

## 1. Context And Goal

Hệ thống Quản lý Kho (WMS) có giao diện người dùng tương tác phức tạp bao gồm các luồng nghiệp vụ quan trọng (đăng nhập, quản lý sản phẩm, nhập/xuất kho, điều chuyển, kiểm kê). Để đảm bảo tính ổn định của giao diện, ngăn chặn lỗi hiển thị dữ liệu sai lệch, kiểm soát phân quyền hiển thị theo vai trò người dùng (Zustand store), và loại bỏ hoàn toàn các lỗi hồi quy (regression) khi nâng cấp UI, dự án cần thiết lập hệ thống kiểm thử giao diện (Frontend Testing System) hoàn chỉnh.

**Goal:** Xây dựng môi trường chạy test giao diện tự động, triển khai bộ kiểm thử cho các hàm xử lý dữ liệu (Utilities & Validation), kiểm soát trạng thái đăng nhập & quyền hạn (State Management Store), định tuyến & bảo vệ trang (Routing & Guard), kiểm thử hiển thị màn hình (Components), và giả lập giao tiếp dữ liệu (API Mocking).

## Clarifications

### Session 2026-07-04
- Q: Đặc tả `spec.md` của Frontend Testing cần bổ sung chi tiết các phân vùng kiểm thử (Zustand, Routing, API Mocking, và Nghiệp vụ WMS) từ `test_frontend.md` như thế nào? → A: Tích hợp đầy đủ chi tiết nghiệp vụ: Bổ sung chi tiết các ca kiểm thử cho Zustand store (login/logout/state recovery), Mock API (lỗi 400/401/403/404/500, network error), và 9 luồng nghiệp vụ WMS ưu tiên vào spec.md.

### Session 2026-07-12
- Q: Làm thế nào để xử lý chỉ số đo lường độ bao phủ (coverage) 80% của SonarQube cho dự án hiện tại? → A: Chỉ áp dụng Quality Gate 80% cho New Code (mã nguồn viết mới hoặc sửa đổi) trong Pull Request, đồng thời thiết lập loại trừ (exclusions) cho các tệp UI thuần, config, DTO.
- Q: Chiến lược viết test cho các hàm/kịch bản có nhiều bộ dữ liệu đầu vào là gì để tránh hardcode? → A: Bắt buộc áp dụng Parameterized Tests (sử dụng JUnit 5 Parameterized Tests cho Backend và Vitest parameterized tests cho Frontend).

### Session 2026-07-22
- Q: Xử lý phạm vi Test Coverage ở Frontend như thế nào khi trước đó loại trừ toàn bộ UI Pages/Components? → A: Bỏ exclusion UI Pages/Components (không loại trừ src/pages/**, src/components/** trong SonarQube), yêu cầu bổ sung Component Tests cho các màn hình nghiệp vụ WMS chính và đưa vào đo lường SonarQube coverage.
- Q: Quy trình kiểm thử và nghiệm thu QA ở Frontend được tối ưu như thế nào? → A: Giản lược thủ tục ký duyệt QA Sign-off và môi trường Staging cồng kềnh, chuyển hoàn toàn sang tự động hóa kiểm thử trên CI/CD Pipeline (chặn PR nếu test fail hoặc Quality Gate < 80% trên New Code).
### Session 2026-07-23
- Q: Bổ sung Selenium E2E Automation Testing cho kiểm thử System Test Round 2 như thế nào? → A: Đưa Selenium E2E Testing vào phạm vi (In-Scope), tạo thư mục `test_selenium/` chứa các kịch bản E2E WebDriver giả lập người dùng thật trên trình duyệt Chrome để chạy và ghi nhận kết quả Round 2 vào file `docs/test/test_final.xlsx`.

---

## 2. Actors

| Actor | Role | Responsibilities |
|-------|------|------------------|
| Developer | Maker | Viết mã nguồn kiểm thử giao diện, thiết lập API Mocking, kiểm tra test cục bộ trước khi gửi PR |
| Reviewer / Tech Lead | Checker | Đánh giá chất lượng Component tests, API mocks và phê duyệt PR |
| CI/CD Runner | System | Tự động cài đặt dependencies, chạy linter, thực thi toàn bộ bộ test giao diện, sinh báo cáo coverage, thực thi Quality Gate và chặn merge nếu test fail |

---

## 3. User Scenarios & Testing *(mandatory)*

### Phân vùng Kiểm thử Giao diện (Frontend Test Scopes)

Để đảm bảo chất lượng giao diện toàn diện, bộ test suite phải bao phủ 6 phân vùng cốt lõi sau:

1. **Kiểm thử Tiện ích & Validation (Utility & Validation Testing)**:
   - Xác thực định dạng số lượng, tiền tệ với các giá trị hợp lệ.
   - Xử lý biên an toàn đối với các giá trị `null`, `undefined`, chuỗi rỗng, số âm, hoặc vượt quá capacity.
2. **Kiểm thử Zustand Store (Zustand State Testing)**:
   - Xác minh trạng thái sau khi đăng nhập thành công (lưu user profile, token).
   - Đảm bảo đăng xuất xóa sạch user, token và làm sạch localStorage.
   - Xác minh phân quyền vai trò (ADMIN, CEO truy cập đúng; WAREHOUSE_MANAGER chỉ thao tác trên kho được phân công; chặn người dùng không gán kho).
3. **Kiểm thử Component (Component Rendering & Interaction)**:
   - Render đúng các trạng thái giao diện: Loading (đang tải), Empty (trống), Success (thành công), và Error (lỗi).
   - Nhập liệu và submit form, hiển thị validation error, disable nút submit khi đang gửi request.
4. **Kiểm thử Routing & Phân quyền (Routing & Route Guard)**:
   - Chuyển hướng về `/login` nếu truy cập trang bảo vệ khi chưa đăng nhập.
   - Trả về trang 403 Forbidden nếu đăng nhập đúng nhưng sai vai trò/không thuộc warehouse scope của route.
5. **Kiểm thử API Mocking (API Simulation)**:
   - Giả lập gọi API thành công (success path) và bắt lỗi các status codes (400, 401, 403, 404, 500).
   - Test hành vi khi mất kết nối (Network Error) hoặc timeout.
6. **Kiểm thử 9 Luồng Nghiệp vụ WMS Ưu tiên (WMS Flows)**:
   - Đăng nhập (Login).
   - Quản lý sản phẩm (Product management).
   - Nhập kho (Inbound receipt).
   - QC và cất hàng (QC & Putaway).
   - Xuất kho và giao hàng (Outbound DO & Delivery).
   - Điều chuyển liên kho (Transfer).
   - Kiểm kê tồn kho (Stocktake).
   - Trả hàng & Hủy bỏ (Returns & Scrap).
   - Phân quyền người dùng & Warehouse scope.

### User Story 1 - Kiểm thử Logic Tiện ích và Ràng buộc Dữ liệu trên Form (Priority: P1)

Nhân viên vận hành và thủ kho thường xuyên nhập liệu trên các form như tạo phiếu nhập, phiếu xuất, điều chỉnh số lượng kiểm kê. Hệ thống cần bảo đảm các hàm tiện ích và validator bắt đúng lỗi nhập liệu trước khi gửi request lên backend.

**Why this priority**: Tránh gửi dữ liệu lỗi lên server, tăng trải nghiệm người dùng và giảm tải xử lý cho backend.

**Independent Test**: Gửi các input lỗi biên trực tiếp vào các hàm validation và component form để xác minh thông báo lỗi hiển thị đúng.

**Acceptance Scenarios**:

1. **Given** Người dùng mở form nhập số lượng kiểm kê, **When** Nhập giá trị âm hoặc để trống, **Then** Giao diện hiển thị cảnh báo lỗi màu đỏ và nút xác nhận bị vô hiệu hóa (disabled).
2. **Given** Hàm định dạng số lượng hàng (Quantity Formatter), **When** Nhập vào giá trị `null`, `undefined` hoặc chuỗi không phải số, **Then** Hàm trả về giá trị mặc định là `"0"` mà không gây lỗi crash ứng dụng.

---

### User Story 2 - Kiểm thử Phân quyền Giao diện & Định tuyến (Priority: P1)

Đảm bảo an toàn thông tin và bảo vệ vùng dữ liệu bằng cách giới hạn hiển thị menu và chuyển hướng (redirect) người dùng không hợp lệ về trang đăng nhập hoặc trang báo lỗi.

**Why this priority**: Bảo mật hệ thống, tránh người dùng có quyền hạn thấp (Storekeeper) truy cập các trang cấu hình hệ thống hoặc xem báo cáo tài chính của CEO/Manager.

**Independent Test**: Giả lập trạng thái người dùng với các vai trò khác nhau và kiểm tra hành vi chuyển hướng của Router.

**Acceptance Scenarios**:

1. **Given** Người dùng chưa đăng nhập hệ thống, **When** Cố gắng truy cập trực tiếp URL `/admin/dashboard`, **Then** Hệ thống tự động chuyển hướng người dùng về trang `/login`.
2. **Given** Thủ kho thuộc kho Hải Phòng đăng nhập thành công, **When** Truy cập trang quản lý của kho Hà Nội, **Then** Hệ thống hiển thị thông báo lỗi "Bạn không có quyền truy cập kho này" hoặc chuyển hướng về trang chủ kho Hải Phòng.

---

### User Story 3 - Kiểm thử Hiển thị Trạng thái Component & Tránh Double Submit (Priority: P2)

Đảm bảo giao diện phản hồi trực quan với người dùng trong suốt vòng đời của một yêu cầu API (Loading, Success, Error).

**Why this priority**: Tăng trải nghiệm người dùng, ngăn việc người dùng nhấn liên tiếp nhiều lần gây trùng lặp phiếu.

**Independent Test**: Giả lập API phản hồi chậm và kiểm tra trạng thái của các nút bấm cùng loading spinner.

**Acceptance Scenarios**:

1. **Given** Người dùng nhấn nút "Xác nhận Nhập kho", **When** API đang trong quá trình gửi yêu cầu (pending), **Then** Nút bấm hiển thị spinner loading và bị vô hiệu hóa (disabled) để chặn lượt nhấn tiếp theo.
2. **Given** API trả về lỗi hệ thống 500, **When** Component nhận được phản hồi lỗi, **Then** Màn hình hiển thị thông báo lỗi thân thiện dưới dạng Toast message và giữ nguyên dữ liệu đã nhập trên form để người dùng sửa.

---

### Edge Cases

- **Mất kết nối mạng đột ngột (Network Error)**: Khi người dùng đang submit form mà kết nối mạng bị mất, giao diện phải hiển thị thông báo lỗi kết nối và không làm mất dữ liệu hiện tại của người dùng.
- **Dọn dẹp State giữa các ca test**: Trạng thái đăng nhập và quyền hạn lưu trong Store toàn cục phải được làm sạch hoàn toàn sau mỗi ca test để tránh rò rỉ dữ liệu (state pollution).
- **Dữ liệu trả về từ API bị thiếu trường (Missing fields)**: Khi Mock API trả về payload thiếu một số trường không bắt buộc, các component hiển thị không được bị crash (lỗi trắng trang).

---

## 4. Functional Requirements (EARS)

- **FR-001**: WHEN chạy lệnh kiểm thử giao diện, hệ thống SHALL thực thi toàn bộ các ca kiểm thử Unit và Component một cách độc lập.
- **FR-002**: WHILE thực thi các ca kiểm thử Component, hệ thống SHALL sử dụng cơ chế giả lập API (API Mocking) để cách ly hoàn toàn với server backend thật.
- **FR-003**: IF bài kiểm thử yêu cầu quyền truy cập cụ thể, hệ thống SHALL cung cấp cơ chế thiết lập vai trò người dùng (Zustand store mock) trước khi render component.
- **FR-004**: WHERE các thay đổi trên giao diện được thực hiện, hệ thống SHALL sinh báo cáo độ bao phủ mã nguồn (code coverage) định dạng tiêu chuẩn.

---

## 5. Non-functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-001 | Code Coverage Target | Line và Branch coverage đạt tối thiểu 80% áp dụng cho **New Code** trong PR đối với tất cả tệp nguồn Frontend (bao gồm pages, components, stores, utilities, validation, và route guards). Chỉ loại trừ tệp boilerplate/config qua `sonar.coverage.exclusions`. |
| NFR-002 | Test Execution Time | Tổng thời gian chạy bộ test giao diện cục bộ không vượt quá 2 phút |
| NFR-003 | Compatibility | Báo cáo coverage được xuất dưới dạng định dạng LCOV tiêu chuẩn để tích hợp đồng bộ với SonarQube |
| NFR-004 | Test Code Quality | Sử dụng Vitest Parameterized Tests (`test.each`) cho các kịch bản test có nhiều bộ dữ liệu (edge cases) để loại bỏ trùng lặp code. |

---

## 6. Business Invariants

- **Warehouse Isolation**: Người dùng chỉ được nhìn thấy các thành phần giao diện, nút bấm và dữ liệu của kho mà họ được phân quyền gán.
- **Double Submit Protection**: Tất cả các nút bấm thực hiện giao dịch ghi nhận kho bãi bắt buộc phải có cơ chế vô hiệu hóa (disabled) trong lúc đang chờ phản hồi từ API.

---

## 7. Success Criteria

- **SC-001**: 100% các kịch bản kiểm thử giao diện đã thiết lập chạy thành công và không xảy ra lỗi build hoặc lint.
- **SC-002**: Sinh thành công báo cáo coverage định dạng HTML (để lập trình viên xem trực quan) và LCOV (để SonarQube đọc).
- **SC-003**: GitHub Actions CI thực thi bắt buộc các lệnh kiểm thử giao diện và ngăn chặn merge PR nếu có bất kỳ ca test nào thất bại.

---

## 8. Assumptions

- Cấu trúc thư mục mã nguồn và cấu hình định tuyến (React Router) không thay đổi lớn trong quá trình cài đặt test.
- Công cụ phân tích chất lượng (SonarQube) đã được bật cấu hình hỗ trợ thu nhận báo cáo định dạng LCOV từ frontend.

---

## 9. Scope Update & Integration

- **Selenium E2E Testing (In-Scope)**: Bổ sung bộ test tự động E2E sử dụng Selenium WebDriver tại thư mục `test_selenium/` để thực hiện Round 2 System Test tự động trên giao diện web thật.
- **Out of Scope**:
  - Kiểm thử giao diện tương thích trên nhiều trình duyệt khác nhau (Cross-browser matrix).
  - Kiểm thử hiệu năng kết xuất của các component đồ họa (UI Profiling).
