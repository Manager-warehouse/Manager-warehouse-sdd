# Feature Specification: Production Continuous Delivery

**Feature Branch**: `chore/cd-production-hardening`

**Created**: 2026-07-03

**Status**: Draft

**Input**: User description: "Thiết kế và triển khai quy trình CD cụ thể, áp dụng các skill và có double-check."

## 1. Context And Goal

WMS cần một quy trình phát hành production lặp lại được, có kiểm soát và có thể
truy vết. Mục tiêu là chỉ đưa phiên bản đã vượt qua các cổng chất lượng lên môi
trường production, giảm thao tác thủ công trên máy chủ, bảo vệ dữ liệu nghiệp vụ
và cung cấp đường lui rõ ràng khi phát hành thất bại.

## 2. Actors

| Actor | Role | Responsibilities |
|-------|------|------------------|
| Developer | Maker | Chuẩn bị thay đổi, theo dõi kiểm tra và cung cấp bằng chứng phát hành |
| Reviewer | Checker | Phê duyệt thay đổi sau khi các cổng chất lượng đạt yêu cầu |
| Release Approver | Production approver | Cho phép hoặc từ chối phát hành production |
| Operator | Incident responder | Theo dõi phát hành, xử lý lỗi và thực hiện rollback được phê duyệt |

## 3. User Scenarios & Testing *(mandatory)*

### User Story 1 - Phát hành phiên bản đã kiểm chứng (Priority: P1)

Release Approver có thể xác định chính xác phiên bản sắp được phát hành, xem
bằng chứng kiểm tra và chỉ cho phép triển khai khi tất cả cổng bắt buộc đã đạt.

**Why this priority**: Đây là luồng chính ngăn mã chưa được kiểm chứng đi vào production.

**Independent Test**: Chuẩn bị một phiên bản đạt tất cả cổng, phê duyệt phát hành,
và chứng minh phiên bản đang chạy trùng khớp với phiên bản đã được phê duyệt.

**Acceptance Scenarios**:

1. **Given** một thay đổi đã được review và vượt qua mọi cổng bắt buộc, **When** Release Approver phê duyệt, **Then** đúng phiên bản đã kiểm chứng được phát hành và ghi nhận.
2. **Given** bất kỳ cổng bắt buộc nào thất bại, **When** có yêu cầu phát hành, **Then** production không thay đổi và nguyên nhân bị chặn được hiển thị.
3. **Given** hai yêu cầu phát hành gần đồng thời, **When** yêu cầu đầu đang chạy, **Then** yêu cầu sau không thể thay đổi production song song.

---

### User Story 2 - Bảo vệ dữ liệu khi phát hành (Priority: P1)

Operator có bằng chứng backup hợp lệ và đánh giá tương thích thay đổi dữ liệu
trước khi phiên bản mới được phép thay đổi production.

**Why this priority**: Dữ liệu tồn kho, tài chính và audit trail không thể tái tạo an toàn nếu bị mất.

**Independent Test**: Mô phỏng một phát hành có thay đổi dữ liệu và chứng minh
phát hành bị chặn khi backup hoặc kiểm tra tương thích không đạt.

**Acceptance Scenarios**:

1. **Given** phiên bản có thay đổi lược đồ dữ liệu, **When** chưa có backup hợp lệ, **Then** phát hành bị chặn trước khi production bị thay đổi.
2. **Given** thay đổi dữ liệu không tương thích với phiên bản đang chạy, **When** đánh giá phát hành, **Then** hệ thống yêu cầu chia thay đổi thành các bước tương thích trước khi tiếp tục.
3. **Given** backup đã được tạo, **When** kiểm tra tính toàn vẹn thất bại, **Then** backup đó không được dùng làm bằng chứng phục hồi.

---

### User Story 3 - Phát hiện lỗi và rollback ứng dụng (Priority: P1)

Operator nhận biết nhanh phát hành không lành mạnh và có thể đưa ứng dụng về
phiên bản ổn định gần nhất mà không tự động đảo ngược lịch sử dữ liệu.

**Why this priority**: Giảm thời gian gián đoạn trong khi tránh làm hỏng dữ liệu bằng downgrade tùy tiện.

**Independent Test**: Phát hành một phiên bản cố ý không vượt health check và
chứng minh phiên bản ứng dụng trước đó được khôi phục trong thời gian mục tiêu.

**Acceptance Scenarios**:

1. **Given** phiên bản mới không vượt health check hoặc smoke test, **When** hết thời gian xác minh, **Then** phát hành được đánh dấu thất bại và rollback ứng dụng được kích hoạt.
2. **Given** rollback ứng dụng hoàn tất, **When** kiểm tra lại production, **Then** phiên bản ổn định trước đó hoạt động và kết quả được ghi nhận.
3. **Given** lỗi yêu cầu phục hồi dữ liệu, **When** Operator xử lý sự cố, **Then** việc restore chỉ diễn ra qua quy trình sự cố được phê duyệt, không phải rollback tự động.

---

### User Story 4 - Truy vết và vận hành phát hành (Priority: P2)

Reviewer và Operator có thể tra cứu ai phê duyệt, phiên bản nào đã chạy, các cổng
nào đã đạt, thời điểm phát hành và kết quả cuối cùng mà không để lộ bí mật.

**Why this priority**: Hỗ trợ audit, điều tra sự cố và cải tiến độ tin cậy.

**Independent Test**: Chọn một lần phát hành và tái dựng đầy đủ timeline từ bằng
chứng lưu trữ, đồng thời xác minh không có secret trong log hoặc artifact.

**Acceptance Scenarios**:

1. **Given** một lần phát hành kết thúc, **When** Reviewer tra cứu, **Then** có đủ phiên bản, người phê duyệt, thời gian, kết quả cổng và trạng thái cuối.
2. **Given** log và artifact phát hành, **When** kiểm tra dữ liệu nhạy cảm, **Then** không có mật khẩu, token, khóa riêng hoặc nội dung secret.

### Edge Cases

- Production đang có thay đổi thủ công hoặc không trùng với trạng thái phát hành gần nhất.
- Máy chủ thiếu dung lượng để backup, tải phiên bản mới hoặc giữ phiên bản rollback.
- Mất kết nối trong lúc phát hành sau khi dữ liệu đã được cập nhật.
- Health check nội bộ đạt nhưng domain public hoặc luồng đăng nhập không hoạt động.
- Phiên bản rollback không còn tương thích với lược đồ dữ liệu hiện tại.
- Yêu cầu phát hành thủ công tham chiếu phiên bản chưa vượt đủ cổng chất lượng.
- Backup được tạo nhưng không thể đọc hoặc không đủ thành phần để phục hồi.

## 4. Functional Requirements (EARS)

- **FR-001**: WHEN thay đổi được đề nghị hợp nhất, hệ thống SHALL chạy các kiểm tra backend, frontend, cấu hình phát hành và gói triển khai bắt buộc.
- **FR-002**: IF một kiểm tra bắt buộc thất bại, hệ thống SHALL chặn hợp nhất hoặc phát hành và lưu bằng chứng lỗi.
- **FR-003**: WHEN một phiên bản vượt tất cả cổng, hệ thống SHALL tạo một định danh bất biến liên kết phiên bản nguồn với gói triển khai đã kiểm chứng.
- **FR-004**: WHEN phát hành production được yêu cầu, hệ thống SHALL yêu cầu phê duyệt của Release Approver.
- **FR-005**: WHILE một phát hành production đang chạy, hệ thống SHALL ngăn phát hành production khác thay đổi môi trường đồng thời.
- **FR-006**: WHEN phiên bản có thể tác động dữ liệu, hệ thống SHALL tạo và xác minh backup trước khi thay đổi production.
- **FR-007**: IF thay đổi dữ liệu không tương thích ngược, hệ thống SHALL chặn phát hành cho đến khi có kế hoạch nhiều bước tương thích.
- **FR-008**: WHEN triển khai bắt đầu, hệ thống SHALL triển khai đúng gói bất biến đã vượt cổng, không tái tạo gói khác trên production.
- **FR-009**: WHEN triển khai hoàn tất, hệ thống SHALL chạy health check nội bộ và smoke test qua đường truy cập production.
- **FR-010**: IF xác minh sau triển khai thất bại, hệ thống SHALL dừng phát hành, thu thập bằng chứng chẩn đoán và khôi phục phiên bản ứng dụng ổn định gần nhất khi tương thích.
- **FR-011**: WHILE rollback ứng dụng diễn ra, hệ thống SHALL không tự động downgrade lịch sử thay đổi dữ liệu.
- **FR-012**: WHEN phát hành kết thúc, hệ thống SHALL ghi lại phiên bản, người phê duyệt, thời gian, kết quả cổng, kết quả xác minh và trạng thái cuối.
- **FR-013**: WHERE log hoặc artifact phát hành được tạo, hệ thống SHALL loại trừ secret và dữ liệu xác thực nhạy cảm.
- **FR-014**: WHEN phát hành thủ công được yêu cầu, hệ thống SHALL áp dụng cùng cổng, phê duyệt và truy vết như phát hành tự động.
- **FR-015**: IF production có thay đổi thủ công chưa được kiểm soát, hệ thống SHALL chặn phát hành và báo sai lệch để Operator xử lý.
- **FR-016**: WHERE cổng chất lượng bắt buộc được đánh giá, hệ thống SHALL yêu cầu backend tests, frontend lint/tests/build, cấu hình production hợp lệ, image build thành công, không có lỗ hổng image mức HIGH hoặc CRITICAL chưa được chấp thuận, và release manifest hợp lệ.
- **FR-017**: IF registry, SSH, production host hoặc đường truy cập public không sẵn sàng, hệ thống SHALL retry trong giới hạn đã định, không thay đổi production khi lỗi xảy ra trước deploy, và chuyển sang rollback hoặc incident khi lỗi xảy ra sau deploy.
- **FR-018**: WHERE production drift được kiểm tra, hệ thống SHALL cho phép duy nhất server-local `.env`, release/backup artifacts và Docker runtime data đã khai báo; thay đổi tracked source, Compose, deployment scripts hoặc running image ngoài release manifest SHALL bị chặn.

## 5. Non-functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-001 | Thời gian phát hiện phát hành không lành mạnh | Không quá 3 phút sau khi triển khai |
| NFR-002 | Thời gian rollback ứng dụng sau khi phát hiện lỗi | Không quá 10 phút |
| NFR-003 | Khả năng truy vết | 100% lần phát hành có bản ghi đầy đủ |
| NFR-004 | Tính toàn vẹn phiên bản | 100% phiên bản chạy trùng định danh gói đã phê duyệt |
| NFR-005 | Bảo mật bằng chứng | Không có secret dạng rõ trong log hoặc artifact |
| NFR-006 | Độ tin cậy của cổng | Không phát hành khi bất kỳ cổng bắt buộc nào chưa đạt |

## 6. Data Model

### Release Record

- Release identifier
- Source revision identifier
- Immutable package identifier
- Requested and approved timestamps
- Approver identity
- Gate results
- Backup evidence reference when applicable
- Previous stable release identifier
- Final status and diagnostic evidence references

### Entity Rules

- Release records là append-only và không thay thế audit log nghiệp vụ WMS.
- Release artifacts không được chứa secret hoặc dữ liệu nghiệp vụ production.
- Backup production phải được lưu ngoài vòng đời của container ứng dụng.
- Release record phải chứa requester, approver, kết quả từng gate và các timestamp xác minh; các trường này không được sửa sau khi release kết thúc.

## 7. API Spec

Feature này không thêm hoặc thay đổi REST API nghiệp vụ của WMS. Health và smoke
checks chỉ sử dụng các giao diện vận hành hoặc nghiệp vụ đã được phê duyệt; mọi
thay đổi endpoint phát sinh phải được đặc tả và cập nhật OpenAPI riêng.

## 8. Error Handling

| Error | Outcome | Condition |
|-------|---------|-----------|
| QUALITY_GATE_FAILED | Blocked | Một hoặc nhiều cổng chất lượng thất bại |
| APPROVAL_MISSING | Blocked | Chưa có phê duyệt production hợp lệ |
| BACKUP_INVALID | Blocked | Backup thiếu, rỗng hoặc không vượt kiểm tra tính toàn vẹn |
| PRODUCTION_DRIFT | Blocked | Trạng thái production có thay đổi thủ công chưa xử lý |
| DEPLOYMENT_UNHEALTHY | Rollback/incident | Health check hoặc smoke test thất bại |
| ROLLBACK_INCOMPATIBLE | Incident | Phiên bản trước không tương thích với trạng thái dữ liệu hiện tại |
| EXTERNAL_DEPENDENCY_UNAVAILABLE | Blocked/Rollback/Incident | Registry, SSH, production host hoặc public path không sẵn sàng sau retry giới hạn |

## 9. Audit Trail

- Mỗi lần phát hành SHALL có release record append-only với requester, approver, phiên bản, thời gian và kết quả.
- Log phát hành SHALL che hoặc loại bỏ mật khẩu, JWT, khóa SSH, tunnel token và credentials.
- Quy trình CD không được sửa hoặc xóa audit log nghiệp vụ WMS.

## 10. Business Invariants

- Inventory: CD không được bỏ qua các test bảo vệ tồn kho âm, reservation, FIFO hoặc optimistic locking khi bề mặt liên quan thay đổi.
- QC/quarantine: CD không được phát hành thay đổi vi phạm QC gate hoặc đưa quarantine vào available stock.
- Transfer: CD không được bỏ qua In-Transit và xử lý discrepancy khi bề mặt liên quan thay đổi.
- Accounting/credit: Migration và rollback không được làm mất hoặc ghi đè lịch sử tài chính.
- Master/transaction data: Phát hành không được xóa vật lý lịch sử nghiệp vụ, migration đã dùng chung hoặc dữ liệu trong `/data`, `/uploads`.

## 11. Success Criteria

- **SC-001**: 100% phát hành production triển khai đúng phiên bản bất biến đã vượt tất cả cổng và được phê duyệt.
- **SC-002**: Một lỗi xác minh sau triển khai được phát hiện trong 3 phút và ứng dụng được rollback trong 10 phút khi rollback tương thích.
- **SC-003**: Không có lần phát hành nào làm mất dữ liệu nghiệp vụ hoặc xóa lịch sử migration đã áp dụng.
- **SC-004**: Reviewer có thể tái dựng đầy đủ timeline và bằng chứng của 100% lần phát hành trong tối đa 10 phút.
- **SC-005**: 100% phát hành có thay đổi dữ liệu bị chặn nếu thiếu backup hợp lệ hoặc kế hoạch tương thích.
- **SC-006**: Không có secret dạng rõ được phát hiện trong log, artifact hoặc release record.

## 12. Assumptions

- Sprint hiện tại sử dụng một VPS production và một cơ sở dữ liệu production chính.
- Một khoảng gián đoạn ngắn có kiểm soát được chấp nhận; zero-downtime chưa phải yêu cầu Sprint 1.
- Production release luôn cần một người phê duyệt ngoài người tạo thay đổi.
- Rollback tự động chỉ áp dụng cho ứng dụng; restore dữ liệu là quy trình sự cố riêng có phê duyệt.
- Các thay đổi lược đồ production dùng chiến lược tương thích ngược và migration tiến tới, không downgrade tự động.
- Bằng chứng backup có thời hạn lưu giữ tối thiểu 14 ngày; Operator chịu trách nhiệm phê duyệt và ghi nhận mọi chính sách dài hơn trước lần phát hành production đầu tiên.

## 13. Out Of Scope

- Multi-region, active-active hoặc tự động chuyển vùng giữa nhiều VPS.
- Zero-downtime deployment trong Sprint 1.
- Tự động downgrade migration hoặc tự động restore database production.
- Thay đổi domain inventory, QC, transfer, accounting hoặc authorization của WMS.
- Quản lý hạ tầng Cloudflare/Azure hoàn chỉnh theo mô hình infrastructure-as-code.
